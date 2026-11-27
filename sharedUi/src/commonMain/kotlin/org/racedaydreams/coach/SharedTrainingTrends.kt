package org.racedaydreams.coach

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.RecordedWeekAggregation
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * The full Training trends screen, shared by Android and iOS: a compact running header and the
 * reporting period, then the same chart-first explorer as Progress with heart rate added. Weeks
 * come from the existing weekly aggregation (one entry per physical run); nothing here computes a
 * new figure.
 */
@Composable
fun SharedTrainingTrends(
    activities: List<ActivityRecord>,
    today: LocalDate,
    dateAt: (Instant) -> LocalDate,
    imperial: Boolean,
    distance: (Double) -> String,
    decimal: (Double) -> String,
    paceSeconds: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    openRun: (String) -> Unit,
    seeAll: (() -> Unit)?,
) {
    var periodName by rememberSaveable { mutableStateOf(Period.TWELVE_WEEKS.name) }
    val period = Period.valueOf(periodName)
    val weeks =
        remember(activities, today, period) {
            RecordedWeekAggregation.weekly(activities, today, period.weeks, dateAt)
        }
    ScreenHeader(
        Discipline.PROGRESS,
        Glyph.TREND,
        eyebrow = "Weekly running",
        title = "Training trends",
    )
    PeriodSelector(period, Discipline.PROGRESS) { periodName = it.name }
    weeks.firstOrNull()?.weekStart()?.let { first ->
        Text(
            "${friendlyDate(first)} – ${friendlyDate(today)} · calendar weeks, this week in progress",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("trends-range"),
        )
    }
    if (activities.isEmpty()) Text("Record or import runs to see your trends.")
    RunningTrendExplorer(
        weeks,
        activities,
        today,
        dateAt,
        imperial,
        distance,
        decimal,
        paceSeconds,
        shortDate,
        friendlyDate,
        currentWeekComparison = null,
        openRun = openRun,
        seeAll = seeAll,
        openTrends = null,
        heartRate = true,
    )
    WhyDetails(
        "Distance: known recorded distances added within each calendar week; runs without distance " +
            "stay missing.\n\nTime: recorded elapsed durations added by week, including pauses.\n\n" +
            "Pace: total elapsed time divided by total distance for runs with a known distance. " +
            "Lower time per kilometre is faster, so faster weeks sit higher.\n\nHeart rate: each " +
            "run's own recorded average, as saved by the device or app that recorded it. Runs " +
            "without heart rate are not counted, and no zones, readiness or load scores are " +
            "produced.\n\nNo recorded runs does not prove that no training happened.",
        "About these measures",
    )
}
