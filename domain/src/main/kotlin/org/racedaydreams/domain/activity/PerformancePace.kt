package org.racedaydreams.domain.activity

import java.time.Duration
import java.util.Objects
import org.racedaydreams.domain.PacePolicy
import org.racedaydreams.domain.UnitSystem

/** Retained JVM Duration API; the reusable formatting calculation lives in shared policy. */
object PerformancePace {
    @JvmStatic
    fun format(duration: Duration?, distance: PerformanceDistance?, units: UnitSystem?): String {
        val time = Objects.requireNonNull(duration, "Duration")!!
        val course = Objects.requireNonNull(distance, "Distance")!!
        val display = Objects.requireNonNull(units, "Units")!!
        return PacePolicy.performancePace(ActivityChecks.seconds(time), course, display)
    }
}
