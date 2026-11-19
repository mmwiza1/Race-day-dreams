package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.invalid
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.write

/** Original independently versioned concrete preview. Decode never accepts/activates a revision. */
object ConcreteStrengthDraftPayloadJson {
    fun encode(draft: ConcreteStrengthProgrammeDraft): String =
        write(
            buildJsonObject {
                put("version", 1)
                put("kind", "concreteDraft")
                put("id", draft.id())
                put("revision", draft.revision())
                put("before", draft.before()?.let { reference(it) } ?: JsonNull)
                put("programme", StrengthProgrammingPayloadJson.element(draft.programme()))
                put("substitutions", changes(draft.substitutions()))
            }
        )

    fun decode(payload: String): ConcreteStrengthProgrammeDraft {
        val value = read(payload)
        fields(value, "version", "kind", "id", "revision", "before", "programme", "substitutions")
        version(value, 1)
        if (text(value, "kind", 40) != "concreteDraft") invalid("concrete draft kind")
        return ConcreteStrengthProgrammeDraft(
            text(value, "id", 256),
            integer(value, "revision"),
            if (isNull(value, "before")) null else readReference(child(value, "before")),
            StrengthProgrammingPayloadJson.decode(child(value, "programme")),
            readChanges(array(value, "substitutions", 312 * 12)),
        )
    }

    internal fun reference(value: StrengthProgrammingRepository.Reference): JsonObject =
        buildJsonObject {
            put("owner", value.owner().name)
            put("id", value.id())
            put("revision", value.revision())
        }

    internal fun readReference(value: JsonObject): StrengthProgrammingRepository.Reference {
        fields(value, "owner", "id", "revision")
        return StrengthProgrammingRepository.Reference(
            choice<StrengthProgrammingRepository.Owner>(value, "owner"),
            text(value, "id", 256),
            integer(value, "revision"),
        )
    }

    internal fun changes(values: List<StrengthProgrammeEditor.Change>): JsonArray = buildJsonArray {
        for (c in values) add(
            buildJsonObject {
                put("workoutId", c.workoutId())
                put("slotId", c.slotId())
                put("from", c.fromExerciseId())
                put("to", c.toExerciseId())
                put("reason", c.reason())
            }
        )
    }

    internal fun readChanges(values: JsonArray): List<StrengthProgrammeEditor.Change> =
        values.map {
            val c = row(it)
            fields(c, "workoutId", "slotId", "from", "to", "reason")
            StrengthProgrammeEditor.Change(
                text(c, "workoutId", 256),
                text(c, "slotId", 256),
                text(c, "from", 256),
                text(c, "to", 256),
                text(c, "reason", 2000),
            )
        }
}
