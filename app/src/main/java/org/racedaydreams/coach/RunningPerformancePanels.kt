package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.ZoneId
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.RecordedWeek

internal fun runningTrendPoints(
    weeks: List<RecordedWeek>,
    activities: List<ActivityRecord>,
    metric: RunningTrendMetric,
    today: LocalDate,
    zone: ZoneId,
): List<RunningTrendPoint> =
    sharedRunningTrendPoints(weeks, activities, metric, today) { it.atZone(zone).toLocalDate() }

/** Reuse the existing source-precedence calculator before ranking display rows. */
internal fun rankedRunningResults(
    results: List<RecordedResult>,
    code: String,
): List<RecordedResult> {
    val atDistance = results.filter { it.distance().code() == code }
    val best =
        PersonalBestCalculator.current(atDistance).firstOrNull()?.result() ?: return emptyList()
    val remaining =
        atDistance
            .groupBy { result ->
                result.activityId()?.let { "activity:$it" } ?: "entry:${result.id()}"
            }
            .values
            .flatMap { PersonalBestCalculator.current(it).map { entry -> entry.result() } }
            .filter { it.id() != best.id() }
            .sortedWith(
                compareBy<RecordedResult> { it.duration() }.thenBy { it.date() }.thenBy { it.id() }
            )
            .take(2)
    return listOf(best) + remaining
}

@Composable
internal fun RunningHeartRatePanel(activities: List<ActivityRecord>, state: CoachUiState) {
    val zone = ZoneId.systemDefault()
    SharedRunningHeartRatePanel(
        activities,
        dateLabel = { friendlyDate(it.start().atZone(zone).toLocalDate()) },
        shortLabel = { shortDate(it.start().atZone(zone).toLocalDate()) },
        distance = { formatDistance(it, state) },
    )
}
