package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

/** Exclusions are input policy, never mutation of already retained programme or actual evidence. */
public final class ExerciseExclusionTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    public static void main(String[] args) {
        var generator = new GymProgrammeGenerator();
        var excluded = Set.of("bench-press", "back-squat", "romanian-deadlift", "plank");
        for (var goal : GymProgramme.Goal.values())
            for (var experience : GymProgramme.Experience.values())
                for (var priority : GymProgramme.Priority.values())
                    for (int frequency : new int[] {1, 2, 3, 4, 5, 6}) {
                        var days = EnumSet.allOf(DayOfWeek.class);
                        var settings =
                                new GymProgramme.Settings(
                                        goal,
                                        priority,
                                        experience,
                                        days,
                                        frequency,
                                        Set.of("full gym"),
                                        60,
                                        GymProgramme.Effort.RPE);
                        var original =
                                generator.generate(
                                        "retained",
                                        1,
                                        NOW,
                                        "Before preference",
                                        settings,
                                        LocalDate.of(2026, 10, 28),
                                        List.of(),
                                        List.of());
                        var before = original.sessions();
                        var result =
                                generator.generate(
                                        "new",
                                        1,
                                        NOW,
                                        "Reviewed exclusions",
                                        settings,
                                        LocalDate.of(2026, 10, 28),
                                        List.of(),
                                        List.of(),
                                        excluded,
                                        excluded);
                        check(
                                result.sessions().size() == frequency * 6,
                                "Session allocation preserved");
                        for (var session : result.sessions())
                            for (var prescription : session.exercises())
                                check(
                                        !excluded.contains(prescription.exerciseId()),
                                        "Even a favourite cannot override exclusion");
                        check(
                                before.equals(original.sessions()),
                                "Historical prescriptions remain unchanged");
                    }
        var request =
                new GymNowWorkouts.Request(
                        60,
                        Set.of("full gym"),
                        GymNowWorkouts.Focus.FULL_BODY,
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.INTERMEDIATE,
                        GymProgramme.Priority.BALANCED,
                        GymProgramme.Effort.RPE);
        var quick = GymNowWorkouts.generate("quick", NOW, request, excluded, excluded);
        for (var slot : quick.definition().slots())
            check(!excluded.contains(slot.exercise().id()), "One-off selection honours exclusions");
        try {
            GymNowWorkouts.generate(
                    "none",
                    NOW,
                    request,
                    Set.of(),
                    Set.of(
                            "back-squat",
                            "romanian-deadlift",
                            "goblet-squat",
                            "deadlift",
                            "bodyweight-squat"));
            throw new AssertionError("No suitable candidate must fail explicitly");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("exclusions"), "Explain unsatisfied selection");
        }
        for (var reason : ExerciseExclusionRepository.Reason.values())
            check(
                    new ExerciseExclusionRepository.Exclusion("bench-press", reason, NOW).reason()
                            == reason,
                    "Non-medical reason retained");
        for (String id : Arrays.asList(null, "", " ", "x".repeat(201))) {
            try {
                new ExerciseExclusionRepository.Exclusion(
                        id, ExerciseExclusionRepository.Reason.PREFERENCE, NOW);
                throw new AssertionError("Invalid exclusion accepted");
            } catch (IllegalArgumentException expected) {
                checks++;
            }
        }
        System.out.println("ExerciseExclusionTestSuite: " + checks + " checks passed.");
    }

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
}
