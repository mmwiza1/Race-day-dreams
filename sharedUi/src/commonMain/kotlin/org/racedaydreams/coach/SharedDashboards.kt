@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
import org.racedaydreams.domain.strength.StrengthRecords
import org.racedaydreams.domain.strength.StrengthVolume
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** Existing Progress destinations; each platform shows the ones it supports. */
enum class ProgressLink(val title: String, val summary: String) {
    HISTORY("Run history", "Every recorded run, newest first"),
    TRENDS("Training trends", "Distance, pace and time by week"),
    PBS("Running PBs", "Ranked results and recorded pace"),
    WEEKLY_REVIEW("Weekly coaching review", "Evidence and your decisions"),
    MILESTONES("Recorded milestones", "Running and strength evidence"),
    BODY_WEIGHT("Body weight", "Recorded measurements"),
    BODY_GUIDANCE("Body & protein guidance", "Optional, source-labelled"),
    STRENGTH_ANALYTICS("Strength analytics", "Working sets and muscle areas"),
}

/** Existing Strength destinations; each platform shows the ones it supports. */
enum class StrengthLink(val title: String, val summary: String) {
    GYM_NOW("Gym now", "Start a planned or spontaneous workout"),
    LOG("Log workout", "Record what you actually did"),
    PROGRAMME("Gym programme", "Your strength programme"),
    ANALYTICS("Strength trends", "Working sets and exercise frequency"),
    PBS("PBs & progression", "Recorded bests and history"),
    MILESTONES("Recorded milestones", "Recognition from genuine training"),
    HISTORY("Workout history", "Every recorded strength session"),
    EXERCISES("Exercises", "Search, favourite and manage"),
    RECOVERY("Recovery & training balance", "Optional guidance"),
    BOULDERING("Bouldering", "Log climbs, grades and sends"),
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
    var periodName by rememberSaveable { mutableStateOf(Period.FOUR_WEEKS.name) }
    val period = Period.valueOf(periodName)
    val overview =
        remember(state.activities, today, period) {
            DashboardAnalytics.running(state.activities, today, period, dateAt)
        }
    val planned = state.currentPlan?.plan()?.workouts() ?: state.plannedWorkouts
    val completion =
        remember(planned, state.activities, state.workoutDecisions, today, period) {
            DashboardAnalytics.planCompletion(
                planned,
                state.activities,
                state.workoutDecisions,
                overview.window,
                today,
            )
        }
    val distance = presentation.distance
    CoachHeading("Progress")

    SemanticCard(Tile.Progress) {
        Text("Distance this week", style = MaterialTheme.typography.titleMedium)
        val week = overview.weekToDate
        if (week.runs == 0) {
            Text("No runs recorded yet this week", style = MaterialTheme.typography.headlineSmall)
        } else {
            Text(
                week.distanceMetres?.let(distance) ?: "Distance unavailable",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("dashboard-week-distance"),
            )
            Text("${plural(week.runs, "run")} · ${plural(week.runningDays, "running day")}")
        }
        val days = weekdaySpan(overview.weekDays)
        Text(
            if (overview.weekDays == 7) "Full week" else "Week in progress · $days so far",
            style = MaterialTheme.typography.bodyMedium,
        )
        val last = overview.sameDaysLastWeek
        val lastDistance = last.distanceMetres
        Text(
            "Same days last week ($days): " +
                when {
                    last.runs == 0 -> "no runs"
                    lastDistance == null -> "distance unavailable"
                    else ->
                        distance(lastDistance) +
                            (week.distanceMetres?.let {
                                " · ${change(it - lastDistance, distance)}"
                            } ?: "")
                },
            modifier = Modifier.testTag("dashboard-week-comparison"),
        )
        if (week.missingDistance > 0)
            Text("${plural(week.missingDistance, "run")} without distance not added.")
    }

    PeriodSelector(period) { periodName = it.name }

    val current = overview.current
    val currentDistance = current.distanceMetres
    val previousDistance = overview.previous.distanceMetres
    val previousNote =
        when {
            !overview.comparable -> "no earlier ${period.label} to compare"
            currentDistance != null && previousDistance != null ->
                "${change(currentDistance - previousDistance, distance)} vs previous"
            else -> "previous ${period.label}: ${plural(overview.previous.runs, "run")}"
        }
    KpiGrid(
        listOf(
            Kpi(
                "Distance",
                if (current.runs == 0) "No runs"
                else currentDistance?.let(distance) ?: "Unavailable",
                "${plural(current.runs, "run")} · $previousNote",
            ),
            Kpi(
                "Average pace",
                current.movingSecondsPerKm?.let { presentation.paceSeconds(it) }
                    ?: "Not enough data",
                if (current.pacedRuns > 0)
                    "Moving pace · ${current.pacedRuns} of ${plural(current.runs, "run")}"
                else "Needs distance and moving time",
            ),
            Kpi(
                "Longest run",
                current.longestMetres?.let(distance) ?: "No runs",
                current.longestDate?.let { presentation.shortDate(it) } ?: period.label,
                current.longestActivityId?.let { id -> { openRun(id) } },
            ),
            Kpi(
                "Plan completion",
                if (completion.due > 0) "${completion.completed} / ${completion.due}"
                else "Nothing due",
                buildList {
                        add(if (completion.due > 0) "Planned runs due" else "No planned runs due")
                        if (completion.skipped > 0) add("${completion.skipped} skipped")
                    }
                    .joinToString(" · "),
            ),
        )
    )

    CoachCard {
        Text("Weekly distance", style = MaterialTheme.typography.titleLarge)
        Text(
            "${period.label} · from ${presentation.shortDate(overview.window.start)} · this week in progress",
            style = MaterialTheme.typography.bodyMedium,
        )
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
                distance,
                presentation.decimal,
                presentation.paceSeconds,
                presentation.shortDate,
                presentation.friendlyDate,
                remember(state.activities) { sharedWeekRuns(state.activities, dateAt) },
            )
            Text("Tap a bar to inspect that week.", style = MaterialTheme.typography.bodySmall)
        }
        if (ProgressLink.TRENDS in links)
            TextButton(onClick = { open(ProgressLink.TRENDS) }) {
                Text("Distance, pace and time trends")
            }
    }

    CoachCard {
        SectionHeader(
            "Recent runs",
            if (ProgressLink.HISTORY in links) ({ open(ProgressLink.HISTORY) }) else null,
        )
        if (overview.recentRuns.isEmpty()) Text("No runs recorded yet.")
        for (run in overview.recentRuns) {
            val record = state.activities.firstOrNull { it.id() == run.activityId }
            DashboardRow(
                title = if (run.planned) "Planned run" else "Run",
                detail =
                    presentation.friendlyDate(run.date) +
                        (record?.let { " · ${ActivitySources.friendly(it.source())}" } ?: ""),
                value = run.distanceMetres?.let(distance) ?: "No distance",
                secondary =
                    run.movingSecondsPerKm?.let { "${presentation.paceSeconds(it)} moving" }
                        ?: "Pace unavailable",
                onClick = { openRun(run.activityId) },
            )
        }
    }

    MoreDetail(
        listOf(
                "Running" to
                    listOf(ProgressLink.TRENDS, ProgressLink.PBS, ProgressLink.WEEKLY_REVIEW),
                "Milestones & body" to
                    listOf(
                        ProgressLink.MILESTONES,
                        ProgressLink.BODY_WEIGHT,
                        ProgressLink.BODY_GUIDANCE,
                    ),
                "Strength training" to listOf(ProgressLink.STRENGTH_ANALYTICS),
            )
            .map { (name, group) -> name to group.filter { it in links } },
        { it.title },
        { it.summary },
        open,
    )
}

/** Scheduled programme sessions and those completed, using the calendar's gym link rules. */
private fun programmeSchedule(s: CoachUiState): Pair<List<Pair<String, LocalDate>>, Set<String>>? {
    val authored = s.authoredGymProgramme
    if (authored != null) {
        val actualIds = s.strengthSessions.map { it.id() }.toSet()
        val prefix = authored.id() + ":v"
        val completed =
            s.gymLinks.values
                .filter { link ->
                    val version =
                        link
                            .versionKey()
                            .takeIf { it.startsWith(prefix) }
                            ?.removePrefix(prefix)
                            ?.toIntOrNull()
                    link.actualSessionId() in actualIds &&
                        version != null &&
                        version in 1..authored.revision()
                }
                .map { it.prescribedSessionId() }
                .toSet()
        return authored.workouts().map { it.id() to it.date() } to completed
    }
    val gym = s.gymProgramme ?: return null
    return gym.sessions().map { it.id() to it.date() } to
        s.gymLinks.values.map { it.prescribedSessionId() }.toSet()
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
    openWorkout: (String) -> Unit,
    climbing: List<org.racedaydreams.domain.climbing.ClimbingSession> = emptyList(),
    climbingDraft: Boolean = false,
    gradeSystem: org.racedaydreams.domain.climbing.GradeSystem =
        org.racedaydreams.domain.climbing.GradeSystem.V_SCALE,
) = CoachPage {
    var periodName by rememberSaveable { mutableStateOf(Period.FOUR_WEEKS.name) }
    var exerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val period = Period.valueOf(periodName)
    val overview =
        remember(state.strengthSessions, today, asOf, period, exerciseId) {
            DashboardAnalytics.strength(state.strengthSessions, today, asOf, period, exerciseId)
        }
    val schedule =
        remember(
            state.authoredGymProgramme,
            state.gymProgramme,
            state.gymLinks,
            state.strengthSessions,
        ) {
            programmeSchedule(state)
        }
    val programme =
        schedule?.let { (scheduled, completed) ->
            DashboardAnalytics.programmeCompletion(scheduled, completed, overview.window, today)
        }
    val unit = state.massUnit
    fun mass(kilograms: Double) =
        "${decimal(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"
    CoachHeading("Strength")
    if (StrengthLink.GYM_NOW in links)
        Button(
            onClick = { open(StrengthLink.GYM_NOW) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(StrengthLink.GYM_NOW.title)
        }

    PeriodSelector(period) { periodName = it.name }

    KpiGrid(
        listOf(
            Kpi(
                "Working sets",
                overview.workingSets.toString(),
                overview.previousWorkingSets?.let {
                    "${signed(overview.workingSets - it)} vs previous ${period.label}"
                } ?: "No earlier ${period.label} to compare",
            ),
            Kpi("Workouts", overview.workouts.toString(), "Recorded · ${period.label}"),
            Kpi(
                "Programme",
                when {
                    programme == null -> "None"
                    programme.due == 0 -> "Nothing due"
                    else -> "${programme.completed} / ${programme.due}"
                },
                if (programme == null) "No gym programme scheduled"
                else "Scheduled sessions completed",
            ),
            Kpi(
                "New records",
                overview.recordImprovements.toString(),
                "Recorded bests improved · each record counted once per workout",
                if (StrengthLink.PBS in links) ({ open(StrengthLink.PBS) }) else null,
            ),
        )
    )

    // Bouldering stays separate from lifting metrics; shown only to people who climb.
    if (StrengthLink.BOULDERING in links && (climbing.isNotEmpty() || climbingDraft)) {
        val climbs =
            remember(climbing, today, period, gradeSystem) {
                org.racedaydreams.domain.climbing.ClimbingAnalytics.summary(
                    climbing,
                    today,
                    period,
                    gradeSystem,
                )
            }
        val hardest = climbs.highestSend[gradeSystem] ?: climbs.highestSend.values.firstOrNull()
        Card(
            onClick = { open(StrengthLink.BOULDERING) },
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
            modifier = Modifier.fillMaxWidth().testTag("strength-bouldering-card"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Bouldering", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (climbingDraft) "Unsaved session · tap to resume"
                    else
                        "${plural(climbs.sessions, "session")} · ${plural(climbs.sends, "send")}" +
                            (hardest?.let { " · hardest ${it.label}" } ?: "") +
                            " · ${period.label}"
                )
            }
        }
    }

    CoachCard {
        Text("Estimated 1RM trend", style = MaterialTheme.typography.titleLarge)
        val exercise = overview.trendExercise
        if (exercise == null) {
            Text(
                "Record weighted working sets of 2–10 repetitions in this period to see an estimate."
            )
        } else {
            ExerciseSelector(
                overview.eligibleExercises.map { it.id() to it.name() },
                exercise.id(),
            ) {
                exerciseId = it
            }
            OneRepMaxChart(overview.oneRepMaxTrend, ::mass, shortDate, friendlyDate, openWorkout)
            overview.heaviestSet?.let { set ->
                DashboardRow(
                    title = "Heaviest recorded set",
                    detail = "Actual, not estimated · ${friendlyDate(set.date)}",
                    value = "${mass(set.kilograms)} × ${set.reps}",
                    secondary = null,
                    onClick = { openWorkout(set.sessionId) },
                )
            }
            Explainer("How the estimate works", StrengthRecords.estimateExplanation())
        }
    }

    CoachCard {
        Text("Muscle-group workload", style = MaterialTheme.typography.titleLarge)
        Text("Recorded working sets · ${period.label}", style = MaterialTheme.typography.bodyMedium)
        if (overview.workingSets == 0) {
            Text("Record working sets to see how your training is spread.")
        } else {
            MuscleGroupBreakdown(overview, decimal)
        }
        Text(
            "Recorded workload only — not a measure of muscle growth or recovery.",
            style = MaterialTheme.typography.bodySmall,
        )
        Explainer("How sets are counted", StrengthVolume.attribution())
    }

    CoachCard {
        SectionHeader(
            "Recent workouts",
            if (StrengthLink.HISTORY in links) ({ open(StrengthLink.HISTORY) }) else null,
        )
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
                onClick = { openWorkout(workout.sessionId) },
            )
        }
    }

    MoreDetail(
        listOf(
                "Train" to
                    listOf(
                        StrengthLink.LOG,
                        StrengthLink.BOULDERING,
                        StrengthLink.PROGRAMME,
                        StrengthLink.EXERCISES,
                    ),
                "Records & trends" to
                    listOf(StrengthLink.ANALYTICS, StrengthLink.PBS, StrengthLink.MILESTONES),
                "Recovery" to listOf(StrengthLink.RECOVERY),
            )
            .map { (name, group) -> name to group.filter { it in links } },
        { it.title },
        { it.summary },
        open,
    )
}

internal fun plural(count: Int, noun: String) = "$count $noun${if (count == 1) "" else "s"}"

private fun signed(value: Int) = if (value > 0) "+$value" else value.toString()

private fun change(metres: Double, distance: (Double) -> String): String =
    when {
        abs(metres) < 0.5 -> "no change"
        metres > 0 -> "+${distance(metres)}"
        else -> "−${distance(-metres)}"
    }

private val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun weekdaySpan(days: Int) = if (days <= 1) "Mon" else "Mon–${weekdays[days - 1]}"

@Composable
internal fun PeriodSelector(selected: Period, select: (Period) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = "Reporting period" },
    ) {
        for (period in Period.values()) FilterChip(
            selected = period == selected,
            onClick = { select(period) },
            label = { Text(period.label) },
            modifier = Modifier.heightIn(min = 48.dp).testTag("period-${period.name}"),
        )
    }
}

internal class Kpi(
    val title: String,
    val value: String,
    val detail: String,
    val onClick: (() -> Unit)? = null,
)

/** Four tiles: two per row on phones, one per row with very large text, four on wide screens. */
@Composable
internal fun KpiGrid(tiles: List<Kpi>) {
    val large = LocalDensity.current.fontScale >= 1.5f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns =
            when {
                large -> 1
                maxWidth >= 600.dp -> 4
                else -> 2
            }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in tiles.chunked(columns)) Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (tile in row) KpiTile(tile, Modifier.weight(1f).fillMaxHeight())
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun KpiTile(tile: Kpi, modifier: Modifier) {
    val colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val content: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(tile.title, style = MaterialTheme.typography.labelLarge)
            Text(
                tile.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(tile.detail, style = MaterialTheme.typography.bodySmall)
        }
    }
    val tagged = modifier.semantics(mergeDescendants = true) {}.testTag("kpi-${tile.title}")
    val click = tile.onClick
    if (click != null) Card(onClick = click, modifier = tagged, colors = colors, content = content)
    else Card(modifier = tagged, colors = colors, content = content)
}

@Composable
internal fun SectionHeader(title: String, seeAll: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (seeAll != null) TextButton(onClick = seeAll) { Text("See all") }
    }
}

@Composable
internal fun Explainer(label: String, text: String) {
    var shown by rememberSaveable(label) { mutableStateOf(false) }
    TextButton(
        onClick = { shown = !shown },
        modifier = Modifier.semantics { stateDescription = if (shown) "Expanded" else "Collapsed" },
    ) {
        Text(if (shown) "$label ▴" else "$label ▾")
    }
    if (shown) Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun DashboardRow(
    title: String,
    detail: String,
    value: String,
    secondary: String?,
    onClick: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp).semantics(
                mergeDescendants = true
            ) {},
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

/**
 * One compact card of grouped links instead of a stack of large cards. Group names must differ from
 * the bottom tab names so a tab label is never duplicated on screen.
 */
@Composable
private fun <T> MoreDetail(
    groups: List<Pair<String, List<T>>>,
    title: (T) -> String,
    summary: (T) -> String,
    open: (T) -> Unit,
) {
    val shown = groups.filter { it.second.isNotEmpty() }
    if (shown.isEmpty()) return
    Text(
        "More detail",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    OutlinedCard(Modifier.fillMaxWidth()) {
        shown.forEachIndexed { index, (name, links) ->
            if (index > 0) HorizontalDivider()
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            )
            for (link in links) Surface(
                onClick = { open(link) },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title(link), style = MaterialTheme.typography.titleSmall)
                        Text(
                            summary(link),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ExerciseSelector(
    options: List<Pair<String, String>>,
    selected: String,
    select: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val name = options.firstOrNull { it.first == selected }?.second ?: ""
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = options.size > 1,
            modifier =
                Modifier.heightIn(min = 48.dp).testTag("exercise-selector").semantics {
                    contentDescription = "Exercise: $name. Choose exercise"
                },
        ) {
            Text(if (options.size > 1) "$name ▾" else name)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for ((id, label) in options) DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    select(id)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun OneRepMaxChart(
    points: List<DashboardAnalytics.OneRepMaxPoint>,
    mass: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    openWorkout: (String) -> Unit,
) {
    val known = points.withIndex().filter { it.value.kilograms != null }
    if (known.isEmpty()) {
        Text("No estimate for this exercise in this period.")
        return
    }
    var selectedWeek by
        rememberSaveable(points.first().weekStart.toString(), known.size) {
            mutableStateOf(known.last().value.weekStart.toString())
        }
    val selectedIndex =
        known.indexOfFirst { it.value.weekStart.toString() == selectedWeek }.takeIf { it >= 0 }
            ?: known.lastIndex
    val selected = known[selectedIndex].value
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val dot = MaterialTheme.colorScheme.surface
    fun describe(point: DashboardAnalytics.OneRepMaxPoint) =
        "Week of ${shortDate(point.weekStart)}: ${mass(requireNotNull(point.kilograms))} estimated"
    val low = known.minOf { requireNotNull(it.value.kilograms) }
    val high = known.maxOf { requireNotNull(it.value.kilograms) }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp).testTag("selected-one-rep-max").semantics(
                mergeDescendants = true
            ) {},
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "Week of ${friendlyDate(selected.weekStart)}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "${mass(requireNotNull(selected.kilograms))} estimated",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val setKilograms = selected.setKilograms
            val reps = selected.reps
            val date = selected.date
            if (setKilograms != null && reps != null && date != null)
                Text("From ${mass(setKilograms)} × $reps on ${friendlyDate(date)}")
        }
    }
    FlowRow(verticalArrangement = Arrangement.Center) {
        TextButton(
            onClick = { selectedWeek = known[selectedIndex - 1].value.weekStart.toString() },
            enabled = selectedIndex > 0,
        ) {
            Text("‹ Earlier")
        }
        TextButton(
            onClick = { selectedWeek = known[selectedIndex + 1].value.weekStart.toString() },
            enabled = selectedIndex < known.lastIndex,
        ) {
            Text("Later ›")
        }
        selected.sessionId?.let { id ->
            TextButton(onClick = { openWorkout(id) }) { Text("Open workout") }
        }
    }
    Canvas(
        Modifier.fillMaxWidth()
            .height(160.dp)
            .testTag("one-rep-max-chart")
            .clearAndSetSemantics {
                contentDescription =
                    "Estimated one-rep max by week. " +
                        known.joinToString("; ") { describe(it.value) }
                stateDescription = describe(selected)
                customActions =
                    known.map { (_, point) ->
                        CustomAccessibilityAction("Select week of ${shortDate(point.weekStart)}") {
                            selectedWeek = point.weekStart.toString()
                            true
                        }
                    }
            }
            .pointerInput(points) {
                detectTapGestures { position ->
                    val step =
                        if (points.size <= 1) 1f else size.width.toFloat() / (points.size - 1)
                    val nearest = known.minByOrNull { abs(it.index * step - position.x) }
                    nearest?.let { selectedWeek = it.value.weekStart.toString() }
                }
            }
    ) {
        val span = (high - low).takeIf { it > 0 } ?: 1.0
        val bottom = size.height - 10.dp.toPx()
        val top = 10.dp.toPx()
        fun x(index: Int) =
            if (points.size <= 1) size.width / 2 else size.width * index / (points.size - 1)
        fun y(value: Double) =
            (bottom - (value - low) / span * (bottom - top) * 0.85 - (bottom - top) * 0.05)
                .toFloat()
        for (fraction in listOf(0f, .5f, 1f)) {
            val gy = top + (bottom - top) * fraction
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx())
        }
        if (known.size >= 2) {
            val path = Path()
            val area = Path()
            known.forEachIndexed { n, (i, point) ->
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
            area.lineTo(x(known.last().index), bottom)
            area.close()
            drawPath(area, fill)
            drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        }
        known.forEachIndexed { n, (i, point) ->
            val centre = Offset(x(i), y(requireNotNull(point.kilograms)))
            val radius = if (n == selectedIndex) 7.dp.toPx() else 5.dp.toPx()
            drawCircle(if (n == selectedIndex) line else dot, radius, centre)
            drawCircle(line, radius, centre, style = Stroke(2.dp.toPx()))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(shortDate(points.first().weekStart), style = MaterialTheme.typography.bodySmall)
        Text("Range ${mass(low)} – ${mass(high)}", style = MaterialTheme.typography.bodySmall)
        Text(shortDate(points.last().weekStart), style = MaterialTheme.typography.bodySmall)
    }
    Text("Tap a point to inspect that week.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun LegendRow(
    colour: Color,
    label: String,
    sets: Double,
    total: Double,
    decimal: (Double) -> String,
) {
    val share = (sets / total * 100).roundToInt()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = "$label: $share percent, ${decimal(sets)} working sets"
            },
    ) {
        Surface(shape = CircleShape, color = colour, modifier = Modifier.size(12.dp)) {}
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
        Text("$share%", modifier = Modifier.widthIn(min = 44.dp))
        Text("${decimal(sets)} sets", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MuscleGroupBreakdown(
    overview: DashboardAnalytics.Strength,
    decimal: (Double) -> String,
) {
    val scheme = MaterialTheme.colorScheme
    val palette =
        listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.secondary,
            scheme.inversePrimary,
            scheme.tertiaryContainer,
            scheme.secondaryContainer,
            scheme.primaryContainer,
        )
    val unclassifiedColour = scheme.outline
    val primary = overview.muscleGroups.filter { it.primarySets > 0.0 }
    val total = overview.workingSets.toDouble()
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
                val segments =
                    primary.mapIndexed { i, g -> palette[i % palette.size] to g.primarySets } +
                        listOf(unclassifiedColour to overview.unclassifiedSets.toDouble())
                for ((colour, sets) in segments) {
                    if (sets <= 0.0) continue
                    val sweep = (sets / total * 360f).toFloat()
                    drawArc(
                        colour,
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
                    overview.workingSets.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text("sets", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    val legend: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            primary.forEachIndexed { i, group ->
                LegendRow(
                    palette[i % palette.size],
                    group.group.label,
                    group.primarySets,
                    total,
                    decimal,
                )
            }
            if (overview.unclassifiedSets > 0)
                LegendRow(
                    unclassifiedColour,
                    "Unclassified",
                    overview.unclassifiedSets.toDouble(),
                    total,
                    decimal,
                )
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
    val secondary = overview.muscleGroups.filter { it.secondarySets > 0 }
    if (secondary.isNotEmpty())
        Text(
            "Also involved secondarily (not added to the total): " +
                secondary.joinToString(", ") {
                    "${it.group.label} ${plural(it.secondarySets, "set")}"
                },
            style = MaterialTheme.typography.bodyMedium,
        )
    if (overview.unclassifiedSets > 0)
        Text(
            "Unclassified sets come from custom or changed exercises without catalogue muscle data.",
            style = MaterialTheme.typography.bodySmall,
        )
}
