package org.racedaydreams.domain.strength;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Original conservative preparation heuristic. Suggestions are never performed set evidence. */
public final class GymWarmup {
    private GymWarmup() {}

    public record Ramp(double kilograms, int repetitions) {}

    public record Suggestion(String instruction, String why, List<Ramp> ramps) {
        public Suggestion {
            ramps = List.copyOf(ramps);
        }
    }

    public static Suggestion suggest(
            GymWorkoutDefinition.Slot slot,
            List<StrengthSession> history,
            List<GymExecutionSet> today,
            GymProgramme.Experience experience,
            Instant now) {
        Objects.requireNonNull(slot);
        Objects.requireNonNull(now);
        Objects.requireNonNull(experience);
        if (today.stream()
                .anyMatch(s -> s.exercise().equals(slot.exercise()) && !s.actual().warmUp()))
            return new Suggestion(
                    "You have already recorded working sets for this exercise. Prepare again only"
                            + " if useful.",
                    "No additional ramp is suggested after working sets.",
                    List.of());
        var metadata = ExerciseMetadataCatalog.find(slot.exercise().id());
        if (metadata != null
                && today.stream()
                        .filter(s -> !s.actual().warmUp())
                        .map(s -> ExerciseMetadataCatalog.find(s.exercise().id()))
                        .filter(Objects::nonNull)
                        .anyMatch(m -> m.primaryRole() == metadata.primaryRole()))
            return new Suggestion(
                    "You have practised a related movement. Start with light, controlled"
                            + " preparation if needed.",
                    "Related recorded work reduces the need for another numerical ramp.",
                    List.of());
        var fallback =
                new Suggestion(
                        "Practise the movement gently before working sets. Choose a light load or"
                                + " an easier version; stop if uncomfortable.",
                        "No trustworthy recent exact-exercise working-load evidence is available."
                                + " No kilograms are invented.",
                        List.of());
        if (metadata == null
                || !metadata.roles().contains(ExerciseMetadata.Role.PRIMARY_STRENGTH)
                || !slot.exercise().measurement().usesReps()
                || slot.exercise().mode() != StrengthExercise.Mode.WEIGHTED
                || metadata.roles().contains(ExerciseMetadata.Role.POWER)) return fallback;
        for (var session :
                history.stream()
                        .filter(
                                s ->
                                        !s.startedAt().isAfter(now)
                                                && !s.startedAt()
                                                        .isBefore(now.minus(Duration.ofDays(28))))
                        .sorted(
                                java.util.Comparator.comparing(StrengthSession::startedAt)
                                        .reversed())
                        .toList()) {
            var eligible = new ArrayList<StrengthSession.SetEntry>();
            for (var exercise : session.exercises())
                if (exercise.exercise().equals(slot.exercise()))
                    for (var set : exercise.sets())
                        if (!set.warmUp()
                                && set.kilograms() > 0
                                && set.reps() >= 3
                                && set.reps() <= 12
                                && set.coachingRpe() != null
                                && set.coachingRpe() >= 4
                                && set.coachingRpe() <= 8) eligible.add(set);
            if (eligible.size() < 2) continue;
            double working =
                    eligible.stream()
                            .mapToDouble(StrengthSession.SetEntry::kilograms)
                            .min()
                            .orElseThrow();
            if (slot.target() != null && slot.target().referenceLoadKg() != null)
                working = Math.min(working, slot.target().referenceLoadKg());
            if (working < 10) return fallback;
            // Main strength movements only: two low-fatigue practice sets, not a universal1RM
            // recipe.
            var ramps =
                    List.of(
                            new Ramp(Math.floor(working * .4 * 2) / 2, 6),
                            new Ramp(
                                    Math.floor(working * .65 * 2) / 2,
                                    experience == GymProgramme.Experience.BEGINNER ? 4 : 3));
            return new Suggestion(
                    "Optional light practice sets before your working sets. Adjust for the"
                            + " equipment and how the movement feels.",
                    "Two recent exact-exercise working sets with recorded effort support these"
                        + " lower-load examples. They are not a working-weight recommendation or"
                        + " actual results.",
                    ramps);
        }
        return fallback;
    }
}
