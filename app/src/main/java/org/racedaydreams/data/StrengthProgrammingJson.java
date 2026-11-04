package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.StrengthProgramme.*;

import java.time.*;
import java.util.*;

/**
 * Independently versioned authoring payloads. Reuses the strict bounded gym parser and frozen
 * workout codec. Unknown/future fields, coercions and malformed nested structures fail closed;
 * decoding never resolves a historical exercise through today's catalogue.
 */
public final class StrengthProgrammingJson {
    private StrengthProgrammingJson() {}

    public static String write(StrengthProgramme programme) {
        return GymJsonSupport.write(encode(programme));
    }

    public static StrengthProgramme read(String payload) {
        return decode(GymJsonSupport.read(payload));
    }

    public static String writeDraft(StrengthProgrammeDraft draft) {
        return GymJsonSupport.write(encodeDraft(draft));
    }

    public static StrengthProgrammeDraft readDraft(String payload) {
        return decodeDraft(GymJsonSupport.read(payload));
    }

    public static String writeCustom(CustomStrengthExercise exercise) {
        return GymJsonSupport.write(encodeCustom(exercise));
    }

    public static CustomStrengthExercise readCustom(String payload) {
        return decodeCustom(GymJsonSupport.read(payload));
    }

    public static JSONObject encode(StrengthProgramme programme) {
        var blocks = new JSONArray();
        for (var block : programme.blocks()) {
            var workouts = new JSONArray();
            for (var workout : block.workouts()) workouts.put(encodeWorkout(workout));
            blocks.put(
                    object(
                            "id",
                            block.id(),
                            "phase",
                            block.phase().name(),
                            "name",
                            block.name(),
                            "start",
                            block.start().toString(),
                            "weeks",
                            block.weeks(),
                            "workouts",
                            workouts,
                            "explanation",
                            block.explanation()));
        }
        return object(
                "version",
                StrengthProgramme.FORMAT_VERSION,
                "kind",
                "programme",
                "id",
                programme.id(),
                "revision",
                programme.revision(),
                "parentRevision",
                programme.parentRevision(),
                "createdAt",
                programme.createdAt().toString(),
                "name",
                programme.name(),
                "origin",
                programme.origin().name(),
                "templateId",
                programme.templateId(),
                "settings",
                settings(programme.settings()),
                "blocks",
                blocks,
                "effectiveFrom",
                programme.effectiveFrom().toString(),
                "reason",
                programme.reason(),
                "tradeoffs",
                strings(programme.tradeoffs()));
    }

    public static StrengthProgramme decode(JSONObject json) {
        fields(
                json,
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
                "tradeoffs");
        kind(json, "programme");
        var blocks = new ArrayList<Block>();
        var values = array(json, "blocks", 12);
        for (int i = 0; i < values.length(); i++) {
            var block = row(values, i);
            fields(block, "id", "phase", "name", "start", "weeks", "workouts", "explanation");
            var workouts = new ArrayList<Workout>();
            var rows = array(block, "workouts", 156);
            for (int n = 0; n < rows.length(); n++) workouts.add(decodeWorkout(row(rows, n)));
            blocks.add(
                    new Block(
                            text(block, "id", 256),
                            choice(block, "phase", Phase.class),
                            text(block, "name", 120),
                            date(block, "start"),
                            integer(block, "weeks"),
                            workouts,
                            text(block, "explanation", 2000)));
        }
        return new StrengthProgramme(
                text(json, "id", 256),
                integer(json, "revision"),
                integer(json, "parentRevision"),
                instant(json, "createdAt"),
                text(json, "name", 120),
                choice(json, "origin", Origin.class),
                nullableText(json, "templateId", 256),
                settings(child(json, "settings")),
                blocks,
                date(json, "effectiveFrom"),
                text(json, "reason", 2000),
                strings(array(json, "tradeoffs", 100), 2000));
    }

    /** Shared frozen row; its enclosing programme/association owns the format version. */
    static JSONObject encodeWorkout(Workout workout) {
        return object(
                "id",
                workout.id(),
                "date",
                workout.date().toString(),
                "expectedMinutes",
                workout.expectedMinutes(),
                "definition",
                GymWorkoutJson.encode(workout.definition()),
                "notes",
                workout.notes());
    }

    static Workout decodeWorkout(JSONObject workout) {
        fields(workout, "id", "date", "expectedMinutes", "definition", "notes");
        return new Workout(
                text(workout, "id", 256),
                date(workout, "date"),
                integer(workout, "expectedMinutes"),
                GymWorkoutJson.decode(child(workout, "definition")),
                nullableText(workout, "notes", 2000));
    }

    public static JSONObject encodeDraft(StrengthProgrammeDraft draft) {
        var blocks = new JSONArray();
        for (var block : draft.blocks()) {
            var days = new JSONArray();
            for (var day : block.days())
                days.put(
                        object(
                                "id",
                                day.id(),
                                "weekday",
                                day.weekday().name(),
                                "title",
                                day.title(),
                                "slots",
                                GymWorkoutJson.slots(day.slots()),
                                "notes",
                                day.notes()));
            blocks.put(
                    object(
                            "id",
                            block.id(),
                            "phase",
                            block.phase().name(),
                            "name",
                            block.name(),
                            "weeks",
                            block.weeks(),
                            "days",
                            days));
        }
        return object(
                "version",
                1,
                "kind",
                "draft",
                "id",
                draft.id(),
                "revision",
                draft.revision(),
                "programmeId",
                draft.programmeId(),
                "baseRevision",
                draft.baseRevision(),
                "name",
                draft.name(),
                "settings",
                settings(draft.settings()),
                "start",
                draft.start().toString(),
                "blocks",
                blocks,
                "origin",
                draft.origin().name(),
                "templateId",
                draft.templateId());
    }

    public static StrengthProgrammeDraft decodeDraft(JSONObject json) {
        fields(
                json,
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
                "templateId");
        kind(json, "draft");
        var blocks = new ArrayList<StrengthProgrammeDraft.WeekBlock>();
        var values = array(json, "blocks", 12);
        for (int i = 0; i < values.length(); i++) {
            var block = row(values, i);
            fields(block, "id", "phase", "name", "weeks", "days");
            var days = new ArrayList<StrengthProgrammeDraft.Day>();
            var rows = array(block, "days", 6);
            for (int n = 0; n < rows.length(); n++) {
                var day = row(rows, n);
                fields(day, "id", "weekday", "title", "slots", "notes");
                days.add(
                        new StrengthProgrammeDraft.Day(
                                text(day, "id", 256),
                                choice(day, "weekday", DayOfWeek.class),
                                text(day, "title", 120),
                                GymWorkoutJson.slots(array(day, "slots", 12)),
                                nullableText(day, "notes", 2000)));
            }
            blocks.add(
                    new StrengthProgrammeDraft.WeekBlock(
                            text(block, "id", 256),
                            choice(block, "phase", Phase.class),
                            text(block, "name", 120),
                            integer(block, "weeks"),
                            days));
        }
        return new StrengthProgrammeDraft(
                text(json, "id", 256),
                integer(json, "revision"),
                text(json, "programmeId", 256),
                integer(json, "baseRevision"),
                text(json, "name", 120),
                settings(child(json, "settings")),
                date(json, "start"),
                blocks,
                choice(json, "origin", Origin.class),
                nullableText(json, "templateId", 256));
    }

    public static JSONObject encodeCustom(CustomStrengthExercise exercise) {
        var alternatives = new JSONArray();
        for (var setup : exercise.equipment().alternatives()) alternatives.put(enums(setup));
        return object(
                "version",
                1,
                "kind",
                "custom",
                "exercise",
                GymWorkoutJson.exercise(exercise.exercise()),
                "bodyArea",
                exercise.bodyArea().name(),
                "role",
                exercise.role().name(),
                "equipmentAlternatives",
                alternatives,
                "notes",
                exercise.notes());
    }

    public static CustomStrengthExercise decodeCustom(JSONObject json) {
        fields(
                json,
                "version",
                "kind",
                "exercise",
                "bodyArea",
                "role",
                "equipmentAlternatives",
                "notes");
        kind(json, "custom");
        var setups = new ArrayList<Set<ExerciseMetadata.Equipment>>();
        var values = array(json, "equipmentAlternatives", 16);
        for (int i = 0; i < values.length(); i++) {
            Object value = values.opt(i);
            if (!(value instanceof JSONArray)) throw invalid("equipment setup array");
            setups.add(enumSet((JSONArray) value, ExerciseMetadata.Equipment.class));
        }
        return new CustomStrengthExercise(
                GymWorkoutJson.exercise(child(json, "exercise")),
                choice(json, "bodyArea", ExerciseMetadata.BodyArea.class),
                choice(json, "role", ExerciseMetadata.Role.class),
                new ExerciseMetadata.EquipmentRequirement(setups),
                nullableText(json, "notes", 2000));
    }

    static JSONObject settings(Settings settings) {
        return object(
                "goal",
                settings.goal().name(),
                "priority",
                settings.priority().name(),
                "experience",
                settings.experience().name(),
                "days",
                enums(settings.days()),
                "frequency",
                settings.frequency(),
                "sessionMinutes",
                settings.sessionMinutes(),
                "equipment",
                enums(settings.equipment()));
    }

    static Settings settings(JSONObject json) {
        fields(
                json,
                "goal",
                "priority",
                "experience",
                "days",
                "frequency",
                "sessionMinutes",
                "equipment");
        return new Settings(
                choice(json, "goal", GymProgramme.Goal.class),
                choice(json, "priority", GymProgramme.Priority.class),
                choice(json, "experience", GymProgramme.Experience.class),
                enumSet(array(json, "days", 7), DayOfWeek.class),
                integer(json, "frequency"),
                integer(json, "sessionMinutes"),
                enumSet(
                        array(json, "equipment", ExerciseMetadata.Equipment.values().length),
                        ExerciseMetadata.Equipment.class));
    }

    private static void kind(JSONObject json, String kind) {
        version(json, 1);
        if (!text(json, "kind", 16).equals(kind)) throw invalid("programming payload kind");
    }

    private static LocalDate date(JSONObject json, String key) {
        try {
            return LocalDate.parse(text(json, key, 64));
        } catch (DateTimeException e) {
            throw invalid("programme date");
        }
    }

    private static JSONArray enums(Collection<? extends Enum<?>> values) {
        var result = new JSONArray();
        values.stream().map(Enum::name).sorted().forEach(result::put);
        return result;
    }

    private static <T extends Enum<T>> Set<T> enumSet(JSONArray values, Class<T> type) {
        if (values.length() > type.getEnumConstants().length) throw invalid("enum set limit");
        var result = EnumSet.noneOf(type);
        for (int i = 0; i < values.length(); i++) {
            var value = values.opt(i);
            if (!(value instanceof String) || !result.add(Enum.valueOf(type, (String) value)))
                throw invalid("duplicate or non-text enum");
        }
        return result;
    }

    private static JSONArray strings(List<String> values) {
        var result = new JSONArray();
        for (var value : values) result.put(value);
        return result;
    }

    private static List<String> strings(JSONArray values, int length) {
        var result = new ArrayList<String>();
        for (int i = 0; i < values.length(); i++) {
            Object value = values.opt(i);
            if (!(value instanceof String) || ((String) value).length() > length)
                throw invalid("bounded string list");
            result.add((String) value);
        }
        return result;
    }
}
