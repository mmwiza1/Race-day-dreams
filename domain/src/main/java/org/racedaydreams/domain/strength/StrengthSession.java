package org.racedaydreams.domain.strength;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record StrengthSession(String id, Instant startedAt, ZoneOffset offset, Duration duration,
        Double rpe, String notes, List<ExerciseSets> exercises) {
    public record SetEntry(String id, int reps, double kilograms, Double rpe, boolean warmUp, Long durationSeconds, Double distanceMetres, Double repetitionsInReserve) {
        public SetEntry(String id,int reps,double kilograms,Double rpe,boolean warmUp,Long durationSeconds,Double distanceMetres){this(id,reps,kilograms,rpe,warmUp,durationSeconds,distanceMetres,null);}
        public Double coachingRpe(){return rpe!=null?rpe:repetitionsInReserve==null?null:10-repetitionsInReserve;}
        public SetEntry(String id, int reps, double kilograms, Double rpe, boolean warmUp) { this(id,reps,kilograms,rpe,warmUp,null,null,null); }
        public SetEntry {
            if (id == null || id.trim().isEmpty() || reps < 0 || reps > 500 || !Double.isFinite(kilograms)
                    || kilograms < 0 || kilograms > 1500) throw new IllegalArgumentException("Check set ID, reps and load.");
            if (durationSeconds != null && (durationSeconds <= 0 || durationSeconds > 43200)) throw new IllegalArgumentException("Check set duration.");
            if (distanceMetres != null && (!Double.isFinite(distanceMetres) || distanceMetres <= 0 || distanceMetres > 100000)) throw new IllegalArgumentException("Check set distance.");
            if ((reps > 0 ? 1 : 0) + (durationSeconds != null ? 1 : 0) + (distanceMetres != null ? 1 : 0) != 1) throw new IllegalArgumentException("Record exactly one of repetitions, duration or distance.");
            if(repetitionsInReserve!=null&&(!Double.isFinite(repetitionsInReserve)||repetitionsInReserve<0||repetitionsInReserve>10||reps==0||rpe!=null))throw new IllegalArgumentException("Record RPE or rep-based RIR, not both.");
            checkRpe(rpe);
        }
    }
    public record ExerciseSets(StrengthExercise exercise, List<SetEntry> sets) {
        public ExerciseSets {
            Objects.requireNonNull(exercise); sets = org.racedaydreams.domain.coaching.Immutable.copyList(sets);
            if (sets.isEmpty() || sets.size() > 100) throw new IllegalArgumentException("Record 1–100 sets per exercise.");
            for (SetEntry set : sets) {
                var profile = exercise.measurement();
                if (profile.usesReps() != (set.reps() > 0) || profile.usesDuration() != (set.durationSeconds() != null) || profile.usesDistance() != (set.distanceMetres() != null)) throw new IllegalArgumentException("Set does not match the exercise measurement profile.");
                if (!profile.usesLoad() && set.kilograms() != 0) throw new IllegalArgumentException("This exercise does not use external load.");
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
