package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ActivityChecks {
    private ActivityChecks() {}
    static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required.");
        return value;
    }
    static void nonNegative(Double value, String name) {
        if (value != null && (!Double.isFinite(value) || value < 0)) throw new IllegalArgumentException(name + " must be finite and non-negative.");
    }
    static void positive(Double value, String name) {
        if (value != null && (!Double.isFinite(value) || value <= 0)) throw new IllegalArgumentException(name + " must be positive when available.");
    }
    static void duration(Duration value, String name) {
        if (value != null && value.isNegative()) throw new IllegalArgumentException(name + " cannot be negative.");
    }
    static <T> List<T> copy(List<T> values) {
        Objects.requireNonNull(values, "A list is required.");
        List<T> result = new ArrayList<>(values.size());
        for (T value : values) result.add(Objects.requireNonNull(value, "Null list entries are not supported."));
        return Collections.unmodifiableList(result);
    }
    static double seconds(Duration duration) { return duration.getSeconds() + duration.getNano() / 1_000_000_000.0; }
    static Duration duration(double seconds) { return Duration.ofNanos(Math.round(seconds * 1_000_000_000.0)); }
}
