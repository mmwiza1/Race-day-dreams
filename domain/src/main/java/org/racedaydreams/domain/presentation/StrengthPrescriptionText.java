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
        if (next != null) {
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
                "A reference is an optional planning guide, not a weight you must lift or a"
                        + " recorded result. Choose a manageable weight for the intended task and"
                        + " effort; record what you actually do.");
        return List.copyOf(lines);
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
