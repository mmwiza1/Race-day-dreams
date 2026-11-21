package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.activity.PhysicalRunSessions
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedWeeklyRunningChart(
    weeks: List<RecordedWeek>,
    points: List<RunningTrendPoint>,
    metric: RunningTrendMetric,
    imperial: Boolean,
    distance: (Double) -> String,
    decimal: (Double) -> String,
    formatPaceSeconds: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    /** Physical runs counted in a week, with each run's display date, for inspection. */
    weekRuns: ((LocalDate) -> List<Pair<LocalDate, PhysicalRunSessions.Session>>)? = null,
) {
    if (weeks.isEmpty()) {
        Text("Record runs to see your weekly distance.")
        return
    }
    var selectedWeek by rememberSaveable { mutableStateOf<String?>(null) }
    val selected =
        weeks.indexOfFirst { it.weekStart().toString() == selectedWeek }.takeIf { it >= 0 }
            ?: weeks.lastIndex
    val values = points.mapNotNull { point -> point.value?.takeIf { it > 0 } }
    val minimumScale =
        when (metric) {
            RunningTrendMetric.DISTANCE -> 1000.0
            RunningTrendMetric.TIME -> 60.0
            RunningTrendMetric.PACE -> 1.0
        }
    val maximum = (values.maxOrNull() ?: minimumScale).coerceAtLeast(minimumScale)
    val minimum = if (metric == RunningTrendMetric.PACE) (values.minOrNull() ?: 0.0) * .9 else 0.0
    val upper = if (metric == RunningTrendMetric.PACE) maximum * 1.1 else maximum
    val primary = MaterialTheme.colorScheme.primary
    val normal = MaterialTheme.colorScheme.secondary
    val contrast = MaterialTheme.colorScheme.onPrimary
    val outline = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant
    val background = MaterialTheme.colorScheme.surfaceContainerLow
    fun valueText(value: Double?): String =
        when {
            value == null -> "Unavailable"
            metric == RunningTrendMetric.DISTANCE -> distance(value)
            metric == RunningTrendMetric.TIME -> "${decimal(value / 60)} min"
            else ->
                "${formatPaceSeconds(value * if (imperial) 1.609344 else 1.0)} /${if (imperial) "mi" else "km"}"
        }
    fun description(point: RunningTrendPoint): String =
        "Week of ${shortDate(requireNotNull(point.week.weekStart()))}: " +
            when {
                point.week.activityCount() == 0 -> "No recorded runs"
                point.value == null -> "${metric.label} unavailable"
                else ->
                    "${valueText(point.value)}, ${point.week.activityCount()} recorded runs" +
                        if (point.measuredRuns < point.week.activityCount())
                            ", based on ${point.measuredRuns} with this measurement"
                        else ""
            }
    val current = points[selected]
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).testTag("selected-running-week").semantics(
                mergeDescendants = true
            ) {
                liveRegion = LiveRegionMode.Polite
            },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Week of ${friendlyDate(requireNotNull(current.week.weekStart()))}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                if (current.week.activityCount() == 0) "No recorded runs"
                else valueText(current.value),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                "${current.week.activityCount()} recorded runs · ${current.week.runningDays()} running days"
            )
            if (current.measuredRuns < current.week.activityCount())
                Text(
                    "${current.measuredRuns} of ${current.week.activityCount()} runs have this measurement."
                )
        }
    }
    if (metric == RunningTrendMetric.PACE && values.isEmpty()) {
        Text(
            "Average pace is unavailable. A recorded run needs a known positive distance and elapsed time."
        )
        return
    }
    val axis =
        if (metric == RunningTrendMetric.PACE)
            "Pace axis: ${valueText(minimum)} at top to ${valueText(upper)} at bottom"
        else "${metric.label} axis: ${valueText(0.0)} to ${valueText(upper)}"
    val chartHeight = if (LocalDensity.current.fontScale >= 1.5f) 224.dp else 180.dp
    Row(
        Modifier.fillMaxWidth().height(chartHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.fillMaxHeight()
                .width(if (LocalDensity.current.fontScale >= 1.5f) 88.dp else 68.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            val topValue = if (metric == RunningTrendMetric.PACE) minimum else upper
            val bottomValue = if (metric == RunningTrendMetric.PACE) upper else 0.0
            Text(valueText(topValue), style = MaterialTheme.typography.labelSmall)
            Text(
                valueText((topValue + bottomValue) / 2),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(valueText(bottomValue), style = MaterialTheme.typography.labelSmall)
        }
        Canvas(
            Modifier.weight(1f)
                .fillMaxHeight()
                .background(background, RoundedCornerShape(24.dp))
                .testTag("running-trend-chart")
                .semantics {
                    contentDescription =
                        "${metric.heading}. " + points.joinToString("; ", transform = ::description)
                    stateDescription = description(current)
                    customActions =
                        points.map { point ->
                            CustomAccessibilityAction(
                                "Select week of ${shortDate(requireNotNull(point.week.weekStart()))}"
                            ) {
                                selectedWeek = point.week.weekStart().toString()
                                true
                            }
                        }
                }
                .pointerInput(points) {
                    detectTapGestures { position ->
                        val cell = size.width.toFloat() / points.size
                        val index = (position.x / cell).toInt().coerceIn(points.indices)
                        selectedWeek = points[index].week.weekStart().toString()
                    }
                }
        ) {
            val cell = size.width / points.size
            val top = 16.dp.toPx()
            val bottom = size.height - 14.dp.toPx()
            val chartHeight = bottom - top
            repeat(3) { index ->
                val y = top + chartHeight * index / 2
                drawLine(
                    grid,
                    Offset(10.dp.toPx(), y),
                    Offset(size.width - 10.dp.toPx(), y),
                    1.dp.toPx(),
                )
            }
            fun y(value: Double) =
                if (metric == RunningTrendMetric.PACE)
                    top +
                        ((value - minimum) / (upper - minimum).coerceAtLeast(1.0)).toFloat() *
                            chartHeight
                else bottom - (value / upper).toFloat() * chartHeight
            points.forEachIndexed { index, point ->
                val x = cell * (index + .5f)
                val value = point.value
                if (value != null && value > 0) {
                    if (metric == RunningTrendMetric.PACE) {
                        if (
                            index > 0 &&
                                points[index - 1].value != null &&
                                points[index - 1].week.activityCount() > 0
                        )
                            drawLine(
                                primary,
                                Offset(x - cell, y(points[index - 1].value!!)),
                                Offset(x, y(value)),
                                4.dp.toPx(),
                                StrokeCap.Round,
                            )
                        drawCircle(
                            if (index == selected) primary else outline,
                            if (index == selected) 7.dp.toPx() else 4.dp.toPx(),
                            Offset(x, y(value)),
                        )
                        if (index == selected)
                            drawCircle(contrast, 2.dp.toPx(), Offset(x, y(value)))
                    } else {
                        val height = (bottom - y(value)).coerceAtLeast(3.dp.toPx())
                        val width = (cell * .66f).coerceAtMost(42.dp.toPx())
                        drawRoundRect(
                            if (index == selected) primary else normal,
                            Offset(x - width / 2, bottom - height),
                            Size(width, height),
                            CornerRadius(width / 2),
                        )
                        if (index == selected) {
                            drawRoundRect(
                                outline,
                                Offset(x - width / 2, bottom - height),
                                Size(width, height),
                                CornerRadius(width / 2),
                                style = Stroke(1.dp.toPx()),
                            )
                            drawCircle(
                                contrast,
                                2.dp.toPx(),
                                Offset(
                                    x,
                                    (bottom - height + 6.dp.toPx()).coerceAtMost(
                                        bottom - 1.dp.toPx()
                                    ),
                                ),
                            )
                        }
                    }
                } else if (value == 0.0 && point.week.activityCount() > 0) {
                    // A known zero is an outlined baseline pill, never the unknown-data cross.
                    drawRoundRect(
                        outline,
                        Offset(x - 5.dp.toPx(), bottom - 3.dp.toPx()),
                        Size(10.dp.toPx(), 3.dp.toPx()),
                        CornerRadius(2.dp.toPx()),
                        style = Stroke(1.dp.toPx()),
                    )
                } else if (point.week.activityCount() > 0) {
                    drawLine(
                        outline,
                        Offset(x - 4.dp.toPx(), bottom - 8.dp.toPx()),
                        Offset(x + 4.dp.toPx(), bottom),
                        2.dp.toPx(),
                    )
                    drawLine(
                        outline,
                        Offset(x - 4.dp.toPx(), bottom),
                        Offset(x + 4.dp.toPx(), bottom - 8.dp.toPx()),
                        2.dp.toPx(),
                    )
                } else drawCircle(outline, 2.dp.toPx(), Offset(x, bottom))
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            shortDate(requireNotNull(weeks.first().weekStart())),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            shortDate(requireNotNull(weeks.last().weekStart())),
            style = MaterialTheme.typography.labelSmall,
        )
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
    ) {
        OutlinedButton(
            onClick = { selectedWeek = weeks[selected - 1].weekStart().toString() },
            enabled = selected > 0,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Earlier week")
        }
        OutlinedButton(
            onClick = { selectedWeek = weeks[selected + 1].weekStart().toString() },
            enabled = selected < weeks.lastIndex,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Later week")
        }
    }
    current.week.longestRunMetres()?.let {
        Text("Longest: ${distance(it)}", style = MaterialTheme.typography.bodySmall)
    }
    if (weekRuns != null && current.week.activityCount() > 0)
        WeekRunSources(weekRuns(requireNotNull(current.week.weekStart())), distance, friendlyDate)
    // The visible axis labels and selected-week panel carry the reading; the full key stays
    // available on request instead of three permanent lines of small print.
    Explainer(
        "How to read this chart",
        "$axis. Tap a week or use the buttons. A dot means no recorded runs, a cross means " +
            "unavailable and an outline means a recorded zero. ${description(current)}.",
    )
}

/**
 * Which runs and sources make up a week's totals. Copies of the same run from other sources are
 * listed but not counted again, so the runner can see why the count differs from raw imports.
 */
@Composable
private fun WeekRunSources(
    runs: List<Pair<LocalDate, PhysicalRunSessions.Session>>,
    distance: (Double) -> String,
    friendlyDate: (LocalDate) -> String,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(
        onClick = { open = !open },
        modifier = Modifier.heightIn(min = 48.dp).testTag("week-run-sources-toggle"),
    ) {
        Text(if (open) "Hide the runs in this week" else "Show the runs in this week")
    }
    if (!open) return
    Column(
        Modifier.fillMaxWidth().testTag("week-run-sources"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((date, session) in runs) {
            val run = session.counted
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(
                    "${friendlyDate(date)} · ${run.distanceMetres()?.let(distance) ?: "No distance"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Counted from ${ActivitySources.detailed(run.source())}",
                    style = MaterialTheme.typography.bodySmall,
                )
                for (copy in session.duplicates) Text(
                    "Same run also from ${ActivitySources.detailed(copy.source())}" +
                        (copy.distanceMetres()?.let { " (${distance(it)})" } ?: "") +
                        ": not counted again",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
