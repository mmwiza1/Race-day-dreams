package org.racedaydreams.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Actual observations, including custom runs with no plannedWorkoutId. Missing HR/RPE is valid. */
public record CompletedWorkout(String id, String plannedWorkoutId, Instant completedAt,
        Double distanceKm, Duration duration, Double averageHeartRate, Double rpe, String note) {
    public CompletedWorkout {
        id = DomainChecks.id(id, "completed workout id");
        if (plannedWorkoutId != null) DomainChecks.id(plannedWorkoutId, "planned workout id");
        Objects.requireNonNull(completedAt, "completedAt");
        DomainChecks.nonNegative(distanceKm, "distance");
        DomainChecks.nonNegative(duration, "duration");
        DomainChecks.nonNegative(averageHeartRate, "heart rate");
        DomainChecks.rpe(rpe);
    }
}
