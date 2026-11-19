@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.strength.StrengthRecords
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** Existing Progress destinations; each platform shows the ones it supports. */
enum class ProgressLink(val title: String, val summary: String) {
    HISTORY("Run history", "Every recorded run, newest first"),
    TRENDS("Training trends", "Distance, pace, time and recorded heart rate"),
    PBS("Running PBs", "Ranked retained results, dates and recorded pace"),
    WEEKLY_REVIEW("Weekly coaching review", "Evidence, recommendations and your decisions"),
    MILESTONES("Recorded milestones", "Running and strength evidence, with optional celebrations"),
    BODY_WEIGHT("Body weight", "Recorded measurements, without judgement"),
    BODY_GUIDANCE("Body & protein guidance", "Optional source-labelled measurements and guidance"),
    STRENGTH_ANALYTICS("Strength analytics", "Working sets, muscle areas and exercise frequency"),
}

/** Existing Strength destinations; each platform shows the ones it supports. */
enum class StrengthLink(val title: String, val summary: String) {
    GYM_NOW("Gym now", "Start a planned or spontaneous workout"),
    LOG("Log workout", "Record what you actually did"),
    PROGRAMME("Gym programme", "Programming for your strength goals, alone or with running"),
    ANALYTICS("Strength trends", "Working sets, primary muscle areas and exercise frequency"),
    PBS("PBs & progression", "Historical and workout records together"),
    MILESTONES("Recorded milestones", "Local recognition from genuine training"),
    HISTORY("Workout history", "Every recorded strength session"),
    EXERCISES("Exercises", "Search, favourite and manage your exercises"),
    RECOVERY("Recovery & training balance", "Optional guidance; review changes before activation"),
}

@Composable
fun SharedProgressDashboard(
    state: CoachUiState,
    presentation: HomePresentation,
    dateAt: (Instant) -> LocalDate,
    links: List<ProgressLink>,
    openRun: (String) -> Unit,
    open: (ProgressLink) -> Unit,
) = CoachPage {
    val today = presentation.currentDate()
    val overview =
        remember(state.activities, state.plannedWorkouts, today) {
            DashboardAnalytics.running(state.activities, state.plannedWorkouts, today, dateAt)
        }
    CoachHeading("Progress")
    Text(
        "Running, consistency and recent trends from your recorded training.",
        style = MaterialTheme.typography.bodyMedium,
    )

    SemanticCard(Tile.Progress) {
        Text("Distance this week", style = MaterialTheme.typography.titleMedium)
        val week = overview.thisWeek
        if (week.activityCount() == 0) {
            Text("No runs recorded yet this week", style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(
                week.distanceMetres()?.let(presentation.distance) ?: "Distance unavailable",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dashboard-week-distance"),
            )
            Text(
                "${plural(week.activityCount(), "run")} · ${plural(week.runningDays(), "running day")} · week in progress"
            )
        }
        Text(
            overview.lastWeek.distanceMetres()?.let { "Last week: ${presentation.distance(it)}" }
                ?: if (overview.lastWeek.activityCount() > 0) "Last week: distance unavailable"
                else "Last week: no runs recorded"
        )
        if (week.missingDistanceCount() > 0)
            Text("${plural(week.missingDistanceCount(), "run")} without distance are not added.")
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 3,
    ) {
        KpiTile(
            "Average pace",
            overview.averageSecondsPerKm?.let { presentation.paceSeconds(it) } ?: "Not enough data",
            if (overview.pacedRuns > 0)
                "Moving pace · ${plural(overview.pacedRuns, "run")} · last 4 weeks"
            else "Needs runs with distance and moving time",
        )
        KpiTile(
            "Longest run",
            overview.longestRunMetres?.let(presentation.distance) ?: "No runs yet",
            overview.longestRunDate?.let { "Last 30 days · ${presentation.shortDate(it)}" }
                ?: "Last 30 days",
        )
        KpiTile(
            "Plan completion",
            if (overview.plannedDue > 0) "${overview.plannedCompleted} / ${overview.plannedDue}"
            else "No planned runs",
            if (overview.plannedDue > 0) "Planned runs due · last 4 weeks"
            else "Nothing was due in the last 4 weeks",
        )
    }

    CoachCard {
        Text("Weekly distance", style = MaterialTheme.typography.titleLarge)
        Text("Last ${DashboardAnalytics.TREND_WEEKS} weeks · this week is still in progress")
        if (!overview.hasAnyRun()) {
            Text("Record or import runs to see your trend.")
        } else {
            val points =
                remember(overview.weeklyTrend, state.activities) {
                    sharedRunningTrendPoints(
                        overview.weeklyTrend,
                        state.activities,
                        RunningTrendMetric.DISTANCE,
                        today,
                        dateAt,
                    )
                }
            SharedWeeklyRunningChart(
                overview.weeklyTrend,
                points,
                RunningTrendMetric.DISTANCE,
                state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
                presentation.distance,
                presentation.decimal,
                presentation.paceSeconds,
                presentation.shortDate,
                presentation.friendlyDate,
            )
        }
        if (ProgressLink.TRENDS in links)
            TextButton(onClick = { open(ProgressLink.TRENDS) }) { Text("Open training trends") }
    }

    CoachCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recent runs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (ProgressLink.HISTORY in links)
                TextButton(onClick = { open(ProgressLink.HISTORY) }) { Text("See all") }
        }
        if (overview.recentRuns.isEmpty()) Text("No runs recorded yet.")
        for (run in overview.recentRuns) {
            val record = state.activities.firstOrNull { it.id() == run.activityId }
            DashboardRow(
                title = record?.let(::runTitle) ?: "Run",
                detail = presentation.friendlyDate(run.date),
                value = run.distanceMetres?.let(presentation.distance) ?: "No distance",
                secondary = run.secondsPerKm?.let { presentation.paceSeconds(it) },
                onClick = { openRun(run.activityId) },
            )
        }
    }

    DashboardLinks(links.filter { it != ProgressLink.HISTORY }, { it.title }, { it.summary }, open)
}

@Composable
fun SharedStrengthDashboard(
    state: CoachUiState,
    today: LocalDate,
    asOf: Instant,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    decimal: (Double) -> String,
    links: List<StrengthLink>,
    open: (StrengthLink) -> Unit,
) = CoachPage {
    val overview =
        remember(state.strengthSessions, state.strengthPlannedDates, today, asOf) {
            DashboardAnalytics.strength(
                state.strengthSessions,
                state.strengthPlannedDates.values,
                today,
                asOf,
            )
        }
    val unit = state.massUnit
    fun mass(kilograms: Double) =
        "${decimal(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"
    CoachHeading("Strength")
    Text(
        "Training completion, working sets and recorded progress at a glance.",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (StrengthLink.GYM_NOW in links)
        Button(
            onClick = { open(StrengthLink.GYM_NOW) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(StrengthLink.GYM_NOW.title)
        }

    val thisWeek = overview.thisWeek()
    val lastWeek = overview.lastWeek()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 3,
    ) {
        KpiTile(
            "Working sets",
            thisWeek.workingSets().toString(),
            "This week · last week ${lastWeek.workingSets()}",
        )
        KpiTile(
            "Workouts",
            if (overview.plannedThisWeek > 0) "${thisWeek.workouts()} / ${overview.plannedThisWeek}"
            else thisWeek.workouts().toString(),
            if (overview.plannedThisWeek > 0) "Recorded / planned this week"
            else "Recorded this week",
        )
        KpiTile(
            "New records",
            overview.recordImprovements.toString(),
            "Recorded bests improved · last 30 days",
        )
    }

    CoachCard {
        Text("Estimated 1RM trend", style = MaterialTheme.typography.titleLarge)
        val exercise = overview.trendExercise
        if (exercise == null) {
            Text("Record weighted working sets of 2–10 repetitions to see an estimate trend.")
        } else {
            Text("${exercise.name()} · last ${DashboardAnalytics.STRENGTH_WEEKS} weeks")
            OneRepMaxChart(overview.oneRepMaxTrend, ::mass, shortDate)
            Text(StrengthRecords.estimateExplanation(), style = MaterialTheme.typography.bodySmall)
        }
    }

    CoachCard {
        Text("Muscle-group workload", style = MaterialTheme.typography.titleLarge)
        Text("Working sets · last 4 weeks")
        if (overview.muscleGroupSets.isEmpty()) {
            Text("Record working sets with catalogue exercises to see your training balance.")
        } else {
            MuscleGroupBreakdown(overview.muscleGroupSets, decimal)
        }
        if (overview.unclassifiedSets > 0)
            Text(
                "${plural(overview.unclassifiedSets, "set")} could not be classified and are not shown."
            )
        Text(
            "A set is shared between its exercise's primary areas. Broad classification, not exact muscle stimulus.",
            style = MaterialTheme.typography.bodySmall,
        )
    }

    CoachCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recent workouts",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (StrengthLink.HISTORY in links)
                TextButton(onClick = { open(StrengthLink.HISTORY) }) { Text("See all") }
        }
        if (overview.recentWorkouts.isEmpty()) Text("No strength workouts recorded yet.")
        for (workout in overview.recentWorkouts) {
            DashboardRow(
                title =
                    workout.exerciseNames.take(3).joinToString(", ") +
                        if (workout.exerciseNames.size > 3) " +${workout.exerciseNames.size - 3}"
                        else "",
                detail =
                    "${friendlyDate(workout.date)} · ${plural(workout.workingSets, "working set")}",
                value = workout.loadVolumeKg?.let(::mass) ?: "",
                secondary = if (workout.loadVolumeKg != null) "load × reps" else null,
                onClick =
                    if (StrengthLink.HISTORY in links) ({ open(StrengthLink.HISTORY) }) else null,
            )
        }
    }

    DashboardLinks(
        links.filter { it != StrengthLink.GYM_NOW && it != StrengthLink.HISTORY },
        { it.title },
        { it.summary },
        open,
    )
}

private fun plural(count: Int, noun: String) = "$count $noun${if (count == 1) "" else "s"}"

private fun runTitle(record: ActivityRecord): String =
    if (record.plannedWorkoutId() != null) "Planned run" else "Run"

@Composable
private fun KpiTile(title: String, value: String, detail: String) {
    Card(
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier =
            Modifier.widthIn(min = 150.dp)
                .semantics(mergeDescendants = true) {}
                .testTag("kpi-$title"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardRow(
    title: String,
    detail: String,
    value: String,
    secondary: String?,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.titleMedium)
                secondary?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, color = Color.Transparent) { content() }
    else content()
}

@Composable
private fun <T> DashboardLinks(
    links: List<T>,
    title: (T) -> String,
    summary: (T) -> String,
    open: (T) -> Unit,
) {
    if (links.isEmpty()) return
    Text("More detail", style = MaterialTheme.typography.titleMedium)
    for (link in links) OutlinedCard(
        onClick = { open(link) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title(link), style = MaterialTheme.typography.titleMedium)
            Text(summary(link), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OneRepMaxChart(
    points: List<DashboardAnalytics.OneRepMaxPoint>,
    mass: (Double) -> String,
    shortDate: (LocalDate) -> String,
) {
    val known = points.filter { it.kilograms != null }
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.surface
    val description =
        known.joinToString(prefix = "Estimated one-rep max by week: ", separator = "; ") {
            "${shortDate(it.weekStart)} ${mass(requireNotNull(it.kilograms))}"
        }
    val low = known.minOfOrNull { requireNotNull(it.kilograms) } ?: 0.0
    val high = known.maxOfOrNull { requireNotNull(it.kilograms) } ?: 0.0
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "Latest ${known.lastOrNull()?.kilograms?.let(mass) ?: "—"}",
            fontWeight = FontWeight.SemiBold,
        )
        Text("Range ${mass(low)} – ${mass(high)}", style = MaterialTheme.typography.bodySmall)
    }
    Canvas(
        Modifier.fillMaxWidth().height(160.dp).testTag("one-rep-max-chart").clearAndSetSemantics {
            contentDescription = description
        }
    ) {
        val span = (high - low).takeIf { it > 0 } ?: 1.0
        val bottom = size.height - 8.dp.toPx()
        val top = 8.dp.toPx()
        fun x(index: Int) =
            if (points.size <= 1) size.width / 2 else size.width * index / (points.size - 1)
        fun y(value: Double) =
            (bottom - (value - low) / span * (bottom - top) * 0.85 - (bottom - top) * 0.05)
                .toFloat()
        for (fraction in listOf(0f, .5f, 1f)) {
            val gy = top + (bottom - top) * fraction
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
        }
        val indexed = points.withIndex().filter { it.value.kilograms != null }
        if (indexed.size >= 2) {
            val path = Path()
            val area = Path()
            indexed.forEachIndexed { n, (i, point) ->
                val px = x(i)
                val py = y(requireNotNull(point.kilograms))
                if (n == 0) {
                    path.moveTo(px, py)
                    area.moveTo(px, bottom)
                    area.lineTo(px, py)
                } else {
                    path.lineTo(px, py)
                    area.lineTo(px, py)
                }
            }
            area.lineTo(x(indexed.last().index), bottom)
            area.close()
            drawPath(area, fill)
            drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        for ((i, point) in indexed) {
            val centre = Offset(x(i), y(requireNotNull(point.kilograms)))
            drawCircle(dot, 5.dp.toPx(), centre)
            drawCircle(line, 5.dp.toPx(), centre, style = Stroke(2.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        points.firstOrNull()?.let {
            Text(shortDate(it.weekStart), style = MaterialTheme.typography.bodySmall)
        }
        points.lastOrNull()?.let {
            Text(shortDate(it.weekStart), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MuscleGroupBreakdown(
    groups: List<Pair<DashboardAnalytics.MuscleGroup, Double>>,
    decimal: (Double) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.secondary,
            scheme.primaryContainer,
            scheme.tertiaryContainer,
            scheme.secondaryContainer,
            scheme.outline,
        )
    val total = groups.sumOf { it.second }
    val track = scheme.surfaceContainerHighest
    val stacked = LocalDensity.current.fontScale >= 1.5f
    val donut: @Composable () -> Unit = {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    track,
                    0f,
                    360f,
                    false,
                    Offset(inset, inset),
                    arcSize,
                    style = Stroke(stroke),
                )
                var start = -90f
                groups.forEachIndexed { i, (_, sets) ->
                    val sweep = (sets / total * 360f).toFloat()
                    drawArc(
                        palette[i % palette.size],
                        start,
                        (sweep - 1.5f).coerceAtLeast(0.5f),
                        false,
                        Offset(inset, inset),
                        arcSize,
                        style = Stroke(stroke),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    decimal(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("sets", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val legend: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEachIndexed { i, (group, sets) ->
                val percent = (sets / total * 100).roundToInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.semantics(mergeDescendants = true) {
                            contentDescription =
                                "${group.label}: $percent percent, ${decimal(sets)} sets"
                        },
                ) {
                    Box(Modifier.size(12.dp).padding(1.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = palette[i % palette.size],
                            modifier = Modifier.fillMaxSize(),
                        ) {}
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(group.label, modifier = Modifier.weight(1f))
                    Text("$percent%", modifier = Modifier.widthIn(min = 48.dp))
                    Text("${decimal(sets)} sets", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            donut()
            legend(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            donut()
            legend(Modifier.weight(1f))
        }
    }
}
