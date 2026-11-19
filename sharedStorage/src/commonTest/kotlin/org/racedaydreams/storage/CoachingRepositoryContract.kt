package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Exercises the full interface composition, not a parallel implementation of its rules. */
abstract class CoachingRepositoryContract {
    abstract fun database(): SqlDatabase

    private val now = StoredInstant.parse("2026-11-17T09:00:00Z")

    private fun storage(block: (SqlDatabase, CoachingRepository) -> Unit) {
        val db = database()
        var ids = 0
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "coaching-composition")
            }
            block(
                db,
                SqliteCoachingRepository(
                    db,
                    { now },
                    { "chosen-version-${++ids}" },
                    PortableLegacyStrengthFingerprint,
                ),
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun actualSessionsAndCanonicalExerciseOverridesShareTheExistingDatabaseAndFrozenHistory() =
        storage { db, repository ->
            val exercise = StrengthExercise.starters().first()
            val before = repository.exercises().map { it.id() }
            val renamed = exercise.copy(name = "Athlete's logging name")
            repository.saveExercise(renamed)
            assertEquals(before, repository.exercises().map { it.id() })
            assertEquals(renamed, repository.exercises().first { it.id() == exercise.id() })
            val actual = LegacyFingerprintFixtures.session()
            repository.saveStrength(actual)
            val oldPayload = db.query("SELECT payload FROM strength_sessions").single().single()
            repository.saveExercise(renamed.copy(name = "A future logging name"))
            assertEquals(listOf(actual), repository.strength())
            assertEquals(
                oldPayload,
                db.query("SELECT payload FROM strength_sessions").single().single(),
            )
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun explicitActualEditsAndDeletionUseExistingExecutionProvenanceWithoutCreatingPrescriptions() =
        storage { db, repository ->
            val original = LegacyFingerprintFixtures.session()
            repository.saveStrength(original)
            val edited = original.copy(notes = "Explicit actual edit")
            repository.saveStrength(edited)
            assertEquals(listOf(edited), repository.strength())
            assertNull(repository.current())
            assertTrue(repository.versions().isEmpty())
            repository.deleteStrength(edited.id())
            assertTrue(repository.strength().isEmpty())
            assertTrue(db.query("SELECT * FROM coach_current_plan").isEmpty())
        }

    @Test
    fun futureActualIsRejectedBeforeAnyActualOrExerciseMutation() = storage { db, repository ->
        val before = db.query("SELECT total_changes()")
        assertFailsWith<IllegalArgumentException> {
            repository.saveStrength(
                LegacyFingerprintFixtures.session()
                    .copy(startedAt = StoredInstant.parse("2027-01-01T00:00:00Z"))
            )
        }
        assertTrue(repository.strength().isEmpty())
        assertEquals(before, db.query("SELECT total_changes()"))
    }

    @Test
    fun proposalRemainsOptionalAndKeepChoiceDoesNotCreateAnotherPlanVersion() =
        storage { db, repository ->
            val original = CoachingArchiveFixtures.version()
            repository.saveCurrent(original)
            val proposal = CoachingArchiveFixtures.proposal()
            repository.saveProposal(proposal)
            assertTrue(repository.proposalKnown(proposal.id()))
            assertEquals(original, repository.current())
            assertEquals(listOf(proposal), repository.proposals())
            repository.decide(proposal, "keep", emptySet())
            assertEquals(listOf(original), repository.versions())
            assertTrue(repository.proposals().isEmpty())
            assertFails { repository.decide(proposal, "keep", emptySet()) }
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun completedPrescriptionsAndOriginalResponseSurviveAttemptedFutureReplacement() =
        storage { _, repository ->
            val original = CoachingArchiveFixtures.version()
            repository.saveCurrent(original)
            val response =
                RunExecution(
                    "actual-run",
                    "done",
                    original.id(),
                    null,
                    null,
                    "Actual stays distinct",
                )
            repository.saveResponse(response)
            val changed =
                original.plan().workouts().map {
                    if (it.id() == "done") it.copy(distanceKm = 8.0) else it
                }
            assertFails { repository.saveCurrent(CoachingArchiveFixtures.version(2, changed)) }
            assertEquals(original, repository.current())
            assertEquals(listOf(response), repository.responses())
            assertEquals(listOf(original), repository.versions())
        }

    @Test
    fun oneRejectedInterfaceTransactionRollsBackRunningAndStrengthCommandsTogether() =
        storage { _, repository ->
            assertFailsWith<IllegalStateException> {
                repository.transaction(
                    object : CoachingTransaction {
                        override fun run() {
                            repository.saveCurrent(CoachingArchiveFixtures.version())
                            repository.saveStrength(LegacyFingerprintFixtures.session())
                            error("Reject the combined operation")
                        }
                    }
                )
            }
            assertNull(repository.current())
            assertTrue(repository.versions().isEmpty())
            assertTrue(repository.strength().isEmpty())
        }

    @Test
    fun customLoggingEditsKeepTypedMetadataAndRejectMeasurementTransfers() =
        storage { db, repository ->
            val exercise =
                StrengthExercise(
                    "custom-composition",
                    "My press",
                    StrengthExercise.Region.UPPER,
                    StrengthExercise.Mode.WEIGHTED,
                    false,
                    StrengthExercise.MeasurementProfile.LOAD_REPS,
                )
            val metadata =
                CustomStrengthExercise(
                    exercise,
                    ExerciseMetadata.BodyArea.CHEST,
                    ExerciseMetadata.Role.HORIZONTAL_PUSH,
                    ExerciseMetadata.EquipmentRequirement(
                        listOf(setOf(ExerciseMetadata.Equipment.DUMBBELLS))
                    ),
                    "Athlete notes",
                )
            SqliteStrengthDrafts(db).saveCustom(metadata, null)
            val renamed = exercise.copy(name = "My renamed press")
            repository.saveExercise(renamed)
            val records = SqliteStrengthProgrammingRecords(db)
            assertEquals(metadata.copy(exercise = renamed), records.customExercise(exercise.id()))
            assertEquals(renamed, repository.exercises().first { it.id() == exercise.id() })
            assertFailsWith<IllegalArgumentException> {
                repository.saveExercise(
                    renamed.copy(
                        mode = StrengthExercise.Mode.BODYWEIGHT,
                        measurement = StrengthExercise.MeasurementProfile.REPS_ONLY,
                    )
                )
            }
            assertEquals(metadata.copy(exercise = renamed), records.customExercise(exercise.id()))
            assertTrue(repository.strength().isEmpty())
        }

    @Test
    fun unfinishedExecutionCannotBeReplacedOrDeletedThroughTheFullCoachingInterface() =
        storage { db, repository ->
            val actual = LegacyFingerprintFixtures.session()
            val execution =
                GymExecutionEngine.start(
                        actual.id(),
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        Offsets.ofTotalSeconds(0),
                        GymExecution.Tick("boot", 0, actual.startedAt()),
                    )
                    .snapshot()
            val live = SqliteGymExecutions(db, { now }, PortableLegacyStrengthFingerprint)
            val stored = live.start(execution)
            val changes = db.query("SELECT total_changes()")
            assertFailsWith<IllegalArgumentException> { repository.saveStrength(actual) }
            assertFailsWith<IllegalArgumentException> { repository.deleteStrength(actual.id()) }
            assertEquals(stored, live.load(actual.id()))
            assertEquals(changes, db.query("SELECT total_changes()"))
            assertTrue(repository.strength().isEmpty())
        }
}
