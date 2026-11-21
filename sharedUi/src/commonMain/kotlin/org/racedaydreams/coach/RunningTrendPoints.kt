package org.racedaydreams.coach

import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.PhysicalRunSessions
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

enum class RunningTrendMetric(val label: String, val heading: String) {
    DISTANCE("Distance", "Weekly recorded distance"),
    PACE("Pace", "Weekly elapsed average pace"),
    TIME("Training time", "Weekly recorded running time"),
}

/** Derived presentation only; this never replaces an activity's historical totals. */
data class RunningTrendPoint(val week: RecordedWeek, val value: Double?, val measuredRuns: Int)

fun sharedRunningTrendPoints(
    weeks: List<RecordedWeek>,
    activities: List<ActivityRecord>,
    metric: RunningTrendMetric,
    today: LocalDate,
    dateAt: (Instant) -> LocalDate,
): List<RunningTrendPoint> =
    PhysicalRunSessions.counted(activities).let { counted ->
        weeks.map { week ->
            val weekStart =
                requireNotNull(week.weekStart()) {
                    "Generated week must retain its calendar identity."
                }
            val runs =
                counted.filter {
                    val date = dateAt(it.start())
                    date.toEpochDay() >= weekStart.toEpochDay() &&
                        date.toEpochDay() < weekStart.toEpochDay() + 7 &&
                        date.toEpochDay() <= today.toEpochDay()
                }
            when (metric) {
                RunningTrendMetric.DISTANCE ->
                    RunningTrendPoint(
                        week,
                        week.distanceMetres(),
                        week.activityCount() - week.missingDistanceCount(),
                    )
                RunningTrendMetric.TIME ->
                    RunningTrendPoint(
                        week,
                        runs.takeIf { it.isNotEmpty() }?.sumOf { elapsedSeconds(it) },
                        runs.size,
                    )
                RunningTrendMetric.PACE -> {
                    val measured = runs.filter { (it.distanceMetres() ?: 0.0) > 0.0 }
                    val metres = measured.sumOf { it.distanceMetres()!! }
                    RunningTrendPoint(
                        week,
                        if (metres > 0) measured.sumOf { elapsedSeconds(it) } / (metres / 1000.0)
                        else null,
                        measured.size,
                    )
                }
            }
        }
    }

private fun elapsedSeconds(activity: ActivityRecord): Double {
    val elapsed = activity.elapsedTime()
    return elapsed.getSeconds() + (elapsed.getNano() / 1_000_000) / 1_000.0
}

/** For a week's Monday, the physical runs counted that week with their display dates. */
fun sharedWeekRuns(
    activities: List<ActivityRecord>,
    dateAt: (Instant) -> LocalDate,
): (LocalDate) -> List<Pair<LocalDate, PhysicalRunSessions.Session>> {
    val dated = PhysicalRunSessions.sessions(activities).map { dateAt(it.counted.start()) to it }
    return { weekStart ->
        dated.filter {
            it.first.toEpochDay() >= weekStart.toEpochDay() &&
                it.first.toEpochDay() < weekStart.toEpochDay() + 7
        }
    }
}
