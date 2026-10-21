package org.racedaydreams.domain.baseline;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A transparent snapshot of evidence. Missing values remain null rather than becoming zero. */
public record RunnerBaseline(Double weeklyDistanceKm, Double runsPerWeek, Double longestRunKm,
        Double typicalRunMinutes, Double easyPaceSecondsPerKm, Integer consistentWeeks,
        Duration recent5k, Duration recent10k, Duration recentHalf, int activityCount,
        int weeksWithRuns, Instant windowStart, Instant windowEnd, Instant lastActivityAt,
        BaselineConfidence confidence, Map<String, BaselineProvenance> provenance,
        List<String> explanations, List<WeeklyRunningSummary> weeklyHistory,
        List<RecordedTrainingGap> gaps, Instant manualRecordedAt) {
    public static final String WEEKLY_DISTANCE = "weeklyDistanceKm";
    public static final String RUNS_PER_WEEK = "runsPerWeek";
    public static final String LONGEST_RUN = "longestRunKm";
    public static final String TYPICAL_DURATION = "typicalRunMinutes";
    public static final String EASY_PACE = "easyPaceSecondsPerKm";
    public static final String CONSISTENT_WEEKS = "consistentWeeks";
    public static final String RECENT_5K = "recent5k";
    public static final String RECENT_10K = "recent10k";
    public static final String RECENT_HALF = "recentHalf";

    public RunnerBaseline(Double weeklyDistanceKm, Double runsPerWeek, Double longestRunKm,
            Double typicalRunMinutes, Double easyPaceSecondsPerKm, Integer consistentWeeks,
            Duration recent5k, Duration recent10k, Duration recentHalf, int activityCount,
            int weeksWithRuns, Instant windowStart, Instant windowEnd, Instant lastActivityAt,
            BaselineConfidence confidence, Map<String, BaselineProvenance> provenance,
            List<String> explanations, List<WeeklyRunningSummary> weeklyHistory, List<RecordedTrainingGap> gaps) {
        this(weeklyDistanceKm, runsPerWeek, longestRunKm, typicalRunMinutes, easyPaceSecondsPerKm,
                consistentWeeks, recent5k, recent10k, recentHalf, activityCount, weeksWithRuns,
                windowStart, windowEnd, lastActivityAt, confidence, provenance, explanations,
                weeklyHistory, gaps, null);
    }

    public RunnerBaseline {
        Objects.requireNonNull(windowStart, "windowStart"); Objects.requireNonNull(windowEnd, "windowEnd");
        if (!windowEnd.isAfter(windowStart)) throw new IllegalArgumentException("Baseline window must have positive length.");
        if (activityCount < 0 || weeksWithRuns < 0) throw new IllegalArgumentException("Evidence counts cannot be negative.");
        ManualBaselineInput.nonNegative(weeklyDistanceKm, "Weekly distance");
        ManualBaselineInput.nonNegative(runsPerWeek, "Runs per week");
        ManualBaselineInput.nonNegative(longestRunKm, "Longest run");
        ManualBaselineInput.nonNegative(typicalRunMinutes, "Typical duration");
        ManualBaselineInput.nonNegative(easyPaceSecondsPerKm, "Easy pace");
        if (consistentWeeks != null && consistentWeeks < 0) throw new IllegalArgumentException("Consistent weeks cannot be negative.");
        measuredTime(recent5k); measuredTime(recent10k); measuredTime(recentHalf);
        Objects.requireNonNull(confidence, "confidence");
        provenance = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(provenance, "provenance")));
        explanations = immutable(explanations); weeklyHistory = immutable(weeklyHistory); gaps = immutable(gaps);
    }

    private static <T> List<T> immutable(List<T> values) {
        ArrayList<T> copy = new ArrayList<>(Objects.requireNonNull(values, "values"));
        for (T value : copy) Objects.requireNonNull(value, "List values cannot be null.");
        return Collections.unmodifiableList(copy);
    }

    private static void measuredTime(Duration duration) {
        if (duration != null && (duration.isNegative() || duration.isZero() || duration.compareTo(Duration.ofHours(48)) > 0)) {
            throw new IllegalArgumentException("Performance duration must be positive and no more than 48 hours.");
        }
    }

    public static RunnerBaseline empty(Instant from, Instant to) {
        return new RunnerBaseline(null, null, null, null, null, null, null, null, null,
                0, 0, from, to, null, BaselineConfidence.LOW, Collections.emptyMap(),
                Collections.singletonList("No running history is available. You can continue with optional manual details or an assessment."),
                Collections.emptyList(), Collections.emptyList());
    }
}
