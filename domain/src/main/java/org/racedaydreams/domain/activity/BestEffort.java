package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Candidate calculated from samples. Only QUALIFIED candidates may become automatic PB evidence. */
public record BestEffort(PerformanceDistance distance, Duration duration, Duration startElapsed,
                         Duration endElapsed, SampleQuality quality, List<String> reasons) {
    public BestEffort {
        Objects.requireNonNull(distance, "Effort distance");
        Objects.requireNonNull(duration, "Effort duration");
        Objects.requireNonNull(startElapsed, "Effort start");
        Objects.requireNonNull(endElapsed, "Effort end");
        Objects.requireNonNull(quality, "Effort quality");
        if (duration.isZero() || duration.isNegative() || startElapsed.isNegative() || !endElapsed.minus(startElapsed).equals(duration)) throw new IllegalArgumentException("Effort times must be positive and consistent.");
        reasons = ActivityChecks.copy(reasons);
    }
}
