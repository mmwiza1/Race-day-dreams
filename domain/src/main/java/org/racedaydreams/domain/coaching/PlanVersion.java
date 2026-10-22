package org.racedaydreams.domain.coaching;

import java.time.Instant;
import java.util.Objects;
import org.racedaydreams.domain.*;

/** Immutable goal/evidence snapshot; the current-version pointer is storage's single authority. */
public record PlanVersion(String id, String planId, int version, Instant createdAt, String reason,
        UserProfile goalSnapshot, String evidenceSnapshot, TrainingPlan plan) {
    public PlanVersion {
        if (id == null || id.isBlank() || planId == null || planId.isBlank() || version < 1)
            throw new IllegalArgumentException("Check plan identity/version.");
        Objects.requireNonNull(createdAt); Objects.requireNonNull(reason); Objects.requireNonNull(goalSnapshot);
        Objects.requireNonNull(evidenceSnapshot); Objects.requireNonNull(plan);
        if (!planId.equals(plan.id())) throw new IllegalArgumentException("Plan identity differs from version identity.");
    }
}
