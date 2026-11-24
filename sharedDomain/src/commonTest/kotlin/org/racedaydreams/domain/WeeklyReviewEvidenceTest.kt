package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.race.RacePredictionEngine
import org.racedaydreams.domain.time.*

/**
 * Audit B-02 (one physical run counted once), B-R4 (hold without distance evidence) and B-R5 (the
 * review's "not after today" boundary is local midnight).
 */
class WeeklyReviewEvidenceTest {
    private val utc = LocalZone.fixed(Offsets.ofTotalSeconds(0))

    private fun at(day: LocalDate, second: Long) =
        Instants.ofEpochSecond(day.toEpochDay() * 86400 + second)

    private fun workout(id: String, day: LocalDate, km: Double = 10.0) =
        PlannedWorkout(
            id,
            day,
            WorkoutCategory.EASY_RUN,
            km,
            null,
            listOf(
                WorkoutSegment(WorkoutSegment.Kind.RUN, km, null, 300.0, 360.0, null, 3.0, "easy")
            ),
            "easy",
        )

    private fun version(workouts: List<PlannedWorkout>, today: LocalDate) =
        PlanVersion(
            "v",
            "p",
            1,
            at(today, 0),
            "",
            UserProfile(
                RaceGoal(RaceDistance.HALF_MARATHON, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    DayOfWeek.values().toSet(),
                    4,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FIXED,
                    UnitSystem.METRIC,
                ),
                false,
            ),
            "",
            TrainingPlan("p", today.minusDays(35), today.plusDays(28), emptyList(), workouts),
        )

    private fun record(
        id: String,
        provider: String,
        origin: String?,
        start: Instant,
        seconds: Long,
        km: Double?,
        planned: String? = null,
        rpe: Double? = null,
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, origin),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + seconds),
            null,
            null,
            km?.times(1000),
            null,
            null,
            null,
            null,
            null,
            rpe,
            "",
            emptyList(),
            emptyList(),
            planned,
            null,
        )

    /** A native recording of [w] plus the watch's copy of the same run through Health Connect. */
    private fun recordedTwice(w: PlannedWorkout, rpe: Double? = null, km: Double? = 10.0) =
        listOf(
            record(
                "rdd:${w.id()}",
                "rdd",
                "org.racedaydreams",
                at(w.date(), 3600),
                3600,
                km,
                w.id(),
                rpe,
            ),
            record(
                "hc:${w.id()}",
                "health-connect",
                "com.garmin",
                at(w.date(), 3660),
                3570,
                km?.plus(0.05),
            ),
        )

    // Reviewed week: Monday 2 to Sunday 8 November 2026.
    private val today = CalendarDates.of(2026, 11, 15)
    private val reviewed =
        listOf(3L, 5L, 7L).map { workout("r$it", CalendarDates.of(2026, 11, it.toInt())) }
    private val upcoming = (9..12).map { workout("u$it", CalendarDates.of(2026, 11, it)) }

    private fun review(runs: List<ActivityRecord>): WeeklyCoaching.Review {
        val v = version(reviewed + upcoming, today)
        return WeeklyCoaching.review(
            v,
            listOf(v),
            runs,
            emptyList(),
            emptyList(),
            emptyList(),
            today,
            null,
        )
    }

    @Test
    fun aRunRecordedNativelyAndImportedFromAWatchCountsOnceInTheReview() =
        ActivityDates.withZone(utc) {
            val r = review(reviewed.flatMap { recordedTwice(it, rpe = 9.0) })
            assertEquals(30.0, r.kilometres())
            assertEquals(10.0, r.longestKm())
            assertEquals(3, r.completed())
            // Three hard sessions: hold is offered with the deduplicated 30 km ceiling, so the
            // following 40 km week is held to 75%, not left untouched by a doubled 60 km.
            val hold = r.choices().single { it.id() == "hold" }.replacements()
            assertEquals(
                List(4) { 7.5 },
                hold
                    .filter { it.date().toEpochDay() < CalendarDates.of(2026, 11, 16).toEpochDay() }
                    .map { it.distanceKm() },
            )
        }

    @Test
    fun holdWithoutAnyRecordedDistanceKeepsThePlannedVolume() =
        ActivityDates.withZone(utc) {
            val r = review(reviewed.flatMap { recordedTwice(it, rpe = 9.0, km = null) })
            assertEquals(0.0, r.kilometres())
            val hold = r.choices().single { it.id() == "hold" }.replacements()
            val reduce = r.choices().single { it.id() == "reduce" }.replacements()
            assertTrue(hold.isNotEmpty())
            assertTrue(hold.all { it.distanceKm() == 10.0 }, hold.toString())
            assertTrue(reduce.all { it.distanceKm() == 8.0 }, reduce.toString())
        }

    @Test
    fun racePredictionCountsAWatchCopyOfANativeRunOnce() =
        ActivityDates.withZone(utc) {
            val runs = reviewed.flatMap { recordedTwice(it) }
            val reasons =
                RacePredictionEngine.predict(21097.5, emptyList(), runs, today).reasons()!!
            // 30 km over four weeks is 7.5 km a week, not 15.
            assertTrue(
                reasons.any { it.startsWith("Recent weekly average 7.5") },
                reasons.toString(),
            )
        }

    @Test
    fun aMovedRunCompletedLateTodayWestOfUtcIsNotTreatedAsMissed() =
        ActivityDates.withZone(LocalZone.fixed(Offsets.ofTotalSeconds(-8 * 3600))) {
            // Monday 16 November; three tempo workouts from last week were all run this evening,
            // 21:00 to 23:00 local, which is already Tuesday in UTC.
            val monday = CalendarDates.of(2026, 11, 16)
            val moved =
                (10..12).map {
                    PlannedWorkout(
                        "t$it",
                        CalendarDates.of(2026, 11, it),
                        WorkoutCategory.TEMPO_THRESHOLD_RUN,
                        8.0,
                        null,
                        emptyList(),
                        "tempo",
                    )
                }
            val runs =
                moved.mapIndexed { i, w ->
                    record(
                        "late$i",
                        "rdd",
                        "org.racedaydreams",
                        at(monday, (21L + i) * 3600 + 8 * 3600),
                        1800,
                        8.0,
                        w.id(),
                    )
                }
            val v = version(moved, monday)
            val r =
                WeeklyCoaching.review(
                    v,
                    listOf(v),
                    runs,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    monday,
                    null,
                )
            assertTrue(
                r.observations().none { it.contains("were not recorded recently") },
                r.observations().toString(),
            )
        }
}
