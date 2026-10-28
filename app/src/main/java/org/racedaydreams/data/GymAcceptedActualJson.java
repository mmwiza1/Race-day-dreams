package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.strength.StrengthExercise;
import org.racedaydreams.domain.strength.StrengthSession;

/**
 * The engine section uses the existing actual-row encoding, with a strict new-format reader. Legacy
 * coaching backups retain their older optional-field/default rules in CoachingJson.
 */
final class GymAcceptedActualJson {
    private GymAcceptedActualJson() {}

    static StrengthSession read(JSONObject json) {
        fields(json, "id", "startedAt", "offset", "duration", "rpe", "notes", "exercises");
        text(json, "id", 256);
        text(json, "startedAt", 64);
        text(json, "offset", 16);
        text(json, "duration", 64);
        nullableNumber(json, "rpe");
        text(json, "notes", 10000);
        var exercises = array(json, "exercises", 100);
        for (int i = 0; i < exercises.length(); i++) {
            JSONObject block = row(exercises, i);
            fields(block, "exercise", "sets");
            JSONObject exercise = child(block, "exercise");
            fields(exercise, "id", "name", "region", "mode", "estimate", "measurement");
            text(exercise, "id", 256);
            text(exercise, "name", 120);
            choice(exercise, "region", StrengthExercise.Region.class);
            choice(exercise, "mode", StrengthExercise.Mode.class);
            bool(exercise, "estimate");
            choice(exercise, "measurement", StrengthExercise.MeasurementProfile.class);
            var sets = array(block, "sets", 100);
            for (int k = 0; k < sets.length(); k++) {
                JSONObject set = row(sets, k);
                fields(set, "id", "reps", "kg", "rpe", "warmUp", "seconds", "metres", "rir");
                text(set, "id", 256);
                integer(set, "reps");
                number(set, "kg");
                nullableNumber(set, "rpe");
                bool(set, "warmUp");
                nullableLong(set, "seconds");
                nullableNumber(set, "metres");
                nullableNumber(set, "rir");
            }
        }
        try {
            return CoachingJson.strength(json);
        } catch (JSONException | java.time.DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid accepted gym actual.", invalid);
        }
    }
}
