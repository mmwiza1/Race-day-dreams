package org.racedaydreams.domain

import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.LocalDate

/** Runner-owned goal; no race date still means no event is selected. */
data class RaceGoal(
    private val distance: RaceDistance?,
    private var raceName: String?,
    private val raceDate: LocalDate?,
    private val mode: GoalMode?,
    private val targetTime: Duration?,
) {
    init {
        if (distance == null) throw NullPointerException("Choose a goal distance.")
        if (mode == null) throw NullPointerException("Choose a goal mode.")
        raceName = raceName?.trim { it <= ' ' } ?: ""
        require(raceName!!.length <= 200) { "Race name must be 200 characters or fewer." }
        require(raceDate != null || raceName!!.isEmpty()) {
            "Choose a race date for your named event."
        }
        require(
            distance != RaceDistance.GENERAL_FITNESS || raceDate == null && raceName!!.isEmpty()
        ) {
            "General fitness does not require a race event. Choose a race distance to add an event."
        }
        if (mode == GoalMode.TARGET_TIME) {
            require(distance != RaceDistance.GENERAL_FITNESS) {
                "A target finishing time needs a race distance."
            }
            PaceCalculator.requireTargetTime(targetTime)
        } else
            require(targetTime == null) {
                "A finishing time belongs only to the Target Time goal mode."
            }
    }

    fun distance(): RaceDistance = distance!!

    fun raceName(): String = raceName!!

    fun raceDate(): LocalDate? = raceDate

    fun mode(): GoalMode = mode!!

    fun targetTime(): Duration? = targetTime

    override fun toString(): String =
        "RaceGoal[distance=$distance, raceName=$raceName, raceDate=$raceDate, mode=$mode, targetTime=$targetTime]"
}
