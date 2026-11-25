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
        recentHalf: Duration? = null,
    ) =
        BaselineEngine.applyManualOverlay(
            empty,
            ManualBaselineInput(weekly, runs, longest, null, null, recentHalf, easyPace, 8),
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
        // Week totals no longer collapse after week 1: no build week falls below 95% of week 1
        // except planned cutback weeks (every 4th).
        val totals = buildWeeks.mapValues { entry -> entry.value.sumOf { it.distanceKm()!! } }
        val first = totals.getValue(0L)
        for (entry in totals) if (entry.key % 4 != 3L)
            assertTrue(entry.value >= first * .95, "week ${entry.key + 1}: ${entry.value}")
    }

    @Test
    fun aMovedLongRunStaysOnAvailableDaysAndAvoidsHardLegDays() {
        // End to end: the moved long run stays on the runner's available days.
        val plan =
            generate(
                profile(RaceDistance.HALF_MARATHON, 16, monTueSun, DayOfWeek.MONDAY),
                baseline(30.0, 12.0),
            )
        val moved = plan.workouts().filter { it.note()!!.contains("moved within this week") }
        assertTrue(moved.isNotEmpty())
        assertTrue(moved.all { it.date().getDayOfWeek() in monTueSun })
        // The choice itself: available days only, two days from demanding runs, away from hard
        // leg days and the day after them, otherwise the latest suitable day.
        val sunday = start.minusDays(1)
        val mon = start
        val tue = start.plusDays(1)
        val wed = start.plusDays(2)
        val fri = start.plusDays(4)
        val week = listOf(mon, tue, wed, fri)
        val choose = { heavy: Set<LocalDate> ->
            OriginalTrainingPlanGenerator.relocatedLongDay(week, fri, sunday, heavy)
        }
        // Monday follows Sunday's demanding run; Friday is the quality day; Thursday is not
        // available.
        assertEquals(wed, choose(emptySet()))
        // A hard squat session on Wednesday: Tuesday carries no recovery cost, so it is preferred.
        assertEquals(tue, choose(setOf(wed)))
        // A hard session on Tuesday: Wednesday would be the day after it, Tuesday the same day;
        // the same-day cost is lower, so Tuesday is kept rather than dropping the long run.
        assertEquals(tue, choose(setOf(tue)))
        // No suitable day at all: no long run this week (the budget is then shared).
        assertNull(
            OriginalTrainingPlanGenerator.relocatedLongDay(listOf(mon), null, sunday, emptySet())
        )
    }

    @Test
    fun plannedRunsAddUpToTheWeeklyBudgetOrExplainTheShortfall() {
        val plan =
            generate(
                profile(RaceDistance.HALF_MARATHON, 25, monThuFri, DayOfWeek.MONDAY),
                baseline(19.6, 9.0),
            )
        // The reported half marathon: since Piece 3, every week's runs add up to its budget, so no
        // week needs a shortfall explanation. (Session-budget shortfalls are covered below.)
        assertTrue(plan.workouts().none { it.note()!!.contains("below the") })
        val week23 =
            plan.workouts().filter { weekOf(it.date()) == 22L }.sortedBy { it.date().toEpochDay() }
        assertEquals(listOf(17.0, 8.5, 8.5), week23.map { it.distanceKm() })
        // An experienced runner's 5K plan is no longer cut by a 7 km single-run limit.
        val experienced =
            generate(
                profile(RaceDistance.FIVE_K, 8, monThuFri, DayOfWeek.MONDAY),
                baseline(50.0, 20.0),
            )
        val first = experienced.workouts().filter { weekOf(it.date()) == 0L }
        assertTrue(first.none { it.note()!!.contains("below the") })
        assertEquals(50.0, first.sumOf { it.distanceKm()!! }, 0.5)
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
            "Your plan can't prepare you to run a Marathon by race day.",
            result.headline(),
        )
        assertTrue(result.reasons().first().contains("run/walk of up to"))
        assertTrue(result.alternatives().any { it.startsWith("Add your recent running") })
        assertTrue(result.alternatives().any { it.contains("Half Marathon first") })
        assertTrue(
            result.reasons().first().contains("doesn't yet show you can run the whole Marathon")
        )
        assertTrue(result.alternatives().any { it.contains("qualified coach") })
        // The plan itself is not made more aggressive to satisfy the date.
        assertTrue(
            plan
                .workouts()
                .filter { it.category() != WorkoutCategory.LONG_RUN }
                .mapNotNull { it.duration()?.getSeconds()?.div(60) }
                .all { it <= 30 } &&
                plan.workouts().mapNotNull { it.duration()?.getSeconds()?.div(60) }.all { it <= 90 }
        )
    }

    private fun assess(
        d: RaceDistance,
        weeks: Long,
        b: RunnerBaseline,
        runs: Int = 3,
        mode: GoalMode = GoalMode.FINISH,
        days: Set<DayOfWeek> = monThuFri + DayOfWeek.SATURDAY,
    ): PlanFeasibility.Assessment? {
        val p = profile(d, weeks, days, DayOfWeek.MONDAY, runs, mode)
        return PlanFeasibility.assess(generate(p, b), p.raceGoal(), p.preferences(), b)
    }

    @Test
    fun feasibilityExplainsEachDistanceWithoutChangingThePlan() {
        // The reported half marathon with a 2-hour target.
        val reported =
            assess(
                RaceDistance.HALF_MARATHON,
                25,
                baseline(19.6, 9.0),
                mode = GoalMode.TARGET_TIME,
            )!!
        // Since Piece 3 it builds to a 17 km long run and 34 km weeks, so preparation is no
        // longer the limit; with no recent race time the 2-hour target still can't be judged.
        assertEquals(PlanFeasibility.Level.LIMITED, reported.level())
        assertTrue(reported.reasons().single().contains("no recent race time"))
        assertEquals(
            "Your plan prepares you to finish a Half Marathon, but your target time isn't backed by recent results.",
            reported.headline(),
        )
        assertTrue(reported.alternatives().any { it.contains("completion goal") })
        // An established half-marathon runner with time and four running days follows the usual
        // build-up; even then the rating carries the not-a-guarantee caveat.
        val established = assess(RaceDistance.HALF_MARATHON, 20, baseline(30.0, 12.0), 4)!!
        assertEquals(
            PlanFeasibility.Level.SUPPORTED,
            established.level(),
            established.reasons().toString(),
        )
        assertTrue(established.reasons().isEmpty() && established.alternatives().isEmpty())
        assertTrue(established.caveat().contains("not a guarantee"))
        // General fitness has no race to assess.
        assertNull(assess(RaceDistance.GENERAL_FITNESS, 12, baseline(20.0, 8.0)))
    }

    @Test
    fun shortBuildsAndFewRunningDaysLowerTheRatingNotJustTheText() {
        // Same experienced runner and a long-enough longest run, but only 8 weeks to a half.
        val short =
            assess(
                RaceDistance.HALF_MARATHON,
                8,
                baseline(50.0, 20.0, 5.0),
                5,
                days = monThuFri + DayOfWeek.SATURDAY + DayOfWeek.TUESDAY,
            )!!
        assertEquals(PlanFeasibility.Level.LIMITED, short.level(), short.reasons().toString())
        assertTrue(
            short.reasons().any {
                it.contains("8 weeks until race day") && it.contains("at least 12 weeks")
            }
        )
        // Twenty weeks to a half, but only two running days.
        val two =
            assess(
                RaceDistance.HALF_MARATHON,
                20,
                baseline(30.0, 12.0, 2.0),
                2,
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            )!!
        assertNotEquals(PlanFeasibility.Level.SUPPORTED, two.level())
        assertTrue(two.reasons().any { it.startsWith("Running 2 days a week") })
    }

    @Test
    fun runWalkTimeIsNotTreatedAsContinuousRunning() {
        // Beginners: run/walk never rates Supported, even for a 5K, and the reason says why.
        val fiveK = assess(RaceDistance.FIVE_K, 9, empty)!!
        assertEquals(PlanFeasibility.Level.LIMITED, fiveK.level())
        assertTrue(fiveK.reasons().first().contains("doesn't yet show you can run the whole 5K"))
        assertTrue(fiveK.alternatives().any { it.startsWith("Run/walk the race") })
        assertEquals(PlanFeasibility.Level.LIMITED, assess(RaceDistance.TEN_K, 10, empty)!!.level())
        // A hand-built plan with long run/walk sessions is still not Supported for a half.
        val race = start.plusWeeks(16).minusDays(1)
        val walks =
            (0 until 16).map { w ->
                PlannedWorkout(
                    "w$w",
                    start.plusWeeks(w.toLong()),
                    WorkoutCategory.EASY_RUN,
                    null,
                    Durations.ofMinutes(150),
                    emptyList(),
                    "run/walk",
                )
            }
        val plan = TrainingPlan("walks", start, race, emptyList(), walks)
        val goal = RaceGoal(RaceDistance.HALF_MARATHON, "Race", race, GoalMode.FINISH, null)
        assertEquals(
            PlanFeasibility.Level.LIMITED,
            PlanFeasibility.assess(plan, goal, null)!!.level(),
        )
    }

    @Test
    fun oneLongRunIsNotEnoughEvidence() {
        // Plenty of weekly running, but the half-marathon-length run happens only once.
        val race = start.plusWeeks(16).minusDays(1)
        val workouts =
            (0 until 15).flatMap { w ->
                val monday = start.plusWeeks(w.toLong())
                listOf(
                    PlannedWorkout(
                        "l$w",
                        monday,
                        WorkoutCategory.LONG_RUN,
                        if (w == 12) 18.0 else 11.0,
                        null,
                        emptyList(),
                        "long",
                    ),
                    PlannedWorkout(
                        "a$w",
                        monday.plusDays(2),
                        WorkoutCategory.EASY_RUN,
                        10.0,
                        null,
                        emptyList(),
                        "easy",
                    ),
                    PlannedWorkout(
                        "b$w",
                        monday.plusDays(4),
                        WorkoutCategory.EASY_RUN,
                        10.0,
                        null,
                        emptyList(),
                        "easy",
                    ),
                )
            }
        val plan = TrainingPlan("once", start, race, emptyList(), workouts)
        val goal = RaceGoal(RaceDistance.HALF_MARATHON, "Race", race, GoalMode.FINISH, null)
        val result = PlanFeasibility.assess(plan, goal, null)!!
        assertEquals(PlanFeasibility.Level.LIMITED, result.level())
        assertTrue(result.reasons().single().contains("only once"))
    }

    @Test
    fun readinessTextAgreesWithTheRatingWhenThereIsNoRecentRaceTime() {
        // Audit R-01: a target time without a recent race time must not claim the plan prepares
        // the runner to finish unless the preparation itself is Supported.
        fun texts(a: PlanFeasibility.Assessment) = a.reasons() + a.headline()
        val finishClaim = "prepares you to finish"
        // Supported preparation: the finish statement is true and kept.
        val supported =
            assess(
                RaceDistance.HALF_MARATHON,
                25,
                baseline(19.6, 9.0),
                mode = GoalMode.TARGET_TIME,
            )!!
        assertEquals(PlanFeasibility.Level.LIMITED, supported.level())
        assertTrue(supported.reasons().single().contains("Your plan prepares you to finish;"))
        // Limited preparation (8 weeks to a half): no finish claim; finishing comes first.
        val limited =
            assess(RaceDistance.HALF_MARATHON, 8, baseline(30.0, 12.0), 4, GoalMode.TARGET_TIME)!!
        assertEquals(PlanFeasibility.Level.LIMITED, limited.level())
        assertTrue(texts(limited).none { it.contains(finishClaim) }, texts(limited).toString())
        assertTrue(limited.reasons().any { it.contains("focus on being ready to finish") })
        // The audit case: marathon target, three runs, no history, five weeks: Not supported,
        // and nothing reassures the runner that the plan prepares them to finish.
        for ((distance, weeks) in
            listOf(RaceDistance.MARATHON to 5L, RaceDistance.HALF_MARATHON to 4L)) {
            val unsupported = assess(distance, weeks, empty, 3, GoalMode.TARGET_TIME)!!
            assertEquals(PlanFeasibility.Level.NOT_SUPPORTED, unsupported.level(), distance.name)
            assertTrue(texts(unsupported).none { it.contains(finishClaim) }, distance.name)
            assertTrue(
                unsupported.reasons().any { it.contains("can't yet prepare you to finish") },
                distance.name,
            )
        }
        // 5K run/walk with a target and no history: Limited, not described as finish-ready.
        val fiveK = assess(RaceDistance.FIVE_K, 10, empty, 3, GoalMode.TARGET_TIME)!!
        assertEquals(PlanFeasibility.Level.LIMITED, fiveK.level())
        assertTrue(texts(fiveK).none { it.contains(finishClaim) }, texts(fiveK).toString())
        // Ambiguous evidence (a recent time slower than the target) with limited preparation:
        // the headline describes limited preparation, never finish readiness.
        val ambiguous =
            assess(
                RaceDistance.HALF_MARATHON,
                8,
                baseline(30.0, 12.0, recentHalf = Durations.ofMinutes(130)),
                4,
                GoalMode.TARGET_TIME,
            )!!
        assertEquals(PlanFeasibility.Level.LIMITED, ambiguous.level())
        assertTrue(texts(ambiguous).none { it.contains(finishClaim) }, texts(ambiguous).toString())
    }

    @Test
    fun aTargetTimeNeedsARecentResultThatSupportsIt() {
        // 1:55 recent half supports a 2-hour target; 2:10 does not.
        val fast =
            assess(
                RaceDistance.HALF_MARATHON,
                20,
                baseline(30.0, 12.0, recentHalf = Durations.ofMinutes(115)),
                4,
                GoalMode.TARGET_TIME,
            )!!
        assertEquals(PlanFeasibility.Level.SUPPORTED, fast.level(), fast.reasons().toString())
        val slow =
            assess(
                RaceDistance.HALF_MARATHON,
                20,
                baseline(30.0, 12.0, recentHalf = Durations.ofMinutes(130)),
                4,
                GoalMode.TARGET_TIME,
            )!!
        assertEquals(PlanFeasibility.Level.LIMITED, slow.level())
        assertEquals(
            "Your plan prepares you to finish a Half Marathon, but your target time isn't backed by recent results.",
            slow.headline(),
        )
        assertTrue(
            slow.reasons().single().contains("about 2:10:00") &&
                slow.reasons().single().contains("2:00:00")
        )
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
