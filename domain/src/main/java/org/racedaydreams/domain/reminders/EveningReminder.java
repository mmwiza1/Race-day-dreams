package org.racedaydreams.domain.reminders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** A schedule proposal, revalidated against the current plan when delivery is attempted. */
public record EveningReminder(String workoutId, long revision, LocalDate workoutDate,
                              Instant triggerAt, String title, String message) {
    public EveningReminder {
        Objects.requireNonNull(workoutId, "workout id");
        Objects.requireNonNull(workoutDate, "workout date");
        Objects.requireNonNull(triggerAt, "trigger time");
        Objects.requireNonNull(title, "notification title");
        Objects.requireNonNull(message, "notification message");
        if (workoutId.trim().isEmpty() || revision < 1) throw new IllegalArgumentException("Invalid reminder identity.");
    }
}
