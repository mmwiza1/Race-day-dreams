package org.racedaydreams.domain

/** Legacy eligibility is retained evidence, not authority to prescribe performance training. */
data class UserProfile(
    private val raceGoal: RaceGoal?,
    private val preferences: TrainingPreferences?,
    private val performanceEligible: Boolean,
) {
    init {
        if (raceGoal == null) throw NullPointerException("Your goal is required.")
        if (preferences == null)
            throw NullPointerException("Your training preferences are required.")
    }

    fun raceGoal(): RaceGoal = raceGoal!!

    fun preferences(): TrainingPreferences = preferences!!

    fun performanceEligible(): Boolean = performanceEligible

    override fun toString(): String =
        "UserProfile[raceGoal=$raceGoal, preferences=$preferences, performanceEligible=$performanceEligible]"
}
