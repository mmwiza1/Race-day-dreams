package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/**
 * Large synthetic histories: the linear dashboard aggregations produce exactly the figures of the
 * earlier per-week scans and quadratic duplicate grouping (kept below as references), and the
 * timings are printed as before/after evidence. Timings are reported, never asserted.
 */
class DashboardHistoryPerformanceTest {
    private val today = CalendarDates.of(2026, 11, 18)
    private val zone = Offsets.ofTotalSeconds(3600)

    private fun displayDate(at: Instant) = CalendarDates.atOffset(at, zone)

    private fun at(epochDay: Long, minutes: Long) =
        Instants.ofEpochSecond(epochDay * 86_400 + minutes * 60)

    private fun record(
        id: String,
        provider: String,
        origin: String?,
        start: Instant,
        minutes: Long,
        metres: Double?,
        planned: String? = null,
    ) =
        ActivityRecord(
            id,
            SourceIdentity(provider, id, origin),
            start,
            Instants.ofEpochSecond(start.getEpochSecond() + minutes * 60),
            zone,
            zone,
            metres,
            Durations.ofSeconds(minutes * 60 - 30),
            null,
            null,
            null,
            null,
            null,
            "",
            emptyList(),
            emptyList(),
            planned,
            null,
        )

    /**
     * Four runs a week cycling through 5K, 10K, half-marathon and marathon-length sessions, with
     * the duplicates seen in practice: another app's Health Connect copy, a manual entry of the
     * same run, a separate evening run, and a run without distance.
     */
    private fun history(years: Int): List<ActivityRecord> {
        val out = mutableListOf<ActivityRecord>()
        val days = years * 364L
        val first = today.toEpochDay() - days
        val lengths = listOf(5_000.0 to 30L, 10_000.0 to 58L, 21_097.5 to 125L, 42_195.0 to 270L)
        var n = 0
        for (day in first..today.toEpochDay()) {
            if ((day - first) % 7 !in listOf(0L, 2L, 4L, 6L)) continue
            val (metres, minutes) = lengths[n % 4]
            val start = at(day, 6 * 60 + (n % 5) * 7L)
            val id = "r$n"
            out +=
                record(
                    "rdd:$id",
                    "rdd",
                    "org.racedaydreams",
                    start,
                    minutes,
                    metres,
                    "p$n".takeIf { n % 3 == 0 },
                )
            if (n % 3 == 1)
                out +=
                    record(
                        "hc:s$id",
                        "health-connect",
                        "com.sec.android.app.shealth",
                        at(day, 6 * 60 + (n % 5) * 7L + 1),
                        minutes - 1,
                        metres + 40,
                    )
            if (n % 5 == 2)
                out +=
                    record(
                        "hc:t$id",
                        "health-connect",
                        "com.strava",
                        at(day, 6 * 60 + (n % 5) * 7L + 2),
                        minutes,
                        metres - 25,
                    )
            if (n % 11 == 3) out += record("manual:$id", "manual", null, start, minutes, metres)
            if (n % 13 == 4)
                out += record("rdd:e$id", "rdd", "org.racedaydreams", at(day, 18 * 60), 25, 4_000.0)
            if (n % 17 == 5)
                out += record("hc:x$id", "health-connect", "com.garmin", at(day, 12 * 60), 40, null)
            n++
        }
        // Repeated syncs deliver the same records again.
        return out + out.takeLast(40)
    }

    // ---- References: the implementations before this change, kept verbatim in behaviour. ----

    /** The earlier grouping: every record compared with every group so far (quadratic). */
    private fun referenceGroups(activities: List<ActivityRecord>): Set<Set<String>> {
        val ordered =
            activities
                .distinctBy { it.id() }
                .sortedWith(compareBy<ActivityRecord> { it.start() }.thenBy { it.id() })
        fun key(a: ActivityRecord) = a.source().provider() to a.source().originPackage()
        val groups = mutableListOf<MutableList<ActivityRecord>>()
        for (activity in ordered) {
            val group =
                groups.firstOrNull { members ->
                    members.none { key(it) == key(activity) } &&
                        members.any { PhysicalRunSessions.sameRun(it, activity) }
                }
            if (group == null) groups.add(mutableListOf(activity)) else group.add(activity)
        }
        return groups.map { members -> members.map { it.id() }.toSet() }.toSet()
    }

    /** The earlier weekly totals: the whole history scanned once per displayed week. */
    private fun referenceWeekly(counted: List<ActivityRecord>, weeks: Int): List<RecordedWeek> {
        val current = today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())
        return (weeks - 1 downTo 0).map { offset ->
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
                    longest = longest?.let { maxOf(it, metres) } ?: metres
                }
            }
            RecordedWeek(
                start,
                if (count > 0 && unknown == count) null else distance,
                count,
                days.size,
                unknown,
                longest,
            )
        }
    }

    private fun millis(repeats: Int = 5, block: () -> Unit): Double {
        repeat(2) { block() }
        return (1..repeats)
            .map {
                val start = System.nanoTime()
                block()
                (System.nanoTime() - start) / 1e6
            }
            .sorted()[repeats / 2]
    }

    @Test
    fun linearAggregationsMatchTheEarlierScansOnLargeHistories() {
        for (years in listOf(1, 3, 5)) {
            val activities = history(years)
            val sessions = PhysicalRunSessions.sessions(activities)
            assertEquals(
                referenceGroups(activities),
                sessions.map { session -> session.records().map { it.id() }.toSet() }.toSet(),
                "$years years: same runs grouped",
            )
            val counted = PhysicalRunSessions.counted(activities)
            assertTrue(sessions.any { it.duplicates.isNotEmpty() }, "history includes duplicates")
            assertTrue(sessions.size < activities.distinctBy { it.id() }.size)
            for (weeks in listOf(4, 12, 26, 52, 104)) {
                assertEquals(
                    referenceWeekly(counted, weeks),
                    RecordedWeekAggregation.weekly(activities, today, weeks, ::displayDate),
                    "$years years, $weeks weeks",
                )
            }
            for (period in Period.values()) {
                val running = DashboardAnalytics.running(activities, today, period, ::displayDate)
                assertEquals(referenceWeekly(counted, period.weeks), running.weeklyTrend)
            }
        }
    }

    @Test
    fun printBeforeAndAfterTimingsForLargeHistories() {
        println("dashboard-benchmark | years | records | before ms | after ms | step")
        for (years in listOf(1, 3, 5)) {
            val activities = history(years)
            val records = activities.size
            val counted = PhysicalRunSessions.counted(activities)
            val groupBefore = millis(3) { referenceGroups(activities) }
            val groupAfter = millis { PhysicalRunSessions.sessions(activities) }
            val weeklyBefore = millis { referenceWeekly(counted, 52) }
            val weeklyAfter = millis {
                RecordedWeekAggregation.weeklyCounted(counted, today, 52, ::displayDate)
            }
            val running = millis {
                DashboardAnalytics.running(activities, today, Period.ONE_YEAR, ::displayDate)
            }
            println(
                "dashboard-benchmark | $years | $records | ${"%.1f".format(groupBefore)} | ${"%.1f".format(groupAfter)} | duplicate grouping"
            )
            println(
                "dashboard-benchmark | $years | $records | ${"%.1f".format(weeklyBefore)} | ${"%.1f".format(weeklyAfter)} | 52-week totals"
            )
            println(
                "dashboard-benchmark | $years | $records | - | ${"%.1f".format(running)} | running dashboard, 1 year"
            )
        }
    }
}
