package org.racedaydreams.storage

import kotlinx.serialization.json.*

/** Read-only original support tables in one existing-database transaction. No restore commands. */
class SqliteRunningSupportArchives(private val database: SqlDatabase) {
    data class SchedulePreferences(
        val maximumMinutes: Int,
        val distribution: String,
        val unavailable: Set<org.racedaydreams.domain.time.LocalDate>,
    )

    /** Read only the original schedule; unrelated dense route payloads are not hydrated. */
    fun schedule(): SchedulePreferences? =
        configuration("schedule")?.let { payload ->
            SchedulePreferences(
                SupportPayloadJson.integer(payload, "maximumMinutes"),
                SupportPayloadJson.text(payload, "distribution"),
                org.racedaydreams.domain.coaching.Immutable.copySet(
                    payload
                        .getValue("unavailable")
                        .jsonArray
                        .map { StoredDate.parse((it as? JsonPrimitive)?.content ?: it.toString()) }
                        .toSet()
                ),
            )
        }

    /** Read only the original shoe-feedback preference; never hydrate unrelated route payloads. */
    fun shoeDiscomfort(): Set<String> =
        configuration("shoe-feedback")?.let { payload ->
            org.racedaydreams.domain.coaching.Immutable.copySet(
                payload
                    .getValue("activities")
                    .jsonArray
                    .map { (it as? JsonPrimitive)?.content ?: it.toString() }
                    .toSet()
            )
        } ?: emptySet()

    private fun configuration(id: String): JsonObject? {
        val payload =
            database
                .query(
                    "SELECT payload FROM release08 WHERE kind='config' AND id=?",
                    listOf(SqlValue.Text(id)),
                )
                .firstOrNull()
                ?.single()
                ?.sqlText() ?: return null
        return Release08ArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put(
                        "rows",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "config")
                                    put("id", id)
                                    put("payload", StoredJson.objectValue(payload))
                                }
                            )
                        },
                    )
                }
            )
            .records
            .single()
            .payload
    }

    class Snapshot
    internal constructor(
        val support: SupportArchiveJson.Snapshot,
        val release08: Release08ArchiveJson.Snapshot,
    )

    fun snapshot(): Snapshot =
        database.transaction {
            var minimumBytes = 0L
            for (table in listOf("coach_support", "release08")) {
                val totals =
                    database
                        .query(
                            "SELECT COUNT(*),COALESCE(SUM(LENGTH(CAST(kind AS BLOB))+LENGTH(CAST(id AS BLOB))+LENGTH(CAST(payload AS BLOB))),0) FROM $table"
                        )
                        .single()
                require(totals[0].sqlLong() <= 100000) {
                    "Too many support records; nothing was truncated."
                }
                val bytes = totals[1].sqlLong()
                require(bytes >= 0 && bytes <= BackupEnvelopeJson.MAX_BYTES - minimumBytes) {
                    "Running support exceeds the whole-backup budget; nothing was truncated."
                }
                minimumBytes += bytes
            }
            fun section(table: String): JsonObject = buildJsonObject {
                put("version", 1)
                val rows =
                    database.query(
                        "SELECT kind,id,payload FROM $table ORDER BY kind,id LIMIT 100001"
                    )
                require(rows.size <= 100000) { "Too many support records; nothing was truncated." }
                put(
                    "rows",
                    JsonArray(
                        rows.map { r ->
                            buildJsonObject {
                                put("kind", r[0].sqlText())
                                put("id", r[1].sqlText())
                                put(
                                    "payload",
                                    Json.parseToJsonElement(r[2].sqlText()) as? JsonObject
                                        ?: error("Invalid support payload."),
                                )
                            }
                        }
                    ),
                )
            }
            Snapshot(
                SupportArchiveJson.decode(section("coach_support")),
                Release08ArchiveJson.decode(section("release08")),
            )
        }
}
