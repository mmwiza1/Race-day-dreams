package org.racedaydreams.domain.activity

import kotlin.math.max
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Original ISO-week aggregation. Platforms project instants into the actual display timezone. */
object RecordedWeekAggregation {
    fun weekly(
        activities: List<ActivityRecord>,
        today: LocalDate,
        weeks: Int,
        displayDate: (Instant) -> LocalDate,
    ): List<RecordedWeek> {
        require(weeks in 1..104) { "Choose between one and 104 weeks." }
        val current = today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())
        val result = mutableListOf<RecordedWeek>()
        // One entry per physical run: other sources' copies of the same run are not counted again.
        val counted = PhysicalRunSessions.counted(activities)
        for (offset in weeks - 1 downTo 0) {
            val start = current.plusWeeks(-offset.toLong())
            var distance = 0.0
            var count = 0
            var unknown = 0
            var longest: Double? = null
            val days = mutableSetOf<LocalDate>()
            for (activity in counted) {
                val date = displayDate(activity.start())
                if (
                    date.toEpochDay() < start.toEpochDay() ||
                        date.toEpochDay() >= start.plusWeeks(1).toEpochDay() ||
                        date.toEpochDay() > today.toEpochDay()
                )
                    continue
                count++
                days.add(date)
                val metres = activity.distanceMetres()
                if (metres == null) unknown++
                else {
                    distance += metres
                    longest = longest?.let { max(it, metres) } ?: metres
                }
            }
            result.add(
                RecordedWeek(
                    start,
                    if (count > 0 && unknown == count) null else distance,
                    count,
                    days.size,
                    unknown,
                    longest,
                )
            )
        }
        return Immutable.copyList(result)
    }
}
