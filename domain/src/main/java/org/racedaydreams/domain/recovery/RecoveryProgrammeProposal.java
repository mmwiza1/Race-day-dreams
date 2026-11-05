package org.racedaydreams.domain.recovery;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/** Preview-only short changes. The existing revision repository owns reviewed activation. */
public final class RecoveryProgrammeProposal {
    private RecoveryProgrammeProposal() {}

    public static StrengthProgrammeEditor.Proposal lighterWeek(
            StrengthProgramme previous,
            TrainingRecovery.Review review,
            Instant now,
            ZoneOffset offset,
            Set<String> protectedIds) {
        LocalDate today = now.atOffset(offset).toLocalDate();
        if (!review.date().equals(today) || !review.lighterWeek())
            throw new IllegalArgumentException(
                    "Refresh supported recovery evidence before creating a lighter-week preview.");
        int[] changed = {0};
        var blocks = new ArrayList<StrengthProgramme.Block>();
        for (var block : previous.blocks()) {
            var workouts = new ArrayList<StrengthProgramme.Workout>();
            for (var w : block.workouts()) {
                if (w.date().isBefore(today)
                        || w.date().isBefore(previous.effectiveFrom())
                        || w.date().isAfter(today.plusDays(6))
                        || protectedIds.contains(w.id())) {
                    workouts.add(w);
                    continue;
                }
                var slots = new ArrayList<GymWorkoutDefinition.Slot>();
                boolean different = false;
                for (var slot : w.definition().slots()) {
                    var t = slot.target();
                    if (t != null && t.sets() > 1) {
                        t =
                                new GymWorkoutDefinition.Target(
                                        t.sets() - 1,
                                        t.minimumReps(),
                                        t.maximumReps(),
                                        t.seconds(),
                                        t.metres(),
                                        t.effort(),
                                        t.restSeconds(),
                                        t.referenceLoadKg());
                        different = true;
                    }
                    slots.add(
                            new GymWorkoutDefinition.Slot(
                                    slot.id(), slot.exercise(), t, slot.supersetGroup()));
                }
                if (!different) {
                    workouts.add(w);
                    continue;
                }
                changed[0]++;
                workouts.add(
                        new StrengthProgramme.Workout(
                                w.id(),
                                w.date(),
                                w.expectedMinutes(),
                                new GymWorkoutDefinition(
                                        w.definition().id(),
                                        Math.addExact(w.definition().revision(), 1),
                                        now,
                                        w.definition().title(),
                                        slots),
                                w.notes()));
            }
            blocks.add(copy(block, workouts));
        }
        if (changed[0] == 0)
            throw new IllegalArgumentException(
                    "No eligible future working-set targets can be reduced. Keep your programme or"
                            + " review it manually.");
        return proposal(
                previous,
                blocks,
                today,
                now,
                protectedIds,
                "Athlete requested a lighter-week preview after repeated demanding recorded"
                    + " strength sessions. One fewer working set per targeted exercise for the next"
                    + " seven days, minimum one; loads, effort, tasks and later weeks are"
                    + " unchanged. Activation requires a separate review.");
    }

    /**
     * Only offers dates in the same anchored programme week/block with fewer schedule conflicts.
     */
    public static List<LocalDate> alternatives(
            StrengthProgramme p,
            String workoutId,
            List<PlannedWorkout> running,
            LocalDate today,
            Set<String> protectedIds) {
        var w = find(p, workoutId);
        if (w.date().isBefore(today)
                || w.date().isBefore(p.effectiveFrom())
                || protectedIds.contains(workoutId)) return Collections.emptyList();
        var block =
                p.blocks().stream()
                        .filter(b -> b.workouts().stream().anyMatch(x -> x.id().equals(workoutId)))
                        .findFirst()
                        .orElseThrow();
        long week = java.time.temporal.ChronoUnit.DAYS.between(block.start(), w.date()) / 7;
        var first = block.start().plusWeeks(week);
        var occupied = new HashSet<LocalDate>();
        for (var row : p.workouts()) if (!row.id().equals(workoutId)) occupied.add(row.date());
        int old = penalty(w.date(), running, today);
        var choices = new ArrayList<LocalDate>();
        for (int i = 0; i < 7; i++) {
            var date = first.plusDays(i);
            if (date.isBefore(today)
                    || date.isBefore(p.effectiveFrom())
                    || date.equals(w.date())
                    || occupied.contains(date)
                    || !p.settings().days().contains(date.getDayOfWeek())) continue;
            if (penalty(date, running, today) < old) choices.add(date);
        }
        choices.sort(
                Comparator.comparingInt((LocalDate d) -> penalty(d, running, today))
                        .thenComparing(d -> d));
        return org.racedaydreams.domain.coaching.Immutable.copyList(choices);
    }

    public static StrengthProgrammeEditor.Proposal move(
            StrengthProgramme previous,
            String workoutId,
            LocalDate destination,
            List<PlannedWorkout> running,
            LocalDate today,
            Instant now,
            Set<String> protectedIds) {
        if (!alternatives(previous, workoutId, running, today, protectedIds).contains(destination))
            throw new IllegalArgumentException(
                    "That date is no longer an eligible lower-conflict option. Refresh the"
                            + " schedule.");
        var blocks = new ArrayList<StrengthProgramme.Block>();
        for (var block : previous.blocks()) {
            var workouts = new ArrayList<StrengthProgramme.Workout>();
            for (var w : block.workouts())
                workouts.add(
                        w.id().equals(workoutId)
                                ? new StrengthProgramme.Workout(
                                        w.id(),
                                        destination,
                                        w.expectedMinutes(),
                                        w.definition(),
                                        w.notes())
                                : w);
            workouts.sort(Comparator.comparing(StrengthProgramme.Workout::date));
            blocks.add(copy(block, workouts));
        }
        return proposal(
                previous,
                blocks,
                today,
                now,
                protectedIds,
                "Athlete requested a lower-conflict gym date for "
                        + workoutId
                        + " on "
                        + destination
                        + ". Workout targets and running plans are unchanged; review and activate"
                        + " separately.");
    }

    private static int penalty(LocalDate date, List<PlannedWorkout> running, LocalDate today) {
        int total = 0;
        for (var run : running)
            if (TrainingRecovery.demanding(run) && !run.date().isBefore(today)) {
                long gap = java.time.temporal.ChronoUnit.DAYS.between(date, run.date());
                total +=
                        gap == 0
                                ? 4
                                : gap == 1
                                        ? 3
                                        : gap == -1
                                                ? 1
                                                : run.category()
                                                                        == org.racedaydreams.domain
                                                                                .WorkoutCategory
                                                                                .RACE
                                                                && gap >= 2
                                                                && gap <= 6
                                                        ? 2
                                                        : 0;
            }
        return total;
    }

    private static StrengthProgramme.Workout find(StrengthProgramme p, String id) {
        return p.workouts().stream()
                .filter(w -> w.id().equals(id))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Programme workout is unavailable."));
    }

    private static StrengthProgramme.Block copy(
            StrengthProgramme.Block b, List<StrengthProgramme.Workout> workouts) {
        return new StrengthProgramme.Block(
                b.id(), b.phase(), b.name(), b.start(), b.weeks(), workouts, b.explanation());
    }

    private static StrengthProgrammeEditor.Proposal proposal(
            StrengthProgramme old,
            List<StrengthProgramme.Block> blocks,
            LocalDate today,
            Instant now,
            Set<String> protectedIds,
            String reason) {
        LocalDate effective =
                Collections.max(Arrays.asList(today, old.effectiveFrom(), old.start()));
        if (effective.isAfter(old.end()) || now.isBefore(old.createdAt()))
            throw new IllegalArgumentException(
                    "Review the programme dates and device clock before creating a proposal.");
        var next =
                new StrengthProgramme(
                        old.id(),
                        Math.addExact(old.revision(), 1),
                        old.revision(),
                        now,
                        old.name(),
                        old.origin(),
                        old.templateId(),
                        old.settings(),
                        blocks,
                        effective,
                        reason,
                        old.tradeoffs());
        return new StrengthProgrammeEditor.Proposal(
                old, next, protectedIds, Collections.emptyList());
    }
}
