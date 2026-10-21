package org.racedaydreams.domain;

import java.util.List;

/** An offered choice, not a command. Keeping the original must remain a possible action. */
public record AdjustmentOption(String id, String label, Action action,
        List<PlannedWorkout> proposedWorkouts, String explanation) {
    public enum Action { ACCEPT_RECOMMENDATION, KEEP_ORIGINAL, MOVE_WORKOUT, EASY_RUN, REST, CUSTOMISE }

    public AdjustmentOption {
        id = DomainChecks.id(id, "option id");
        label = DomainChecks.id(label, "option label");
        if (action == null) throw new IllegalArgumentException("Option action is required");
        proposedWorkouts = DomainChecks.immutableList(proposedWorkouts);
        explanation = DomainChecks.id(explanation, "option explanation");
    }
}
