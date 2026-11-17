package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.*
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.RecordedWeekAggregation
import org.racedaydreams.reminders.KotlinxReminderCalendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppleRunningTrends(state: CoachUiState, back: () -> Unit) {
    var window by rememberSaveable { mutableIntStateOf(8) }
    var selection by rememberSaveable { mutableStateOf(RunningTrendMetric.DISTANCE.name) }
    val metric = RunningTrendMetric.entries.firstOrNull { it.name == selection }
    val today = AppleCalendarDates.today()
    val zone = TimeZone.currentSystemDefault().id
    val weeks =
        remember(state.activities, today, zone, window) {
            RecordedWeekAggregation.weekly(state.activities, today, window) {
                KotlinxReminderCalendar.at(it, zone).date
            }
        }
    val points =
        remember(weeks, state.activities, metric, today, zone) {
            if (metric == null) emptyList()
            else
                sharedRunningTrendPoints(weeks, state.activities, metric, today) {
                    KotlinxReminderCalendar.at(it, zone).date
                }
        }
    val presentation = appleHomePresentation(state)
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    CoachPage {
        CoachButton("Back", back)
        CoachHeading("Training trends")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 8, 12).forEach { count ->
                CoachFilterChip(
                    selected = window == count,
                    onClick = { window = count },
                    label = { Text("$count weeks") },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunningTrendMetric.entries.forEach {
                CoachFilterChip(
                    selected = metric == it,
                    onClick = { selection = it.name },
                    label = { Text(it.label) },
                )
            }
            CoachFilterChip(
                selected = metric == null,
                onClick = { selection = "HEART_RATE" },
                label = { Text("Heart rate") },
            )
        }
        if (metric == null) {
            val earliest = requireNotNull(weeks.first().weekStart()).toEpochDay()
            val latest = today.toEpochDay()
            val runs =
                remember(state.activities, earliest, latest, zone) {
                    state.activities.filter {
                        KotlinxReminderCalendar.at(it.start(), zone).date.toEpochDay() in
                            earliest..latest
                    }
                }
            SharedRunningHeartRatePanel(
                runs,
                presentation.activityDate,
                presentation.activityDate,
                presentation.distance,
            )
        } else {
            SharedWeeklyRunningChart(
                weeks,
                points,
                metric,
                imperial,
                presentation.distance,
                presentation.decimal,
                presentation.paceSeconds,
                presentation.shortDate,
                presentation.friendlyDate,
            )
        }
    }
}
