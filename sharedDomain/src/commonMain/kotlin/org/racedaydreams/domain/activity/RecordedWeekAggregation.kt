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
    ): List<RecordedWeek> =
        // One entry per physical run: other sources' copies of the same run are not counted again.
        weeklyCounted(PhysicalRunSessions.counted(activities), today, weeks, displayDate)

    /** [weekly] over runs already reduced to one record per physical run. */
    internal fun weeklyCounted(
        counted: List<ActivityRecord>,
        today: LocalDate,
        weeks: Int,
        displayDate: (Instant) -> LocalDate,
    ): List<RecordedWeek> {
        require(weeks in 1..104) { "Choose between one and 104 weeks." }
        val current = today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())
        val result = mutableListOf<RecordedWeek>()
        // Each run's display date is computed once and grouped by week, instead of scanning the
        // whole history for every displayed week. Runs keep their order, so totals are identical.
        val byWeek = byWeek(counted, displayDate)
        for (offset in weeks - 1 downTo 0) {
            val start = current.plusWeeks(-offset.toLong())
            var distance = 0.0
            var count = 0
            var unknown = 0
            var longest: Double? = null
            val days = mutableSetOf<LocalDate>()
            for ((activity, date) in byWeek[start.toEpochDay()].orEmpty()) {
                if (date.toEpochDay() > today.toEpochDay()) continue
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

    /**
     * Runs with their display dates, keyed by the epoch day of the Monday starting their ISO week.
     * Each list keeps the order of [runs]; each display date is computed once.
     */
    fun byWeek(
        runs: List<ActivityRecord>,
        displayDate: (Instant) -> LocalDate,
    ): Map<Long, List<Pair<ActivityRecord, LocalDate>>> {
        val out = mutableMapOf<Long, MutableList<Pair<ActivityRecord, LocalDate>>>()
        for (run in runs) {
            val date = displayDate(run.start())
            out.getOrPut(mondayEpochDay(date.toEpochDay())) { mutableListOf() }.add(run to date)
        }
        return out
    }

    /** The epoch day of the Monday of [epochDay]'s ISO week (1970-01-01 was a Thursday). */
    fun mondayEpochDay(epochDay: Long): Long = epochDay - (epochDay + 3).mod(7L)
}
