package org.racedaydreams.storage

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.recording.RunMetricsEngine.*
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.*

abstract class RetentionPolicyRepositoryContract {
    abstract fun database(): SqlDatabase

    private val now = StoredInstant.parse("2026-11-17T09:00:00.123456789Z")
    private val old = StoredInstant.parse("2020-01-01T09:00:00.123456789Z")
    private var sequence = 0

    private fun repo(db: SqlDatabase, replacing: () -> Boolean = { false }) =
        SqliteRetentionPolicy(db, { "policy-choice-${++sequence}" }, replacing)

    private fun storage(block: (SqlDatabase) -> Unit) {
        val db = database()
        try {
            ApplicationSchema.open(db) {
                ApplicationSchema.NewInstallation(now.toString(), "retention-policy-test")
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
                        "Retained summary",
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
            val point =
                Point(
                    Fix(end, 60000, 51.5, -.12, 3.0, null, null, null),
                    100.0,
                    60000,
                    0,
                    false,
                    null,
                    null,
                )
            db.execute(
                "INSERT INTO recording_points VALUES('run',0,?)",
                listOf(SqlValue.Text(RecordingPointJson.encode(point))),
            )
            block(db)
        } finally {
            db.close()
        }
    }

    private fun later(seconds: Long) =
        Instants.ofEpochSecond(now.getEpochSecond() + seconds, now.getNano().toLong())

    private fun evidence(db: SqlDatabase) =
        listOf(
                "recording_sessions",
                "recording_points",
                "recording_events",
                "activities",
                "best_efforts",
                "preserved_best_efforts",
                "recording_detail_removals",
            )
            .associateWith { db.query("SELECT * FROM $it") }

    @Test
    fun applicationOwnerPreviewsAndSavesWithoutRemovingEvidence() = storage { db ->
        val owner = LocalBackupOperations(db, { now }) { "owner-choice-${++sequence}" }
        val policy = DetailRetention.Policy.LAST_5_YEARS
        val before = evidence(db)
        val previous = owner.retentionPreferences()
        assertEquals(1, owner.previewRetention(policy).recordings().size)
        assertEquals(previous, owner.retentionPreferences())
        owner.writeApp {
            assertFailsWith<IllegalStateException> { owner.acknowledgeRetention(policy) }
        }
        owner.acknowledgeRetention(policy)
        assertEquals(policy, owner.retentionPreferences().policy())
        assertEquals(DetailRetention.Choice.ACKNOWLEDGED, owner.retentionPreferences().choice())
        assertEquals(before, evidence(db))
    }

    @Test
    fun ownerRequiresFreshApprovalAndSerializesCleanupWithBackup() = storage { db ->
        var clock = now
        val owner = LocalBackupOperations(db, { clock }) { "cleanup-choice-${++sequence}" }
        val policy = DetailRetention.Policy.LAST_24_MONTHS
        owner.acknowledgeRetention(policy)
        val preview = owner.previewRetention(policy)
        val approval = owner.approveRetentionCleanup(preview)
        owner.writeApp {
            assertFailsWith<IllegalStateException> { owner.cleanRecordingDetail(approval, "run") }
        }
        clock = later(86401)
        assertFails { owner.cleanRecordingDetail(approval, "run") }
        assertEquals(1L, owner.recordingInventory().usage.pointRows)
        val fresh = owner.approveRetentionCleanup(owner.previewRetention(policy))
        assertTrue(owner.cleanRecordingDetail(fresh, "run"))
        assertFalse(owner.cleanRecordingDetail(fresh, "run"))
        val usage = owner.recordingInventory().usage
        assertEquals(0L, usage.pointRows)
        assertEquals(1L, usage.activityRows)
    }

    @Test
    fun existingUserCanPreviewConsequencesButCannotApproveUntilInformedAcknowledgement() =
        storage { db ->
            db.execute(
                "UPDATE detail_retention_preferences SET policy='KEEP_FOREVER',choice='EXISTING_UNREVIEWED',chosen_at=NULL,choice_id=NULL"
            )
            val r = repo(db)
            val before = evidence(db)
            val preview = r.preview(DetailRetention.Policy.LAST_24_MONTHS, now)
            assertEquals(1, preview.recordings().size)
            assertFails { r.approve(preview, now) }
            assertEquals(
                DetailRetention.Preferences.existing(),
                SqliteRetentionPreferences(db).load(),
            )
            r.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val approval = r.approve(preview, now)
            assertEquals(SqliteRetentionPreferences(db).load().choiceId(), approval.choiceId())
            assertEquals(before, evidence(db))
        }

    @Test
    fun allThreeChoicesPersistExactSourceTimestampAndRoundTripInExistingBackupMetadata() =
        storage { db ->
            val r = repo(db)
            val identities = mutableSetOf<String>()
            for (policy in DetailRetention.Policy.entries) {
                r.acknowledge(policy, now)
                val current = SqliteRetentionPreferences(db).load()
                assertEquals(policy, current.policy())
                assertEquals(DetailRetention.Choice.ACKNOWLEDGED, current.choice())
                assertEquals(now, current.chosenAt())
                assertTrue(identities.add(requireNotNull(current.choiceId())))
                val backup = SqliteApplicationBackup(db).encode(now)
                assertEquals(
                    current,
                    UserBackupDataJson.inspect(backup).snapshot.retention.preferences,
                )
                if (policy == DetailRetention.Policy.KEEP_FOREVER) {
                    val preview = r.preview(policy, now)
                    assertTrue(preview.recordings().isEmpty())
                    assertFails { r.approve(preview, now) }
                }
            }
        }

    @Test
    fun reusedBlankOrOversizeChoiceIdentityRejectsBeforeAnyPreferenceWrite() = storage { db ->
        val current = SqliteRetentionPreferences(db).load()
        var writes = 0
        val watched =
            object : SqlDatabase by db {
                override fun execute(sql: String, arguments: List<SqlValue>) {
                    writes++
                    db.execute(sql, arguments)
                }
            }
        for (id in listOf(requireNotNull(current.choiceId()), " ", "x".repeat(257))) {
            assertFails {
                SqliteRetentionPolicy(watched, { id }, { false })
                    .acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            }
            assertEquals(0, writes)
            assertEquals(current, SqliteRetentionPreferences(db).load())
        }
    }

    @Test
    fun restoredDatasetClockChangesAndExpiredPreviewRejectButExactDeadlineRemainsSupported() =
        storage { db ->
            val r = repo(db)
            r.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val preview = r.preview(DetailRetention.Policy.LAST_24_MONTHS, now)
            assertFails { r.approve(preview, later(-1)) }
            assertNotNull(r.approve(preview, later(86400)))
            assertFails { r.approve(preview, later(86401)) }
            db.execute("UPDATE detail_retention_state SET epoch='restored-dataset'")
            assertFails { r.approve(preview, now) }
        }

    @Test
    fun actualActivePausedRecoverableAndReplacementOperationStateAllBlockApproval() =
        storage { db ->
            var replacing = false
            val r = repo(db) { replacing }
            r.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val preview = r.preview(DetailRetention.Policy.LAST_24_MONTHS, now)
            db.execute(
                "INSERT INTO recording_sessions VALUES('live','ACTIVE',?,0,1000,NULL)",
                listOf(SqlValue.Text(now.toString())),
            )
            for (state in listOf("ACTIVE", "PAUSED", "RECOVERABLE")) {
                db.execute(
                    "UPDATE recording_sessions SET state=? WHERE id='live'",
                    listOf(SqlValue.Text(state)),
                )
                assertFails { r.approve(preview, now) }
            }
            db.execute("DELETE FROM recording_sessions WHERE id='live'")
            replacing = true
            assertFails { r.approve(preview, now) }
            replacing = false
            assertNotNull(r.approve(preview, now))
            assertEquals(1, db.query("SELECT * FROM recording_points").size)
        }

    @Test
    fun everyPolicyReadAndWriteAndRequiredOperationCheckSharesTheSameDatabaseScope() =
        storage { db ->
            var depth = 0
            val watched =
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

                    override fun query(
                        sql: String,
                        arguments: List<SqlValue>,
                    ): List<List<SqlValue>> {
                        assertTrue(depth > 0)
                        return db.query(sql, arguments)
                    }

                    override fun execute(sql: String, arguments: List<SqlValue>) {
                        assertTrue(depth > 0)
                        assertTrue(sql.startsWith("UPDATE detail_retention_preferences"))
                        db.execute(sql, arguments)
                    }
                }
            val r =
                repo(watched) {
                    assertTrue(depth > 0)
                    false
                }
            r.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
            val before = db.query("SELECT total_changes()")
            r.approve(r.preview(DetailRetention.Policy.LAST_24_MONTHS, now), now)
            assertEquals(before, db.query("SELECT total_changes()"))
        }

    @Test
    fun failedPreferenceWriteRetainsConsentAndAllRawAndPermanentEvidence() = storage { db ->
        val before = SqliteRetentionPreferences(db).load()
        val raw = evidence(db)
        db.execute(
            "CREATE TRIGGER reject_policy BEFORE UPDATE ON detail_retention_preferences BEGIN SELECT RAISE(ABORT,'simulated policy write failure'); END"
        )
        assertFails { repo(db).acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now) }
        assertEquals(before, SqliteRetentionPreferences(db).load())
        assertEquals(raw, evidence(db))
        db.execute("DROP TRIGGER reject_policy")
        repo(db).acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
        assertEquals(
            DetailRetention.Choice.ACKNOWLEDGED,
            SqliteRetentionPreferences(db).load().choice(),
        )
    }

    @Test
    fun callerRollbackRetainsOriginalChoiceAndNeverTurnsPreviewOrApprovalIntoDeletion() =
        storage { db ->
            val r = repo(db)
            val before = SqliteRetentionPreferences(db).load()
            val raw = evidence(db)
            assertFails {
                db.transaction {
                    r.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, now)
                    r.approve(r.preview(DetailRetention.Policy.LAST_24_MONTHS, now), now)
                    error("Caller rejects consent change")
                }
            }
            assertEquals(before, SqliteRetentionPreferences(db).load())
            assertEquals(raw, evidence(db))
        }
}
