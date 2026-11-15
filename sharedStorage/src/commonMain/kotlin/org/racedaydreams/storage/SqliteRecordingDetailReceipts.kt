package org.racedaydreams.storage

import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.storage.RemovedRecordingDetail

/**
 * Read-only original removal provenance. Does not enable cleanup or claim archived detail exists.
 */
class SqliteRecordingDetailReceipts(private val database: SqlDatabase) {
    fun removal(sessionId: String): RemovedRecordingDetail? {
        val row =
            database
                .query(
                    "SELECT activity_id,provider,record_id,ended_at,removed_at,policy,started_at,started_mono,last_mono,point_count,event_count,points_hash,events_hash FROM recording_detail_removals WHERE session_id=?",
                    listOf(SqlValue.Text(sqlRequired(sessionId, "recording"))),
                )
                .firstOrNull() ?: return null
        return RemovedRecordingDetail(
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
}
