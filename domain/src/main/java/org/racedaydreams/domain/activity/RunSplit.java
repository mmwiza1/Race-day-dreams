package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.Objects;

/** Reproducibly derived distance split; averages use available recorded samples only. */
public record RunSplit(int number, double distanceMetres, Duration duration,
        Double averageHeartRate, Double elevationChangeMetres, Double cadence, boolean partial) {
    public RunSplit {
        if (number < 1 || !Double.isFinite(distanceMetres) || distanceMetres <= 0) throw new IllegalArgumentException("A positive split number and distance are required.");
        Objects.requireNonNull(duration, "Split duration");
        if (duration.isZero() || duration.isNegative()) throw new IllegalArgumentException("Split duration must be positive.");
        ActivityChecks.positive(averageHeartRate, "Split heart rate");
        ActivityChecks.nonNegative(cadence, "Split cadence");
        if (elevationChangeMetres != null && !Double.isFinite(elevationChangeMetres)) throw new IllegalArgumentException("Elevation change must be finite.");
    }
    public double secondsPerKilometre() { return ActivityChecks.seconds(duration) / (distanceMetres / 1_000.0); }
}
