package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*
import org.racedaydreams.domain.time.*

abstract class ProgrammingBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = GymEngineArchiveFixtures.at
    private val start = LegacyGymArchiveFixtures.start
    private val bench = GymEngineArchiveFixtures.bench

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "programming-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun programme(): StrengthProgramme {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.INTERMEDIATE,
                setOf(DayOfWeek.TUESDAY),
                1,
                60,
                ExerciseMetadata.Equipment.entries.toSet(),
            )
        val slots =
            listOf(
                GymWorkoutDefinition.Slot(
                    "bench",
                    bench,
                    GymWorkoutDefinition.Target(
                        2,
                        4,
                        6,
                        null,
                        null,
                        GymWorkoutDefinition.EffortRange(
                            GymWorkoutDefinition.EffortScale.RIR,
                            2.5,
                            4.5,
                        ),
                        180,
                        82.25,
                    ),
                    null,
                )
            )
        val workouts =
            List(2) { i ->
                val id = "workout-$i"
                StrengthProgramme.Workout(
                    id,
                    start.plusWeeks(i.toLong()),
                    StrengthWorkoutBudget.workoutMinutes(slots),
                    GymWorkoutDefinition(id, 1, at, "Upper", slots),
                    null,
                )
            }
        return StrengthProgramme(
            "authored",
            1,
            0,
            at,
            "Authored",
            StrengthProgramme.Origin.CUSTOM,
            null,
            settings,
            listOf(
                StrengthProgramme.Block(
                    "block",
                    StrengthProgramme.Phase.DEVELOPMENT,
                    "Development",
                    start,
                    2,
                    workouts,
                    "Retained explanation",
                )
            ),
            start,
            "Athlete accepted",
            emptyList(),
        )
    }

    private fun seed(db: SqlDatabase): UserBackupDataJson.Snapshot {
        val p = programme()
        val drafts = SqliteStrengthDrafts(db)
        val custom =
            CustomStrengthExercise(
                StrengthExercise(
                    "custom-hold",
                    "My hold",
                    StrengthExercise.Region.FULL,
                    StrengthExercise.Mode.BODYWEIGHT,
                    false,
                    StrengthExercise.MeasurementProfile.DURATION,
                ),
                ExerciseMetadata.BodyArea.TRUNK,
                ExerciseMetadata.Role.CORE,
                ExerciseMetadata.EquipmentRequirement(listOf(emptySet())),
                "Retained custom note",
            )
        drafts.saveCustom(custom, null)
        var concrete = ConcreteStrengthProgrammeDraft("competing-preview", 1, null, p, emptyList())
        drafts.saveConcrete(concrete, 0)
        for (revision in 2..4) {
            concrete = concrete.copy(revision = revision)
            drafts.saveConcrete(concrete, revision - 1)
        }
        val repo = SqliteStrengthProgramming(db, { at }, { start })
        val accepted = repo.activate(p, null, "accept-original", emptyList())
        val changed =
            p.copy(
                revision = 2,
                parentRevision = 1,
                effectiveFrom = start,
                reason = "Athlete accepted new explanation",
            )
        repo.activate(changed, accepted.after(), "accept-future", emptyList())
        drafts.saveDraft(
            StrengthProgrammeDraft(
                "future-draft",
                3,
                p.id(),
                2,
                "",
                p.settings(),
                start,
                emptyList(),
                StrengthProgramme.Origin.CUSTOM,
                null,
            ),
            0,
        )
        return SqliteApplicationBackup(db).snapshot()
    }

    private fun restoreContext(target: SqlDatabase, snapshot: UserBackupDataJson.Snapshot) {
        SqliteCoreBackupRestore(target).populate(snapshot.core)
        SqliteArchiveBackupRestore(target).populate(snapshot)
    }

    private fun restorer(db: SqlDatabase) =
        SqliteStrengthProgramming(
            db,
            { error("Restoring decisions must not use today's clock") },
            { error("Restoring history must not require future dates") },
        )

    @Test
    fun immutableRevisionsDraftsCustomMetadataAndExactDecisionHistoryRestoreWithoutActivation() =
        storage { source, target ->
            val snapshot = seed(source)
            restoreContext(target, snapshot)
            restorer(target).populateArchive(snapshot)
            val expected = snapshot.programming!!
            val actual = SqliteApplicationBackup(target).snapshot().programming!!
            assertEquals(
                StrengthProgrammingArchiveJson.element(expected),
                StrengthProgrammingArchiveJson.element(actual),
            )
            assertEquals(expected.current, restorer(target).currentReference())
            assertEquals(expected.decisions, restorer(target).decisions())
            assertEquals(
                3,
                SqliteStrengthProgrammingRecords(target).draft("future-draft")!!.revision(),
            )
            assertTrue(
                SqliteStrengthProgrammingRecords(target).draft("future-draft")!!.blocks().isEmpty()
            )
            val preview =
                SqliteStrengthProgrammingRecords(target).concreteDraft("competing-preview")!!
            assertEquals(4, preview.revision())
            assertEquals(expected.programmes.getValue("authored:v1"), preview.programme())
            assertEquals(
                82.25,
                preview
                    .programme()
                    .workouts()
                    .first()
                    .definition()
                    .slots()
                    .single()
                    .target()!!
                    .referenceLoadKg(),
            )
            assertEquals(
                2L,
                target
                    .query("SELECT COUNT(*) FROM strength_programme_decisions")
                    .single()
                    .single()
                    .sqlLong(),
            )
            assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun stoppedProgrammingHistoryStaysStoppedAndDoesNotCreateAReplacementDecision() =
        storage { source, target ->
            seed(source)
            val repo = SqliteStrengthProgramming(source, { at }, { start })
            repo.stop(repo.currentReference()!!, "stop-original")
            val snapshot = SqliteApplicationBackup(source).snapshot()
            restoreContext(target, snapshot)
            restorer(target).populateArchive(snapshot)
            val actual = SqliteApplicationBackup(target).snapshot().programming!!
            assertNull(actual.current)
            assertEquals(snapshot.programming!!.decisions, actual.decisions)
            assertEquals(2, actual.programmes.size)
            assertEquals("stop-original", actual.decisions.last().id())
        }

    @Test
    fun missingOldProgrammingArchiveKeepsActualLegacySelectionAndHistory() =
        storage { source, target ->
            val legacy = LegacyGymArchiveFixtures.programme()
            SqliteLegacyGymProgrammes(source, { at }, { "unused" }).save(legacy)
            val current = SqliteApplicationBackup(source).snapshot()
            val old =
                UserBackupDataJson.decode(
                    kotlinx.serialization.json.JsonObject(
                        UserBackupDataJson.element(current) - "strengthProgramming"
                    )
                )
            assertNull(old.programming)
            restoreContext(target, old)
            val before = target.query("SELECT total_changes()")
            restorer(target).populateArchive(old)
            assertEquals(before, target.query("SELECT total_changes()"))
            assertEquals(
                Reference(Owner.LEGACY, legacy.id(), legacy.version()),
                restorer(target).currentReference(),
            )
            assertEquals(
                legacy,
                SqliteLegacyGymProgrammes(target, { error("No clock") }, { error("No decision") })
                    .current(),
            )
        }

    @Test
    fun occupiedLateTableRejectsBeforeRestoringAnyRevisionOrCustomDefinition() =
        storage { source, target ->
            val snapshot = seed(source)
            restoreContext(target, snapshot)
            target.execute("INSERT INTO strength_custom_exercises VALUES('existing','{}')")
            val before = target.query("SELECT total_changes()")
            assertFails { restorer(target).populateArchive(snapshot) }
            assertEquals(before, target.query("SELECT total_changes()"))
            assertTrue(target.query("SELECT * FROM strength_programme_revisions").isEmpty())
            assertEquals(
                "{}",
                target
                    .query("SELECT payload FROM strength_custom_exercises")
                    .single()
                    .single()
                    .sqlText(),
            )
        }

    @Test
    fun latePointerFailureAndCallerRollbackCannotLeaveHalfRestoredProgrammeHistory() =
        storage { source, target ->
            val snapshot = seed(source)
            restoreContext(target, snapshot)
            val definitions = target.query("SELECT * FROM strength_exercises ORDER BY id")
            val failed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT INTO strength_programme_current"))
                            error("Simulated last pointer write failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails { restorer(failed).populateArchive(snapshot) }
            assertTrue(target.query("SELECT * FROM strength_programme_revisions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_programme_drafts").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_concrete_drafts").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_custom_exercises").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_programme_decisions").isEmpty())
            assertEquals(definitions, target.query("SELECT * FROM strength_exercises ORDER BY id"))
            assertFails {
                target.transaction {
                    restorer(target).populateArchive(snapshot)
                    assertEquals("authored:v2", restorer(target).currentReference()!!.key())
                    error("Reject full replacement")
                }
            }
            assertNull(restorer(target).currentReference())
            restorer(target).populateArchive(snapshot)
            assertEquals(
                StrengthProgrammingArchiveJson.element(snapshot.programming!!),
                StrengthProgrammingArchiveJson.element(
                    SqliteApplicationBackup(target).snapshot().programming!!
                ),
            )
        }
}
