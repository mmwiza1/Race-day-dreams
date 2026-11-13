package org.racedaydreams.domain

import java.time.Duration
import java.util.Objects

/** Existing Java API retained as a JVM time adapter; calculation lives only in common code. */
object PaceCalculator {
    @JvmStatic
    fun parseTime(value: String?): Duration =
        Duration.ofSeconds(PacePolicy.parseTargetSeconds(value))

    @JvmStatic
    fun formatTime(duration: Duration?): String {
        val time = Objects.requireNonNull(duration, "A duration is required.")!!
        require(!time.isNegative) { "A duration cannot be negative." }
        return PacePolicy.formatSeconds(time.seconds)
    }

    @JvmStatic
    fun targetPace(time: Duration?, distance: RaceDistance?, units: UnitSystem?): String {
        requireTargetTime(time)
        val race = Objects.requireNonNull(distance, "A race distance is required.")!!
        val display = Objects.requireNonNull(units, "Display units are required.")!!
        return PacePolicy.targetPace(time!!.seconds, race, display)
    }

    @JvmStatic
    fun requireTargetTime(time: Duration?) {
        require(time != null && !time.isNegative && !time.isZero && time <= Duration.ofHours(48)) {
            "Target time must be greater than zero and no more than 48 hours."
        }
        require(time.nano == 0) { "Enter target time in whole seconds." }
    }
}
