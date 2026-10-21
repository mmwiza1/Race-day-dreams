package org.racedaydreams.domain;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Pure, deterministic time/pace conversions. No pace means no invented estimate. */
public final class PaceCalculator {
    private static final Pattern TIME = Pattern.compile("[0-9]{1,4}:[0-5][0-9](?::[0-5][0-9])?");
    private static final Duration MAX_TARGET = Duration.ofHours(48);
    private static final double KILOMETRES_PER_MILE = 1.609344;

    private PaceCalculator() { }

    public static Duration parseTime(String value) {
        if (value == null || !TIME.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Enter a time as H:MM:SS or MM:SS, for example 1:59:59 or 27:00.");
        }
        String[] parts = value.trim().split(":");
        long seconds = parts.length == 3
                ? Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2])
                : Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
        Duration duration = Duration.ofSeconds(seconds);
        requireTargetTime(duration);
        return duration;
    }

    public static String formatTime(Duration duration) {
        Objects.requireNonNull(duration, "A duration is required.");
        if (duration.isNegative()) throw new IllegalArgumentException("A duration cannot be negative.");
        long seconds = duration.getSeconds();
        return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    /** Rounded to the nearest second per chosen unit, with carries handled before formatting. */
    public static String targetPace(Duration time, RaceDistance distance, UnitSystem units) {
        requireTargetTime(time);
        Objects.requireNonNull(distance, "A race distance is required.");
        Objects.requireNonNull(units, "Display units are required.");
        if (distance == RaceDistance.GENERAL_FITNESS) {
            throw new IllegalArgumentException("General fitness has no target race pace.");
        }
        double kilometres = distance.kilometres();
        double unitsInRace = units == UnitSystem.METRIC ? kilometres : kilometres / KILOMETRES_PER_MILE;
        double seconds = time.getSeconds() + time.getNano() / 1_000_000_000.0;
        long secondsPerUnit = Math.round(seconds / unitsInRace);
        return String.format(Locale.ROOT, "%d:%02d/%s", secondsPerUnit / 60, secondsPerUnit % 60,
                units == UnitSystem.METRIC ? "km" : "mi");
    }

    static void requireTargetTime(Duration time) {
        if (time == null || time.isNegative() || time.isZero() || time.compareTo(MAX_TARGET) > 0) {
            throw new IllegalArgumentException("Target time must be greater than zero and no more than 48 hours.");
        }
        if (time.getNano() != 0) {
            throw new IllegalArgumentException("Enter target time in whole seconds.");
        }
    }
}
