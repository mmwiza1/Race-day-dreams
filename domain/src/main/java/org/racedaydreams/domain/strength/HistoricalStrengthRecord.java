package org.racedaydreams.domain.strength;
import java.time.LocalDate;
import java.util.Objects;
/** Independent historical evidence, not a fabricated completed workout. */
public record HistoricalStrengthRecord(String id, StrengthExercise exercise, double kilograms, int reps,
        LocalDate date, DatePrecision precision, String notes) {
    public enum DatePrecision { EXACT, APPROXIMATE, UNKNOWN }
    public HistoricalStrengthRecord {
        Objects.requireNonNull(exercise); Objects.requireNonNull(precision);
        if (id == null || id.trim().isEmpty() || !Double.isFinite(kilograms) || kilograms < 0 || kilograms > 1500 || reps < 1 || reps > 500
            || (precision == DatePrecision.UNKNOWN) != (date == null)) throw new IllegalArgumentException("Check historical record and date precision.");
    }
}
