package org.racedaydreams.storage

import org.racedaydreams.domain.storage.RemovedRecordingDetail

/** Exact original sequence:text:newline hashes, never JSON re-encoding or route recalculation. */
class SqliteRecordingEvidence(private val database: SqlDatabase) {
    fun requireMatches(receipt: RemovedRecordingDetail) =
        database.transaction {
            for ((table, count, hash) in
                listOf(
                    Triple("recording_points", receipt.points(), receipt.pointsHash()),
                    Triple("recording_events", receipt.events(), receipt.eventsHash()),
                )) {
                require(evidence(table, receipt.sessionId()) == Evidence(count, hash)) {
                    "Archived detail conflicts with the original removed evidence; existing data retained."
                }
            }
        }

    internal data class Evidence(val count: Long, val sha256: String)

    internal fun evidence(table: String, session: String): Evidence {
        require(table in listOf("recording_points", "recording_events")) { "Unsupported evidence." }
        val digest = archiveSha256()
        try {
            var count = 0L
            var after: Long? = null
            while (true) {
                val arguments =
                    listOf(SqlValue.Text(session)) +
                        (after?.let { listOf(SqlValue.Integer(it)) } ?: emptyList())
                val page =
                    database.query(
                        "SELECT sequence,LENGTH(CAST(payload AS BLOB)) FROM $table WHERE session_id=?" +
                            (if (after == null) "" else " AND sequence>?") +
                            " ORDER BY sequence LIMIT 512",
                        arguments,
                    )
                if (page.isEmpty()) break
                var first = 0
                while (first < page.size) {
                    var end = first
                    var bytes = 0L
                    while (end < page.size) {
                        val size = page[end][1].sqlLong()
                        require(size in 0..RecordingArchiveRows.MAX_LINE.toLong() * 4) {
                            "Unsupported recording source row size; existing data was retained."
                        }
                        if (end > first && size > 1024 * 1024 - bytes) break
                        bytes += size
                        end++
                    }
                    val batch = page.subList(first, end)
                    val rows =
                        database.query(
                            "SELECT sequence,payload FROM $table WHERE session_id=? AND sequence IN (${batch.joinToString(",") { "?" }}) ORDER BY sequence",
                            listOf(SqlValue.Text(session)) + batch.map { it[0] },
                        )
                    require(rows.size == batch.size) { "Incomplete recording evidence snapshot." }
                    for (row in rows) {
                        // Text bytes and SQL INTEGER precision are part of the original receipt.
                        val value =
                            (row[0].sqlLong().toString() + ":" + row[1].sqlText() + "\n")
                                .encodeToByteArray(throwOnInvalidSequence = true)
                        for (offset in value.indices step BackupStagingFile.BLOCK_BYTES) {
                            digest.update(
                                value,
                                offset,
                                minOf(BackupStagingFile.BLOCK_BYTES, value.size - offset),
                            )
                        }
                        count++
                    }
                    first = end
                }
                after = page.last()[0].sqlLong()
            }
            return Evidence(
                count,
                digest.finish().joinToString("") { it.toUByte().toString(16).padStart(2, '0') },
            )
        } finally {
            digest.close()
        }
    }
}
