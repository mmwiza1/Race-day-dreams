package org.racedaydreams.domain.strength;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record StrengthSession(String id, Instant startedAt, ZoneOffset offset, Duration duration,
        Double rpe, String notes, List<ExerciseSets> exercises) {
    public record SetEntry(String id, int reps, double kilograms, Double rpe, boolean warmUp) {
        public SetEntry {
            if (id == null || id.trim().isEmpty() || reps < 1 || reps > 500 || !Double.isFinite(kilograms)
                    || kilograms < 0 || kilograms > 1500) throw new IllegalArgumentException("Check set ID, reps and load.");
            checkRpe(rpe);
        }
    }
    public record ExerciseSets(StrengthExercise exercise, List<SetEntry> sets) {
        public ExerciseSets {
            Objects.requireNonNull(exercise); sets = org.racedaydreams.domain.coaching.Immutable.copyList(sets);
            if (sets.isEmpty() || sets.size() > 100) throw new IllegalArgumentException("Record 1–100 sets per exercise.");
            for (SetEntry set : sets) {
                if (exercise.mode() == StrengthExercise.Mode.BODYWEIGHT && set.kilograms() != 0)
                    throw new IllegalArgumentException("Use a weighted-bodyweight exercise for added load.");
            }
        }
    }
    public StrengthSession {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Session ID is required.");
        Objects.requireNonNull(startedAt); Objects.requireNonNull(offset); Objects.requireNonNull(duration);
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofHours(12)) > 0)
            throw new IllegalArgumentException("Session duration must be positive and at most 12 hours.");
        checkRpe(rpe); notes = notes == null ? "" : notes;
        if (notes.length() > 10000) throw new IllegalArgumentException("Notes are too long.");
        exercises = org.racedaydreams.domain.coaching.Immutable.copyList(exercises);
        if (exercises.isEmpty() || exercises.size() > 100) throw new IllegalArgumentException("Record 1–100 exercises.");
        HashSet<String> ids = new HashSet<>();
        for (ExerciseSets exercise : exercises) for (SetEntry set : exercise.sets())
            if (!ids.add(set.id())) throw new IllegalArgumentException("Set IDs must be unique within a workout.");
    }
    static void checkRpe(Double rpe) {
        if (rpe != null && (!Double.isFinite(rpe) || rpe < 0 || rpe > 10))
            throw new IllegalArgumentException("RPE must be between 0 and 10.");
    }
}
