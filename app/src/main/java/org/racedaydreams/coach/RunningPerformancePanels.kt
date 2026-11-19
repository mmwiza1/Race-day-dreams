package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.ZoneId
import org.racedaydreams.domain.activity.ActivityRecord
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

internal fun rankedRunningResults(results: List<RecordedResult>, code: String) =
    sharedRankedRunningResults(results, code)

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
