package org.racedaydreams.domain;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Running availability and gym availability are independent and may overlap. */
public record TrainingPreferences(TrainingStrategy strategy, Set<DayOfWeek> runningDays,
                                  int runsPerWeek, DayOfWeek longRunDay, Set<DayOfWeek> gymDays,
                                  ScheduleType scheduleType, UnitSystem units) {
    public TrainingPreferences {
        Objects.requireNonNull(strategy, "Choose a training strategy.");
        Objects.requireNonNull(scheduleType, "Choose a schedule preference.");
        Objects.requireNonNull(units, "Choose display units.");
        runningDays = immutableDays(runningDays, "Running days are required.");
        gymDays = immutableDays(gymDays, "Gym days are required (an empty set means no gym days).");
        if (runsPerWeek < 1 || runsPerWeek > 7) {
            throw new IllegalArgumentException("Choose between 1 and 7 runs per week.");
        }
        if (runsPerWeek > runningDays.size()) {
            throw new IllegalArgumentException("Choose at least as many preferred running days as runs per week.");
        }
        if (longRunDay != null && !runningDays.contains(longRunDay)) {
            throw new IllegalArgumentException("Your preferred long-run day must be a preferred running day.");
        }
    }

    private static Set<DayOfWeek> immutableDays(Set<DayOfWeek> days, String message) {
        Objects.requireNonNull(days, message);
        EnumSet<DayOfWeek> copy = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek day : days) copy.add(Objects.requireNonNull(day, "Days cannot contain null."));
        return Collections.unmodifiableSet(copy);
    }
}
