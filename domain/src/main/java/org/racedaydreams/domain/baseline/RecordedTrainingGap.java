package org.racedaydreams.domain.baseline;

import java.time.Instant;
import java.util.Objects;

/** A gap in records, not proof that the runner did not train. */
public record RecordedTrainingGap(Instant after, Instant before, long daysWithoutRecordedRun) {
    public RecordedTrainingGap {
        Objects.requireNonNull(after, "after"); Objects.requireNonNull(before, "before");
        if (!before.isAfter(after) || daysWithoutRecordedRun < 1) throw new IllegalArgumentException("Invalid recorded gap.");
    }
}
