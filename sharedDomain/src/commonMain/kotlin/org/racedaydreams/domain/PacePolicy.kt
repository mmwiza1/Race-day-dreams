package org.racedaydreams.domain

import kotlin.math.floor
import org.racedaydreams.domain.activity.PerformanceDistance

/** Existing deterministic pace rules without java.time or platform locale dependencies. */
object PacePolicy {
    private val timePattern = Regex("[0-9]{1,4}:[0-5][0-9](?::[0-5][0-9])?")
    const val MAX_TARGET_SECONDS: Long = 48 * 3600L
    private const val KILOMETRES_PER_MILE = 1.609344

    fun parseTargetSeconds(value: String?): Long {
        // Preserve java.lang.String.trim semantics, including rejection of non-breaking spaces.
        val text = value?.trim { it <= ' ' }
        require(text != null && timePattern.matches(text)) {
            "Enter a time as H:MM:SS or MM:SS, for example 1:59:59 or 27:00."
        }
        val parts = text.split(':').map { it.toLong() }
        val seconds =
            if (parts.size == 3) parts[0] * 3600 + parts[1] * 60 + parts[2]
            else parts[0] * 60 + parts[1]
        requireTargetSeconds(seconds)
        return seconds
    }

    fun requireTargetSeconds(seconds: Long) {
        require(seconds > 0 && seconds <= MAX_TARGET_SECONDS) {
            "Target time must be greater than zero and no more than 48 hours."
        }
    }

    fun formatSeconds(seconds: Long): String {
        require(seconds >= 0) { "A duration cannot be negative." }
        return "${seconds / 3600}:${twoDigits((seconds / 60) % 60)}:${twoDigits(seconds % 60)}"
    }

    fun targetPace(seconds: Long, distance: RaceDistance, units: UnitSystem): String {
        requireTargetSeconds(seconds)
        require(distance != RaceDistance.GENERAL_FITNESS) {
            "General fitness has no target race pace."
        }
        val unitsInRace =
            if (units == UnitSystem.METRIC) distance.kilometres()
            else distance.kilometres() / KILOMETRES_PER_MILE
        // Same rounding as Java Math.round for these bounded, positive inputs.
        val secondsPerUnit = roundedSeconds(seconds / unitsInRace)
        return "${secondsPerUnit / 60}:${twoDigits(secondsPerUnit % 60)}/${if (units == UnitSystem.METRIC) "km" else "mi"}"
    }

    /**
     * Same recorded-performance pace conversion, including second carries and overflow clamping.
     */
    fun performancePace(seconds: Double, distance: PerformanceDistance, units: UnitSystem): String {
        require(seconds.isFinite() && seconds > 0) { "A pace needs positive elapsed time." }
        val scale = if (units == UnitSystem.IMPERIAL) 1609.344 else 1000.0
        val value = seconds * scale / distance.metres()
        val rounded = roundedSeconds(value)
        return "${rounded / 60}:${twoDigits(rounded % 60)}/${if (units == UnitSystem.IMPERIAL) "mi" else "km"}"
    }

    private fun roundedSeconds(value: Double): Long {
        if (value >= Long.MAX_VALUE.toDouble()) return Long.MAX_VALUE
        // Adding 0.5 first can round a just-below-half input up, or an already integral
        // large Double to the next even integer. Compare the exact fractional part instead.
        val whole = floor(value)
        return whole.toLong() + if (value - whole >= 0.5) 1 else 0
    }

    private fun twoDigits(value: Long): String = value.toString().padStart(2, '0')
}
