package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.RecordedWeek

internal enum class RunningTrendMetric(val label: String, val heading: String) {
    DISTANCE("Distance", "Weekly recorded distance"),
    PACE("Pace", "Weekly elapsed average pace"),
    TIME("Training time", "Weekly recorded running time"),
}

/** Derived presentation only; this never replaces an activity's historical totals. */
internal data class RunningTrendPoint(
    val week: RecordedWeek,
    val value: Double?,
    val measuredRuns: Int,
)

internal fun runningTrendPoints(
    weeks: List<RecordedWeek>,
    activities: List<ActivityRecord>,
    metric: RunningTrendMetric,
    today: LocalDate,
    zone: ZoneId,
): List<RunningTrendPoint> =
    weeks.map { week ->
        val runs =
            activities.filter {
                val date = it.start().atZone(zone).toLocalDate()
                !date.isBefore(week.weekStart()) &&
                    date.isBefore(week.weekStart().plusWeeks(1)) &&
                    !date.isAfter(today)
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
                    runs.takeIf { it.isNotEmpty() }?.sumOf { it.elapsedTime().toMillis() / 1000.0 },
                    runs.size,
                )
            RunningTrendMetric.PACE -> {
                val measured = runs.filter { (it.distanceMetres() ?: 0.0) > 0.0 }
                val metres = measured.sumOf { it.distanceMetres()!! }
                RunningTrendPoint(
                    week,
                    if (metres > 0)
                        measured.sumOf { it.elapsedTime().toMillis() / 1000.0 } / (metres / 1000.0)
                    else null,
                    measured.size,
                )
            }
        }
    }

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
    val records = remember(activities) { activities.sortedBy { it.start() } }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected =
        records.firstOrNull { it.id() == selectedId }
            ?: records.lastOrNull { it.averageHeartRate() != null || it.maxHeartRate() != null }
            ?: records.lastOrNull()
    val selectedIndex = records.indexOf(selected)
    CoachCard {
        Text("Recorded heart rate", style = MaterialTheme.typography.titleLarge)
        Text(
            "${records.count { it.averageHeartRate() != null || it.maxHeartRate() != null }} of ${activities.size} runs in this range include a saved heart-rate summary.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (selected == null) {
            Text("No heart-rate measurements recorded in this range.")
            Text(
                "You can keep running without a heart-rate sensor. Missing readings stay unavailable."
            )
        } else {
            Text(
                friendlyDate(selected.start().atZone(ZoneId.systemDefault()).toLocalDate()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Selected recorded run · ${selected.distanceMetres()?.let { formatDistance(it, state) } ?: "distance unavailable"}"
            )
            if (selected.averageHeartRate() == null && selected.maxHeartRate() == null)
                Text("No heart-rate measurements recorded for this run.")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2,
            ) {
                HeartRateMetric("Average heart rate", selected.averageHeartRate())
                HeartRateMetric("Maximum heart rate", selected.maxHeartRate())
            }
            Text(
                "Source: ${selected.source().provider()}. These are this run's saved values, not a range-wide average.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { selectedId = records[selectedIndex - 1].id() },
                    enabled = selectedIndex > 0,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("View earlier run")
                }
                OutlinedButton(
                    onClick = { selectedId = records[selectedIndex + 1].id() },
                    enabled = selectedIndex >= 0 && selectedIndex < records.lastIndex,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("View later run")
                }
            }
            val recent = records.takeLast(6)
            HorizontalDivider()
            Text("Recent recorded readings", style = MaterialTheme.typography.titleMedium)
            HeartRateHistoryChart(recent, selected.id()) { selectedId = it }
            recent.forEach { run ->
                val date = friendlyDate(run.start().atZone(ZoneId.systemDefault()).toLocalDate())
                TextButton(
                    onClick = { selectedId = run.id() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(date, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Average ${run.averageHeartRate()?.let { "${it.toInt()} bpm" } ?: "unavailable"} · maximum ${run.maxHeartRate()?.let { "${it.toInt()} bpm" } ?: "unavailable"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
    CoachCard {
        Text("Heart-rate zones unavailable", style = MaterialTheme.typography.titleLarge)
        Text(
            "Time in zones and pace by zone need personal zone boundaries and reliable timed heart-rate samples."
        )
        Text(
            "Those requirements are not established in this overview. Average or maximum heart rate alone cannot supply them."
        )
        WhyDetails(
            "This page uses saved activity summaries and does not load every run's raw sensor history. It does not assume an age-based maximum, invent zone boundaries, fill recording gaps or turn missing heart rate into zero.",
            "About heart-rate evidence",
        )
    }
}

@Composable
private fun HeartRateMetric(label: String, value: Double?) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            value?.let { "${it.toInt()} bpm" } ?: "Unavailable",
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
private fun HeartRateHistoryChart(
    runs: List<ActivityRecord>,
    selected: String,
    select: (String) -> Unit,
) {
    if (runs.none { it.averageHeartRate() != null || it.maxHeartRate() != null }) return
    val averageColour = MaterialTheme.colorScheme.primary
    val maxColour = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maximum =
        runs
            .flatMap { listOfNotNull(it.averageHeartRate(), it.maxHeartRate()) }
            .maxOrNull()!!
            .coerceAtLeast(1.0)
    Text("Average and maximum · beats per minute", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("● Average", color = averageColour, style = MaterialTheme.typography.labelMedium)
        Text("● Maximum", color = maxColour, style = MaterialTheme.typography.labelMedium)
    }
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.fillMaxHeight().width(52.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${maximum.toInt()}", style = MaterialTheme.typography.labelSmall)
            Text("${(maximum / 2).toInt()}", style = MaterialTheme.typography.labelSmall)
            Text("0", style = MaterialTheme.typography.labelSmall)
        }
        Canvas(
            Modifier.weight(1f)
                .fillMaxHeight()
                .testTag("running-heart-rate-chart")
                .semantics {
                    contentDescription =
                        "Saved run heart-rate summaries. " +
                            runs.joinToString("; ") {
                                "${friendlyDate(it.start().atZone(ZoneId.systemDefault()).toLocalDate())}: average ${it.averageHeartRate()?.toInt() ?: "unavailable"}, maximum ${it.maxHeartRate()?.toInt() ?: "unavailable"} beats per minute"
                            }
                    customActions =
                        runs.map { run ->
                            CustomAccessibilityAction(
                                "Select run ${friendlyDate(run.start().atZone(ZoneId.systemDefault()).toLocalDate())}"
                            ) {
                                select(run.id())
                                true
                            }
                        }
                }
                .pointerInput(runs) {
                    detectTapGestures { point ->
                        select(
                            runs[(point.x / size.width * runs.size).toInt().coerceIn(runs.indices)]
                                .id()
                        )
                    }
                }
        ) {
            val bottom = size.height - 8.dp.toPx()
            val top = 4.dp.toPx()
            val cell = size.width / runs.size
            repeat(3) { i ->
                val y = top + (bottom - top) * i / 2
                drawLine(grid, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            runs.forEachIndexed { index, run ->
                val centre = cell * (index + .5f)
                val width = (cell * .28f).coerceAtMost(18.dp.toPx())
                listOf(run.averageHeartRate() to averageColour, run.maxHeartRate() to maxColour)
                    .forEachIndexed { series, (value, colour) ->
                        val x = centre + if (series == 0) -width - 1.dp.toPx() else 1.dp.toPx()
                        if (value != null) {
                            val height = (value / maximum).toFloat() * (bottom - top)
                            drawRoundRect(
                                colour,
                                Offset(x, bottom - height),
                                Size(width, height),
                                CornerRadius(width / 2),
                            )
                            if (run.id() == selected)
                                drawRoundRect(
                                    outline,
                                    Offset(x, bottom - height),
                                    Size(width, height),
                                    CornerRadius(width / 2),
                                    style = Stroke(1.dp.toPx()),
                                )
                        } else {
                            drawLine(
                                outline,
                                Offset(x, bottom - 5.dp.toPx()),
                                Offset(x + width, bottom),
                                1.dp.toPx(),
                            )
                            drawLine(
                                outline,
                                Offset(x, bottom),
                                Offset(x + width, bottom - 5.dp.toPx()),
                                1.dp.toPx(),
                            )
                        }
                    }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            shortDate(runs.first().start().atZone(ZoneId.systemDefault()).toLocalDate()),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            shortDate(runs.last().start().atZone(ZoneId.systemDefault()).toLocalDate()),
            style = MaterialTheme.typography.labelSmall,
        )
    }
    Text(
        "Each pair is one run · missing readings are crossed, never zero · tap to select",
        style = MaterialTheme.typography.bodySmall,
    )
}
