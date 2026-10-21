package org.racedaydreams.domain.baseline;

import java.time.Duration;

/** Optional answers fill gaps or explicitly correct an imported baseline. Null means unknown. */
public record ManualBaselineInput(Double weeklyDistanceKm, Double runsPerWeek, Double longestRunKm,
        Duration recent5k, Duration recent10k, Duration recentHalf, Double easyPaceSecondsPerKm,
        Integer consistentWeeks) {
    public ManualBaselineInput {
        nonNegative(weeklyDistanceKm, "Weekly distance");
        nonNegative(runsPerWeek, "Runs per week");
        if (runsPerWeek != null && runsPerWeek > 21) throw new IllegalArgumentException("Check the number of runs per week.");
        nonNegative(longestRunKm, "Longest recent run");
        positiveTime(recent5k); positiveTime(recent10k); positiveTime(recentHalf);
        if (easyPaceSecondsPerKm != null && (!Double.isFinite(easyPaceSecondsPerKm) || easyPaceSecondsPerKm <= 0)) {
            throw new IllegalArgumentException("Easy pace must be a positive time per kilometre.");
        }
        if (consistentWeeks != null && consistentWeeks < 0) throw new IllegalArgumentException("Consistent weeks cannot be negative.");
    }

    public static ManualBaselineInput empty() { return new ManualBaselineInput(null, null, null, null, null, null, null, null); }

    public boolean hasAnyValue() {
        return weeklyDistanceKm != null || runsPerWeek != null || longestRunKm != null || recent5k != null
                || recent10k != null || recentHalf != null || easyPaceSecondsPerKm != null || consistentWeeks != null;
    }

    static void nonNegative(Double value, String label) {
        if (value != null && (!Double.isFinite(value) || value < 0)) throw new IllegalArgumentException(label + " cannot be negative or non-finite.");
    }

    static void positiveTime(Duration value) {
        if (value != null && (value.isZero() || value.isNegative() || value.getNano() != 0 || value.compareTo(Duration.ofHours(48)) > 0)) {
            throw new IllegalArgumentException("Recent performance times must be positive whole seconds, up to 48 hours.");
        }
    }
}
