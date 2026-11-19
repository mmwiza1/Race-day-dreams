package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained summary; absent evidence remains unknown. */
data class RecordedWeek(
    private val weekStart: LocalDate?,
    private val distanceMetres: Double?,
    private val activityCount: Int,
    private val runningDays: Int,
    private val missingDistanceCount: Int,
    private val longestRunMetres: Double?,
) {
    fun weekStart(): LocalDate? = weekStart

    fun distanceMetres(): Double? = distanceMetres

    fun activityCount(): Int = activityCount

    fun runningDays(): Int = runningDays

    fun missingDistanceCount(): Int = missingDistanceCount

    fun longestRunMetres(): Double? = longestRunMetres

    override fun toString(): String =
        "RecordedWeek[weekStart=$weekStart, distanceMetres=$distanceMetres, activityCount=$activityCount, runningDays=$runningDays, missingDistanceCount=$missingDistanceCount, longestRunMetres=$longestRunMetres]"
}
