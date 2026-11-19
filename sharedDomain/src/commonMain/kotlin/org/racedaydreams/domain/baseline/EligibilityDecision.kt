package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class EligibilityDecision(
    private val eligible: Boolean,
    private var reasons: List<String>?,
    private val validUntil: Instant?,
) {
    init {
        reasons = baselineListSnapshot(reasons ?: throw NullPointerException("reasons"))
        if (validUntil == null) throw NullPointerException("validUntil")
    }

    fun eligible(): Boolean = eligible

    fun reasons(): List<String> = reasons!!

    fun validUntil(): Instant = validUntil!!

    override fun toString(): String =
        "EligibilityDecision[eligible=$eligible, reasons=$reasons, validUntil=$validUntil]"
}
