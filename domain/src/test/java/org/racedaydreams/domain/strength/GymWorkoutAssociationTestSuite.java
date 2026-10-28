package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

public final class GymWorkoutAssociationTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    public static void main(String[] args) {
        var actual = actual(false, 60);
        for (var origin : GymExecution.Origin.values()) {
            for (int days = -4; days <= 4; days++) {
                var plan = plan(1, days, "bench-press");
                var proposals =
                        GymWorkoutAssociation.proposals(actual, origin, plan, Set.of(), NOW);
                check(
                        proposals.size()
                                == (origin != GymExecution.Origin.PLANNED && Math.abs(days) <= 3
                                        ? 1
                                        : 0));
                if (!proposals.isEmpty()) {
                    var proposal = proposals.get(0);
                    GymWorkoutAssociation.validateForAcceptance(
                            proposal, actual, origin, plan, Set.of(), false);
                    check(
                            proposal.actual() == actual
                                    && proposal.prescription().equals(plan.sessions().get(0)));
                    var extra =
                            new GymWorkoutAssociation.Decision(
                                    "keep", proposal, GymWorkoutAssociation.Choice.KEEP_EXTRA, NOW);
                    check(extra.proposal().origin() == origin);
                    var count =
                            new GymWorkoutAssociation.Decision(
                                    "count",
                                    proposal,
                                    GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION,
                                    NOW);
                    check(
                            count.proposal().actual().exercises().get(0).sets().get(0).kilograms()
                                    == 60);
                }
            }
        }
        var plan = plan(1, 0, "bench-press");
        var reviewed =
                GymWorkoutAssociation.proposals(
                                actual, GymExecution.Origin.FREE, plan, Set.of(), NOW)
                        .get(0);
        reject(
                () ->
                        GymWorkoutAssociation.validateForAcceptance(
                                reviewed,
                                actual(false, 65),
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of(),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateForAcceptance(
                                reviewed,
                                actual,
                                GymExecution.Origin.FREE,
                                plan(2, 0, "bench-press"),
                                Set.of(),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateForAcceptance(
                                reviewed,
                                actual,
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of("planned"),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateForAcceptance(
                                reviewed, actual, GymExecution.Origin.FREE, plan, Set.of(), true));
        reject(
                () ->
                        new GymWorkoutAssociation.Decision(
                                "bad",
                                reviewed,
                                GymWorkoutAssociation.Choice.KEEP_EXTRA,
                                NOW.minusSeconds(1)));
        check(
                GymWorkoutAssociation.proposals(
                                actual(true, 60), GymExecution.Origin.FREE, plan, Set.of(), NOW)
                        .isEmpty());
        check(
                GymWorkoutAssociation.proposals(
                                actual, GymExecution.Origin.FREE, plan, Set.of("planned"), NOW)
                        .isEmpty());
        check(
                GymWorkoutAssociation.proposals(
                                actual,
                                GymExecution.Origin.FREE,
                                plan(1, 0, "overhead-press"),
                                Set.of(),
                                NOW)
                        .isEmpty());
        check(
                GymWorkoutAssociation.proposals(
                                actual,
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of(),
                                NOW.minusSeconds(4000))
                        .isEmpty());
        check(
                GymWorkoutAssociation.proposals(
                                actual, GymExecution.Origin.FREE, null, Set.of(), NOW)
                        .isEmpty());
        check(plan.sessions().get(0).exercises().get(0).referenceLoadKg() == 80);
        var eastern =
                new StrengthSession(
                        "eastern",
                        actual.startedAt(),
                        ZoneOffset.ofHours(14),
                        actual.duration(),
                        null,
                        "",
                        actual.exercises());
        check(
                GymWorkoutAssociation.proposals(
                                        eastern,
                                        GymExecution.Origin.FREE,
                                        plan(1, 4, "bench-press"),
                                        Set.of(),
                                        NOW)
                                .size()
                        == 1);
        var western =
                new StrengthSession(
                        "western",
                        actual.startedAt(),
                        ZoneOffset.ofHours(-12),
                        actual.duration(),
                        null,
                        "",
                        actual.exercises());
        check(
                GymWorkoutAssociation.proposals(
                                western,
                                GymExecution.Origin.FREE,
                                plan(1, 3, "bench-press"),
                                Set.of(),
                                NOW)
                        .isEmpty());
        reject(
                () ->
                        GymWorkoutAssociation.validateForAcceptance(
                                reviewed,
                                actual,
                                GymExecution.Origin.PLANNED,
                                plan,
                                Set.of(),
                                false));
        var historicalTimedExercise =
                new StrengthExercise(
                        "bench-press",
                        "Historical timed task",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.DURATION);
        var timed =
                new StrengthSession(
                        "timed",
                        actual.startedAt(),
                        actual.offset(),
                        actual.duration(),
                        null,
                        "",
                        List.of(
                                new StrengthSession.ExerciseSets(
                                        historicalTimedExercise,
                                        List.of(
                                                new StrengthSession.SetEntry(
                                                        "timed-set",
                                                        0,
                                                        0,
                                                        null,
                                                        false,
                                                        30L,
                                                        null)))));
        check(
                GymWorkoutAssociation.proposals(
                                timed, GymExecution.Origin.FREE, plan, Set.of(), NOW)
                        .isEmpty());
        System.out.println("GymWorkoutAssociationTestSuite: " + checks + " checks passed.");
    }

    private static StrengthSession actual(boolean warm, double load) {
        return new StrengthSession(
                "actual",
                NOW.minusSeconds(3600),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                List.of(
                        new StrengthSession.ExerciseSets(
                                ExerciseCatalog.definition("bench-press").exercise(),
                                List.of(new StrengthSession.SetEntry("set", 6, load, 7.0, warm)))));
    }

    private static GymProgramme plan(int revision, int days, String exercise) {
        var day = LocalDate.of(2026, 10, 28).plusDays(days);
        return new GymProgramme(
                "programme",
                revision,
                NOW.minusSeconds(86400),
                "Original plan",
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(day.getDayOfWeek()),
                        1,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Effort.RPE),
                day,
                day.plusDays(41),
                List.of(
                        new GymProgramme.Session(
                                "planned",
                                day,
                                "Strength",
                                List.of(
                                        new GymProgramme.Prescription(
                                                exercise,
                                                3,
                                                5,
                                                8,
                                                null,
                                                null,
                                                6,
                                                8,
                                                120,
                                                "Strength",
                                                80.0)),
                                "Original prescription")),
                List.of());
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Association contract failed");
    }

    private static void reject(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Unsafe association accepted");
    }
}
