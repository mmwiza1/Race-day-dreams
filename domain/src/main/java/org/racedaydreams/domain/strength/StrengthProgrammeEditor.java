package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/** Future-only, explicit proposals. Persistence/activation is a separate athlete command. */
public final class StrengthProgrammeEditor {
    private StrengthProgrammeEditor() {}

    /** Preview only. Acceptance must independently verify current revision and completed IDs. */
    public static Proposal replaceFuture(
            StrengthProgramme previous,
            StrengthProgramme candidate,
            LocalDate effectiveFrom,
            Instant at,
            Set<String> completed,
            String reason) {
        Objects.requireNonNull(previous);
        Objects.requireNonNull(candidate);
        Objects.requireNonNull(effectiveFrom);
        Objects.requireNonNull(completed);
        if (!candidate.id().equals(previous.id()) || !candidate.start().equals(previous.start()))
            throw new IllegalArgumentException(
                    "Regeneration must retain programme identity and start.");
        var retained = new ArrayList<StrengthProgramme.Workout>();
        var occupied = new HashSet<LocalDate>();
        var old = new HashMap<String, StrengthProgramme.Workout>();
        for (var workout : previous.workouts()) {
            old.put(workout.id(), workout);
            if (workout.date().isBefore(effectiveFrom) || completed.contains(workout.id())) {
                if (workout.date().isBefore(candidate.start())
                        || workout.date().isAfter(candidate.end()))
                    throw new IllegalArgumentException(
                            "New duration would discard retained prescription history.");
                retained.add(workout);
                occupied.add(workout.date());
            }
        }
        var blocks = new ArrayList<StrengthProgramme.Block>();
        for (var block : candidate.blocks()) {
            var sessions = new ArrayList<StrengthProgramme.Workout>();
            for (var workout : block.workouts()) {
                if (workout.date().isBefore(effectiveFrom) || occupied.contains(workout.date()))
                    continue;
                var existing = old.get(workout.id());
                var d = workout.definition();
                sessions.add(
                        new StrengthProgramme.Workout(
                                workout.id(),
                                workout.date(),
                                workout.expectedMinutes(),
                                new GymWorkoutDefinition(
                                        d.id(),
                                        existing == null ? 1 : existing.definition().revision() + 1,
                                        at,
                                        d.title(),
                                        d.slots()),
                                workout.notes()));
            }
            for (var workout : retained)
                if (!workout.date().isBefore(block.start()) && !workout.date().isAfter(block.end()))
                    sessions.add(workout);
            sessions.sort(Comparator.comparing(StrengthProgramme.Workout::date));
            blocks.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            block.start(),
                            block.weeks(),
                            sessions,
                            block.explanation()));
        }
        var tradeoffs = new ArrayList<>(candidate.tradeoffs());
        if (!retained.isEmpty())
            tradeoffs.add(
                    "Historical and completed sessions retain their exact prior prescriptions, even"
                            + " when availability changes.");
        var next =
                new StrengthProgramme(
                        previous.id(),
                        previous.revision() + 1,
                        previous.revision(),
                        at,
                        candidate.name(),
                        candidate.origin(),
                        candidate.templateId(),
                        candidate.settings(),
                        blocks,
                        effectiveFrom,
                        reason,
                        tradeoffs);
        return new Proposal(previous, next, completed, java.util.Collections.emptyList());
    }

    public record Change(
            String workoutId,
            String slotId,
            String fromExerciseId,
            String toExerciseId,
            String reason) {
        public Change {
            GymWorkoutDefinition.requireId(workoutId, "workout");
            GymWorkoutDefinition.requireId(slotId, "slot");
            GymWorkoutDefinition.requireId(fromExerciseId, "original exercise");
            GymWorkoutDefinition.requireId(toExerciseId, "replacement exercise");
            StrengthProgramme.text(reason, 2000, false, "substitution reason");
            if (fromExerciseId.equals(toExerciseId))
                throw new IllegalArgumentException("Choose a different exercise.");
        }
    }

    public record Proposal(
            StrengthProgramme previous,
            StrengthProgramme proposed,
            Set<String> completedWorkoutIds,
            List<Change> substitutions) {
        public Proposal {
            Objects.requireNonNull(previous);
            Objects.requireNonNull(proposed);
            completedWorkoutIds = Immutable.copySet(completedWorkoutIds);
            substitutions = Immutable.copyList(substitutions);
            if (!previous.id().equals(proposed.id())
                    || proposed.revision() != previous.revision() + 1
                    || proposed.createdAt().isBefore(previous.createdAt())
                    || proposed.effectiveFrom().isBefore(previous.effectiveFrom()))
                throw new IllegalArgumentException("Review a forward revision of this programme.");
            var after = new HashMap<String, StrengthProgramme.Workout>();
            for (var w : proposed.workouts()) after.put(w.id(), w);
            var before = new HashMap<String, StrengthProgramme.Workout>();
            for (var w : previous.workouts()) {
                before.put(w.id(), w);
                if ((w.date().isBefore(proposed.effectiveFrom())
                                || completedWorkoutIds.contains(w.id()))
                        && !w.equals(after.get(w.id())))
                    throw new IllegalArgumentException(
                            "Historical and completed prescriptions cannot be rewritten.");
            }
            if (!before.keySet().containsAll(completedWorkoutIds))
                throw new IllegalArgumentException(
                        "Completed evidence references an unknown session.");
            for (var workout : proposed.workouts()) {
                if (!workout.date().isBefore(proposed.effectiveFrom())
                        && !completedWorkoutIds.contains(workout.id())
                        && !proposed.settings().days().contains(workout.date().getDayOfWeek()))
                    throw new IllegalArgumentException(
                            "Future programming must respect available days.");
                if (workout.date().isBefore(proposed.effectiveFrom())
                        && !workout.equals(before.get(workout.id())))
                    throw new IllegalArgumentException(
                            "New backdated prescriptions are not permitted.");
            }
            for (var change : substitutions) {
                var old = before.get(change.workoutId());
                var updated = after.get(change.workoutId());
                if (old == null || updated == null)
                    throw new IllegalArgumentException("Substitution session is missing.");
                var from = slot(old, change.slotId());
                var to = slot(updated, change.slotId());
                if (!from.exercise().id().equals(change.fromExerciseId())
                        || !to.exercise().id().equals(change.toExerciseId())
                        || to.target() != null && to.target().referenceLoadKg() != null)
                    throw new IllegalArgumentException(
                            "Substitution provenance or cleared load reference is invalid.");
            }
        }
    }

    /** Validate eligibility using the same intent/equipment/exclusion policy as today's swaps. */
    public static Proposal substitute(
            StrengthProgramme programme,
            String workoutId,
            String slotId,
            ExerciseSubstitutionEngine.Context context,
            String replacementId,
            GymWorkoutDefinition.Target changedMeasurementTarget,
            LocalDate effectiveFrom,
            Instant at,
            Set<String> completed) {
        var original =
                programme.workouts().stream()
                        .filter(w -> w.id().equals(workoutId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Session not found."));
        var from = slot(original, slotId);
        if (!from.exercise().equals(context.original())
                || context.goal() != programme.settings().goal()
                || context.experience() != programme.settings().experience()
                || !context.availableEquipment().equals(programme.settings().equipment()))
            throw new IllegalArgumentException(
                    "Review alternatives using the programme's current context.");
        var suggestion =
                ExerciseSubstitutionEngine.rank(context, 50).stream()
                        .filter(s -> s.exercise().id().equals(replacementId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Replacement is unavailable or excluded."));
        var target = suggestion.measurementChanged() ? changedMeasurementTarget : from.target();
        if (suggestion.measurementChanged() && target == null)
            throw new IllegalArgumentException("Review a target for the new measurement.");
        if (target != null) target = target.withoutLoadReference();
        var slots = new ArrayList<>(original.definition().slots());
        slots.set(
                slots.indexOf(from),
                new GymWorkoutDefinition.Slot(slotId, suggestion.exercise(), target, null));
        var changed =
                new StrengthProgramme.Workout(
                        original.id(),
                        original.date(),
                        StrengthWorkoutBudget.workoutMinutes(slots),
                        new GymWorkoutDefinition(
                                original.definition().id(),
                                original.definition().revision() + 1,
                                at,
                                original.definition().title(),
                                slots),
                        original.notes());
        var blocks = new ArrayList<StrengthProgramme.Block>();
        for (var block : programme.blocks()) {
            var sessions = new ArrayList<>(block.workouts());
            for (int i = 0; i < sessions.size(); i++)
                if (sessions.get(i).id().equals(workoutId)) sessions.set(i, changed);
            blocks.add(
                    new StrengthProgramme.Block(
                            block.id(),
                            block.phase(),
                            block.name(),
                            block.start(),
                            block.weeks(),
                            sessions,
                            block.explanation()));
        }
        var reason =
                "Athlete chose a permanent programme substitution. " + suggestion.explanation();
        var next =
                new StrengthProgramme(
                        programme.id(),
                        programme.revision() + 1,
                        programme.revision(),
                        at,
                        programme.name(),
                        programme.origin(),
                        programme.templateId(),
                        programme.settings(),
                        blocks,
                        effectiveFrom,
                        reason,
                        programme.tradeoffs());
        return new Proposal(
                programme,
                next,
                completed,
                Immutable.list(
                        new Change(
                                workoutId, slotId, from.exercise().id(), replacementId, reason)));
    }

    private static GymWorkoutDefinition.Slot slot(StrengthProgramme.Workout w, String id) {
        return w.definition().slots().stream()
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Preserved exercise slot is missing."));
    }
}
