package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Authored programme revision. Frozen workout definitions use the existing gym target/exercise
 * contract; this model never contains actual sets. Blocks are concrete dated prescriptions rather
 * than labels interpreted again at execution time.
 */
public record StrengthProgramme(
        String id,
        int revision,
        int parentRevision,
        Instant createdAt,
        String name,
        Origin origin,
        String templateId,
        Settings settings,
        List<Block> blocks,
        LocalDate effectiveFrom,
        String reason,
        List<String> tradeoffs) {
    public static final int FORMAT_VERSION = 1;

    public enum Origin {
        GENERATED,
        CUSTOM,
        TEMPLATE
    }

    public enum Phase {
        FOUNDATION,
        DEVELOPMENT,
        INTENSIFICATION,
        RECOVERY_ASSESSMENT
    }

    public record Settings(
            GymProgramme.Goal goal,
            GymProgramme.Priority priority,
            GymProgramme.Experience experience,
            Set<DayOfWeek> days,
            int frequency,
            int sessionMinutes,
            Set<ExerciseMetadata.Equipment> equipment) {
        public Settings {
            Objects.requireNonNull(goal);
            Objects.requireNonNull(priority);
            Objects.requireNonNull(experience);
            days = Immutable.copySet(days);
            equipment = Immutable.copySet(equipment);
            if (days.isEmpty()
                    || days.contains(null)
                    || equipment.contains(null)
                    || frequency < 1
                    || frequency > 6
                    || frequency > days.size()
                    || sessionMinutes < 20
                    || sessionMinutes > 120)
                throw new IllegalArgumentException("Choose 1–6 available days and 20–120 minutes.");
        }
    }

    public record Workout(
            String id,
            LocalDate date,
            int expectedMinutes,
            GymWorkoutDefinition definition,
            String notes) {
        public Workout {
            GymWorkoutDefinition.requireId(id, "programme session");
            Objects.requireNonNull(date);
            Objects.requireNonNull(definition);
            if (expectedMinutes < 1 || expectedMinutes > 180)
                throw new IllegalArgumentException("Choose a realistic session duration.");
            notes = text(notes, 2000, true, "workout notes");
            if (definition.slots().size() > 12)
                throw new IllegalArgumentException(
                        "A programme workout supports up to 12 exercises.");
            var exerciseIds = new HashSet<String>();
            for (var slot : definition.slots()) {
                if (!exerciseIds.add(slot.exercise().id()))
                    throw new IllegalArgumentException(
                            "Combine repeated exercise sets in one slot.");
                if (slot.supersetGroup() != null)
                    throw new IllegalArgumentException(
                            "Grouped execution is outside this release.");
            }
        }
    }

    public record Block(
            String id,
            Phase phase,
            String name,
            LocalDate start,
            int weeks,
            List<Workout> workouts,
            String explanation) {
        public Block {
            GymWorkoutDefinition.requireId(id, "block");
            Objects.requireNonNull(phase);
            name = text(name, 120, false, "block name");
            Objects.requireNonNull(start);
            if (weeks < 1 || weeks > 26)
                throw new IllegalArgumentException("A block spans 1–26 programme weeks.");
            workouts = Immutable.copyList(workouts);
            explanation = text(explanation, 2000, false, "block explanation");
            if (workouts.isEmpty() || workouts.size() > weeks * 6)
                throw new IllegalArgumentException(
                        "A block needs 1–6 workouts per programme week.");
            var dates = new HashSet<LocalDate>();
            var ids = new HashSet<String>();
            for (var workout : workouts) {
                long day = ChronoUnit.DAYS.between(start, workout.date());
                if (day < 0
                        || day >= weeks * 7L
                        || !dates.add(workout.date())
                        || !ids.add(workout.id()))
                    throw new IllegalArgumentException(
                            "Workouts need unique dates/IDs within the block.");
            }
            // Date placement is relative to the actual start, including mid-week starts.
            for (int week = 0; week < weeks; week++) {
                var first = start.plusWeeks(week);
                long count =
                        workouts.stream()
                                .filter(
                                        w ->
                                                !w.date().isBefore(first)
                                                        && w.date().isBefore(first.plusWeeks(1)))
                                .count();
                if (count < 1 || count > 6)
                    throw new IllegalArgumentException("Every block week needs 1–6 sessions.");
            }
        }

        public LocalDate end() {
            return start.plusWeeks(weeks).minusDays(1);
        }
    }

    public StrengthProgramme {
        GymWorkoutDefinition.requireId(id, "programme");
        if (revision < 1 || parentRevision < 0 || parentRevision != revision - 1)
            throw new IllegalArgumentException("Programme revisions form an explicit lineage.");
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(origin);
        Objects.requireNonNull(settings);
        Objects.requireNonNull(effectiveFrom);
        name = text(name, 120, false, "programme name");
        reason = text(reason, 2000, false, "revision reason");
        if (origin == Origin.TEMPLATE) GymWorkoutDefinition.requireId(templateId, "template");
        else if (templateId != null)
            throw new IllegalArgumentException("Only templates have template provenance.");
        blocks = Immutable.copyList(blocks);
        tradeoffs = Immutable.copyList(tradeoffs);
        if (blocks.isEmpty() || blocks.size() > 12 || tradeoffs.size() > 100)
            throw new IllegalArgumentException(
                    "A programme needs 1–12 blocks and bounded explanations.");
        var ids = new HashSet<String>();
        var sessions = new HashSet<String>();
        LocalDate next = blocks.get(0).start();
        int weeks = 0;
        for (var block : blocks) {
            if (!ids.add(block.id()) || !block.start().equals(next))
                throw new IllegalArgumentException(
                        "Programme blocks must be contiguous and uniquely identified.");
            weeks += block.weeks();
            next = block.end().plusDays(1);
            for (var workout : block.workouts()) {
                if (!sessions.add(workout.id()))
                    throw new IllegalArgumentException("Duplicate session ID.");
            }
        }
        if (weeks > 52
                || effectiveFrom.isBefore(blocks.get(0).start())
                || effectiveFrom.isAfter(next.minusDays(1)))
            throw new IllegalArgumentException(
                    "A programme spans at most 52 weeks with an in-range effective date.");
        for (var note : tradeoffs) text(note, 2000, false, "trade-off");
    }

    public LocalDate start() {
        return blocks.get(0).start();
    }

    public LocalDate end() {
        return blocks.get(blocks.size() - 1).end();
    }

    public List<Workout> workouts() {
        return blocks.stream()
                .flatMap(b -> b.workouts().stream())
                .sorted(Comparator.comparing(Workout::date).thenComparing(Workout::id))
                .collect(
                        java.util.stream.Collectors.collectingAndThen(
                                java.util.stream.Collectors.toList(), Immutable::copyList));
    }

    static String text(String value, int maximum, boolean optional, String label) {
        if (value == null && optional) return null;
        if (value == null || value.isBlank() || value.length() > maximum)
            throw new IllegalArgumentException(
                    "Provide " + label + " (up to " + maximum + " characters).");
        return value;
    }
}
