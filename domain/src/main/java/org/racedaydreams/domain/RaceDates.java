package org.racedaydreams.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class RaceDates {
    private RaceDates() { }

    /** Past events have zero remaining days, so completed goals remain readable. */
    public static long daysUntil(LocalDate date, Clock clock) {
        Objects.requireNonNull(date, "A race date is required.");
        Objects.requireNonNull(clock, "A clock is required.");
        return Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(clock), date));
    }

    /** Whole weeks remaining; a race tomorrow has zero whole weeks remaining. */
    public static long weeksUntil(LocalDate date, Clock clock) {
        return daysUntil(date, clock) / 7;
    }
}
