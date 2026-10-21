package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

/** Official/manual results are retained independently of any linked GPS activity. */
public record RecordedResult(String id, PerformanceDistance distance, Duration duration,
        LocalDate date, String eventName, ResultSource source, String activityId, String note) {
    public RecordedResult {
        id = ActivityChecks.required(id, "Result ID");
        Objects.requireNonNull(distance, "Result distance");
        Objects.requireNonNull(duration, "Result duration");
        if (duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("Result time must be positive.");
        Objects.requireNonNull(date, "Result date");
        Objects.requireNonNull(source, "Result source");
        if (source == ResultSource.ROLLING && (activityId == null || activityId.trim().isEmpty())) throw new IllegalArgumentException("Rolling efforts must link an activity.");
    }
}
