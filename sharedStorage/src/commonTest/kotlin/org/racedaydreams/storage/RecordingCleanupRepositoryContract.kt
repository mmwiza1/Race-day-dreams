package org.racedaydreams.storage

import kotlin.test.*
import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

abstract class RecordingCleanupRepositoryContract {
    abstract fun database(): SqlDatabase

    abstract fun directory(): String

    private val old = StoredInstant.parse("2020-01-01T09:00:00.123456789Z")
    private val now = StoredInstant.parse("2026-11-17T09:00:00.123456789Z")
    private var choices = 0

    private class Operation : RecordingDetailOperation {
        var busy = false
        var replacing = false
        var inside = false

        override fun exclusive(block: () -> Boolean): Boolean {
            check(!busy) { "Another data operation is running" }
            busy = true
            inside = true
            return try {
                block()
            } finally {
                inside = false
                busy = false
            }
        }

        override fun replacementInProgress() = replacing
    }

    private fun policy(db: SqlDatabase, op: Operation) =
        SqliteRetentionPolicy(db, { "cleanup-choice-${++choices}" }, { op.replacing })

    private fun approve(db: SqlDatabase, op: Operation): DetailRetention.Approval {
        val p = policy(db, op)
        p.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
        return p.approve(p.preview(DetailRetention.Policy.LAST_24_MONTHS, now), now)
    }

    private fun storage(block: (SqlDatabase, Operation) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "cleanup-test")
            }
            val end = Instants.ofEpochSecond(old.getEpochSecond() + 60, old.getNano().toLong())
            SqliteActivities(db) { now }
                .upsertManualActivity(
                    ActivityRecord(
                        "activity",
                        SourceIdentity("rdd", "run", null),
                        old,
                        end,
                        null,
                        null,
                        100.0,
                        Durations.ofSeconds(60),
                        null,
                        null,
                        null,
                        null,
                        null,
                        "Retain original measured summary",
                        emptyList(),
                        emptyList(),
                        null,
                        null,
                    )
                )
            db.execute(
                "INSERT INTO recording_sessions VALUES('run','FINISHED',?,0,60000,'activity')",
                listOf(SqlValue.Text(old.toString())),
            )
            for (i in 0..1) {
                val p =
                    Point(
                        Fix(
                            if (i == 0) old else end,
                            i * 60000L,
                            51.5 + i * .001,
                            -.12,
                            3.0,
                            null,
                            null,
                            null,
                        ),
                        i * 100.0,
                        i * 60000L,
                        0,
                        false,
                        null,
                        null,
                    )
                db.execute(
                    "INSERT INTO recording_points VALUES('run',?,?)",
                    listOf(
                        SqlValue.Integer(i.toLong()),
                        SqlValue.Text(RecordingPointJson.encode(p)),
                    ),
                )
            }
            db.execute(
                "INSERT INTO recording_events VALUES('run',0,?)",
                listOf(SqlValue.Text("""{"type":"GPS_RECOVERED","mono":0,"wall":"$old"}""")),
            )
            val result =
                RecordedResult(
                    "retained-pb",
                    PerformanceDistance("100m", 100.0, "100m"),
                    Durations.ofSeconds(60),
                    CalendarDates.of(2020, 1, 1),
                    "Retain PB",
                    ResultSource.ROLLING,
                    "activity",
                    null,
                )
            db.execute(
                "INSERT INTO preserved_best_efforts VALUES('retained-pb','activity','100m','rdd','run',NULL,?)",
                listOf(SqlValue.Text(ResultJson.encode(result))),
            )
            db.execute("INSERT INTO settings VALUES('permanent-setting','keep')")
            block(db, Operation())
        } finally {
            db.close()
        }
    }

    private fun permanent(db: SqlDatabase) =
        db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
            )
            .map { it.single().sqlText() }
            .filter {
                it !in listOf("recording_points", "recording_events", "recording_detail_removals")
            }
            .associateWith { db.query("SELECT * FROM $it") }

    private fun raw(db: SqlDatabase) =
        listOf("recording_points", "recording_events", "recording_detail_removals").associateWith {
            db.query("SELECT * FROM $it")
        }

    private fun later(seconds: Long) =
        Instants.ofEpochSecond(now.getEpochSecond() + seconds, now.getNano().toLong())

    @Test
    fun removesOnlyApprovedRawRowsAndPreservesAllOtherTablesAndExactSourceHashes() =
        storage { db, op ->
            val approval = approve(db, op)
            val before = permanent(db)
            val points = SqliteRecordingEvidence(db).evidence("recording_points", "run")
            val events = SqliteRecordingEvidence(db).evidence("recording_events", "run")
            assertTrue(SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now))
            assertEquals(before, permanent(db))
            val receipt = SqliteRecordingDetailReceipts(db).removal("run")!!
            assertEquals(points.count, receipt.points())
            assertEquals(points.sha256, receipt.pointsHash())
            assertEquals(events.count, receipt.events())
            assertEquals(events.sha256, receipt.eventsHash())
            assertEquals(approval.preview().recordings().single().endedAt(), receipt.endedAt())
            assertTrue(db.query("SELECT * FROM recording_points").isEmpty())
            assertTrue(db.query("SELECT * FROM recording_events").isEmpty())
            assertEquals(1, SqliteResults(db).results().size)
            assertTrue(db.query("PRAGMA foreign_key_check").isEmpty())
        }

    @Test
    fun completedRetryIsEvidenceIdempotentButConsentMustStillBeCurrent() = storage { db, op ->
        val approval = approve(db, op)
        val cleaner = SqliteRecordingDetailCleanup(db, op)
        assertTrue(cleaner.cleanOne(approval, "run", now))
        val before = raw(db)
        val history = permanent(db)
        assertFalse(cleaner.cleanOne(approval, "run", later(1)))
        assertEquals(before, raw(db))
        assertEquals(history, permanent(db))
        policy(db, op).acknowledge(DetailRetention.Policy.LAST_24_MONTHS, later(1))
        assertFails { cleaner.cleanOne(approval, "run", later(1)) }
        assertEquals(before, raw(db))
    }

    @Test
    fun restoredEpochChangedChoiceBackwardClockAndExpiryCannotReuseApproval() {
        for (change in listOf("epoch", "choice", "clock", "expiry")) storage { db, op ->
            val approval = approve(db, op)
            if (change == "epoch") db.execute("UPDATE detail_retention_state SET epoch='restored'")
            if (change == "choice")
                policy(db, op).acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val before = raw(db)
            val history = permanent(db)
            assertFails {
                SqliteRecordingDetailCleanup(db, op)
                    .cleanOne(
                        approval,
                        "run",
                        when (change) {
                            "clock" -> later(-1)
                            "expiry" -> later(86401)
                            else -> now
                        },
                    )
            }
            assertEquals(before, raw(db))
            assertEquals(history, permanent(db))
        }
    }

    @Test
    fun activePausedAndRecoverableRunsProtectEveryCleanupIncludingRetries() = storage { db, op ->
        val approval = approve(db, op)
        db.execute(
            "INSERT INTO recording_sessions VALUES('live','ACTIVE',?,0,1000,NULL)",
            listOf(SqlValue.Text(now.toString())),
        )
        for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
            db.execute(
                "UPDATE recording_sessions SET state=? WHERE id='live'",
                listOf(SqlValue.Text(state)),
            )
            val before = raw(db)
            assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
            assertEquals(before, raw(db))
        }
    }

    @Test
    fun requiredOperationBoundaryRejectsBusyBackupAndRechecksReplacementState() =
        storage { db, op ->
            val approval = approve(db, op)
            val before = raw(db)
            var touches = 0
            val watched =
                object : SqlDatabase by db {
                    override fun <T> transaction(block: () -> T): T {
                        touches++
                        return db.transaction(block)
                    }
                }
            op.busy = true
            assertFails { SqliteRecordingDetailCleanup(watched, op).cleanOne(approval, "run", now) }
            assertEquals(0, touches)
            op.busy = false
            op.replacing = true
            assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
            assertEquals(before, raw(db))
            assertFalse(op.busy)
        }

    @Test
    fun unreviewedOrKeepForeverPreferenceCannotBeBypassedWithPreviouslyApprovedValue() {
        for (unreviewed in listOf(true, false)) storage { db, op ->
            val approval = approve(db, op)
            if (unreviewed)
                db.execute(
                    "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
                )
            else policy(db, op).acknowledge(DetailRetention.Policy.KEEP_FOREVER, now)
            val before = raw(db)
            assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
            assertEquals(before, raw(db))
        }
    }

    @Test
    fun changedCountsOrSourceOrEndIndexRequireAnotherPreview() {
        for (change in listOf("counts", "source", "end")) storage { db, op ->
            val approval = approve(db, op)
            when (change) {
                "counts" -> db.execute("DELETE FROM recording_events")
                "source" -> db.execute("UPDATE activities SET provider='health_connect'")
                else -> db.execute("UPDATE activities SET end_millis=end_millis+1")
            }
            val before = raw(db)
            val history = permanent(db)
            assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
            assertEquals(before, raw(db))
            assertEquals(history, permanent(db))
        }
    }

    @Test
    fun sessionOutsideApprovedPreviewIsNeverDeleted() = storage { db, op ->
        val approval = approve(db, op)
        val before = raw(db)
        assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "not-approved", now) }
        assertEquals(before, raw(db))
    }

    @Test
    fun actualPayloadIdentityConflictRejectsWithoutDeletingRawEvidence() = storage { db, op ->
        val approval = approve(db, op)
        val a =
            ActivityJson.decode(
                db.query("SELECT payload FROM activities").single().single().sqlText()
            )
        db.execute(
            "UPDATE activities SET payload=?",
            listOf(SqlValue.Text(ActivityJson.encode(a.copy(id = "different-identity")))),
        )
        val before = raw(db)
        assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
        assertEquals(before, raw(db))
    }

    @Test
    fun availableDetailNeverOverwritesPreExistingRemovalProvenance() = storage { db, op ->
        val approval = approve(db, op)
        SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now)
        // A partially imported/conflicting stream must not silently replace an earlier receipt.
        db.execute("INSERT INTO recording_events VALUES('run',0,'{}')")
        val before = raw(db)
        assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
        assertEquals(before, raw(db))
    }

    @Test
    fun mismatchedExistingReceiptCannotBecomeASuccessfulNoOp() = storage { db, op ->
        val approval = approve(db, op)
        val cleaner = SqliteRecordingDetailCleanup(db, op)
        cleaner.cleanOne(approval, "run", now)
        db.execute("UPDATE recording_detail_removals SET point_count=point_count+1")
        val before = raw(db)
        assertFails { cleaner.cleanOne(approval, "run", now) }
        assertEquals(before, raw(db))
    }

    @Test
    fun lateEventDeleteFailureRollsBackPointDeletionAndReceiptAndCanBeRetried() =
        storage { db, op ->
            val approval = approve(db, op)
            val before = raw(db)
            val history = permanent(db)
            db.execute(
                "CREATE TRIGGER fail_events BEFORE DELETE ON recording_events BEGIN SELECT RAISE(ABORT,'event deletion failed'); END"
            )
            val cleaner = SqliteRecordingDetailCleanup(db, op)
            assertFails { cleaner.cleanOne(approval, "run", now) }
            assertEquals(before, raw(db))
            assertEquals(history, permanent(db))
            assertFalse(op.busy)
            db.execute("DROP TRIGGER fail_events")
            assertTrue(cleaner.cleanOne(approval, "run", now))
        }

    @Test
    fun unexpectedDeleteCountRollsBackEvenWhenDatabaseDoesNotThrow() = storage { db, op ->
        val approval = approve(db, op)
        val before = raw(db)
        db.execute(
            "CREATE TRIGGER skip_point BEFORE DELETE ON recording_points WHEN OLD.sequence=1 BEGIN SELECT RAISE(IGNORE); END"
        )
        assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
        assertEquals(before, raw(db))
    }

    @Test
    fun rejectedOuterTransactionRestoresAllEvidenceAndRemovalMetadata() = storage { db, op ->
        val approval = approve(db, op)
        val before = raw(db)
        val history = permanent(db)
        assertFails {
            db.transaction {
                assertTrue(SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now))
                error("Caller rejects")
            }
        }
        assertEquals(before, raw(db))
        assertEquals(history, permanent(db))
    }

    @Test
    fun removedDetailAndCurrentBackupsRemainTruthfulAndCompatibleArchiveRestoresExactEvidence() =
        storage { db, op ->
            val exported = SqliteRecordingArchive(db).export(directory())
            val archive =
                try {
                    InspectedRecordingArchive.inspect(exported, directory())
                } finally {
                    exported.close()
                }
            try {
                val approval = approve(db, op)
                val source = raw(db)
                val history = permanent(db)
                SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now)
                val receipt = SqliteRecordingDetailReceipts(db).removal("run")!!
                val json = SqliteApplicationBackup(db).encode(now)
                assertEquals(
                    listOf(receipt),
                    UserBackupDataJson.inspect(json).snapshot.retention.removals,
                )
                val pruned = SqliteCompleteBackup(db).export(directory(), now)
                try {
                    val inspected = InspectedBackupPackage.inspect(pruned, directory())
                    try {
                        val run = inspected.recordings.sessions.getValue("run")
                        assertEquals(receipt, run.removal)
                        assertEquals(0, run.points)
                        assertEquals(0L, run.events)
                    } finally {
                        inspected.close()
                    }
                } finally {
                    pruned.close()
                }
                SqliteRecordingArchiveRestore(db) { "restored-after-cleanup" }.merge(archive)
                assertEquals(source, raw(db))
                assertNull(SqliteRecordingDetailReceipts(db).removal("run"))
                // Restore invalidates approvals; every other permanent row remains exact.
                val after = permanent(db)
                assertEquals(history - "detail_retention_state", after - "detail_retention_state")
                assertFails { SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now) }
            } finally {
                archive.close()
            }
        }

    @Test
    fun oversizedRetainedActivityRejectsBeforePayloadAllocationOrAnyDeletion() = storage { db, op ->
        val approval = approve(db, op)
        db.execute(
            "UPDATE activities SET payload=zeroblob(?)",
            listOf(SqlValue.Integer(BackupEnvelopeJson.MAX_BYTES.toLong() + 1)),
        )
        val before = raw(db)
        var payloadReads = 0
        val watched =
            object : SqlDatabase by db {
                override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> {
                    if (sql.startsWith("SELECT payload FROM activities")) payloadReads++
                    return db.query(sql, arguments)
                }
            }
        assertFails { SqliteRecordingDetailCleanup(watched, op).cleanOne(approval, "run", now) }
        assertEquals(0, payloadReads)
        assertEquals(before, raw(db))
    }

    @Test
    fun legacyInlineEvidenceIsRetainedExactlyRatherThanTreatedAsDisposableRawRows() =
        storage { db, op ->
            val full = SqliteActivities(db) { now }.activityDetail("activity")!!
            assertEquals(2, full.samples().size)
            val payload = ActivityJson.encode(full)
            db.execute("UPDATE activities SET payload=?", listOf(SqlValue.Text(payload)))
            val approval = approve(db, op)
            val before = permanent(db)
            assertTrue(SqliteRecordingDetailCleanup(db, op).cleanOne(approval, "run", now))
            assertEquals(before, permanent(db))
            assertEquals(
                payload,
                db.query("SELECT payload FROM activities").single().single().sqlText(),
            )
            assertEquals(
                full.samples(),
                SqliteActivities(db) { now }.activityDetail("activity")!!.samples(),
            )
        }

    @Test
    fun actualApplicationOperationGuardPreventsOverlapAndAllowsOnlyExplicitFreshRetry() =
        storage { db, _ ->
            val operations = LocalDataOperations()
            val policy =
                SqliteRetentionPolicy(
                    db,
                    { "actual-operation-choice" },
                    operations::replacementInProgress,
                )
            policy.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val preview = policy.preview(DetailRetention.Policy.LAST_24_MONTHS, now)
            val approval = policy.approve(preview, now)
            val cleaner = SqliteRecordingDetailCleanup(db, operations)
            val before = raw(db)
            operations.backup {
                assertFails { policy.approve(preview, now) }
                assertFails { cleaner.cleanOne(approval, "run", now) }
            }
            operations.replace { assertFails { cleaner.cleanOne(approval, "run", now) } }
            assertEquals(before, raw(db))
            assertTrue(cleaner.cleanOne(approval, "run", now))
            assertFalse(cleaner.cleanOne(approval, "run", now))
        }

    @Test
    fun operationAndTransactionScopesEncloseEveryQueryAndOnlyRawAndReceiptWritesAreAllowed() =
        storage { db, op ->
            val approval = approve(db, op)
            var depth = 0
            val watched =
                object : SqlDatabase by db {
                    override fun <T> transaction(block: () -> T): T =
                        db.transaction {
                            assertTrue(op.inside)
                            depth++
                            try {
                                block()
                            } finally {
                                depth--
                            }
                        }

                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        assertTrue(op.inside && depth > 0)
                        return db.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        assertTrue(op.inside && depth > 0)
                        assertTrue(
                            sql.startsWith("DELETE FROM recording_points ") ||
                                sql.startsWith("DELETE FROM recording_events ") ||
                                sql.startsWith("INSERT INTO recording_detail_removals(")
                        )
                        db.execute(sql, arguments)
                    }
                }
            assertTrue(SqliteRecordingDetailCleanup(watched, op).cleanOne(approval, "run", now))
            assertFalse(op.inside)
        }
}
