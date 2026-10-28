package org.racedaydreams.domain.strength;

import java.util.List;

/** Transparent planning estimate, not measured exercise speed or actual elapsed time. */
public final class StrengthWorkoutBudget {
    private StrengthWorkoutBudget() {}

    public static int exerciseMinutes(GymWorkoutDefinition.Target target) {
        if (target == null)
            throw new IllegalArgumentException("An open exercise has no calculable time budget.");
        long working =
                target.seconds() != null
                        ? target.seconds()
                        : target.metres() != null
                                ? Math.round(target.metres() * 2)
                                : target.maximumReps() * 4L;
        long rest = target.restSeconds() == null ? 60 : target.restSeconds();
        return (int)
                Math.ceil(
                        (target.sets() * working + Math.max(0, target.sets() - 1) * rest + 60)
                                / 60.0);
    }

    public static int workoutMinutes(List<GymWorkoutDefinition.Slot> slots) {
        return 4 + slots.stream().mapToInt(s -> exerciseMinutes(s.target())).sum();
    }

    public static String explanation() {
        return "Planning estimate: four minutes to prepare, four seconds per repetition or two"
                + " seconds per metre, prescribed duration, rest between sets (one minute when"
                + " unspecified), and one minute per exercise change. Actual time varies.";
    }
}
