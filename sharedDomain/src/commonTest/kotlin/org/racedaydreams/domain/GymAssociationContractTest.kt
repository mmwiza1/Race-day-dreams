package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class GymAssociationContractTest {
    private val now = Instants.ofEpochSecond(1793188800, 500000000)
    private val day = CalendarDates.atOffset(now, Offsets.ofTotalSeconds(0))
    private val bench = ExerciseCatalog.definition("bench-press")!!.exercise()

    private fun actual(
        warmUp: Boolean = false,
        offset: ZoneOffset = Offsets.ofTotalSeconds(0),
        duration: Duration = Durations.ofMinutes(30),
    ) =
        StrengthSession(
            "actual",
            Instants.ofEpochSecond(now.getEpochSecond() - 3600, now.getNano().toLong()),
            offset,
            duration,
            null,
            "",
            listOf(
                StrengthSession.ExerciseSets(
                    bench,
                    listOf(StrengthSession.SetEntry("set", 6, 60.0, 7.0, warmUp)),
                )
            ),
        )

    private fun legacy(delta: Long = 0, revision: Int = 1): GymProgramme {
        val date = day.plusDays(delta)
        return GymProgramme(
            "programme",
            revision,
            Instants.ofEpochSecond(now.getEpochSecond() - 86400),
            "Retained",
            GymProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(date.getDayOfWeek()),
                1,
                setOf("full gym"),
                45,
                GymProgramme.Effort.RIR,
            ),
            date,
            date.plusDays(41),
            listOf(
                GymProgramme.Session(
                    "planned",
                    date,
                    "Upper",
                    listOf(
                        GymProgramme.Prescription(
                            "bench-press",
                            3,
                            5,
                            8,
                            null,
                            null,
                            6,
                            8,
                            120,
                            "Original",
                            80.0,
                        )
                    ),
                    "Retained",
                )
            ),
            emptyList(),
        )
    }

    private fun proposal(actual: StrengthSession = actual(), programme: GymProgramme = legacy()) =
        GymWorkoutAssociation.proposals(
                actual,
                GymExecution.Origin.FREE,
                programme,
                emptySet(),
                now,
            )
            .single()

    @Test
    fun discoveryUsesStoredLocalDateWorkingTaskAndExplicitOccupancy() {
        val recorded = actual()
        for (delta in -4L..4L) {
            val programme = legacy(delta)
            val choices =
                GymWorkoutAssociation.proposals(
                    recorded,
                    GymExecution.Origin.FREE,
                    programme,
                    emptySet(),
                    now,
                )
            assertEquals(if (delta in -3L..3L) 1 else 0, choices.size)
            if (choices.isNotEmpty()) {
                assertSame(recorded, choices.single().actual())
                assertEquals(programme.sessions().single(), choices.single().prescription())
                GymWorkoutAssociation.validateForAcceptance(
                    choices.single(),
                    actual(),
                    GymExecution.Origin.FREE,
                    programme,
                    emptySet(),
                    false,
                )
            }
        }
        assertTrue(
            GymWorkoutAssociation.proposals(
                    actual(true),
                    GymExecution.Origin.FREE,
                    legacy(),
                    emptySet(),
                    now,
                )
                .isEmpty()
        )
        assertTrue(
            GymWorkoutAssociation.proposals(
                    actual(),
                    GymExecution.Origin.PLANNED,
                    legacy(),
                    emptySet(),
                    now,
                )
                .isEmpty()
        )
        assertTrue(
            GymWorkoutAssociation.proposals(
                    actual(),
                    GymExecution.Origin.FREE,
                    legacy(),
                    setOf("planned"),
                    now,
                )
                .isEmpty()
        )
        val afterMidnight = actual(offset = Offsets.ofTotalSeconds(14 * 3600))
        val storedDay = CalendarDates.atOffset(afterMidnight.startedAt(), afterMidnight.offset())
        assertEquals(day.plusDays(1), storedDay)
        assertEquals(
            1,
            GymWorkoutAssociation.proposals(
                    afterMidnight,
                    GymExecution.Origin.FREE,
                    legacy(4),
                    emptySet(),
                    now,
                )
                .size,
        )
    }

    @Test
    fun futureCompletionAndChangedEvidenceCannotBeAssociated() {
        assertTrue(
            GymWorkoutAssociation.proposals(
                    actual(duration = Durations.ofHours(2)),
                    GymExecution.Origin.FREE,
                    legacy(),
                    emptySet(),
                    now,
                )
                .isEmpty()
        )
        val actual = actual()
        val programme = legacy()
        val p = proposal(actual, programme)
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateForAcceptance(
                p,
                actual,
                GymExecution.Origin.FREE,
                legacy(revision = 2),
                emptySet(),
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateForAcceptance(
                p,
                actual,
                GymExecution.Origin.FREE,
                programme,
                setOf("planned"),
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateForAcceptance(
                p,
                actual,
                GymExecution.Origin.FREE,
                programme,
                emptySet(),
                true,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateRetainedEvidence(
                p,
                actual.copy(notes = "Changed"),
                GymExecution.Origin.FREE,
                programme,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.Decision(
                "decision",
                p,
                GymWorkoutAssociation.Choice.KEEP_EXTRA,
                Instants.ofEpochSecond(now.getEpochSecond(), 499999999),
            )
        }
    }

    @Test
    fun frozenLegacySourceTargetsIncludeExactRirAndReferenceValues() {
        val programme = legacy()
        val prepared = GymWorkoutAdapter.fromProgramme(programme, "planned")
        assertEquals(
            programme.sessions().single(),
            GymWorkoutSourceValidator.programme(prepared.definition(), prepared.source(), programme),
        )
        val slot = prepared.definition().slots().single()
        assertEquals(GymWorkoutDefinition.EffortScale.RIR, slot.target()!!.effort()!!.scale())
        assertEquals(2.0, slot.target()!!.effort()!!.minimum())
        assertEquals(80.0, slot.target()!!.referenceLoadKg())
        val altered =
            prepared
                .definition()
                .copy(slots = listOf(slot.copy(target = slot.target()!!.withoutLoadReference())))
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.programme(altered, prepared.source(), programme)
        }
        val renamed =
            prepared
                .definition()
                .copy(slots = listOf(slot.copy(exercise = bench.copy(name = "Old recorded name"))))
        // Legacy source stored IDs and targets, not names; do not reconstruct it from today's
        // library.
        assertEquals(
            programme.sessions().single(),
            GymWorkoutSourceValidator.programme(renamed, prepared.source(), programme),
        )
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.programme(
                prepared.definition(),
                prepared.source(),
                legacy(revision = 2),
            )
        }
    }

    @Test
    fun repeatsValidateExactSourceWithoutInventingTargetsOrActuals() {
        val actual = actual()
        val prepared = GymWorkoutAdapter.repeatActual("repeat", 1, now, "Repeat", actual)
        GymWorkoutSourceValidator.repeatedActual(prepared.definition(), prepared.source(), actual)
        assertTrue(prepared.definition().slots().all { it.target() == null })
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.repeatedActual(
                prepared.definition(),
                prepared.source().copy(id = "different"),
                actual,
            )
        }
        val source =
            GymExecution.SourceReference(
                GymExecution.SourceKind.SAVED_WORKOUT,
                prepared.definition().id(),
                prepared.definition().revision(),
                null,
            )
        GymWorkoutSourceValidator.savedWorkout(prepared.definition(), source, prepared.definition())
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.savedWorkout(
                prepared.definition().copy(title = "Changed"),
                source,
                prepared.definition(),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.savedWorkout(
                prepared.definition(),
                prepared.source(),
                prepared.definition(),
            )
        }
    }

    private fun authored(open: Boolean = false): StrengthProgramme {
        val target =
            if (open) null
            else
                GymWorkoutDefinition.Target(
                    3,
                    5,
                    8,
                    null,
                    null,
                    GymWorkoutDefinition.EffortRange(
                        GymWorkoutDefinition.EffortScale.RIR,
                        1.5,
                        3.5,
                    ),
                    null,
                    80.0,
                )
        val slots = listOf(GymWorkoutDefinition.Slot("bench", bench, target, null))
        val workout =
            StrengthProgramme.Workout(
                "authored-session",
                day,
                // A raw retained open slot can have an athlete-entered duration; never calculate
                // it.
                if (open) 30 else StrengthWorkoutBudget.workoutMinutes(slots),
                GymWorkoutDefinition("authored-workout", 1, now, "Authored", slots),
                null,
            )
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(day.getDayOfWeek()),
                1,
                45,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        return StrengthProgramme(
            "authored",
            1,
            0,
            now,
            "Authored",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.FOUNDATION,
                    "Foundation",
                    day,
                    1,
                    listOf(workout),
                    "Retained",
                )
            ),
            day,
            "Athlete chose this programme",
            emptyList(),
        )
    }

    @Test
    fun authoredSourcesRetainFractionalTargetsAndExactFrozenExerciseDefinitions() {
        val programme = authored()
        val prepared = GymWorkoutAdapter.fromAuthoredProgramme(programme, "authored-session")
        assertEquals(
            programme.workouts().single(),
            GymWorkoutSourceValidator.authoredProgramme(
                prepared.definition(),
                prepared.source(),
                programme,
            ),
        )
        val slot = prepared.definition().slots().single()
        assertEquals(1.5, slot.target()!!.effort()!!.minimum())
        assertEquals(3.5, slot.target()!!.effort()!!.maximum())
        val changed =
            prepared
                .definition()
                .copy(slots = listOf(slot.copy(exercise = bench.copy(name = "Renamed"))))
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutSourceValidator.authoredProgramme(changed, prepared.source(), programme)
        }
        val proposal =
            GymWorkoutAssociation.authoredProposals(
                    actual(),
                    GymExecution.Origin.FREE,
                    programme,
                    emptySet(),
                    now,
                )
                .single()
        assertTrue(proposal.authored())
        assertNull(proposal.prescription())
        assertEquals(programme.workouts().single(), proposal.authoredPrescription())
        GymWorkoutAssociation.validateAuthoredForAcceptance(
            proposal,
            actual(),
            GymExecution.Origin.FREE,
            programme,
            emptySet(),
            false,
        )
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateAuthoredForAcceptance(
                proposal,
                actual(),
                GymExecution.Origin.FREE,
                programme,
                setOf("authored-session"),
                false,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GymWorkoutAssociation.validateForAcceptance(
                proposal,
                actual(),
                GymExecution.Origin.FREE,
                legacy(),
                emptySet(),
                false,
            )
        }
    }

    @Test
    fun openAuthoredSlotsCannotInventComparablePrescribedWork() {
        val programme = authored(true)
        val prepared = GymWorkoutAdapter.fromAuthoredProgramme(programme, "authored-session")
        assertEquals(
            programme.workouts().single(),
            GymWorkoutSourceValidator.authoredProgramme(
                prepared.definition(),
                prepared.source(),
                programme,
            ),
        )
        assertTrue(
            GymWorkoutAssociation.authoredProposals(
                    actual(),
                    GymExecution.Origin.FREE,
                    programme,
                    emptySet(),
                    now,
                )
                .isEmpty()
        )
        assertTrue(actual().exercises().single().sets().isNotEmpty())
    }

    private class Store(val p: GymWorkoutAssociation.Proposal) : GymAssociationRepository {
        var current: GymWorkoutAssociation.Decision? = null
        var failAfterCommit = false
        var failRead = false
        var wrongAck = false
        var writes = 0

        override fun proposals(
            actualId: String,
            reviewedAt: Instant,
        ): List<GymWorkoutAssociation.Proposal> {
            if (failRead) {
                failRead = false
                error("read failed")
            }
            return if (current?.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)
                emptyList()
            else listOf(p)
        }

        override fun current(actualId: String) = current

        override fun reviewCurrent(actualId: String, reviewedAt: Instant) = p

        override fun decide(
            decision: GymWorkoutAssociation.Decision
        ): GymWorkoutAssociation.Decision? {
            if (current != decision) {
                current = decision
                writes++
            }
            if (failAfterCommit) {
                failAfterCommit = false
                error("lost acknowledgement")
            }
            return if (wrongAck) null else current
        }
    }

    @Test
    fun associationRetryDoesNotCreateAnotherDecisionOrLosePendingAfterReadFailure() {
        val p = proposal()
        val store = Store(p)
        var ids = 0
        var clocks = 0
        val c =
            GymAssociationCoordinator(
                store,
                {
                    clocks++
                    now
                },
                { "decision-${++ids}" },
            )
        assertNull(c.view().actualId())
        assertEquals(0, clocks)
        c.review(p.actual().id())
        store.failAfterCommit = true
        assertFailsWith<IllegalStateException> {
            c.choose(p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)
        }
        val pending = c.view().pending()!!
        assertNull(c.view().current())
        assertEquals(1, ids)
        assertFailsWith<IllegalArgumentException> { c.clear() }
        assertFailsWith<IllegalArgumentException> {
            c.choose(p, GymWorkoutAssociation.Choice.KEEP_EXTRA)
        }
        store.failRead = true
        assertFailsWith<IllegalStateException> { c.retry() }
        assertSame(pending, c.view().pending())
        c.retry()
        assertNull(c.view().pending())
        assertEquals(pending, c.view().current())
        assertEquals(1, store.writes)
        assertEquals(1, ids)
        c.choose(c.view().currentReview(), GymWorkoutAssociation.Choice.KEEP_EXTRA)
        assertEquals(GymWorkoutAssociation.Choice.KEEP_EXTRA, c.view().current()!!.choice())
        assertEquals(2, store.writes)
        assertEquals(2, ids)
        c.clear()
        assertNull(c.view().actualId())
    }

    @Test
    fun wrongAcknowledgementCannotPublishAChoiceAndReloadIsExplicit() {
        val p = proposal()
        val store = Store(p)
        val c = GymAssociationCoordinator(store, { now }, { "decision" })
        c.review(p.actual().id())
        store.wrongAck = true
        assertFailsWith<IllegalArgumentException> {
            c.choose(p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION)
        }
        assertNotNull(c.view().pending())
        assertNull(c.view().current())
        store.failRead = true
        assertFailsWith<IllegalStateException> { c.reloadSaved() }
        assertNotNull(c.view().pending())
        c.reloadSaved()
        assertNull(c.view().pending())
        assertEquals(store.current, c.view().current())
    }
}
