package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.*

/**
 * Existing running-plan presentation. Real locale, optional routine evidence and actions are
 * required host bindings.
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
            CoachHeading("Plan")
            GuidanceCard {
                Text("Your next chapter", style = MaterialTheme.typography.titleLarge)
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CoachHeading("Plan")
                Text(
                    "${version.goalSnapshot().raceGoal().distance().label()} · Week $weekNumber of $totalWeeks",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            val largeFont = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.15f
            if (largeFont) Text("This week", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!largeFont)
                    Text(
                        "This week",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                TextButton(
                    onClick = { week-- },
                    enabled = selectedWeek.toEpochDay() > startWeek.toEpochDay(),
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(
                        "‹",
                        modifier = Modifier.semantics { contentDescription = "Previous week" },
                    )
                }
                Text(
                    weekRangeLabel(selectedWeek),
                    modifier = if (largeFont) Modifier.weight(1f) else Modifier,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
                TextButton(
                    onClick = { week++ },
                    enabled = selectedWeek.plusWeeks(1).toEpochDay() <= plan.endDate().toEpochDay(),
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text("›", modifier = Modifier.semantics { contentDescription = "Next week" })
                }
            }
            if (week != 0) TextButton(onClick = { week = 0 }) { Text("Current week") }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(7) { offset ->
                    val day = selectedWeek.plusDays(offset.toLong())
                    val entries = sharedCalendarEntries(state, day, today)
                    val runs = plan.workouts().filter { it.date() == day }
                    // Calendar remains the authority for explicit rest, moved and skipped states.
                    GuidanceCard(
                        if (
                            entries.any {
                                it.category() in
                                    listOf(
                                        CalendarData.Category.EASY,
                                        CalendarData.Category.QUALITY,
                                        CalendarData.Category.LONG,
                                        CalendarData.Category.RACE,
                                    )
                            }
                        )
                            guidanceBlue()
                        else MaterialTheme.colorScheme.surface,
                        padding = 12.dp,
                        spacing = 0.dp,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.widthIn(min = 36.dp),
                            ) {
                                Text(
                                    day.getDayOfWeek().name.take(3),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Text(
                                    "${day.getDayOfMonth()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (entries.isEmpty()) {
                                    Text(
                                        "No session planned",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "Your day, your choice",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                entries.forEach { entry ->
                                    val workout = runs.firstOrNull { it.id() == entry.id() }
                                    if (workout != null) {
                                        TextButton(
                                            onClick = { open(workout.id()) },
                                            modifier =
                                                Modifier.fillMaxWidth()
                                                    .heightIn(min = 48.dp)
                                                    .semantics {
                                                        contentDescription =
                                                            "View workout and purpose"
                                                        stateDescription =
                                                            requireNotNull(entry.state())
                                                                .name
                                                                .lowercase()
                                                                .replace('_', ' ')
                                                    },
                                            shape = MaterialTheme.shapes.small,
                                            contentPadding =
                                                PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Column(
                                                Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Text(
                                                    entry.label() +
                                                        (if (
                                                            entry.state() ==
                                                                CalendarData.State.COMPLETED
                                                        )
                                                            " ✓"
                                                        else ""),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                Text(
                                                    workout.distanceKm()?.let {
                                                        distanceLabel(it * 1000)
                                                    }
                                                        ?: workout.duration()?.let {
                                                            "${it.getSeconds() / 60} min"
                                                        }
                                                        ?: "Run by effort",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            Text("›", style = MaterialTheme.typography.titleLarge)
                                        }
                                        val warm =
                                            org.racedaydreams.domain.support.RunningRoutines.warmup(
                                                workout.category(),
                                                version
                                                    .goalSnapshot()
                                                    .raceGoal()
                                                    .distance()
                                                    .kilometres(),
                                            )
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(CoachSpace.Chip),
                                            verticalArrangement =
                                                Arrangement.spacedBy(CoachSpace.Chip),
                                        ) {
                                            warmup(warm, workout.id()) {
                                                routine(warm, workout.id())
                                            }
                                            if (
                                                workout.category() == WorkoutCategory.LONG_RUN ||
                                                    AdaptiveCoaching.isQuality(
                                                        workout.category()
                                                    ) ||
                                                    workout.category() == WorkoutCategory.RACE
                                            )
                                                TextButton(
                                                    onClick = {
                                                        routine("cool-demanding", workout.id())
                                                    },
                                                    contentPadding = PaddingValues(0.dp),
                                                ) {
                                                    Text(
                                                        "Cool-down · 6 min",
                                                        style = MaterialTheme.typography.labelMedium,
                                                    )
                                                }
                                        }
                                    } else
                                        when {
                                            entry.category() == CalendarData.Category.REST -> {
                                                Text(
                                                    "Rest Day",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    "Recover and prepare for your next session.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                )
                                            }
                                            entry.category() == CalendarData.Category.STRENGTH ->
                                                TextButton(
                                                    onClick = { strength(day) },
                                                    modifier =
                                                        Modifier.fillMaxWidth()
                                                            .heightIn(min = 48.dp),
                                                    shape = MaterialTheme.shapes.small,
                                                    contentPadding =
                                                        PaddingValues(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp,
                                                        ),
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            entry.label() +
                                                                (if (
                                                                    entry.state() ==
                                                                        CalendarData.State.COMPLETED
                                                                )
                                                                    " ✓"
                                                                else ""),
                                                            style =
                                                                MaterialTheme.typography
                                                                    .titleMedium,
                                                            color =
                                                                MaterialTheme.colorScheme.onSurface,
                                                        )
                                                        Text(
                                                            if (
                                                                entry.state() ==
                                                                    CalendarData.State.COMPLETED
                                                            )
                                                                "Recorded · View strength"
                                                            else "View strength workout",
                                                            style =
                                                                MaterialTheme.typography.bodySmall,
                                                        )
                                                    }
                                                    Text("›")
                                                }
                                            else -> {
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
                                                )
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
            mobility()
            state.proposals.forEach { proposal ->
                AdjustmentCard(proposal, state.busy) { decide(proposal, it) }
            }
            TextButton(onClick = { details = !details }) {
                Text(if (details) "Hide programme details" else "Programme details")
            }
            if (details) {
                Text("${dates.label(plan.startDate())} – ${dates.label(plan.endDate())}")
                Text(
                    "Saved plan version ${version.version()} · ${state.planVersions.size} preserved versions"
                )
                WhyDetails(version.reason(), "Why this plan version?")
                WhyDetails(version.evidenceSnapshot(), "Evidence used for this plan")
                if (state.profile != version.goalSnapshot())
                    Text(
                        "Your saved goal or availability has changed. This programme retains its original snapshot until you explicitly rebuild."
                    )
                plan.phases().forEach { phase ->
                    CoachCard {
                        Text(
                            phase.kind().name.lowercase().replace('_', ' ').replaceFirstChar {
                                it.uppercase()
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text("${dates.label(phase.startDate())} – ${dates.label(phase.endDate())}")
                        phase.explanation()?.let { WhyDetails(it) }
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
