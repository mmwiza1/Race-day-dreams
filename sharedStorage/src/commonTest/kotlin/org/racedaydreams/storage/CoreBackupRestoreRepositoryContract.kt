package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.time.*

abstract class CoreBackupRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = CoachingArchiveFixtures.at

    private fun storage(block: (SqlDatabase, SqlDatabase) -> Unit) {
        val source = database()
        val target = database()
        try {
            for (db in listOf(source, target)) ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "core-restore-test")
            }
            block(source, target)
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun runnerComparisonSurvivesProviderRefreshAndBackupWithoutLosingEitherRun() =
        storage { source, target ->
            val first =
                ActivityRecord(
                    "native",
                    SourceIdentity("rdd", "native-record", "local"),
                    at,
                    Instants.ofEpochSecond(at.getEpochSecond() + 1_980, at.getNano().toLong()),
                    Offsets.ofTotalSeconds(0),
                    Offsets.ofTotalSeconds(0),
                    1500.0,
                    Durations.ofSeconds(1980),
                    null,
                    null,
                    null,
                    null,
                    null,
                    "",
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                )
            val watch =
                first.copy(
                    id = "watch",
                    source =
                        SourceIdentity("health-connect", "fitbit-id", "com.fitbit.FitbitMobile"),
                    distanceMetres = 5100.0,
                )
            val activities = SqliteActivities(source) { at }
            activities.upsertManualActivity(first)
            activities.applyImport("health-connect", listOf(watch), emptyList(), "initial")
            activities.reviewDuplicateRun("native", "watch", DuplicateRunChoice.COUNT_OTHER)
            activities.applyImport("health-connect", listOf(watch), emptyList(), "updated")
            val reviewed = activities.activitySummaries()
            assertEquals(setOf("native", "watch"), reviewed.map { it.id() }.toSet())
            assertEquals(5100.0, PhysicalRunSessions.counted(reviewed).single().distanceMetres())
            assertFails { activities.deleteReviewedLocalDuplicate("watch", "native") }
            SqliteCoreBackupRestore(target).populate(SqliteCoreBackup(source).snapshot().core)
            val restored = SqliteActivities(target) { at }
            // Provider histories are intentionally omitted from the app backup. The locally owned
            // record retains its choice, which takes effect when the same provider record is seen.
            assertEquals(
                reviewed.single { it.id() == "native" },
                restored.activitySummaries().single(),
            )
            restored.applyImport("health-connect", listOf(watch), emptyList(), "resync")
            assertEquals(
                setOf("native", "watch"),
                restored.activitySummaries().map { it.id() }.toSet(),
            )
            assertEquals(
                5100.0,
                PhysicalRunSessions.counted(restored.activitySummaries()).single().distanceMetres(),
            )
            restored.reviewDuplicateRun("native", "watch", DuplicateRunChoice.SEPARATE)
            assertEquals(2, PhysicalRunSessions.counted(restored.activitySummaries()).size)
            restored.reviewDuplicateRun("native", "watch", DuplicateRunChoice.COUNT_OTHER)
            restored.deleteReviewedLocalDuplicate("native", "watch")
            assertEquals("watch", restored.activitySummaries().single().id())
        }

    @Test
    fun treadmillSummarySurvivesBackupWithoutInventedMovingTimeOrRoute() =
        storage { source, target ->
            val atEnd = Instants.ofEpochSecond(at.getEpochSecond() + 1800, at.getNano().toLong())
            val treadmill =
                ActivityRecord(
                    "treadmill",
                    SourceIdentity("manual", "treadmill", null),
                    at,
                    atEnd,
                    Offsets.ofTotalSeconds(0),
                    Offsets.ofTotalSeconds(0),
                    5_000.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "",
                    emptyList(),
                    emptyList(),
                    null,
                    null,
                    environment = RunEnvironment.TREADMILL,
                )
            SqliteActivities(source) { at }.upsertManualActivity(treadmill)
            SqliteCoreBackupRestore(target).populate(SqliteCoreBackup(source).snapshot().core)
            val restored = SqliteActivities(target) { at }.activitySummaries().single()
            assertEquals(treadmill, restored)
            assertEquals(5_000.0, restored.distanceMetres())
            assertEquals(Durations.ofSeconds(1800), restored.elapsedTime())
            assertTrue(restored.samples().isEmpty())
            assertNull(restored.movingTime())
        }

    private fun seed(db: SqlDatabase): CoreBackupDataJson.Snapshot {
        SqliteAthleteProfile(db).save(CoachingArchiveFixtures.profile())
        db.execute(
            "INSERT INTO onboarding_draft VALUES(1,?)",
            listOf(SqlValue.Text("""{"unfinished":"retain 🏃"}""")),
        )
        SqliteShoes(db)
            .saveShoe(Shoe("default-shoe", "Brand", "Default", null, 100.0, false, null, true))
        val actual =
            ActivityRecord(
                "actual",
                SourceIdentity("rdd", "run", "local"),
                at,
                Instants.ofEpochSecond(at.getEpochSecond() + 1800, at.getNano().toLong()),
                Offsets.ofTotalSeconds(0),
                null,
                5000.0,
                Durations.ofSeconds(1800),
                null,
                null,
                null,
                null,
                null,
                "Actual recorded summary",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        SqliteActivities(db) { at }.putActivity(actual, true, false)
        // No automatic default assignment; annotations and custom notes must remain authoritative.
        db.execute("UPDATE activity_annotations SET notes='Edited note',rpe=7.5")
        val race =
            RecordedResult(
                "race",
                PerformanceDistance.FIVE_K,
                Durations.ofSeconds(1700),
                CoachingArchiveFixtures.start,
                "Official race",
                ResultSource.OFFICIAL_CHIP,
                null,
                null,
            )
        SqliteResults(db).saveResult(race)
        val best =
            race.copy(id = "retained-best", source = ResultSource.ROLLING, activityId = "actual")
        db.execute(
            "INSERT INTO preserved_best_efforts VALUES('retained-best','actual',?,'rdd','run','local',?)",
            listOf(SqlValue.Text(best.distance().code()), SqlValue.Text(ResultJson.encode(best))),
        )
        SqliteBaselines(db) { error("Do not invent a baseline time") }
            .putBaseline(ManualBaselineInput(20.0, 3.0, null, null, null, null, null, 10), null)
        db.execute("INSERT INTO settings VALUES('primary.focus','hybrid')")
        SqlitePlannedWorkouts(db).put(CoachingArchiveFixtures.workout(), 9007199254740993L)
        SqliteBaselines(db) { at }
            .saveAssessment(
                AssessmentResult(
                    "assessment",
                    BaselineAssessment(5, AssessmentMode.CONTROLLED),
                    Durations.ofMinutes(30),
                    null,
                    at,
                    "actual",
                )
            )
        db.execute(
            "INSERT INTO training_plans VALUES('plan',?)",
            listOf(SqlValue.Text("""{"id":"plan","retained":"unchanged"}""")),
        )
        db.execute(
            "INSERT INTO plan_adjustments VALUES('adjustment','plan','keep',?)",
            listOf(SqlValue.Text("""{"id":"adjustment","planId":"plan","userDecision":"keep"}""")),
        )
        return SqliteCoreBackup(db).snapshot().core
    }

    @Test
    fun originalCoreFieldsRestoreWithExactReferencesAndWithoutRecalculatingPerformanceOrTimes() =
        storage { source, target ->
            val core = seed(source)
            val expected = SqliteCoreBackup(source).snapshot().data
            val retention = SqliteRecordingDetailReceipts(target).snapshot()
            SqliteCoreBackupRestore(target).populate(core)
            assertEquals(expected, SqliteCoreBackup(target).snapshot().data)
            assertEquals(
                listOf(core.bestEfforts.single().result),
                SqliteResults(target).results().filter { it.source() == ResultSource.ROLLING },
            )
            assertTrue(target.query("SELECT * FROM best_efforts").isEmpty())
            assertEquals(
                9007199254740993L,
                SqlitePlannedWorkouts(target).plannedWorkoutRevision("done"),
            )
            assertNull(SqliteActivities(target) { at }.activitySummaries().single().shoeId())
            assertEquals(retention.preferences, SqliteRetentionPreferences(target).load())
            assertEquals(
                retention.removals,
                SqliteRecordingDetailReceipts(target).snapshot().removals,
            )
            assertTrue(target.query("SELECT * FROM recording_sessions").isEmpty())
        }

    @Test
    fun strengthOnlyRestoreDoesNotCreateARaceProfileOrInventMissingReferences() =
        storage { source, target ->
            source.execute("INSERT INTO settings VALUES('primary.focus','strength')")
            val snapshot = SqliteCoreBackup(source).snapshot()
            SqliteCoreBackupRestore(target).populate(snapshot.core)
            assertEquals(snapshot.data, SqliteCoreBackup(target).snapshot().data)
            assertNull(SqliteAthleteProfile(target).load())
            assertNull(SqliteBaselines(target) { at }.manualBaselineRecordedAt())
            assertTrue(SqliteActivities(target) { at }.activities().isEmpty())
        }

    @Test
    fun existingLateTableOrImportCheckpointRejectsBeforeAnyWriteInsteadOfPartiallyOverwriting() =
        storage { source, target ->
            val core = seed(source)
            for ((insert, clear) in
                listOf(
                    "INSERT INTO plan_adjustments VALUES('existing',NULL,NULL,'{}')" to
                        "DELETE FROM plan_adjustments",
                    "INSERT INTO import_checkpoints VALUES('health-connect','existing-token','2026-11-16T00:00:00Z')" to
                        "DELETE FROM import_checkpoints",
                    "INSERT INTO settings VALUES('keep','existing')" to "DELETE FROM settings",
                )) {
                target.execute(insert)
                val before = target.query("SELECT total_changes()")
                assertFails { SqliteCoreBackupRestore(target).populate(core) }
                assertEquals(before, target.query("SELECT total_changes()"))
                assertNull(SqliteAthleteProfile(target).load())
                target.execute(clear)
            }
        }

    @Test
    fun lateDatabaseFailureAndCallerRollbackDoNotLeaveHalfRestoredHistory() =
        storage { source, target ->
            val core = seed(source)
            val failure =
                object : SqlDatabase by target {
                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        if (sql.startsWith("INSERT INTO plan_adjustments"))
                            error("Simulated late restore failure")
                        target.execute(sql, arguments)
                    }
                }
            assertFails { SqliteCoreBackupRestore(failure).populate(core) }
            assertNull(SqliteAthleteProfile(target).load())
            assertTrue(SqliteActivities(target) { at }.activities().isEmpty())
            assertTrue(SqliteResults(target).results().isEmpty())
            assertTrue(SqlitePlannedWorkouts(target).plannedWorkouts().isEmpty())
            assertFails {
                target.transaction {
                    SqliteCoreBackupRestore(target).populate(core)
                    assertEquals(1, SqliteActivities(target) { at }.activities().size)
                    error("Reject enclosing complete replacement")
                }
            }
            assertTrue(SqliteActivities(target) { at }.activities().isEmpty())
            SqliteCoreBackupRestore(target).populate(core)
            assertEquals(
                SqliteCoreBackup(source).snapshot().data,
                SqliteCoreBackup(target).snapshot().data,
            )
        }

    @Test
    fun retainedRawTablesAreUntouchedAndRemainHydratableAfterSummaryPopulation() =
        storage { source, target ->
            val core = seed(source)
            target.execute(
                "INSERT INTO recording_sessions VALUES('run','FINISHED',?,0,1000,'actual')",
                listOf(SqlValue.Text(at.toString())),
            )
            val point =
                org.racedaydreams.domain.recording.RunMetricsEngine.Point(
                    org.racedaydreams.domain.recording.RunMetricsEngine.Fix(
                        at,
                        0,
                        51.5,
                        -.12,
                        4.0,
                        null,
                        null,
                        null,
                    ),
                    0.0,
                    0,
                    0,
                    false,
                    null,
                    null,
                )
            target.execute(
                "INSERT INTO recording_points VALUES('run',0,?)",
                listOf(SqlValue.Text(RecordingPointJson.encode(point))),
            )
            val before = target.query("SELECT * FROM recording_points")
            SqliteCoreBackupRestore(target).populate(core)
            assertEquals(before, target.query("SELECT * FROM recording_points"))
            val detail = SqliteActivities(target) { at }.activityDetail("actual")!!
            assertEquals(5000.0, detail.distanceMetres())
            assertEquals(1, detail.samples().size)
            assertEquals(
                core.bestEfforts.single().result,
                SqliteResults(target).results().first { it.source() == ResultSource.ROLLING },
            )
        }
}
