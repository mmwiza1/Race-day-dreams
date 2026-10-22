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
    public AdjustmentProposal { evidence = org.racedaydreams.domain.coaching.Immutable.copyList(evidence); options = org.racedaydreams.domain.coaching.Immutable.copyList(options); }
}
