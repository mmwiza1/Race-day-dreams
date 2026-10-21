package org.racedaydreams.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Proposed adaptation only. A runner's explicit choice is required before later application. */
public record PlanAdjustment(String id, String planId, Instant proposedAt, String recommendation,
        String reason, List<AdjustmentOption> options, String selectedOptionId) {
    public PlanAdjustment {
        id = DomainChecks.id(id, "adjustment id");
        planId = DomainChecks.id(planId, "plan id");
        Objects.requireNonNull(proposedAt, "proposedAt");
        recommendation = DomainChecks.id(recommendation, "recommendation");
        reason = DomainChecks.id(reason, "recommendation reason");
        options = DomainChecks.immutableList(options);
        if (options.size() < 2 || options.stream().noneMatch(option -> option.action() == AdjustmentOption.Action.KEEP_ORIGINAL)) {
            throw new IllegalArgumentException("Adjustments must offer choices including keeping the original");
        }
        HashSet<String> ids = new HashSet<>();
        for (AdjustmentOption option : options) {
            if (!ids.add(option.id())) throw new IllegalArgumentException("Option ids must be unique");
        }
        if (selectedOptionId != null && !ids.contains(selectedOptionId)) {
            throw new IllegalArgumentException("Selected option is not among the offered choices");
        }
    }
}
