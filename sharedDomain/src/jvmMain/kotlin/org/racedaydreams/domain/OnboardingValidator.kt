package org.racedaydreams.domain

import java.time.Clock
import java.time.LocalDate

/** Original clock/zone API; the rule shared with iOS uses the explicitly selected local date. */
object OnboardingValidator {
    @JvmStatic
    fun validate(profile: UserProfile?, clock: Clock?) {
        val current = profile ?: throw NullPointerException("Your profile is required.")
        validateRaceGoal(current.raceGoal(), clock)
    }

    @JvmStatic
    fun validateRaceGoal(goal: RaceGoal?, clock: Clock?) {
        val current = goal ?: throw NullPointerException("Your goal is required.")
        val source = clock ?: throw NullPointerException("A clock is required.")
        GoalValidation.validateRaceGoal(current, LocalDate.now(source))
    }
}
