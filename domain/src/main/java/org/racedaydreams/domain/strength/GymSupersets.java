package org.racedaydreams.domain.strength;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Uses the existing frozen slot groups. Actual set evidence is never copied between movements. */
public final class GymSupersets {
    private GymSupersets() {}

    public record Next(String slotId, int round, boolean restBeforeNext, String instruction) {}

    public static List<GymWorkoutDefinition.Slot> group(
            List<GymWorkoutDefinition.Slot> slots, int from, int count, String groupId) {
        if (count < 2 || from < 0 || from + count > slots.size())
            throw new IllegalArgumentException("Choose at least two adjacent exercises.");
        GymWorkoutDefinition.requireId(groupId, "superset");
        var result = new ArrayList<>(slots);
        for (int i = from; i < from + count; i++) {
            var s = slots.get(i);
            if (s.supersetGroup() != null)
                throw new IllegalArgumentException("Break the existing group first.");
            result.set(i, new GymWorkoutDefinition.Slot(s.id(), s.exercise(), s.target(), groupId));
        }
        GymWorkoutDefinition.validateSlots(result, false);
        return List.copyOf(result);
    }

    public static List<GymWorkoutDefinition.Slot> ungroup(
            List<GymWorkoutDefinition.Slot> slots, String group) {
        var result =
                slots.stream()
                        .map(
                                s ->
                                        Objects.equals(s.supersetGroup(), group)
                                                ? new GymWorkoutDefinition.Slot(
                                                        s.id(), s.exercise(), s.target(), null)
                                                : s)
                        .toList();
        GymWorkoutDefinition.validateSlots(result, false);
        return result;
    }

    public static Next next(GymExecution execution) {
        var current =
                execution.slots().stream()
                        .filter(s -> s.id().equals(execution.selectedSlotId()))
                        .findFirst()
                        .orElse(null);
        if (current == null || current.supersetGroup() == null) return null;
        Set<String> skipped =
                execution.events().stream()
                        .filter(e -> e.type() == GymExecution.EventType.EXERCISE_SKIPPED)
                        .map(GymExecution.Event::slotId)
                        .collect(Collectors.toSet());
        var group =
                execution.slots().stream()
                        .filter(
                                s ->
                                        current.supersetGroup().equals(s.supersetGroup())
                                                && !skipped.contains(s.id()))
                        .toList();
        GymWorkoutDefinition.Slot chosen = null;
        int round = Integer.MAX_VALUE;
        for (var s : group) {
            int saved =
                    (int)
                            execution.actualSets().stream()
                                    .filter(a -> a.slotId().equals(s.id()) && !a.actual().warmUp())
                                    .count();
            // An open/substituted target still has one optional round, never fabricated sets.
            int target = s.target() == null ? 1 : s.target().sets();
            if (saved < target && saved < round) {
                chosen = s;
                round = saved;
            }
        }
        if (chosen == null) return null;
        boolean rest = round > 0 && group.indexOf(chosen) <= group.indexOf(current);
        return new Next(
                chosen.id(),
                round + 1,
                rest,
                (rest ? "Rest before the next round, then complete " : "Next, complete ")
                        + chosen.exercise().name()
                        + ". Record only what you actually perform.");
    }
}
