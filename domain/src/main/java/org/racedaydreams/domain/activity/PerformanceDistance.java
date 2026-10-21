package org.racedaydreams.domain.activity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Extensible distance value; persistence stores code plus metres rather than a closed enum. */
public record PerformanceDistance(String code, double metres, String label) {
    public static final PerformanceDistance ONE_K = new PerformanceDistance("1k", 1_000, "1K");
    public static final PerformanceDistance ONE_MILE = new PerformanceDistance("mile", 1_609.344, "1 mile");
    public static final PerformanceDistance TWO_K = new PerformanceDistance("2k", 2_000, "2K");
    public static final PerformanceDistance THREE_K = new PerformanceDistance("3k", 3_000, "3K");
    public static final PerformanceDistance FIVE_K = new PerformanceDistance("5k", 5_000, "5K");
    public static final PerformanceDistance TEN_K = new PerformanceDistance("10k", 10_000, "10K");
    public static final PerformanceDistance HALF_MARATHON = new PerformanceDistance("half_marathon", 21_097.5, "Half Marathon");
    public static final PerformanceDistance MARATHON = new PerformanceDistance("marathon", 42_195, "Marathon");
    public static final List<PerformanceDistance> CORE = Collections.unmodifiableList(Arrays.asList(
            ONE_K, ONE_MILE, TWO_K, THREE_K, FIVE_K, TEN_K, HALF_MARATHON, MARATHON));

    public PerformanceDistance {
        code = ActivityChecks.required(code, "Distance code");
        if (!code.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Distance code must contain lowercase letters, numbers, underscores or hyphens.");
        label = ActivityChecks.required(label, "Distance label");
        if (!Double.isFinite(metres) || metres <= 0) throw new IllegalArgumentException("Performance distance must be positive.");
    }
}
