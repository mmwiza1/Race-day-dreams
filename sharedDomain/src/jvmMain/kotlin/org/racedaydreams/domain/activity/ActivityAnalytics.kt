package org.racedaydreams.domain.activity

import java.time.LocalDate
import java.time.ZoneId

/**
 * Original Java-callable API. Only platform timezone projection remains outside the common core.
 */
object ActivityAnalytics {
    @JvmStatic
    fun weekly(
        activities: List<ActivityRecord>,
        today: LocalDate,
        displayZone: ZoneId?,
        weeks: Int,
    ): List<RecordedWeek> =
        RecordedWeekAggregation.weekly(activities, today, weeks) {
            it.atZone(displayZone).toLocalDate()
        }
}
