package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.LocalDate
import java.time.ZoneId
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedWeek

/** One bar per calendar week; no recorded runs and unknown distance remain distinct. */
@Composable
fun WeeklyDistanceChart(weeks: List<RecordedWeek>, state: CoachUiState) {
    WeeklyRunningChart(weeks, state.activities, RunningTrendMetric.DISTANCE, state)
}

@Composable
internal fun WeeklyRunningChart(
    weeks: List<RecordedWeek>,
    activities: List<ActivityRecord>,
    metric: RunningTrendMetric,
    state: CoachUiState,
) {
    val points =
        remember(weeks, activities, metric) {
            runningTrendPoints(weeks, activities, metric, LocalDate.now(), ZoneId.systemDefault())
        }
    SharedWeeklyRunningChart(
        weeks,
        points,
        metric,
        state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
        { formatDistance(it, state) },
        ::decimal,
        ::formatPaceSeconds,
        ::shortDate,
        ::friendlyDate,
        remember(activities) {
            val zone = ZoneId.systemDefault()
            sharedWeekRuns(activities) { it.atZone(zone).toLocalDate() }
        },
    )
}
