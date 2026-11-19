package org.racedaydreams.storage

import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.storage.DetailRetention
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants

/**
 * Original read-only recording inventory over ONE existing SQLite snapshot. No route/summary JSON
 * is decoded, no training evidence is recalculated, and this boundary never authorizes cleanup.
 * Physical bytes are an optional platform observation, including DB/WAL/SHM when available, not
 * bytes reclaimed by deleting rows. Missing physical information stays unknown.
 */
class SqliteRecordingInventory(
    private val database: SqlDatabase,
    private val physicalFileBytes: () -> Long? = { null },
) {
    data class Usage(
        val pointRows: Long,
        val eventRows: Long,
        val detailPayloadBytes: Long,
        val activityRows: Long,
        val activityPayloadBytes: Long,
        val databaseFileBytes: Long?,
    ) {
        init {
            require(
                listOf(pointRows, eventRows, detailPayloadBytes, activityRows, activityPayloadBytes)
                    .all { it >= 0 } && (databaseFileBytes == null || databaseFileBytes >= 0)
            ) {
                "Invalid storage inventory."
            }
        }
    }

    class Inventory(
        recordings: List<DetailRetention.Recording>,
        val unfinishedRecording: Boolean,
        val usage: Usage,
    ) {
        val recordings = Immutable.copyList(recordings)
    }

    fun inspect(): Inventory =
        database.transaction {
            // Fixed metadata budget before loading a history list. Do not silently truncate
            // records.
            val metadata =
                database
                    .query(
                        "SELECT COUNT(*),COALESCE(MAX(LENGTH(s.id)),0),COALESCE(MAX(LENGTH(s.state)),0),COALESCE(MAX(LENGTH(s.activity_id)),0),COALESCE(MAX(LENGTH(a.provider)),0),COALESCE(MAX(LENGTH(a.record_id)),0),COALESCE(SUM(LENGTH(CAST(s.id AS BLOB))+LENGTH(CAST(s.state AS BLOB))+COALESCE(LENGTH(CAST(s.activity_id AS BLOB)),0)+COALESCE(LENGTH(CAST(a.provider AS BLOB)),0)+COALESCE(LENGTH(CAST(a.record_id AS BLOB)),0)),0) FROM recording_sessions s LEFT JOIN activities a ON a.id=s.activity_id"
                    )
                    .single()
            require(
                metadata[0].sqlLong() <= 100000 &&
                    metadata[2].sqlLong() <= 32 &&
                    listOf(1, 3, 4, 5).all { metadata[it].sqlLong() <= 256 } &&
                    metadata[6].sqlLong() in 0..BackupEnvelopeJson.MAX_BYTES.toLong()
            ) {
                "Storage inventory exceeds its supported metadata budget; nothing was truncated."
            }
            // Aggregate BEFORE the join: no point/event cross-product or per-session query.
            val recordings =
                database
                    .query(
                        "SELECT s.id,s.state,s.activity_id,a.provider,a.record_id,a.end_millis,COALESCE(p.n,0),COALESCE(e.n,0) FROM recording_sessions s LEFT JOIN activities a ON a.id=s.activity_id LEFT JOIN (SELECT session_id,COUNT(*) n FROM recording_points GROUP BY session_id) p ON p.session_id=s.id LEFT JOIN (SELECT session_id,COUNT(*) n FROM recording_events GROUP BY session_id) e ON e.session_id=s.id ORDER BY s.id LIMIT 100001"
                    )
                    .map(::read)
            require(recordings.size <= 100000) {
                "Too many recording headers; nothing was truncated."
            }
            val points = counts("recording_points")
            val events = counts("recording_events")
            val activities = counts("activities")
            Inventory(
                recordings,
                recordings.any { !it.finished() },
                Usage(
                    points.first,
                    events.first,
                    add(points.second, events.second),
                    activities.first,
                    activities.second,
                    physicalFileBytes(),
                ),
            )
        }

    /**
     * Indexed single-session recheck for the future guarded cleanup transaction. Still read-only.
     */
    internal fun recording(id: String): DetailRetention.Recording? =
        database.transaction {
            database
                .query(
                    "SELECT s.id,s.state,s.activity_id,a.provider,a.record_id,a.end_millis,(SELECT COUNT(*) FROM recording_points p WHERE p.session_id=s.id),(SELECT COUNT(*) FROM recording_events e WHERE e.session_id=s.id) FROM recording_sessions s LEFT JOIN activities a ON a.id=s.activity_id WHERE s.id=?",
                    listOf(SqlValue.Text(sqlRequired(id, "recording"))),
                )
                .singleOrNull()
                ?.let(::read)
        }

    private fun read(row: List<SqlValue>): DetailRetention.Recording {
        val state = row[1].sqlText()
        require(state in listOf("ACTIVE", "PAUSED", "RECOVERABLE", "FINISHED")) {
            "Unsupported recording state; existing detail retained."
        }
        val provider = row[3].nullableSqlText().orEmpty()
        return DetailRetention.Recording(
            row[0].sqlText(),
            row[2].nullableSqlText(),
            provider,
            row[4].nullableSqlText().orEmpty(),
            if (row[5] == SqlValue.Null) null else upperBound(row[5].sqlLong()),
            state == "FINISHED",
            row[6].sqlLong(),
            row[7].sqlLong(),
            // Only normalized recording_points/events are eligible. Inline activity detail is
            // retained; this does not inspect or authorize removal of embedded legacy samples.
            if (provider in listOf("rdd", "gpx")) 0L else null,
        )
    }

    private fun counts(table: String): Pair<Long, Long> {
        val row =
            database
                .query("SELECT COUNT(*),COALESCE(SUM(LENGTH(CAST(payload AS BLOB))),0) FROM $table")
                .single()
        return row[0].sqlLong() to row[1].sqlLong()
    }

    private fun add(a: Long, b: Long): Long {
        require(a >= 0 && b >= 0) { "Invalid storage byte count." }
        if (b > Long.MAX_VALUE - a) throw ArithmeticException("long overflow")
        return a + b
    }

    private fun upperBound(milliseconds: Long): Instant {
        // Original Instant.ofEpochMilli(ms).plusMillis(1), including Long.MIN/MAX. Adding to ms
        // directly would overflow; Double conversion would lose legitimate millisecond evidence.
        val remainder = milliseconds % 1000
        val seconds = milliseconds / 1000 - if (remainder < 0) 1 else 0
        val within = if (remainder < 0) remainder + 1000 else remainder
        return Instants.ofEpochSecond(seconds, (within + 1) * 1000000)
    }
}
