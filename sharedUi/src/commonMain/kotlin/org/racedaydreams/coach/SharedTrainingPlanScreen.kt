package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.*

/**
 * Existing running-plan presentation. Real locale, optional routine evidence and actions are
 * required host bindings.
 *
 * Composition (W2): a compact journey header (race distance, "Plan", the week and phase, and a
 * phase track to race day), "This week" with a tonal week navigator, one borderless panel per day
 * with discipline glyph discs (key sessions emphasised, rest muted), then movement support,
 * adjustments and the programme details as a milestone list. Scheduling and data are unchanged.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SharedTrainingPlanScreen(
    state: CoachUiState,
    dates: CoachCalendarDates,
    weekRangeLabel: (LocalDate) -> String,
    distanceLabel: (Double) -> String,
    generate: () -> Unit,
    decide: (AdjustmentProposal, String) -> Unit,
    open: (String) -> Unit,
    editGoal: () -> Unit,
    strength: (LocalDate) -> Unit,
    routine: (String, String?) -> Unit,
    warmup: @Composable (String, String, () -> Unit) -> Unit,
    mobility: @Composable () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var week by rememberSaveable { mutableIntStateOf(0) }
    val version = state.currentPlan
    val today = dates.today()
    CoachPage {
        if (version == null) {
            ScreenHeader(Discipline.RUNNING, Glyph.FLAG, "Your programme", "Plan")
            Panel {
                Text(
                    "Your next chapter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Create a programme from your goal and availability, or record runs without a plan."
                )
                CoachButton("Generate my programme") { generate() }
            }
            WhyDetails(
                "Missing or stale workload evidence starts with short conversational run/walk sessions. A short race horizon never triggers a catch-up build."
            )
            CoachButton("Edit goal and routine", editGoal)
        } else {
            val plan = version.plan()
            val goal = version.goalSnapshot().raceGoal()
            val startWeek = dates.weekStart(plan.startDate())
            val endWeek = dates.weekStart(plan.endDate())
            val currentWeek = dates.weekStart(today)
            val baseWeek =
                CalendarDates.ofEpochDay(
                    currentWeek.toEpochDay().coerceIn(startWeek.toEpochDay(), endWeek.toEpochDay())
                )
            val selectedWeek =
                CalendarDates.ofEpochDay(
                    baseWeek
                        .plusWeeks(week.toLong())
                        .toEpochDay()
                        .coerceIn(startWeek.toEpochDay(), endWeek.toEpochDay())
                )
            val totalWeeks = (endWeek.toEpochDay() - startWeek.toEpochDay()) / 7 + 1
            val weekNumber = (selectedWeek.toEpochDay() - startWeek.toEpochDay()) / 7 + 1
            val journeyWeek = (baseWeek.toEpochDay() - startWeek.toEpochDay()) / 7 + 1
            val journey =
                remember(plan, today, goal) {
                    planJourney(
                        plan.startDate(),
                        plan.endDate(),
                        plan.phases(),
                        today,
                        goal.raceDate(),
                    )
                }
            JourneyHeader(
                goal.distance().label(),
                "Week $journeyWeek of $totalWeeks" +
                    (journey.currentPhase?.let { " · ${phaseName(it.kind())}" } ?: ""),
                journey,
                "Started ${dates.label(plan.startDate())}",
                goal
                    .raceDate()
                    ?.takeIf { journey.raceFraction != null }
                    ?.let { "Race day · ${dates.label(it)}" }
                    ?: "Programme ends · ${dates.label(plan.endDate())}",
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel(
                    "This week",
                    Glyph.CALENDAR,
                    ChartHue.BLUE,
                    if (selectedWeek == currentWeek) "The week you are in"
                    else "Week $weekNumber of $totalWeeks",
                )
                WeekNavigator(
                    label = weekRangeLabel(selectedWeek),
                    previousEnabled = selectedWeek.toEpochDay() > startWeek.toEpochDay(),
                    nextEnabled =
                        selectedWeek.plusWeeks(1).toEpochDay() <= plan.endDate().toEpochDay(),
                    previous = { week-- },
                    next = { week++ },
                )
                if (week != 0)
                    TextButton(onClick = { week = 0 }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Current week")
                    }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(7) { offset ->
                    val day = selectedWeek.plusDays(offset.toLong())
                    val entries = sharedCalendarEntries(state, day, today)
                    val runs = plan.workouts().filter { it.date() == day }
                    // Calendar remains the authority for explicit rest, moved and skipped states.
                    PlanDay(day, day == today, entries) { entry ->
                        val workout = runs.firstOrNull { it.id() == entry.id() }
                        if (workout != null)
                            RunSession(entry, workout, distanceLabel, open) {
                                val warm =
                                    org.racedaydreams.domain.support.RunningRoutines.warmup(
                                        workout.category(),
                                        goal.distance().kilometres(),
                                    )
                                warmup(warm, workout.id()) { routine(warm, workout.id()) }
                                if (
                                    workout.category() == WorkoutCategory.LONG_RUN ||
                                        AdaptiveCoaching.isQuality(workout.category()) ||
                                        workout.category() == WorkoutCategory.RACE
                                )
                                    TextButton(
                                        onClick = { routine("cool-demanding", workout.id()) },
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                    ) {
                                        Text(
                                            "Cool-down · 6 min",
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                            }
                        else
                            when {
                                entry.category() == CalendarData.Category.REST ->
                                    SessionLine(Glyph.SPARK, null, DiscTone.MUTED) {
                                        Text(
                                            "Rest Day",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            "Recover and prepare for your next session.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                entry.category() == CalendarData.Category.STRENGTH ->
                                    StrengthSession(entry) { strength(day) }
                                else ->
                                    SessionLine(entry.glyph(), entry.discipline(), entry.tone()) {
                                        Text(
                                            requireNotNull(entry.label()),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            requireNotNull(entry.state())
                                                .name
                                                .lowercase()
                                                .replace('_', ' '),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                            }
                    }
                }
            }
            mobility()
            state.proposals.forEach { proposal ->
                AdjustmentCard(proposal, state.busy) { decide(proposal, it) }
            }
            TextButton(
                onClick = { details = !details },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (details) "Hide programme details" else "Programme details")
            }
            if (details) {
                Panel {
                    Text(
                        "${dates.label(plan.startDate())} – ${dates.label(plan.endDate())}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Saved plan version ${version.version()} · ${state.planVersions.size} preserved versions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WhyDetails(version.reason(), "Why this plan version?")
                    WhyDetails(version.evidenceSnapshot(), "Evidence used for this plan")
                    if (state.profile != version.goalSnapshot())
                        Text(
                            "Your saved goal or availability has changed. This programme retains its original snapshot until you explicitly rebuild."
                        )
                }
                if (plan.phases().isNotEmpty()) {
                    SectionLabel("Milestones", Glyph.FLAG, ChartHue.BLUE)
                    Panel {
                        val phases = plan.phases()
                        Column {
                            phases.forEachIndexed { index, phase ->
                                PhaseMilestone(
                                    phaseName(phase.kind()),
                                    "${dates.label(phase.startDate())} – ${dates.label(phase.endDate())}",
                                    past = phase.endDate().toEpochDay() < today.toEpochDay(),
                                    current = phase == journey.currentPhase,
                                    last = index == phases.lastIndex,
                                ) {
                                    phase.explanation()?.let { WhyDetails(it) }
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { confirm = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Rebuild remaining programme…")
                }
                CoachButton("Edit goal and routine", editGoal)
            }
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Rebuild your remaining programme?") },
            text = {
                Text(
                    "Use your current goal, availability and evidence from today onward. Future sessions may change. Previous prescriptions remain in history. No catch-up mileage is added."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        generate()
                    }
                ) {
                    Text("Rebuild")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Keep current plan") }
            },
        )
}

/** The selected week as a tonal pill: previous and next as 48 dp targets around its dates. */
@Composable
private fun WeekNavigator(
    label: String,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    previous: () -> Unit,
    next: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth().testTag("plan-week-navigator"),
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            NavigatorButton("Previous week", previousEnabled, flip = true, previous)
            Text(
                label,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            NavigatorButton("Next week", nextEnabled, flip = false, next)
        }
    }
}

@Composable
private fun NavigatorButton(
    description: String,
    enabled: Boolean,
    flip: Boolean,
    onClick: () -> Unit,
) {
    val ink = LocalContentColor.current
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier =
            Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                contentDescription = description
            },
    ) {
        Box(Modifier.graphicsLayer { if (flip) rotationZ = 180f }) {
            GlyphIcon(Glyph.CHEVRON, if (enabled) ink else ink.copy(alpha = .38f), 22.dp)
        }
    }
}

/**
 * One day of the plan in a borderless panel: a date column (today in a filled disc) and the day's
 * sessions. A day with a long run, quality session or race is tinted in the running colour; a day
 * with nothing to train is drawn flat and muted.
 */
@Composable
private fun PlanDay(
    day: LocalDate,
    isToday: Boolean,
    entries: List<CalendarData.Entry>,
    session: @Composable (CalendarData.Entry) -> Unit,
) {
    val key = entries.any { it.isKey() }
    val training = entries.any { it.discipline() != null }
    val large = LocalDensity.current.fontScale >= 1.5f
    Surface(
        color =
            when {
                key -> Discipline.RUNNING.container().copy(alpha = .6f)
                training || isToday -> MaterialTheme.colorScheme.surfaceContainerLow
                else -> Color.Transparent
            },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().testTag("plan-day-$day"),
    ) {
        val content: @Composable () -> Unit = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (key)
                    Eyebrow(
                        "Key session",
                        MaterialTheme.typography.labelSmall,
                        Discipline.RUNNING.accent(),
                    )
                if (entries.isEmpty())
                    Column {
                        Text(
                            "No session planned",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Your day, your choice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                entries.forEach { session(it) }
            }
        }
        if (large)
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateColumn(day, isToday, inline = true)
                content()
            }
        else
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DateColumn(day, isToday, inline = false)
                Box(Modifier.weight(1f)) { content() }
            }
    }
}

/** Weekday over a bold day number; today's number sits in a filled running-colour disc. */
@Composable
private fun DateColumn(day: LocalDate, isToday: Boolean, inline: Boolean) {
    val weekday = day.getDayOfWeek().name
    val accent = Discipline.RUNNING.accent()
    val spoken =
        weekday.lowercase().replaceFirstChar { it.uppercase() } +
            " ${day.getDayOfMonth()}" +
            if (isToday) ", today" else ""
    val number: @Composable () -> Unit = {
        Box(
            Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
                .clip(CircleShape)
                .background(if (isToday) accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.getDayOfMonth()}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color =
                    if (isToday) Discipline.RUNNING.onAccent()
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                // The disc grows with the number at large text instead of clipping it.
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
    val name: @Composable () -> Unit = {
        Text(
            weekday.take(3),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (inline)
        Row(
            Modifier.clearAndSetSemantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            number()
            name()
        }
    else
        Column(
            Modifier.widthIn(min = 40.dp).clearAndSetSemantics { contentDescription = spoken },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            name()
            number()
        }
}

/** A glyph disc beside a session's text; the disc is decorative. */
@Composable
private fun SessionLine(
    glyph: Glyph,
    discipline: Discipline?,
    tone: DiscTone,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionDisc(
            glyph,
            discipline ?: Discipline.RECOVERY,
            if (discipline == null) DiscTone.MUTED else tone,
        )
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/** A planned run: opens its workout and purpose; its warm-up and cool-down links follow. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RunSession(
    entry: CalendarData.Entry,
    workout: PlannedWorkout,
    distanceLabel: (Double) -> String,
    open: (String) -> Unit,
    routines: @Composable () -> Unit,
) {
    val key = entry.isKey()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionDisc(entry.glyph(), Discipline.RUNNING, entry.tone())
            TextButton(
                onClick = { open(workout.id()) },
                modifier =
                    Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        contentDescription = "View workout and purpose"
                        stateDescription =
                            requireNotNull(entry.state()).name.lowercase().replace('_', ' ')
                    },
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        entry.label() +
                            (if (entry.state() == CalendarData.State.COMPLETED) " ✓" else ""),
                        style =
                            if (key) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                        fontWeight = if (key) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        workout.distanceKm()?.let { distanceLabel(it * 1000) }
                            ?: workout.duration()?.let { "${it.getSeconds() / 60} min" }
                            ?: "Run by effort",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
            }
        }
        FlowRow(
            Modifier.padding(start = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        ) {
            routines()
        }
    }
}

/** A strength session: opens the day's strength workout, recorded or planned. */
@Composable
private fun StrengthSession(entry: CalendarData.Entry, onClick: () -> Unit) {
    val done = entry.state() == CalendarData.State.COMPLETED
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionDisc(Glyph.DUMBBELL, Discipline.STRENGTH, entry.tone())
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.label() + (if (done) " ✓" else ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (done) "Recorded · View strength" else "View strength workout",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            GlyphIcon(Glyph.CHEVRON, MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
        }
    }
}

@Composable
fun AdjustmentCard(proposal: AdjustmentProposal, busy: Boolean, decide: (String) -> Unit) =
    SemanticCard(Tile.Insight) {
        Text("Plan adjustment suggested", style = MaterialTheme.typography.titleMedium)
        Text(proposal.reason())
        Text(proposal.impact())
        Text(proposal.confidence())
        proposal.options().forEach { option ->
            OutlinedButton(
                onClick = { decide(option.id()) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(option.label())
            }
        }
    }
