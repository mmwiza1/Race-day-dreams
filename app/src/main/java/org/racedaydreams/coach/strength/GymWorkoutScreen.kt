package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** The screen displays acknowledged evidence; the shared coordinator owns all workout timing. */
@Composable
fun GymWorkoutScreen(model: GymNowViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) model.setWorkoutVisible(true)
            if (event == Lifecycle.Event.ON_STOP) model.setWorkoutVisible(false)
        }
        owner.lifecycle.addObserver(observer)
        model.setWorkoutVisible(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            owner.lifecycle.removeObserver(observer)
            model.setWorkoutVisible(false)
        }
    }
    var chooseExercise by rememberSaveable { mutableStateOf(false) }
    var discardConfirmation by rememberSaveable { mutableStateOf(false) }
    val execution = state.view?.observed()
    val pending = state.view?.pending() != null
    val enabled = !state.loading && !state.busy && !pending
    val active = execution?.status() == GymExecution.Status.ACTIVE
    val terminal =
        execution?.status() == GymExecution.Status.FINISHED ||
            execution?.status() == GymExecution.Status.DISCARDED
    val slot = execution?.slots()?.firstOrNull { it.id() == execution.selectedSlotId() }
    val currentSets =
        remember(execution?.actualSets(), slot) {
            execution?.actualSets()?.filter {
                slot != null && it.slotId() == slot.id() && it.exercise() == slot.exercise()
            } ?: emptyList()
        }
    val beforeSwap =
        remember(execution?.actualSets(), slot) {
            execution?.actualSets()?.filter {
                slot != null && it.slotId() == slot.id() && it.exercise() != slot.exercise()
            } ?: emptyList()
        }

    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (terminal) "Back to Strength" else "Back to Gym Now")
        }
        CoachHeading(
            when (execution?.status()) {
                GymExecution.Status.FINISHED -> "Workout saved"
                GymExecution.Status.DISCARDED -> "Workout discarded"
                else -> execution?.prescription()?.title() ?: "Gym workout"
            }
        )
        if (state.loading || state.busy) CircularProgressIndicator()
        state.error?.let { SemanticCard(Tile.Warning) { Text(it) } }
        state.notice?.let { SemanticCard(Tile.Insight) { Text(it) } }
        if (pending) GymPendingWriteCard(model, enabled = !state.busy)

        if (execution == null) {
            if (!pending) {
                SemanticCard(Tile.Insight) {
                    Text("Choose a workout or recover saved evidence in Gym Now.")
                    Text("A new app process never restarts a workout clock automatically.")
                }
            }
        } else if (terminal) {
            SemanticCard(
                if (execution.status() == GymExecution.Status.FINISHED) Tile.Progress
                else Tile.Neutral
            ) {
                Text(
                    execution.prescription()?.title() ?: "Your workout",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (execution.status() == GymExecution.Status.FINISHED) {
                    Text(
                        "${execution.actualSets().count { !it.actual().warmUp() }} working sets recorded"
                    )
                    Text("Active time ${gymElapsed(execution.activeMillis())}")
                    if (execution.partial())
                        Text(
                            "Finished early · actual sets and the original prescription are preserved."
                        )
                    if (execution.origin() != GymExecution.Origin.PLANNED)
                        Text("Saved as an extra workout. It has not completed a programme session.")
                } else Text("This stage does not count as completed training or PB evidence.")
            }
            if (execution.status() == GymExecution.Status.FINISHED) {
                execution
                    .actualSets()
                    .groupBy { it.exercise() }
                    .forEach { (exercise, rows) ->
                        SemanticCard(Tile.Strength) {
                            Text(exercise.name(), style = MaterialTheme.typography.titleMedium)
                            rows.forEach { Text(gymActualDescription(it.actual(), exercise)) }
                        }
                    }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Done")
            }
        } else {
            SemanticCard(Tile.Strength) {
                Text(
                    gymStatusLabel(execution.status()),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    gymElapsed(execution.activeMillis()),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                "Active workout time ${gymElapsed(execution.activeMillis())}"
                        },
                )
                Text("Recorded elapsed ${gymElapsed(execution.recordedElapsedMillis())}")
                if (execution.status() == GymExecution.Status.RECOVERY_REQUIRED) {
                    Text(
                        "Saved sets are intact. The interruption is retained as a gap; unobserved time has not been added to active training."
                    )
                }
                if (execution.origin() == GymExecution.Origin.PLANNED)
                    Text(
                        "Planned workout · preserved programme version ${execution.source()?.revision()}"
                    )
                else Text("${gymOriginLabel(execution.origin())} · separate from your programme")
            }

            if (slot != null) {
                SemanticCard(Tile.Planned) {
                    Text(slot.exercise().name(), style = MaterialTheme.typography.headlineSmall)
                    Text(gymExerciseContext(slot.exercise()))
                    Text(gymTargetDescription(slot))
                    val working = currentSets.count { !it.actual().warmUp() }
                    Text(
                        if (slot.target() == null) "$working working sets saved"
                        else "$working of ${slot.target().sets()} target working sets saved"
                    )
                    Text(
                        "Targets and references are guidance. Record the load and repetitions, time or distance you actually perform.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (active) {
                        OutlinedButton(
                            onClick = {
                                model.openSubstitutions(
                                    ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY
                                )
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Equipment busy?")
                        }
                        TextButton(
                            onClick = {
                                model.openSubstitutions(
                                    ExerciseSubstitutionEngine.Reason.ALTERNATIVES
                                )
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Swap exercise")
                        }
                    }
                }

                val previous =
                    remember(state.recent, slot.exercise(), execution.id()) {
                        state.recent
                            .asSequence()
                            .filter { it.id() != execution.id() }
                            .mapNotNull { session ->
                                val sets =
                                    session
                                        .exercises()
                                        .filter { it.exercise() == slot.exercise() }
                                        .flatMap { it.sets() }
                                        .filterNot { it.warmUp() }
                                if (sets.isEmpty()) null else sets
                            }
                            .firstOrNull()
                    }
                if (previous != null) {
                    SemanticCard(Tile.Neutral) {
                        Text(
                            "Previous comparable performance",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        previous.take(6).forEach { Text(gymActualDescription(it, slot.exercise())) }
                        if (previous.size > 6)
                            Text("${previous.size - 6} more working sets in that workout")
                        Text(
                            "Exact exercise and measurement. These values are not entered as your current actual sets.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                SemanticCard(Tile.Strength) {
                    Text("Current set", style = MaterialTheme.typography.titleLarge)
                    val profile = slot.exercise().measurement()
                    if (
                        profile.usesLoad() &&
                            slot.exercise().mode() != StrengthExercise.Mode.BODYWEIGHT
                    )
                        GymActualField(
                            state.form.kilograms,
                            { value -> model.updateForm { it.copy(kilograms = value) } },
                            "Actual load · kg",
                            KeyboardType.Decimal,
                            enabled && active,
                        )
                    if (profile.usesReps())
                        GymActualField(
                            state.form.repetitions,
                            { value -> model.updateForm { it.copy(repetitions = value) } },
                            "Actual repetitions",
                            KeyboardType.Number,
                            enabled && active,
                        )
                    if (profile.usesDuration())
                        GymActualField(
                            state.form.seconds,
                            { value -> model.updateForm { it.copy(seconds = value) } },
                            "Actual duration · seconds",
                            KeyboardType.Number,
                            enabled && active,
                        )
                    if (profile.usesDistance())
                        GymActualField(
                            state.form.metres,
                            { value -> model.updateForm { it.copy(metres = value) } },
                            "Actual distance · metres",
                            KeyboardType.Decimal,
                            enabled && active,
                        )
                    if (profile.usesReps()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { model.updateForm { it.copy(rir = false) } },
                                enabled = enabled && active && state.form.rir,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Text(if (state.form.rir) "Use RPE" else "RPE selected")
                            }
                            OutlinedButton(
                                onClick = { model.updateForm { it.copy(rir = true) } },
                                enabled = enabled && active && !state.form.rir,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) {
                                Text(if (state.form.rir) "RIR selected" else "Use RIR")
                            }
                        }
                    }
                    GymActualField(
                        state.form.effort,
                        { value -> model.updateForm { it.copy(effort = value) } },
                        if (state.form.rir && profile.usesReps()) "RIR · optional"
                        else "RPE · optional",
                        KeyboardType.Decimal,
                        enabled && active,
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = state.form.warmUp,
                            onCheckedChange = { value ->
                                model.updateForm { it.copy(warmUp = value) }
                            },
                            enabled = enabled && active,
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Mark this set as a warm-up"
                                },
                        )
                        Column(Modifier.weight(1f).padding(top = 12.dp)) {
                            Text("Warm-up set")
                            Text(
                                "Excluded from working-set totals and PBs.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Button(
                        onClick = model::saveSet,
                        enabled = enabled && active,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Save actual set")
                    }
                    if (!active) Text("Resume the workout before recording another set.")
                }

                if (currentSets.isNotEmpty()) {
                    SemanticCard(Tile.Progress) {
                        Text(
                            "Saved sets",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        currentSets.forEachIndexed { index, set ->
                            Text(
                                "${index + 1}. ${gymActualDescription(set.actual(), set.exercise())}"
                            )
                        }
                    }
                }
                if (beforeSwap.isNotEmpty()) {
                    SemanticCard(Tile.Neutral) {
                        Text("Before this swap", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "These saved sets retain their original exercise. They do not count toward the replacement's target."
                        )
                        beforeSwap
                            .groupBy { it.exercise() }
                            .forEach { (exercise, sets) ->
                                Text(exercise.name(), style = MaterialTheme.typography.titleSmall)
                                sets.forEach { Text(gymActualDescription(it.actual(), exercise)) }
                            }
                    }
                }
            } else {
                SemanticCard(Tile.Insight) {
                    Text("Choose your first exercise", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Build your workout as you go. No exercise or set is logged until you enter it."
                    )
                }
            }

            if (active) {
                OutlinedButton(
                    onClick = { chooseExercise = true },
                    enabled = enabled && execution.slots().size < 100,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Add exercise")
                }
            }
            if (execution.slots().size > 1) {
                Text("Exercises in this workout", style = MaterialTheme.typography.titleMedium)
                execution.slots().forEach { exerciseSlot ->
                    OutlinedButton(
                        onClick = { model.select(exerciseSlot.id()) },
                        enabled =
                            enabled && active && exerciseSlot.id() != execution.selectedSlotId(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            (if (exerciseSlot.id() == execution.selectedSlotId()) "Current · "
                            else "") + exerciseSlot.exercise().name()
                        )
                    }
                }
            }
            if (active) {
                OutlinedButton(
                    onClick = model::pause,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Pause workout")
                }
            } else {
                Button(
                    onClick = model::resume,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Text("Resume workout")
                }
            }
            Button(
                onClick = { model.finish() },
                enabled = enabled && execution.actualSets().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text("Finish and save workout")
            }
            TextButton(
                onClick = { discardConfirmation = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("Discard unfinished workout")
            }
        }
    }

    if (state.substitution != null) GymSubstitutionSheet(model)

    if (state.finishEarlyRequired) {
        AlertDialog(
            onDismissRequest = model::clearFinishPrompt,
            title = { Text("Finish this workout early?") },
            text = {
                Text(
                    "Some prescribed working sets are not recorded. Save only what you performed, with the workout marked as partially completed. Your original prescription is retained."
                )
            },
            confirmButton = {
                TextButton(onClick = { model.finish(true) }, enabled = enabled) {
                    Text("Finish early and save")
                }
            },
            dismissButton = {
                TextButton(onClick = model::clearFinishPrompt) { Text("Keep training") }
            },
        )
    }
    if (discardConfirmation) {
        AlertDialog(
            onDismissRequest = { discardConfirmation = false },
            title = { Text("Discard this unfinished workout?") },
            text = {
                Text(
                    "It will not count as completed training or PB evidence. Your other workouts and programme remain unchanged."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        discardConfirmation = false
                        model.discard()
                    },
                    enabled = enabled,
                ) {
                    Text("Discard workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { discardConfirmation = false }) { Text("Keep workout") }
            },
        )
    }
    if (chooseExercise) {
        GymExercisePicker(
            state = state,
            onDismiss = { chooseExercise = false },
            onChoose = {
                model.addExercise(it)
                chooseExercise = false
            },
        )
    }
}

@Composable
internal fun GymPendingWriteCard(model: GymNowViewModel, enabled: Boolean) {
    var confirmReload by rememberSaveable { mutableStateOf(false) }
    SemanticCard(Tile.Warning) {
        Text("Save acknowledgement pending", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your entered values are retained. Retry the same save before another action; it may already be safely stored."
        )
        Button(
            onClick = model::retry,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Retry save")
        }
        TextButton(
            onClick = { confirmReload = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Reload saved evidence")
        }
    }
    if (confirmReload)
        AlertDialog(
            onDismissRequest = { confirmReload = false },
            title = { Text("Reload the saved workout?") },
            text = {
                Text(
                    "Use this if an exact retry keeps reporting a conflict. Unacknowledged values stay in the form, but are not claimed as saved. The retained workout will require an explicit resume."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReload = false
                        model.reloadSavedEvidence()
                    },
                    enabled = enabled,
                ) {
                    Text("Reload evidence")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReload = false }) { Text("Keep pending save") }
            },
        )
}

@Composable
private fun GymExercisePicker(
    state: GymNowState,
    onDismiss: () -> Unit,
    onChoose: (StrengthExercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val recent =
        remember(state.recent) {
            state.recent.flatMap { it.exercises().map { row -> row.exercise().id() } }.distinct()
        }
    val candidates =
        remember(
            state.exercises,
            query,
            recent,
            state.favouriteExerciseIds,
            state.archivedExerciseIds,
        ) {
            ExerciseSearch.search(
                    state.exercises,
                    query,
                    state.favouriteExerciseIds,
                    recent,
                    state.archivedExerciseIds,
                )
                .take(8)
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an exercise") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    query,
                    { query = it },
                    label = { Text("Exercise name or alias") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Recent and favourite exercises appear first. Search to explore all.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (candidates.isEmpty()) Text("No matching exercises.")
                candidates.forEach { exercise ->
                    TextButton(
                        onClick = { onChoose(exercise) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(exercise.name())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GymActualField(
    value: String,
    change: (String) -> Unit,
    label: String,
    keyboard: KeyboardType,
    enabled: Boolean,
) {
    OutlinedTextField(
        value,
        change,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun gymActualDescription(
    actual: StrengthSession.SetEntry,
    exercise: StrengthExercise,
): String {
    val load =
        if (
            exercise.measurement().usesLoad() && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
        )
            "${gymDecimal(actual.kilograms())} kg × "
        else ""
    val measurement =
        if (exercise.measurement().usesReps()) "${actual.reps()} reps"
        else
            actual.durationSeconds()?.let { "${it}s" }
                ?: "${gymDecimal(actual.distanceMetres()!!)} m"
    val effort =
        actual.rpe()?.let { " · RPE ${gymDecimal(it)}" }
            ?: actual.repetitionsInReserve()?.let { " · RIR ${gymDecimal(it)}" }
            ?: ""
    return "$load$measurement$effort" + if (actual.warmUp()) " · warm-up" else ""
}

internal fun gymElapsed(millis: Long): String {
    val total = millis / 1000
    return String.format(Locale.UK, "%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60)
}

private fun gymDecimal(value: Double) =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

internal fun gymOriginLabel(origin: GymExecution.Origin): String =
    when (origin) {
        GymExecution.Origin.PLANNED -> "Planned workout"
        GymExecution.Origin.QUICK -> "Quick workout"
        GymExecution.Origin.CUSTOM -> "Custom workout"
        GymExecution.Origin.REPEATED -> "Repeated workout"
        GymExecution.Origin.FAVOURITE -> "Favourite workout"
        GymExecution.Origin.GENERATED -> "Generated one-off workout"
        GymExecution.Origin.FREE -> "Spontaneous workout"
    }

private fun gymStatusLabel(status: GymExecution.Status): String =
    when (status) {
        GymExecution.Status.ACTIVE -> "Workout in progress"
        GymExecution.Status.PAUSED -> "Workout paused"
        GymExecution.Status.RECOVERY_REQUIRED -> "Saved workout recovered · resume when ready"
        GymExecution.Status.FINISHED -> "Workout saved"
        GymExecution.Status.DISCARDED -> "Workout discarded"
    }
