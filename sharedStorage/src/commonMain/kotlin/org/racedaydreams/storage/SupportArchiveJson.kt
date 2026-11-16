package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.domain.support.DreamRoutes
import org.racedaydreams.domain.support.RunningRoutines
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.storage.StoredJson.row
import org.racedaydreams.storage.SupportPayloadJson.array
import org.racedaydreams.storage.SupportPayloadJson.integer
import org.racedaydreams.storage.SupportPayloadJson.long
import org.racedaydreams.storage.SupportPayloadJson.number
import org.racedaydreams.storage.SupportPayloadJson.text
import org.racedaydreams.storage.SupportPayloadJson.version

/**
 * Original support-v1 evidence; no assignment, completion, response calculation or plan activation.
 */
object SupportArchiveJson {
    enum class Kind(val wire: String) {
        DREAM("dream"),
        ROUTINE("routine"),
        EXECUTION("execution"),
    }

    sealed interface Evidence {
        data class Dream(val assignment: DreamRoutes.Assignment) : Evidence

        data class Routine(val routineId: String, val at: Instant) : Evidence

        class Execution(val mode: String, steps: List<Step>) : Evidence {
            val steps = Immutable.copyList(steps)
        }
    }

    class Record
    internal constructor(
        val kind: Kind,
        val id: String,
        val payload: JsonObject,
        val evidence: Evidence,
    )

    class Snapshot internal constructor(records: List<Record>) {
        val records = Immutable.copyList(records)
    }

    /** Parent backup syntax and total allocation budget must already have been validated. */
    fun decode(root: JsonObject): Snapshot {
        version(root)
        val ids = mutableSetOf<Pair<Kind, String>>()
        return Snapshot(
            array(root, "rows", 100000).map { item ->
                val r = item.row()
                val wire = text(r, "kind")
                val kind =
                    Kind.entries.firstOrNull { it.wire == wire }
                        ?: error("Unsupported support kind.")
                val id = text(r, "id")
                require(id.length in 1..256 && ids.add(kind to id)) {
                    "Invalid or duplicate support identity."
                }
                val p = Json.parseToJsonElement(r.getValue("payload").row().toString()).row()
                Record(kind, id, p, evidence(kind, p))
            }
        )
    }

    fun element(snapshot: Snapshot): JsonObject =
        buildJsonObject {
                put("version", 1)
                put(
                    "rows",
                    JsonArray(
                        snapshot.records.map { r ->
                            buildJsonObject {
                                put("kind", r.kind.wire)
                                put("id", r.id)
                                put("payload", r.payload)
                            }
                        }
                    ),
                )
            }
            .also { decode(it) }

    private fun evidence(kind: Kind, p: JsonObject): Evidence {
        version(p)
        return when (kind) {
            Kind.DREAM ->
                Evidence.Dream(
                    DreamRoutes.Assignment(
                        text(p, "route"),
                        number(p, "start"),
                        number(p, "distance"),
                    )
                )
            Kind.ROUTINE -> {
                val id = text(p, "routine")
                RunningRoutines.get(id)
                Evidence.Routine(id, StoredInstant.parse(text(p, "at")))
            }
            Kind.EXECUTION -> {
                val steps = SupportPayloadJson.steps(p)
                val mode = text(p, "mode")
                require(mode in listOf("workout", "race", "quick", "custom", "repeat")) {
                    "Unsupported execution mode."
                }
                if (mode == "workout" || mode == "race")
                    require(
                        text(p, "planVersion").length in 1..256 &&
                            text(p, "workoutId").length in 1..256
                    )
                if ("workout" in p) PlannedWorkoutJson.read(p.getValue("workout").row())
                if ("events" in p) {
                    var lastActive = 0L
                    var lastMetres = 0.0
                    for (item in array(p, "events", 2000)) {
                        val e = item.row()
                        val active = long(e, "active")
                        val metres = number(e, "metres")
                        require(
                            integer(e, "step") in 0..steps.size &&
                                active >= lastActive &&
                                metres.isFinite() &&
                                metres >= lastMetres &&
                                text(e, "type") in
                                    listOf(
                                        "WORKOUT_STARTED",
                                        "STEP_STARTED",
                                        "STEP_COMPLETED",
                                        "STEP_SKIPPED",
                                        "WORKOUT_FINISHED_EARLY",
                                        "WORKOUT_COMPLETED",
                                    )
                        ) {
                            "Invalid retained execution event."
                        }
                        lastActive = active
                        lastMetres = metres
                    }
                }
                Evidence.Execution(mode, steps)
            }
        }
    }
}
