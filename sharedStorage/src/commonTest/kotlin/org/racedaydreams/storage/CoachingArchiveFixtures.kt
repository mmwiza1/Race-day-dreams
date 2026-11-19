package org.racedaydreams.storage

import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

internal object CoachingArchiveFixtures {
    val start = CalendarDates.of(2026, 11, 16)
    val at = StoredInstant.parse("2026-11-15T09:00:00.123456789Z")

    fun workout(id: String = "done", date: LocalDate = start): PlannedWorkout =
        PlannedWorkout(
            id,
            date,
            WorkoutCategory.EASY_RUN,
            5.0,
            Durations.ofMinutes(30),
            listOf(
                WorkoutSegment(
                    WorkoutSegment.Kind.RUN,
                    null,
                    Durations.ofMinutes(30),
                    300.0,
                    360.0,
                    null,
                    4.5,
                    "Talk comfortably",
                )
            ),
            "retained 🏃\u0000 note",
        )

    fun profile(): UserProfile =
        UserProfile(
            RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                3,
                DayOfWeek.FRIDAY,
                setOf(DayOfWeek.TUESDAY),
                ScheduleType.FLEXIBLE,
                UnitSystem.METRIC,
            ),
            false,
        )

    fun version(
        number: Int = 1,
        workouts: List<PlannedWorkout> = listOf(workout(), workout("future", start.plusDays(1))),
        id: String = "v$number",
    ): PlanVersion =
        PlanVersion(
            id,
            "plan",
            number,
            at,
            "Athlete accepted",
            profile(),
            "retained evidence",
            TrainingPlan(
                "plan",
                start,
                start.plusDays(7),
                listOf(
                    TrainingPhase(
                        TrainingPhase.Kind.FOUNDATION,
                        start,
                        start.plusDays(7),
                        "Build a routine",
                    )
                ),
                workouts,
            ),
        )

    fun proposal(id: String = "proposal", versionId: String = "v1"): AdjustmentProposal =
        AdjustmentProposal(
            id,
            versionId,
            "future",
            at,
            "Recorded context",
            listOf("Actual recorded evidence"),
            "Keep or rest",
            "Limited",
            listOf(
                AdjustmentProposal.Option(
                    "keep",
                    "Keep plan",
                    AdjustmentProposal.Action.KEEP,
                    null,
                ),
                AdjustmentProposal.Option(
                    "rest",
                    "Rest today",
                    AdjustmentProposal.Action.REST,
                    null,
                ),
                AdjustmentProposal.Option(
                    "replace",
                    "Shorter",
                    AdjustmentProposal.Action.REPLACE,
                    workout("future", start.plusDays(1)).copy(distanceKm = 3.0),
                ),
            ),
        )
}
