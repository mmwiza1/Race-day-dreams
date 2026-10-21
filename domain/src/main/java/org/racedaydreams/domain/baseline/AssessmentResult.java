package org.racedaydreams.domain.baseline;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** A completed assessment entered by the runner or linked to an actual activity. */
public record AssessmentResult(String id, BaselineAssessment assessment, Duration duration,
                               Double rpe, Instant completedAt, String activityId) {
    public AssessmentResult {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Assessment ID is required.");
        Objects.requireNonNull(assessment, "assessment"); Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(completedAt, "completedAt");
        if (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofHours(48)) > 0) throw new IllegalArgumentException("Enter a positive assessment time up to 48 hours.");
        if (rpe != null && (!Double.isFinite(rpe) || rpe < 0 || rpe > 10)) throw new IllegalArgumentException("Assessment RPE must be between 0 and 10.");
    }
}
