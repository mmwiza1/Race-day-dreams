package org.racedaydreams.domain.strength;

import java.time.Instant;
import java.util.*;

/** Edits an unaccepted concrete preview; every other dated prescription is retained exactly. */
public final class ConcreteStrengthProgrammeEditor {
    private ConcreteStrengthProgrammeEditor() {}

    public static ConcreteStrengthProgrammeDraft replaceWorkout(
            ConcreteStrengthProgrammeDraft draft, StrengthProgramme.Workout replacement) {
        var old = draft.programme();
        var blocks = new ArrayList<StrengthProgramme.Block>();
        boolean found = false;
        for (var block : old.blocks()) {
            var rows = new ArrayList<StrengthProgramme.Workout>();
            for (var workout : block.workouts()) {
                if (workout.id().equals(replacement.id())) {
                    if (!workout.date().equals(replacement.date()))
                        throw new IllegalArgumentException(
                                "Review schedule changes through regeneration.");
                    rows.add(replacement);
                    found = true;
                } else rows.add(workout);
            }
            blocks.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            block.start(),
                            block.weeks(),
                            rows,
                            block.explanation()));
        }
        if (!found) throw new IllegalArgumentException("This workout is no longer in the preview.");
        var edited =
                new StrengthProgramme(
                        old.id(),
                        old.revision(),
                        old.parentRevision(),
                        old.createdAt(),
                        old.name(),
                        old.origin(),
                        old.templateId(),
                        old.settings(),
                        blocks,
                        old.effectiveFrom(),
                        old.reason(),
                        old.tradeoffs());
        return draft.withProgramme(edited, draft.substitutions());
    }

    /** A retained accepted intent, including a replacement already chosen in this draft. */
    public static boolean retainedIntent(
            ConcreteStrengthProgrammeDraft draft,
            StrengthProgramme base,
            String workoutId,
            String slotId) {
        var previous = findSlot(base, workoutId, slotId);
        var current = findSlot(draft.programme(), workoutId, slotId);
        if (previous == null || current == null) return false;
        return previous.exercise().id().equals(current.exercise().id())
                || draft.substitutions().stream()
                        .anyMatch(
                                c ->
                                        c.workoutId().equals(workoutId)
                                                && c.slotId().equals(slotId)
                                                && c.fromExerciseId()
                                                        .equals(previous.exercise().id())
                                                && c.toExerciseId()
                                                        .equals(current.exercise().id()));
    }

    /**
     * One athlete choice updates one draft revision. All-occurrence swaps keep each phase's target;
     * changed measurements require a separate reviewed target for each dated workout.
     */
    public static ConcreteStrengthProgrammeDraft substitute(
            ConcreteStrengthProgrammeDraft draft,
            StrengthProgramme base,
            String workoutId,
            String slotId,
            ExerciseSubstitutionEngine.Context context,
            String replacementId,
            GymWorkoutDefinition.Target changedTarget,
            Set<String> protectedIds,
            boolean allFutureOccurrences,
            Instant at) {
        draft.validateAgainst(base);
        var programme = draft.programme();
        var selected =
                programme.workouts().stream()
                        .filter(w -> w.id().equals(workoutId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Choose a dated workout."));
        var selectedSlot = findSlot(programme, workoutId, slotId);
        if (selectedSlot == null
                || !selectedSlot.exercise().equals(context.original())
                || !retainedIntent(draft, base, workoutId, slotId)
                || selected.date().isBefore(programme.effectiveFrom())
                || protectedIds.contains(workoutId)
                || context.goal() != programme.settings().goal()
                || context.experience() != programme.settings().experience()
                || !context.availableEquipment().equals(programme.settings().equipment()))
            throw new IllegalArgumentException(
                    "Review a retained, uncompleted future exercise using this programme's"
                            + " context.");
        var suggestion =
                ExerciseSubstitutionEngine.rank(context, 50).stream()
                        .filter(c -> c.exercise().id().equals(replacementId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Replacement is unavailable or excluded."));
        if (suggestion.measurementChanged() && (changedTarget == null || allFutureOccurrences))
            throw new IllegalArgumentException(
                    "Review a fresh measurement target for each dated workout.");
        var changes = new ArrayList<>(draft.substitutions());
        var blocks = new ArrayList<StrengthProgramme.Block>();
        int count = 0;
        for (var block : programme.blocks()) {
            var workouts = new ArrayList<StrengthProgramme.Workout>();
            for (var workout : block.workouts()) {
                var slots = new ArrayList<>(workout.definition().slots());
                boolean changed = false;
                if (!workout.date().isBefore(programme.effectiveFrom())
                        && !protectedIds.contains(workout.id()))
                    for (int index = 0; index < slots.size(); index++) {
                        var slot = slots.get(index);
                        if (!(workout.id().equals(workoutId) && slot.id().equals(slotId))
                                && !(allFutureOccurrences
                                        && slot.exercise().id().equals(context.original().id())
                                        && retainedIntent(draft, base, workout.id(), slot.id())))
                            continue;
                        var previous = findSlot(base, workout.id(), slot.id());
                        // The final alternative must still retain the accepted base's intent;
                        // a chain of pending swaps cannot drift to an unrelated movement.
                        var baseContext =
                                new ExerciseSubstitutionEngine.Context(
                                        previous.exercise(),
                                        context.goal(),
                                        context.experience(),
                                        context.availableEquipment(),
                                        context.excludedIds(),
                                        context.favouriteIds(),
                                        context.historyIds(),
                                        ExerciseSubstitutionEngine.Reason.ALTERNATIVES);
                        if (ExerciseSubstitutionEngine.rank(baseContext, 50).stream()
                                        .noneMatch(c -> c.exercise().id().equals(replacementId))
                                && !previous.exercise().id().equals(replacementId))
                            throw new IllegalArgumentException(
                                    "The alternative must retain the original programme intent.");
                        var target =
                                suggestion.measurementChanged() ? changedTarget : slot.target();
                        if (target == null)
                            throw new IllegalArgumentException("Review a target before swapping.");
                        slots.set(
                                index,
                                new GymWorkoutDefinition.Slot(
                                        slot.id(),
                                        suggestion.exercise(),
                                        target.withoutLoadReference(),
                                        null));
                        String changedWorkout = workout.id(), changedSlot = slot.id();
                        changes.removeIf(
                                c ->
                                        c.workoutId().equals(changedWorkout)
                                                && c.slotId().equals(changedSlot));
                        if (!previous.exercise().id().equals(replacementId))
                            changes.add(
                                    new StrengthProgrammeEditor.Change(
                                            workout.id(),
                                            slot.id(),
                                            previous.exercise().id(),
                                            replacementId,
                                            "Athlete chose a programme substitution. "
                                                    + suggestion.explanation()));
                        changed = true;
                        count++;
                    }
                if (changed) {
                    var definition = workout.definition();
                    workouts.add(
                            new StrengthProgramme.Workout(
                                    workout.id(),
                                    workout.date(),
                                    StrengthWorkoutBudget.workoutMinutes(slots),
                                    new GymWorkoutDefinition(
                                            definition.id(),
                                            Math.addExact(definition.revision(), 1),
                                            at,
                                            definition.title(),
                                            slots),
                                    workout.notes()));
                } else workouts.add(workout);
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
        if (count == 0)
            throw new IllegalArgumentException("No uncompleted future occurrence remains.");
        var proposed =
                new StrengthProgramme(
                        programme.id(),
                        programme.revision(),
                        programme.parentRevision(),
                        at,
                        programme.name(),
                        programme.origin(),
                        programme.templateId(),
                        programme.settings(),
                        blocks,
                        programme.effectiveFrom(),
                        "Athlete chose "
                                + suggestion.exercise().name()
                                + " for "
                                + count
                                + " future prescription(s). "
                                + suggestion.explanation(),
                        programme.tradeoffs());
        new StrengthProgrammeEditor.Proposal(base, proposed, protectedIds, changes);
        return draft.withProgramme(proposed, changes);
    }

    private static GymWorkoutDefinition.Slot findSlot(
            StrengthProgramme programme, String workoutId, String slotId) {
        for (var workout : programme.workouts())
            if (workout.id().equals(workoutId))
                for (var slot : workout.definition().slots())
                    if (slot.id().equals(slotId)) return slot;
        return null;
    }
}
