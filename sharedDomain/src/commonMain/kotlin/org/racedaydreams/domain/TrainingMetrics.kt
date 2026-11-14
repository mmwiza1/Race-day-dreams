package org.racedaydreams.domain

/** Existing nullable observations: absence is never converted to a measured zero. */
data class TrainingMetrics(
    private val currentWeekKm: Double?,
    private val previousWeekKm: Double?,
    private val rollingFourWeekKm: Double?,
    private val longestRecentRunKm: Double?,
    private val runningDays: Int?,
    private val gymDays: Int?,
    private val restDays: Int?,
    private val hardSessions: Int?,
) {
    init {
        nonNegative(currentWeekKm, "current weekly distance")
        nonNegative(previousWeekKm, "previous weekly distance")
        nonNegative(rollingFourWeekKm, "four week distance")
        nonNegative(longestRecentRunKm, "longest run")
        checkDays(runningDays)
        checkDays(gymDays)
        checkDays(restDays)
        require(hardSessions == null || hardSessions >= 0) {
            "Hard session count cannot be negative"
        }
    }

    fun currentWeekKm(): Double? = currentWeekKm

    fun previousWeekKm(): Double? = previousWeekKm

    fun rollingFourWeekKm(): Double? = rollingFourWeekKm

    fun longestRecentRunKm(): Double? = longestRecentRunKm

    fun runningDays(): Int? = runningDays

    fun gymDays(): Int? = gymDays

    fun restDays(): Int? = restDays

    fun hardSessions(): Int? = hardSessions

    override fun toString(): String =
        "TrainingMetrics[currentWeekKm=$currentWeekKm, previousWeekKm=$previousWeekKm, rollingFourWeekKm=$rollingFourWeekKm, longestRecentRunKm=$longestRecentRunKm, runningDays=$runningDays, gymDays=$gymDays, restDays=$restDays, hardSessions=$hardSessions]"

    private fun nonNegative(value: Double?, name: String) {
        require(value == null || value.isFinite() && value >= 0) {
            "$name must be finite and non-negative"
        }
    }

    private fun checkDays(days: Int?) {
        require(days == null || days in 0..7) { "Days must be 0 to 7" }
    }
}
