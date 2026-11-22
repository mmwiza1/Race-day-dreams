@file:OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Chart-first running analysis. One surface: a scrub readout (the selected week's figure, large), a
 * metric switch, an interactive weekly chart and the selected week's runs as a drill-down. It draws
 * the existing weekly trend points only; no figure is computed here.
 */
@Composable
fun RunningTrendExplorer(
    weeks: List<RecordedWeek>,
    activities: List<ActivityRecord>,
    today: LocalDate,
    dateAt: (Instant) -> LocalDate,
    imperial: Boolean,
    distance: (Double) -> String,
    decimal: (Double) -> String,
    paceSeconds: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    /** "Same days last week: …" for the current week, from the existing week-to-date summary. */
    currentWeekComparison: String?,
    openRun: (String) -> Unit,
    seeAll: (() -> Unit)?,
    openTrends: (() -> Unit)?,
) {
    if (weeks.isEmpty()) return
    var metricName by rememberSaveable { mutableStateOf(RunningTrendMetric.DISTANCE.name) }
    val metric = RunningTrendMetric.valueOf(metricName)
    val points =
        remember(weeks, activities, metric, today) {
            sharedRunningTrendPoints(weeks, activities, metric, today, dateAt)
        }
    val weekRuns = remember(activities) { sharedWeekRuns(activities, dateAt) }
    var selectedWeek by rememberSaveable(weeks.size) { mutableStateOf<String?>(null) }
    val selected =
        weeks.indexOfFirst { it.weekStart().toString() == selectedWeek }.takeIf { it >= 0 }
            ?: weeks.lastIndex
    val point = points[selected]
    val week = point.week
    val weekStart = requireNotNull(week.weekStart())
    val isCurrent = selected == weeks.lastIndex
    val unit = if (imperial) "mi" else "km"
    fun valueText(value: Double?): String =
        when {
            value == null -> "Unavailable"
            metric == RunningTrendMetric.DISTANCE -> distance(value)
            metric == RunningTrendMetric.TIME -> formatMinutes(value)
            else -> "${paceSeconds(value * if (imperial) 1.609344 else 1.0)} /$unit"
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricSwitch(metric) { metricName = it.name }

        // Readout: what the chart says about the selected week, before the chart itself.
        Column(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                when {
                    isCurrent && metric == RunningTrendMetric.DISTANCE -> "Distance this week"
                    isCurrent -> "${metric.label} this week"
                    else -> "Week of ${friendlyDate(weekStart)}"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (week.activityCount() == 0) "No runs" else valueText(point.value),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                modifier =
                    if (isCurrent && metric == RunningTrendMetric.DISTANCE)
                        Modifier.testTag("dashboard-week-distance")
                    else Modifier.testTag("trend-readout-value"),
            )
            Text(
                buildString {
                    append(plural(week.activityCount(), "run"))
                    append(" · ")
                    append(plural(week.runningDays(), "running day"))
                    if (point.measuredRuns < week.activityCount() && week.activityCount() > 0)
                        append(" · based on ${point.measuredRuns} of ${week.activityCount()}")
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            // The comparison is a distance comparison, so it belongs to the Distance view only;
            // Time and Pace show their own figures without a substitute comparison.
            if (isCurrent && metric == RunningTrendMetric.DISTANCE && currentWeekComparison != null)
                Text(
                    currentWeekComparison,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("dashboard-week-comparison"),
                )
        }

        TrendChart(points, metric, selected, ::valueText, shortDate) {
            selectedWeek = weeks[it].weekStart().toString()
        }

        // Drill-down: the runs behind the selected bar, one tap from each activity.
        val runs =
            remember(weekRuns, weekStart) {
                weekRuns(weekStart).sortedByDescending { it.first.toEpochDay() }
            }
        SectionHeader(if (isCurrent) "Runs this week" else "Runs that week", seeAll)
        Column(Modifier.testTag("week-runs")) {
            if (runs.isEmpty())
                Text(
                    "No runs recorded in this week.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            runs.forEach { (date, session) ->
                val run = session.counted
                DashboardRow(
                    title = friendlyDate(date),
                    detail =
                        ActivitySources.friendly(run.source()) +
                            if (session.duplicates.isNotEmpty())
                                " · also from ${plural(session.duplicates.size, "other source")}"
                            else "",
                    value = run.distanceMetres()?.let(distance) ?: "No distance",
                    secondary = null,
                    onClick = { openRun(run.id()) },
                )
            }
        }
        if (openTrends != null)
            TextButton(onClick = openTrends) { Text("Distance, pace and time trends") }
    }
}

private fun formatMinutes(seconds: Double): String {
    val minutes = (seconds / 60).roundToInt()
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

/** A connected button group, as in Material 3 Expressive, for the chart's measure. */
@Composable
private fun MetricSwitch(selected: RunningTrendMetric, choose: (RunningTrendMetric) -> Unit) {
    val options =
        listOf(
            RunningTrendMetric.DISTANCE to "Distance",
            RunningTrendMetric.TIME to "Time",
            RunningTrendMetric.PACE to "Pace",
        )
    // The segmented row is used only when every label, with the selected checkmark and padding,
    // fits its third of the width. At 150% text and above, or on a narrow screen, the same choices
    // wrap as full-size chips, so no label is squeezed and each target stays at least 48 dp.
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val longest = options.maxOf { (_, label) -> measurer.measure(label, style).size.width }
        val segment = with(density) { longest.toDp() } + 18.dp + 8.dp + 24.dp + 4.dp
        val wrap = density.fontScale >= 1.5f || segment * options.size > maxWidth
        MetricOptions(options, selected, choose, wrap)
    }
}

@Composable
private fun MetricOptions(
    options: List<Pair<RunningTrendMetric, String>>,
    selected: RunningTrendMetric,
    choose: (RunningTrendMetric) -> Unit,
    wrap: Boolean,
) {
    if (wrap) {
        FlowRow(
            Modifier.fillMaxWidth().testTag("trend-metric-chips"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (metric, label) ->
                CoachFilterChip(
                    selected = metric == selected,
                    onClick = { choose(metric) },
                    label = { Text(label) },
                    modifier = Modifier.testTag("trend-metric-${metric.name}"),
                )
            }
        }
        return
    }
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (metric, label) ->
            SegmentedButton(
                selected = metric == selected,
                onClick = { choose(metric) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.heightIn(min = 48.dp).testTag("trend-metric-${metric.name}"),
            ) {
                Text(label)
            }
        }
    }
}

/**
 * Bars for totals (distance, time), a connected line for pace with faster at the top. Weeks with no
 * runs show a baseline dot; unavailable weeks a hollow ring. Tap or drag to select a week; each
 * week is also a separate accessible element.
 */
@Composable
private fun TrendChart(
    points: List<RunningTrendPoint>,
    metric: RunningTrendMetric,
    selectedIndex: Int,
    valueText: (Double?) -> String,
    shortDate: (LocalDate) -> String,
    select: (Int) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val muted = accent.copy(alpha = 0.35f)
    val outline = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant
    val values = points.mapNotNull { it.value?.takeIf { v -> v > 0 } }
    val pace = metric == RunningTrendMetric.PACE
    val top = (values.maxOrNull() ?: 1.0) * (if (pace) 1.05 else 1.0)
    val bottom = if (pace) (values.minOrNull() ?: 0.0) * 0.95 else 0.0
    val height = if (LocalDensity.current.fontScale >= 1.5f) 220.dp else 180.dp
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Box(Modifier.weight(1f).fillMaxHeight().testTag("running-trend-chart")) {
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(points.size) {
                            detectTapGestures { offset ->
                                select(indexAt(offset.x, size.width.toFloat(), points.size))
                            }
                        }
                        .pointerInput(points.size) {
                            detectHorizontalDragGestures { change, _ ->
                                select(
                                    indexAt(change.position.x, size.width.toFloat(), points.size)
                                )
                            }
                        }
                ) {
                    val slot = size.width / points.size
                    val chartTop = 8.dp.toPx()
                    val chartHeight = size.height - chartTop - 6.dp.toPx()
                    fun y(value: Double): Float {
                        val span = (top - bottom).takeIf { it > 0 } ?: 1.0
                        val share = ((value - bottom) / span).toFloat().coerceIn(0f, 1f)
                        // Pace: lower seconds per km is faster, drawn higher.
                        return if (pace) chartTop + chartHeight * share
                        else chartTop + chartHeight * (1f - share)
                    }
                    val baseline = chartTop + chartHeight
                    for (fraction in listOf(0f, 0.5f, 1f)) {
                        val gy = chartTop + chartHeight * fraction
                        drawLine(
                            grid,
                            Offset(0f, gy),
                            Offset(size.width, gy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                    }
                    // Selection guide, as a scrubbing cursor.
                    val sx = slot * selectedIndex + slot / 2
                    drawLine(grid, Offset(sx, chartTop), Offset(sx, baseline), 2.dp.toPx())
                    val linePath = Path()
                    var open = false
                    points.forEachIndexed { index, point ->
                        val cx = slot * index + slot / 2
                        val value = point.value?.takeIf { it > 0 }
                        val chosen = index == selectedIndex
                        when {
                            point.week.activityCount() == 0 ->
                                drawCircle(outline, 3.dp.toPx(), Offset(cx, baseline))
                            value == null ->
                                drawCircle(
                                    outline,
                                    5.dp.toPx(),
                                    Offset(cx, baseline - 6.dp.toPx()),
                                    style = Stroke(1.5.dp.toPx()),
                                )
                            pace -> {
                                val py = y(value)
                                if (open) linePath.lineTo(cx, py) else linePath.moveTo(cx, py)
                                open = true
                            }
                            else -> {
                                val barWidth = slot * 0.58f
                                val by = y(value)
                                drawRoundRect(
                                    if (chosen) accent else muted,
                                    Offset(cx - barWidth / 2, by),
                                    Size(barWidth, baseline - by),
                                    CornerRadius(barWidth / 2.5f),
                                )
                            }
                        }
                        if (pace && value == null) open = false
                    }
                    if (pace) {
                        drawPath(
                            linePath,
                            muted,
                            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
                        )
                        points.forEachIndexed { index, point ->
                            val value = point.value?.takeIf { it > 0 } ?: return@forEachIndexed
                            val c = Offset(slot * index + slot / 2, y(value))
                            if (index == selectedIndex) {
                                drawCircle(accent.copy(alpha = 0.2f), 12.dp.toPx(), c)
                                drawCircle(accent, 6.dp.toPx(), c)
                            } else drawCircle(accent, 3.5.dp.toPx(), c)
                        }
                    }
                }
                // One accessible, selectable element per week over the drawing.
                Row(Modifier.fillMaxSize()) {
                    points.forEachIndexed { index, point ->
                        val start = requireNotNull(point.week.weekStart())
                        Box(
                            Modifier.weight(1f).fillMaxHeight().semantics {
                                contentDescription =
                                    "Week of ${shortDate(start)}: " +
                                        if (point.week.activityCount() == 0) "no recorded runs"
                                        else
                                            "${valueText(point.value)}, ${plural(point.week.activityCount(), "run")}"
                                selected = index == selectedIndex
                                role = Role.Tab
                                onClick {
                                    select(index)
                                    true
                                }
                            }
                        )
                    }
                }
            }
            // Quiet scale on the right: top and bottom values only.
            Column(
                Modifier.fillMaxHeight().padding(start = 8.dp).clearAndSetSemantics {},
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    if (pace) valueText(bottom.takeIf { it > 0 }) else valueText(top),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (pace) valueText(top) else "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                shortDate(requireNotNull(points.first().week.weekStart())),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "This week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (pace) "Tap or drag to inspect a week. Higher is faster."
            else "Tap or drag to inspect a week.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int =
    ((x / width) * count).toInt().coerceIn(0, count - 1)
