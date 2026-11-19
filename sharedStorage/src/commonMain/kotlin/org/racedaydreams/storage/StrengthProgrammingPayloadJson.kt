package org.racedaydreams.storage

import kotlinx.serialization.json.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.DayOfWeek
import org.racedaydreams.storage.GymStoredJson.array
import org.racedaydreams.storage.GymStoredJson.child
import org.racedaydreams.storage.GymStoredJson.choice
import org.racedaydreams.storage.GymStoredJson.fields
import org.racedaydreams.storage.GymStoredJson.integer
import org.racedaydreams.storage.GymStoredJson.invalid
import org.racedaydreams.storage.GymStoredJson.nullableText
import org.racedaydreams.storage.GymStoredJson.read
import org.racedaydreams.storage.GymStoredJson.row
import org.racedaydreams.storage.GymStoredJson.text
import org.racedaydreams.storage.GymStoredJson.version
import org.racedaydreams.storage.GymStoredJson.write

/** Original v1 authoring payloads. Frozen history never resolves against today's catalogue. */
object StrengthProgrammingPayloadJson {
    fun encode(programme: StrengthProgramme): String = write(element(programme))

    fun decode(payload: String): StrengthProgramme = decode(read(payload))

    fun encodeDraft(draft: StrengthProgrammeDraft): String = write(draftElement(draft))

    fun decodeDraft(payload: String): StrengthProgrammeDraft = readDraft(read(payload))

    fun encodeCustom(exercise: CustomStrengthExercise): String = write(customElement(exercise))

    fun decodeCustom(payload: String): CustomStrengthExercise = readCustom(read(payload))

    internal fun element(programme: StrengthProgramme): JsonObject = buildJsonObject {
        put("version", StrengthProgramme.FORMAT_VERSION)
        put("kind", "programme")
        put("id", programme.id())
        put("revision", programme.revision())
        put("parentRevision", programme.parentRevision())
        put("createdAt", programme.createdAt().toString())
        put("name", programme.name())
        put("origin", programme.origin().name)
        put("templateId", StoredJson.nullable(programme.templateId()))
        put("settings", settingsElement(programme.settings()))
        put("effectiveFrom", programme.effectiveFrom().toString())
        put("reason", programme.reason())
        put("tradeoffs", strings(programme.tradeoffs()))
        put(
            "blocks",
            buildJsonArray {
                for (block in programme.blocks()) add(
                    buildJsonObject {
                        put("id", block.id())
                        put("phase", block.phase().name)
                        put("name", block.name())
                        put("start", block.start().toString())
                        put("weeks", block.weeks())
                        put("explanation", block.explanation())
                        put(
                            "workouts",
                            buildJsonArray { block.workouts().forEach { add(workoutElement(it)) } },
                        )
                    }
                )
            },
        )
    }

    internal fun decode(value: JsonObject): StrengthProgramme {
        fields(
            value,
            "version",
            "kind",
            "id",
            "revision",
            "parentRevision",
            "createdAt",
            "name",
            "origin",
            "templateId",
            "settings",
            "blocks",
            "effectiveFrom",
            "reason",
            "tradeoffs",
        )
        kind(value, "programme")
        val blocks =
            array(value, "blocks", 12).map {
                val block = row(it)
                fields(block, "id", "phase", "name", "start", "weeks", "workouts", "explanation")
                StrengthProgramme.Block(
                    text(block, "id", 256),
                    choice<StrengthProgramme.Phase>(block, "phase"),
                    text(block, "name", 120),
                    StoredDate.parse(text(block, "start", 64)),
                    integer(block, "weeks"),
                    array(block, "workouts", 156).map { readWorkout(row(it)) },
                    text(block, "explanation", 2000),
                )
            }
        return StrengthProgramme(
            text(value, "id", 256),
            integer(value, "revision"),
            integer(value, "parentRevision"),
            StoredInstant.parse(text(value, "createdAt", 64)),
            text(value, "name", 120),
            choice<StrengthProgramme.Origin>(value, "origin"),
            nullableText(value, "templateId", 256),
            readSettings(child(value, "settings")),
            blocks,
            StoredDate.parse(text(value, "effectiveFrom", 64)),
            text(value, "reason", 2000),
            readStrings(array(value, "tradeoffs", 100), 2000),
        )
    }

    internal fun workoutElement(workout: StrengthProgramme.Workout): JsonObject = buildJsonObject {
        put("id", workout.id())
        put("date", workout.date().toString())
        put("expectedMinutes", workout.expectedMinutes())
        put("definition", FrozenGymWorkoutJson.element(workout.definition()))
        put("notes", StoredJson.nullable(workout.notes()))
    }

    internal fun readWorkout(workout: JsonObject): StrengthProgramme.Workout {
        fields(workout, "id", "date", "expectedMinutes", "definition", "notes")
        return StrengthProgramme.Workout(
            text(workout, "id", 256),
            StoredDate.parse(text(workout, "date", 64)),
            integer(workout, "expectedMinutes"),
            FrozenGymWorkoutJson.decode(child(workout, "definition")),
            nullableText(workout, "notes", 2000),
        )
    }

    internal fun draftElement(draft: StrengthProgrammeDraft): JsonObject = buildJsonObject {
        put("version", 1)
        put("kind", "draft")
        put("id", draft.id())
        put("revision", draft.revision())
        put("programmeId", draft.programmeId())
        put("baseRevision", draft.baseRevision())
        put("name", draft.name())
        put("settings", settingsElement(draft.settings()))
        put("start", draft.start().toString())
        put("origin", draft.origin().name)
        put("templateId", StoredJson.nullable(draft.templateId()))
        put(
            "blocks",
            buildJsonArray {
                for (block in draft.blocks()) add(
                    buildJsonObject {
                        put("id", block.id())
                        put("phase", block.phase().name)
                        put("name", block.name())
                        put("weeks", block.weeks())
                        put(
                            "days",
                            buildJsonArray {
                                for (day in block.days()) add(
                                    buildJsonObject {
                                        put("id", day.id())
                                        put("weekday", day.weekday().name)
                                        put("title", day.title())
                                        put("slots", FrozenGymWorkoutJson.slots(day.slots()))
                                        put("notes", StoredJson.nullable(day.notes()))
                                    }
                                )
                            },
                        )
                    }
                )
            },
        )
    }

    internal fun readDraft(value: JsonObject): StrengthProgrammeDraft {
        fields(
            value,
            "version",
            "kind",
            "id",
            "revision",
            "programmeId",
            "baseRevision",
            "name",
            "settings",
            "start",
            "blocks",
            "origin",
            "templateId",
        )
        kind(value, "draft")
        val blocks =
            array(value, "blocks", 12).map {
                val block = row(it)
                fields(block, "id", "phase", "name", "weeks", "days")
                val days =
                    array(block, "days", 6).map {
                        val day = row(it)
                        fields(day, "id", "weekday", "title", "slots", "notes")
                        StrengthProgrammeDraft.Day(
                            text(day, "id", 256),
                            choice<DayOfWeek>(day, "weekday"),
                            text(day, "title", 120),
                            FrozenGymWorkoutJson.readSlots(array(day, "slots", 12)),
                            nullableText(day, "notes", 2000),
                        )
                    }
                StrengthProgrammeDraft.WeekBlock(
                    text(block, "id", 256),
                    choice<StrengthProgramme.Phase>(block, "phase"),
                    text(block, "name", 120),
                    integer(block, "weeks"),
                    days,
                )
            }
        return StrengthProgrammeDraft(
            text(value, "id", 256),
            integer(value, "revision"),
            text(value, "programmeId", 256),
            integer(value, "baseRevision"),
            text(value, "name", 120),
            readSettings(child(value, "settings")),
            StoredDate.parse(text(value, "start", 64)),
            blocks,
            choice<StrengthProgramme.Origin>(value, "origin"),
            nullableText(value, "templateId", 256),
        )
    }

    internal fun customElement(exercise: CustomStrengthExercise): JsonObject = buildJsonObject {
        put("version", 1)
        put("kind", "custom")
        put("exercise", FrozenGymWorkoutJson.exercise(exercise.exercise()))
        put("bodyArea", exercise.bodyArea().name)
        put("role", exercise.role().name)
        put(
            "equipmentAlternatives",
            buildJsonArray { exercise.equipment().alternatives().forEach { add(enumArray(it)) } },
        )
        put("notes", StoredJson.nullable(exercise.notes()))
    }

    internal fun readCustom(value: JsonObject): CustomStrengthExercise {
        fields(
            value,
            "version",
            "kind",
            "exercise",
            "bodyArea",
            "role",
            "equipmentAlternatives",
            "notes",
        )
        kind(value, "custom")
        val alternatives =
            array(value, "equipmentAlternatives", 16).map {
                enumSet<ExerciseMetadata.Equipment>(
                    it as? JsonArray ?: invalid("equipment setup array")
                )
            }
        return CustomStrengthExercise(
            FrozenGymWorkoutJson.readExercise(child(value, "exercise")),
            choice<ExerciseMetadata.BodyArea>(value, "bodyArea"),
            choice<ExerciseMetadata.Role>(value, "role"),
            ExerciseMetadata.EquipmentRequirement(alternatives),
            nullableText(value, "notes", 2000),
        )
    }

    internal fun settingsElement(settings: StrengthProgramme.Settings): JsonObject =
        buildJsonObject {
            put("goal", settings.goal().name)
            put("priority", settings.priority().name)
            put("experience", settings.experience().name)
            put("days", enumArray(settings.days()))
            put("frequency", settings.frequency())
            put("sessionMinutes", settings.sessionMinutes())
            put("equipment", enumArray(settings.equipment()))
        }

    internal fun readSettings(value: JsonObject): StrengthProgramme.Settings {
        fields(
            value,
            "goal",
            "priority",
            "experience",
            "days",
            "frequency",
            "sessionMinutes",
            "equipment",
        )
        return StrengthProgramme.Settings(
            choice<GymProgramme.Goal>(value, "goal"),
            choice<GymProgramme.Priority>(value, "priority"),
            choice<GymProgramme.Experience>(value, "experience"),
            enumSet<DayOfWeek>(array(value, "days", 7)),
            integer(value, "frequency"),
            integer(value, "sessionMinutes"),
            enumSet<ExerciseMetadata.Equipment>(
                array(value, "equipment", ExerciseMetadata.Equipment.entries.size)
            ),
        )
    }

    private fun kind(value: JsonObject, expected: String) {
        version(value, 1)
        if (text(value, "kind", 16) != expected) invalid("programming payload kind")
    }

    private fun enumArray(values: Collection<Enum<*>>): JsonArray = buildJsonArray {
        values.map { it.name }.sorted().forEach { add(JsonPrimitive(it)) }
    }

    private inline fun <reified T : Enum<T>> enumSet(values: JsonArray): Set<T> {
        if (values.size > enumValues<T>().size) invalid("enum set limit")
        val parsed =
            values.map {
                if (it !is JsonPrimitive || !it.isString) invalid("non-text enum")
                enumValueOf<T>(it.content)
            }
        if (parsed.distinct().size != parsed.size) invalid("duplicate enum")
        // Original EnumSet iteration is ordinal, independently of alphabetical wire order.
        return parsed.sortedBy { it.ordinal }.toSet()
    }

    private fun strings(values: List<String>): JsonArray = buildJsonArray {
        values.forEach { add(JsonPrimitive(it)) }
    }

    private fun readStrings(values: JsonArray, maximum: Int): List<String> =
        values.map {
            if (it !is JsonPrimitive || !it.isString || it.content.length > maximum)
                invalid("bounded string list")
            it.content
        }
}
