package org.racedaydreams.domain;

import java.util.Objects;

/**
 * No name, account, email address or device identifier is needed to use the app.
 *
 * <p>{@code performanceEligible} is a legacy compatibility snapshot, not current
 * permission to prescribe performance training. Eligibility is derived from current
 * evidence by the eligibility evaluator. A change in that evidence must not erase
 * or make it impossible to load the runner's saved strategy preference.
 */
public record UserProfile(RaceGoal raceGoal, TrainingPreferences preferences, boolean performanceEligible) {
    public UserProfile {
        Objects.requireNonNull(raceGoal, "Your goal is required.");
        Objects.requireNonNull(preferences, "Your training preferences are required.");
    }
}
