package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.isJavaBlank
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.Decision
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.write

/** Original v1 athlete decisions. Parsing never activates a programme or edits actual evidence. */
object StrengthProgrammingDecisionJson {
    fun encode(value: Decision): String = write(element(value))

    fun decode(payload: String): Decision = decode(read(payload))

    internal fun element(value: Decision): JsonObject = buildJsonObject {
        put("version", 1)
        put("id", value.id())
        put(
            "before",
            value.before()?.let { ConcreteStrengthDraftPayloadJson.reference(it) } ?: JsonNull,
        )
        put(
            "after",
            value.after()?.let { ConcreteStrengthDraftPayloadJson.reference(it) } ?: JsonNull,
        )
        put("at", value.at().toString())
        put("reason", value.reason())
        put("protectedWorkoutIds", JsonArray(value.protectedWorkoutIds().map { JsonPrimitive(it) }))
        put("substitutions", ConcreteStrengthDraftPayloadJson.changes(value.substitutions()))
    }

    internal fun decode(value: JsonObject): Decision {
        fields(
            value,
            "version",
            "id",
            "before",
            "after",
            "at",
            "reason",
            "protectedWorkoutIds",
            "substitutions",
        )
        version(value, 1)
        val protected =
            array(value, "protectedWorkoutIds", 312).map {
                require(
                    it is JsonPrimitive &&
                        it.isString &&
                        !isJavaBlank(it.content) &&
                        it.content.length <= 256
                ) {
                    "Invalid protected session identity."
                }
                it.content
            }
        return Decision(
            text(value, "id", 256),
            if (isNull(value, "before")) null
            else ConcreteStrengthDraftPayloadJson.readReference(child(value, "before")),
            if (isNull(value, "after")) null
            else ConcreteStrengthDraftPayloadJson.readReference(child(value, "after")),
            StoredInstant.parse(text(value, "at", 64)),
            text(value, "reason", 2000),
            protected,
            ConcreteStrengthDraftPayloadJson.readChanges(array(value, "substitutions", 312 * 12)),
        )
    }
}
