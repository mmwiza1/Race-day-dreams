package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class AssessmentRecommendation(
    private val distanceKm: Int,
    private val unnecessary: Boolean,
    private var reasons: List<String>?,
) {
    init {
        require(distanceKm in 1..5) { "Assessment distance must be 1–5K." }
        reasons = baselineListSnapshot(reasons ?: throw NullPointerException())
    }

    fun distanceKm(): Int = distanceKm

    fun unnecessary(): Boolean = unnecessary

    fun reasons(): List<String> = reasons!!

    override fun toString(): String =
        "AssessmentRecommendation[distanceKm=$distanceKm, unnecessary=$unnecessary, reasons=$reasons]"
}
