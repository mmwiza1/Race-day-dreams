package org.racedaydreams.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Future estimate range, with evidence and limitations. No estimate is fabricated in Phase 1. */
public record RacePrediction(RaceDistance distance, Duration lowerFinishTime,
        Duration upperFinishTime, Instant assessedAt, List<String> evidence, String limitingFactor) {
    public RacePrediction {
        Objects.requireNonNull(distance, "distance");
        Objects.requireNonNull(lowerFinishTime, "lowerFinishTime");
        Objects.requireNonNull(upperFinishTime, "upperFinishTime");
        Objects.requireNonNull(assessedAt, "assessedAt");
        if (lowerFinishTime.isNegative() || lowerFinishTime.isZero()
                || upperFinishTime.compareTo(lowerFinishTime) < 0) {
            throw new IllegalArgumentException("Prediction must be a positive ordered range");
        }
        evidence = DomainChecks.immutableList(evidence);
        limitingFactor = DomainChecks.id(limitingFactor, "limiting factor explanation");
    }
}
