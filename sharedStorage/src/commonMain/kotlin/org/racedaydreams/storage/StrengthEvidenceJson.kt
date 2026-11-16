package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.StoredJson.array
import org.racedaydreams.storage.StoredJson.bool
import org.racedaydreams.storage.StoredJson.exactLong
import org.racedaydreams.storage.StoredJson.number
import org.racedaydreams.storage.StoredJson.prose
import org.racedaydreams.storage.StoredJson.required
import org.racedaydreams.storage.StoredJson.requiredInteger
import org.racedaydreams.storage.StoredJson.requiredNumber
import org.racedaydreams.storage.StoredJson.row

/** Original CoachingJson actuals. Decode frozen definitions, never today's catalogue or targets. */
object StrengthEvidenceJson {
    fun encodeExercise(exercise: StrengthExercise): String = exercise(exercise).toString()

    fun decodeExercise(text: String): StrengthExercise = readExercise(StoredJson.objectValue(text))

    internal fun exercise(exercise: StrengthExercise): JsonObject = buildJsonObject {
        put("id", exercise.id())
        put("name", exercise.name())
        put("region", exercise.region().name)
        put("mode", exercise.mode().name)
        put("estimate", exercise.estimateOneRepMax())
        put("measurement", exercise.measurement().name)
    }

    internal fun readExercise(row: JsonObject): StrengthExercise {
        val mode = StrengthExercise.Mode.valueOf(row.required("mode"))
        val measurement =
            if (row.containsKey("measurement"))
                StrengthExercise.MeasurementProfile.valueOf(row.required("measurement"))
            else if (mode == StrengthExercise.Mode.BODYWEIGHT)
                StrengthExercise.MeasurementProfile.REPS_ONLY
            else StrengthExercise.MeasurementProfile.LOAD_REPS
        return StrengthExercise(
            row.required("id"),
            row.required("name"),
            StrengthExercise.Region.valueOf(row.required("region")),
            mode,
            row.bool("estimate"),
            measurement,
        )
    }

    fun encodeSession(session: StrengthSession): String = session(session).toString()

    internal fun session(session: StrengthSession): JsonObject = buildJsonObject {
        put("id", session.id())
        put("startedAt", session.startedAt().toString())
        put("offset", session.offset().toString())
        put("duration", session.duration().toString())
        put("rpe", StoredJson.nullable(session.rpe()))
        put("notes", session.notes())
        put(
            "exercises",
            buildJsonArray {
                for (exercise in session.exercises()) add(
                    buildJsonObject {
                        put("exercise", exercise(exercise.exercise()))
                        put("sets", buildJsonArray { exercise.sets().forEach { add(set(it)) } })
                    }
                )
            },
        )
    }

    internal fun set(set: StrengthSession.SetEntry): JsonObject = buildJsonObject {
        put("id", set.id())
        put("reps", set.reps())
        put("kg", set.kilograms())
        put("rpe", StoredJson.nullable(set.rpe()))
        put("warmUp", set.warmUp())
        put("seconds", set.durationSeconds()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("metres", StoredJson.nullable(set.distanceMetres()))
        put("rir", StoredJson.nullable(set.repetitionsInReserve()))
    }

    fun decodeSession(text: String): StrengthSession = readSession(StoredJson.objectValue(text))

    internal fun readSession(row: JsonObject): StrengthSession =
        StrengthSession(
            row.required("id"),
            StoredInstant.parse(row.required("startedAt")),
            StoredOffset.parse(row.required("offset")),
            StoredDuration.parse(row.required("duration")),
            row.number("rpe"),
            row.prose("notes"),
            row.array("exercises", 100).map {
                val item = it.row()
                StrengthSession.ExerciseSets(
                    readExercise(item.getValue("exercise").row()),
                    item.array("sets", 100).map { readSet(it.row()) },
                )
            },
        )

    internal fun readSet(row: JsonObject): StrengthSession.SetEntry =
        StrengthSession.SetEntry(
            row.required("id"),
            row.requiredInteger("reps"),
            row.requiredNumber("kg"),
            row.number("rpe"),
            row.bool("warmUp"),
            if (row["seconds"] == null || row["seconds"] == JsonNull) null
            else row.exactLong("seconds"),
            row.number("metres"),
            row.number("rir"),
        )

    /** New engine/association sections require every field, unlike historical coaching payloads. */
    fun encodeAcceptedSession(actual: StrengthSession): String =
        GymStoredJson.write(session(actual)).also { decodeAcceptedSession(it) }

    fun decodeAcceptedSession(payload: String): StrengthSession =
        readAcceptedSession(GymStoredJson.read(payload))

    internal fun readAcceptedSession(value: JsonObject): StrengthSession {
        with(GymStoredJson) {
            fields(value, "id", "startedAt", "offset", "duration", "rpe", "notes", "exercises")
            text(value, "id", 256)
            text(value, "startedAt", 64)
            text(value, "offset", 16)
            text(value, "duration", 64)
            nullableNumber(value, "rpe")
            text(value, "notes", 10000)
            for (item in array(value, "exercises", 100)) {
                val block = row(item)
                fields(block, "exercise", "sets")
                val exercise = child(block, "exercise")
                fields(exercise, "id", "name", "region", "mode", "estimate", "measurement")
                text(exercise, "id", 256)
                text(exercise, "name", 120)
                choice<StrengthExercise.Region>(exercise, "region")
                choice<StrengthExercise.Mode>(exercise, "mode")
                bool(exercise, "estimate")
                choice<StrengthExercise.MeasurementProfile>(exercise, "measurement")
                for (item in array(block, "sets", 100)) {
                    val set = row(item)
                    fields(set, "id", "reps", "kg", "rpe", "warmUp", "seconds", "metres", "rir")
                    text(set, "id", 256)
                    integer(set, "reps")
                    number(set, "kg")
                    nullableNumber(set, "rpe")
                    bool(set, "warmUp")
                    nullableLong(set, "seconds")
                    nullableNumber(set, "metres")
                    nullableNumber(set, "rir")
                }
            }
        }
        return readSession(value)
    }
}
