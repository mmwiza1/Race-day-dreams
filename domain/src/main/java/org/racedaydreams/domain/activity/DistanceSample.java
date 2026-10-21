package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.Objects;

/** Cumulative measured distance at elapsed time. discontinuity marks a pause/gap before this point. */
public record DistanceSample(Duration elapsed, double distanceMetres, Double heartRate,
                             Double cadence, Double elevationMetres, boolean discontinuity) {
    public DistanceSample {
        Objects.requireNonNull(elapsed, "Sample elapsed time");
        ActivityChecks.duration(elapsed, "Sample time");
        ActivityChecks.nonNegative(distanceMetres, "Sample distance");
        ActivityChecks.positive(heartRate, "Heart rate");
        ActivityChecks.nonNegative(cadence, "Cadence");
        if (elevationMetres != null && !Double.isFinite(elevationMetres)) throw new IllegalArgumentException("Elevation must be finite.");
    }
}
