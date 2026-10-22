package org.racedaydreams.domain.strength;

import java.util.List;
import java.util.Objects;

/** Extensible definitions; sessions retain the definition used when recorded. */
public record StrengthExercise(String id, String name, Region region, Mode mode, boolean estimateOneRepMax, MeasurementProfile measurement) {
    public enum MeasurementProfile { LOAD_REPS, REPS_ONLY, DURATION, LOAD_DURATION, DISTANCE, LOAD_DISTANCE;
        public boolean usesLoad() { return this == LOAD_REPS || this == LOAD_DURATION || this == LOAD_DISTANCE; }
        public boolean usesReps() { return this == LOAD_REPS || this == REPS_ONLY; }
        public boolean usesDuration() { return this == DURATION || this == LOAD_DURATION; }
        public boolean usesDistance() { return this == DISTANCE || this == LOAD_DISTANCE; }
    }
    public StrengthExercise(String id, String name, Region region, Mode mode, boolean estimateOneRepMax) {
        this(id, name, region, mode, estimateOneRepMax, mode == Mode.BODYWEIGHT ? MeasurementProfile.REPS_ONLY : MeasurementProfile.LOAD_REPS);
    }
    public enum Region { UPPER, LOWER, FULL }
    public enum Mode { WEIGHTED, BODYWEIGHT, WEIGHTED_BODYWEIGHT }
    public StrengthExercise {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty() || name.length() > 120)
            throw new IllegalArgumentException("Exercise needs an ID and a name of up to 120 characters.");
        Objects.requireNonNull(region); Objects.requireNonNull(mode); Objects.requireNonNull(measurement);
        if (estimateOneRepMax && measurement != MeasurementProfile.LOAD_REPS) throw new IllegalArgumentException("Estimated 1RM requires load and reps.");
        if (mode != Mode.WEIGHTED && estimateOneRepMax)
            throw new IllegalArgumentException("Estimated 1RM needs a suitable weighted exercise.");
    }
    public static List<StrengthExercise> starters() { return ExerciseCatalog.exercises(); }
}
