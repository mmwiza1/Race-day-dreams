package org.racedaydreams.domain;

import java.time.Duration;
import java.util.Objects;

/** Nullable prescriptions mean no guidance is available; HR is never required. */
public record WorkoutSegment(
        Kind kind, Double distanceKm, Duration duration, Double minimumSecondsPerKm,
        Double maximumSecondsPerKm, Integer heartRateZone, Double rpe, String guidance) {
    public enum Kind { WARM_UP, RUN, WALK, RECOVERY, COOL_DOWN }

    public WorkoutSegment {
        Objects.requireNonNull(kind, "kind");
        DomainChecks.nonNegative(distanceKm, "distance");
        DomainChecks.nonNegative(duration, "duration");
        DomainChecks.nonNegative(minimumSecondsPerKm, "minimum pace");
        DomainChecks.nonNegative(maximumSecondsPerKm, "maximum pace");
        if (minimumSecondsPerKm != null && maximumSecondsPerKm != null
                && minimumSecondsPerKm > maximumSecondsPerKm) {
            throw new IllegalArgumentException("Pace range must be ordered");
        }
        if (heartRateZone != null && (heartRateZone < 1 || heartRateZone > 5)) {
            throw new IllegalArgumentException("HR zone must be 1 to 5");
        }
        DomainChecks.rpe(rpe);
    }
}
