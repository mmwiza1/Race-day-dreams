package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;

public final class StrengthPrescriptionTextTestSuite {
    static int checks;

    static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Instruction check " + checks);
    }

    static GymWorkoutDefinition.Slot slot(
            StrengthExercise.MeasurementProfile profile, GymWorkoutDefinition.EffortRange effort) {
        var exercise =
                new StrengthExercise(
                        "task",
                        "Task",
                        StrengthExercise.Region.UPPER,
                        profile.usesLoad()
                                ? StrengthExercise.Mode.WEIGHTED
                                : StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        profile);
        return new GymWorkoutDefinition.Slot(
                "slot",
                exercise,
                new GymWorkoutDefinition.Target(
                        2,
                        profile.usesReps() ? 4 : null,
                        profile.usesReps() ? 6 : null,
                        profile.usesDuration() ? 45L : null,
                        profile.usesDistance() ? 40.0 : null,
                        effort,
                        180,
                        profile.usesLoad() ? 80.0 : null),
                null);
    }

    public static void main(String[] args) {
        var rpe = new GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 5, 7);
        var rir = new GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RIR, 2, 3);
        for (var profile : StrengthExercise.MeasurementProfile.values()) {
            var slot = slot(profile, rpe);
            var old = slot.target();
            String text = String.join(" ", StrengthPrescriptionText.instructions(slot));
            check(text.contains("2 sets"));
            check(text.contains("Rest for 3 minutes"));
            check(text.contains("moderate to challenging perceived effort"));
            check(!text.contains("RPE") && !text.contains("RIR"));
            check(profile.usesReps() == text.contains("4–6 repetitions"));
            check(profile.usesDuration() == text.contains("45 seconds"));
            check(profile.usesDistance() == text.contains("40 metres"));
            check(slot.target().equals(old));
            var missingEffort = slot(profile, null);
            check(
                    StrengthPrescriptionText.instructions(missingEffort).stream()
                            .noneMatch(s -> s.contains("effort") || s.contains("failure")));
            String weight = String.join(" ", StrengthPrescriptionText.weight(slot, null));
            check(profile.usesLoad() == weight.contains("80 kg"));
            check(!weight.contains("training history"));
        }
        var weighted = slot(StrengthExercise.MeasurementProfile.LOAD_REPS, rpe);
        var proposed = new GymWorkoutDefinition.Target(2, 4, 6, null, null, rpe, 180, 82.0);
        String weight = String.join(" ", StrengthPrescriptionText.weight(weighted, proposed));
        check(weight.contains("current weight reference is 80 kg"));
        check(weight.contains("suggested new weight reference is 82 kg"));
        check(weight.contains("2 kg more"));
        check(weight.contains("not a weight you must lift or a recorded result"));
        check(!weight.contains("Lift 82") && !weight.contains("completed 82"));
        var decimalSlot =
                new GymWorkoutDefinition.Slot(
                        "decimal",
                        weighted.exercise(),
                        new GymWorkoutDefinition.Target(2, 4, 6, null, null, rpe, 180, 80.1),
                        null);
        var decimalProposal = new GymWorkoutDefinition.Target(2, 4, 6, null, null, rpe, 180, 82.1);
        check(
                String.join(" ", StrengthPrescriptionText.weight(decimalSlot, decimalProposal))
                        .contains("2 kg more"));
        var noCurrent =
                new GymWorkoutDefinition.Slot(
                        "slot",
                        weighted.exercise(),
                        weighted.target().withoutLoadReference(),
                        null);
        weight = String.join(" ", StrengthPrescriptionText.weight(noCurrent, proposed));
        check(weight.contains("current target has no weight reference"));
        check(!weight.contains("current weight reference is 82"));
        check(
                StrengthPrescriptionText.effortDetails(rpe)
                        .contains("RPE (rating of perceived effort): 5–7 out of 10"));
        check(!StrengthPrescriptionText.effortDetails(rpe).contains("RIR"));
        check(
                StrengthPrescriptionText.effortDetails(rir)
                        .contains("RIR (repetitions in reserve): 2–3"));
        check(!StrengthPrescriptionText.effortDetails(rir).contains("RPE"));
        check(
                String.join(
                                " ",
                                StrengthPrescriptionText.instructions(
                                        slot(StrengthExercise.MeasurementProfile.REPS_ONLY, rir)))
                        .contains("2–3 repetitions left before failure"));
        check(
                StrengthPrescriptionText.effortDetails(null)
                        .equals("No effort target is specified."));
        check(StrengthPrescriptionText.duration(90).equals("1 minute and 30 seconds"));
        check(StrengthPrescriptionText.duration(61).equals("1 minute and 1 second"));
        check(StrengthPrescriptionText.duration(0).equals("0 seconds"));
        var open = new GymWorkoutDefinition.Slot("open", weighted.exercise(), null, null);
        check(
                StrengthPrescriptionText.instructions(open)
                        .get(0)
                        .contains("No set or effort target is prescribed"));
        var fixed =
                new GymWorkoutDefinition.Slot(
                        "fixed",
                        weighted.exercise(),
                        new GymWorkoutDefinition.Target(1, 8, 8, null, null, null, 0, null),
                        null);
        check(
                StrengthPrescriptionText.instructions(fixed)
                        .get(0)
                        .equals("Complete 1 set of 8 repetitions."));
        check(
                StrengthPrescriptionText.instructions(fixed)
                        .get(1)
                        .equals("No rest is prescribed between sets."));
        var missing =
                new GymWorkoutDefinition.Slot(
                        "missing",
                        weighted.exercise(),
                        new GymWorkoutDefinition.Target(3, 8, 8, null, null, null, null, null),
                        null);
        check(StrengthPrescriptionText.instructions(missing).size() == 1);
        var comparison = StrengthPrescriptionText.comparisons(weighted, proposed);
        var differences =
                comparison.stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(differences.size() == 1);
        check(differences.get(0).key().equals("weight-reference"));
        check(differences.get(0).currentValue().equals("80 kg"));
        check(differences.get(0).proposedValue().equals("82 kg"));
        check(
                comparison.stream()
                        .filter(r -> !r.changed())
                        .map(StrengthPrescriptionText.TargetComparison::key)
                        .toList()
                        .equals(java.util.List.of("sets", "repetitions", "effort", "rest")));
        check(
                comparison.stream()
                        .filter(r -> r.key().equals("effort"))
                        .findFirst()
                        .orElseThrow()
                        .currentValue()
                        .equals("RPE 5–7/10"));
        check(
                StrengthPrescriptionText.comparisons(weighted, weighted.target()).stream()
                        .noneMatch(StrengthPrescriptionText.TargetComparison::changed));
        check(
                StrengthPrescriptionText.comparisons(weighted, null).stream()
                        .noneMatch(StrengthPrescriptionText.TargetComparison::changed));
        var missingReference =
                StrengthPrescriptionText.comparisons(noCurrent, proposed).stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .findFirst()
                        .orElseThrow();
        check(missingReference.currentValue().equals("Not set"));
        check(missingReference.proposedValue().equals("82 kg"));
        check(StrengthPrescriptionText.comparisons(open, null).isEmpty());
        var zeroReference =
                new GymWorkoutDefinition.Slot(
                        "zero",
                        weighted.exercise(),
                        new GymWorkoutDefinition.Target(2, 4, 6, null, null, rpe, 180, -0.0),
                        null);
        check(
                StrengthPrescriptionText.comparisons(
                                zeroReference,
                                new GymWorkoutDefinition.Target(2, 4, 6, null, null, rpe, 180, 0.0))
                        .stream()
                        .noneMatch(StrengthPrescriptionText.TargetComparison::changed));
        var timeSlot = slot(StrengthExercise.MeasurementProfile.DURATION, rpe);
        var timeRows =
                StrengthPrescriptionText.comparisons(
                        timeSlot,
                        new GymWorkoutDefinition.Target(2, null, null, 50L, null, rpe, 180, null));
        var timeChange =
                timeRows.stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(timeChange.size() == 1 && timeChange.get(0).key().equals("duration"));
        check(
                timeChange.get(0).currentValue().equals("45 seconds")
                        && timeChange.get(0).proposedValue().equals("50 seconds"));
        var distanceSlot = slot(StrengthExercise.MeasurementProfile.DISTANCE, rpe);
        var distanceChange =
                StrengthPrescriptionText.comparisons(
                                distanceSlot,
                                new GymWorkoutDefinition.Target(
                                        2, null, null, null, 45.0, rpe, 180, null))
                        .stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(distanceChange.size() == 1 && distanceChange.get(0).key().equals("distance"));
        check(
                distanceChange.get(0).currentValue().equals("40 metres")
                        && distanceChange.get(0).proposedValue().equals("45 metres"));
        var effortChange =
                StrengthPrescriptionText.comparisons(
                                weighted,
                                new GymWorkoutDefinition.Target(
                                        2, 4, 6, null, null, rir, 180, 80.0))
                        .stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(effortChange.size() == 1 && effortChange.get(0).key().equals("effort"));
        check(
                effortChange.get(0).currentValue().equals("RPE 5–7/10")
                        && effortChange.get(0).proposedValue().equals("RIR 2–3"));
        var restChange =
                StrengthPrescriptionText.comparisons(
                                missing,
                                new GymWorkoutDefinition.Target(3, 8, 8, null, null, null, 0, null))
                        .stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(restChange.size() == 1 && restChange.get(0).key().equals("rest"));
        check(
                restChange.get(0).currentValue().equals("Not set")
                        && restChange.get(0).proposedValue().equals("0 seconds"));
        var repChange =
                StrengthPrescriptionText.comparisons(
                                weighted,
                                new GymWorkoutDefinition.Target(
                                        2, 4, 7, null, null, rpe, 180, 80.0))
                        .stream()
                        .filter(StrengthPrescriptionText.TargetComparison::changed)
                        .toList();
        check(repChange.size() == 1 && repChange.get(0).key().equals("repetitions"));
        check(
                repChange.get(0).currentValue().equals("4–6")
                        && repChange.get(0).proposedValue().equals("4–7"));
        System.out.println("StrengthPrescriptionTextTestSuite: " + checks + " checks passed.");
    }
}
