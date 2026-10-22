package org.racedaydreams.domain.strength;

import java.util.List;
import java.util.Objects;

/** Extensible definitions; sessions retain the definition used when recorded. */
public record StrengthExercise(String id, String name, Region region, Mode mode, boolean estimateOneRepMax) {
    public enum Region { UPPER, LOWER, FULL }
    public enum Mode { WEIGHTED, BODYWEIGHT, WEIGHTED_BODYWEIGHT }
    public StrengthExercise {
        if (id == null || id.isBlank() || name == null || name.isBlank() || name.length() > 120)
            throw new IllegalArgumentException("Exercise needs an ID and a name of up to 120 characters.");
        Objects.requireNonNull(region); Objects.requireNonNull(mode);
        if (mode != Mode.WEIGHTED && estimateOneRepMax)
            throw new IllegalArgumentException("Estimated 1RM needs a suitable weighted exercise.");
    }
    public static List<StrengthExercise> starters() {
        return List.of(weighted("back-squat", "Back Squat", Region.LOWER),
            weighted("front-squat", "Front Squat", Region.LOWER), weighted("deadlift", "Deadlift", Region.LOWER),
            weighted("trap-bar-deadlift", "Trap-Bar Deadlift", Region.LOWER),
            weighted("romanian-deadlift", "Romanian Deadlift", Region.LOWER),
            weighted("bench-press", "Bench Press", Region.UPPER), weighted("incline-bench-press", "Incline Bench Press", Region.UPPER),
            weighted("overhead-press", "Overhead Press", Region.UPPER), weighted("hip-thrust", "Hip Thrust", Region.LOWER),
            weighted("split-squat", "Bulgarian Split Squat", Region.LOWER), weighted("lunge", "Lunge", Region.LOWER),
            weighted("leg-press", "Leg Press", Region.LOWER), body("pull-up", "Pull-Up", Mode.BODYWEIGHT),
            body("chin-up", "Chin-Up", Mode.BODYWEIGHT), body("weighted-pull-up", "Weighted Pull-Up", Mode.WEIGHTED_BODYWEIGHT),
            body("push-up", "Push-Up", Mode.BODYWEIGHT), body("weighted-push-up", "Weighted Push-Up", Mode.WEIGHTED_BODYWEIGHT),
            weighted("row", "Row", Region.UPPER), weighted("calf-raise", "Calf Raise", Region.LOWER));
    }
    private static StrengthExercise weighted(String id, String name, Region region) {
        return new StrengthExercise(id, name, region, Mode.WEIGHTED, true);
    }
    private static StrengthExercise body(String id, String name, Mode mode) {
        return new StrengthExercise(id, name, Region.UPPER, mode, false);
    }
}
