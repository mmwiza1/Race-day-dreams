package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** One bounded consistent legacy coaching snapshot. No replacement/activation/cleanup commands. */
class SqliteCoachingArchive(private val database: SqlDatabase) {
    fun snapshot(): CoachingArchiveJson.Snapshot =
        database.transaction {
            var minimumBytes = 0L
            // The sum of raw SQL text sizes is a lower bound on the enclosing escaped JSON file.
            // Reject impossible exports before loading payloads; do not impose a second16MiB cap.
            for (table in CoachingArchiveJson.Table.entries) {
                val size =
                    table.columns.joinToString("+") { "COALESCE(LENGTH(CAST($it AS BLOB)),0)" }
                val counts =
                    database
                        .query("SELECT COUNT(*),COALESCE(SUM($size),0) FROM ${table.sqlName}")
                        .single()
                require(counts[0].sqlLong() <= 100000) {
                    "Too many coaching records; nothing was truncated."
                }
                val bytes = counts[1].sqlLong()
                require(bytes >= 0 && bytes <= BackupEnvelopeJson.MAX_BYTES - minimumBytes) {
                    "Coaching history exceeds the whole-backup budget; nothing was truncated."
                }
                minimumBytes += bytes
            }
            val root = buildJsonObject {
                put("version", 1)
                for (table in CoachingArchiveJson.Table.entries) {
                    val rows =
                        database.query(
                            "SELECT ${table.columns.joinToString(",")} FROM ${table.sqlName} ORDER BY rowid LIMIT 100001"
                        )
                    require(rows.size <= 100000) {
                        "Too many coaching records; nothing was truncated."
                    }
                    put(
                        table.sqlName,
                        JsonArray(
                            rows.map { row ->
                                buildJsonObject {
                                    table.columns.forEachIndexed { index, column ->
                                        put(
                                            column,
                                            when (val value = row[index]) {
                                                SqlValue.Null -> JsonNull
                                                is SqlValue.Text -> JsonPrimitive(value.value)
                                                is SqlValue.Integer ->
                                                    JsonPrimitive(value.value.toString())
                                                else ->
                                                    throw IllegalArgumentException(
                                                        "Unexpected coaching SQL value."
                                                    )
                                            },
                                        )
                                    }
                                }
                            }
                        ),
                    )
                }
            }
            CoachingArchiveJson.decode(root)
        }
}
