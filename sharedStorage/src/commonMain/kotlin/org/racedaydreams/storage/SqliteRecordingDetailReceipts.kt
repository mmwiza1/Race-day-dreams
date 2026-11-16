package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail

/**
 * Read-only original removal provenance. Does not enable cleanup or claim archived detail exists.
 */
class SqliteRecordingDetailReceipts(private val database: SqlDatabase) {
    /**
     * A single consistent metadata snapshot; no raw points/events or cleanup approval is exported.
     */
    fun snapshot(): DetailRetentionArchiveJson.Snapshot =
        database.transaction {
            val rows =
                database.query(
                    "SELECT activity_id,provider,record_id,ended_at,removed_at,policy,started_at,started_mono,last_mono,point_count,event_count,points_hash,events_hash,session_id FROM recording_detail_removals ORDER BY session_id LIMIT 100001"
                )
            require(rows.size <= 100000) {
                "Detail receipt export limit exceeded; nothing was truncated."
            }
            DetailRetentionArchiveJson.Snapshot(
                SqliteRetentionPreferences(database).load(),
                rows.map { read(it[13].sqlText(), it) },
            )
        }

    fun removal(sessionId: String): RemovedRecordingDetail? {
        val row =
            database
                .query(
                    "SELECT activity_id,provider,record_id,ended_at,removed_at,policy,started_at,started_mono,last_mono,point_count,event_count,points_hash,events_hash FROM recording_detail_removals WHERE session_id=?",
                    listOf(SqlValue.Text(sqlRequired(sessionId, "recording"))),
                )
                .firstOrNull() ?: return null
        return read(sessionId, row)
    }

    private fun read(sessionId: String, row: List<SqlValue>): RemovedRecordingDetail =
        RemovedRecordingDetail(
            sessionId,
            row[0].sqlText(),
            row[1].sqlText(),
            row[2].sqlText(),
            StoredInstant.parse(row[3].sqlText()),
            StoredInstant.parse(row[4].sqlText()),
            DetailRetention.Policy.valueOf(row[5].sqlText()),
            StoredInstant.parse(row[6].sqlText()),
            row[7].sqlLong(),
            row[8].sqlLong(),
            row[9].sqlLong(),
            row[10].sqlLong(),
            row[11].sqlText(),
            row[12].sqlText(),
        )
}
