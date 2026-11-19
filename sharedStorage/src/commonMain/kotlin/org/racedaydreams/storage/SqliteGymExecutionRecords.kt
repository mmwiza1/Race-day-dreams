package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.strength.GymExecutionRepository.Stored
import org.racedaydreams.domain.strength.GymExecutionRepository.Summary
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.text

/** Normalised original executions. Loading never finalises sets or substitutes edited actuals. */
class SqliteGymExecutionRecords(private val database: SqlDatabase) {
    data class Evidence(
        val original: Stored,
        val currentActual: StrengthSession?,
        val actualEditedAt: Instant?,
    )

    fun load(id: String): Stored? =
        database.transaction {
            val values = listOf(SqlValue.Text(sqlRequired(id, "gym execution")))
            val indexed =
                database
                    .query(
                        "SELECT revision,header,set_count,event_count,origin,state,started_at,title,recorded_elapsed,active_millis,workout_id,workout_revision FROM strength_executions WHERE id=?",
                        values,
                    )
                    .firstOrNull() ?: return@transaction null
            val header = read(indexed[1].sqlText())
            val sets = rows("strength_execution_sets", id, indexed[2].sqlLong())
            val events = rows("strength_execution_events", id, indexed[3].sqlLong())
            var restored = JsonObject(header + mapOf("actualSets" to sets, "events" to events))
            if (!isNull(header, "completedSession"))
                restored =
                    JsonObject(
                        restored +
                            ("completedSession" to
                                JsonObject(
                                    child(header, "completedSession") +
                                        ("exercises" to actualBlocks(sets))
                                ))
                    )
            val execution = GymExecutionSnapshotJson.decode(restored.toString())
            val prescription = execution.prescription()
            val expected =
                listOf(
                    SqlValue.Text(execution.origin().name),
                    SqlValue.Text(execution.status().name),
                    SqlValue.Text(execution.startedAt().toString()),
                    SqlValue.Text(prescription?.title() ?: "Free workout"),
                    SqlValue.Integer(execution.recordedElapsedMillis()),
                    SqlValue.Integer(execution.activeMillis()),
                    sqlText(prescription?.id()),
                    prescription?.revision()?.let { SqlValue.Integer(it.toLong()) } ?: SqlValue.Null,
                )
            require(execution.id() == id && indexed.drop(4) == expected) {
                "Gym summary differs from its retained execution."
            }
            Stored(execution, indexed[0].sqlLong())
        }

    /**
     * Current accepted actual is separate from original performed evidence, including after edits.
     */
    fun evidence(id: String): Evidence? =
        database.transaction {
            val original = load(id) ?: return@transaction null
            val indexed =
                database
                    .query(
                        "SELECT accepted_actual,actual_edited_at FROM strength_executions WHERE id=?",
                        listOf(SqlValue.Text(id)),
                    )
                    .single()
            val current =
                indexed[0].nullableSqlText()?.let { StrengthEvidenceJson.decodeAcceptedSession(it) }
            val edited = indexed[1].nullableSqlText()?.let { StoredInstant.parse(it) }
            val actual =
                database
                    .query(
                        "SELECT payload FROM strength_sessions WHERE id=?",
                        listOf(SqlValue.Text(id)),
                    )
                    .firstOrNull()
                    ?.single()
                    ?.sqlText()
                    ?.let { StrengthEvidenceJson.decodeSession(it) }
            val execution = original.execution()
            if (execution.status() == GymExecution.Status.FINISHED) {
                require(current != null && current.id() == execution.id() && current == actual) {
                    "Accepted gym evidence is missing or differs from its recorded actual."
                }
                require(current == execution.completedSession() || edited != null) {
                    "A changed actual needs its explicit edit observation."
                }
            } else
                require(current == null && edited == null && actual == null) {
                    "Unfinished or discarded execution cannot claim completed evidence."
                }
            Evidence(original, current, edited)
        }

    /** Indexed selection deliberately avoids hydrating event/set payloads. */
    fun unfinished(): List<Summary> =
        database
            .query(
                "SELECT id,origin,state,title,started_at,recorded_elapsed,active_millis,set_count,revision FROM strength_executions WHERE state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED') ORDER BY started_at DESC,id"
            )
            .map {
                Summary(
                    it[0].sqlText(),
                    GymExecution.Origin.valueOf(it[1].sqlText()),
                    GymExecution.Status.valueOf(it[2].sqlText()),
                    it[3].sqlText(),
                    StoredInstant.parse(it[4].sqlText()),
                    it[5].sqlLong(),
                    it[6].sqlLong(),
                    it[7].sqlLong().toInt(),
                    it[8].sqlLong(),
                )
            }

    fun hasUnfinished(): Boolean =
        database
            .query(
                "SELECT 1 FROM strength_executions WHERE state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED') LIMIT 1"
            )
            .isNotEmpty()

    fun finishedActualIds(candidates: List<String>): Set<String> {
        require(candidates.size <= 20) { "Use a bounded recent page." }
        if (candidates.isEmpty()) return emptySet()
        val placeholders = candidates.joinToString(",") { "?" }
        return database
            .query(
                "SELECT id FROM strength_executions WHERE state='FINISHED' AND id IN ($placeholders)",
                candidates.map { SqlValue.Text(it) },
            )
            .map { it.single().sqlText() }
            .toSet()
    }

    private fun rows(table: String, id: String, expected: Long): JsonArray {
        val maximum = if (table == "strength_execution_sets") 10000 else 50000
        require(expected in 0..maximum.toLong()) { "Invalid gym staging count." }
        val result =
            database
                .query(
                    "SELECT sequence,payload FROM $table WHERE execution_id=? ORDER BY sequence",
                    listOf(SqlValue.Text(id)),
                )
                .mapIndexed { i, row ->
                    require(row[0].sqlLong() == i.toLong() && i < expected) {
                        "Gym staging order/count differs."
                    }
                    read(row[1].sqlText())
                }
        require(result.size.toLong() == expected) { "Gym staging rows are incomplete." }
        return JsonArray(result)
    }

    private fun actualBlocks(actual: JsonArray): JsonArray {
        val groups =
            linkedMapOf<
                Pair<String, StrengthExercise>,
                Pair<JsonObject, MutableList<JsonElement>>,
            >()
        for (value in actual) {
            val row = value.jsonObject
            val exercise = child(row, "exercise")
            val key = text(row, "slotId", 256) to FrozenGymWorkoutJson.readExercise(exercise)
            val group = groups.getOrPut(key) { exercise to mutableListOf() }
            group.second.add(child(row, "actual"))
        }
        return JsonArray(
            groups.values.map { (exercise, sets) ->
                buildJsonObject {
                    put("exercise", exercise)
                    put("sets", JsonArray(sets))
                }
            }
        )
    }
}
