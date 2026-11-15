package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class AssessmentSummary(
    private val averagePaceSecondsPerKm: Double,
    private val firstKilometrePace: Double?,
    private val finalKilometrePace: Double?,
    private val paceFadePercent: Double?,
    private val averageHeartRate: Double?,
    private val cadence: Double?,
    private val confidence: BaselineConfidence?,
    private val evidenceWeight: Double,
    private var findings: List<String>?,
) {
    init {
        findings = baselineListSnapshot(findings ?: throw NullPointerException())
    }

    fun averagePaceSecondsPerKm(): Double = averagePaceSecondsPerKm

    fun firstKilometrePace(): Double? = firstKilometrePace

    fun finalKilometrePace(): Double? = finalKilometrePace

    fun paceFadePercent(): Double? = paceFadePercent

    fun averageHeartRate(): Double? = averageHeartRate

    fun cadence(): Double? = cadence

    fun confidence(): BaselineConfidence? = confidence

    fun evidenceWeight(): Double = evidenceWeight

    fun findings(): List<String> = findings!!

    override fun toString(): String =
        "AssessmentSummary[averagePaceSecondsPerKm=$averagePaceSecondsPerKm, firstKilometrePace=$firstKilometrePace, finalKilometrePace=$finalKilometrePace, paceFadePercent=$paceFadePercent, averageHeartRate=$averageHeartRate, cadence=$cadence, confidence=$confidence, evidenceWeight=$evidenceWeight, findings=$findings]"
}
