package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.activity.*

/**
 * Read-only original store-v1 CORE capture. All feature sections still need capture and full parent
 * validation; this is not a complete backup, replacement command or raw-detail archive. One
 * existing database snapshot owns every read. Imported activity caches are filtered before
 * budgets/decoding.
 */
class SqliteCoreBackup(private val database: SqlDatabase) {
    class Snapshot
    internal constructor(val core: CoreBackupDataJson.Snapshot, val data: JsonObject)

    fun snapshot(): Snapshot =
        database.transaction {
            for ((table, fields) in
                listOf(
                    "race_goal" to listOf("distance", "race_name", "race_date", "goal_mode"),
                    "training_preferences" to listOf("strategy", "schedule_type", "units"),
                )) {
                val length = fields.joinToString("+") { "COALESCE(LENGTH(CAST($it AS BLOB)),0)" }
                val total =
                    database
                        .query("SELECT TOTAL($length) FROM $table")
                        .single()
                        .single()
                        .nullableSqlNumber()!!
                require(total.isFinite() && total <= BackupEnvelopeJson.MAX_BYTES) {
                    "Profile source exceeds the supported backup size."
                }
            }
            var bytes = 0L
            fun charge(value: JsonElement) {
                val next = value.toString().encodeToByteArray(throwOnInvalidSequence = true).size
                require(next <= BackupEnvelopeJson.MAX_BYTES - bytes) {
                    "Owned core data exceeds the supported backup size; nothing was truncated."
                }
                bytes += next
            }
            fun array(values: MutableList<JsonElement>, value: JsonElement) {
                require(values.size < 100000) {
                    "Too many owned core records; nothing was truncated."
                }
                charge(value)
                values += value
            }
            val root = buildJsonObject {
                put("storeVersion", 1)
                val profile =
                    SqliteAthleteProfile(database).load()?.let { ProfileJson.element(it) }
                        ?: JsonNull
                charge(profile)
                put("profile", profile)
                var draft: JsonElement = JsonNull
                scan("onboarding_draft", "id", listOf("payload"), listOf("payload")) {
                    val text = it.single().sqlText()
                    objectValue(text)
                    draft = JsonPrimitive(text)
                    charge(draft)
                }
                put("draft", draft)
                var baseline: JsonElement = JsonNull
                var baselineAt: JsonElement = JsonNull
                scan(
                    "baseline_state",
                    "id",
                    listOf("manual_json", "manual_recorded_at"),
                    listOf("manual_json", "manual_recorded_at"),
                ) {
                    baseline = objectValue(it[0].sqlText())
                    baselineAt = StoredJson.nullable(it[1].nullableSqlText())
                    charge(baseline)
                    charge(baselineAt)
                }
                put("manualBaseline", baseline)
                put("manualBaselineRecordedAt", baselineAt)
                val activities = mutableListOf<Pair<Pair<Long, String>, JsonElement>>()
                scan(
                    "activities a LEFT JOIN activity_annotations n ON a.provider=n.provider AND a.record_id=n.record_id LEFT JOIN run_responses r ON r.activity_id=a.id",
                    "a.id",
                    listOf(
                        "a.payload",
                        "n.provider",
                        "n.notes",
                        "n.rpe",
                        "n.shoe_id",
                        "r.planned_workout_id",
                        "a.start_millis",
                        "a.id",
                    ),
                    listOf(
                        "a.payload",
                        "n.notes",
                        "n.provider",
                        "n.shoe_id",
                        "r.planned_workout_id",
                    ),
                    "a.provider IN ('manual','assessment','rdd','gpx')",
                ) {
                    objectValue(
                        it[0].sqlText()
                    ) // Bound legacy payload syntax before domain decoding.
                    val summary = readAnnotatedActivity(it)
                    require(summary.id() == it[7].sqlText() && isUserActivity(summary)) {
                        "Inconsistent owned activity identity."
                    }
                    val backup =
                        if (summary.source().provider() in listOf("rdd", "gpx"))
                            summary.copy(samples = emptyList())
                        else summary
                    val value = objectValue(ActivityJson.encode(backup))
                    charge(value)
                    activities += (it[6].sqlLong() to it[7].sqlText()) to value
                }
                put(
                    "activities",
                    JsonArray(activities.sortedByDescending { it.first.first }.map { it.second }),
                )
                val shoes = mutableListOf<JsonElement>()
                val orderedShoes = mutableListOf<Pair<Long, JsonElement>>()
                scan("shoes", "id", listOf("payload", "is_default"), listOf("payload")) {
                    val shoe =
                        ShoeJson.decode(objectValue(it[0].sqlText()).toString())
                            .copy(defaultShoe = it[1].sqlLong() == 1L)
                    val value = objectValue(ShoeJson.encode(shoe))
                    array(shoes, value)
                    orderedShoes += it[1].sqlLong() to value
                }
                put(
                    "shoes",
                    JsonArray(orderedShoes.sortedByDescending { it.first }.map { it.second }),
                )
                val results = mutableListOf<JsonElement>()
                val seenResults = mutableSetOf<String>()
                // Match original result-table precedence, including exclusion of derived rolling
                // rows.
                for (table in
                    listOf("manual_results", "best_efforts", "preserved_best_efforts")) scan(
                    table,
                    "id",
                    listOf("id", "payload"),
                    listOf("payload"),
                ) {
                    if (seenResults.add(it[0].sqlText())) {
                        val result = ResultJson.decode(objectValue(it[1].sqlText()).toString())
                        if (result.source() != ResultSource.ROLLING)
                            array(results, objectValue(ResultJson.encode(result)))
                    }
                }
                put("results", JsonArray(results))
                val efforts = mutableListOf<JsonElement>()
                val seenEfforts = mutableSetOf<String>()
                fun effort(row: List<SqlValue>) {
                    if (seenEfforts.add(row[0].sqlText()))
                        array(
                            efforts,
                            buildJsonObject {
                                put("result", objectValue(row[1].sqlText()))
                                put(
                                    "source",
                                    buildJsonObject {
                                        put("provider", row[2].sqlText())
                                        put("recordId", row[3].sqlText())
                                        put(
                                            "originPackage",
                                            StoredJson.nullable(row[4].nullableSqlText()),
                                        )
                                    },
                                )
                            },
                        )
                }
                scan(
                    "best_efforts b JOIN activities a ON b.activity_id=a.id",
                    "b.id",
                    listOf("b.id", "b.payload", "a.provider", "a.record_id", "a.origin_package"),
                    listOf("b.payload", "a.provider", "a.record_id", "a.origin_package"),
                    consume = ::effort,
                )
                scan(
                    "preserved_best_efforts",
                    "id",
                    listOf("id", "payload", "provider", "record_id", "origin_package"),
                    listOf("payload", "provider", "record_id", "origin_package"),
                    consume = ::effort,
                )
                put("bestEfforts", JsonArray(efforts))
                val annotations = mutableListOf<JsonElement>()
                // Source-pair ordering matches Android; capture all app-owned notes, even on
                // imports.
                val annotationBytes =
                    "COALESCE(LENGTH(CAST(provider AS BLOB)),0)+COALESCE(LENGTH(CAST(record_id AS BLOB)),0)+COALESCE(LENGTH(CAST(notes AS BLOB)),0)+COALESCE(LENGTH(CAST(shoe_id AS BLOB)),0)"
                val annotationTotals =
                    database
                        .query("SELECT COUNT(*),TOTAL($annotationBytes) FROM activity_annotations")
                        .single()
                require(
                    annotationTotals[0].sqlLong() <= 100000 &&
                        annotationTotals[1].nullableSqlNumber()!! <=
                            BackupEnvelopeJson.MAX_BYTES - bytes
                ) {
                    "Annotations exceed the supported backup size."
                }
                for (row in
                    database.query(
                        "SELECT provider,record_id,notes,rpe,shoe_id FROM activity_annotations ORDER BY provider,record_id LIMIT 100001"
                    )) {
                    array(
                        annotations,
                        buildJsonObject {
                            put("provider", row[0].sqlText())
                            put("recordId", row[1].sqlText())
                            put("notes", StoredJson.nullable(row[2].nullableSqlText()))
                            put("rpe", StoredJson.nullable(row[3].nullableSqlNumber()))
                            put("shoeId", StoredJson.nullable(row[4].nullableSqlText()))
                        },
                    )
                }
                put("annotations", JsonArray(annotations))
                put(
                    "settings",
                    buildJsonObject {
                        scan(
                            "settings",
                            "setting_key",
                            listOf("setting_key", "setting_value"),
                            listOf("setting_key", "setting_value"),
                            maximumRows = Int.MAX_VALUE,
                        ) {
                            charge(JsonPrimitive(it[0].sqlText()))
                            charge(JsonPrimitive(it[1].sqlText()))
                            put(it[0].sqlText(), it[1].sqlText())
                        }
                    },
                )
                val planned = mutableListOf<JsonElement>()
                scan("planned_workouts", "id", listOf("payload", "revision"), listOf("payload")) {
                    array(
                        planned,
                        buildJsonObject {
                            put("workout", objectValue(it[0].sqlText()))
                            put("revision", it[1].sqlLong())
                        },
                    )
                }
                put("plannedWorkouts", JsonArray(planned))
                for ((key, table) in
                    listOf(
                        "assessments" to "assessment_results",
                        "trainingPlans" to "training_plans",
                        "planAdjustments" to "plan_adjustments",
                    )) {
                    val values = mutableListOf<JsonElement>()
                    scan(table, "id", listOf("payload"), listOf("payload")) {
                        array(values, objectValue(it.single().sqlText()))
                    }
                    put(key, JsonArray(values))
                }
            }
            val text = root.toString()
            require(
                text.encodeToByteArray(throwOnInvalidSequence = true).size <=
                    BackupEnvelopeJson.MAX_BYTES
            ) {
                "Core backup exceeds 32 MiB."
            }
            syntax(text)
            Snapshot(CoreBackupDataJson.decode(root), root)
        }

    private fun scan(
        from: String,
        key: String,
        columns: List<String>,
        sized: List<String>,
        filter: String = "1=1",
        maximumRows: Int = 100000,
        consume: (List<SqlValue>) -> Unit,
    ) {
        val count =
            database.query("SELECT COUNT(*) FROM $from WHERE $filter").single().single().sqlLong()
        require(count <= maximumRows) { "Too many core source records; nothing was truncated." }
        val size = sized.joinToString("+") { "COALESCE(LENGTH(CAST($it AS BLOB)),0)" }
        var after: SqlValue? = null
        while (true) {
            val page =
                database.query(
                    "SELECT $key,$size FROM $from WHERE ($filter)" +
                        (if (after == null) "" else " AND ($key)>?") +
                        " ORDER BY $key LIMIT 512",
                    after?.let { listOf(it) }.orEmpty(),
                )
            if (page.isEmpty()) break
            var first = 0
            while (first < page.size) {
                var end = first
                var bytes = 0L
                while (end < page.size) {
                    val next = page[end][1].sqlLong()
                    require(next in 0..BackupEnvelopeJson.MAX_BYTES.toLong()) {
                        "Unsupported core source row size."
                    }
                    if (end > first && next > 1024 * 1024 - bytes) break
                    bytes += next
                    end++
                }
                val batch = page.subList(first, end)
                val rows =
                    database.query(
                        "SELECT ${columns.joinToString(",")} FROM $from WHERE ($filter) AND ($key) IN (${batch.joinToString(",") { "?" }}) ORDER BY $key",
                        batch.map { it[0] },
                    )
                require(rows.size == batch.size) { "Incomplete core snapshot." }
                rows.forEach(consume)
                first = end
            }
            after = page.last()[0]
        }
    }

    private fun objectValue(text: String): JsonObject {
        syntax(text)
        return StoredJson.objectValue(text)
    }

    private fun syntax(text: String) =
        StoredJsonSyntax(text, 32, 2000000, 1024 * 1024) {
                throw IllegalArgumentException("Invalid core backup source: $it")
            }
            .validate()
}
