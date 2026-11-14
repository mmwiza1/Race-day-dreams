package org.racedaydreams.domain

import org.racedaydreams.domain.time.LocalDate

/**
 * Existing onboarding rule only; retained historical goals remain readable in record constructors.
 */
object GoalValidation {
    fun validateRaceGoal(goal: RaceGoal, localToday: LocalDate) {
        val date = goal.raceDate()
        require(date == null || date.toEpochDay() >= localToday.toEpochDay()) {
            "Choose today or a future race date."
        }
    }
}
