package org.racedaydreams.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** The prescription is preserved separately from CompletedWorkout, including when overridden. */
public record PlannedWorkout(String id, LocalDate date, WorkoutCategory category,
        Double distanceKm, Duration duration, List<WorkoutSegment> segments, String note) {
    public PlannedWorkout {
        id = DomainChecks.id(id, "workout id");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(category, "category");
        DomainChecks.nonNegative(distanceKm, "distance");
        DomainChecks.nonNegative(duration, "duration");
        segments = DomainChecks.immutableList(segments);
    }
}
