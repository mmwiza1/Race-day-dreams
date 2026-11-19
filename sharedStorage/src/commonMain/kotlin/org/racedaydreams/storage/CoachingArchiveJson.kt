package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.integer
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.StoredJson.text

/**
 * Original coaching-v1 archive graph. Reading history never recalculates scores or activates plans.
 */
object CoachingArchiveJson {
    enum class Table(val sqlName: String, vararg fields: String) {
        FEEDBACK("run_feedback", "activity_id", "plan_version_id", "payload"),
        RESPONSES(
            "run_responses",
            "activity_id",
            "planned_workout_id",
            "plan_version_id",
            "payload",
        ),
        PROPOSALS(
            "coach_proposals",
            "id",
            "base_version_id",
            "payload",
            "decision",
            "result_version_id",
        ),
        CURRENT("coach_current_plan", "id", "version_id"),
        VERSIONS("coach_plan_versions", "id", "plan_id", "version", "payload"),
        SESSIONS("strength_sessions", "id", "started_at", "payload"),
        EXERCISES("strength_exercises", "id", "payload");

        val columns: List<String> = Immutable.copyList(fields.asList())
    }

    data class Proposal(
        val value: AdjustmentProposal,
        val decision: String?,
        val resultVersionId: String?,
    )

    class Snapshot
    internal constructor(
        rows: Map<Table, List<Map<String, String?>>>,
        versions: Map<String, PlanVersion>,
        val currentVersionId: String?,
        exercises: Map<String, StrengthExercise>,
        sessions: Map<String, StrengthSession>,
        responses: Map<String, RunExecution>,
        proposals: List<Proposal>,
    ) {
        /** Exact original SQL payload strings, including retained legacy feedback and evidence. */
        val rows =
            Immutable.copyMap(
                rows.mapValues { (_, rows) -> Immutable.copyList(rows.map { frozenRow(it) }) }
            )
        val versions = Immutable.copyMap(versions)
        val exercises = Immutable.copyMap(exercises)
        val sessions = Immutable.copyMap(sessions)
        val responses = Immutable.copyMap(responses)
        val proposals = Immutable.copyList(proposals)
    }

    private class Column(override val key: String, override val value: String?) :
        Map.Entry<String, String?> {
        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor (value?.hashCode() ?: 0)
    }

    /** Retain SQL NULL while exposing no mutable map entries or caller-owned collections. */
    private fun frozenRow(row: Map<String, String?>): Map<String, String?> =
        object : AbstractMap<String, String?>() {
            override val entries: Set<Map.Entry<String, String?>> =
                Immutable.copySet(row.map { Column(it.key, it.value) })
        }

    /** The enclosing root validates syntax/size first; do not apply a second standalone gym cap. */
    fun decode(root: JsonObject): Snapshot {
        require(root.requiredInteger("version") == 1) { "Unsupported coaching backup version." }
        val tables =
            Table.entries.associateWith { table ->
                root.array(table.sqlName, 100000).map { item ->
                    val row = item.row()
                    require(row.keys == table.columns.toSet()) {
                        "Unexpected coaching backup columns."
                    }
                    table.columns.associateWith { row.text(it) }
                }
            }
        fun Map<String, String?>.required(key: String) =
            this[key] ?: throw IllegalArgumentException("Missing coaching column: $key")
        fun match(row: Map<String, String?>, key: String, value: String?) {
            require(row[key] == value) { "Backup column/payload disagree: $key" }
        }
        val versions = linkedMapOf<String, PlanVersion>()
        val numbers = mutableSetOf<Pair<String, Int>>()
        for (row in tables.getValue(Table.VERSIONS)) {
            val v = RunningCoachingJson.decodeVersion(row.required("payload"))
            match(row, "id", v.id())
            match(row, "plan_id", v.planId())
            match(row, "version", v.version().toString())
            require(versions.put(v.id(), v) == null && numbers.add(v.planId() to v.version())) {
                "Duplicated plan version."
            }
        }
        val current = tables.getValue(Table.CURRENT)
        require(current.size <= 1) { "Only one plan can be current." }
        for (row in current) {
            match(row, "id", "1")
            require(row.required("version_id") in versions) { "Current version is missing." }
        }
        val exercises = linkedMapOf<String, StrengthExercise>()
        for (row in tables.getValue(Table.EXERCISES)) {
            val e = StrengthEvidenceJson.decodeExercise(row.required("payload"))
            match(row, "id", e.id())
            require(exercises.put(e.id(), e) == null) { "Duplicate exercise." }
        }
        val sessions = linkedMapOf<String, StrengthSession>()
        for (row in tables.getValue(Table.SESSIONS)) {
            val s = StrengthEvidenceJson.decodeSession(row.required("payload"))
            match(row, "id", s.id())
            match(row, "started_at", s.startedAt().toString())
            require(sessions.put(s.id(), s) == null) { "Duplicate strength workout." }
        }
        val responses = linkedMapOf<String, RunExecution>()
        for (row in tables.getValue(Table.RESPONSES)) {
            val r = RunningCoachingJson.decodeResponse(row.required("payload"))
            match(row, "activity_id", r.activityId())
            match(row, "planned_workout_id", r.plannedWorkoutId())
            match(row, "plan_version_id", r.planVersionId())
            require(responses.put(r.activityId(), r) == null) { "Duplicate response." }
            if (r.planVersionId() != null)
                require(
                    versions[r.planVersionId()]?.plan()?.workouts()?.any {
                        it.id() == r.plannedWorkoutId()
                    } == true
                ) {
                    "Response prescription is missing."
                }
        }
        val ids = mutableSetOf<String>()
        val proposals =
            tables.getValue(Table.PROPOSALS).map { row ->
                val p = RunningCoachingJson.decodeProposal(row.required("payload"))
                match(row, "id", p.id())
                match(row, "base_version_id", p.baseVersionId())
                require(ids.add(p.id()) && p.baseVersionId() in versions) {
                    "Invalid proposal identity/version."
                }
                val decision = row["decision"]
                val result = row["result_version_id"]
                require((decision == null) == (result == null)) {
                    "Decision and resulting version must be recorded together."
                }
                if (decision != null)
                    require(p.options().any { it.id() == decision } && result in versions) {
                        "Invalid proposal decision."
                    }
                Proposal(p, decision, result)
            }
        ids.clear()
        for (row in tables.getValue(Table.FEEDBACK)) {
            require(
                ids.add(row.required("activity_id")) && row.required("plan_version_id") in versions
            ) {
                "Invalid feedback identity/version."
            }
            val f = StoredJson.objectValue(row.required("payload"))
            require(f.requiredInteger("ruleVersion") in 1..RunScoreEngine.RULE_VERSION) {
                "Unsupported feedback rule version."
            }
            val score = f.integer("score")
            require(score == null || score in 1..10) { "Invalid Run Score." }
            require(f["components"] is JsonArray) { "Missing feedback components." }
        }
        return Snapshot(
            tables,
            versions,
            current.firstOrNull()?.get("version_id"),
            exercises,
            sessions,
            responses,
            proposals,
        )
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                for (table in Table.entries) put(
                    table.sqlName,
                    JsonArray(
                        snapshot.rows.getValue(table).map { row ->
                            buildJsonObject {
                                for (column in table.columns) put(
                                    column,
                                    StoredJson.nullable(row[column]),
                                )
                            }
                        }
                    ),
                )
            }
            .also { decode(it) }
}
