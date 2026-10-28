@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.domain.strength.GymNowWorkouts
import org.racedaydreams.domain.strength.GymProgramme

/** Every entry starts the same durable engine; none silently replaces a programme session. */
@Composable
fun GymNowScreen(
    model: GymNowViewModel,
    onWorkout: () -> Unit,
    onBuild: () -> Unit,
    onBack: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(model) { model.refresh() }
    var section by rememberSaveable { mutableStateOf("Overview") }
    var discardId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReload by rememberSaveable { mutableStateOf(false) }
    var plannedLimit by rememberSaveable { mutableStateOf(7) }
    val mutable = !state.loading && !state.busy && state.view?.pending() == null
    val mayStart = mutable && state.unfinished.isEmpty()
    BackHandler(enabled = section != "Overview") { section = "Overview" }

    CoachPage {
        TextButton(
            onClick = { if (section == "Overview") onBack() else section = "Overview" },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text(if (section == "Overview") "Back" else "Back to Gym Now")
        }
        CoachHeading(if (section == "Overview") "Gym Now" else section)
        if (state.loading) CircularProgressIndicator()
        if (state.busy && !state.loading)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let { message ->
            SemanticCard(Tile.Warning) {
                Text(message)
                TextButton(onClick = model::clearMessage) { Text("Dismiss message") }
            }
        }
        state.notice?.let { message -> SemanticCard(Tile.Insight) { Text(message) } }
        if (state.view?.pending() != null) {
            SemanticCard(Tile.Warning) {
                Text("Save needs acknowledgement", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your entered values are retained. Retry the same write before changing this workout."
                )
                Button(onClick = model::retry, enabled = !state.busy) { Text("Retry saved action") }
                TextButton(onClick = { confirmReload = true }, enabled = !state.busy) {
                    Text("Review saved evidence instead")
                }
            }
        }
        state.unfinished.forEach { saved ->
            val attached = state.view?.accepted()?.execution()?.id() == saved.id()
            SemanticCard(Tile.Insight) {
                Text(
                    saved.title().ifBlank { "Unfinished workout" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${saved.actualSetCount()} recorded sets · ${saved.status().name.lowercase(Locale.UK).replace('_', ' ')}"
                )
                Text(
                    if (attached)
                        "Your workout remains on this device. Open it to continue or finish."
                    else
                        "Recorded evidence is safe. Review this unfinished workout; any interruption is kept as a gap."
                )
                Button(
                    onClick = {
                        if (attached) onWorkout() else model.recover(saved.id(), onWorkout)
                    },
                    enabled = mutable,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(if (attached) "Continue workout" else "Review unfinished workout")
                }
                TextButton(onClick = { discardId = saved.id() }, enabled = mutable) {
                    Text("Discard unfinished workout")
                }
            }
        }
        when (section) {
            "Overview" -> {
                SemanticCard(Tile.Strength) {
                    Text("Train your way", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Follow a programme or choose a one-off workout. Spontaneous training stays separate from planned sessions."
                    )
                    Button(
                        onClick = { model.startFree(onWorkout) },
                        enabled = mayStart,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Start open workout")
                    }
                    Text(
                        "Choose exercises as you go. No prescribed sets or loads.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                EntryButton(
                    "Start planned workout",
                    "Use the exact retained programme session",
                    mutable,
                ) {
                    section = "Planned workouts"
                }
                EntryButton(
                    "Quick workout",
                    "A simple one-off session for the time and equipment you have",
                    mutable,
                ) {
                    section = "Quick workout"
                }
                EntryButton("Build a workout", "Choose exercises, sets and targets", mutable) {
                    model.newBuilder()
                    onBuild()
                }
                EntryButton(
                    "Generate one for me",
                    "Choose your focus and review a suggested workout",
                    mutable,
                ) {
                    section = "Generate one for me"
                }
                TextButton(
                    onClick = { section = "Favourite workouts" },
                    enabled = mutable,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Favourite workouts")
                }
                TextButton(
                    onClick = { section = "Repeat previous workout" },
                    enabled = mutable,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Repeat previous workout")
                }
            }
            "Planned workouts" -> {
                val programme = state.programme
                if (programme == null) {
                    Text(
                        "There is no active gym programme. You can create one from Strength, or choose a one-off workout here."
                    )
                    OutlinedButton(onClick = { section = "Quick workout" }, enabled = mutable) {
                        Text("Choose a quick workout")
                    }
                } else {
                    Text(
                        "Programme version ${programme.version()}. Starting a session preserves this exact prescription."
                    )
                    val today = LocalDate.now()
                    val sessions =
                        programme
                            .sessions()
                            .sortedBy { it.date() }
                            .let { rows ->
                                rows.filter { !it.date().isBefore(today) } +
                                    rows.filter { it.date().isBefore(today) }.asReversed()
                            }
                    Text(
                        "Today and upcoming sessions first. Earlier prescriptions remain available; there is no catch-up requirement."
                    )
                    sessions.take(plannedLimit).forEach { session ->
                        SemanticCard(Tile.Planned) {
                            Text(
                                session.date().format(GYM_DAY),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(session.title(), style = MaterialTheme.typography.titleLarge)
                            Text(
                                session.exercises().joinToString(" · ") { prescription ->
                                    state.exercises
                                        .firstOrNull { it.id() == prescription.exerciseId() }
                                        ?.name() ?: prescription.exerciseId()
                                }
                            )
                            Button(
                                onClick = { model.startPlanned(session.id(), onWorkout) },
                                enabled = mayStart,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) {
                                Text("Start this session")
                            }
                        }
                    }
                    if (plannedLimit < sessions.size)
                        TextButton(
                            onClick = { plannedLimit += 7 },
                            enabled = mutable,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Show more programme sessions")
                        }
                }
            }
            "Quick workout",
            "Generate one for me" ->
                OneOffWorkoutPicker(
                    state = state,
                    model = model,
                    quick = section == "Quick workout",
                    mayStart = mayStart,
                    enabled = mutable,
                    onWorkout = onWorkout,
                )
            "Favourite workouts" -> {
                Text("Start a saved prescription with empty actual sets, or edit a new revision.")
                if (state.favourites.isEmpty()) {
                    Text("No favourite workouts on this page yet.")
                    OutlinedButton(
                        onClick = {
                            model.newBuilder()
                            onBuild()
                        },
                        enabled = mutable,
                    ) {
                        Text("Build a favourite workout")
                    }
                }
                state.favourites.forEach { definition ->
                    SemanticCard(Tile.Progress) {
                        Text(definition.title(), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${definition.slots().size} exercises · revision ${definition.revision()}"
                        )
                        Text(definition.slots().joinToString(" · ") { it.exercise().name() })
                        Button(
                            onClick = { model.startFavourite(definition, onWorkout) },
                            enabled = mayStart,
                        ) {
                            Text("Start favourite")
                        }
                        TextButton(
                            onClick = {
                                model.editFavourite(definition)
                                onBuild()
                            },
                            enabled = mutable,
                        ) {
                            Text("Edit workout")
                        }
                        TextButton(
                            onClick = { model.removeFavourite(definition) },
                            enabled = mutable,
                        ) {
                            Text("Remove from favourites")
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.favouritePage > 0)
                        OutlinedButton(onClick = { model.favouritePage(-1) }, enabled = mutable) {
                            Text("Previous favourites")
                        }
                    if (state.hasMoreFavourites)
                        OutlinedButton(onClick = { model.favouritePage(1) }, enabled = mutable) {
                            Text("More favourites")
                        }
                }
            }
            "Repeat previous workout" -> {
                Text(
                    "Repeat the intended structure with empty actuals. Previous loads are not copied into a new log."
                )
                if (state.recent.isEmpty())
                    Text("No completed strength workouts yet. Start an open workout or build one.")
                state.recent.forEach { actual ->
                    SemanticCard(Tile.Strength) {
                        Text(
                            actual.startedAt().atZone(ZoneId.systemDefault()).format(GYM_DAY),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(actual.exercises().joinToString(" · ") { it.exercise().name() })
                        Text(
                            "${actual.exercises().sumOf { it.sets().size }} recorded sets in the original workout"
                        )
                        Button(
                            onClick = { model.repeatPrevious(actual.id(), onWorkout) },
                            enabled = mayStart,
                        ) {
                            Text("Repeat workout")
                        }
                    }
                }
            }
        }
    }
    discardId?.let { id ->
        AlertDialog(
            onDismissRequest = { discardId = null },
            title = { Text("Discard this unfinished workout?") },
            text = {
                Text(
                    "It will remain marked as discarded and will not count as completed training or PB evidence."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.discardUnfinished(id)
                        discardId = null
                    },
                    enabled = mutable,
                ) {
                    Text("Discard workout")
                }
            },
            dismissButton = { TextButton(onClick = { discardId = null }) { Text("Keep workout") } },
        )
    }
    if (confirmReload)
        AlertDialog(
            onDismissRequest = { confirmReload = false },
            title = { Text("Reload saved evidence?") },
            text = {
                Text(
                    "This leaves the unacknowledged action and loads what is actually stored. Your form values remain available to review. A saved interrupted workout requires explicit recovery."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.reloadSavedEvidence()
                        confirmReload = false
                    },
                    enabled = !state.busy,
                ) {
                    Text("Reload saved evidence")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReload = false }) { Text("Keep pending action") }
            },
        )
}

@Composable
private fun EntryButton(title: String, detail: String, enabled: Boolean, action: () -> Unit) {
    OutlinedButton(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OneOffWorkoutPicker(
    state: GymNowState,
    model: GymNowViewModel,
    quick: Boolean,
    mayStart: Boolean,
    enabled: Boolean,
    onWorkout: () -> Unit,
) {
    var minutes by rememberSaveable(quick) { mutableStateOf(30) }
    var equipment by rememberSaveable(quick) { mutableStateOf(listOf("bodyweight")) }
    var focus by rememberSaveable(quick) { mutableStateOf(GymNowWorkouts.Focus.FULL_BODY.name) }
    var goal by rememberSaveable(quick) { mutableStateOf(GymProgramme.Goal.GENERAL.name) }
    var experience by
        rememberSaveable(quick) { mutableStateOf(GymProgramme.Experience.BEGINNER.name) }
    var priority by
        rememberSaveable(quick) { mutableStateOf(GymProgramme.Priority.STRENGTH_FIRST.name) }
    var effort by rememberSaveable(quick) { mutableStateOf(GymProgramme.Effort.RPE.name) }
    var showPreview by rememberSaveable(quick) { mutableStateOf(false) }
    var error by rememberSaveable(quick) { mutableStateOf<String?>(null) }

    Text(
        "A one-off prescription for today. It will not change your programme or complete a planned session."
    )
    Text("Available time", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(30, 45, 60, 75).forEach { value ->
            FilterChip(
                selected = minutes == value,
                onClick = {
                    minutes = value
                    showPreview = false
                },
                enabled = enabled,
                label = { Text("$value min") },
            )
        }
    }
    Text("Equipment available", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GYM_EQUIPMENT.forEach { value ->
            FilterChip(
                selected = value in equipment,
                onClick = {
                    equipment = if (value in equipment) equipment - value else equipment + value
                    showPreview = false
                },
                enabled = enabled,
                label = { Text(value.replaceFirstChar { it.titlecase(Locale.UK) }) },
            )
        }
    }
    Text("Focus", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GymNowWorkouts.Focus.values().forEach { value ->
            FilterChip(
                selected = focus == value.name,
                onClick = {
                    focus = value.name
                    showPreview = false
                },
                enabled = enabled,
                label = { Text(value.title().removeSuffix(" workout")) },
            )
        }
    }
    if (!quick) {
        Text("Goal", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GymProgramme.Goal.values().forEach { value ->
                FilterChip(
                    selected = goal == value.name,
                    onClick = {
                        goal = value.name
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(gymGoalLabel(value)) },
                )
            }
        }
        Text("Experience", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GymProgramme.Experience.values().forEach { value ->
                FilterChip(
                    selected = experience == value.name,
                    onClick = {
                        experience = value.name
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(gymEnumLabel(value.name)) },
                )
            }
        }
        Text("Training priority", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GymProgramme.Priority.values().forEach { value ->
                FilterChip(
                    selected = priority == value.name,
                    onClick = {
                        priority = value.name
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(gymEnumLabel(value.name)) },
                )
            }
        }
        Text("Effort guidance", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GymProgramme.Effort.values().forEach { value ->
                FilterChip(
                    selected = effort == value.name,
                    onClick = {
                        effort = value.name
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(value.name) },
                )
            }
        }
    } else
        Text(
            "Quick workouts use general fitness, beginner experience and conservative RPE guidance. Use Generate one for me to choose a different goal or experience."
        )
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Button(
        onClick = {
            try {
                val request =
                    GymNowWorkouts.Request(
                        minutes,
                        equipment.toSet(),
                        GymNowWorkouts.Focus.valueOf(focus),
                        GymProgramme.Goal.valueOf(goal),
                        GymProgramme.Experience.valueOf(experience),
                        GymProgramme.Priority.valueOf(priority),
                        GymProgramme.Effort.valueOf(effort),
                    )
                error = null
                showPreview = true
                model.generate(request)
            } catch (invalid: IllegalArgumentException) {
                error = invalid.message ?: "Review the workout choices."
                showPreview = false
            }
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("Review suggested workout")
    }
    if (showPreview && !state.busy)
        state.generated?.let { suggestion ->
            SemanticCard(Tile.Strength) {
                Text(suggestion.definition().title(), style = MaterialTheme.typography.titleLarge)
                Text(suggestion.explanation())
                suggestion.definition().slots().forEach { slot ->
                    Text(slot.exercise().name(), style = MaterialTheme.typography.titleMedium)
                    Text(gymTargetDescription(slot))
                }
                suggestion.limitations().forEach { limitation -> Text(limitation) }
                Text(
                    "These are targets, not completed sets. You choose and record your actual loads."
                )
                Button(
                    onClick = { model.startGenerated(quick, onWorkout) },
                    enabled = mayStart,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(if (quick) "Start quick workout" else "Start generated workout")
                }
            }
        }
}

private val GYM_DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
private val GYM_EQUIPMENT =
    listOf(
        "bodyweight",
        "dumbbells",
        "barbell",
        "squat rack",
        "bench",
        "machines",
        "cables",
        "kettlebells",
        "pull-up bar",
        "trap bar",
        "sled",
        "full gym",
    )

private fun gymEnumLabel(value: String) =
    value.lowercase(Locale.UK).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.UK) }

private fun gymGoalLabel(goal: GymProgramme.Goal) =
    when (goal) {
        GymProgramme.Goal.SUPPORT_RUNNING -> "Support running"
        GymProgramme.Goal.STRENGTH -> "Get stronger"
        GymProgramme.Goal.MUSCLE -> "Build muscle"
        GymProgramme.Goal.GENERAL -> "General fitness"
        GymProgramme.Goal.POWER -> "Power preparation"
    }
