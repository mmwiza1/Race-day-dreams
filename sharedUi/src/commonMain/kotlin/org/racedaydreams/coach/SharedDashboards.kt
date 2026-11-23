@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.presentation.DashboardAnalytics
import org.racedaydreams.domain.presentation.DashboardAnalytics.Period
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

    val sameDays = overview.sameDaysLastWeek
    val sameDaysDistance = sameDays.distanceMetres
    val comparison =
        "Same days last week: " +
            when {
                sameDays.runs == 0 -> "no runs"
                sameDaysDistance == null -> "distance unavailable"
                else ->
                    distance(sameDaysDistance) +
                        (overview.weekToDate.distanceMetres?.let {
                            " (${change(it - sameDaysDistance, distance)})"
                        } ?: "")
            }
    val explorer: @Composable () -> Unit = {
        if (!overview.hasAnyRun()) Text("Record or import runs to see your trend.")
        RunningTrendExplorer(
            overview.weeklyTrend,
            state.activities,
            today,
            dateAt,
            state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
            distance,
            presentation.decimal,
            presentation.paceSeconds,
            presentation.shortDate,
            presentation.friendlyDate,
            comparison,
            openRun,
            if (ProgressLink.HISTORY in links) ({ open(ProgressLink.HISTORY) }) else null,
            if (ProgressLink.TRENDS in links) ({ open(ProgressLink.TRENDS) }) else null,
        )
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
    val runningLinks = listOf(ProgressLink.TRENDS, ProgressLink.PBS, ProgressLink.WEEKLY_REVIEW)
    val bodyLinks =
        listOf(ProgressLink.MILESTONES, ProgressLink.BODY_WEIGHT, ProgressLink.BODY_GUIDANCE)
    val strengthLinks = listOf(ProgressLink.STRENGTH_ANALYTICS)

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
    // Hero: Progress's identity, tinted by the discipline of the selected tab.
    // Compact: Progress has no primary action, so the charts, not the header, lead the screen.
    ScreenHeader(
        when (tab) {
            ProgressTab.STRENGTH -> Discipline.STRENGTH
            ProgressTab.OVERVIEW -> Discipline.RECOVERY
            ProgressTab.RUNNING -> Discipline.RUNNING
        },
        if (tab == ProgressTab.STRENGTH) Glyph.DUMBBELL else Glyph.TREND,
        eyebrow = if (tabs.size > 1) tab.label else "Running",
        title = "Progress",
    )
    if (tabs.size > 1) ProgressTabs(tabs, tab) { tabName = it.name }
    val moreLinks =
        when (tab) {
            ProgressTab.STRENGTH ->
                listOf(
                    ProgressLinkGroup.STRENGTH to strengthLinks,
                    ProgressLinkGroup.RUNNING to listOf(ProgressLink.HISTORY) + runningLinks,
                    ProgressLinkGroup.BODY to bodyLinks,
                )
            else ->
                listOf(
                    ProgressLinkGroup.RUNNING to runningLinks,
                    ProgressLinkGroup.BODY to bodyLinks,
                    ProgressLinkGroup.STRENGTH to strengthLinks,
                )
        }
    val strengthRecords: (() -> Unit)? =
        if (ProgressLink.STRENGTH_ANALYTICS in links) ({ open(ProgressLink.STRENGTH_ANALYTICS) })
        else null
    val tabDiscipline =
        when (tab) {
            ProgressTab.STRENGTH -> Discipline.STRENGTH
            ProgressTab.OVERVIEW -> Discipline.RECOVERY
            ProgressTab.RUNNING -> Discipline.RUNNING
        }
    val periodSelector: @Composable () -> Unit = {
        PeriodSelector(period, tabDiscipline) { periodName = it.name }
    }
    when (tab) {
        // Strength (V3): exercise progression with actual lifts first, then volume, workload,
        // supporting figures and recent workouts.
        ProgressTab.STRENGTH -> {
            val strength =
                rememberStrengthPeriod(state, today, requireNotNull(asOf), period, exerciseId)
            periodSelector()
            ExerciseProgressionCard(
                strength,
                ::mass,
                presentation.shortDate,
                presentation.friendlyDate,
                { exerciseId = it },
                openWorkout ?: {},
            )
            TrainingVolumeCard(
                strength,
                ::volume,
                presentation.shortDate,
                presentation.friendlyDate,
                openWorkout,
            )
            MuscleWorkloadCard(strength, presentation.decimal)
            SectionLabel("At a glance", Glyph.DUMBBELL, ChartHue.PLUM, "${period.label} · recorded")
            StrengthKpis(strength, strengthRecords)
            RecentWorkoutsCard(strength, presentation.friendlyDate, ::volume, null, openWorkout)
        }
        // Hybrid: one split summary, then the running chart, then both sets of figures.
        ProgressTab.OVERVIEW -> {
            val strength = rememberStrengthPeriod(state, today, requireNotNull(asOf), period, null)
            SplitSummary(state, overview, today, dateAt, presentation)
            periodSelector()
            explorer()
            // Both disciplines' figures at the same depth: one labelled section each, in the
            // running (blue) and strength (plum) hues.
            SectionLabel("Running", Glyph.RUN, ChartHue.BLUE, "${period.label} · recorded")
            runningKpis()
            SectionLabel("Strength", Glyph.DUMBBELL, ChartHue.PLUM, "${period.label} · recorded")
            StrengthKpis(strength, strengthRecords)
            RecentWorkoutsCard(strength, presentation.friendlyDate, ::volume, null, openWorkout)
        }
        // Running: chart first, with the week's runs as its drill-down, then the figures.
        ProgressTab.RUNNING -> {
            periodSelector()
            explorer()
            SectionLabel("At a glance", Glyph.RUN, ChartHue.BLUE, "${period.label} · recorded")
            runningKpis()
        }
    }
    ProgressDrillDowns(
        moreLinks
            .map { (group, items) -> group to items.filter { it in links } }
            .filter { it.second.isNotEmpty() },
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
 * Hybrid summary without stacked cards: running and strength this week side by side on one
 * borderless panel, from the existing week-to-date running summary and recorded strength sessions.
 */
@Composable
private fun SplitSummary(
    state: CoachUiState,
    overview: DashboardAnalytics.Running,
    today: LocalDate,
    dateAt: (Instant) -> LocalDate,
    presentation: HomePresentation,
) {
    val start = WeekAtAGlance.weekStart(today)
    val dates =
        remember(state.strengthSessions) { state.strengthSessions.map { dateAt(it.startedAt()) } }
    val workouts =
        dates.count {
            it.toEpochDay() >= start.toEpochDay() && it.toEpochDay() <= today.toEpochDay()
        }
    val week = overview.weekToDate
    val running: @Composable (Modifier) -> Unit = { modifier ->
        SummaryColumn(
            Discipline.RUNNING,
            Glyph.RUN,
            "Running this week",
            // A compact count only: the chart directly below owns the large weekly figure.
            if (week.runs == 0) "No runs" else plural(week.runs, "run"),
            state.activities
                .map { dateAt(it.start()) }
                .filter { it.toEpochDay() <= today.toEpochDay() }
                .maxByOrNull { it.toEpochDay() }
                ?.let { "Last: ${presentation.shortDate(it)}" } ?: "No runs recorded",
            modifier.testTag("progress-running-summary"),
        )
    }
    val strength: @Composable (Modifier) -> Unit = { modifier ->
        SummaryColumn(
            Discipline.STRENGTH,
            Glyph.DUMBBELL,
            "Strength this week",
            if (workouts == 0) "None yet" else plural(workouts, "workout"),
            dates
                .filter { it.toEpochDay() <= today.toEpochDay() }
                .maxByOrNull { it.toEpochDay() }
                ?.let { "Last: ${presentation.shortDate(it)}" } ?: "No workouts recorded",
            modifier.testTag("progress-strength-hero"),
        )
    }
    // One borderless panel holds both disciplines at the same depth, as Home's hybrid day does:
    // the same glyph disc, label, figure and last date for each, in its discipline colour.
    // Side by side on phones; stacked at very large text so neither figure is squeezed.
    val stacked = LocalDensity.current.fontScale >= 1.5f
    Panel(
        Modifier.testTag("progress-hybrid-summary"),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        if (stacked) {
            running(Modifier.fillMaxWidth())
            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            strength(Modifier.fillMaxWidth())
        } else
            Row(Modifier.height(IntrinsicSize.Min)) {
                running(Modifier.weight(1f))
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                strength(Modifier.weight(1f))
            }
    }
}

@Composable
private fun SummaryColumn(
    discipline: Discipline,
    glyph: Glyph,
    label: String,
    value: String,
    detail: String,
    modifier: Modifier,
) {
    Column(
        modifier.padding(horizontal = 20.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Decorative: the label names the discipline.
        Box(
            Modifier.size(36.dp)
                .background(discipline.accent(), CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, discipline.onAccent(), 20.dp)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = discipline.accent(),
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
) =
    ExpressiveSection(
        "Recent workouts",
        Glyph.LIST,
        ChartHue.PLUM,
        trailing = seeAll?.let { { TextButton(onClick = it) { Text("See all") } } },
    ) {
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
                secondary =
                    if (workout.loadVolumeKg != null) "Training volume (load × reps)" else null,
                onClick = openWorkout?.let { open -> { open(workout.sessionId) } },
            )
        }
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
    // Hero: the tab's identity, one line of context from recorded workouts, Gym now as the
    // primary action and the two other frequent jobs as quieter secondary actions.
    ScreenHero(
        Discipline.STRENGTH,
        Glyph.DUMBBELL,
        eyebrow = "Strength training",
        title = "Strength",
        supporting =
            data.overview.recentWorkouts.firstOrNull()?.let {
                "Last workout ${friendlyDate(it.date)}"
            } ?: "Start a workout or log one you've done.",
        primary =
            if (StrengthLink.GYM_NOW in links)
                StrengthLink.GYM_NOW.title to { open(StrengthLink.GYM_NOW) }
            else null,
        secondary =
            listOf(StrengthLink.LOG, StrengthLink.PROGRAMME)
                .filter { it in links }
                .map { link -> link.title to { open(link) } },
    )

    PeriodSelector(period) { periodName = it.name }

    // V3 hierarchy: exercise progression (with actual lifts), volume, workload, then figures.
    ExerciseProgressionCard(data, ::mass, shortDate, friendlyDate, { exerciseId = it }, openWorkout)
    TrainingVolumeCard(data, ::volume, shortDate, friendlyDate, openWorkout)
    MuscleWorkloadCard(data, decimal)
    SectionLabel("At a glance", Glyph.DUMBBELL, ChartHue.PLUM, "${data.period.label} · recorded")
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
internal fun PeriodSelector(selected: Period, select: (Period) -> Unit) =
    PeriodSelector(selected, null, select)

/**
 * The reporting period. With a [discipline] (Progress and Training trends), the choices are
 * borderless tonal capsules and the selected one takes the discipline's container colour with a
 * checkmark, so the selection never relies on colour alone. Wraps at large text and on narrow
 * screens; every choice stays at least 48 dp tall.
 */
@Composable
internal fun PeriodSelector(selected: Period, discipline: Discipline?, select: (Period) -> Unit) {
    val reporting = Modifier.semantics { contentDescription = "Reporting period" }
    if (discipline != null) {
        // One row at normal text on phones; wraps at 150%+ text or on narrow screens.
        TonalPills(
            Period.values().map { it to it.label },
            selected,
            select,
            selectedContainer = discipline.container(),
            selectedContent = discipline.onContainer(),
            unselectedContainer = MaterialTheme.colorScheme.surfaceContainerLow,
            unselectedContent = MaterialTheme.colorScheme.onSurfaceVariant,
            tag = { "period-${it.name}" },
            modifier = reporting,
        )
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = reporting,
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
    // Flat figures, not cards: a divider above, the figure leading, label and context receding.
    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))
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
            if (tile.onClick != null)
                Text(
                    "Details ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
        }
    }
    val tagged = modifier.semantics(mergeDescendants = true) {}.testTag("kpi-${tile.title}")
    val click = tile.onClick
    if (click != null)
        Surface(
            onClick = click,
            modifier = tagged.heightIn(min = 48.dp),
            color = Color.Transparent,
        ) {
            content()
        }
    else Box(tagged) { content() }
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
internal fun ExerciseSelector(
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
