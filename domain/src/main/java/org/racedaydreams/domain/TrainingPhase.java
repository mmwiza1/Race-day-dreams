package org.racedaydreams.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Inclusive phase dates. Allocation is intentionally pending the training engine phase. */
public record TrainingPhase(Kind kind, LocalDate startDate, LocalDate endDate, String explanation) {
    public enum Kind { FOUNDATION, ENDURANCE_DEVELOPMENT, RACE_SPECIFIC, PEAK, TAPER, RACE }

    public TrainingPhase {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("Phase end precedes start");
    }
}
