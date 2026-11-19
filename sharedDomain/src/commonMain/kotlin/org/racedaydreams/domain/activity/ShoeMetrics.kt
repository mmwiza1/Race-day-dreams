package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained summary; absent evidence remains unknown. */
data class ShoeMetrics(
    private val shoeId: String?,
    private val recordedDistanceMetres: Double,
    private val totalDistanceMetres: Double?,
    private val runCount: Int,
    private val runsWithKnownDistance: Int,
    private val ageDays: Long?,
    private val longestRunMetres: Double?,
    private val averageRunDistanceMetres: Double?,
    private val firstRecordedRun: Instant?,
    private val wearGuidance: String?,
) {
    fun shoeId(): String? = shoeId

    fun recordedDistanceMetres(): Double = recordedDistanceMetres

    fun totalDistanceMetres(): Double? = totalDistanceMetres

    fun runCount(): Int = runCount

    fun runsWithKnownDistance(): Int = runsWithKnownDistance

    fun ageDays(): Long? = ageDays

    fun longestRunMetres(): Double? = longestRunMetres

    fun averageRunDistanceMetres(): Double? = averageRunDistanceMetres

    fun firstRecordedRun(): Instant? = firstRecordedRun

    fun wearGuidance(): String? = wearGuidance

    override fun toString(): String =
        "ShoeMetrics[shoeId=$shoeId, recordedDistanceMetres=$recordedDistanceMetres, totalDistanceMetres=$totalDistanceMetres, runCount=$runCount, runsWithKnownDistance=$runsWithKnownDistance, ageDays=$ageDays, longestRunMetres=$longestRunMetres, averageRunDistanceMetres=$averageRunDistanceMetres, firstRecordedRun=$firstRecordedRun, wearGuidance=$wearGuidance]"
}
