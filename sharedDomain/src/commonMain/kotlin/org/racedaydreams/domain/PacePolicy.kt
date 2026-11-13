package org.racedaydreams.domain

import kotlin.math.floor

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
        val secondsPerUnit = floor(seconds / unitsInRace + 0.5).toLong()
        return "${secondsPerUnit / 60}:${twoDigits(secondsPerUnit % 60)}/${if (units == UnitSystem.METRIC) "km" else "mi"}"
    }

    private fun twoDigits(value: Long): String = value.toString().padStart(2, '0')
}
