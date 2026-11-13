package org.racedaydreams.domain.activity

import java.time.Clock
import java.time.LocalDate

/** Original JVM Clock boundary; all assignment arithmetic is shared. */
object ShoeMetricsCalculator {
    @JvmStatic
    fun calculate(shoe: Shoe?, activities: List<ActivityRecord>?, clock: Clock?): ShoeMetrics {
        if (shoe == null) throw NullPointerException("Shoe")
        if (activities == null) throw NullPointerException("Activities")
        if (clock == null) throw NullPointerException("Clock")
        return ShoeMileage.calculate(shoe, activities, LocalDate.now(clock))
    }
}
