package org.racedaydreams.domain;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Shared invariants for future training records. No training recommendations are calculated here. */
final class DomainChecks {
    private DomainChecks() { }

    static String id(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    /** Android API 28-compatible immutable defensive copy, rejecting null members. */
    static <T> List<T> immutableList(List<T> items) {
        Objects.requireNonNull(items, "A list is required");
        ArrayList<T> copy = new ArrayList<>(items.size());
        for (T item : items) copy.add(Objects.requireNonNull(item, "List members cannot be null"));
        return Collections.unmodifiableList(copy);
    }

    static void nonNegative(Double value, String name) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }

    static void nonNegative(Duration value, String name) {
        if (value != null && value.isNegative()) throw new IllegalArgumentException(name + " cannot be negative");
    }

    static void rpe(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0 || value > 10)) {
            throw new IllegalArgumentException("RPE must be between 0 and 10");
        }
    }
}
