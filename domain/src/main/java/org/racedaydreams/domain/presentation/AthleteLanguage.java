package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.strength.StrengthExercise;

/**
 * Display-only explanations. Never changes an exercise, target, record key or training decision.
 */
public final class AthleteLanguage {
    private AthleteLanguage() {}

    public static String measurement(StrengthExercise exercise) {
        String weight =
                exercise.mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT
                        ? "the extra weight you add"
                        : "the weight you lift";
        return switch (exercise.measurement()) {
            case LOAD_REPS -> "Record " + weight + " and how many repetitions you complete.";
            case REPS_ONLY ->
                    "Record how many repetitions you complete. One repetition is one complete"
                            + " movement.";
            case DURATION -> "Record how long you hold or perform the exercise.";
            case LOAD_DURATION -> "Record " + weight + " and how long you perform the exercise.";
            case DISTANCE -> "Record how far you travel during the exercise.";
            case LOAD_DISTANCE -> "Record " + weight + " and how far you carry it.";
        };
    }

    public static String recordLabel(String key) {
        if (key.equals("Estimated 1RM")) return "Estimated maximum for one repetition";
        if (key.startsWith("Load × ")) {
            String repetitions = key.substring(7);
            return "Best weight for "
                    + repetitions
                    + (repetitions.equals("1") ? " repetition" : " repetitions");
        }
        if (key.equals("Bodyweight reps")) return "Most bodyweight repetitions";
        return key;
    }

    public static String runningPurpose(WorkoutCategory category) {
        return switch (category) {
            case RECOVERY_RUN ->
                    "Keep this run gentle. It is intended to fit between more demanding training"
                            + " days.";
            case EASY_RUN, ZONE_2_RUN ->
                    "Run at an easy, comfortable effort. You should be able to hold a conversation."
                            + " Follow any personal targets shown below; no heart-rate zone is"
                            + " assumed.";
            case STEADY_RUN ->
                    "Keep a controlled, steady effort. Follow the planned sections rather than"
                            + " turning this into a race.";
            case LONG_RUN ->
                    "Spend longer on your feet at the planned effort. Follow the sections below and"
                            + " adjust to conditions.";
            case PROGRESSION_RUN ->
                    "Build the effort gradually through the planned sections, rather than starting"
                            + " fast.";
            case TEMPO_THRESHOLD_RUN ->
                    "The main section is a sustained, challenging effort. Keep it controlled and"
                            + " follow the planned easier sections too.";
            case INTERVALS ->
                    "Alternate the harder sections with the planned recovery sections. Recovery is"
                            + " part of the workout.";
            case RACE_PACE_RUN ->
                    "Practise the planned race effort for the sections shown. Only use a pace"
                            + " target when one is provided.";
            case TIME_TRIAL ->
                    "This is a timed effort to review your current performance. Follow the"
                            + " prescribed effort; a maximal effort is not assumed.";
            case RACE ->
                    "Choose how to approach your race. Any targets are guidance, not a guaranteed"
                            + " result.";
            case CUSTOM_RUN ->
                    "Follow the sections you chose. Record what you actually do, even if you change"
                            + " the workout.";
        };
    }

    public static final String WORKING_SETS =
            "Working sets are the main sets you record, excluding warm-up practice sets. A set is a"
                    + " group of repetitions or one timed or distance-based effort.";
    public static final String GYM_TERMS =
            "One repetition is one complete movement. A set is a group of repetitions, or one timed"
                + " hold or distance-based effort. Warm-up sets are practice before the main work."
                + " Record only what you actually complete.";
    public static final String BACKUP =
            "This JSON file saves your app history, goals, programmes, gym sets, results, shoes and"
                + " settings. It does not include detailed GPS routes or sensor samples. Use"
                + " Complete backup in Record a run to save app history together with the detailed"
                + " recordings still on this device. Imported Health Connect records are not"
                + " copied; reading them again depends on the provider and your permissions. Keep"
                + " your backup somewhere you trust.";
}
