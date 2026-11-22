package org.racedaydreams.coach

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
