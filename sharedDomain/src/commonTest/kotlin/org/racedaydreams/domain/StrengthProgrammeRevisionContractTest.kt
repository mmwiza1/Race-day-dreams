package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class StrengthProgrammeRevisionContractTest {
    private val now = Instants.ofEpochSecond(1794218400)
    private val start = CalendarDates.of(2026, 11, 9)
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun programme(id: String = "programme", candidate: Boolean = false): StrengthProgramme {
        val target =
            GymWorkoutDefinition.Target(
                2,
                4,
                6,
                null,
                null,
                GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 6.0, 8.0),
                90,
                80.0,
            )
        val slots = listOf(GymWorkoutDefinition.Slot("bench", bench, target, null))
        val workouts =
            (0..1).map { n ->
                StrengthProgramme.Workout(
                    if (n == 0) "protected" else "future",
                    start.plusDays(if (candidate) n + 3L else n * 2L),
                    StrengthWorkoutBudget.workoutMinutes(slots),
                    GymWorkoutDefinition("definition-$n", 1, now, "Workout $n", slots),
                    null,
                )
            }
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                DayOfWeek.entries.toSet(),
                2,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        return StrengthProgramme(
            id,
            1,
            0,
            now,
            "Programme",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.FOUNDATION,
                    "Foundation",
                    start,
                    1,
                    workouts,
                    "Retained block",
                )
            ),
            start,
            "Athlete chose this programme",
            emptyList(),
        )
    }

    private fun at() = Instants.ofEpochSecond(now.getEpochSecond() + 30)

    private fun context(p: StrengthProgramme, excluded: Set<String> = emptySet()) =
        ExerciseSubstitutionEngine.Context(
            bench,
            p.settings().goal(),
            p.settings().experience(),
            p.settings().equipment(),
            excluded,
            emptySet(),
            emptySet(),
            ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY,
        )

    private fun regenerate(
        p: StrengthProgramme = programme(),
        candidate: StrengthProgramme = programme(candidate = true),
    ) =
        StrengthProgrammeEditor.replaceFuture(
            p,
            candidate,
            start.plusDays(1),
            at(),
            setOf("protected"),
            "Availability changed",
        )

    @Test
    fun regeneratedProtectedIdentitiesRetainExactOriginalJavaUuidSemantics() {
        val examples =
            listOf(
                "programme" to "regenerated-4ff577f2-3744-32aa-b40f-dd6519046ed8",
                "équipe" to "regenerated-8ff8c629-95d8-3864-9d4b-bc5ac92af86a",
                "old\uD800id" to "regenerated-11fbe2a8-d401-3391-a8b1-3d04479ac311",
            )
        for ((id, expected) in examples) {
            val previous = programme(id)
            val candidate = programme(id, true)
            val before = previous.toString()
            val result = regenerate(previous, candidate)
            assertEquals(2, result.proposed().revision())
            assertEquals(1, result.proposed().parentRevision())
            assertEquals(previous.workouts()[0], result.proposed().workouts().first())
            val renamed = result.proposed().workouts().first { it.date() == start.plusDays(3) }
            assertEquals(expected, renamed.id())
            assertEquals(expected, renamed.definition().id())
            assertEquals(1, renamed.definition().revision())
            assertEquals(
                2,
                result.proposed().workouts().first { it.id() == "future" }.definition().revision(),
            )
            assertEquals(before, previous.toString())
            assertEquals(1, candidate.revision())
        }
    }

    @Test
    fun deterministicRegenerationAvoidsReservedCandidateIdsWithoutLosingHistory() {
        val base = programme(candidate = true)
        val reserved = "regenerated-4ff577f2-3744-32aa-b40f-dd6519046ed8"
        val block = base.blocks().single()
        val altered =
            base.copy(
                blocks =
                    listOf(
                        block.copy(
                            workouts =
                                listOf(block.workouts()[0], block.workouts()[1].copy(id = reserved))
                        )
                    )
            )
        val result = regenerate(candidate = altered).proposed()
        assertEquals(3, result.workouts().size)
        assertEquals(3, result.workouts().map { it.id() }.toSet().size)
        assertEquals("protected", result.workouts().first().id())
        assertNotEquals(reserved, result.workouts().first { it.date() == start.plusDays(3) }.id())
        assertEquals(reserved, result.workouts().last().id())
    }

    @Test
    fun completedOrHistoricalTargetsAndUnknownProtectedIdsCannotBeRewritten() {
        val previous = programme()
        val valid = regenerate(previous).proposed()
        val block = valid.blocks().single()
        val old = block.workouts().first()
        val slot = old.definition().slots().single()
        val corrupted =
            old.copy(
                definition =
                    old.definition()
                        .copy(
                            slots =
                                listOf(
                                    slot.copy(target = slot.target()!!.copy(referenceLoadKg = 60.0))
                                )
                        )
            )
        val edited =
            valid.copy(
                blocks = listOf(block.copy(workouts = listOf(corrupted) + block.workouts().drop(1)))
            )
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.Proposal(previous, edited, setOf("protected"), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.Proposal(previous, valid, setOf("missing"), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.Proposal(
                previous,
                valid.copy(revision = 3, parentRevision = 2),
                emptySet(),
                emptyList(),
            )
        }
        assertEquals(
            80.0,
            previous.workouts().first().definition().slots().single().target()!!.referenceLoadKg(),
        )
    }

    @Test
    fun futureAvailabilityIsRevalidatedWithoutMovingOrDroppingRetainedWork() {
        val candidate = programme(candidate = true)
        val unavailable =
            candidate.copy(
                settings = candidate.settings().copy(days = setOf(DayOfWeek.MONDAY), frequency = 1)
            )
        assertFailsWith<IllegalArgumentException> { regenerate(candidate = unavailable) }
        val result = regenerate()
        assertEquals(start.plusDays(1), result.proposed().effectiveFrom())
        assertEquals(programme().workouts().first(), result.proposed().workouts().first())
        assertEquals(setOf("protected"), result.completedWorkoutIds())
    }

    @Test
    fun permanentSubstitutionIsOnlyAForwardProposalAndClearsReferenceLoading() {
        val previous = programme()
        val before = previous.toString()
        val result =
            StrengthProgrammeEditor.substitute(
                previous,
                "future",
                "bench",
                context(previous),
                "dumbbell-bench-press",
                null,
                start,
                at(),
                setOf("protected"),
            )
        val changed = result.proposed().workouts().last().definition().slots().single()
        assertEquals("dumbbell-bench-press", changed.exercise().id())
        assertNull(changed.target()!!.referenceLoadKg())
        assertEquals(2, changed.target()!!.sets())
        assertEquals(1, result.substitutions().size)
        assertEquals("bench-press", result.substitutions().single().fromExerciseId())
        assertEquals(before, previous.toString())
        assertEquals(previous.workouts().first(), result.proposed().workouts().first())
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.substitute(
                previous,
                "protected",
                "bench",
                context(previous),
                "dumbbell-bench-press",
                null,
                start,
                at(),
                setOf("protected"),
            )
        }
    }

    @Test
    fun exclusionContextAndChangedMeasurementNeedExplicitReviewedChoices() {
        val p = programme()
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.substitute(
                p,
                "future",
                "bench",
                context(p, setOf("dumbbell-bench-press")),
                "dumbbell-bench-press",
                null,
                start,
                at(),
                emptySet(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.substitute(
                p,
                "future",
                "bench",
                context(p).copy(goal = GymProgramme.Goal.POWER),
                "dumbbell-bench-press",
                null,
                start,
                at(),
                emptySet(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammeEditor.substitute(
                p,
                "future",
                "bench",
                context(p),
                "push-up",
                null,
                start,
                at(),
                emptySet(),
            )
        }
        val task = GymWorkoutDefinition.Target(2, 8, 12, null, null, null, 90, null)
        val proposed =
            StrengthProgrammeEditor.substitute(
                p,
                "future",
                "bench",
                context(p),
                "push-up",
                task,
                start,
                at(),
                emptySet(),
            )
        assertEquals(
            task,
            proposed.proposed().workouts().last().definition().slots().single().target(),
        )
    }

    @Test
    fun repositoryDecisionsValidateForwardOwnersAndFreezeProtectionEvidence() {
        val before =
            StrengthProgrammingRepository.Reference(
                StrengthProgrammingRepository.Owner.AUTHORED,
                "programme",
                1,
            )
        val after =
            StrengthProgrammingRepository.Reference(
                StrengthProgrammingRepository.Owner.AUTHORED,
                "programme",
                2,
            )
        val protected = mutableListOf("protected")
        val changes =
            mutableListOf(
                StrengthProgrammeEditor.Change(
                    "future",
                    "bench",
                    "bench-press",
                    "dumbbell-bench-press",
                    "Athlete choice",
                )
            )
        val decision =
            StrengthProgrammingRepository.Decision(
                "decision",
                before,
                after,
                at(),
                "  Keep original reason spacing  ",
                protected,
                changes,
            )
        protected.clear()
        changes.clear()
        assertEquals(listOf("protected"), decision.protectedWorkoutIds())
        assertEquals(1, decision.substitutions().size)
        assertEquals("  Keep original reason spacing  ", decision.reason())
        assertEquals("programme:v1", before.key())
        assertFailsWith<IllegalArgumentException> {
            decision.copy(after = after.copy(revision = 3))
        }
        assertFailsWith<IllegalArgumentException> {
            decision.copy(protectedWorkoutIds = listOf("protected", "protected"))
        }
        assertFailsWith<IllegalArgumentException> { decision.copy(before = null, after = null) }
        assertFailsWith<IllegalArgumentException> {
            decision.copy(after = after.copy(owner = StrengthProgrammingRepository.Owner.LEGACY))
        }
        assertFails { (decision.protectedWorkoutIds() as MutableList<String>).clear() }
    }

    @Test
    fun revisionReviewSnapshotsKnownCompletionsAndStopPreservesHistoricalReference() {
        val p = programme()
        val ids = mutableSetOf("protected")
        val review = StrengthProgrammingRepository.RevisionReview(p, ids)
        ids.clear()
        assertEquals(setOf("protected"), review.completedWorkoutIds())
        assertEquals(StrengthProgrammingRepository.Owner.AUTHORED, review.reference().owner())
        assertFailsWith<IllegalArgumentException> {
            StrengthProgrammingRepository.RevisionReview(p, setOf("unknown"))
        }
        val stop =
            StrengthProgrammingRepository.Decision(
                "stop",
                review.reference(),
                null,
                at(),
                "Keep all history",
                emptyList(),
                emptyList(),
            )
        assertNull(stop.after())
        assertEquals(review.reference(), stop.before())
        assertEquals(p, review.programme())
    }
}
