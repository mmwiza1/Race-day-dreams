package org.racedaydreams.domain

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.Durations

/**
 * Existing API and target checks; platform temporal identities preserve JVM caller compatibility.
 */
object PaceCalculator {
    @JvmStatic
    fun parseTime(value: String?): Duration =
        Durations.ofSeconds(PacePolicy.parseTargetSeconds(value))

    @JvmStatic
    fun formatTime(duration: Duration?): String {
        val time = duration ?: throw NullPointerException("A duration is required.")
        require(!time.isNegative()) { "A duration cannot be negative." }
        return PacePolicy.formatSeconds(time.getSeconds())
    }

    @JvmStatic
    fun targetPace(time: Duration?, distance: RaceDistance?, units: UnitSystem?): String {
        requireTargetTime(time)
        val race = distance ?: throw NullPointerException("A race distance is required.")
        val display = units ?: throw NullPointerException("Display units are required.")
        return PacePolicy.targetPace(time!!.getSeconds(), race, display)
    }

    @JvmStatic
    fun requireTargetTime(time: Duration?) {
        require(
            time != null &&
                !time.isNegative() &&
                !time.isZero() &&
                (time.getSeconds() < PacePolicy.MAX_TARGET_SECONDS ||
                    time.getSeconds() == PacePolicy.MAX_TARGET_SECONDS && time.getNano() == 0)
        ) {
            "Target time must be greater than zero and no more than 48 hours."
        }
        require(time.getNano() == 0) { "Enter target time in whole seconds." }
    }
}
