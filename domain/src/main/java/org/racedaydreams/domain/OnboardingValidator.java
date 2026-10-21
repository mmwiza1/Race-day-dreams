package org.racedaydreams.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/** Time-dependent input validation stays outside views and record constructors. */
public final class OnboardingValidator {
    private OnboardingValidator() { }

    public static void validate(UserProfile profile, Clock clock) {
        Objects.requireNonNull(profile, "Your profile is required.");
        validateRaceGoal(profile.raceGoal(), clock);
    }

    public static void validateRaceGoal(RaceGoal goal, Clock clock) {
        Objects.requireNonNull(goal, "Your goal is required.");
        Objects.requireNonNull(clock, "A clock is required.");
        LocalDate date = goal.raceDate();
        if (date != null && date.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Choose today or a future race date.");
        }
    }
}
