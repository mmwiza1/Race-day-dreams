package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class CoachingSnapshotContractTest {
    private val date = CalendarDates.of(2026, 11, 15)
    private val at = Instants.ofEpochSecond(1794700800)

    private fun later(seconds: Long) = Instants.ofEpochSecond(at.getEpochSecond() + seconds)

    private fun workout(
        id: String = "w",
        day: LocalDate = date,
        category: WorkoutCategory = WorkoutCategory.EASY_RUN,
        segments: List<WorkoutSegment> = emptyList(),
    ) = PlannedWorkout(id, day, category, 5.0, Durations.ofSeconds(1800), segments, "original")

    private fun plan(rows: List<PlannedWorkout> = listOf(workout())) =
        TrainingPlan("p", date, date.plusDays(7), emptyList(), rows)

    private fun profile() =
        UserProfile(
            RaceGoal(RaceDistance.FIVE_K, "", null, GoalMode.FINISH, null),
            TrainingPreferences(
                TrainingStrategy.BALANCED,
                setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                3,
                DayOfWeek.FRIDAY,
                emptySet(),
                ScheduleType.FIXED,
                UnitSystem.METRIC,
            ),
            false,
        )

    private fun step(
        index: Int = 0,
        kind: WorkoutSegment.Kind? = WorkoutSegment.Kind.RUN,
        metres: Double = 1000.0,
        millis: Long = 330000,
        fast: Double? = 300.0,
        slow: Double? = 360.0,
        complete: Boolean = true,
        reliable: Boolean = true,
    ) = RunExecution.StepEvidence(index, kind, metres, millis, fast, slow, complete, reliable)

    private fun actual(metres: Double? = 5000.0, rpe: Double? = null, end: Instant = later(1800)) =
        ActivityRecord(
            "a",
            SourceIdentity("native", "a", null),
            at,
            end,
            null,
            null,
            metres,
            null,
            null,
            null,
            null,
            null,
            rpe,
            "",
            emptyList(),
            emptyList(),
            null,
            null,
        )

    private fun proposal(
        options: List<AdjustmentProposal.Option>,
        evidence: List<String> = emptyList(),
    ) =
        AdjustmentProposal(
            "suggestion",
            "v",
            "w",
            at,
            "reason",
            evidence,
            "impact",
            "limited",
            options,
        )

    private fun keep() =
        AdjustmentProposal.Option("keep", "Keep", AdjustmentProposal.Action.KEEP, null)

    @Test
    fun responseNeedsPreservedPlanLinkAndDoesNotInferCompletion() {
        val unknown = RunExecution(" actual ", null, null, null, null, null)
        assertEquals(" actual ", unknown.activityId())
        assertNull(unknown.completedReps())
        assertNull(unknown.recoveriesCompleted())
        assertEquals("", unknown.responseNotes())
        assertFailsWith<IllegalArgumentException> { RunExecution("a", "w", null, null, null, null) }
        assertFailsWith<IllegalArgumentException> { RunExecution("a", null, "v", null, null, null) }
        assertEquals(0, RunExecution("a", "w", "v", 0, false, "").completedReps())
    }

    @Test
    fun structuredStepsRequireUniqueIndicesAndPreservedPrescription() {
        assertFailsWith<IllegalArgumentException> {
            RunExecution("a", "w", "v", 1, null, "", listOf(step(), step()))
        }
        assertFailsWith<IllegalArgumentException> {
            RunExecution("a", null, null, null, null, "", listOf(step()))
        }
        val rows = mutableListOf(step())
        val response = RunExecution("a", "w", "v", null, null, null, rows)
        rows.clear()
        assertEquals(1, response.steps().size)
        assertFails { (response.steps() as MutableList).clear() }
        assertEquals(
            500,
            RunExecution("a", "w", "v", null, null, "", (0..499).map { step(it) }).steps().size,
        )
    }

    @Test
    fun stepEvidenceRetainsIndependentReliabilityAndGeometryBounds() {
        assertEquals(499, step(499).index())
        assertFalse(step(reliable = false).reliablePace())
        assertNull(step(fast = null).fastPace())
        for (index in listOf(-1, 500)) assertFailsWith<IllegalArgumentException> { step(index) }
        for (metres in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) assertFailsWith<
            IllegalArgumentException
        > {
            step(metres = metres)
        }
        assertFailsWith<IllegalArgumentException> { step(kind = null) }
        assertFailsWith<IllegalArgumentException> { step(millis = -1) }
        assertFailsWith<IllegalArgumentException> { step(fast = 400.0, slow = 300.0) }
        assertFailsWith<IllegalArgumentException> { step(fast = 0.0) }
    }

    @Test
    fun responseBoundariesAndRawNamesMatchExistingRecordContract() {
        assertEquals("\u2003", RunExecution("\u2003", null, null, null, null, null).activityId())
        assertFailsWith<IllegalArgumentException> {
            RunExecution(" \t", null, null, null, null, null)
        }
        for (reps in listOf(-1, 1001)) assertFailsWith<IllegalArgumentException> {
            RunExecution("a", "w", "v", reps, null, "")
        }
        assertFailsWith<IllegalArgumentException> {
            RunExecution("a", null, null, null, null, "x".repeat(10001))
        }
    }

    @Test
    fun requestSnapshotsKeepAvailabilityIndependentFromStrengthAndTimePreferences() {
        val days = mutableSetOf(date)
        val request =
            PlanRequest(profile(), RunnerBaseline.empty(at, later(1)), date, days, emptyList())
        days.clear()
        assertEquals(setOf(date), request.unavailableDates())
        assertEquals("BALANCED", request.distribution())
        assertNull(request.maximumMinutes())
        assertFails { (request.unavailableDates() as MutableSet).clear() }
        for (minutes in listOf(14, 361)) assertFailsWith<IllegalArgumentException> {
            request.copy(maximumMinutes = minutes)
        }
        assertFailsWith<IllegalArgumentException> { request.copy(distribution = "unexpected") }
    }

    @Test
    fun versionPreservesGoalEvidenceAndPlanIdentityWithoutNormalisingIdentifiers() {
        val originalPlan = plan()
        val v = PlanVersion(" version ", "p", 1, at, "", profile(), "", originalPlan)
        assertEquals(" version ", v.id())
        assertSame(originalPlan, v.plan())
        assertEquals(at, v.createdAt())
        assertFailsWith<IllegalArgumentException> { v.copy(version = 0) }
        assertFailsWith<IllegalArgumentException> { v.copy(planId = "different") }
        assertFailsWith<NullPointerException> { v.copy(evidenceSnapshot = null) }
    }

    @Test
    fun proposalMustOfferKeepAndPreserveTargetIdentityForEveryReplacement() {
        assertFailsWith<IllegalArgumentException> { proposal(emptyList()) }
        assertFailsWith<IllegalArgumentException> { proposal(listOf(keep(), keep())) }
        assertFailsWith<IllegalArgumentException> {
            proposal(
                listOf(
                    keep(),
                    AdjustmentProposal.Option(
                        "replace",
                        "Replace",
                        AdjustmentProposal.Action.REPLACE,
                        workout("other"),
                    ),
                )
            )
        }
        val reasons = mutableListOf("recorded evidence")
        val p =
            proposal(
                listOf(
                    keep(),
                    AdjustmentProposal.Option("rest", "Rest", AdjustmentProposal.Action.REST, null),
                ),
                reasons,
            )
        reasons.clear()
        assertEquals(listOf("recorded evidence"), p.evidence())
        assertFails { (p.options() as MutableList).clear() }
    }

    @Test
    fun substitutionActionsCannotBeConfusedWithRestOrKeepingTheCurrentPrescription() {
        assertFailsWith<IllegalArgumentException> {
            AdjustmentProposal.Option("r", "Replace", AdjustmentProposal.Action.REPLACE, null)
        }
        assertFailsWith<IllegalArgumentException> {
            AdjustmentProposal.Option("k", "Keep", AdjustmentProposal.Action.KEEP, workout())
        }
        val option =
            AdjustmentProposal.Option("r", "Replace", AdjustmentProposal.Action.REPLACE, workout())
        assertEquals("w", option.replacement()!!.id())
        assertNull(keep().replacement())
    }

    @Test
    fun explicitMoveAndSkipDecisionsHaveDifferentDestinationRequirements() {
        val moved =
            WorkoutDecision("d", "w", "v", date, date.plusDays(1), WorkoutDecision.Kind.MOVED, at)
        assertEquals(date, moved.originalDate())
        assertEquals(date.plusDays(1), moved.movedTo())
        assertFailsWith<IllegalArgumentException> { moved.copy(movedTo = null) }
        assertFailsWith<IllegalArgumentException> {
            moved.copy(kind = WorkoutDecision.Kind.SKIPPED)
        }
        assertNull(
            WorkoutDecision("d", "w", "v", date, null, WorkoutDecision.Kind.SKIPPED, at).movedTo()
        )
    }

    @Test
    fun rebuildRetainsExactCompletedPrescriptionAndRejectsConflictingDates() {
        val original = workout(category = WorkoutCategory.RECOVERY_RUN)
        val replacement = workout()
        val candidate = plan(listOf(replacement))
        val updated = PlanPreservation.retainCompleted(candidate, mapOf("w" to original))
        assertSame(original, updated.workouts().single())
        assertSame(replacement, candidate.workouts().single())
        assertFailsWith<IllegalArgumentException> {
            PlanPreservation.retainCompleted(plan(), mapOf("w" to workout("different")))
        }
        assertFailsWith<IllegalArgumentException> {
            PlanPreservation.retainCompleted(
                plan(listOf(workout(), workout("second", date.plusDays(1)))),
                mapOf("second" to workout("second")),
            )
        }
        assertEquals(
            1,
            PlanPreservation.retainCompleted(plan(), mapOf("old" to workout("old"))).workouts().size,
        )
    }

    @Test
    fun loadWindowUsesRunEndButStrengthStartAndRetainsUnknownEffort() {
        val r = actual(rpe = null)
        val run = TrainingLoadEvidence.window(listOf(r), emptyList(), r.end(), later(3600))
        assertEquals(1, run.runsObserved())
        assertEquals(0, run.runsWithRpe())
        assertNull(run.runningLoad())
        val zero =
            TrainingLoadEvidence.window(listOf(actual(rpe = 0.0)), emptyList(), at, later(3600))
        assertEquals(0.0, zero.runningLoad())
        assertEquals(1, zero.runsWithRpe())
        assertEquals(
            0,
            TrainingLoadEvidence.window(listOf(r), emptyList(), at, r.end()).runsObserved(),
        )
        assertFailsWith<IllegalArgumentException> {
            TrainingLoadEvidence.window(emptyList(), emptyList(), at, at)
        }
    }

    @Test
    fun strengthLoadNeedsRecordedEffortAndUsesActualSessionDuration() {
        val exercise =
            StrengthExercise(
                "test",
                "Test",
                StrengthExercise.Region.UPPER,
                StrengthExercise.Mode.WEIGHTED,
                true,
            )
        val recorded =
            listOf(
                StrengthSession.ExerciseSets(
                    exercise,
                    listOf(StrengthSession.SetEntry("set", 8, 20.0, null, false)),
                )
            )
        val known =
            StrengthSession(
                "s",
                at,
                Offsets.ofTotalSeconds(0),
                Durations.ofMinutes(30),
                6.0,
                "",
                recorded,
            )
        val unknown =
            StrengthSession(
                "u",
                at,
                Offsets.ofTotalSeconds(0),
                Durations.ofMinutes(30),
                null,
                "",
                recorded,
            )
        val load = TrainingLoadEvidence.window(emptyList(), listOf(known, unknown), at, later(1))
        assertEquals(2, load.strengthObserved())
        assertEquals(1, load.strengthWithRpe())
        assertEquals(180.0, load.strengthLoad())
        assertNull(load.runningLoad())
    }

    @Test
    fun completionAloneCannotManufactureAnExecutionScore() {
        val feedback = RunScoreEngine.analyse(workout(), actual(), null)
        assertNull(feedback.score())
        assertEquals("Execution evidence incomplete", feedback.label())
        assertNull(feedback.components().first { it.name() == "Effort match" }.fraction())
        assertTrue(feedback.learned()!!.contains("excluded, not scored as zero"))
    }

    @Test
    fun unplannedRunCannotBeScoredAgainstAnInventedPrescription() {
        val feedback = RunScoreEngine.analyse(null, null, null)
        assertNull(feedback.score())
        assertEquals("Run Analysis", feedback.label())
        assertTrue(feedback.components().isEmpty())
        assertTrue(feedback.learned()!!.contains("no planned workout"))
    }

    @Test
    fun intervalAverageDoesNotProveRepPaceAndIncompleteRepsCapTheScore() {
        val parts =
            listOf(
                WorkoutSegment(WorkoutSegment.Kind.RUN, 1.0, null, 300.0, 360.0, null, null, null),
                WorkoutSegment(
                    WorkoutSegment.Kind.RECOVERY,
                    1.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
                WorkoutSegment(WorkoutSegment.Kind.RUN, 1.0, null, 300.0, 360.0, null, null, null),
            )
        val p = workout(category = WorkoutCategory.INTERVALS, segments = parts)
        val unknown = RunScoreEngine.analyse(p, actual(rpe = 6.0), null)
        assertNull(unknown.components().first { it.name() == "Target pace" }.fraction())
        assertNull(unknown.components().first { it.name() == "Reps completed" }.fraction())
        val r = RunExecution("a", "w", "v", 1, false, "", listOf(step()))
        val measured = RunScoreEngine.analyse(p, actual(rpe = 6.0), r)
        assertEquals(1.0, measured.components().first { it.name() == "Target pace" }.fraction())
        assertEquals(.5, measured.components().first { it.name() == "Reps completed" }.fraction())
        assertTrue(measured.score()!! <= 7)
    }

    @Test
    fun unreliableOrIncompleteStepsNeverCreatePaceAdherence() {
        val parts =
            listOf(
                WorkoutSegment(WorkoutSegment.Kind.RUN, 1.0, null, 300.0, 360.0, null, null, null),
                WorkoutSegment(
                    WorkoutSegment.Kind.RECOVERY,
                    1.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
            )
        val p = workout(category = WorkoutCategory.INTERVALS, segments = parts)
        for (e in
            listOf(
                step(reliable = false),
                step(complete = false),
                step(metres = 0.0),
                step(millis = 0),
                step(fast = null),
            )) {
            val r = RunExecution("a", "w", "v", null, null, "", listOf(e))
            assertNull(
                RunScoreEngine.analyse(p, actual(), r)
                    .components()
                    .first { it.name() == "Target pace" }
                    .fraction()
            )
        }
        assertFailsWith<IllegalArgumentException> {
            RunScoreEngine.analyse(p, actual(), RunExecution("a", "other", "v", null, null, ""))
        }
    }

    @Test
    fun highReportedEffortChangesContextWithoutClaimingPhysiologicalImprovement() {
        val feedback = RunScoreEngine.analyse(workout(), actual(rpe = 10.0), null)
        assertTrue(feedback.differed().any { it.contains("reported effort") })
        assertTrue(
            feedback.learned()!!.contains("Completion alone does not prove fitness improved")
        )
        assertTrue(feedback.nextStep()!!.contains("your choice"))
        assertFails { (feedback.components() as MutableList).clear() }
    }
}
