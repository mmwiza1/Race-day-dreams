package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.plan.*
import org.racedaydreams.domain.time.*

class PlanDiagnosticsContractTest {
    private val start = CalendarDates.of(2026, 11, 16)
    private val end = start.plusDays(13)
    private val policy = PlanRegressionPolicy(4.0, 1.3, 3.0, 1.0, 2, 2, 1, 0.85)
    private val profile =
        UserProfile(
            RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                DayOfWeek.entries.toSet(),
                7,
                null,
                emptySet(),
                ScheduleType.FIXED,
                UnitSystem.METRIC,
            ),
            false,
        )

    private fun workout(
        id: String = "w",
        day: LocalDate = start,
        kind: WorkoutCategory = WorkoutCategory.EASY_RUN,
        distance: Double? = 5.0,
        note: String? = "Original",
    ) =
        PlannedWorkout(
            id,
            day,
            kind,
            distance,
            if (distance == null) Durations.ofMinutes(20) else null,
            emptyList(),
            note,
        )

    private fun plan(
        workouts: List<PlannedWorkout>,
        phases: List<TrainingPhase> =
            listOf(TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, end, "Original")),
    ) = TrainingPlan("p", start, end, phases, workouts)

    @Test
    fun timeOnlyPrescriptionIsUnverifiableRatherThanZeroDistanceOrAnExplicitViolation() {
        val p = plan(listOf(workout(distance = null)))
        val before = PlanSnapshot.render(p)
        val report = PlanRegression.inspect(p, profile, emptySet(), policy)
        assertTrue(report.passed())
        assertFalse(report.fullyVerified())
        assertTrue(report.has("UNKNOWN_WEEKLY_DISTANCE"))
        val week = report.weeks().first()
        assertNull(week.trainingDistanceKm())
        assertEquals(0.0, week.knownDistanceKm())
        assertEquals(1, week.missingDistancePrescriptions())
        assertNull(week.longestRunKm())
        assertEquals(before, PlanSnapshot.render(p))
    }

    @Test
    fun duplicateIdentityDaysAndCloseHardSessionsProduceSeparateExplainableDiagnostics() {
        val p =
            plan(
                listOf(
                    workout(kind = WorkoutCategory.INTERVALS),
                    workout(kind = WorkoutCategory.INTERVALS),
                    workout("w2", start.plusDays(1), WorkoutCategory.INTERVALS),
                )
            )
        val report = PlanRegression.inspect(p, profile, emptySet(), policy)
        for (code in
            listOf(
                "DUPLICATE_WORKOUT_ID",
                "MULTIPLE_RUNS_ON_DAY",
                "HARD_DAY_SPACING",
                "HARD_FREQUENCY",
            )) assertTrue(report.has(code), code)
        assertFalse(report.passed())
    }

    @Test
    fun lowerBodyContextIsExplicitAndCannotSilentlyMoveThePrescription() {
        val p = plan(listOf(workout(day = start.plusDays(1), kind = WorkoutCategory.LONG_RUN)))
        val before = PlanSnapshot.render(p)
        assertFalse(
            PlanRegression.inspect(p, profile, emptySet(), policy).has("HARD_LOWER_BODY_PROXIMITY")
        )
        assertTrue(
            PlanRegression.inspect(p, profile, setOf(start), policy)
                .has("HARD_LOWER_BODY_PROXIMITY")
        )
        assertEquals(before, PlanSnapshot.render(p))
    }

    @Test
    fun missingAndOverlappingPhasesRemainStructuralErrors() {
        val one = workout()
        assertTrue(
            PlanRegression.inspect(plan(listOf(one), emptyList()), profile, emptySet(), policy)
                .has("PHASE_GAP")
        )
        val phase = TrainingPhase(TrainingPhase.Kind.FOUNDATION, start, end, "Original")
        assertTrue(
            PlanRegression.inspect(
                    plan(listOf(one), listOf(phase, phase)),
                    profile,
                    emptySet(),
                    policy,
                )
                .has("PHASE_OVERLAP")
        )
    }

    @Test
    fun snapshotIsOrderIndependentAndDistinguishesUnknownNotesFromLiteralMarkers() {
        val a = workout(note = null)
        val b = workout("b", start.plusDays(1), note = "~|\r\n\\")
        assertEquals(
            PlanSnapshot.render(plan(listOf(a, b))),
            PlanSnapshot.render(plan(listOf(b, a))),
        )
        val unknown = PlanSnapshot.render(plan(listOf(a)))
        assertNotEquals(unknown, PlanSnapshot.render(plan(listOf(workout(note = "~")))))
        assertTrue(PlanSnapshot.render(plan(listOf(b))).contains("\\~\\|\\r\\n\\\\"))
    }

    @Test
    fun lineDiffPreservesTrailingEmptyLinesAndKeepsItsOriginalBound() {
        assertEquals("", PlanSnapshot.difference("same\n", "same\n"))
        assertEquals("-2 \n", PlanSnapshot.difference("one\n", "one"))
        val diff = PlanSnapshot.difference("old\n".repeat(100), "new\n".repeat(100))
        assertTrue(diff.contains("-60 old\n+60 new\n"))
        assertFalse(diff.contains("-61"))
        assertTrue(diff.endsWith("... additional changed lines omitted\n"))
        assertFalse(PlanRegression.hard(null))
        assertFalse(PlanRegression.hard(WorkoutCategory.LONG_RUN))
        assertTrue(PlanRegression.hard(WorkoutCategory.RACE))
    }
}
