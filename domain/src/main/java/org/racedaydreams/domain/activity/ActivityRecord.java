package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/** Actual observations with provenance. Null values remain unavailable, never fabricated zeros. */
public record ActivityRecord(String id, SourceIdentity source, Instant start, Instant end,
        ZoneOffset startOffset, ZoneOffset endOffset, Double distanceMetres, Duration movingTime,
        Double averageHeartRate, Double maxHeartRate, Double elevationGainMetres, Double cadence,
        Double rpe, String notes, List<DistanceSample> samples, List<ActivityLap> laps,
        String plannedWorkoutId, String shoeId) {
    public ActivityRecord {
        id = ActivityChecks.required(id, "Activity ID");
        Objects.requireNonNull(source, "Activity source");
        Objects.requireNonNull(start, "Activity start");
        Objects.requireNonNull(end, "Activity end");
        if (!end.isAfter(start)) throw new IllegalArgumentException("Activity end must follow start.");
        ActivityChecks.nonNegative(distanceMetres, "Activity distance");
        ActivityChecks.duration(movingTime, "Moving duration");
        if (movingTime != null && movingTime.compareTo(Duration.between(start, end)) > 0) throw new IllegalArgumentException("Moving duration cannot exceed elapsed duration.");
        ActivityChecks.positive(averageHeartRate, "Average heart rate");
        ActivityChecks.positive(maxHeartRate, "Maximum heart rate");
        if (averageHeartRate != null && maxHeartRate != null && maxHeartRate < averageHeartRate) throw new IllegalArgumentException("Maximum heart rate cannot be below its average.");
        ActivityChecks.nonNegative(elevationGainMetres, "Elevation gain");
        ActivityChecks.nonNegative(cadence, "Cadence");
        if (rpe != null && (!Double.isFinite(rpe) || rpe < 0 || rpe > 10)) throw new IllegalArgumentException("RPE must be between 0 and 10.");
        samples = ActivityChecks.copy(samples);
        laps = ActivityChecks.copy(laps);
    }

    public Duration elapsedTime() { return Duration.between(start, end); }
}
