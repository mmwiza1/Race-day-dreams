package org.racedaydreams.domain;

import java.time.LocalDate;
import java.util.Objects;

/** Inclusive phase dates, allocated deterministically from the actual horizon and runner evidence. */
public record TrainingPhase(Kind kind, LocalDate startDate, LocalDate endDate, String explanation) {
    public enum Kind { FOUNDATION, ENDURANCE_DEVELOPMENT, RACE_SPECIFIC, PEAK, TAPER, RACE }

    public TrainingPhase {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("Phase end precedes start");
    }
}
