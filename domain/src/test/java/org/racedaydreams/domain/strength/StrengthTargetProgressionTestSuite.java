package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

public final class StrengthTargetProgressionTestSuite {
    static int checks;
    static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    public static void main(String[] args) {
        for (String id :
                List.of(
                        "bench-press",
                        "push-up",
                        "weighted-pull-up",
                        "plank",
                        "weighted-plank",
                        "farmers-carry")) {
            var exercise = ExerciseCatalog.definition(id).exercise();
            var target = target(exercise);
            var slot = new GymWorkoutDefinition.Slot("slot", exercise, target, null);
            double load = exercise.measurement().usesLoad() ? 40 : 0;
            var recent =
                    session(
                            "recent",
                            exercise,
                            load,
                            NOW.minus(Duration.ofDays(1)),
                            7.0,
                            false,
                            true);
            var older =
                    session(
                            "older",
                            exercise,
                            load,
                            NOW.minus(Duration.ofDays(3)),
                            7.0,
                            false,
                            true);
            var history = List.of(recent, older);
            var before = new ArrayList<>(history);
            var advice = StrengthTargetProgression.review(slot, history, NOW);
            check(
                    advice.action() == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                    id + " supported task progression");
            check(
                    advice.supportingSessionIds().equals(List.of("recent", "older")),
                    "exact evidence provenance");
            check(before.equals(history), "actual history unchanged");
            if (exercise.measurement().usesDuration()) {
                check(advice.proposedTarget().seconds() == 35, "small time option");
                check(
                        Objects.equals(
                                advice.proposedTarget().referenceLoadKg(), load == 0 ? null : load),
                        "time progression retains proven loading");
            } else if (exercise.measurement().usesDistance()) {
                check(advice.proposedTarget().metres() == 25.0, "small distance option");
                check(
                        advice.proposedTarget().referenceLoadKg() == load,
                        "carry load not increased simultaneously");
            } else if (exercise.measurement().usesLoad())
                check(
                        advice.proposedTarget().referenceLoadKg() == 41.0,
                        "existing 2.5 percent rule retained");
            else check(advice.proposedTarget().maximumReps() == 13, "bodyweight repetition option");
            check(
                    StrengthTargetProgression.review(slot, List.of(recent), NOW).action()
                            == StrengthProgressionAdvisor.Action.HOLD,
                    "one session holds");
            check(
                    StrengthTargetProgression.review(slot, List.of(recent, recent), NOW).action()
                            == StrengthProgressionAdvisor.Action.HOLD,
                    "duplicate identity not two sessions");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    recent,
                                                    session(
                                                            "stale",
                                                            exercise,
                                                            load,
                                                            NOW.minus(Duration.ofDays(29)),
                                                            7.0,
                                                            false,
                                                            true)),
                                            NOW)
                                    .proposedTarget()
                            == null,
                    "each supporting session must be recent");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    recent,
                                                    session(
                                                            "future",
                                                            exercise,
                                                            load,
                                                            NOW.plusSeconds(1),
                                                            7.0,
                                                            false,
                                                            true)),
                                            NOW)
                                    .proposedTarget()
                            == null,
                    "future evidence ignored");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    recent,
                                                    session(
                                                            "unknown",
                                                            exercise,
                                                            load,
                                                            NOW.minusSeconds(5),
                                                            null,
                                                            false,
                                                            true)),
                                            NOW)
                                    .proposedTarget()
                            == null,
                    "unknown effort not easy");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    recent,
                                                    session(
                                                            "hard",
                                                            exercise,
                                                            load,
                                                            NOW.minusSeconds(5),
                                                            9.0,
                                                            false,
                                                            true)),
                                            NOW)
                                    .action()
                            == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                    "too hard prompts optional simplification");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    recent,
                                                    session(
                                                            "short",
                                                            exercise,
                                                            load,
                                                            NOW.minusSeconds(5),
                                                            7.0,
                                                            false,
                                                            false)),
                                            NOW)
                                    .action()
                            == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                    "missed task is not success");
            check(
                    StrengthTargetProgression.review(
                                            slot,
                                            List.of(
                                                    session(
                                                            "warm",
                                                            exercise,
                                                            load,
                                                            NOW.minusSeconds(5),
                                                            7.0,
                                                            true,
                                                            true)),
                                            NOW)
                                    .action()
                            == StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                    "warm-ups excluded");
            var noEffort =
                    new GymWorkoutDefinition.Target(
                            target.sets(),
                            target.minimumReps(),
                            target.maximumReps(),
                            target.seconds(),
                            target.metres(),
                            null,
                            null,
                            null);
            check(
                    StrengthTargetProgression.review(
                                            new GymWorkoutDefinition.Slot(
                                                    "no-effort", exercise, noEffort, null),
                                            history,
                                            NOW)
                                    .proposedTarget()
                            == null,
                    "no intended effort does not progress");
            if (exercise.measurement().usesLoad())
                check(
                        StrengthTargetProgression.review(
                                                slot,
                                                List.of(
                                                        recent,
                                                        session(
                                                                "different-load",
                                                                exercise,
                                                                load - 5,
                                                                NOW.minusSeconds(5),
                                                                7.0,
                                                                false,
                                                                true)),
                                                NOW)
                                        .proposedTarget()
                                == null,
                        "same load required");
        }
        var custom =
                new StrengthExercise(
                        "custom-distance",
                        "My distance task",
                        StrengthExercise.Region.FULL,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.DISTANCE);
        var slot = new GymWorkoutDefinition.Slot("distance", custom, target(custom), null);
        check(
                StrengthTargetProgression.review(
                                        slot,
                                        List.of(
                                                session(
                                                        "a",
                                                        custom,
                                                        0,
                                                        NOW.minusSeconds(20),
                                                        7.0,
                                                        false,
                                                        true),
                                                session(
                                                        "b",
                                                        custom,
                                                        0,
                                                        NOW.minusSeconds(40),
                                                        7.0,
                                                        false,
                                                        true)),
                                        NOW)
                                .proposedTarget()
                        == null,
                "custom distance logs do not invent supported progression");
        var push = ExerciseCatalog.definition("push-up").exercise();
        var rir =
                new GymWorkoutDefinition.Target(
                        2,
                        8,
                        12,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, 2, 4),
                        90,
                        null);
        var rirHistory = new ArrayList<StrengthSession>();
        for (int n = 1; n <= 2; n++) {
            var sets =
                    List.of(
                            new StrengthSession.SetEntry(
                                    "rir-a" + n, 12, 0, null, false, null, null, 2.0),
                            new StrengthSession.SetEntry(
                                    "rir-b" + n, 12, 0, null, false, null, null, 2.0));
            rirHistory.add(
                    new StrengthSession(
                            "rir" + n,
                            NOW.minus(Duration.ofDays(n)),
                            ZoneOffset.UTC,
                            Duration.ofMinutes(30),
                            null,
                            null,
                            List.of(new StrengthSession.ExerciseSets(push, sets))));
        }
        check(
                StrengthTargetProgression.review(
                                        new GymWorkoutDefinition.Slot("rir", push, rir, null),
                                        rirHistory,
                                        NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "recorded RIR supports conservative rep advice");
        var impossibleBench =
                new StrengthExercise(
                        "bench-press",
                        "Wrong retained task",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_DISTANCE);
        check(
                StrengthTargetProgression.review(
                                        new GymWorkoutDefinition.Slot(
                                                "wrong",
                                                impossibleBench,
                                                target(impossibleBench),
                                                null),
                                        List.of(
                                                session(
                                                        "x",
                                                        impossibleBench,
                                                        20,
                                                        NOW.minusSeconds(1),
                                                        7.0,
                                                        false,
                                                        true),
                                                session(
                                                        "y",
                                                        impossibleBench,
                                                        20,
                                                        NOW.minusSeconds(2),
                                                        7.0,
                                                        false,
                                                        true)),
                                        NOW)
                                .proposedTarget()
                        == null,
                "a familiar ID does not validate a changed task");
        authoredSourcesUseExistingExecution();
        System.out.println("StrengthTargetProgressionTestSuite: " + checks + " checks passed.");
    }

    static void authoredSourcesUseExistingExecution() {
        var settings =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        EnumSet.allOf(DayOfWeek.class),
                        2,
                        60,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class));
        var p =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        "authored",
                                        NOW,
                                        "My programme",
                                        settings,
                                        LocalDate.of(2026, 10, 27),
                                        8,
                                        null,
                                        org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH,
                                        Set.of(),
                                        null,
                                        Set.of(),
                                        Set.of(),
                                        false));
        for (var workout : p.workouts()) {
            var prepared = GymWorkoutAdapter.fromAuthoredProgramme(p, workout.id());
            check(
                    GymWorkoutSourceValidator.authoredProgramme(
                                    prepared.definition(), prepared.source(), p)
                            .equals(workout),
                    "exact authored source validation");
            var engine =
                    GymExecutionEngine.start(
                            "actual-" + workout.id(),
                            GymExecution.Origin.PLANNED,
                            prepared.definition(),
                            prepared.source(),
                            ZoneOffset.UTC,
                            new GymExecution.Tick("boot", 10, NOW));
            check(engine.snapshot().actualSets().isEmpty(), "intentions never become actuals");
            check(
                    engine.snapshot().prescription().slots().equals(workout.definition().slots()),
                    "one execution path with frozen authored slots");
            var wrong =
                    new GymExecution.SourceReference(
                            GymExecution.SourceKind.PROGRAMME, p.id(), 2, workout.id());
            reject(
                    () ->
                            GymWorkoutSourceValidator.authoredProgramme(
                                    prepared.definition(), wrong, p));
        }
    }

    static GymWorkoutDefinition.Target target(StrengthExercise e) {
        return new GymWorkoutDefinition.Target(
                2,
                e.measurement().usesReps() ? 8 : null,
                e.measurement().usesReps() ? 12 : null,
                e.measurement().usesDuration() ? 30L : null,
                e.measurement().usesDistance() ? 20.0 : null,
                new GymWorkoutDefinition.EffortRange(GymWorkoutDefinition.EffortScale.RPE, 6, 8),
                90,
                null);
    }

    static StrengthSession session(
            String id,
            StrengthExercise e,
            double load,
            Instant at,
            Double effort,
            boolean warmUp,
            boolean success) {
        var sets = new ArrayList<StrengthSession.SetEntry>();
        for (int n = 0; n < 2; n++)
            sets.add(
                    new StrengthSession.SetEntry(
                            id + n,
                            e.measurement().usesReps() ? success ? 12 : 5 : 0,
                            load,
                            effort,
                            warmUp,
                            e.measurement().usesDuration() ? success ? 30L : 10L : null,
                            e.measurement().usesDistance() ? success ? 20.0 : 10.0 : null));
        return new StrengthSession(
                id,
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                null,
                List.of(new StrengthSession.ExerciseSets(e, sets)));
    }

    static void check(boolean b, String reason) {
        checks++;
        if (!b) throw new AssertionError(reason);
    }

    static void reject(Runnable r) {
        checks++;
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("Invalid source accepted");
    }
}
