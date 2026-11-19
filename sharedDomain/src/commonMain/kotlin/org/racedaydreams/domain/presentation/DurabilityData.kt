package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.activity.*

/** Existing qualified-sample presentation; aggregate average pace is never a replacement. */
data class DurabilityData(
    private val status: String?,
    private val firstHalfSecondsPerKm: Double?,
    private val secondHalfSecondsPerKm: Double?,
    private val evidence: LongRunDurability?,
) {
    fun status(): String? = status

    fun firstHalfSecondsPerKm(): Double? = firstHalfSecondsPerKm

    fun secondHalfSecondsPerKm(): Double? = secondHalfSecondsPerKm

    fun evidence(): LongRunDurability? = evidence

    override fun toString(): String =
        "DurabilityData[status=$status, firstHalfSecondsPerKm=$firstHalfSecondsPerKm, secondHalfSecondsPerKm=$secondHalfSecondsPerKm, evidence=$evidence]"

    companion object {
        @JvmStatic
        fun of(run: ActivityRecord): DurabilityData {
            val d = LongRunDurability.analyse(run)
            if (!d.sufficient())
                return DurabilityData("Insufficient detailed run data", null, null, d)
            val first = run.samples().first()
            val last = run.samples().last()
            val seconds = last.elapsed().minus(first.elapsed()).toMillis() / 1000.0
            val metres = last.distanceMetres() - first.distanceMetres()
            val fade = d.paceFadePercent()!!
            val ratio = 1 + fade / 100
            val pace = seconds / 2 * (1 + 1 / ratio) / metres * 1000
            return DurabilityData(
                if (fade > 8) "Late fade"
                else if (fade < -3) "Faster second half" else "Stable pacing",
                pace,
                pace * ratio,
                d,
            )
        }
    }
}
