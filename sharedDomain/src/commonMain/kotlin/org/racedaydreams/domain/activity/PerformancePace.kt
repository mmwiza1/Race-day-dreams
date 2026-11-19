package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.PacePolicy
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.time.Duration

/** Retained Duration API on both platforms; formatting calculation lives only in shared policy. */
object PerformancePace {
    @JvmStatic
    fun format(duration: Duration?, distance: PerformanceDistance?, units: UnitSystem?): String {
        val time = duration ?: throw NullPointerException("Duration")
        val course = distance ?: throw NullPointerException("Distance")
        val display = units ?: throw NullPointerException("Units")
        return PacePolicy.performancePace(ActivityChecks.seconds(time), course, display)
    }
}
