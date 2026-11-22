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
 * Chart-first running analysis, shared by Progress and the full Training trends screen. One
 * surface: a readout (the selected week's figure, large), a measure switch, an interactive weekly
 * chart and the selected week's runs as a drill-down. It draws the existing weekly trend points
 * only; no figure is computed here. Heart rate ([heartRate]) shows each run's own recorded average,
 * never a combined weekly value.
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
    heartRate: Boolean = false,
) {
    if (weeks.isEmpty()) return
    var metricName by rememberSaveable { mutableStateOf(RunningTrendMetric.DISTANCE.name) }
    val metric =
        RunningTrendMetric.valueOf(metricName).let {
            if (it == RunningTrendMetric.HEART_RATE && !heartRate) RunningTrendMetric.DISTANCE
            else it
        }
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
    val heart = metric == RunningTrendMetric.HEART_RATE
    fun valueText(value: Double?): String =
        when {
            value == null -> "Unavailable"
            metric == RunningTrendMetric.DISTANCE -> distance(value)
            metric == RunningTrendMetric.TIME -> formatMinutes(value)
            heart -> "${value.roundToInt()} bpm"
            else -> "${paceSeconds(value * if (imperial) 1.609344 else 1.0)} /$unit"
        }
    fun pointText(point: RunningTrendPoint): String =
        when {
            point.week.activityCount() == 0 -> "No runs"
            heart -> heartRateRange(point.heartRates) ?: "Not recorded"
            point.value == null -> "Not recorded"
            else -> valueText(point.value)
        }
    val select: (Int) -> Unit = { selectedWeek = weeks[it].weekStart().toString() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricSwitch(metric, heartRate) { metricName = it.name }

        if (heart && points.all { it.heartRates.isEmpty() }) {
            // Nothing to draw: say so plainly rather than drawing an empty or zero chart.
            Text(
                if (points.all { it.week.activityCount() == 0 }) "No runs in this period."
                else
                    "No heart-rate summaries in this period. Runs recorded with a heart-rate " +
                        "sensor, or imported with heart rate, appear here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("heart-rate-empty"),
            )
            return@Column
        }

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
                pointText(point),
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
                        append(
                            if (heart)
                                " · heart rate on ${point.measuredRuns} of ${week.activityCount()}"
                            else " · based on ${point.measuredRuns} of ${week.activityCount()}"
                        )
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag("trend-readout-sample"),
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

        TrendChart(points, metric, selected, ::valueText, ::pointText, shortDate, imperial, select)

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
                    value =
                        if (heart)
                            run.averageHeartRate()?.let { "${it.roundToInt()} bpm" }
                                ?: "No heart rate"
                        else run.distanceMetres()?.let(distance) ?: "No distance",
                    secondary =
                        if (heart)
                            run.averageHeartRate()?.let { _ ->
                                "average" +
                                    (run.maxHeartRate()?.let { " · max ${it.roundToInt()}" } ?: "")
                            }
                        else null,
                    onClick = { openRun(run.id()) },
                )
            }
        }
        if (openTrends != null)
            TextButton(onClick = openTrends) { Text("Distance, pace and time trends") }
    }
}

/** "152 bpm" for one run, "142–151 bpm" for the lowest to highest recorded run averages. */
private fun heartRateRange(rates: List<Double>): String? {
    if (rates.isEmpty()) return null
    val low = rates.min().roundToInt()
    val high = rates.max().roundToInt()
    return if (low == high) "$low bpm" else "$low–$high bpm"
}

private fun formatMinutes(seconds: Double): String {
    val minutes = (seconds / 60).roundToInt()
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

/** A connected button group, as in Material 3 Expressive, for the chart's measure. */
@Composable
private fun MetricSwitch(
    selected: RunningTrendMetric,
    heartRate: Boolean,
    choose: (RunningTrendMetric) -> Unit,
) {
    val options =
        listOf(
            RunningTrendMetric.DISTANCE to "Distance",
            RunningTrendMetric.TIME to "Time",
            RunningTrendMetric.PACE to "Pace",
        ) + if (heartRate) listOf(RunningTrendMetric.HEART_RATE to "Heart rate") else emptyList()
    // The segmented row is used only when every label, with the selected checkmark and padding,
    // fits its share of the width. At 150% text and above, or on a narrow screen, the same choices
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

/** Weeks above this count use the dense long-range presentation with month labels. */
private const val DENSE_WEEKS = 12

/**
 * Bars for totals (distance, time); a connected line for pace with faster at the top; for heart
 * rate, a bar spanning the week's recorded run averages with one dot per run. Weeks with no runs
 * show a baseline dot; weeks whose runs lack the measure show a hollow ring, so missing data is
 * never drawn as zero. Tap, drag or step to select a week; each week is also an accessible element.
 */
@Composable
private fun TrendChart(
    points: List<RunningTrendPoint>,
    metric: RunningTrendMetric,
    selectedIndex: Int,
    valueText: (Double?) -> String,
    pointText: (RunningTrendPoint) -> String,
    shortDate: (LocalDate) -> String,
    imperial: Boolean,
    select: (Int) -> Unit,
) {
    // Each measure has its own hue (shared chart tokens): the selected week in the strong tone
    // with a halo, a marker above it and a guide line; other weeks in the softer, related tone.
    // Both tones keep at least 3:1 contrast with the surface in light and dark themes.
    val tones = chartAccent(metric.hue)
    val accent = tones.strong
    val muted = tones.soft
    val halo = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val outline = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant
    val pace = metric == RunningTrendMetric.PACE
    val heart = metric == RunningTrendMetric.HEART_RATE
    val dense = points.size > DENSE_WEEKS
    val values =
        if (heart) points.flatMap { it.heartRates }
        else points.mapNotNull { it.value?.takeIf { v -> v > 0 } }
    val top =
        when {
            heart -> (values.maxOrNull() ?: 1.0) + 5
            pace -> (values.maxOrNull() ?: 1.0) * 1.05
            else -> values.maxOrNull() ?: 1.0
        }
    val bottom =
        when {
            heart -> ((values.minOrNull() ?: 0.0) - 5).coerceAtLeast(0.0)
            pace -> (values.minOrNull() ?: 0.0) * 0.95
            else -> 0.0
        }
    fun hasMeasure(point: RunningTrendPoint) =
        if (heart) point.heartRates.isNotEmpty() else (point.value ?: 0.0) > 0
    fun describe(point: RunningTrendPoint) =
        "Week of ${shortDate(requireNotNull(point.week.weekStart()))}: " +
            when {
                point.week.activityCount() == 0 -> "no recorded runs"
                !hasMeasure(point) ->
                    "${metric.label.lowercase()} not recorded, ${plural(point.week.activityCount(), "run")}"
                else -> "${pointText(point)}, ${plural(point.week.activityCount(), "run")}"
            }
    val height = if (LocalDensity.current.fontScale >= 1.5f) 220.dp else 180.dp
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(height)
                        .testTag("running-trend-chart")
                        .semantics { stateDescription = describe(points[selectedIndex]) }
                ) {
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
                                        indexAt(
                                            change.position.x,
                                            size.width.toFloat(),
                                            points.size,
                                        )
                                    )
                                }
                            }
                    ) {
                        val slot = size.width / points.size
                        val chartTop = 14.dp.toPx()
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
                        // Selection guide, as a scrubbing cursor, with a marker at the top.
                        val sx = slot * selectedIndex + slot / 2
                        drawLine(
                            accent.copy(alpha = 0.45f),
                            Offset(sx, chartTop),
                            Offset(sx, baseline),
                            2.dp.toPx(),
                        )
                        drawCircle(halo, 6.dp.toPx(), Offset(sx, chartTop - 7.dp.toPx()))
                        drawCircle(accent, 4.5.dp.toPx(), Offset(sx, chartTop - 7.dp.toPx()))
                        val barWidth =
                            (slot * if (dense) 0.72f else 0.58f).coerceAtMost(40.dp.toPx())
                        val radius =
                            if (dense) (barWidth / 2.5f).coerceAtMost(3.dp.toPx())
                            else barWidth / 2.5f
                        val linePath = Path()
                        var open = false
                        points.forEachIndexed { index, point ->
                            val cx = slot * index + slot / 2
                            val value = point.value?.takeIf { it > 0 }
                            val chosen = index == selectedIndex
                            val ink = if (chosen) accent else muted
                            when {
                                point.week.activityCount() == 0 ->
                                    drawCircle(outline, 3.dp.toPx(), Offset(cx, baseline))
                                !hasMeasure(point) ->
                                    drawCircle(
                                        outline,
                                        (if (dense) 3.5f else 5f).dp.toPx(),
                                        Offset(cx, baseline - 6.dp.toPx()),
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                heart -> {
                                    val low = y(point.heartRates.min())
                                    val high = y(point.heartRates.max())
                                    val w = (slot * 0.36f).coerceIn(4.dp.toPx(), 10.dp.toPx())
                                    drawRoundRect(
                                        ink.copy(alpha = if (chosen) 0.35f else 0.25f),
                                        Offset(cx - w / 2, high - w / 2),
                                        Size(w, low - high + w),
                                        CornerRadius(w / 2),
                                    )
                                    point.heartRates.forEach {
                                        val c = Offset(cx, y(it))
                                        if (chosen)
                                            drawCircle(
                                                halo,
                                                5.5.dp.toPx(),
                                                c,
                                                style = Stroke(1.5.dp.toPx()),
                                            )
                                        drawCircle(ink, (if (chosen) 4f else 3f).dp.toPx(), c)
                                    }
                                }
                                pace -> {
                                    val py = y(requireNotNull(value))
                                    if (open) linePath.lineTo(cx, py) else linePath.moveTo(cx, py)
                                    open = true
                                }
                                else -> {
                                    val by = y(requireNotNull(value))
                                    if (chosen) {
                                        // An outline as well as colour, so selection never relies
                                        // on hue alone (and stands out in dark mode).
                                        val o = 2.5.dp.toPx()
                                        drawRoundRect(
                                            halo,
                                            Offset(cx - barWidth / 2 - o, by - o),
                                            Size(barWidth + o * 2, baseline - by + o),
                                            CornerRadius(radius + o),
                                            style = Stroke(1.5.dp.toPx()),
                                        )
                                    }
                                    drawRoundRect(
                                        ink,
                                        Offset(cx - barWidth / 2, by),
                                        Size(barWidth, baseline - by),
                                        CornerRadius(radius),
                                    )
                                }
                            }
                            if (pace && value == null) open = false
                        }
                        if (pace) {
                            drawPath(
                                linePath,
                                muted,
                                style =
                                    Stroke((if (dense) 2f else 3f).dp.toPx(), cap = StrokeCap.Round),
                            )
                            points.forEachIndexed { index, point ->
                                val value = point.value?.takeIf { it > 0 } ?: return@forEachIndexed
                                val c = Offset(slot * index + slot / 2, y(value))
                                if (index == selectedIndex) {
                                    drawCircle(accent.copy(alpha = 0.25f), 12.dp.toPx(), c)
                                    drawCircle(
                                        halo,
                                        7.5.dp.toPx(),
                                        c,
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                    drawCircle(accent, 6.dp.toPx(), c)
                                } else if (!dense) drawCircle(muted, 3.5.dp.toPx(), c)
                            }
                        }
                    }
                    // One accessible, selectable element per week over the drawing.
                    Row(Modifier.fillMaxSize()) {
                        points.forEachIndexed { index, point ->
                            Box(
                                Modifier.weight(1f).fillMaxHeight().semantics {
                                    contentDescription = describe(point)
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
                ChartDates(points, dense, shortDate)
            }
            // Quiet scale on the right: top and bottom values only.
            Column(
                Modifier.height(height).padding(start = 8.dp).clearAndSetSemantics {},
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                val upper = if (pace) bottom.takeIf { it > 0 } else top
                val lower = if (pace) top else bottom.takeIf { heart }
                Text(
                    valueText(upper),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    lower?.let(valueText) ?: "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        ChartLegend(points, ::hasMeasure)
        // Precise, accessible selection for dense charts and anyone who prefers buttons to drags.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { select(selectedIndex - 1) },
                enabled = selectedIndex > 0,
                modifier = Modifier.heightIn(min = 48.dp).testTag("trend-earlier"),
            ) {
                Text("‹ Earlier week")
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { select(selectedIndex + 1) },
                enabled = selectedIndex < points.lastIndex,
                modifier = Modifier.heightIn(min = 48.dp).testTag("trend-later"),
            ) {
                Text("Later week ›")
            }
        }
        Text(
            when {
                pace -> "Higher is faster: less time per ${if (imperial) "mile" else "kilometre"}."
                heart -> "Each bar spans that week's recorded run averages; each dot is one run."
                else -> "Tap or drag the chart to inspect a week."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Dates under the chart. Short ranges show the first week and "This week"; long ranges show a label
 * at each month's first week, skipping labels that would collide, with the year at January and at
 * the start.
 */
@Composable
private fun ChartDates(
    points: List<RunningTrendPoint>,
    dense: Boolean,
    shortDate: (LocalDate) -> String,
) {
    val style = MaterialTheme.typography.labelSmall
    val colour = MaterialTheme.colorScheme.onSurfaceVariant
    if (!dense) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                shortDate(requireNotNull(points.first().week.weekStart())),
                style = style,
                color = colour,
            )
            Text("This week", style = style, color = colour)
        }
        return
    }
    val starts = points.map { requireNotNull(it.week.weekStart()) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("trend-month-labels")
            .clearAndSetSemantics {}
    ) {
        val total = with(density) { maxWidth.toPx() }
        val marks =
            remember(starts, total, style, density.fontScale) {
                ChartTimeline.monthMarks(starts, total, with(density) { 6.dp.toPx() }) {
                    measurer.measure(it, style).size.width.toFloat()
                }
            }
        for (mark in marks) Text(
            mark.label,
            style = style,
            color = colour,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(x = with(density) { mark.x.toDp() }),
        )
    }
}

/** A small key, shown only when the chart has weeks without runs or without the measure. */
@Composable
private fun ChartLegend(
    points: List<RunningTrendPoint>,
    hasMeasure: (RunningTrendPoint) -> Boolean,
) {
    val noRuns = points.any { it.week.activityCount() == 0 }
    val missing = points.any { it.week.activityCount() > 0 && !hasMeasure(it) }
    if (!noRuns && !missing) return
    val outline = MaterialTheme.colorScheme.outline
    FlowRow(
        Modifier.fillMaxWidth().testTag("trend-legend").clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (noRuns)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(10.dp)) { drawCircle(outline, 3.dp.toPx()) }
                Spacer(Modifier.width(6.dp))
                Text("No runs", style = MaterialTheme.typography.labelSmall)
            }
        if (missing)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) {
                    drawCircle(outline, 4.5.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
                Spacer(Modifier.width(6.dp))
                Text("Not recorded", style = MaterialTheme.typography.labelSmall)
            }
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int =
    ((x / width) * count).toInt().coerceIn(0, count - 1)
