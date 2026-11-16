package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.storage.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class RetentionRestoreRepositoryContract {
    abstract fun database(): SqlDatabase

    private val at = StoredInstant.parse("2026-11-16T09:00:00.123456789Z")
    private val emptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    private val payload = """{"distance":100.0,"note":"é😀"}"""

    private fun storage(block: (SqlDatabase) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(at.toString(), "retention-restore-test")
            }
            return block(db)
        } finally {
            db.close()
        }
    }

    private fun seed(
        db: SqlDatabase,
        id: String = "run",
        points: Boolean = true,
    ): RemovedRecordingDetail {
        val actual =
            ActivityRecord(
                "activity-$id",
                SourceIdentity("rdd", id, null),
                at,
                Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
                null,
                null,
                100.0,
                Durations.ofSeconds(60),
                null,
                null,
                null,
                null,
                null,
                "Retained summary",
                emptyList(),
                emptyList(),
                null,
                null,
            )
        SqliteActivities(db) { at }.upsertManualActivity(actual)
        db.execute(
            "INSERT INTO recording_sessions VALUES(?,'FINISHED',?,0,1000,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(at.toString()), SqlValue.Text(actual.id())),
        )
        if (points)
            db.execute(
                "INSERT INTO recording_points VALUES(?,9007199254740993,?)",
                listOf(SqlValue.Text(id), SqlValue.Text(payload)),
            )
        return RemovedRecordingDetail(
            id,
            actual.id(),
            "rdd",
            id,
            actual.end(),
            Instants.ofEpochSecond(at.getEpochSecond() + 120),
            DetailRetention.Policy.LAST_24_MONTHS,
            at,
            0,
            1000,
            1,
            0,
            "bda462373ba105299341c683fc034913fe8f53573b35a7d764fccaf5d41ce229",
            emptyHash,
        )
    }

    private fun archive(vararg rows: RemovedRecordingDetail) =
        DetailRetentionArchiveJson.Snapshot(
            DetailRetention.Preferences.acknowledged(
                DetailRetention.Policy.LAST_5_YEARS,
                at,
                "backup-choice",
            ),
            rows.toList(),
        )

    private fun epoch(db: SqlDatabase) =
        db.query("SELECT epoch FROM detail_retention_state").single().single().sqlText()

    @Test
    fun hashesUseOriginalStoredTextExactIntegerSequenceAndUtf8WithoutJsonNormalisation() =
        storage { db ->
            val proof = seed(db)
            val before = db.query("SELECT total_changes()")
            SqliteRecordingEvidence(db).requireMatches(proof)
            assertEquals(before, db.query("SELECT total_changes()"))
            db.execute(
                "UPDATE recording_points SET payload=?",
                listOf(SqlValue.Text(payload.replace("100.0", "100"))),
            )
            assertFails { SqliteRecordingEvidence(db).requireMatches(proof) }
            db.execute(
                "UPDATE recording_points SET payload=?,sequence=9007199254740992",
                listOf(SqlValue.Text(payload)),
            )
            assertFails { SqliteRecordingEvidence(db).requireMatches(proof) }
            db.execute("UPDATE recording_points SET sequence=9007199254740993")
            SqliteRecordingEvidence(db).requireMatches(proof)
        }

    @Test
    fun boundedKeysetHashReadsEveryRowAndRejectsOversizeBeforeHydration() = storage { db ->
        seed(db, points = false)
        db.transaction {
            repeat(1100) { i ->
                db.execute(
                    "INSERT INTO recording_points VALUES('run',?,?)",
                    listOf(SqlValue.Integer(i.toLong()), SqlValue.Text("x".repeat(2048))),
                )
            }
        }
        var payloadPages = 0
        val bounded =
            object : SqlDatabase by db {
                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> {
                    assertTrue(arguments.size <= 513)
                    val rows = db.query(sql, arguments)
                    if (sql.startsWith("SELECT sequence,payload")) {
                        payloadPages++
                        assertTrue(
                            rows.sumOf { it[1].sqlText().encodeToByteArray().size.toLong() } <=
                                1024 * 1024
                        )
                    }
                    return rows
                }
            }
        val result =
            db.transaction { SqliteRecordingEvidence(bounded).evidence("recording_points", "run") }
        assertEquals(1100L, result.count)
        assertEquals(
            "3ce1434b920fc428a0be28ed9764458768e89bed03d0d53d14bac388389b09c6",
            result.sha256,
        )
        assertTrue(payloadPages >= 3)
        db.execute(
            "UPDATE recording_points SET payload=zeroblob(?) WHERE sequence=0",
            listOf(SqlValue.Integer(RecordingArchiveRows.MAX_LINE.toLong() * 4 + 1)),
        )
        val before = payloadPages
        assertFails { SqliteRecordingEvidence(bounded).requireMatches(seedProof()) }
        assertEquals(before, payloadPages)
        assertFails { SqliteRecordingEvidence(db).evidence("activities", "run") }
    }

    private fun seedProof() =
        RemovedRecordingDetail(
            "run",
            "activity-run",
            "rdd",
            "run",
            Instants.ofEpochSecond(at.getEpochSecond() + 60, at.getNano().toLong()),
            Instants.ofEpochSecond(at.getEpochSecond() + 120),
            DetailRetention.Policy.LAST_24_MONTHS,
            at,
            0,
            1000,
            1,
            0,
            "bda462373ba105299341c683fc034913fe8f53573b35a7d764fccaf5d41ce229",
            emptyHash,
        )

    @Test
    fun matchingAvailableDetailClearsOnlyReceiptAndMissingDetailStaysTruthfullyRemoved() =
        storage { db ->
            val available = seed(db)
            val missing = seed(db, "missing", false)
            val summaries = SqliteActivities(db) { at }.activitySummaries()
            val raw = db.query("SELECT * FROM recording_points")
            SqliteRetentionRestore(db) { "fresh-restored-dataset" }
                .replace(archive(available, missing))
            assertNull(SqliteRecordingDetailReceipts(db).removal("run"))
            assertEquals(missing, SqliteRecordingDetailReceipts(db).removal("missing"))
            assertEquals(raw, db.query("SELECT * FROM recording_points"))
            assertEquals(summaries, SqliteActivities(db) { at }.activitySummaries())
            assertEquals("fresh-restored-dataset", epoch(db))
            assertEquals(archive().preferences, SqliteRetentionPreferences(db).load())
            assertTrue(db.query("SELECT * FROM recording_events").isEmpty())
            // Repeated restore doesn't invent detail and invalidates the previous dataset again.
            SqliteRetentionRestore(db) { "fresh-second-dataset" }
                .replace(archive(available, missing))
            assertEquals(listOf(missing), SqliteRecordingDetailReceipts(db).snapshot().removals)
            assertEquals(raw, db.query("SELECT * FROM recording_points"))
        }

    @Test
    fun conflictInLaterReceiptCannotChangeAnyPreferencesReceiptsOrRecordedHistory() =
        storage { db ->
            val good = seed(db, "good", false)
            val conflict = seed(db, "conflict")
            db.execute("UPDATE recording_points SET payload='different original text'")
            val before = SqliteRecordingDetailReceipts(db).snapshot()
            val oldEpoch = epoch(db)
            val changes = db.query("SELECT total_changes()")
            assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(good, conflict)) }
            assertEquals(changes, db.query("SELECT total_changes()"))
            assertEquals(before.preferences, SqliteRetentionPreferences(db).load())
            assertEquals(before.removals, SqliteRecordingDetailReceipts(db).snapshot().removals)
            assertEquals(oldEpoch, epoch(db))
            assertEquals(
                "different original text",
                db.query("SELECT payload FROM recording_points").single().single().sqlText(),
            )
            db.execute("UPDATE activities SET record_id='another-source' WHERE id='activity-good'")
            assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(good)) }
            assertEquals(oldEpoch, epoch(db))
        }

    @Test
    fun lateFailureAndCallerRollbackRestoreTheWholeMetadataTransaction() = storage { db ->
        val proof = seed(db, points = false)
        val oldEpoch = epoch(db)
        val oldPreferences = SqliteRetentionPreferences(db).load()
        val interrupted =
            object : SqlDatabase by db {
                override fun execute(sql: String, arguments: List<SqlValue>) {
                    if (sql.startsWith("UPDATE detail_retention_state"))
                        error("Simulated final write failure")
                    db.execute(sql, arguments)
                }
            }
        assertFails { SqliteRetentionRestore(interrupted) { "failed" }.replace(archive(proof)) }
        assertEquals(oldPreferences, SqliteRetentionPreferences(db).load())
        assertTrue(SqliteRecordingDetailReceipts(db).snapshot().removals.isEmpty())
        assertEquals(oldEpoch, epoch(db))
        assertFails {
            db.transaction {
                SqliteRetentionRestore(db) { "caller-pending" }.replace(archive(proof))
                assertEquals(proof, SqliteRecordingDetailReceipts(db).removal("run"))
                error("Caller rejects enclosing app restore")
            }
        }
        assertEquals(oldPreferences, SqliteRetentionPreferences(db).load())
        assertTrue(SqliteRecordingDetailReceipts(db).snapshot().removals.isEmpty())
        assertEquals(oldEpoch, epoch(db))
        SqliteRetentionRestore(db) { "later-valid" }.replace(archive(proof))
        assertEquals(proof, SqliteRecordingDetailReceipts(db).removal("run"))
    }

    @Test
    fun restoredAcknowledgementCannotReuseAValidApprovalForThePreviousDataset() = storage { db ->
        val preferences = archive().preferences
        db.execute(
            "UPDATE detail_retention_preferences SET policy='LAST_5_YEARS',choice='ACKNOWLEDGED',chosen_at=?,choice_id='backup-choice' WHERE id=1",
            listOf(SqlValue.Text(at.toString())),
        )
        val recording =
            DetailRetention.Recording(
                "old",
                "old-activity",
                "rdd",
                "old",
                StoredInstant.parse("2020-01-01T00:00:00Z"),
                true,
                10,
                0,
                10,
            )
        val preview =
            DetailRetention.preview(
                DetailRetention.Policy.LAST_5_YEARS,
                listOf(recording),
                at,
                epoch(db),
            )
        val approval = DetailRetention.approve(preview, preferences, at, epoch(db), false, false)
        DetailRetention.checkBatch(
            approval,
            preferences,
            at,
            epoch(db),
            listOf(recording),
            false,
            false,
        )
        SqliteRetentionRestore(db) { "new-restored-dataset" }.replace(archive())
        assertEquals(preferences, SqliteRetentionPreferences(db).load())
        assertFails {
            DetailRetention.checkBatch(
                approval,
                SqliteRetentionPreferences(db).load(),
                at,
                epoch(db),
                emptyList(),
                false,
                false,
            )
        }
        assertFails {
            DetailRetention.approve(
                preview,
                SqliteRetentionPreferences(db).load(),
                at,
                epoch(db),
                false,
                false,
            )
        }
    }

    @Test
    fun countsEventsAndMissingSingletonsCannotBeHiddenByMatchingPointPayloads() = storage { db ->
        val proof = seed(db)
        assertFails { SqliteRecordingEvidence(db).requireMatches(proof.copy(points = 2)) }
        db.execute("INSERT INTO recording_events VALUES('run',0,'event bytes')")
        assertFails { SqliteRecordingEvidence(db).requireMatches(proof) }
        db.execute("DELETE FROM recording_events")
        SqliteRecordingEvidence(db).requireMatches(proof)
        db.execute("DELETE FROM detail_retention_preferences")
        val oldEpoch = epoch(db)
        assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(proof)) }
        assertTrue(db.query("SELECT * FROM detail_retention_preferences").isEmpty())
        assertEquals(oldEpoch, epoch(db))
        assertEquals(
            1L,
            db.query("SELECT COUNT(*) FROM recording_points").single().single().sqlLong(),
        )
        db.execute("DELETE FROM detail_retention_state")
        val before = db.query("SELECT total_changes()")
        assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(proof)) }
        assertEquals(before, db.query("SELECT total_changes()"))
    }

    @Test
    fun unfinishedRunAndGymRecoveryOrUnchangedEpochRejectBeforeMetadataWrites() = storage { db ->
        val proof = seed(db, points = false)
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
            db.execute("UPDATE recording_sessions SET state=?", listOf(SqlValue.Text(state)))
            val before = db.query("SELECT total_changes()")
            assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(proof)) }
            assertEquals(before, db.query("SELECT total_changes()"))
        }
        db.execute("UPDATE recording_sessions SET state='FINISHED'")
        val engine =
            GymExecutionEngine.start(
                "gym",
                GymExecution.Origin.FREE,
                null,
                null,
                Offsets.ofTotalSeconds(0),
                GymEngineArchiveFixtures.tick(0),
            )
        SqliteGymExecutions(db, { GymEngineArchiveFixtures.at }, PortableLegacyStrengthFingerprint)
            .start(engine.snapshot())
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERY_REQUIRED")) {
            db.execute("UPDATE strength_executions SET state=?", listOf(SqlValue.Text(state)))
            val before = db.query("SELECT total_changes()")
            assertFails { SqliteRetentionRestore(db) { "new" }.replace(archive(proof)) }
            assertEquals(before, db.query("SELECT total_changes()"))
        }
        db.execute("DELETE FROM strength_executions")
        val originalEpoch = epoch(db)
        assertFails { SqliteRetentionRestore(db) { originalEpoch }.replace(archive(proof)) }
        assertEquals(originalEpoch, epoch(db))
    }
}
