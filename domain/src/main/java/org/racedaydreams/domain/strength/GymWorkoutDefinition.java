package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Date-free, preserved intentions. No actual set is created from a workout definition. */
public record GymWorkoutDefinition(
        String id, int revision, Instant createdAt, String title, List<Slot> slots) {
    public static final int FORMAT_VERSION = 1;

    public enum EffortScale {
        RPE,
        RIR
    }

    public record EffortRange(EffortScale scale, double minimum, double maximum) {
        public EffortRange {
            Objects.requireNonNull(scale, "effort scale");
            if (!Double.isFinite(minimum)
                    || !Double.isFinite(maximum)
                    || minimum < 0
                    || maximum > 10
                    || maximum < minimum) {
                throw new IllegalArgumentException("Effort guidance needs an ordered 0–10 range.");
            }
        }
    }

    /** Exactly one measurement is prescribed. A slot with no target is explicitly open. */
    public record Target(
            int sets,
            Integer minimumReps,
            Integer maximumReps,
            Long seconds,
            Double metres,
            EffortRange effort,
            Integer restSeconds,
            Double referenceLoadKg) {
        public Target {
            if (sets < 1 || sets > 100)
                throw new IllegalArgumentException("Choose 1–100 target sets.");
            if ((minimumReps == null) != (maximumReps == null)
                    || minimumReps != null
                            && (minimumReps < 1
                                    || maximumReps < minimumReps
                                    || maximumReps > 500)) {
                throw new IllegalArgumentException("Choose an ordered 1–500 repetition range.");
            }
            if ((minimumReps != null ? 1 : 0) + (seconds != null ? 1 : 0) + (metres != null ? 1 : 0)
                            != 1
                    || seconds != null && (seconds < 1 || seconds > 43200)
                    || metres != null
                            && (!Double.isFinite(metres) || metres <= 0 || metres > 100000)) {
                throw new IllegalArgumentException(
                        "Choose one valid repetition, duration or distance target.");
            }
            if (restSeconds != null && (restSeconds < 0 || restSeconds > 1800)) {
                throw new IllegalArgumentException("Optional rest must be 0–1800 seconds.");
            }
            if (referenceLoadKg != null
                    && (!Double.isFinite(referenceLoadKg)
                            || referenceLoadKg < 0
                            || referenceLoadKg > 1500)) {
                throw new IllegalArgumentException("Optional load reference must be 0–1500 kg.");
            }
        }

        public Target withoutLoadReference() {
            return new Target(
                    sets, minimumReps, maximumReps, seconds, metres, effort, restSeconds, null);
        }
    }

    /**
     * The exercise definition is frozen here, independently of the current catalogue/user rename.
     */
    public record Slot(String id, StrengthExercise exercise, Target target, String supersetGroup) {
        public Slot {
            requireId(id, "exercise slot");
            Objects.requireNonNull(exercise, "exercise");
            if (supersetGroup != null) requireId(supersetGroup, "superset group");
            if (target != null) {
                var measurement = exercise.measurement();
                if (measurement.usesReps() != (target.minimumReps() != null)
                        || measurement.usesDuration() != (target.seconds() != null)
                        || measurement.usesDistance() != (target.metres() != null)) {
                    throw new IllegalArgumentException(
                            "Target does not match the frozen exercise measurement.");
                }
                if (target.referenceLoadKg() != null
                        && (!measurement.usesLoad()
                                || exercise.mode() == StrengthExercise.Mode.BODYWEIGHT)) {
                    throw new IllegalArgumentException(
                            "This exercise does not support an external load reference.");
                }
                if (target.effort() != null
                        && target.effort().scale() == EffortScale.RIR
                        && !measurement.usesReps()) {
                    throw new IllegalArgumentException("RIR guidance requires repetitions.");
                }
            }
        }
    }

    public GymWorkoutDefinition {
        requireId(id, "workout");
        if (revision < 1) throw new IllegalArgumentException("Workout revision must be positive.");
        Objects.requireNonNull(createdAt, "created at");
        if (title == null || title.trim().isEmpty() || title.length() > 120) {
            throw new IllegalArgumentException("Workout needs a title of up to 120 characters.");
        }
        slots = Immutable.copyList(slots);
        validateSlots(slots, false);
    }

    static void validateSlots(List<Slot> slots, boolean allowEmpty) {
        if ((!allowEmpty && slots.isEmpty()) || slots.size() > 100) {
            throw new IllegalArgumentException("A workout requires 1–100 exercise slots.");
        }
        var ids = new HashSet<String>();
        var groupCounts = new HashMap<String, Integer>();
        var finishedGroups = new HashSet<String>();
        String previous = null;
        for (Slot slot : slots) {
            Objects.requireNonNull(slot, "exercise slot");
            if (!ids.add(slot.id()))
                throw new IllegalArgumentException("Exercise slot IDs must be unique.");
            String group = slot.supersetGroup();
            if (!Objects.equals(group, previous)) {
                if (previous != null) finishedGroups.add(previous);
                if (group != null && finishedGroups.contains(group)) {
                    throw new IllegalArgumentException("Superset exercises must stay together.");
                }
            }
            if (group != null) groupCounts.put(group, groupCounts.getOrDefault(group, 0) + 1);
            previous = group;
        }
        for (int count : groupCounts.values()) {
            if (count < 2)
                throw new IllegalArgumentException("A superset needs at least two exercises.");
        }
    }

    static void requireId(String value, String label) {
        if (value == null || value.trim().isEmpty() || value.length() > 256) {
            throw new IllegalArgumentException("A stable " + label + " ID is required.");
        }
    }
}
