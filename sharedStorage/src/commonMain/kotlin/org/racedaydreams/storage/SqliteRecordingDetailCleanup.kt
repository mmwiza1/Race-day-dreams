package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.domain.time.Instant

/**
 * Required application-owned operation boundary. Its implementation must serialize against the SAME
 * application's backup/export/restore operations and invoke the block synchronously once. No idle
 * default is provided. Native UI remains disabled until the real host binds this guard.
 */
internal interface RecordingDetailOperation {
    fun exclusive(block: () -> Boolean): Boolean

    fun replacementInProgress(): Boolean
}

/** One original, approved session per transaction. Never updates summaries, actuals or PBs. */
internal class SqliteRecordingDetailCleanup(
    private val database: SqlDatabase,
    private val operation: RecordingDetailOperation,
) {
    fun cleanOne(approval: DetailRetention.Approval, sessionId: String, now: Instant): Boolean =
        operation.exclusive {
            database.transaction {
                fun check(fresh: List<DetailRetention.Recording>) =
                    DetailRetention.checkBatch(
                        approval,
                        SqliteRetentionPreferences(database).load(),
                        now,
                        database
                            .query("SELECT epoch FROM detail_retention_state WHERE id=1")
                            .single()
                            .single()
                            .sqlText(),
                        fresh,
                        database
                            .query(
                                "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1"
                            )
                            .isNotEmpty(),
                        operation.replacementInProgress(),
                    )
                // Recheck even before a no-op retry; replacement/clock/consent can invalidate it.
                check(emptyList())
                val approved =
                    requireNotNull(
                        approval.preview().recordings().firstOrNull { it.sessionId() == sessionId }
                    ) {
                        "Recording was not included in the approved preview."
                    }
                val row = SqliteRecordingInventory(database).recording(sessionId)
                val receipts = SqliteRecordingDetailReceipts(database)
                val old = receipts.removal(sessionId)
                if (row != null && row.points() == 0L && row.events() == 0L && old != null) {
                    require(
                        old.activityId() == approved.activityId() &&
                            old.provider() == approved.provider() &&
                            old.recordId() == approved.sourceRecordId() &&
                            old.endedAt() == approved.endedAt() &&
                            old.policy() == approval.preview().policy() &&
                            old.points() == approved.points() &&
                            old.events() == approved.events()
                    ) {
                        "Removed detail differs from the approved recording."
                    }
                    SqliteRecordingReceiptIdentity(database).requireMatches(old)
                    return@transaction false
                }
                require(row != null && row == approved) {
                    "Recording changed after preview; original detail retained."
                }
                require(old == null) { "Existing removal provenance must not be overwritten." }
                check(listOf(row))
                val header =
                    database
                        .query(
                            "SELECT started_at,started_mono,last_mono FROM recording_sessions WHERE id=? AND state='FINISHED' AND activity_id=?",
                            listOf(
                                SqlValue.Text(sessionId),
                                SqlValue.Text(requireNotNull(row.activityId())),
                            ),
                        )
                        .singleOrNull()
                        ?: error("Finished recording changed; existing detail retained.")
                val proof =
                    RemovedRecordingDetail(
                        sessionId,
                        row.activityId(),
                        row.provider(),
                        row.sourceRecordId(),
                        row.endedAt(),
                        now,
                        approval.preview().policy(),
                        StoredInstant.parse(header[0].sqlText()),
                        header[1].sqlLong(),
                        header[2].sqlLong(),
                        row.points(),
                        row.events(),
                        SqliteRecordingEvidence(database)
                            .evidence("recording_points", sessionId)
                            .sha256,
                        SqliteRecordingEvidence(database)
                            .evidence("recording_events", sessionId)
                            .sha256,
                    )
                SqliteRecordingReceiptIdentity(database).requireMatches(proof)
                SqliteRetentionRestore(database) { error("Cleanup must not replace the dataset") }
                    .insert(proof)
                for ((table, count) in
                    listOf(
                        "recording_points" to row.points(),
                        "recording_events" to row.events(),
                    )) {
                    database.execute(
                        "DELETE FROM $table WHERE session_id=?",
                        listOf(SqlValue.Text(sessionId)),
                    )
                    require(
                        database.query("SELECT changes()").single().single().sqlLong() == count
                    ) {
                        "Recording detail changed; cleanup rolled back."
                    }
                }
                true
            }
        }
}
