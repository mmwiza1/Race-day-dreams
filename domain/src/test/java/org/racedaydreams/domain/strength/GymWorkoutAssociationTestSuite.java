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
        check(
                GymWorkoutAssociation.authoredProposals(
                                timed,
                                GymExecution.Origin.FREE,
                                authoredPlan(1, 0, true),
                                Set.of(),
                                NOW)
                        .isEmpty());
        authoredAssociationEvidence();
        exactAssociationAcknowledgementProtocol(reviewed);
        System.out.println("GymWorkoutAssociationTestSuite: " + checks + " checks passed.");
    }

    private static void authoredAssociationEvidence() {
        var actual = actual(false, 60);
        for (var origin : GymExecution.Origin.values()) {
            for (int days = -4; days <= 4; days++) {
                var plan = authoredPlan(1, days, true);
                var proposals =
                        GymWorkoutAssociation.authoredProposals(
                                actual, origin, plan, Set.of(), NOW);
                check(
                        proposals.size()
                                == (origin != GymExecution.Origin.PLANNED && Math.abs(days) <= 3
                                        ? 1
                                        : 0));
                if (proposals.isEmpty()) continue;
                var p = proposals.get(0);
                GymWorkoutAssociation.validateAuthoredForAcceptance(
                        p, actual, origin, plan, Set.of(), false);
                check(p.authored() && p.prescription() == null);
                check(p.authoredPrescription().equals(plan.workouts().get(0)));
                check(p.sessionId().equals("authored-session") && p.date().equals(plan.start()));
                check(
                        p.title().equals("Preserved authored workout")
                                && p.prescribedExerciseCount() == 1);
                var target = p.authoredPrescription().definition().slots().get(0).target();
                check(target.effort().scale() == GymWorkoutDefinition.EffortScale.RIR);
                check(target.effort().minimum() == 1.5 && target.effort().maximum() == 3.5);
                check(target.restSeconds() == null && target.referenceLoadKg() == 80);
                check(p.actual().exercises().get(0).sets().get(0).kilograms() == 60);
            }
        }
        var plan = authoredPlan(1, 0, false);
        var p =
                GymWorkoutAssociation.authoredProposals(
                                actual, GymExecution.Origin.FREE, plan, Set.of(), NOW)
                        .get(0);
        check(p.authoredPrescription().definition().slots().get(0).target().effort() == null);
        check(p.reviewed(actual, GymExecution.Origin.FREE, NOW).equals(p));
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredForAcceptance(
                                p,
                                actual(false, 65),
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of(),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredForAcceptance(
                                p,
                                actual,
                                GymExecution.Origin.FREE,
                                authoredPlan(2, 0, false),
                                Set.of(),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredForAcceptance(
                                p,
                                actual,
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of("authored-session"),
                                false));
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredForAcceptance(
                                p, actual, GymExecution.Origin.FREE, plan, Set.of(), true));
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredRetainedEvidence(
                                p, actual, GymExecution.Origin.PLANNED, plan));
        reject(
                () ->
                        GymWorkoutAssociation.validateRetainedEvidence(
                                p, actual, GymExecution.Origin.FREE, plan(1, 0, "bench-press")));
        var legacy =
                GymWorkoutAssociation.proposals(
                                actual,
                                GymExecution.Origin.FREE,
                                plan(1, 0, "bench-press"),
                                Set.of(),
                                NOW)
                        .get(0);
        reject(
                () ->
                        GymWorkoutAssociation.validateAuthoredRetainedEvidence(
                                legacy, actual, GymExecution.Origin.FREE, plan));
        reject(
                () ->
                        new GymWorkoutAssociation.Proposal(
                                actual,
                                GymExecution.Origin.FREE,
                                plan.id(),
                                1,
                                legacy.prescription(),
                                NOW,
                                p.authoredPrescription()));
        check(
                GymWorkoutAssociation.authoredProposals(
                                actual(true, 60), GymExecution.Origin.FREE, plan, Set.of(), NOW)
                        .isEmpty());
        check(
                GymWorkoutAssociation.authoredProposals(
                                actual,
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of("authored-session"),
                                NOW)
                        .isEmpty());
        check(
                GymWorkoutAssociation.authoredProposals(
                                actual,
                                GymExecution.Origin.FREE,
                                plan,
                                Set.of(),
                                NOW.minusSeconds(4000))
                        .isEmpty());
        check(
                GymWorkoutAssociation.authoredProposals(
                                actual, GymExecution.Origin.FREE, null, Set.of(), NOW)
                        .isEmpty());
        exactAssociationAcknowledgementProtocol(p);
    }

    private static StrengthProgramme authoredPlan(int revision, int days, boolean rir) {
        var day = LocalDate.of(2026, 10, 28).plusDays(days);
        var slots =
                List.of(
                        new GymWorkoutDefinition.Slot(
                                "bench",
                                ExerciseCatalog.definition("bench-press").exercise(),
                                new GymWorkoutDefinition.Target(
                                        3,
                                        5,
                                        8,
                                        null,
                                        null,
                                        rir
                                                ? new GymWorkoutDefinition.EffortRange(
                                                        GymWorkoutDefinition.EffortScale.RIR,
                                                        1.5,
                                                        3.5)
                                                : null,
                                        null,
                                        80.0),
                                null));
        var workout =
                new StrengthProgramme.Workout(
                        "authored-session",
                        day,
                        StrengthWorkoutBudget.workoutMinutes(slots),
                        new GymWorkoutDefinition(
                                "authored-workout",
                                1,
                                NOW.minusSeconds(86400),
                                "Preserved authored workout",
                                slots),
                        null);
        return new StrengthProgramme(
                "authored",
                revision,
                revision - 1,
                NOW.minusSeconds(86400),
                "Authored programme",
                StrengthProgramme.Origin.CUSTOM,
                null,
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(day.getDayOfWeek()),
                        1,
                        45,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class)),
                List.of(
                        new StrengthProgramme.Block(
                                "block",
                                StrengthProgramme.Phase.FOUNDATION,
                                "Foundation",
                                day,
                                1,
                                List.of(workout),
                                "Original authored prescription")),
                day,
                "Athlete chose this revision",
                List.of());
    }

    private static void exactAssociationAcknowledgementProtocol(GymWorkoutAssociation.Proposal p) {
        class Store implements GymAssociationRepository {
            GymWorkoutAssociation.Decision current;
            boolean loseAck = true;
            int writes;

            public List<GymWorkoutAssociation.Proposal> proposals(String id, Instant at) {
                return current == null
                                || current.choice() == GymWorkoutAssociation.Choice.KEEP_EXTRA
                        ? List.of(p)
                        : List.of();
            }

            public GymWorkoutAssociation.Decision current(String id) {
                return current;
            }

            public GymWorkoutAssociation.Proposal reviewCurrent(String id, Instant at) {
                return current == null ? null : p;
            }

            public GymWorkoutAssociation.Decision decide(GymWorkoutAssociation.Decision d) {
                if (!d.equals(current)) {
                    current = d;
                    writes++;
                }
                if (loseAck) {
                    loseAck = false;
                    throw new IllegalArgumentException("Lost acknowledgement");
                }
                return current;
            }
        }
        var store = new Store();
        var idCount = new int[] {0};
        var coordinator =
                new GymAssociationCoordinator(
                        store, Clock.fixed(NOW, ZoneOffset.UTC), () -> "choice-" + (++idCount[0]));
        coordinator.review(p.actual().id());
        check(store.current == null);
        check(coordinator.view().proposals().size() == 1);
        reject(() -> coordinator.choose(p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        var pending = coordinator.view().pending();
        check(pending != null);
        check(store.writes == 1);
        reject(() -> coordinator.choose(p, GymWorkoutAssociation.Choice.KEEP_EXTRA));
        reject(() -> coordinator.clear());
        reject(() -> coordinator.review(p.actual().id()));
        coordinator.retry();
        check(store.writes == 1);
        check(idCount[0] == 1);
        check(coordinator.view().pending() == null);
        check(coordinator.view().current().equals(pending));
        coordinator.choose(
                coordinator.view().currentReview(), GymWorkoutAssociation.Choice.KEEP_EXTRA);
        check(store.writes == 2);
        check(idCount[0] == 2);
        check(coordinator.view().current().choice() == GymWorkoutAssociation.Choice.KEEP_EXTRA);
        coordinator.clear();
        check(coordinator.view().actualId() == null);
        coordinator.review(p.actual().id());
        store.loseAck = true;
        reject(() -> coordinator.choose(p, GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION));
        coordinator.reloadSaved();
        check(coordinator.view().pending() == null);
        check(
                coordinator.view().current().choice()
                        == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION);
        check(store.writes == 3);
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
