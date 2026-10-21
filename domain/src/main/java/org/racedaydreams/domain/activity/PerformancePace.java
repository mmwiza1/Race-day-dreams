package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import org.racedaydreams.domain.UnitSystem;

public final class PerformancePace {
    private PerformancePace() {}
    public static String format(Duration duration, PerformanceDistance distance, UnitSystem units) {
        Objects.requireNonNull(duration, "Duration");
        Objects.requireNonNull(distance, "Distance");
        Objects.requireNonNull(units, "Units");
        if (duration.isZero() || duration.isNegative()) throw new IllegalArgumentException("A pace needs positive elapsed time.");
        double metresPerUnit = units == UnitSystem.IMPERIAL ? 1_609.344 : 1_000;
        long secondsPerUnit = Math.round(ActivityChecks.seconds(duration) * metresPerUnit / distance.metres());
        return String.format(Locale.ROOT, "%d:%02d/%s", secondsPerUnit / 60, secondsPerUnit % 60,
                units == UnitSystem.IMPERIAL ? "mi" : "km");
    }
}
