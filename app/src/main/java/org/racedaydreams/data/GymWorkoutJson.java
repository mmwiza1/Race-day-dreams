package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;

import java.util.ArrayList;
import java.util.List;

/** Explicit v1 frozen workout payload, independent of the current canonical exercise catalogue. */
public final class GymWorkoutJson {
    private GymWorkoutJson() {}

    public static String write(GymWorkoutDefinition workout) { return GymJsonSupport.write(encode(workout)); }

    public static GymWorkoutDefinition read(String payload) { return decode(GymJsonSupport.read(payload)); }

    public static JSONObject encode(GymWorkoutDefinition workout) {
        if (workout == null) throw invalid("workout");
        return object(
                "version", GymWorkoutDefinition.FORMAT_VERSION,
                "id", workout.id(), "revision", workout.revision(),
                "createdAt", workout.createdAt().toString(), "title", workout.title(),
                "slots", slots(workout.slots()));
    }

    public static GymWorkoutDefinition decode(JSONObject json) {
        fields(json, "version", "id", "revision", "createdAt", "title", "slots");
        version(json, GymWorkoutDefinition.FORMAT_VERSION);
        return new GymWorkoutDefinition(
                text(json, "id", 256), integer(json, "revision"),
                instant(json, "createdAt"), text(json, "title", 120),
                slots(array(json, "slots", 100)));
    }

    static JSONArray slots(List<GymWorkoutDefinition.Slot> slots) {
        JSONArray values = new JSONArray();
        for (var slot : slots)
            values.put(object("id", slot.id(), "exercise", exercise(slot.exercise()),
                    "target", slot.target() == null ? null : target(slot.target()),
                    "supersetGroup", slot.supersetGroup()));
        return values;
    }

    static List<GymWorkoutDefinition.Slot> slots(JSONArray values) {
        if (values.length() > 100) throw invalid("slot limit");
        List<GymWorkoutDefinition.Slot> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            JSONObject slot = row(values, i);
            fields(slot, "id", "exercise", "target", "supersetGroup");
            result.add(new GymWorkoutDefinition.Slot(
                    text(slot, "id", 256), exercise(child(slot, "exercise")),
                    isNull(slot, "target") ? null : target(child(slot, "target")),
                    nullableText(slot, "supersetGroup", 256)));
        }
        return result;
    }

    static JSONObject exercise(StrengthExercise exercise) {
        if (exercise.id().length() > 256) throw invalid("exercise ID limit");
        return object("id", exercise.id(), "name", exercise.name(), "region", exercise.region().name(),
                "mode", exercise.mode().name(), "estimateOneRepMax", exercise.estimateOneRepMax(),
                "measurement", exercise.measurement().name());
    }

    static StrengthExercise exercise(JSONObject json) {
        fields(json, "id", "name", "region", "mode", "estimateOneRepMax", "measurement");
        return new StrengthExercise(text(json, "id", 256), text(json, "name", 120),
                choice(json, "region", StrengthExercise.Region.class),
                choice(json, "mode", StrengthExercise.Mode.class), bool(json, "estimateOneRepMax"),
                choice(json, "measurement", StrengthExercise.MeasurementProfile.class));
    }

    private static JSONObject target(GymWorkoutDefinition.Target target) {
        var effort = target.effort();
        return object("sets", target.sets(), "minimumReps", target.minimumReps(),
                "maximumReps", target.maximumReps(), "seconds", target.seconds(), "metres", target.metres(),
                "effort", effort == null ? null : object("scale", effort.scale().name(),
                        "minimum", effort.minimum(), "maximum", effort.maximum()),
                "restSeconds", target.restSeconds(), "referenceLoadKg", target.referenceLoadKg());
    }

    private static GymWorkoutDefinition.Target target(JSONObject json) {
        fields(json, "sets", "minimumReps", "maximumReps", "seconds", "metres", "effort",
                "restSeconds", "referenceLoadKg");
        GymWorkoutDefinition.EffortRange effort = null;
        if (!isNull(json, "effort")) {
            var value = child(json, "effort");
            fields(value, "scale", "minimum", "maximum");
            effort = new GymWorkoutDefinition.EffortRange(
                    choice(value, "scale", GymWorkoutDefinition.EffortScale.class),
                    number(value, "minimum"), number(value, "maximum"));
        }
        return new GymWorkoutDefinition.Target(integer(json, "sets"), nullableInteger(json, "minimumReps"),
                nullableInteger(json, "maximumReps"), nullableLong(json, "seconds"),
                nullableNumber(json, "metres"), effort, nullableInteger(json, "restSeconds"),
                nullableNumber(json, "referenceLoadKg"));
    }
}
