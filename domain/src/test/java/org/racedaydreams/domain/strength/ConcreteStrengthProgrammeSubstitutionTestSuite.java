package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.time.*;
import java.util.*;

/** Permanent choices retain accepted evidence and phase-specific intentions, never actuals. */
public final class ConcreteStrengthProgrammeSubstitutionTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private static final Set<ExerciseMetadata.Equipment> ALL =
            EnumSet.allOf(ExerciseMetadata.Equipment.class);

    public static void main(String[] args) {
        var settings =
                StrengthProgrammeTestSuite.settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Experience.INTERMEDIATE,
                        GymProgramme.Priority.BALANCED,
                        4,
                        60,
                        ALL);
        var generated =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        "swap-base",
                                        NOW,
                                        "Accepted programme",
                                        settings,
                                        LocalDate.of(2026, 11, 4),
                                        12,
                                        "upper-lower-4",
                                        PrimaryFocus.STRENGTH,
                                        Set.of(),
                                        null,
                                        Set.of(),
                                        Set.of("bench-press"),
                                        false));
        var blocks = new ArrayList<StrengthProgramme.Block>();
        for (var block : generated.blocks()) {
            var workouts = new ArrayList<StrengthProgramme.Workout>();
            for (var workout : block.workouts()) {
                var slots = new ArrayList<GymWorkoutDefinition.Slot>();
                for (var slot : workout.definition().slots()) {
                    var t = slot.target();
                    if (slot.exercise().id().equals("bench-press"))
                        t =
                                new GymWorkoutDefinition.Target(
                                        t.sets(),
                                        t.minimumReps(),
                                        t.maximumReps(),
                                        null,
                                        null,
                                        t.effort(),
                                        t.restSeconds(),
                                        80.0);
                    slots.add(new GymWorkoutDefinition.Slot(slot.id(), slot.exercise(), t, null));
                }
                var d = workout.definition();
                workouts.add(
                        new StrengthProgramme.Workout(
                                workout.id(),
                                workout.date(),
                                workout.expectedMinutes(),
                                new GymWorkoutDefinition(
                                        d.id(), d.revision(), d.createdAt(), d.title(), slots),
                                workout.notes()));
            }
            blocks.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            block.start(),
                            block.weeks(),
                            workouts,
                            block.explanation()));
        }
        var base =
                new StrengthProgramme(
                        generated.id(),
                        1,
                        0,
                        NOW,
                        generated.name(),
                        generated.origin(),
                        generated.templateId(),
                        settings,
                        blocks,
                        generated.effectiveFrom(),
                        generated.reason(),
                        generated.tradeoffs());
        var future =
                new StrengthProgramme(
                        base.id(),
                        2,
                        1,
                        NOW,
                        base.name(),
                        base.origin(),
                        base.templateId(),
                        settings,
                        base.blocks(),
                        base.effectiveFrom(),
                        "Athlete reviewing future work",
                        base.tradeoffs());
        var draft =
                new ConcreteStrengthProgrammeDraft(
                        "swap-review",
                        1,
                        new Reference(Owner.AUTHORED, base.id(), 1),
                        future,
                        List.of());
        var benches =
                base.workouts().stream()
                        .filter(
                                w ->
                                        w.definition().slots().stream()
                                                .anyMatch(
                                                        s ->
                                                                s.exercise()
                                                                        .id()
                                                                        .equals("bench-press")))
                        .toList();
        check(benches.size() > 2, "Fixture contains multiple phase-specific bench prescriptions");
        var protectedWorkout = benches.get(0);
        var selected = benches.get(benches.size() - 1);
        var slot =
                selected.definition().slots().stream()
                        .filter(s -> s.exercise().id().equals("bench-press"))
                        .findFirst()
                        .orElseThrow();
        var context =
                context(
                        slot.exercise(),
                        settings,
                        Set.of(),
                        ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY);
        String replacement = "dumbbell-bench-press";
        check(
                ExerciseSubstitutionEngine.rank(context, 50).stream()
                        .anyMatch(c -> c.exercise().id().equals(replacement)),
                "Replacement really is ranked");
        var protectedIds = Set.of(protectedWorkout.id());
        var one =
                ConcreteStrengthProgrammeEditor.substitute(
                        draft,
                        base,
                        selected.id(),
                        slot.id(),
                        context,
                        replacement,
                        null,
                        protectedIds,
                        false,
                        NOW.plusSeconds(1));
        check(
                one.revision() == 2 && one.programme().revision() == 2,
                "One action advances draft only, retaining pending programme revision");
        check(one.substitutions().size() == 1, "One dated replacement has one provenance mapping");
        check(
                one.substitutions().get(0).fromExerciseId().equals("bench-press"),
                "Mapping starts at accepted base");
        check(
                one.programme().workouts().stream()
                        .filter(w -> !w.id().equals(selected.id()))
                        .allMatch(base.workouts()::contains),
                "All other dated workouts remain exact");
        var changed =
                one.programme().workouts().stream()
                        .filter(w -> w.id().equals(selected.id()))
                        .findFirst()
                        .orElseThrow();
        check(
                changed.definition().slots().stream()
                                .filter(s -> s.id().equals(slot.id()))
                                .findFirst()
                                .orElseThrow()
                                .target()
                                .referenceLoadKg()
                        == null,
                "Barbell load is not transferred to dumbbells");
        check(slot.target().referenceLoadKg() == 80.0, "Accepted reference is immutable");
        var all =
                ConcreteStrengthProgrammeEditor.substitute(
                        draft,
                        base,
                        selected.id(),
                        slot.id(),
                        context,
                        replacement,
                        null,
                        protectedIds,
                        true,
                        NOW.plusSeconds(1));
        check(all.revision() == 2, "Bulk choice remains one optimistic save");
        check(
                all.substitutions().size() == benches.size() - 1,
                "All future occurrences exclude protected completion");
        check(
                all.programme().workouts().stream()
                        .filter(w -> w.id().equals(protectedWorkout.id()))
                        .findFirst()
                        .orElseThrow()
                        .equals(protectedWorkout),
                "Completed prescription retains its own load and target");
        for (var change : all.substitutions()) {
            var before = slot(base, change.workoutId(), change.slotId());
            var after = slot(all.programme(), change.workoutId(), change.slotId());
            check(
                    before.target().withoutLoadReference().equals(after.target()),
                    "Each phase keeps its own rep/effort/rest target");
        }
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.substitute(
                                draft,
                                base,
                                selected.id(),
                                slot.id(),
                                context,
                                replacement,
                                null,
                                Set.of(selected.id()),
                                false,
                                NOW.plusSeconds(1)));
        var excluded =
                context(
                        slot.exercise(),
                        settings,
                        Set.of(replacement),
                        ExerciseSubstitutionEngine.Reason.ALTERNATIVES);
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.substitute(
                                draft,
                                base,
                                selected.id(),
                                slot.id(),
                                excluded,
                                replacement,
                                null,
                                Set.of(),
                                false,
                                NOW.plusSeconds(1)));
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.substitute(
                                draft,
                                base,
                                selected.id(),
                                slot.id(),
                                context,
                                "seated-leg-curl",
                                null,
                                Set.of(),
                                false,
                                NOW.plusSeconds(1)));
        var current = slot(one.programme(), selected.id(), slot.id());
        var undo =
                ConcreteStrengthProgrammeEditor.substitute(
                        one,
                        base,
                        selected.id(),
                        slot.id(),
                        context(
                                current.exercise(),
                                settings,
                                Set.of(),
                                ExerciseSubstitutionEngine.Reason.ALTERNATIVES),
                        "bench-press",
                        null,
                        protectedIds,
                        false,
                        NOW.plusSeconds(2));
        check(
                undo.substitutions().isEmpty(),
                "Undoing an unaccepted replacement clears its mapping");
        check(
                slot(undo.programme(), selected.id(), slot.id()).target().referenceLoadKg() == null,
                "Undo does not resurrect a stale load reference");
        check(draft.programme().equals(future), "Input draft remains immutable");
        var changedMeasurement =
                ExerciseSubstitutionEngine.rank(context, 50).stream()
                        .filter(ExerciseSubstitutionEngine.Suggestion::measurementChanged)
                        .findFirst()
                        .orElseThrow();
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.substitute(
                                draft,
                                base,
                                selected.id(),
                                slot.id(),
                                context,
                                changedMeasurement.exercise().id(),
                                null,
                                protectedIds,
                                false,
                                NOW.plusSeconds(1)));
        var freshTarget =
                new GymWorkoutDefinition.Target(
                        2,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, 2, 3),
                        90,
                        12.5);
        rejects(
                () ->
                        ConcreteStrengthProgrammeEditor.substitute(
                                draft,
                                base,
                                selected.id(),
                                slot.id(),
                                context,
                                changedMeasurement.exercise().id(),
                                freshTarget,
                                protectedIds,
                                true,
                                NOW.plusSeconds(1)));
        var changedTask =
                ConcreteStrengthProgrammeEditor.substitute(
                        draft,
                        base,
                        selected.id(),
                        slot.id(),
                        context,
                        changedMeasurement.exercise().id(),
                        freshTarget,
                        protectedIds,
                        false,
                        NOW.plusSeconds(1));
        check(
                slot(changedTask.programme(), selected.id(), slot.id()).target().referenceLoadKg()
                        == null,
                "Changed task never inherits a load reference");
        check(
                slot(changedTask.programme(), selected.id(), slot.id()).exercise().measurement()
                        == changedMeasurement.exercise().measurement(),
                "Reviewed measurement matches replacement");
        System.out.println(
                "ConcreteStrengthProgrammeSubstitutionTestSuite: " + checks + " checks passed.");
    }

    private static GymWorkoutDefinition.Slot slot(
            StrengthProgramme p, String workout, String slot) {
        return p.workouts().stream()
                .filter(w -> w.id().equals(workout))
                .findFirst()
                .orElseThrow()
                .definition()
                .slots()
                .stream()
                .filter(s -> s.id().equals(slot))
                .findFirst()
                .orElseThrow();
    }

    private static ExerciseSubstitutionEngine.Context context(
            StrengthExercise e,
            StrengthProgramme.Settings s,
            Set<String> excluded,
            ExerciseSubstitutionEngine.Reason reason) {
        return new ExerciseSubstitutionEngine.Context(
                e, s.goal(), s.experience(), s.equipment(), excluded, Set.of(), Set.of(), reason);
    }

    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }

    private static void rejects(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected rejection");
    }
}
