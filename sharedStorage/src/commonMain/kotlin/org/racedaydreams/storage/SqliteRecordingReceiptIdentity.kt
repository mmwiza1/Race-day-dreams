package org.racedaydreams.storage

import org.racedaydreams.domain.storage.RemovedRecordingDetail

/** Original session/source identity shared by compatible restore and raw-only cleanup. */
internal class SqliteRecordingReceiptIdentity(private val database: SqlDatabase) {
    fun requireMatches(receipt: RemovedRecordingDetail) {
        require(
            database
                .query(
                    "SELECT state,started_at,started_mono,last_mono,activity_id FROM recording_sessions WHERE id=?",
                    listOf(SqlValue.Text(receipt.sessionId())),
                )
                .singleOrNull() ==
                listOf(
                    SqlValue.Text("FINISHED"),
                    SqlValue.Text(receipt.startedAt().toString()),
                    SqlValue.Integer(receipt.startedMono()),
                    SqlValue.Integer(receipt.lastMono()),
                    SqlValue.Text(receipt.activityId()),
                )
        ) {
            "Removed-detail provenance disagrees with its retained recording."
        }
        val args = listOf(SqlValue.Text(receipt.activityId()))
        val size =
            database.query("SELECT LENGTH(CAST(payload AS BLOB)) FROM activities WHERE id=?", args)
        require(
            size.size == 1 &&
                size.single().single().sqlLong() in 0..BackupEnvelopeJson.MAX_BYTES.toLong()
        ) {
            "Removed detail has no supported retained activity; existing data retained."
        }
        val text =
            database
                .query("SELECT payload FROM activities WHERE id=?", args)
                .single()
                .single()
                .sqlText()
        StoredJsonSyntax(text, 32, 2000000, BackupEnvelopeJson.MAX_BYTES) {
                throw IllegalArgumentException("Invalid retained recording payload: $it")
            }
            .validate()
        val actual = ActivityJson.decode(text)
        require(
            actual.id() == receipt.activityId() &&
                actual.source().provider() == receipt.provider() &&
                actual.source().recordId() == receipt.recordId() &&
                removedDetailMatchesActivityEnd(receipt.endedAt(), actual.end()) &&
                database
                    .query("SELECT provider,record_id FROM activities WHERE id=?", args)
                    .single() ==
                    listOf(SqlValue.Text(receipt.provider()), SqlValue.Text(receipt.recordId()))
        ) {
            "Removed-detail provenance disagrees with its retained activity."
        }
    }
}
