package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Presentation only: no progression calculations, inferred actuals or RPE-to-RIR conversions. */
public final class StrengthPrescriptionText {
    private StrengthPrescriptionText() {}

    public static List<String> instructions(GymWorkoutDefinition.Slot slot) {
        var target = slot.target();
        if (target == null)
            return List.of(
                    "Choose your own task for this exercise. No set or effort target is"
                            + " prescribed.");
        var lines = new ArrayList<String>();
        String task =
                target.minimumReps() != null
                        ? range(target.minimumReps(), target.maximumReps())
                                + (target.maximumReps() == 1 ? " repetition" : " repetitions")
                        : target.seconds() != null
                                ? duration(target.seconds())
                                : number(target.metres()) + " metres";
        lines.add(
                "Complete "
                        + target.sets()
                        + (target.sets() == 1 ? " set of " : " sets of ")
                        + task
                        + ".");
        if (target.restSeconds() != null)
            lines.add(
                    target.restSeconds() == 0
                            ? "No rest is prescribed between sets."
                            : "Rest for " + duration(target.restSeconds()) + " between sets.");
        var effort = target.effort();
        if (effort != null) {
            if (effort.scale() == GymWorkoutDefinition.EffortScale.RIR)
                lines.add(
                        "Finish each set with "
                                + range(effort.minimum(), effort.maximum())
                                + " repetitions left before failure.");
            else {
                String low = effortWord(effort.minimum()), high = effortWord(effort.maximum());
                lines.add(
                        "Aim for "
                                + (low.equals(high) ? low : low + " to " + high)
                                + " perceived effort. Use how hard the set feels to guide your"
                                + " effort.");
            }
        }
        if (slot.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT)
            lines.add("Use your body weight; no added weight is prescribed.");
        return List.copyOf(lines);
    }

    public static String effortDetails(GymWorkoutDefinition.EffortRange effort) {
        if (effort == null) return "No effort target is specified.";
        return effort.scale() == GymWorkoutDefinition.EffortScale.RPE
                ? "RPE (rating of perceived effort): "
                        + range(effort.minimum(), effort.maximum())
                        + " out of 10. This describes how hard the set feels."
                : "RIR (repetitions in reserve): "
                        + range(effort.minimum(), effort.maximum())
                        + " repetitions left before failure. This counts repetitions, not perceived"
                        + " effort.";
    }

    public static List<String> weight(
            GymWorkoutDefinition.Slot slot, GymWorkoutDefinition.Target proposed) {
        if (!slot.exercise().measurement().usesLoad()
                || slot.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT)
            return List.of("No external weight is prescribed for this exercise.");
        var lines = new ArrayList<String>();
        Double current = slot.target() == null ? null : slot.target().referenceLoadKg();
        Double next = proposed == null ? null : proposed.referenceLoadKg();
        String label =
                slot.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT
                        ? "added-weight reference"
                        : "weight reference";
        lines.add(
                current == null
                        ? "Your current target has no " + label + "."
                        : "Your current " + label + " is " + number(current) + " kg.");
        if (next != null && current != null && number(next).equals(number(current))) {
            lines.add("Your " + label + " is unchanged in this suggestion.");
        } else if (next != null) {
            lines.add(
                    "The suggested new "
                            + label
                            + " is "
                            + number(next)
                            + " kg."
                            + (current != null
                                    ? " That is "
                                            + BigDecimal.valueOf(next)
                                                    .subtract(BigDecimal.valueOf(current))
                                                    .abs()
                                                    .stripTrailingZeros()
                                                    .toPlainString()
                                            + " kg "
                                            + (next >= current ? "more" : "less")
                                            + " than your current reference."
                                    : ""));
        }
        lines.add(
                "A reference is optional, not a weight you must lift or a recorded result."
                        + " Choose a manageable weight and record what you actually do.");
        return List.copyOf(lines);
    }

    /** A display comparison, never a recommendation or an inference from logged weights. */
    public record TargetComparison(
            String key, String label, String currentValue, String proposedValue, boolean changed) {}

    public static List<TargetComparison> comparisons(
            GymWorkoutDefinition.Slot slot, GymWorkoutDefinition.Target proposed) {
        var current = slot.target();
        var next = proposed == null ? current : proposed;
        if (current == null && next == null) return List.of();
        var rows = new ArrayList<TargetComparison>();
        row(
                rows,
                "sets",
                "Sets",
                current == null ? null : number(current.sets()),
                next == null ? null : number(next.sets()));
        if (current != null && current.minimumReps() != null
                || next != null && next.minimumReps() != null)
            row(rows, "repetitions", "Repetitions", reps(current), reps(next));
        if (current != null && current.seconds() != null || next != null && next.seconds() != null)
            row(
                    rows,
                    "duration",
                    "Time per set",
                    current == null || current.seconds() == null
                            ? null
                            : duration(current.seconds()),
                    next == null || next.seconds() == null ? null : duration(next.seconds()));
        if (current != null && current.metres() != null || next != null && next.metres() != null)
            row(
                    rows,
                    "distance",
                    "Distance per set",
                    current == null || current.metres() == null
                            ? null
                            : number(current.metres()) + " metres",
                    next == null || next.metres() == null
                            ? null
                            : number(next.metres()) + " metres");
        row(
                rows,
                "effort",
                "Effort",
                compactEffort(current == null ? null : current.effort()),
                compactEffort(next == null ? null : next.effort()));
        row(
                rows,
                "rest",
                "Rest",
                current == null || current.restSeconds() == null
                        ? null
                        : duration(current.restSeconds()),
                next == null || next.restSeconds() == null ? null : duration(next.restSeconds()));
        if (current != null && current.referenceLoadKg() != null
                || next != null && next.referenceLoadKg() != null)
            row(
                    rows,
                    "weight-reference",
                    slot.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT
                            ? "Added weight reference"
                            : "Weight reference",
                    current == null || current.referenceLoadKg() == null
                            ? null
                            : number(current.referenceLoadKg()) + " kg",
                    next == null || next.referenceLoadKg() == null
                            ? null
                            : number(next.referenceLoadKg()) + " kg");
        return List.copyOf(rows);
    }

    private static void row(
            List<TargetComparison> rows, String key, String label, String current, String next) {
        String before = current == null ? "Not set" : current;
        String after = next == null ? "Not set" : next;
        rows.add(new TargetComparison(key, label, before, after, !before.equals(after)));
    }

    private static String reps(GymWorkoutDefinition.Target target) {
        return target == null || target.minimumReps() == null
                ? null
                : range(target.minimumReps(), target.maximumReps());
    }

    public static String compactEffort(GymWorkoutDefinition.EffortRange effort) {
        if (effort == null) return null;
        return effort.scale() == GymWorkoutDefinition.EffortScale.RPE
                ? "RPE " + range(effort.minimum(), effort.maximum()) + "/10"
                : "RIR " + range(effort.minimum(), effort.maximum());
    }

    public static String duration(long seconds) {
        long minutes = seconds / 60, remainder = seconds % 60;
        String m = minutes + (minutes == 1 ? " minute" : " minutes");
        String s = remainder + (remainder == 1 ? " second" : " seconds");
        return minutes == 0 ? s : remainder == 0 ? m : m + " and " + s;
    }

    private static String effortWord(double value) {
        if (value <= 2) return "very light";
        if (value <= 4) return "light";
        if (value <= 6) return "moderate";
        if (value <= 7) return "challenging";
        if (value <= 8) return "hard";
        if (value < 10) return "very hard";
        return "maximum";
    }

    private static String range(double minimum, double maximum) {
        return number(minimum) + (minimum == maximum ? "" : "–" + number(maximum));
    }

    public static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
