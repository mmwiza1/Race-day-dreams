package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.Objects;
import org.racedaydreams.domain.UnitSystem;

/** One actual improvement in retained evidence; previousBest is null for first recorded evidence. */
public record PersonalBestEntry(RecordedResult result, RecordedResult previousBest, Duration improvement) {
    public PersonalBestEntry {
        Objects.requireNonNull(result, "PB result");
        if ((previousBest == null) != (improvement == null)) throw new IllegalArgumentException("First PBs have no invented previous time or improvement.");
        if (improvement != null && (improvement.isNegative() || improvement.isZero())) throw new IllegalArgumentException("A PB improvement must be positive.");
    }
    public String pace(UnitSystem units) { return PerformancePace.format(result.duration(), result.distance(), units); }
}
