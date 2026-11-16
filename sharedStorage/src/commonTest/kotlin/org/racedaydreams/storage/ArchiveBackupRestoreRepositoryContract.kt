package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.*

abstract class ArchiveBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = CoachingArchiveFixtures.at

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "archive-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    private fun seed(db: SqlDatabase): UserBackupDataJson.Snapshot {
        val running = SqliteRunningCoaching(db, { at }, { "unused" })
        running.saveCurrent(CoachingArchiveFixtures.version())
        running.saveCurrent(CoachingArchiveFixtures.version(2))
        val actual = LegacyFingerprintFixtures.session()
        for (e in actual.exercises()) db.execute(
            "INSERT INTO strength_exercises VALUES(?,?)",
            listOf(
                SqlValue.Text(e.exercise().id()),
                SqlValue.Text(StrengthEvidenceJson.encodeExercise(e.exercise())),
            ),
        )
        db.execute(
            "INSERT INTO strength_sessions VALUES(?,?,?)",
            listOf(
                SqlValue.Text(actual.id()),
                SqlValue.Text(actual.startedAt().toString()),
                SqlValue.Text(StrengthEvidenceJson.encodeSession(actual)),
            ),
        )
        SqliteLegacyGymProgrammes(db, { at }, { "not-a-new-decision" })
            .save(LegacyGymArchiveFixtures.programme())
        SqliteStrengthChoices(db).save(SqliteStrengthChoices.Choice("bench-press", true, true))
        SqliteExerciseExclusions(db).exclude(Exclusion("bench-press", Reason.PREFERENCE, at))
        db.execute("INSERT INTO pb_acknowledgements VALUES('actual-bench-pb')")
        db.execute(
            "INSERT INTO coach_evidence VALUES('WEIGHT','manual-weight',?)",
            listOf(
                SqlValue.Text(
                    """{"id":"manual-weight","kg":87.25,"at":"${at}","offset":"+05:30:15","provider":"manual","sourceId":"entry","origin":null,"retained":"original"}"""
                )
            ),
        )
        db.execute(
            "INSERT INTO coach_support VALUES('execution','interrupted-run',?)",
            listOf(
                SqlValue.Text(
                    """{"version":1,"mode":"quick","steps":[{"label":"Run easily","kind":"RUN","metres":"1000.5","millis":"300000","rpe":null}],"interrupted":true,"retained":"original"}"""
                )
            ),
        )
        db.execute(
            "INSERT INTO release08 VALUES('config','start',?)",
            listOf(
                SqlValue.Text(
                    """{"version":1,"type":"start","path":"hybrid","retained":"original"}"""
                )
            ),
        )
        return SqliteApplicationBackup(db).snapshot()
    }

    private fun compare(source: SqlDatabase, target: SqlDatabase) {
        val expected = SqliteApplicationBackup(source).snapshot()
        val actual = SqliteApplicationBackup(target).snapshot()
        assertEquals(
            CoachingArchiveJson.element(expected.coaching!!),
            CoachingArchiveJson.element(actual.coaching!!),
        )
        assertEquals(
            DevelopmentArchiveJson.element(expected.development!!),
            DevelopmentArchiveJson.element(actual.development!!),
        )
        assertEquals(
            StrengthChoicesArchiveJson.element(expected.choices!!),
            StrengthChoicesArchiveJson.element(actual.choices!!),
        )
        assertEquals(expected.legacy!!.programmes, actual.legacy!!.programmes)
        assertEquals(expected.legacy.current, actual.legacy.current)
        assertEquals(expected.legacy.decisions, actual.legacy.decisions)
        assertEquals(
            SupportArchiveJson.element(expected.support!!),
            SupportArchiveJson.element(actual.support!!),
        )
        assertEquals(
            Release08ArchiveJson.element(expected.release08!!),
            Release08ArchiveJson.element(actual.release08!!),
        )
    }

    @Test
    fun archivesRestoreOriginalPayloadsActualSetsChoicesAndSelectionsWithoutNewDecisions() =
        storage { source, target ->
            val snapshot = seed(source)
            SqliteCoreBackupRestore(target).populate(snapshot.core)
            SqliteArchiveBackupRestore(target).populate(snapshot)
            compare(source, target)
            assertEquals(
                "v2",
                SqliteRunningCoaching(target, { error("No clock") }, { error("No new ID") })
                    .current()!!
                    .id(),
            )
            assertEquals(1, SqliteExerciseExclusions(target).exclusions().size)
            assertTrue(target.query("SELECT * FROM strength_executions").isEmpty())
            assertTrue(target.query("SELECT * FROM gym_progression_decisions").isEmpty())
            assertTrue(target.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun oldestMissingFeatureSectionsStayEmptyAndDoNotManufactureTrainingHistory() =
        storage { source, target ->
            val snapshot = SqliteApplicationBackup(source).snapshot()
            // Actual old parent, with only store-v1 core data and no newer feature sections.
            val old = UserBackupDataJson.decode(SqliteCoreBackup(source).snapshot().data)
            assertNull(old.coaching)
            assertNull(old.legacy)
            SqliteCoreBackupRestore(target).populate(old.core)
            SqliteArchiveBackupRestore(target).populate(old)
            assertEquals(
                UserBackupDataJson.element(snapshot),
                UserBackupDataJson.element(SqliteApplicationBackup(target).snapshot()),
            )
        }

    @Test
    fun supportedOlderImportedWeightIsRetainedButNotReexportedAsUserOwnedEvidence() =
        storage { source, target ->
            val original = seed(source)
            val root = UserBackupDataJson.element(original)
            val development = root.getValue("development").jsonObject
            val manual = development.getValue("records").jsonArray.single().jsonObject
            val cached =
                JsonObject(
                    manual +
                        mapOf(
                            "id" to JsonPrimitive("old-import"),
                            "payload" to
                                JsonObject(
                                    manual.getValue("payload").jsonObject +
                                        mapOf(
                                            "id" to JsonPrimitive("old-import"),
                                            "provider" to JsonPrimitive("health-connect"),
                                            "sourceId" to JsonPrimitive("provider-record"),
                                        )
                                ),
                        )
                )
            val snapshot =
                UserBackupDataJson.decode(
                    JsonObject(
                        root +
                            ("development" to
                                JsonObject(
                                    development + ("records" to JsonArray(listOf(manual, cached)))
                                ))
                    )
                )
            SqliteCoreBackupRestore(target).populate(snapshot.core)
            SqliteArchiveBackupRestore(target).populate(snapshot)
            assertEquals(
                2L,
                target
                    .query("SELECT COUNT(*) FROM coach_evidence WHERE kind='WEIGHT'")
                    .single()
                    .single()
                    .sqlLong(),
            )
            assertEquals(
                cached.getValue("payload").toString(),
                target
                    .query("SELECT payload FROM coach_evidence WHERE id='old-import'")
                    .single()
                    .single()
                    .sqlText(),
            )
            assertEquals(
                listOf("manual-weight"),
                SqliteDevelopmentArchive(target).snapshot().records.map { it.id },
            )
            assertEquals(
                2L,
                target
                    .query("SELECT COUNT(*) FROM coach_evidence WHERE kind='WEIGHT'")
                    .single()
                    .single()
                    .sqlLong(),
            )
        }

    @Test
    fun anyOccupiedArchiveTableRejectsBeforeEarlierFeatureWrites() = storage { source, target ->
        val snapshot = seed(source)
        target.execute("INSERT INTO release08 VALUES('config','start','{}')")
        val before = target.query("SELECT total_changes()")
        assertFails { SqliteArchiveBackupRestore(target).populate(snapshot) }
        assertEquals(before, target.query("SELECT total_changes()"))
        assertTrue(target.query("SELECT * FROM coach_plan_versions").isEmpty())
        assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
        assertEquals(
            "{}",
            target.query("SELECT payload FROM release08").single().single().sqlText(),
        )
    }

    @Test
    fun finalFeatureFailureAndOuterRollbackKeepAllDependentHistoryAtomic() =
        storage { source, target ->
            val snapshot = seed(source)
            val failed =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT INTO release08"))
                            error("Simulated last feature write failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails {
                target.transaction {
                    SqliteCoreBackupRestore(target).populate(snapshot.core)
                    SqliteArchiveBackupRestore(failed).populate(snapshot)
                }
            }
            assertTrue(target.query("SELECT * FROM planned_workouts").isEmpty())
            assertTrue(target.query("SELECT * FROM coach_plan_versions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
            assertTrue(target.query("SELECT * FROM strength_exclusions").isEmpty())
            assertTrue(target.query("SELECT * FROM coach_support").isEmpty())
            assertFails {
                target.transaction {
                    SqliteCoreBackupRestore(target).populate(snapshot.core)
                    SqliteArchiveBackupRestore(target).populate(snapshot)
                    compare(source, target)
                    error("Reject full restore")
                }
            }
            assertTrue(target.query("SELECT * FROM strength_sessions").isEmpty())
            target.transaction {
                SqliteCoreBackupRestore(target).populate(snapshot.core)
                SqliteArchiveBackupRestore(target).populate(snapshot)
            }
            compare(source, target)
        }
}
