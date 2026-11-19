package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class WeeklyRunningSummary(
    private val weekStarting: LocalDate?,
    private val recordedRuns: Int,
    private val distanceKm: Double?,
    private val completeWindowWeek: Boolean,
) {
    init {
        if (weekStarting == null) throw NullPointerException("weekStarting")
        require(recordedRuns >= 1) { "Only observed weeks are summaries." }
        ManualBaselineInput.nonNegative(distanceKm, "Recorded distance")
    }

    fun weekStarting(): LocalDate = weekStarting!!

    fun recordedRuns(): Int = recordedRuns

    fun distanceKm(): Double? = distanceKm

    fun completeWindowWeek(): Boolean = completeWindowWeek

    override fun toString(): String =
        "WeeklyRunningSummary[weekStarting=$weekStarting, recordedRuns=$recordedRuns, distanceKm=$distanceKm, completeWindowWeek=$completeWindowWeek]"
}
