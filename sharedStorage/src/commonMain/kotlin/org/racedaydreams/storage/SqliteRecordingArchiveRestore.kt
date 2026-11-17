package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.storage.RemovedRecordingDetail
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.string

/**
 * Internal compatible merge of a fully inspected PRIVATE raw archive, never replacement of recorded
 * evidence. Native destructive UI stays off until complete-package/operation wiring is verified.
 * All rows, receipt reconciliation and approval invalidation share ONE transaction.
 */
internal class SqliteRecordingArchiveRestore(
    private val database: SqlDatabase,
    private val freshEpoch: () -> String,
) {
    fun merge(archive: InspectedRecordingArchive) {
        archive.recordingArchive().size
        merge(archive.snapshot, archive::replay)
    }

    /** Called only after the complete parent replacement already invalidated approvals. */
    internal fun mergeComplete(archive: InspectedBackupPackage) =
        merge(archive.recordings, archive::replayRecordings, invalidateApprovals = false)

    private fun merge(
        snapshot: RecordingArchiveRows.Snapshot,
        replay: ((String) -> Unit) -> RecordingArchiveRows.Snapshot,
        invalidateApprovals: Boolean = true,
    ) =
        database.transaction {
            require(
                database
                    .query(
                        "SELECT 1 FROM recording_sessions WHERE state IN ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1"
                    )
                    .isEmpty()
            ) {
                "Finish or recover the active recording before restoring detail."
            }
            val oldEpoch =
                database
                    .query("SELECT epoch FROM detail_retention_state WHERE id=1")
                    .single()
                    .single()
                    .sqlText()
            val epoch =
                if (invalidateApprovals) sqlRequired(freshEpoch(), "storage identity") else null
            require(epoch == null || epoch != oldEpoch) {
                "Restored detail requires a new storage identity."
            }
            val skipped = mutableSetOf<String>()
            val restoring = mutableMapOf<String, RemovedRecordingDetail>()
            val receipts = SqliteRecordingDetailReceipts(database)
            fun count(table: String, id: String) =
                database
                    .query(
                        "SELECT COUNT(*) FROM $table WHERE session_id=?",
                        listOf(SqlValue.Text(id)),
                    )
                    .single()
                    .single()
                    .sqlLong()
            // Inspect every existing header/receipt BEFORE any replay write. Missing detail may
            // only
            // be filled from a compatible archive, with original byte/count proof checked at the
            // end.
            for ((id, incoming) in snapshot.sessions) {
                val prior =
                    database
                        .query(
                            "SELECT state,started_at,started_mono,last_mono,activity_id FROM recording_sessions WHERE id=?",
                            listOf(SqlValue.Text(id)),
                        )
                        .singleOrNull() ?: continue
                require(
                    prior ==
                        listOf(
                            SqlValue.Text("FINISHED"),
                            SqlValue.Text(incoming.startedAt.toString()),
                            SqlValue.Integer(incoming.startedMono),
                            SqlValue.Integer(incoming.lastMono),
                            sqlText(incoming.activityId),
                        ) && incoming.state == "FINISHED"
                ) {
                    "Existing recording conflicts with archived metadata."
                }
                val proof = receipts.removal(id)
                if (
                    proof != null &&
                        incoming.removal == null &&
                        (incoming.points > 0 || incoming.events > 0)
                ) {
                    require(
                        proof.startedAt() == incoming.startedAt &&
                            proof.startedMono() == incoming.startedMono &&
                            proof.lastMono() == incoming.lastMono
                    ) {
                        "Archived detail has different timing."
                    }
                    require(
                        count("recording_points", id) == 0L && count("recording_events", id) == 0L
                    ) {
                        "Existing recording has conflicting detail."
                    }
                    restoring[id] = proof
                } else skipped += id
            }
            replay { line ->
                val row = Json.parseToJsonElement(line.removePrefix("\uFEFF")).row()
                val id = row.string("id")
                val incoming = snapshot.sessions.getValue(id)
                if (row.string("kind") == "session") {
                    if (id !in skipped && id !in restoring)
                        database.execute(
                            "INSERT INTO recording_sessions(id,state,started_at,started_mono,last_mono,activity_id) VALUES(?,?,?,?,?,?)",
                            listOf(
                                SqlValue.Text(id),
                                SqlValue.Text(
                                    if (incoming.state == "FINISHED") "FINISHED" else "RECOVERABLE"
                                ),
                                SqlValue.Text(incoming.startedAt.toString()),
                                SqlValue.Integer(incoming.startedMono),
                                SqlValue.Integer(incoming.lastMono),
                                sqlText(incoming.activityId),
                            ),
                        )
                } else {
                    val payload = row.getValue("payload").row()
                    when (val kind = row.string("kind")) {
                        "context" -> {
                            val prior =
                                singlePayload("coach_support", "kind='execution' AND id=?", id)
                            require(prior == null || Json.parseToJsonElement(prior) == payload) {
                                "Existing recorded workout context conflicts with the archive."
                            }
                            if (prior == null)
                                database.execute(
                                    "INSERT INTO coach_support VALUES('execution',?,?)",
                                    listOf(SqlValue.Text(id), SqlValue.Text(payload.toString())),
                                )
                        }
                        "point",
                        "event" -> {
                            val table =
                                if (kind == "point") "recording_points" else "recording_events"
                            val sequence = row.exactLong("seq")
                            val arguments = listOf(SqlValue.Text(id), SqlValue.Integer(sequence))
                            if (id in skipped) {
                                val metadata =
                                    database.query(
                                        "SELECT LENGTH(CAST(payload AS BLOB)) FROM $table WHERE session_id=? AND sequence=?",
                                        arguments,
                                    )
                                require(
                                    metadata.size == 1 &&
                                        metadata.single().single().sqlLong() in
                                            0..RecordingArchiveRows.MAX_LINE.toLong() * 4
                                ) {
                                    "Existing recording has missing or unsupported samples."
                                }
                                val old =
                                    database
                                        .query(
                                            "SELECT payload FROM $table WHERE session_id=? AND sequence=?",
                                            arguments,
                                        )
                                        .single()
                                        .single()
                                        .sqlText()
                                // Preserve existing bytes; do not reinterpret JSON to hide a
                                // mismatch.
                                require(old == payload.toString()) {
                                    "Existing recorded sample conflicts with the archive."
                                }
                            } else
                                database.execute(
                                    "INSERT INTO $table VALUES(?,?,?)",
                                    arguments + SqlValue.Text(payload.toString()),
                                )
                        }
                        "activity" -> {
                            val activity = ActivityJson.decode(payload)
                            val prior = singlePayload("activities", "id=?", activity.id())
                            if (prior == null) {
                                // New records only. This may derive qualified PBs from actual
                                // samples;
                                // existing summaries, annotations and historical PBs are never
                                // rewritten.
                                val repository =
                                    SqliteActivities(database) {
                                        error("Restore must not date evidence")
                                    }
                                repository.putActivity(
                                    repository.withRetainedSamples(activity),
                                    false,
                                    assignDefault = false,
                                )
                            } else {
                                val old = ActivityJson.decode(prior)
                                require(
                                    recordedActivityMeasurements(old) ==
                                        recordedActivityMeasurements(activity) &&
                                        database
                                            .query(
                                                "SELECT provider,record_id FROM activities WHERE id=?",
                                                listOf(SqlValue.Text(activity.id())),
                                            )
                                            .single() ==
                                            listOf(
                                                SqlValue.Text(activity.source().provider()),
                                                SqlValue.Text(activity.source().recordId()),
                                            )
                                ) {
                                    "Archived summary conflicts with retained activity; existing data retained."
                                }
                            }
                        }
                        "removed" -> {
                            val proof = requireNotNull(incoming.removal)
                            if (
                                count("recording_points", id) > 0 ||
                                    count("recording_events", id) > 0
                            ) {
                                SqliteRecordingEvidence(database).requireMatches(proof)
                            } else {
                                val prior = receipts.removal(id)
                                require(prior == null || prior == proof) {
                                    "Conflicting removed-detail provenance."
                                }
                                if (prior == null)
                                    SqliteRetentionRestore(database, freshEpoch).insert(proof)
                            }
                        }
                    }
                }
            }
            for (id in skipped) {
                val incoming = snapshot.sessions.getValue(id)
                if (incoming.removal == null)
                    require(
                        count("recording_points", id) == incoming.points.toLong() &&
                            count("recording_events", id) == incoming.events
                    ) {
                        "Existing recording has different evidence coverage."
                    }
            }
            for ((id, proof) in restoring) {
                SqliteRecordingReceiptIdentity(database).requireMatches(proof)
                SqliteRecordingEvidence(database).requireMatches(proof)
                database.execute(
                    "DELETE FROM recording_detail_removals WHERE session_id=?",
                    listOf(SqlValue.Text(id)),
                )
            }
            // Imported receipts cannot hide already available detail. Check all original hashes.
            for (receipt in receipts.snapshot().removals) {
                if (
                    count("recording_points", receipt.sessionId()) > 0 ||
                        count("recording_events", receipt.sessionId()) > 0
                ) {
                    SqliteRecordingReceiptIdentity(database).requireMatches(receipt)
                    SqliteRecordingEvidence(database).requireMatches(receipt)
                    database.execute(
                        "DELETE FROM recording_detail_removals WHERE session_id=?",
                        listOf(SqlValue.Text(receipt.sessionId())),
                    )
                }
            }
            require(database.query("PRAGMA foreign_key_check").isEmpty()) {
                "Archived detail has missing references."
            }
            if (epoch != null) {
                database.execute(
                    "UPDATE detail_retention_state SET epoch=? WHERE id=1",
                    listOf(SqlValue.Text(epoch)),
                )
                require(database.query("SELECT changes()").single().single().sqlLong() == 1L) {
                    "Storage identity missing."
                }
            }
        }

    private fun singlePayload(table: String, where: String, id: String): String? {
        val arguments = listOf(SqlValue.Text(id))
        val sizes =
            database.query(
                "SELECT LENGTH(CAST(payload AS BLOB)) FROM $table WHERE $where",
                arguments,
            )
        if (sizes.isEmpty()) return null
        require(
            sizes.size == 1 &&
                sizes.single().single().sqlLong() in 0..BackupEnvelopeJson.MAX_BYTES.toLong()
        ) {
            "Unsupported retained payload size; existing data retained."
        }
        val text =
            database
                .query("SELECT payload FROM $table WHERE $where", arguments)
                .single()
                .single()
                .sqlText()
        StoredJsonSyntax(text, 32, 2000000, BackupEnvelopeJson.MAX_BYTES) {
                throw IllegalArgumentException("Invalid retained recording payload: $it")
            }
            .validate()
        return text
    }
}
