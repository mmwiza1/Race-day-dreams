package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.domain.time.Instant

/** Original running-support records over the existing database. No independent restore owner. */
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

    fun saveSchedule(preferences: SchedulePreferences) =
        saveOriginal(
            "config",
            "schedule",
            buildJsonObject {
                put("version", 1)
                put("type", "schedule")
                put("maximumMinutes", preferences.maximumMinutes)
                put("distribution", preferences.distribution)
                put(
                    "unavailable",
                    JsonArray(
                        preferences.unavailable
                            .sortedBy { it.toEpochDay() }
                            .map { JsonPrimitive(it.toString()) }
                    ),
                )
            },
        )

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

    /** The existing preference, not a new data bucket or an inferred cause of discomfort. */
    fun saveShoeDiscomfort(activities: Set<String>) =
        saveOriginal(
            "config",
            "shoe-feedback",
            buildJsonObject {
                put("version", 1)
                put("type", "shoe_feedback")
                put("activities", JsonArray(activities.sorted().map(::JsonPrimitive)))
            },
        )

    /** Explicit link receipt; the calling coaching transaction also saves response and feedback. */
    fun saveRunAssociation(
        activity: String,
        planVersion: String,
        workout: String,
        at: org.racedaydreams.domain.time.Instant,
    ) =
        saveOriginal(
            "association",
            activity,
            buildJsonObject {
                put("version", 1)
                put("activity", activity)
                put("planVersion", planVersion)
                put("workout", workout)
                put("at", at.toString())
            },
        )

    data class DreamPreferences(val enabled: Boolean, val cities: Set<String>)

    fun dreamPreferences(): DreamPreferences =
        configuration("dream")?.let {
            DreamPreferences(
                it.getValue("enabled").jsonPrimitive.boolean,
                it.getValue("cities").jsonArray.map { city -> city.jsonPrimitive.content }.toSet(),
            )
        } ?: DreamPreferences(false, DreamRoutes.routes().map { it.id() }.toSet())

    fun saveDreamPreferences(preferences: DreamPreferences) =
        saveOriginal(
            "config",
            "dream",
            buildJsonObject {
                put("version", 1)
                put("type", "dream")
                put("enabled", preferences.enabled)
                put("cities", JsonArray(preferences.cities.sorted().map(::JsonPrimitive)))
            },
        )

    fun recordingMode(recordId: String): String? =
        database
            .query(
                "SELECT payload FROM release08 WHERE kind='origin' AND id=?",
                listOf(SqlValue.Text(recordId)),
            )
            .singleOrNull()
            ?.single()
            ?.sqlText()
            ?.let { StoredJson.objectValue(it)["mode"]?.jsonPrimitive?.content }
            ?.takeIf { it in setOf("free", "quick", "custom", "repeat", "workout", "race") }

    fun recordingOrigin(mode: String, at: Instant): String {
        require(mode in setOf("free", "workout", "race"))
        val preferences = dreamPreferences()
        val route =
            if (preferences.enabled) {
                DreamRoutes.routes()
                    .filter { it.id() in preferences.cities }
                    .randomOrNull()
                    ?.let(DreamRoutes::currentGeometry)
            } else null
        return buildJsonObject {
                put("version", 1)
                put("mode", mode)
                put("at", at.toString())
                route?.let { put("dreamRoute", it.id()) }
            }
            .toString()
    }

    fun dreamAssignment(
        activityId: String
    ): org.racedaydreams.domain.support.DreamRoutes.Assignment? {
        val payload =
            database
                .query(
                    "SELECT payload FROM coach_support WHERE kind='dream' AND id=?",
                    listOf(SqlValue.Text(sqlRequired(activityId, "activity ID"))),
                )
                .singleOrNull()
                ?.single()
                ?.sqlText() ?: return null
        val archive =
            SupportArchiveJson.decode(
                buildJsonObject {
                    put("version", 1)
                    put(
                        "rows",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("kind", "dream")
                                    put("id", activityId)
                                    put("payload", StoredJson.objectValue(payload))
                                }
                            )
                        },
                    )
                }
            )
        return (archive.records.single().evidence as SupportArchiveJson.Evidence.Dream).assignment
    }

    fun completeRoutine(id: String, routine: String, at: org.racedaydreams.domain.time.Instant) {
        val payload = buildJsonObject {
            put("version", 1)
            put("routine", routine)
            put("at", at.toString())
        }
        SupportArchiveJson.decode(
            buildJsonObject {
                put("version", 1)
                put(
                    "rows",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("kind", "routine")
                                put("id", id)
                                put("payload", payload)
                            }
                        )
                    },
                )
            }
        )
        database.execute(
            "INSERT INTO coach_support(kind,id,payload) VALUES('routine',?,?)",
            listOf(SqlValue.Text(id), SqlValue.Text(payload.toString())),
        )
    }

    private fun saveOriginal(kind: String, id: String, payload: JsonObject) {
        // Validate through the exact existing archive contract before modifying retained bytes.
        Release08ArchiveJson.decode(
            buildJsonObject {
                put("version", 1)
                put(
                    "rows",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("kind", kind)
                                put("id", id)
                                put("payload", payload)
                            }
                        )
                    },
                )
            }
        )
        val values =
            listOf(SqlValue.Text(payload.toString()), SqlValue.Text(kind), SqlValue.Text(id))
        database.transaction {
            database.execute("UPDATE release08 SET payload=? WHERE kind=? AND id=?", values)
            if (database.query("SELECT changes()").single().single().sqlLong() == 0L)
                database.execute("INSERT INTO release08(payload,kind,id) VALUES(?,?,?)", values)
        }
    }

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
