package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.strength.StrengthExercise;

public final class AthleteLanguageTestSuite {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Athlete language check " + checks);
    }

    public static void main(String[] args) {
        for (var profile : StrengthExercise.MeasurementProfile.values()) {
            var exercise =
                    new StrengthExercise(
                            "custom",
                            "Custom",
                            StrengthExercise.Region.FULL,
                            profile.usesLoad()
                                    ? StrengthExercise.Mode.WEIGHTED
                                    : StrengthExercise.Mode.BODYWEIGHT,
                            false,
                            profile);
            var before = exercise.toString();
            String text = AthleteLanguage.measurement(exercise);
            check(text.startsWith("Record "));
            check(profile.usesLoad() == text.contains("weight"));
            check(profile.usesReps() == text.contains("repetitions"));
            check(profile.usesDuration() == text.contains("how long"));
            check(profile.usesDistance() == text.contains("how far"));
            check(!text.contains("kg") && !text.contains("RPE"));
            check(before.equals(exercise.toString()));
        }
        var added =
                new StrengthExercise(
                        "pull",
                        "Pull-up",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        check(AthleteLanguage.measurement(added).contains("extra weight you add"));
        check(
                AthleteLanguage.recordLabel("Estimated 1RM")
                        .equals("Estimated maximum for one repetition"));
        check(AthleteLanguage.recordLabel("Load × 5").equals("Best weight for 5 repetitions"));
        check(AthleteLanguage.recordLabel("Load × 1").equals("Best weight for 1 repetition"));
        check(AthleteLanguage.recordLabel("Bodyweight reps").equals("Most bodyweight repetitions"));
        check(AthleteLanguage.recordLabel("Custom category").equals("Custom category"));
        check(StrengthPrescriptionText.effortDescription(1).equals("very light"));
        check(StrengthPrescriptionText.effortDescription(5).equals("moderate"));
        check(StrengthPrescriptionText.effortDescription(7).equals("challenging"));
        check(StrengthPrescriptionText.effortDescription(10).equals("maximum"));
        check(StrengthPrescriptionText.effortDescription(Double.NaN).contains("no effort target"));
        for (var category : WorkoutCategory.values()) {
            String text = AthleteLanguage.runningPurpose(category);
            check(!text.isBlank());
            check(!text.contains("bpm") && !text.contains("min/km"));
        }
        check(
                AthleteLanguage.runningPurpose(WorkoutCategory.INTERVALS)
                        .contains("recovery sections"));
        check(AthleteLanguage.BACKUP.contains("does not include detailed GPS"));
        check(AthleteLanguage.BACKUP.contains("Complete backup"));
        check(AthleteLanguage.WORKING_SETS.contains("excluding warm-up"));
        System.out.println("AthleteLanguageTestSuite: " + checks + " checks passed.");
    }
}
