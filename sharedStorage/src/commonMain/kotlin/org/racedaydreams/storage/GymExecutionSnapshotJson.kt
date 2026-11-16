package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.bool
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.invalid
import org.racedaydreams.storage.GymStoredJson.isNull
import org.racedaydreams.storage.GymStoredJson.nullableLong
import org.racedaydreams.storage.GymStoredJson.nullableNumber
import org.racedaydreams.storage.GymStoredJson.nullableText
import org.racedaydreams.storage.GymStoredJson.number
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.whole
import org.racedaydreams.storage.GymStoredJson.write

/**
 * Original strict GymExecutionJson. Domain constructors replay state/event invariants on read.
 * Source IDs bind identity only; repositories must separately validate retained source content.
 */
object GymExecutionSnapshotJson {
    fun encode(execution: GymExecution): String = write(element(execution))

    fun decode(payload: String): GymExecution = decode(read(payload))

    internal fun element(execution: GymExecution): JsonObject = buildJsonObject {
        put("version", GymExecution.FORMAT_VERSION)
        put("id", execution.id())
        put("origin", execution.origin().name)
        put(
            "prescription",
            execution.prescription()?.let { FrozenGymWorkoutJson.element(it) } ?: JsonNull,
        )
        put("source", execution.source()?.let { source(it) } ?: JsonNull)
        put("startedAt", execution.startedAt().toString())
        put("offset", execution.offset().toString())
        put("status", execution.status().name)
        put("lastTick", tick(execution.lastTick()))
        put("recordedElapsedMillis", execution.recordedElapsedMillis())
        put("activeMillis", execution.activeMillis())
        put("selectedSlotId", StoredJson.nullable(execution.selectedSlotId()))
        put("slots", FrozenGymWorkoutJson.slots(execution.slots()))
        put(
            "actualSets",
            buildJsonArray {
                for (actual in execution.actualSets()) add(
                    buildJsonObject {
                        put("slotId", actual.slotId())
                        put("exercise", FrozenGymWorkoutJson.exercise(actual.exercise()))
                        put("actual", set(actual.actual()))
                    }
                )
            },
        )
        put(
            "events",
            buildJsonArray {
                for (event in execution.events()) add(
                    buildJsonObject {
                        put("sequence", event.sequence())
                        put("type", event.type().name)
                        put("slotId", StoredJson.nullable(event.slotId()))
                        put("actualSetId", StoredJson.nullable(event.actualSetId()))
                        put("recordedElapsedMillis", event.recordedElapsedMillis())
                        put("activeMillis", event.activeMillis())
                        put("observedAt", event.observedAt().toString())
                    }
                )
            },
        )
        put("completedSession", execution.completedSession()?.let { session(it) } ?: JsonNull)
        put("partial", execution.partial())
    }

    private fun decode(value: JsonObject): GymExecution {
        fields(
            value,
            "version",
            "id",
            "origin",
            "prescription",
            "source",
            "startedAt",
            "offset",
            "status",
            "lastTick",
            "recordedElapsedMillis",
            "activeMillis",
            "selectedSlotId",
            "slots",
            "actualSets",
            "events",
            "completedSession",
            "partial",
        )
        version(value, GymExecution.FORMAT_VERSION)
        val actual =
            array(value, "actualSets", 10000).map {
                val a = row(it)
                fields(a, "slotId", "exercise", "actual")
                GymExecutionSet(
                    text(a, "slotId", 256),
                    FrozenGymWorkoutJson.readExercise(child(a, "exercise")),
                    readSet(child(a, "actual")),
                )
            }
        val events =
            array(value, "events", 50000).map {
                val e = row(it)
                fields(
                    e,
                    "sequence",
                    "type",
                    "slotId",
                    "actualSetId",
                    "recordedElapsedMillis",
                    "activeMillis",
                    "observedAt",
                )
                GymExecution.Event(
                    integer(e, "sequence"),
                    choice<GymExecution.EventType>(e, "type"),
                    nullableText(e, "slotId", 256),
                    nullableText(e, "actualSetId", 256),
                    whole(e, "recordedElapsedMillis"),
                    whole(e, "activeMillis"),
                    StoredInstant.parse(text(e, "observedAt", 64)),
                )
            }
        return GymExecution(
            text(value, "id", 256),
            choice<GymExecution.Origin>(value, "origin"),
            if (isNull(value, "prescription")) null
            else FrozenGymWorkoutJson.decode(child(value, "prescription")),
            if (isNull(value, "source")) null else readSource(child(value, "source")),
            StoredInstant.parse(text(value, "startedAt", 64)),
            StoredOffset.parse(text(value, "offset", 16)),
            choice<GymExecution.Status>(value, "status"),
            readTick(child(value, "lastTick")),
            whole(value, "recordedElapsedMillis"),
            whole(value, "activeMillis"),
            nullableText(value, "selectedSlotId", 256),
            FrozenGymWorkoutJson.readSlots(array(value, "slots", 100)),
            actual,
            events,
            if (isNull(value, "completedSession")) null
            else readSession(child(value, "completedSession")),
            bool(value, "partial"),
        )
    }

    private fun source(source: GymExecution.SourceReference): JsonObject = buildJsonObject {
        put("kind", source.kind().name)
        put("id", source.id())
        put("revision", source.revision())
        put("sessionId", StoredJson.nullable(source.sessionId()))
    }

    private fun readSource(row: JsonObject): GymExecution.SourceReference {
        fields(row, "kind", "id", "revision", "sessionId")
        return GymExecution.SourceReference(
            choice<GymExecution.SourceKind>(row, "kind"),
            text(row, "id", 256),
            integer(row, "revision"),
            nullableText(row, "sessionId", 256),
        )
    }

    private fun tick(tick: GymExecution.Tick): JsonObject = buildJsonObject {
        put("continuityId", tick.continuityId())
        put("monotonicMillis", tick.monotonicMillis())
        put("wallTime", tick.wallTime().toString())
    }

    private fun readTick(row: JsonObject): GymExecution.Tick {
        fields(row, "continuityId", "monotonicMillis", "wallTime")
        return GymExecution.Tick(
            text(row, "continuityId", 256),
            whole(row, "monotonicMillis"),
            StoredInstant.parse(text(row, "wallTime", 64)),
        )
    }

    private fun set(set: StrengthSession.SetEntry): JsonObject = buildJsonObject {
        put("id", set.id())
        put("reps", set.reps())
        put("kilograms", set.kilograms())
        put("rpe", StoredJson.nullable(set.rpe()))
        put("warmUp", set.warmUp())
        put("durationSeconds", set.durationSeconds()?.let { JsonPrimitive(it) } ?: JsonNull)
        put("distanceMetres", StoredJson.nullable(set.distanceMetres()))
        put("repetitionsInReserve", StoredJson.nullable(set.repetitionsInReserve()))
    }

    private fun readSet(row: JsonObject): StrengthSession.SetEntry {
        fields(
            row,
            "id",
            "reps",
            "kilograms",
            "rpe",
            "warmUp",
            "durationSeconds",
            "distanceMetres",
            "repetitionsInReserve",
        )
        return StrengthSession.SetEntry(
            text(row, "id", 256),
            integer(row, "reps"),
            number(row, "kilograms"),
            nullableNumber(row, "rpe"),
            bool(row, "warmUp"),
            nullableLong(row, "durationSeconds"),
            nullableNumber(row, "distanceMetres"),
            nullableNumber(row, "repetitionsInReserve"),
        )
    }

    private fun session(session: StrengthSession): JsonObject = buildJsonObject {
        put("id", session.id())
        put("startedAt", session.startedAt().toString())
        put("offset", session.offset().toString())
        put("durationMillis", session.duration().toMillis())
        put("rpe", StoredJson.nullable(session.rpe()))
        put("notes", session.notes())
        put(
            "exercises",
            buildJsonArray {
                for (exercise in session.exercises()) add(
                    buildJsonObject {
                        put("exercise", FrozenGymWorkoutJson.exercise(exercise.exercise()))
                        put("sets", buildJsonArray { exercise.sets().forEach { add(set(it)) } })
                    }
                )
            },
        )
    }

    private fun readSession(row: JsonObject): StrengthSession {
        fields(row, "id", "startedAt", "offset", "durationMillis", "rpe", "notes", "exercises")
        val rows =
            array(row, "exercises", 100).map {
                val item = row(it)
                fields(item, "exercise", "sets")
                StrengthSession.ExerciseSets(
                    FrozenGymWorkoutJson.readExercise(child(item, "exercise")),
                    array(item, "sets", 100).map { readSet(row(it)) },
                )
            }
        val millis = whole(row, "durationMillis")
        if (millis <= 0 || millis > 12L * 60 * 60 * 1000) invalid("completed duration")
        return StrengthSession(
            text(row, "id", 256),
            StoredInstant.parse(text(row, "startedAt", 64)),
            StoredOffset.parse(text(row, "offset", 16)),
            Durations.ofMillis(millis),
            nullableNumber(row, "rpe"),
            text(row, "notes", 10000),
            rows,
        )
    }
}
