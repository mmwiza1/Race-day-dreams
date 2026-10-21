package org.racedaydreams.domain.reminders;

import java.util.Objects;
import org.racedaydreams.domain.PlannedWorkout;

/** A current prescription and version; the planned prescription never contains actual results. */
public record ReminderPlanEntry(PlannedWorkout workout, long revision, Status status) {
    public enum Status { PLANNED, COMPLETED, CANCELLED, SKIPPED }

    public ReminderPlanEntry {
        Objects.requireNonNull(workout, "workout");
        Objects.requireNonNull(status, "workout status");
        if (revision < 1) throw new IllegalArgumentException("A workout revision must be positive.");
    }
}
