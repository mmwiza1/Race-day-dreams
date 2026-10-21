package org.racedaydreams.domain;

import java.time.Duration;

/** Optional contextual observations, never a diagnosis. Missing sleep/HR is a supported state. */
public record RecoveryMetrics(Duration sleep, Double restingHeartRate, Double fatigue,
        Double soreness, String notes) {
    public RecoveryMetrics {
        DomainChecks.nonNegative(sleep, "sleep");
        DomainChecks.nonNegative(restingHeartRate, "resting heart rate");
        DomainChecks.rpe(fatigue);
        DomainChecks.rpe(soreness);
    }
}
