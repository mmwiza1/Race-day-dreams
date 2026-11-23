@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.strength.StrengthRecords
import org.racedaydreams.domain.strength.StrengthVolume
import org.racedaydreams.domain.time.LocalDate

/*
 * V3 strength progression: the same chart-first pattern as running trends (readout, interactive
 * weekly chart, steppers, drill-down) in the plum strength hue. Everything drawn comes from
 * DashboardAnalytics.Strength; recorded lifts are filled marks, estimates hollow marks.
 */

private fun index(selected: String?, weeks: List<LocalDate>, fallback: Int): Int =
    weeks.indexOfFirst { it.toString() == selected }.takeIf { it >= 0 } ?: fallback

/** The period's exercises with qualifying sets, as a scrollable row of chips (most sets first). */
@Composable
private fun ExerciseChips(
    options: List<Pair<String, String>>,
    selected: String,
    select: (String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("exercise-selector"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for ((id, name) in options) CoachFilterChip(
            selected = id == selected,
            onClick = { select(id) },
            label = { Text(name) },
            modifier = Modifier.testTag("exercise-$id"),
        )
    }
}

/** 48 dp previous/next controls shared by the strength charts. */
@Composable
private fun Steppers(tag: String, selected: Int, last: Int, select: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { select(selected - 1) },
            enabled = selected > 0,
            modifier = Modifier.heightIn(min = 48.dp).testTag("$tag-earlier"),
        ) {
            Text("‹ Earlier week")
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { select(selected + 1) },
            enabled = selected < last,
            modifier = Modifier.heightIn(min = 48.dp).testTag("$tag-later"),
        ) {
            Text("Later week ›")
        }
    }
}

/** One selectable, described element per week over a chart, plus tap and drag selection. */
@Composable
private fun WeekTargets(
    count: Int,
    selected: Int,
    describe: (Int) -> String,
    select: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxSize()
            .pointerInput(count) {
                detectTapGestures {
                    select(((it.x / size.width) * count).toInt().coerceIn(0, count - 1))
                }
            }
            .pointerInput(count) {
                detectHorizontalDragGestures { change, _ ->
                    select(
                        ((change.position.x / size.width) * count).toInt().coerceIn(0, count - 1)
                    )
                }
            }
    ) {
        for (i in 0 until count) Box(
            Modifier.weight(1f).fillMaxHeight().semantics {
                contentDescription = describe(i)
                this.selected = i == selected
                role = Role.Tab
                onClick {
                    select(i)
                    true
                }
            }
        )
    }
}

private fun DrawScope.selectionGuide(
    x: Float,
    top: Float,
    bottom: Float,
    accent: Color,
    halo: Color,
) {
    drawLine(accent.copy(alpha = 0.45f), Offset(x, top), Offset(x, bottom), 2.dp.toPx())
    drawCircle(halo, 6.dp.toPx(), Offset(x, top - 7.dp.toPx()))
    drawCircle(accent, 4.5.dp.toPx(), Offset(x, top - 7.dp.toPx()))
}

private fun DrawScope.grid(top: Float, bottom: Float, colour: Color) {
    for (fraction in listOf(0f, 0.5f, 1f)) {
        val y = top + (bottom - top) * fraction
        drawLine(
            colour,
            Offset(0f, y),
            Offset(size.width, y),
            1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
        )
    }
}

@Composable
private fun Scale(upper: String, lower: String, height: androidx.compose.ui.unit.Dp) {
    Column(
        Modifier.height(height).padding(start = 8.dp).clearAndSetSemantics {},
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            upper,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            lower,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun chartHeight() = if (LocalDensity.current.fontScale >= 1.5f) 220.dp else 180.dp

/** A filled (recorded) or hollow (estimated) mark with its label, for legends and readouts. */
@Composable
private fun Mark(filled: Boolean, colour: Color) {
    Canvas(Modifier.size(12.dp)) {
        if (filled) drawCircle(colour, 5.dp.toPx())
        else drawCircle(colour, 4.5.dp.toPx(), style = Stroke(2.dp.toPx()))
    }
}

/**
 * Exercise progression: the heaviest recorded working set per week (filled marks, solid line) and
 * the best existing estimated one-repetition maximum per week (hollow marks, dashed line), on one
 * mass axis but always labelled as different things. Lines join only weeks that have that measure,
 * and nothing is drawn as a trend until at least two weeks have data.
 */
@Composable
internal fun ExerciseProgressionCard(
    data: StrengthPeriod,
    mass: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    selectExercise: (String) -> Unit,
    openWorkout: (String) -> Unit,
) =
    ExpressiveSection(
        "Exercise progression",
        Glyph.TREND,
        ChartHue.PLUM,
        "Recorded lifts and estimated 1RM, week by week",
    ) {
        val overview = data.overview
        val exercise = overview.trendExercise
        if (exercise == null) {
            Text(
                "Record weighted working sets of 2–10 repetitions in this period to see an estimate.",
                modifier = Modifier.testTag("one-rep-max-empty"),
            )
            return@ExpressiveSection
        }
        ExerciseChips(
            overview.eligibleExercises.map { it.id() to it.name() },
            exercise.id(),
            selectExercise,
        )
        val points = overview.oneRepMaxTrend
        val heaviest =
            overview.heaviestByWeek.takeIf { it.size == points.size } ?: List(points.size) { null }
        val weeks = points.map { it.weekStart }
        val withData =
            points.indices.filter { points[it].kilograms != null || heaviest[it] != null }
        var selectedWeek by
            rememberSaveable(exercise.id(), weeks.firstOrNull().toString()) {
                mutableStateOf<String?>(null)
            }
        val selected = index(selectedWeek, weeks, withData.lastOrNull() ?: points.lastIndex)
        val select: (Int) -> Unit = {
            selectedWeek = weeks[it.coerceIn(0, weeks.lastIndex)].toString()
        }
        val point = points[selected]
        val set = heaviest[selected]
        val tones = chartAccent(ChartHue.PLUM)

        // Readout: the selected week's actual heaviest set first, then the estimate, never merged.
        Column(
            Modifier.fillMaxWidth().testTag("selected-one-rep-max").semantics(
                mergeDescendants = true
            ) {
                liveRegion = LiveRegionMode.Polite
            },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Week of ${friendlyDate(point.weekStart)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mark(true, tones.strong)
                Text(
                    set?.let { "${mass(it.kilograms)} × ${it.reps}" } ?: "No recorded set",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("Heaviest set · recorded", style = MaterialTheme.typography.bodyMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Mark(false, tones.strong)
                Text(
                    point.kilograms?.let { "${mass(it)} estimated" } ?: "No estimate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val setKilograms = point.setKilograms
            val reps = point.reps
            Text(
                if (point.kilograms == null) "Estimated 1RM · no qualifying set this week"
                else
                    "Estimated 1RM · from ${setKilograms?.let(mass) ?: "a set"}" +
                        (reps?.let { " × $it" } ?: "") +
                        " · ${plural(point.qualifyingSets, "qualifying set")}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (withData.size < 2) {
            // One week cannot show a direction: no line, axis or range that suggests one.
            Text(
                "Only one week has data in this period. A trend appears once a second week is recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("one-rep-max-single-week"),
            )
        } else {
            ProgressionChart(points, heaviest, selected, tones, mass, shortDate, select)
            Steppers("progression", selected, points.lastIndex, select)
        }
        // Open the workout behind each figure; one button when both come from the same workout.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val setSession = set?.sessionId
            val estimateSession = point.sessionId
            if (setSession != null && setSession == estimateSession)
                TextButton(
                    onClick = { openWorkout(setSession) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Open workout")
                }
            else {
                if (setSession != null)
                    TextButton(
                        onClick = { openWorkout(setSession) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Open heaviest-set workout")
                    }
                if (estimateSession != null)
                    TextButton(
                        onClick = { openWorkout(estimateSession) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Open estimate workout")
                    }
            }
        }
        Explainer("How the estimate works", StrengthRecords.estimateExplanation())
    }

@Composable
private fun ProgressionChart(
    points: List<DashboardAnalytics.OneRepMaxPoint>,
    heaviest: List<DashboardAnalytics.RecordedSet?>,
    selected: Int,
    tones: ChartAccent,
    mass: (Double) -> String,
    shortDate: (LocalDate) -> String,
    select: (Int) -> Unit,
) {
    val values = points.mapNotNull { it.kilograms } + heaviest.mapNotNull { it?.kilograms }
    val low = (values.minOrNull() ?: 0.0) * 0.95
    val high = (values.maxOrNull() ?: 1.0) * 1.03
    val grid = MaterialTheme.colorScheme.outlineVariant
    val halo = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    val height = chartHeight()
    val dense = points.size > 12
    fun describe(i: Int): String {
        val set = heaviest[i]
        val estimate = points[i].kilograms
        return "Week of ${shortDate(points[i].weekStart)}: " +
            when {
                set == null && estimate == null -> "no qualifying sets"
                else ->
                    listOfNotNull(
                            set?.let {
                                "heaviest set ${mass(it.kilograms)} × ${it.reps}, recorded"
                            },
                            estimate?.let { "estimated 1RM ${mass(it)}" },
                        )
                        .joinToString("; ")
            }
    }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Box(
                Modifier.fillMaxWidth().height(height).testTag("one-rep-max-chart").semantics {
                    stateDescription = describe(selected)
                }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val slot = size.width / points.size
                    val top = 14.dp.toPx()
                    val bottom = size.height - 8.dp.toPx()
                    fun y(v: Double) =
                        (bottom - ((v - low) / (high - low).coerceAtLeast(1e-6)) * (bottom - top))
                            .toFloat()
                    fun x(i: Int) = slot * i + slot / 2
                    grid(top, bottom, grid)
                    selectionGuide(x(selected), top, bottom, tones.strong, halo)
                    // Recorded: solid line through weeks with a recorded set.
                    val recorded = Path()
                    var open = false
                    heaviest.forEachIndexed { i, s ->
                        if (s == null) return@forEachIndexed
                        if (open) recorded.lineTo(x(i), y(s.kilograms))
                        else recorded.moveTo(x(i), y(s.kilograms))
                        open = true
                    }
                    if (heaviest.count { it != null } >= 2)
                        drawPath(recorded, tones.soft, style = Stroke(2.5.dp.toPx()))
                    // Estimated: dashed line through weeks with an estimate.
                    val estimated = Path()
                    open = false
                    points.forEachIndexed { i, p ->
                        val v = p.kilograms ?: return@forEachIndexed
                        if (open) estimated.lineTo(x(i), y(v)) else estimated.moveTo(x(i), y(v))
                        open = true
                    }
                    if (points.count { it.kilograms != null } >= 2)
                        drawPath(
                            estimated,
                            tones.soft,
                            style =
                                Stroke(
                                    2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                                ),
                        )
                    val r = (if (dense) 3.5f else 5f).dp.toPx()
                    for (i in points.indices) {
                        val chosen = i == selected
                        val ink = if (chosen) tones.strong else tones.soft
                        heaviest[i]?.let {
                            val c = Offset(x(i), y(it.kilograms))
                            if (chosen)
                                drawCircle(halo, r + 3.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                            drawCircle(ink, r, c)
                        }
                        points[i].kilograms?.let {
                            val c = Offset(x(i), y(it))
                            if (chosen)
                                drawCircle(halo, r + 3.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                            drawCircle(surface, r, c)
                            drawCircle(ink, r, c, style = Stroke(2.dp.toPx()))
                        }
                    }
                }
                WeekTargets(points.size, selected, ::describe, select)
            }
            ChartDates(points.map { it.weekStart }, dense, shortDate)
        }
        Scale(mass(high), mass(low), height)
    }
    FlowRow(
        Modifier.fillMaxWidth().clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mark(true, tones.strong)
            Spacer(Modifier.width(6.dp))
            Text("Heaviest set (recorded)", style = MaterialTheme.typography.labelSmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Mark(false, tones.strong)
            Spacer(Modifier.width(6.dp))
            Text("Estimated 1RM (calculated)", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Training volume: accumulated load × repetitions of weighted working sets, by week, with the
 * workouts behind the selected week and the period's largest exercises. Never a weight lifted.
 */
@Composable
internal fun TrainingVolumeCard(
    data: StrengthPeriod,
    volume: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    openWorkout: ((String) -> Unit)?,
) =
    ExpressiveSection(
        "Training volume",
        Glyph.VOLUME,
        ChartHue.PLUM,
        "Load × repetitions of weighted sets — not the heaviest weight lifted",
    ) {
        val overview = data.overview
        val values = overview.volumeByWeek.takeIf { it.size == overview.weeks.size }
        if (values == null || values.all { it == null }) {
            Text(
                if (overview.workouts == 0) "No strength workouts recorded in this period."
                else
                    "No weighted load × repetition sets in this period, so there is no volume to show.",
                modifier = Modifier.testTag("volume-empty"),
            )
            return@ExpressiveSection
        }
        val weeks = overview.weeks.map { requireNotNull(it.monday()) }
        var selectedWeek by
            rememberSaveable(weeks.first().toString()) { mutableStateOf<String?>(null) }
        // Open on the latest week with workouts, so an empty start to this week is not the
        // headline.
        val latest =
            overview.weeks.indexOfLast { it.workouts() > 0 }.takeIf { it >= 0 } ?: weeks.lastIndex
        val selected = index(selectedWeek, weeks, latest)
        val select: (Int) -> Unit = {
            selectedWeek = weeks[it.coerceIn(0, weeks.lastIndex)].toString()
        }
        val week = overview.weeks[selected]
        val current = selected == weeks.lastIndex
        val total = values.filterNotNull().sum()

        Column(
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                if (current) "This week" else "Week of ${friendlyDate(weeks[selected])}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                values[selected]?.let(volume)
                    ?: if (week.workouts() == 0) "No workouts" else "No load × reps sets",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("volume-readout"),
            )
            Text(
                "${plural(week.workouts(), "workout")} · ${plural(week.workingSets(), "working set")}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        WeeklyBars(values, overview.weeks, selected, volume, shortDate, select)
        Steppers("volume", selected, weeks.lastIndex, select)
        Text(
            "${data.period.label}: ${volume(total)}" +
                (overview.previousVolume?.let { " · previous ${data.period.label}: ${volume(it)}" }
                    ?: " · no earlier ${data.period.label} to compare"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("volume-period"),
        )

        val workouts = overview.workoutsByWeek.getOrNull(selected).orEmpty()
        SectionHeader(if (current) "Workouts this week" else "Workouts that week", null)
        Column(Modifier.testTag("volume-week-workouts")) {
            if (workouts.isEmpty())
                Text(
                    "No workouts recorded in this week.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            for (workout in workouts) DashboardRow(
                title =
                    workout.exerciseNames.take(3).joinToString(", ") +
                        if (workout.exerciseNames.size > 3) " +${workout.exerciseNames.size - 3}"
                        else "",
                detail =
                    "${friendlyDate(workout.date)} · ${plural(workout.workingSets, "working set")}",
                value = workout.loadVolumeKg?.let(volume) ?: "",
                secondary =
                    if (workout.loadVolumeKg != null) "Training volume (load × reps)" else null,
                onClick = openWorkout?.let { open -> { open(workout.sessionId) } },
            )
        }

        val byExercise = overview.volumeByExercise.take(5)
        if (byExercise.isNotEmpty()) {
            SectionHeader("By exercise · ${data.period.label}", null)
            val largest = byExercise.first().loadVolumeKg.coerceAtLeast(1.0)
            val tones = chartAccent(ChartHue.PLUM)
            Column(
                Modifier.testTag("volume-by-exercise"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (item in byExercise) Column(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(item.name, modifier = Modifier.weight(1f))
                        Text(volume(item.loadVolumeKg), fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier.fillMaxWidth(
                                (item.loadVolumeKg / largest).toFloat().coerceIn(0.02f, 1f)
                            )
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(tones.soft)
                            .clearAndSetSemantics {}
                    )
                    Text(
                        plural(item.workingSets, "working set"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

@Composable
private fun WeeklyBars(
    values: List<Double?>,
    weeks: List<StrengthVolume.Week>,
    selected: Int,
    format: (Double) -> String,
    shortDate: (LocalDate) -> String,
    select: (Int) -> Unit,
) {
    val tones = chartAccent(ChartHue.PLUM)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.outline
    val halo = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val top = values.filterNotNull().maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val height = chartHeight()
    val dense = values.size > 12
    val starts = weeks.map { requireNotNull(it.monday()) }
    fun describe(i: Int) =
        "Week of ${shortDate(starts[i])}: " +
            when {
                weeks[i].workouts() == 0 -> "no workouts"
                values[i] == null -> "no load × repetition sets"
                else ->
                    "${format(requireNotNull(values[i]))}, ${plural(weeks[i].workouts(), "workout")}"
            }
    Row(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Box(
                Modifier.fillMaxWidth().height(height).testTag("volume-chart").semantics {
                    stateDescription = describe(selected)
                }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val slot = size.width / values.size
                    val chartTop = 14.dp.toPx()
                    val baseline = size.height - 6.dp.toPx()
                    grid(chartTop, baseline, grid)
                    val sx = slot * selected + slot / 2
                    selectionGuide(sx, chartTop, baseline, tones.strong, halo)
                    val barWidth = (slot * if (dense) 0.72f else 0.58f).coerceAtMost(40.dp.toPx())
                    val radius =
                        if (dense) (barWidth / 2.5f).coerceAtMost(3.dp.toPx()) else barWidth / 2.5f
                    values.forEachIndexed { i, v ->
                        val cx = slot * i + slot / 2
                        when {
                            weeks[i].workouts() == 0 ->
                                drawCircle(outline, 3.dp.toPx(), Offset(cx, baseline))
                            v == null ->
                                drawCircle(
                                    outline,
                                    (if (dense) 3.5f else 5f).dp.toPx(),
                                    Offset(cx, baseline - 6.dp.toPx()),
                                    style = Stroke(1.5.dp.toPx()),
                                )
                            else -> {
                                val by = baseline - ((v / top).toFloat() * (baseline - chartTop))
                                if (i == selected) {
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
                                    if (i == selected) tones.strong else tones.soft,
                                    Offset(cx - barWidth / 2, by),
                                    Size(barWidth, baseline - by),
                                    CornerRadius(radius),
                                )
                            }
                        }
                    }
                }
                WeekTargets(values.size, selected, ::describe, select)
            }
            ChartDates(starts, dense, shortDate)
        }
        Scale(format(top), "0", height)
    }
}

/**
 * Muscle-group workload: recorded working sets shared between each exercise's primary areas, as
 * bars, with the earlier period marked by a thin line when history covers it. Tapping a group shows
 * the exercises that contributed. More is not shown as better, and this is not a measure of muscle
 * growth or recovery.
 */
@Composable
internal fun MuscleWorkloadCard(data: StrengthPeriod, decimal: (Double) -> String) =
    ExpressiveSection(
        "Muscle-group workload",
        Glyph.BODY,
        ChartHue.PLUM,
        "Recorded working sets · ${data.period.label}",
    ) {
        val overview = data.overview
        if (overview.workingSets == 0) {
            Text("Record working sets to see how your training is spread.")
            return@ExpressiveSection
        }
        val tones = chartAccent(ChartHue.PLUM)
        val outline = MaterialTheme.colorScheme.onSurfaceVariant
        val previous = overview.previousGroupSets
        val compared = previous.isNotEmpty()
        val groups = overview.muscleGroups.filter { it.primarySets > 0.0 }
        val largest =
            (groups.map { it.primarySets } + groups.mapNotNull { previous[it.group] })
                .maxOrNull()
                ?.coerceAtLeast(1.0) ?: 1.0
        var open by rememberSaveable { mutableStateOf<String?>(null) }
        Column(
            Modifier.testTag("muscle-workload"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (g in groups) {
                val before = previous[g.group]
                val expanded = open == g.group.name
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { open = if (expanded) null else g.group.name }
                        .heightIn(min = 48.dp)
                        .testTag("muscle-${g.group.name}")
                        .semantics(mergeDescendants = true) {
                            stateDescription =
                                if (expanded) "Exercises shown" else "Tap to show exercises"
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(g.group.label, modifier = Modifier.weight(1f))
                        Text("${decimal(g.primarySets)} sets", fontWeight = FontWeight.SemiBold)
                    }
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().height(12.dp).clearAndSetSemantics {}
                    ) {
                        val w = maxWidth
                        Box(
                            Modifier.fillMaxWidth(
                                    (g.primarySets / largest).toFloat().coerceIn(0.02f, 1f)
                                )
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(tones.soft)
                        )
                        if (before != null)
                            Box(
                                Modifier.offset(
                                        x = w * (before / largest).toFloat().coerceIn(0f, 1f) - 1.dp
                                    )
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(outline)
                            )
                    }
                    Text(
                        listOfNotNull(
                                if (compared)
                                    "previous ${data.period.label}: ${decimal(before ?: 0.0)}"
                                else null,
                                if (g.secondarySets > 0) "also secondary in ${g.secondarySets}"
                                else null,
                            )
                            .joinToString(" · ")
                            .ifEmpty { "Primary working sets" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (expanded)
                        Text(
                            overview.groupExercises[g.group].orEmpty().joinToString(", ").ifEmpty {
                                "—"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("muscle-${g.group.name}-exercises"),
                        )
                }
            }
            val secondaryOnly =
                overview.muscleGroups.filter { it.primarySets == 0.0 && it.secondarySets > 0 }
            if (secondaryOnly.isNotEmpty())
                Text(
                    "Secondary only: " +
                        secondaryOnly.joinToString(", ") {
                            "${it.group.label} (${it.secondarySets})"
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            if (overview.unclassifiedSets > 0)
                Text(
                    "${plural(overview.unclassifiedSets, "working set")} from custom or changed exercises are not assigned to a group.",
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        if (compared)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clearAndSetSemantics {},
            ) {
                Box(Modifier.width(2.dp).height(12.dp).background(outline))
                Spacer(Modifier.width(6.dp))
                Text("Previous ${data.period.label}", style = MaterialTheme.typography.labelSmall)
            }
        Text(
            "Recorded workload only — not a measure of muscle growth or recovery. More is not necessarily better.",
            style = MaterialTheme.typography.bodySmall,
        )
        Explainer("How sets are counted", StrengthVolume.attribution())
    }
