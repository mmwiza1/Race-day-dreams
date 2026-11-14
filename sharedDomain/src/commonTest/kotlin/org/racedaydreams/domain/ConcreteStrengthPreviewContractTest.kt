package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class ConcreteStrengthPreviewContractTest {
    private val start = CalendarDates.of(2026, 11, 9)
    private val at = Instants.ofEpochSecond(1794218400)
    private val later = Instants.ofEpochSecond(at.getEpochSecond() + 10)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()
    private val settings =
        StrengthProgramme.Settings(
            GymProgramme.Goal.STRENGTH,
            GymProgramme.Priority.BALANCED,
            GymProgramme.Experience.INTERMEDIATE,
            DayOfWeek.entries.toSet(),
            2,
            60,
            ExerciseMetadata.Equipment.entries.toSet(),
        )

    private fun base(): StrengthProgramme {
        val blocks =
            (0..1).map { phase ->
                val workouts =
                    (0..1).map { day ->
                        val id = "workout-$phase-$day"
                        val target =
                            GymWorkoutDefinition.Target(
                                2 + phase,
                                4 + phase,
                                6 + phase,
                                null,
                                null,
                                GymWorkoutDefinition.EffortRange(
                                    GymWorkoutDefinition.EffortScale.RIR,
                                    2.5,
                                    4.0,
                                ),
                                90 + phase * 30,
                                80.0 + phase,
                            )
                        val slots = listOf(GymWorkoutDefinition.Slot("bench", bench, target, null))
                        StrengthProgramme.Workout(
                            id,
                            start.plusDays(phase * 7L + day * 2L),
                            StrengthWorkoutBudget.workoutMinutes(slots),
                            GymWorkoutDefinition(id, 1, at, "Workout $phase $day", slots),
                            "Keep this explanation",
                        )
                    }
                StrengthProgramme.Block(
                    "block-$phase",
                    if (phase == 0) StrengthProgramme.Phase.FOUNDATION
                    else StrengthProgramme.Phase.DEVELOPMENT,
                    "Phase $phase",
                    start.plusDays(phase * 7L),
                    1,
                    workouts,
                    "Distinct phase targets",
                )
            }
        return StrengthProgramme(
            "programme",
            1,
            0,
            at,
            "My programme",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            blocks,
            start,
            "Athlete choice",
            listOf("Original trade-off"),
        )
    }

    private fun revision(base: StrengthProgramme): ConcreteStrengthProgrammeDraft {
        val proposed =
            StrengthProgrammeEditor.replaceFuture(
                    base,
                    base,
                    start.plusDays(1),
                    later,
                    setOf("workout-0-0"),
                    "Future review",
                )
                .proposed()
        return ConcreteStrengthProgrammeDraft(
            "preview",
            1,
            StrengthProgrammingRepository.Reference(
                StrengthProgrammingRepository.Owner.AUTHORED,
                base.id(),
                base.revision(),
            ),
            proposed,
            emptyList(),
        )
    }

    private fun context(exercise: StrengthExercise = bench, excluded: Set<String> = emptySet()) =
        ExerciseSubstitutionEngine.Context(
            exercise,
            settings.goal(),
            settings.experience(),
            settings.equipment(),
            excluded,
            emptySet(),
            emptySet(),
            ExerciseSubstitutionEngine.Reason.ALTERNATIVES,
        )

    private fun swap(
        draft: ConcreteStrengthProgrammeDraft,
        base: StrengthProgramme,
        replacement: String = "dumbbell-bench-press",
        all: Boolean = false,
        protected: Set<String> = setOf("workout-0-0"),
        changed: GymWorkoutDefinition.Target? = null,
        context: ExerciseSubstitutionEngine.Context = context(),
        workout: String = "workout-0-1",
    ) =
        ConcreteStrengthProgrammeEditor.substitute(
            draft,
            base,
            workout,
            "bench",
            context,
            replacement,
            changed,
            protected,
            all,
            later,
        )

    @Test
    fun previewIsFrozenAndSavingEditsNeverCreatesActualsOrAcceptedRevisions() {
        val base = base()
        val mutable = mutableListOf<StrengthProgrammeEditor.Change>()
        val draft = ConcreteStrengthProgrammeDraft("new", 1, null, base, mutable)
        mutable.add(StrengthProgrammeEditor.Change("w", "s", "old", "new", "Choice"))
        assertTrue(draft.substitutions().isEmpty())
        val workout = base.workouts()[1]
        val editedWorkout = workout.copy(notes = "My reviewed guidance")
        val edited = ConcreteStrengthProgrammeEditor.replaceWorkout(draft, editedWorkout)
        assertEquals(2, edited.revision())
        assertEquals(1, edited.programme().revision())
        assertNull(edited.before())
        assertEquals(editedWorkout, edited.programme().workouts()[1])
        assertEquals(
            base.workouts().filterNot { it.id() == workout.id() },
            edited.programme().workouts().filterNot { it.id() == workout.id() },
        )
        assertEquals(base.tradeoffs(), edited.programme().tradeoffs())
        assertEquals(base, draft.programme())
        assertFails { (draft.substitutions() as MutableList).add(mutable.single()) }
    }

    @Test
    fun editingCannotMoveDatesLoseLineageBackdateOrWrapRevision() {
        val p = base()
        val d = ConcreteStrengthProgrammeDraft("new", 1, null, p, emptyList())
        assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthProgrammeEditor.replaceWorkout(
                d,
                p.workouts()[0].copy(date = start.plusDays(1)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            d.withProgramme(p.copy(id = "another"), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            d.withProgramme(
                p.copy(createdAt = Instants.ofEpochSecond(at.getEpochSecond() - 1)),
                emptyList(),
            )
        }
        assertFailsWith<ArithmeticException> {
            d.copy(revision = Int.MAX_VALUE).withProgramme(p, emptyList())
        }
        assertFailsWith<IllegalArgumentException> { d.copy(revision = 0) }
    }

    @Test
    fun revisionPreviewRequiresItsExactBaseAndCompleteTargets() {
        val p = base()
        val d = revision(p)
        d.validateAgainst(p)
        assertFailsWith<IllegalArgumentException> { d.copy(before = null) }
        assertFailsWith<IllegalArgumentException> { d.validateAgainst(p.copy(id = "different")) }
        assertFailsWith<IllegalArgumentException> { d.copy(programme = p) }
        val w = p.workouts()[0]
        val block =
            p.blocks()[0].copy(
                workouts =
                    listOf(
                        w.copy(
                            definition =
                                w.definition()
                                    .copy(
                                        slots =
                                            listOf(w.definition().slots()[0].copy(target = null))
                                    )
                        )
                    ) + p.blocks()[0].workouts().drop(1)
            )
        assertFailsWith<IllegalArgumentException> {
            ConcreteStrengthProgrammeDraft(
                "open",
                1,
                null,
                p.copy(blocks = listOf(block) + p.blocks().drop(1)),
                emptyList(),
            )
        }
    }

    @Test
    fun allFutureSubstitutionsRetainPhaseTargetsClearReferencesAndProtectPast() {
        val p = base()
        val d = revision(p)
        val result = swap(d, p, all = true)
        assertEquals(2, result.revision())
        assertEquals(2, result.programme().revision())
        assertEquals(p.workouts()[0], result.programme().workouts()[0])
        assertEquals(3, result.substitutions().size)
        for (w in result.programme().workouts().drop(1)) {
            val original = d.programme().workouts().first { it.id() == w.id() }
            assertEquals(
                original.definition().slots()[0].target()!!.withoutLoadReference(),
                w.definition().slots()[0].target(),
            )
            assertEquals("dumbbell-bench-press", w.definition().slots()[0].exercise().id())
            assertEquals(original.definition().revision() + 1, w.definition().revision())
            assertEquals(original.notes(), w.notes())
        }
        result.validateAgainst(p)
        StrengthProgrammeEditor.Proposal(
            p,
            result.programme(),
            setOf("workout-0-0"),
            result.substitutions(),
        )
        assertTrue(d.substitutions().isEmpty())
    }

    @Test
    fun changingAndReversingPendingSwapRetainsOriginalProvenance() {
        val p = base()
        val first = swap(revision(p), p)
        val current = first.programme().workouts()[1].definition().slots()[0].exercise()
        assertTrue(ConcreteStrengthProgrammeEditor.retainedIntent(first, p, "workout-0-1", "bench"))
        val reversed = swap(first, p, replacement = "bench-press", context = context(current))
        assertTrue(reversed.substitutions().isEmpty())
        assertEquals(
            "bench-press",
            reversed.programme().workouts()[1].definition().slots()[0].exercise().id(),
        )
        assertNull(
            reversed.programme().workouts()[1].definition().slots()[0].target()!!.referenceLoadKg()
        )
        assertFalse(ConcreteStrengthProgrammeEditor.retainedIntent(first, p, "missing", "bench"))
        assertFailsWith<IllegalArgumentException> {
            first.copy(substitutions = first.substitutions() + first.substitutions())
        }
    }

    @Test
    fun protectedExcludedWrongContextOrUnavailableWorkCannotBeSwapped() {
        val p = base()
        val d = revision(p)
        assertFailsWith<IllegalArgumentException> { swap(d, p, workout = "workout-0-0") }
        assertFailsWith<IllegalArgumentException> {
            swap(d, p, protected = p.workouts().map { it.id() }.toSet())
        }
        assertFailsWith<IllegalArgumentException> {
            swap(d, p, context = context(excluded = setOf("dumbbell-bench-press")))
        }
        assertFailsWith<IllegalArgumentException> {
            swap(d, p, context = context().copy(goal = GymProgramme.Goal.MUSCLE))
        }
        assertFailsWith<IllegalArgumentException> { swap(d, p, workout = "missing") }
        assertFailsWith<IllegalArgumentException> { swap(d, p, replacement = "deadlift") }
    }

    @Test
    fun changedMeasurementRequiresFreshIndividualTaskAndNeverCopiesWeight() {
        val p = base()
        val d = revision(p)
        assertFailsWith<IllegalArgumentException> { swap(d, p, replacement = "push-up") }
        val reps = GymWorkoutDefinition.Target(2, 8, 12, null, null, null, 60, null)
        assertFailsWith<IllegalArgumentException> {
            swap(d, p, replacement = "push-up", changed = reps, all = true)
        }
        val result = swap(d, p, replacement = "push-up", changed = reps)
        assertEquals(reps, result.programme().workouts()[1].definition().slots()[0].target())
        assertNull(
            result.programme().workouts()[1].definition().slots()[0].target()!!.referenceLoadKg()
        )
        assertEquals(d.programme().workouts().drop(2), result.programme().workouts().drop(2))
    }

    @Test
    fun summaryRecordsRetainUnknownsWithoutInventingAvailabilityOrDates() {
        val summary =
            ConcreteStrengthProgrammeDraftRepository.Summary(
                null,
                0,
                null,
                0,
                null,
                null,
                null,
                false,
            )
        assertNull(summary.id())
        assertNull(summary.start())
        assertFalse(summary.revisionExists())
        assertEquals(
            "Summary[id=null, revision=0, programmeId=null, programmeRevision=0, name=null, start=null, end=null, revisionExists=false]",
            summary.toString(),
        )
    }
}
