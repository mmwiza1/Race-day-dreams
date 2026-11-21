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
    asOf: Instant? = null,
    openWorkout: ((String) -> Unit)? = null,
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
    val focus = state.primaryFocus
    var exerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val unit = state.massUnit
    fun mass(kilograms: Double) =
        "${presentation.decimal(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"
    fun volume(kilograms: Double) =
        "${groupedWhole(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"

    val runningWeek: @Composable () -> Unit = {
        HeroCard(Discipline.RUNNING) {
            DisciplineLabel("Distance this week")
            val week = overview.weekToDate
            val days = weekdaySpan(overview.weekDays)
            if (week.runs == 0) {
                Text("No runs recorded yet this week", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    week.distanceMetres?.let(distance) ?: "Distance unavailable",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dashboard-week-distance"),
                )
                Text(
                    "${plural(week.runs, "run")} · ${plural(week.runningDays, "running day")}" +
                        if (overview.weekDays == 7) " · full week" else " · $days so far"
                )
            }
            val last = overview.sameDaysLastWeek
            val lastDistance = last.distanceMetres
            Text(
                "Same days last week: " +
                    when {
                        last.runs == 0 -> "no runs"
                        lastDistance == null -> "distance unavailable"
                        else ->
                            distance(lastDistance) +
                                (week.distanceMetres?.let {
                                    " (${change(it - lastDistance, distance)})"
                                } ?: "")
                    },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("dashboard-week-comparison"),
            )
            if (week.missingDistance > 0)
                Text(
                    "${plural(week.missingDistance, "run")} without distance not added.",
                    style = MaterialTheme.typography.bodyMedium,
                )
        }
    }
    val runningKpis: @Composable () -> Unit = {
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
                    // An average from part of the period says how many runs qualified.
                    when {
                        current.pacedRuns == 0 -> "Needs distance and moving time"
                        current.pacedRuns < current.runs ->
                            "Moving pace · based on ${current.pacedRuns} of ${plural(current.runs, "run")}"
                        else -> "Moving pace · ${plural(current.runs, "run")}"
                    },
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
                            add(
                                if (completion.due > 0) "Planned runs due"
                                else "No planned runs due"
                            )
                            if (completion.skipped > 0) add("${completion.skipped} skipped")
                        }
                        .joinToString(" · "),
                ),
            )
        )
    }
    val weeklyDistance: @Composable () -> Unit = {
        CoachCard {
            Text("Weekly distance", style = MaterialTheme.typography.titleLarge)
            Text(
                "${period.label} · from ${presentation.shortDate(overview.window.start)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
            if (ProgressLink.TRENDS in links)
                TextButton(onClick = { open(ProgressLink.TRENDS) }) {
                    Text("Distance, pace and time trends")
                }
        }
    }
    val recentRuns: @Composable () -> Unit = {
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
    }
    @Composable
    fun sectionTitle(title: String) =
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    val runningLinks = listOf(ProgressLink.TRENDS, ProgressLink.PBS, ProgressLink.WEEKLY_REVIEW)
    val bodyLinks =
        listOf(ProgressLink.MILESTONES, ProgressLink.BODY_WEIGHT, ProgressLink.BODY_GUIDANCE)
    val strengthLinks = listOf(ProgressLink.STRENGTH_ANALYTICS)

    CoachHeading("Progress")
    // Tabs per discipline (Strava/Garmin-style). The person's focus opens first; every analytic
    // stays one tab away. Without strength data (no asOf), Progress is running-only, as before.
    val tabs =
        when {
            asOf == null -> emptyList()
            focus == org.racedaydreams.domain.settings.PrimaryFocus.HYBRID ->
                listOf(ProgressTab.OVERVIEW, ProgressTab.RUNNING, ProgressTab.STRENGTH)
            focus == org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH ->
                listOf(ProgressTab.STRENGTH, ProgressTab.RUNNING)
            else -> listOf(ProgressTab.RUNNING, ProgressTab.STRENGTH)
        }
    var tabName by rememberSaveable(tabs) { mutableStateOf(tabs.firstOrNull()?.name) }
    val tab = tabName?.let { ProgressTab.valueOf(it) } ?: ProgressTab.RUNNING
    if (tabs.size > 1) ProgressTabs(tabs, tab) { tabName = it.name }
    val moreLinks =
        when (tab) {
            ProgressTab.STRENGTH ->
                listOf(
                    "Strength training" to strengthLinks,
                    "Running" to listOf(ProgressLink.HISTORY) + runningLinks,
                    "Milestones & body" to bodyLinks,
                )
            else ->
                listOf(
                    "Running" to runningLinks,
                    "Milestones & body" to bodyLinks,
                    "Strength training" to strengthLinks,
                )
        }
    val strengthRecords: (() -> Unit)? =
        if (ProgressLink.STRENGTH_ANALYTICS in links) ({ open(ProgressLink.STRENGTH_ANALYTICS) })
        else null
    when (tab) {
        ProgressTab.STRENGTH -> {
            val strength =
                rememberStrengthPeriod(state, today, requireNotNull(asOf), period, exerciseId)
            StrengthHero(state, today, dateAt, presentation)
            PeriodSelector(period) { periodName = it.name }
            StrengthKpis(strength, strengthRecords)
            RecentWorkoutsCard(strength, presentation.friendlyDate, ::volume, null, openWorkout)
            OneRepMaxCard(
                strength,
                ::mass,
                presentation.shortDate,
                presentation.friendlyDate,
                { exerciseId = it },
                openWorkout ?: {},
            )
            MuscleWorkloadCard(strength, presentation.decimal)
        }
        ProgressTab.OVERVIEW -> {
            val strength = rememberStrengthPeriod(state, today, requireNotNull(asOf), period, null)
            runningWeek()
            StrengthHero(state, today, dateAt, presentation)
            PeriodSelector(period) { periodName = it.name }
            sectionTitle("Running")
            runningKpis()
            sectionTitle("Strength")
            StrengthKpis(strength, strengthRecords)
            recentRuns()
            RecentWorkoutsCard(strength, presentation.friendlyDate, ::volume, null, openWorkout)
        }
        ProgressTab.RUNNING -> {
            runningWeek()
            PeriodSelector(period) { periodName = it.name }
            runningKpis()
            weeklyDistance()
            recentRuns()
        }
    }
    MoreDetail(
        moreLinks.map { (name, group) -> name to group.filter { it in links } },
        { it.title },
        { it.summary },
        open,
    )
}

internal enum class ProgressTab(val label: String) {
    OVERVIEW("Overview"),
    RUNNING("Running"),
    // Not "Strength": that exact label belongs to the bottom navigation destination.
    STRENGTH("Strength training"),
}

@Composable
private fun ProgressTabs(
    tabs: List<ProgressTab>,
    selected: ProgressTab,
    choose: (ProgressTab) -> Unit,
) {
    TabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { choose(tab) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("progress-tab-${tab.name}"),
                text = { Text(tab.label) },
            )
        }
    }
}

/**
 * The strength counterpart of "Distance this week": workouts recorded since Monday and the last
 * one, from recorded sessions only (no estimate).
 */
@Composable
private fun StrengthHero(
    state: CoachUiState,
    today: LocalDate,
    dateAt: (Instant) -> LocalDate,
    presentation: HomePresentation,
) {
    val start = WeekAtAGlance.weekStart(today)
    val dates =
        remember(state.strengthSessions) { state.strengthSessions.map { dateAt(it.startedAt()) } }
    val thisWeek =
        dates.count {
            it.toEpochDay() >= start.toEpochDay() && it.toEpochDay() <= today.toEpochDay()
        }
    val last =
        dates.filter { it.toEpochDay() <= today.toEpochDay() }.maxByOrNull { it.toEpochDay() }
    HeroCard(Discipline.STRENGTH, Modifier.testTag("progress-strength-hero")) {
        DisciplineLabel("Strength this week")
        Text(
            if (thisWeek == 0) "No workouts yet this week" else plural(thisWeek, "workout"),
            style =
                if (thisWeek == 0) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            last?.let { "Last workout: ${presentation.friendlyDate(it)}" }
                ?: "Record a workout to start your history.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Scheduled programme sessions and those completed, using the calendar's gym link rules. */
internal fun programmeSchedule(s: CoachUiState): Pair<List<Pair<String, LocalDate>>, Set<String>>? {
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

/** The recorded strength overview and programme completion for one period, shared by tabs. */
internal class StrengthPeriod(
    val period: Period,
    val overview: DashboardAnalytics.Strength,
    val programme: DashboardAnalytics.PlanCompletion?,
)

@Composable
internal fun rememberStrengthPeriod(
    state: CoachUiState,
    today: LocalDate,
    asOf: Instant,
    period: Period,
    exerciseId: String?,
): StrengthPeriod {
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
    return StrengthPeriod(period, overview, programme)
}

@Composable
internal fun StrengthKpis(data: StrengthPeriod, openRecords: (() -> Unit)?) {
    val overview = data.overview
    val period = data.period
    val programme = data.programme
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
                "Recorded bests improved",
                openRecords,
            ),
        )
    )
}

@Composable
internal fun RecentWorkoutsCard(
    data: StrengthPeriod,
    friendlyDate: (LocalDate) -> String,
    volume: (Double) -> String,
    seeAll: (() -> Unit)?,
    openWorkout: ((String) -> Unit)?,
) = CoachCard {
    SectionHeader("Recent workouts", seeAll)
    if (data.overview.recentWorkouts.isEmpty()) Text("No strength workouts recorded yet.")
    for (workout in data.overview.recentWorkouts) {
        DashboardRow(
            title =
                workout.exerciseNames.take(3).joinToString(", ") +
                    if (workout.exerciseNames.size > 3) " +${workout.exerciseNames.size - 3}"
                    else "",
            detail =
                "${friendlyDate(workout.date)} · ${plural(workout.workingSets, "working set")}",
            // Accumulated load × repetitions, labelled so it is never read as a weight lifted,
            // a personal best or an estimated one-rep maximum.
            value = workout.loadVolumeKg?.let(volume) ?: "",
            secondary = if (workout.loadVolumeKg != null) "Training volume (load × reps)" else null,
            onClick = openWorkout?.let { open -> { open(workout.sessionId) } },
        )
    }
}

@Composable
internal fun OneRepMaxCard(
    data: StrengthPeriod,
    mass: (Double) -> String,
    shortDate: (LocalDate) -> String,
    friendlyDate: (LocalDate) -> String,
    selectExercise: (String) -> Unit,
    openWorkout: (String) -> Unit,
) = CoachCard {
    val overview = data.overview
    Text("Estimated 1RM trend", style = MaterialTheme.typography.titleLarge)
    val exercise = overview.trendExercise
    if (exercise == null) {
        Text("Record weighted working sets of 2–10 repetitions in this period to see an estimate.")
    } else {
        ExerciseSelector(overview.eligibleExercises.map { it.id() to it.name() }, exercise.id()) {
            selectExercise(it)
        }
        OneRepMaxChart(overview.oneRepMaxTrend, mass, shortDate, friendlyDate, openWorkout)
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

@Composable
internal fun MuscleWorkloadCard(data: StrengthPeriod, decimal: (Double) -> String) = CoachCard {
    Text("Muscle-group workload", style = MaterialTheme.typography.titleLarge)
    Text(
        "Recorded working sets · ${data.period.label}",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (data.overview.workingSets == 0) {
        Text("Record working sets to see how your training is spread.")
    } else {
        MuscleGroupBreakdown(data.overview, decimal)
    }
    Text(
        "Recorded workload only — not a measure of muscle growth or recovery.",
        style = MaterialTheme.typography.bodySmall,
    )
    Explainer("How sets are counted", StrengthVolume.attribution())
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
    val data = rememberStrengthPeriod(state, today, asOf, period, exerciseId)
    val unit = state.massUnit
    fun mass(kilograms: Double) =
        "${decimal(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"
    fun volume(kilograms: Double) =
        "${groupedWhole(unit.fromKilograms(kilograms))} ${unit.name.lowercase()}"
    CoachHeading("Strength")
    if (StrengthLink.GYM_NOW in links)
        Button(
            onClick = { open(StrengthLink.GYM_NOW) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(StrengthLink.GYM_NOW.title)
        }
    // The two other frequent jobs sit directly under Gym now as secondary actions.
    QuickStartRow(
        listOf(StrengthLink.LOG, StrengthLink.PROGRAMME)
            .filter { it in links }
            .map { link -> link.title to { open(link) } }
    )

    PeriodSelector(period) { periodName = it.name }

    StrengthKpis(data, if (StrengthLink.PBS in links) ({ open(StrengthLink.PBS) }) else null)

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

    RecentWorkoutsCard(
        data,
        friendlyDate,
        ::volume,
        if (StrengthLink.HISTORY in links) ({ open(StrengthLink.HISTORY) }) else null,
        openWorkout,
    )
    OneRepMaxCard(data, ::mass, shortDate, friendlyDate, { exerciseId = it }, openWorkout)
    MuscleWorkloadCard(data, decimal)

    MoreDetail(
        listOf(
                "Train" to listOf(StrengthLink.EXERCISES, StrengthLink.BOULDERING),
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

/** A whole number with thousands separators, e.g. 2100.0 → "2,100". */
internal fun groupedWhole(value: Double): String {
    val digits = kotlin.math.abs(kotlin.math.round(value)).toLong().toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (value < -0.5) "−$grouped" else grouped
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
        // Design system: the figure leads (headline size), the label and context recede.
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                tile.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                tile.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                tile.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
