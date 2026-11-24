package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/** Training Piece 3: individual weekly targets, long-run track, cutbacks and taper. */
class RunningProgressionTest {
    private val start = CalendarDates.of(2026, 11, 23)
    private val at = Instants.ofEpochSecond(start.toEpochDay() * 86400)
    private val empty =
        RunnerBaseline.empty(Instants.ofEpochSecond(at.getEpochSecond() - 84 * 86400), at)
    private val monThuFri = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    private val four =
        setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    private val five = four + DayOfWeek.WEDNESDAY

    private fun baseline(weekly: Double, longest: Double, runs: Double) =
        BaselineEngine.applyManualOverlay(
            empty,
            ManualBaselineInput(weekly, runs, longest, null, null, null, null, 8),
            at,
        )

    private fun plan(
        distance: RaceDistance,
        race: LocalDate?,
        b: RunnerBaseline,
        days: Set<DayOfWeek>,
        long: DayOfWeek,
        maximum: Int? = null,
    ): TrainingPlan {
        val p =
            UserProfile(
                RaceGoal(distance, if (race == null) "" else "Race", race, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    days,
                    days.size,
                    long,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            )
        return OriginalTrainingPlanGenerator()
            .generate(PlanRequest(p, b, start, emptySet(), emptyList(), maximum, "BALANCED"))
    }

    private fun weeks(plan: TrainingPlan): List<List<PlannedWorkout>> =
        plan
            .workouts()
            .filter { it.category() != WorkoutCategory.RACE }
            .groupBy { (it.date().toEpochDay() - start.toEpochDay()) / 7 }
            .entries
            .sortedBy { it.key }
            .map { it.value }

    private fun total(week: List<PlannedWorkout>) = week.sumOf { it.distanceKm() ?: 0.0 }

    private fun longs(plan: TrainingPlan) =
        plan.workouts().filter { it.category() == WorkoutCategory.LONG_RUN }

    @Test
    fun theReportedHalfMarathonBuildsToUsualPreparationGradually() {
        val race = CalendarDates.of(2027, 5, 16)
        val p =
            plan(
                RaceDistance.HALF_MARATHON,
                race,
                baseline(19.6, 9.0, 3.0),
                monThuFri,
                DayOfWeek.MONDAY,
            )
        val w = weeks(p)
        val peak = w.maxOf { total(it) }
        // A busiest week in the usual range for three runs, reached gradually from 19.6 km.
        assertTrue(peak in 30.0..38.0, "peak $peak")
        // Each week stays within about 10% of recent tolerated load (the busier of the two
        // previous weeks), so returning from a lighter week is not treated as a spike.
        for (i in 1 until w.size) {
            val recent = maxOf(total(w[i - 1]), if (i >= 2) total(w[i - 2]) else 0.0)
            assertTrue(total(w[i]) <= recent * 1.10 + 0.6, "week ${i + 1}")
        }
        // A longest run in the usual 16–19 km range for a half.
        val longest = longs(p).maxOf { it.distanceKm()!! }
        assertTrue(longest in 16.0..19.0, "longest $longest")
        // Taper: a shorter long run is kept 10–14 days out, none in the last 9 days, and the race
        // week is about half of the busiest week.
        val taperLong = longs(p).filter { race.toEpochDay() - it.date().toEpochDay() in 10..14 }
        assertTrue(taperLong.isNotEmpty() && taperLong.all { it.distanceKm()!! < longest })
        assertTrue(longs(p).none { race.toEpochDay() - it.date().toEpochDay() < 10 })
        assertTrue(total(w.last()) <= peak * .55)
    }

    @Test
    fun longRunsStayWithinAboutTenPercentOfTheLast30DaysLongest() {
        for ((distance, b) in
            listOf(
                RaceDistance.HALF_MARATHON to baseline(19.6, 9.0, 3.0),
                RaceDistance.MARATHON to baseline(30.0, 12.0, 4.0),
            )) {
            val race = start.plusWeeks(20).minusDays(1)
            val p =
                plan(
                    distance,
                    race,
                    b,
                    if (distance == RaceDistance.MARATHON) four else monThuFri,
                    if (distance == RaceDistance.MARATHON) DayOfWeek.SUNDAY else DayOfWeek.MONDAY,
                )
            val longRuns = longs(p)
            for (run in longRuns) {
                val recent =
                    (longRuns
                            .filter {
                                it.date().toEpochDay() < run.date().toEpochDay() &&
                                    it.date().toEpochDay() > run.date().toEpochDay() - 30
                            }
                            .map { it.distanceKm()!! } + b.longestRunKm()!!)
                        .max()
                assertTrue(
                    run.distanceKm()!! <= recent * 1.10 + 0.06,
                    "${run.date()} ${run.distanceKm()} vs $recent",
                )
            }
        }
    }

    @Test
    fun establishedShortRaceRunnersKeepTheirToleratedLongRunsAndVolume() {
        for (distance in listOf(RaceDistance.FIVE_K, RaceDistance.TEN_K)) {
            val p =
                plan(
                    distance,
                    start.plusWeeks(10).minusDays(1),
                    baseline(50.0, 20.0, 5.0),
                    five,
                    DayOfWeek.SUNDAY,
                )
            val w = weeks(p)
            assertEquals(50.0, total(w.first()), 0.5)
            assertEquals(20.0, longs(p).maxOf { it.distanceKm()!! }, 0.05)
            assertTrue(p.workouts().none { it.note()!!.contains("below the") })
        }
    }

    @Test
    fun cutbacksFollowTheCurrentWeekAndNeverStackWithTheTaper() {
        val race = CalendarDates.of(2027, 5, 16)
        val p =
            plan(
                RaceDistance.HALF_MARATHON,
                race,
                baseline(19.6, 9.0, 3.0),
                monThuFri,
                DayOfWeek.MONDAY,
            )
        val w = weeks(p)
        // Week 20 (a cutback) is about 80% of week 19, and its long run about 80% of week 19's.
        assertEquals(total(w[18]) * .8, total(w[19]), 0.4)
        val long19 = w[18].single { it.category() == WorkoutCategory.LONG_RUN }.distanceKm()!!
        val long20 = w[19].single { it.category() == WorkoutCategory.LONG_RUN }.distanceKm()!!
        assertEquals(long19 * .8, long20, 0.15)
        // Week 24 is both a 4th week and 13 days from the race: it gets the taper only (≈75% of
        // the held peak), not taper × cutback (≈56%).
        assertTrue(total(w[23]) >= total(w[22]) * .7, "${total(w[23])} vs ${total(w[22])}")
    }

    @Test
    fun taperLengthFollowsTheRaceDistance() {
        // Marathon (base under 45 km): two lighter weeks; 10K: only the race week is reduced.
        val marathonRace = start.plusWeeks(18).minusDays(1)
        val m =
            weeks(
                plan(
                    RaceDistance.MARATHON,
                    marathonRace,
                    baseline(30.0, 12.0, 4.0),
                    four,
                    DayOfWeek.SUNDAY,
                )
            )
        val mPeak = m.maxOf { total(it) }
        assertTrue(total(m[m.size - 2]) < mPeak * .9 && total(m.last()) < mPeak * .5)
        val tenRace = start.plusWeeks(10).minusDays(1)
        val t =
            weeks(
                plan(RaceDistance.TEN_K, tenRace, baseline(30.0, 12.0, 4.0), four, DayOfWeek.SUNDAY)
            )
        assertTrue(total(t.last()) < total(t[t.size - 2]) * .7)
    }

    @Test
    fun beginnersLongerRunWalkProgressesGentlyAndRespectsTheirTimeBudget() {
        val race = start.plusWeeks(16).minusDays(1)
        val p = plan(RaceDistance.HALF_MARATHON, race, empty, monThuFri, DayOfWeek.MONDAY)
        val minutes = { w: PlannedWorkout -> w.duration()!!.getSeconds() / 60 }
        val longSessions = longs(p).map(minutes)
        assertTrue(longSessions.max() in 31L..70L, longSessions.toString())
        assertTrue(
            p.workouts()
                .filter {
                    it.category() != WorkoutCategory.LONG_RUN &&
                        it.category() != WorkoutCategory.RACE
                }
                .all { minutes(it) <= 30 }
        )
        // No fabricated distance, and a 20-minute budget caps every session, long included.
        assertTrue(
            p.workouts()
                .filter { it.category() != WorkoutCategory.RACE }
                .all { it.distanceKm() == null }
        )
        val budget =
            plan(RaceDistance.HALF_MARATHON, race, empty, monThuFri, DayOfWeek.MONDAY, maximum = 20)
        assertTrue(
            budget
                .workouts()
                .filter { it.category() != WorkoutCategory.RACE }
                .all { minutes(it) <= 20 }
        )
    }

    @Test
    fun generalFitnessProgressesGentlyWithoutAPush() {
        val p =
            plan(
                RaceDistance.GENERAL_FITNESS,
                null,
                baseline(20.0, 8.0, 3.0),
                monThuFri,
                DayOfWeek.MONDAY,
            )
        val peak = weeks(p).maxOf { total(it) }
        assertTrue(peak > 20.0 && peak <= 28.1, "peak $peak")
    }
}
