package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.body.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class ApplicationBackupRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = GymEngineArchiveFixtures.at

    private fun <T> storage(block: (SqlDatabase) -> T): T {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "application-backup-test")
            }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun actual(db: SqlDatabase): GymExecution {
        val engine =
            GymExecutionEngine.start(
                "actual",
                GymExecution.Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(0),
                GymEngineArchiveFixtures.tick(0),
            )
        val repo = SqliteGymExecutions(db, { at }, PortableLegacyStrengthFingerprint)
        var stored = repo.start(engine.snapshot())
        engine.addExercises(
            listOf(GymWorkoutDefinition.Slot("bench", GymEngineArchiveFixtures.bench, null, null)),
            GymEngineArchiveFixtures.tick(10),
        )
        stored = repo.save(engine.snapshot(), stored.revision())
        engine.saveSet(
            "bench",
            StrengthSession.SetEntry("set", 6, 50.0, 7.0, false),
            GymEngineArchiveFixtures.tick(1000),
        )
        val finished =
            engine.finish(GymEngineArchiveFixtures.tick(2000), null, "Recorded actual", true)
        repo.finish(finished, stored.revision())
        return finished
    }

    @Test
    fun emptyCurrentDatabaseExplicitlyCapturesAllSectionsAndRoundTripsWithoutInventingRunningSetup() =
        storage { db ->
            val before = db.query("SELECT total_changes()")
            val expectedRetention = SqliteRetentionPreferences(db).load()
            val snapshot = SqliteApplicationBackup(db).snapshot()
            val payload = UserBackupDataJson.element(snapshot)
            for (section in
                listOf(
                    "coaching",
                    "development",
                    "strengthState",
                    "gymProgramming",
                    "support",
                    "release08",
                    "strengthProgramming",
                    "strengthEngine",
                    "gymTools",
                    "achievements",
                    "bodyMeasurements",
                    "detailRetention",
                )) assertTrue(section in payload, section)
            assertNull(snapshot.core.profile)
            assertTrue(snapshot.core.activities.isEmpty())
            assertNotNull(snapshot.programming)
            assertNotNull(snapshot.engine)
            assertEquals(expectedRetention, snapshot.retention.preferences)
            val encoded = SqliteApplicationBackup(db).encode(at)
            val restored = UserBackupDataJson.inspect(encoded)
            assertEquals(14, restored.version)
            assertEquals(at, restored.createdAt)
            assertEquals(payload, UserBackupDataJson.element(restored.snapshot))
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun actualSetsLegacyHistoryExclusionsMeasurementsAndAwardsAreCapturedRatherThanDefaultedAway() =
        storage { db ->
            val finished = actual(db)
            val legacy = LegacyGymArchiveFixtures.programme()
            SqliteLegacyGymProgrammes(db, { at }, { "decision" }).save(legacy)
            SqliteStrengthChoices(db).save(SqliteStrengthChoices.Choice("bench-press", true, false))
            db.execute(
                "INSERT INTO strength_exclusions VALUES('bench-press','PREFERENCE',?)",
                listOf(SqlValue.Text(at.toString())),
            )
            val height =
                BodyHeight("manual-height", 1.83, at, null, "manual", "manual-height", null, null)
            SqliteBodyHeights(db).saveHeight(height)
            val award =
                Award(Kind.FIRST_GYM, at, listOf(Proof(Source.GYM, "actual", at, null, null)))
            SqliteTrainingMilestones(db).recognize(listOf(award), at)
            val before = db.query("SELECT total_changes()")
            val snapshot = SqliteApplicationBackup(db).snapshot()
            assertEquals(
                finished,
                snapshot.engine!!.executions.getValue("actual").original.execution(),
            )
            assertEquals(
                finished.completedSession(),
                snapshot.coaching!!.sessions.getValue("actual"),
            )
            assertEquals(legacy, snapshot.legacy!!.programmes.getValue("programme:v1"))
            assertEquals(listOf(height), snapshot.body.heights)
            assertEquals(award, snapshot.achievements.awards.single().award)
            assertEquals(1, snapshot.choices!!.exclusions.size)
            assertTrue(snapshot.choices!!.choices.single().favourite)
            assertEquals(before, db.query("SELECT total_changes()"))
            val inspected = UserBackupDataJson.inspect(SqliteApplicationBackup(db).encode(at))
            assertEquals(
                UserBackupDataJson.element(snapshot),
                UserBackupDataJson.element(inspected.snapshot),
            )
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun authoredRevisionAndIncompleteDraftRemainDistinctFromActualWorkoutsAndAreNotActivatedByReading() =
        storage { db ->
            val start = LegacyGymArchiveFixtures.start
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
                        GymEngineArchiveFixtures.bench,
                        GymWorkoutDefinition.Target(2, 4, 6, null, null, null, 180, 82.25),
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
            val programme =
                StrengthProgramme(
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
                            "Stable practice",
                        )
                    ),
                    start,
                    "Athlete accepted",
                    emptyList(),
                )
            SqliteStrengthProgramming(db, { at }, { CalendarDates.of(2026, 11, 16) })
                .activate(programme, null, "activate", emptyList())
            val draft =
                StrengthProgrammeDraft(
                    "draft",
                    1,
                    "authored",
                    1,
                    "Future draft",
                    settings,
                    start,
                    emptyList(),
                    StrengthProgramme.Origin.CUSTOM,
                    null,
                )
            SqliteStrengthDrafts(db).saveDraft(draft, 0)
            val before = db.query("SELECT total_changes()")
            val snapshot = SqliteApplicationBackup(db).snapshot()
            assertEquals(programme, snapshot.programming!!.programmes.getValue("authored:v1"))
            assertEquals(listOf(draft), snapshot.programming!!.drafts)
            assertEquals("authored:v1", snapshot.programming!!.current!!.key())
            assertTrue(snapshot.coaching!!.sessions.isEmpty())
            assertTrue(snapshot.engine!!.executions.isEmpty())
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun unacceptedActualReplacementCannotReturnAnApparentlyCompleteBackup() = storage { db ->
        val finished = actual(db)
        // Canonical definitions may legitimately be resolved from the retained catalogue.
        db.execute("DELETE FROM strength_exercises")
        assertEquals(1, SqliteApplicationBackup(db).snapshot().engine!!.executions.size)
        val original =
            db.query("SELECT payload FROM strength_sessions WHERE id='actual'").single().single()
        val changed = finished.completedSession()!!.copy(notes = "Unaccepted replacement")
        db.execute(
            "UPDATE strength_sessions SET payload=? WHERE id='actual'",
            listOf(SqlValue.Text(StrengthEvidenceJson.session(changed).toString())),
        )
        val before = db.query("SELECT total_changes()")
        assertFails { SqliteApplicationBackup(db).snapshot() }
        assertEquals(before, db.query("SELECT total_changes()"))
        assertEquals(
            1L,
            db.query("SELECT COUNT(*) FROM strength_sessions").single().single().sqlLong(),
        )
        db.execute("UPDATE strength_sessions SET payload=? WHERE id='actual'", listOf(original))
        assertEquals(1, SqliteApplicationBackup(db).snapshot().engine!!.executions.size)
    }

    @Test
    fun oversizedNormalizedGymDataFailsBeforeHydrationAndLeavesEveryRetainedRowUntouched() =
        storage { db ->
            val payload = "x".repeat(262144)
            db.transaction {
                for (i in 0..128) db.execute(
                    "INSERT INTO strength_workout_versions(workout_id,revision,payload) VALUES(?,1,?)",
                    listOf(SqlValue.Text("large-$i"), SqlValue.Text(payload)),
                )
            }
            var hydrated = false
            val tracked =
                object : SqlDatabase by db {
                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        if (sql.startsWith("SELECT payload FROM strength_workout_versions ORDER"))
                            hydrated = true
                        return db.query(sql, arguments)
                    }
                }
            val before = db.query("SELECT total_changes()")
            assertFails { SqliteApplicationBackup(tracked).snapshot() }
            assertFalse(hydrated)
            assertEquals(before, db.query("SELECT total_changes()"))
            assertEquals(
                129L,
                db.query("SELECT COUNT(*) FROM strength_workout_versions")
                    .single()
                    .single()
                    .sqlLong(),
            )
        }

    @Test
    fun completeCaptureNeverWritesOrCommitsItsCallersUnacceptedChanges() = storage { db ->
        var depth = 0
        val readonly =
            object : SqlDatabase by db {
                override fun <T> transaction(block: () -> T): T =
                    db.transaction {
                        depth++
                        try {
                            block()
                        } finally {
                            depth--
                        }
                    }

                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> {
                    assertTrue(depth > 0)
                    return db.query(sql, arguments)
                }

                override fun execute(sql: String, arguments: List<SqlValue>) =
                    error("Capture cannot change the database")
            }
        assertFails {
            db.transaction {
                db.execute("INSERT INTO settings VALUES('pending','athlete choice')")
                assertEquals(
                    "athlete choice",
                    SqliteApplicationBackup(readonly).snapshot().core.settings["pending"],
                )
                error("reject caller changes")
            }
        }
        assertTrue(db.query("SELECT * FROM settings WHERE setting_key='pending'").isEmpty())
        assertFalse("pending" in SqliteApplicationBackup(db).snapshot().core.settings)
    }
}
