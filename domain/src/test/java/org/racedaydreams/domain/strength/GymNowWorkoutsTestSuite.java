package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

/** One-off intentions use the real planner and never become actual or programme evidence. */
public final class GymNowWorkoutsTestSuite {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static int checks;

    public static void main(String[] args) {
        explicitInputsAndImmutableChoices();
        deterministicOneOffAcrossSupportedChoices();
        spontaneousOriginsHaveNoProgrammeAssociation();
        equipmentAndFocusRemainHonest();
        favouritesPreserveTheRequiredRole();
        effortMeasurementsRemainCompatible();
        insufficientFullBodyBudgetIsAnExplicitChoice();
        System.out.println("GymNowWorkoutsTestSuite: " + checks + " checks passed.");
    }

    private static void explicitInputsAndImmutableChoices() {
        reject(
                () -> request(30, Set.of(), GymNowWorkouts.Focus.FULL_BODY),
                "Unknown equipment is not silently treated as a full gym");
        reject(
                () -> request(20, Set.of("bodyweight"), GymNowWorkouts.Focus.FULL_BODY),
                "Unsupported duration needs a supported choice");
        reject(
                () -> request(31, Set.of("bodyweight"), GymNowWorkouts.Focus.FULL_BODY),
                "Duration is not silently rounded");
        reject(
                () -> request(45, Set.of("unknown-machine"), GymNowWorkouts.Focus.UPPER),
                "Unknown equipment must be reviewed");
        reject(() -> request(45, Set.of("bodyweight"), null), "A focus is explicit");
        reject(
                () ->
                        GymNowWorkouts.generate(
                                "",
                                NOW,
                                request(45, Set.of("bodyweight"), GymNowWorkouts.Focus.FULL_BODY)),
                "Workout identity must be stable");
        var equipment = new HashSet<>(Set.of("bodyweight"));
        var request = request(45, equipment, GymNowWorkouts.Focus.FULL_BODY);
        equipment.add("full gym");
        equal(
                Set.of("bodyweight"),
                request.equipment(),
                "Caller mutation cannot change chosen capabilities");
        unsupported(() -> request.equipment().add("full gym"), "Equipment choices are immutable");
        var result = GymNowWorkouts.generate("one-off", NOW, request);
        unsupported(
                () -> result.definition().slots().clear(), "Preserved intentions are immutable");
        unsupported(
                () -> result.limitations().add("false evidence"),
                "Explanation context is immutable");
        equal(
                "one-off",
                result.definition().id(),
                "Private reference IDs never become owned workout identity");
        equal(1, result.definition().revision(), "The first owned definition is revision one");
        equal(
                NOW,
                result.definition().createdAt(),
                "Timestamp is supplied, not read from an ambient clock");
    }

    private static void deterministicOneOffAcrossSupportedChoices() {
        int accepted = 0, constrained = 0;
        for (var goal : GymProgramme.Goal.values())
            for (var experience : GymProgramme.Experience.values())
                for (var priority : GymProgramme.Priority.values())
                    for (int minutes : new int[] {30, 45, 60, 75})
                        for (var focus : GymNowWorkouts.Focus.values())
                            for (var equipment : Set.of(Set.of("full gym"), Set.of("bodyweight"))) {
                                var request =
                                        new GymNowWorkouts.Request(
                                                minutes,
                                                equipment,
                                                focus,
                                                goal,
                                                experience,
                                                priority,
                                                GymProgramme.Effort.RPE);
                                GymNowWorkouts.Suggestion result;
                                try {
                                    result = GymNowWorkouts.generate("matrix", NOW, request);
                                } catch (IllegalArgumentException constraint) {
                                    ok(
                                            focus == GymNowWorkouts.Focus.FULL_BODY
                                                    && minutes == 30
                                                    && experience
                                                            != GymProgramme.Experience.BEGINNER
                                                    && priority
                                                            != GymProgramme.Priority.RUNNING_FIRST
                                                    && (goal == GymProgramme.Goal.STRENGTH
                                                            || goal == GymProgramme.Goal.POWER),
                                            "Only an acknowledged incomplete full-body time budget"
                                                    + " can reject these valid capabilities");
                                    ok(
                                            constraint
                                                    .getMessage()
                                                    .contains("both upper- and lower-body"),
                                            "Constraint explains the actual missing coverage");
                                    constrained++;
                                    continue;
                                }
                                accepted++;
                                var definition = result.definition();
                                equal(
                                        result,
                                        GymNowWorkouts.generate("matrix", NOW, request),
                                        "Same explicit choices produce the same one-off"
                                                + " intentions");
                                equal(
                                        focus.title(),
                                        definition.title(),
                                        "The selected focus remains visible");
                                ok(
                                        !result.explanation().contains("six-week"),
                                        "A one-off is not described as programme enrolment");
                                ok(
                                        result.explanation()
                                                .contains("does not change a programme"),
                                        "Programme independence is explicit");
                                var ids = new HashSet<String>();
                                boolean lower = false, upper = false, pull = false;
                                for (var slot : definition.slots()) {
                                    ok(
                                            ids.add(slot.exercise().id()),
                                            "No unintended repeated exercise in one-off"
                                                    + " prescription");
                                    ok(
                                            GymProgrammeGenerator.available(
                                                    slot.exercise().id(), equipment),
                                            "No unavailable equipment is prescribed");
                                    equal(
                                            null,
                                            slot.target().referenceLoadKg(),
                                            "No arbitrary starting load is inferred");
                                    var metadata =
                                            ExerciseMetadataCatalog.require(slot.exercise().id());
                                    lower |=
                                            slot.exercise().region()
                                                    == StrengthExercise.Region.LOWER;
                                    upper |= upper(metadata);
                                    pull |= pulling(metadata);
                                    if (experience == GymProgramme.Experience.BEGINNER)
                                        ok(
                                                metadata.suitability()
                                                        != ExerciseMetadata.Suitability.TECHNICAL,
                                                "Beginner one-offs do not introduce advanced"
                                                        + " technical lifts");
                                }
                                if (focus == GymNowWorkouts.Focus.FULL_BODY)
                                    ok(
                                            lower && upper,
                                            "Full body really includes upper and lower work");
                                if (focus == GymNowWorkouts.Focus.UPPER)
                                    ok(
                                            upper && !lower,
                                            "Upper focus has meaningful upper work without lower"
                                                    + " compounds");
                                if (focus == GymNowWorkouts.Focus.LOWER)
                                    ok(
                                            lower && !upper,
                                            "Lower focus has meaningful lower work without upper"
                                                    + " compounds");
                                boolean warned =
                                        result.limitations().stream()
                                                .anyMatch(s -> s.contains("No upper-body pulling"));
                                equal(
                                        focus != GymNowWorkouts.Focus.LOWER && !pull,
                                        warned,
                                        "Missing pulling is explained only when relevant to this"
                                                + " focus");
                            }
        ok(accepted > 1000, "The real generator is exercised across broad one-off choices");
        int expected =
                GymProgramme.Goal.values().length
                        * GymProgramme.Experience.values().length
                        * GymProgramme.Priority.values().length
                        * 4
                        * GymNowWorkouts.Focus.values().length
                        * 2;
        equal(
                expected,
                accepted + constrained,
                "Every supported request in the matrix is either meaningful or explicitly"
                        + " constrained");
    }

    private static void spontaneousOriginsHaveNoProgrammeAssociation() {
        var definition =
                GymNowWorkouts.generate(
                                "spontaneous",
                                NOW,
                                request(45, Set.of("bodyweight"), GymNowWorkouts.Focus.FULL_BODY))
                        .definition();
        for (var origin :
                new GymExecution.Origin[] {
                    GymExecution.Origin.QUICK, GymExecution.Origin.GENERATED
                }) {
            var engine =
                    GymExecutionEngine.start(
                            "actual-" + origin.name(),
                            origin,
                            definition,
                            null,
                            ZoneOffset.UTC,
                            new GymExecution.Tick("boot", 0, NOW));
            ok(engine.snapshot().actualSets().isEmpty(), "Intentions never populate actual sets");
            equal(
                    null,
                    engine.snapshot().source(),
                    "Temporary planner selection creates no owned programme source");
            var first = definition.slots().get(0);
            var measurement = first.exercise().measurement();
            engine.saveSet(
                    first.id(),
                    new StrengthSession.SetEntry(
                            "entered",
                            measurement.usesReps() ? 6 : 0,
                            0,
                            null,
                            false,
                            measurement.usesDuration() ? 20L : null,
                            measurement.usesDistance() ? 20.0 : null),
                    new GymExecution.Tick("boot", 1000, NOW.plusSeconds(1)));
            var finished =
                    engine.finish(
                            new GymExecution.Tick("boot", 2000, NOW.plusSeconds(2)),
                            null,
                            "Actual evidence only",
                            true);
            equal(
                    null,
                    finished.plannedAssociation(),
                    "A spontaneous actual cannot silently complete any planned session");
            equal(origin, finished.origin(), "Quick and generated provenance stay distinct");
            equal(1, finished.actualSets().size(), "Only the entered set becomes actual evidence");
            equal(
                    null,
                    finished.completedSession().rpe(),
                    "Missing effort is not replaced by a target");
        }
    }

    private static void equipmentAndFocusRemainHonest() {
        var bodyweight =
                GymNowWorkouts.generate(
                        "bodyweight",
                        NOW,
                        request(30, Set.of("bodyweight"), GymNowWorkouts.Focus.UPPER));
        ok(
                bodyweight.limitations().stream()
                        .anyMatch(s -> s.contains("Core work is not a replacement")),
                "Core-only fallback is never presented as equivalent pulling");
        var gym =
                GymNowWorkouts.generate(
                        "equipped",
                        NOW,
                        request(45, Set.of("full gym"), GymNowWorkouts.Focus.UPPER));
        ok(
                gym.definition().slots().stream()
                        .anyMatch(s -> pulling(ExerciseMetadataCatalog.require(s.exercise().id()))),
                "Available pulling equipment produces actual pulling");
        ok(
                gym.limitations().stream().noneMatch(s -> s.contains("No upper-body pulling")),
                "Available meaningful pulling is not reported missing");
        var power =
                GymNowWorkouts.generate(
                        "power",
                        NOW,
                        new GymNowWorkouts.Request(
                                45,
                                Set.of("bodyweight"),
                                GymNowWorkouts.Focus.FULL_BODY,
                                GymProgramme.Goal.POWER,
                                GymProgramme.Experience.BEGINNER,
                                GymProgramme.Priority.STRENGTH_FIRST,
                                GymProgramme.Effort.RPE));
        ok(
                power.limitations().stream()
                        .anyMatch(s -> s.contains("does not add Olympic lifts")),
                "The present power-preparation boundary is explained");
    }

    private static void favouritesPreserveTheRequiredRole() {
        var request = request(45, Set.of("full gym"), GymNowWorkouts.Focus.UPPER);
        var selected =
                GymNowWorkouts.generate(
                        "favourite", NOW, request, Set.of("dumbbell-bench-press", "back-squat"));
        ok(
                selected.definition().slots().stream()
                        .anyMatch(s -> s.exercise().id().equals("dumbbell-bench-press")),
                "A suitable horizontal-push favourite can win the planner's existing tie-break");
        ok(
                selected.definition().slots().stream()
                        .noneMatch(s -> s.exercise().id().equals("back-squat")),
                "An inappropriate favourite cannot force a lower compound into upper focus");
        ok(
                selected.explanation().contains("Suitable favourite exercises"),
                "An actual preference tie-break is explained");
        equal(
                GymNowWorkouts.generate("unknown-favourite", NOW, request),
                GymNowWorkouts.generate(
                        "unknown-favourite", NOW, request, Set.of("made-up-exercise")),
                "An unknown preference never invents an exercise");
        var orderedA =
                new java.util.LinkedHashSet<>(Immutable.list("back-squat", "dumbbell-bench-press"));
        var orderedB =
                new java.util.LinkedHashSet<>(Immutable.list("dumbbell-bench-press", "back-squat"));
        equal(
                GymNowWorkouts.generate("order", NOW, request, orderedA),
                GymNowWorkouts.generate("order", NOW, request, orderedB),
                "Preference collection iteration order is not a random selection source");
    }

    private static void effortMeasurementsRemainCompatible() {
        var request =
                new GymNowWorkouts.Request(
                        60,
                        Set.of("bodyweight"),
                        GymNowWorkouts.Focus.FULL_BODY,
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Effort.RIR);
        boolean timed = false, repetitions = false;
        for (var slot : GymNowWorkouts.generate("rir", NOW, request).definition().slots()) {
            if (slot.exercise().measurement().usesReps()) {
                repetitions = true;
                equal(
                        GymWorkoutDefinition.EffortScale.RIR,
                        slot.target().effort().scale(),
                        "Repetition guidance can use RIR");
            } else if (slot.exercise().measurement().usesDuration()) {
                timed = true;
                equal(
                        GymWorkoutDefinition.EffortScale.RPE,
                        slot.target().effort().scale(),
                        "A timed hold does not invent repetitions in reserve");
            }
        }
        ok(timed && repetitions, "Both genuine measurement families are exercised");
    }

    private static void insufficientFullBodyBudgetIsAnExplicitChoice() {
        var request =
                new GymNowWorkouts.Request(
                        30,
                        Set.of("full gym"),
                        GymNowWorkouts.Focus.FULL_BODY,
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Experience.EXPERIENCED,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Effort.RPE);
        try {
            var result = GymNowWorkouts.generate("too-short", NOW, request);
            boolean hasUpper =
                    result.definition().slots().stream()
                            .anyMatch(
                                    s -> upper(ExerciseMetadataCatalog.require(s.exercise().id())));
            boolean hasLower =
                    result.definition().slots().stream()
                            .anyMatch(s -> s.exercise().region() == StrengthExercise.Region.LOWER);
            ok(
                    hasUpper && hasLower,
                    "A future planner improvement may fit full body, but lower-only work cannot be"
                            + " labelled full body");
        } catch (IllegalArgumentException expected) {
            ok(
                    expected.getMessage().contains("Choose more time or a different focus"),
                    "The athlete receives a usable alternative, not an empty plan");
        }
    }

    private static GymNowWorkouts.Request request(
            int minutes, Set<String> equipment, GymNowWorkouts.Focus focus) {
        return new GymNowWorkouts.Request(
                minutes,
                equipment,
                focus,
                GymProgramme.Goal.GENERAL,
                GymProgramme.Experience.BEGINNER,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Effort.RPE);
    }

    private static boolean upper(ExerciseMetadata metadata) {
        return metadata.roles().contains(ExerciseMetadata.Role.HORIZONTAL_PUSH)
                || metadata.roles().contains(ExerciseMetadata.Role.VERTICAL_PUSH)
                || pulling(metadata);
    }

    private static boolean pulling(ExerciseMetadata metadata) {
        return metadata.roles().contains(ExerciseMetadata.Role.HORIZONTAL_PULL)
                || metadata.roles().contains(ExerciseMetadata.Role.VERTICAL_PULL);
    }

    private static void reject(Runnable operation, String reason) {
        try {
            operation.run();
            throw new AssertionError(reason);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void unsupported(Runnable operation, String reason) {
        try {
            operation.run();
            throw new AssertionError(reason);
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void equal(Object expected, Object actual, String reason) {
        ok(
                java.util.Objects.equals(expected, actual),
                reason + ": expected=" + expected + ", actual=" + actual);
    }

    private static void ok(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
        checks++;
    }
}
