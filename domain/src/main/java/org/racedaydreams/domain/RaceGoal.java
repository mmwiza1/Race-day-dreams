package org.racedaydreams.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

/** A runner-owned goal. A null race date means no specific event is selected. */
public record RaceGoal(RaceDistance distance, String raceName, LocalDate raceDate,
                       GoalMode mode, Duration targetTime) {
    public RaceGoal {
        Objects.requireNonNull(distance, "Choose a goal distance.");
        Objects.requireNonNull(mode, "Choose a goal mode.");
        raceName = raceName == null ? "" : raceName.trim();
        if (raceName.length() > 200) {
            throw new IllegalArgumentException("Race name must be 200 characters or fewer.");
        }
        if (raceDate == null && !raceName.isEmpty()) {
            throw new IllegalArgumentException("Choose a race date for your named event.");
        }
        if (distance == RaceDistance.GENERAL_FITNESS && (raceDate != null || !raceName.isEmpty())) {
            throw new IllegalArgumentException("General fitness does not require a race event. Choose a race distance to add an event.");
        }
        if (mode == GoalMode.TARGET_TIME) {
            if (distance == RaceDistance.GENERAL_FITNESS) {
                throw new IllegalArgumentException("A target finishing time needs a race distance.");
            }
            PaceCalculator.requireTargetTime(targetTime);
        } else if (targetTime != null) {
            throw new IllegalArgumentException("A finishing time belongs only to the Target Time goal mode.");
        }
    }
}
