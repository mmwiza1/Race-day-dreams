package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Training Piece 1: long-run demotion, budget reconciliation, feasibility and session budget. */
class TrainingPlanSafetyTest {
    private val start = CalendarDates.of(2026, 11, 23)
    private val at = Instants.ofEpochSecond(start.toEpochDay() * 86400)
    private val empty =
        RunnerBaseline.empty(Instants.ofEpochSecond(at.getEpochSecond() - 84 * 86400), at)

    private fun baseline(
        weekly: Double,
        longest: Double,
        runs: Double = 3.0,
        easyPace: Double? = null,
    ) =
        BaselineEngine.applyManualOverlay(
            empty,
            ManualBaselineInput(weekly, runs, longest, null, null, null, easyPace, 8),
            at,
        )

    private fun profile(
        distance: RaceDistance,
        weeks: Long,
        days: Set<DayOfWeek>,
        long: DayOfWeek?,
        frequency: Int = 3,
        mode: GoalMode = GoalMode.FINISH,
    ): UserProfile {
        val race =
            if (distance == RaceDistance.GENERAL_FITNESS) null
            else start.plusWeeks(weeks).minusDays(1)
        return UserProfile(
            RaceGoal(
                distance,
                if (race == null) "" else "Race",
                race,
                mode,
                if (mode == GoalMode.TARGET_TIME) Durations.ofMinutes(120) else null,
            ),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                days,
                frequency,
                long,
                emptySet(),
                ScheduleType.FIXED,
                UnitSystem.METRIC,
            ),
            false,
        )
    }

    private fun generate(p: UserProfile, b: RunnerBaseline, maximum: Int? = null) =
        OriginalTrainingPlanGenerator()
            .generate(PlanRequest(p, b, start, emptySet(), emptyList(), maximum, "BALANCED"))

    private val monTueSun = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.SUNDAY)
    private val monThuFri = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    private fun weekOf(day: LocalDate) = (day.toEpochDay() - start.toEpochDay()) / 7

    @Test
    fun aLongRunAfterTheWeekendsDemandingRunMovesInsteadOfLosingItsKilometres() {
        // Sunday quality followed by a Monday long run: before the fix, every following week
        // lost its long run and the long run's kilometres disappeared from the week.
        val plan =
            generate(
                profile(RaceDistance.HALF_MARATHON, 16, monTueSun, DayOfWeek.MONDAY),
                baseline(30.0, 12.0),
            )
        val race = plan.workouts().single { it.category() == WorkoutCategory.RACE }.date()
        val training = plan.workouts().filter { it.category() != WorkoutCategory.RACE }
        val byWeek = training.groupBy { weekOf(it.date()) }
        // Every week before the taper keeps exactly one long run.
        val buildWeeks =
            byWeek.filterValues { week ->
                week.all { race.toEpochDay() - it.date().toEpochDay() > 14 }
            }
        assertTrue(buildWeeks.size >= 10)
        for ((week, runs) in buildWeeks) assertEquals(
            1,
            runs.count { it.category() == WorkoutCategory.LONG_RUN },
            "week ${week + 1}: $runs",
        )
        val moved = training.filter { it.note()!!.contains("moved within this week") }
        assertTrue(moved.isNotEmpty())
        assertTrue(
            moved.all {
                it.category() == WorkoutCategory.LONG_RUN &&
                    it.date().getDayOfWeek() == DayOfWeek.TUESDAY
            }
        )
        // Demanding sessions keep at least two days apart, across week boundaries too.
        val demanding =
            plan.workouts().filter {
                WorkoutCategories.isDemanding(it.category()) &&
                    it.category() != WorkoutCategory.RACE
            }
        for (i in 1 until demanding.size) assertTrue(
            demanding[i].date().toEpochDay() - demanding[i - 1].date().toEpochDay() >= 2
        )
        // Week totals no longer collapse after week 1: no build week falls below 85% of week 1
        // except planned cutback weeks (every 4th).
        val totals = buildWeeks.toSortedMap().mapValues { (_, w) -> w.sumOf { it.distanceKm()!! } }
        for ((week, total) in totals) if (week % 4 != 3L)
            assertTrue(total >= totals[0]!! * .95, "week ${week + 1}: $total")
    }

    @Test
    fun plannedRunsAddUpToTheWeeklyBudgetOrExplainTheShortfall() {
        val plan =
            generate(
                profile(RaceDistance.HALF_MARATHON, 25, monThuFri, DayOfWeek.MONDAY),
                baseline(19.6, 9.0),
            )
        // The reported half-marathon week 23 is unchanged: 14.7 km long and 8.0 km easy runs.
        val week23 =
            plan.workouts().filter { weekOf(it.date()) == 22L }.sortedBy { it.date().toEpochDay() }
        assertEquals(listOf(14.7, 8.0, 8.0), week23.map { it.distanceKm() })
        assertTrue(week23.none { it.note()!!.contains("below the") })
        // Where single-run limits cut a week, the notes say so instead of hiding it.
        val capped =
            generate(
                profile(RaceDistance.FIVE_K, 8, monThuFri, DayOfWeek.MONDAY),
                baseline(50.0, 20.0),
            )
        val first = capped.workouts().filter { weekOf(it.date()) == 0L }
        assertTrue(first.all { it.distanceKm()!! <= 7.0 })
        assertTrue(
            first.all {
                it.note()!!.contains("below the 50.0 km budget") &&
                    it.note()!!.contains("limited to 7.0 km")
            }
        )
    }

    @Test
    fun aBeginnerMarathonPlanIsNeverPresentedAsSufficientPreparation() {
        val profile = profile(RaceDistance.MARATHON, 18, monThuFri, DayOfWeek.MONDAY)
        val plan = generate(profile, empty)
        assertTrue(
            plan
                .workouts()
                .filter { it.category() != WorkoutCategory.RACE }
                .all { it.distanceKm() == null }
        )
        val result = PlanFeasibility.assess(plan, profile.raceGoal(), profile.preferences())!!
        assertEquals(PlanFeasibility.Level.NOT_SUPPORTED, result.level())
        assertEquals(
            "This plan can't prepare you to run a Marathon by race day.",
            result.headline(),
        )
        assertTrue(result.reasons().first().contains("run/walk of up to 30 minutes"))
        assertTrue(result.alternatives().any { it.startsWith("Add your recent running") })
        assertTrue(result.alternatives().any { it.contains("Half Marathon first") })
        assertTrue(result.alternatives().any { it.contains("qualified coach") })
        // The plan itself is not made more aggressive to satisfy the date.
        assertTrue(
            plan.workouts().mapNotNull { it.duration()?.getSeconds()?.div(60) }.all { it <= 30 }
        )
    }

    @Test
    fun feasibilityExplainsEachDistanceWithoutChangingThePlan() {
        fun level(
            d: RaceDistance,
            weeks: Long,
            b: RunnerBaseline,
            runs: Int = 3,
            mode: GoalMode = GoalMode.FINISH,
        ): PlanFeasibility.Assessment? {
            val p = profile(d, weeks, monThuFri + DayOfWeek.SATURDAY, DayOfWeek.MONDAY, runs, mode)
            return PlanFeasibility.assess(generate(p, b), p.raceGoal(), p.preferences())
        }
        // The reported half marathon: 14.7 km peak is below the usual 16–19 km range.
        val reported =
            level(
                RaceDistance.HALF_MARATHON,
                25,
                baseline(19.6, 9.0),
                mode = GoalMode.TARGET_TIME,
            )!!
        assertEquals(PlanFeasibility.Level.LIMITED, reported.level())
        assertTrue(reported.reasons().first().contains("14.7 km"))
        assertTrue(reported.reasons().any { it.contains("target time") })
        assertTrue(reported.alternatives().any { it.contains("completion goal") })
        // An established half-marathon runner reaches the usual range.
        assertEquals(
            PlanFeasibility.Level.SUPPORTED,
            level(RaceDistance.HALF_MARATHON, 20, baseline(30.0, 12.0), 4)!!.level(),
        )
        assertTrue(
            level(RaceDistance.HALF_MARATHON, 20, baseline(30.0, 12.0), 4)!!
                .alternatives()
                .isEmpty()
        )
        // A beginner 5K with run/walk is supported; a beginner 10K is limited, not refused.
        assertEquals(
            PlanFeasibility.Level.SUPPORTED,
            level(RaceDistance.FIVE_K, 9, empty)!!.level(),
        )
        assertEquals(PlanFeasibility.Level.LIMITED, level(RaceDistance.TEN_K, 10, empty)!!.level())
        // A short build is explained.
        val short = level(RaceDistance.MARATHON, 8, baseline(30.0, 12.0), 4)!!
        assertTrue(
            short.reasons().any {
                it.contains("8 weeks until race day") && it.contains("at least 16 weeks")
            }
        )
        // Two running days for a half are explained.
        val two = level(RaceDistance.HALF_MARATHON, 12, baseline(15.0, 6.0, 2.0), 2)!!
        assertTrue(two.reasons().any { it.startsWith("2 running days") })
        // General fitness has no race to assess.
        assertNull(level(RaceDistance.GENERAL_FITNESS, 12, baseline(20.0, 8.0)))
    }

    @Test
    fun theUsualSessionBudgetLimitsEverydayRunsWhenAPaceEstimateExists() {
        // 6:00/km easy pace and a 45-minute budget: everyday runs stay within 7.5 km; the long
        // run is exempt and its estimated duration is explained.
        val p = profile(RaceDistance.HALF_MARATHON, 16, monThuFri, DayOfWeek.MONDAY)
        val plan = generate(p, baseline(40.0, 16.0, easyPace = 360.0), maximum = 45)
        val training = plan.workouts().filter { it.category() != WorkoutCategory.RACE }
        val everyday = training.filter { it.category() != WorkoutCategory.LONG_RUN }
        assertTrue(
            everyday.all { it.distanceKm()!! <= 7.5 },
            everyday.map { it.distanceKm() }.toString(),
        )
        assertTrue(everyday.all { it.note()!!.contains("Estimated about") })
        val long = training.filter { it.category() == WorkoutCategory.LONG_RUN }
        assertTrue(
            long.any {
                it.distanceKm()!! > 7.5 &&
                    it.note()!!.contains("longer than your usual budget by design")
            }
        )
        assertTrue(training.any { it.note()!!.contains("usual 45-minute session budget") })
        // Without a pace estimate the existing behaviour and explanation are unchanged.
        val noPace = generate(p, baseline(40.0, 16.0), maximum = 45)
        assertTrue(
            noPace
                .workouts()
                .filter { it.category() != WorkoutCategory.RACE }
                .all { it.note()!!.contains("no reliable pace-based duration estimate") }
        )
    }
}
