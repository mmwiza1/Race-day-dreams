package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.invalid
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.nullableInteger
import org.racedaydreams.storage.GymStoredJson.nullableLong
import org.racedaydreams.storage.GymStoredJson.nullableNumber
import org.racedaydreams.storage.GymStoredJson.nullableText
import org.racedaydreams.storage.GymStoredJson.number
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.write

/** Original strict v1 GymWorkoutJson, distinct from legacy CoachingJson actual-session payloads. */
object FrozenGymWorkoutJson {
    fun encode(workout: GymWorkoutDefinition): String = write(element(workout))

    fun decode(payload: String): GymWorkoutDefinition = decode(read(payload))

    internal fun element(workout: GymWorkoutDefinition): JsonObject = buildJsonObject {
        put("version", GymWorkoutDefinition.FORMAT_VERSION)
        put("id", workout.id())
        put("revision", workout.revision())
        put("createdAt", workout.createdAt().toString())
        put("title", workout.title())
        put("slots", slots(workout.slots()))
    }

    internal fun decode(row: JsonObject): GymWorkoutDefinition {
        fields(row, "version", "id", "revision", "createdAt", "title", "slots")
        version(row, GymWorkoutDefinition.FORMAT_VERSION)
        return GymWorkoutDefinition(
            text(row, "id", 256),
            integer(row, "revision"),
            StoredInstant.parse(text(row, "createdAt", 64)),
            text(row, "title", 120),
            readSlots(array(row, "slots", 100)),
        )
    }

    internal fun slots(slots: List<GymWorkoutDefinition.Slot>): JsonArray = buildJsonArray {
        for (slot in slots) add(
            buildJsonObject {
                put("id", slot.id())
                put("exercise", exercise(slot.exercise()))
                put("target", slot.target()?.let { target(it) } ?: JsonNull)
                put("supersetGroup", StoredJson.nullable(slot.supersetGroup()))
            }
        )
    }

    internal fun readSlots(values: JsonArray): List<GymWorkoutDefinition.Slot> {
        if (values.size > 100) invalid("slot limit")
        return values.map {
            val slot = row(it)
            fields(slot, "id", "exercise", "target", "supersetGroup")
            GymWorkoutDefinition.Slot(
                text(slot, "id", 256),
                readExercise(child(slot, "exercise")),
                if (isNull(slot, "target")) null else readTarget(child(slot, "target")),
                nullableText(slot, "supersetGroup", 256),
            )
        }
    }

    internal fun exercise(exercise: StrengthExercise): JsonObject {
        if (exercise.id().length > 256) invalid("exercise ID limit")
        return buildJsonObject {
            put("id", exercise.id())
            put("name", exercise.name())
            put("region", exercise.region().name)
            put("mode", exercise.mode().name)
            put("estimateOneRepMax", exercise.estimateOneRepMax())
            put("measurement", exercise.measurement().name)
        }
    }

    internal fun readExercise(row: JsonObject): StrengthExercise {
        fields(row, "id", "name", "region", "mode", "estimateOneRepMax", "measurement")
        return StrengthExercise(
            text(row, "id", 256),
            text(row, "name", 120),
            choice<StrengthExercise.Region>(row, "region"),
            choice<StrengthExercise.Mode>(row, "mode"),
            bool(row, "estimateOneRepMax"),
            choice<StrengthExercise.MeasurementProfile>(row, "measurement"),
        )
    }

    private fun target(target: GymWorkoutDefinition.Target): JsonObject = buildJsonObject {
        put("sets", target.sets())
        put("minimumReps", target.minimumReps()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("maximumReps", target.maximumReps()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("seconds", target.seconds()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("metres", StoredJson.nullable(target.metres()))
        put(
            "effort",
            target.effort()?.let {
                buildJsonObject {
                    put("scale", it.scale().name)
                    put("minimum", it.minimum())
                    put("maximum", it.maximum())
                }
            } ?: JsonNull,
        )
        put("restSeconds", target.restSeconds()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("referenceLoadKg", StoredJson.nullable(target.referenceLoadKg()))
    }

    private fun readTarget(row: JsonObject): GymWorkoutDefinition.Target {
        fields(
            row,
            "sets",
            "minimumReps",
            "maximumReps",
            "seconds",
            "metres",
            "effort",
            "restSeconds",
            "referenceLoadKg",
        )
        val effort =
            if (isNull(row, "effort")) null
            else
                child(row, "effort").let {
                    fields(it, "scale", "minimum", "maximum")
                    GymWorkoutDefinition.EffortRange(
                        choice<GymWorkoutDefinition.EffortScale>(it, "scale"),
                        number(it, "minimum"),
                        number(it, "maximum"),
                    )
                }
        return GymWorkoutDefinition.Target(
            integer(row, "sets"),
            nullableInteger(row, "minimumReps"),
            nullableInteger(row, "maximumReps"),
            nullableLong(row, "seconds"),
            nullableNumber(row, "metres"),
            effort,
            nullableInteger(row, "restSeconds"),
            nullableNumber(row, "referenceLoadKg"),
        )
    }
}
