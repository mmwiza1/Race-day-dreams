package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Exclusion
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.row

/** Existing choices-v1/v2: reversible preferences and acknowledgements, never workout evidence. */
object StrengthChoicesArchiveJson {
    class Snapshot
    internal constructor(
        val version: Int,
        choices: List<SqliteStrengthChoices.Choice>,
        acknowledged: List<String>,
        exclusions: List<Exclusion>,
        internal val payload: JsonObject,
    ) {
        val choices = Immutable.copyList(choices)
        val acknowledged = Immutable.copyList(acknowledged)
        val exclusions = Immutable.copyList(exclusions)
    }

    fun decode(root: JsonObject): Snapshot {
        val version = root.requiredInteger("version")
        require(version in 1..2) { "Unsupported exercise choices." }
        if (version == 2)
            GymStoredJson.fields(root, "version", "choices", "acknowledged", "exclusions")
        else require("exclusions" !in root) { "Unexpected exercise exclusions." }
        val ids = mutableSetOf<String>()
        val choices =
            root.array("choices", 10000).map { item ->
                val row = item.row()
                val id = row.required("id")
                require(id.length <= 200 && ids.add(id)) { "Invalid duplicate exercise choice." }
                SqliteStrengthChoices.Choice(id, row.bool("favourite"), row.bool("archived"))
            }
        ids.clear()
        val acknowledged =
            root.array("acknowledged", 100000).map { raw ->
                require(
                    raw is JsonPrimitive &&
                        raw.isString &&
                        raw.content.isNotEmpty() &&
                        raw.content.length <= 500 &&
                        ids.add(raw.content)
                ) {
                    "Invalid PB acknowledgement."
                }
                raw.content
            }
        val exclusions =
            if (version == 1) emptyList()
            else {
                val p = root.getValue("exclusions").row()
                GymStoredJson.fields(p, "version", "entries")
                require(p.requiredInteger("version") == 1) { "Unsupported exercise exclusions." }
                ids.clear()
                p.array("entries", 10000).map { item ->
                    val e = item.row()
                    GymStoredJson.fields(e, "exerciseId", "reason", "excludedAt")
                    val value =
                        Exclusion(
                            e.required("exerciseId"),
                            Reason.valueOf(e.required("reason")),
                            StoredInstant.parse(e.required("excludedAt")),
                        )
                    require(ids.add(value.exerciseId())) { "Duplicate exercise exclusion." }
                    value
                }
            }
        return Snapshot(
            version,
            choices,
            acknowledged,
            exclusions,
            Json.parseToJsonElement(root.toString()).row(),
        )
    }

    /**
     * Original version and optional legacy fields survive; no preference is applied by encoding.
     */
    fun element(snapshot: Snapshot): JsonObject = snapshot.payload.also { decode(it) }
}
