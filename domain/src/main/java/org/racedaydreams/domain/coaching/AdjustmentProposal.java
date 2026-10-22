package org.racedaydreams.domain.coaching;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.racedaydreams.domain.PlannedWorkout;

/** Null replacement means rest; KEEP explicitly preserves the original workout. */
public record AdjustmentProposal(String id, String baseVersionId, String workoutId, Instant createdAt,
        String reason, List<String> evidence, String impact, String confidence, List<Option> options) {
    public enum Action { KEEP, REPLACE, REST }
    public record Option(String id, String label, Action action, PlannedWorkout replacement) {
        public Option {
            Objects.requireNonNull(id); Objects.requireNonNull(label); Objects.requireNonNull(action);
            if ((action == Action.REPLACE) != (replacement != null)) throw new IllegalArgumentException("Replacement does not match its action.");
        }
    }
    public AdjustmentProposal {
        if (id == null || id.trim().isEmpty() || baseVersionId == null || workoutId == null || createdAt == null || reason == null || impact == null || confidence == null)
            throw new IllegalArgumentException("Proposal identity, reasoning and timestamps are required.");
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (var option : options) {
            if (!ids.add(option.id()) || option.replacement() != null && !option.replacement().id().equals(workoutId))
                throw new IllegalArgumentException("Proposal alternatives need unique IDs and must preserve workout identity.");
        }
        if (options.stream().noneMatch(o -> o.action() == Action.KEEP)) throw new IllegalArgumentException("The runner must be allowed to keep the original.");
        evidence = org.racedaydreams.domain.coaching.Immutable.copyList(evidence); options = org.racedaydreams.domain.coaching.Immutable.copyList(options); }
}
