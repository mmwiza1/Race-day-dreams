package org.racedaydreams.domain.baseline;

import java.time.LocalDate;
import java.util.Objects;

/** Local Monday week. Null distance means at least one recorded run lacked distance. */
public record WeeklyRunningSummary(LocalDate weekStarting, int recordedRuns, Double distanceKm,
                                   boolean completeWindowWeek) {
    public WeeklyRunningSummary {
        Objects.requireNonNull(weekStarting, "weekStarting");
        if (recordedRuns < 1) throw new IllegalArgumentException("Only observed weeks are summaries.");
        ManualBaselineInput.nonNegative(distanceKm, "Recorded distance");
    }
}
