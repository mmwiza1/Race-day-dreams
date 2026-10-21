package org.racedaydreams.domain.portability;

import java.time.Instant;
import java.util.Objects;

/** Provider-neutral recorded coordinates. Optional sensors remain absent, not zero-filled. */
public record TrackPoint(Instant time, double latitude, double longitude, Double altitudeMetres,
                         Double accuracyMetres, Double heartRate, Double cadence) {
    public TrackPoint {
        Objects.requireNonNull(time, "point time");
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid geographic coordinates.");
        }
        finite(altitudeMetres, false); finite(accuracyMetres, true);
        finite(heartRate, true); finite(cadence, true);
    }

    private static void finite(Double value, boolean nonNegative) {
        if (value != null && (!Double.isFinite(value) || (nonNegative && value < 0))) {
            throw new IllegalArgumentException("Invalid optional point measurement.");
        }
    }
}
