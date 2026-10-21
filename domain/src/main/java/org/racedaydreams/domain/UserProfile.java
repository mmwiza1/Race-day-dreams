package org.racedaydreams.domain;

import java.util.Objects;

/** No name, account, email address or device identifier is needed to use the app. */
public record UserProfile(RaceGoal raceGoal, TrainingPreferences preferences, boolean performanceEligible) {
    public UserProfile {
        Objects.requireNonNull(raceGoal, "Your goal is required.");
        Objects.requireNonNull(preferences, "Your training preferences are required.");
        if (preferences.strategy() == TrainingStrategy.PERFORMANCE && !performanceEligible) {
            throw new IllegalArgumentException("Performance strategy needs supporting training history. Choose another strategy for now.");
        }
    }
}
