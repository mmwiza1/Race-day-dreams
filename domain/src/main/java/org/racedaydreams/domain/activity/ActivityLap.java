package org.racedaydreams.domain.activity;

import java.time.Instant;
import java.util.Objects;

/** A provider/user lap is distinct from an arbitrary kilometre/mile split. */
public record ActivityLap(String id, Instant start, Instant end, Double distanceMetres,
                          String label, String plannedSegmentId) {
    public ActivityLap {
        id = ActivityChecks.required(id, "Lap ID");
        Objects.requireNonNull(start, "Lap start");
        Objects.requireNonNull(end, "Lap end");
        if (!end.isAfter(start)) throw new IllegalArgumentException("Lap end must follow start.");
        ActivityChecks.nonNegative(distanceMetres, "Lap distance");
    }
}
