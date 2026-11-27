@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import org.racedaydreams.coach.ActivityAction
import org.racedaydreams.coach.ActivityActionRole
import org.racedaydreams.coach.ActivityHeading
import org.racedaydreams.coach.CoachFilterChip
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.Glyph
import org.racedaydreams.coach.SectionTitle
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.coach.WhyDetails
import org.racedaydreams.coach.activityNumberStyle
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** The screen displays acknowledged evidence; the shared coordinator owns all workout timing. */
@Composable
fun SharedGymWorkoutScreen(
    model: SharedGymNowCoordinator,
    state: GymNowState,
    onBack: () -> Unit,
    formatElapsed: (Long) -> String,
    formatActual: (StrengthSession.SetEntry, StrengthExercise) -> String,
    formatExercise: (StrengthExercise) -> String,
    formatTarget: (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot) -> String,
    formatAssociationDate: (org.racedaydreams.domain.time.LocalDate) -> String,
    restControls:
        @Composable
        (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot, Boolean) -> Unit,
    warmupGuidance:
        @Composable
        (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot, Boolean) -> Unit,
) {
    var chooseExercise by rememberSaveable { mutableStateOf(false) }
    var discardConfirmation by rememberSaveable { mutableStateOf(false) }
    val execution = state.view?.observed()
    val pending = state.view?.pending() != null || state.association?.pending() != null
    val enabled = !state.loading && !state.busy && !pending
    val active = execution?.status() == GymExecution.Status.ACTIVE
    val terminal =
        execution?.status() == GymExecution.Status.FINISHED ||
            execution?.status() == GymExecution.Status.DISCARDED
    val slot = execution?.slots()?.firstOrNull { it.id() == execution.selectedSlotId() }
    var effortDetails by rememberSaveable(execution?.id(), slot?.id()) { mutableStateOf(false) }
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
        val workoutTitle =
            when (execution?.status()) {
                GymExecution.Status.FINISHED -> "Workout saved"
                GymExecution.Status.DISCARDED -> "Workout discarded"
                else -> execution?.prescription()?.title() ?: "Gym workout"
            }
        if (terminal) ActivityHeading(workoutTitle, Glyph.DUMBBELL) else CoachHeading(workoutTitle)
        if (state.loading || state.busy) CircularProgressIndicator()
        state.error?.let { SemanticCard(Tile.Warning) { Text(it) } }
        state.notice?.let { SemanticCard(Tile.Insight) { Text(it) } }
        if (pending) SharedGymPendingWriteCard(model, enabled = !state.busy)

        if (execution == null) {
            if (!pending) {
                SemanticCard(Tile.Insight) {
                    Text("Choose a workout or recover saved evidence in Gym Now.")
                    Text(
                        "After an interruption, review the saved workout before restarting its timer."
                    )
                }
            }
        } else if (terminal) {
            Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                Text(
                    execution.prescription()?.title() ?: "Your workout",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (execution.status() == GymExecution.Status.FINISHED) {
                    val working = execution.actualSets().count { !it.actual().warmUp() }
                    val label = if (working == 1) "set" else "sets"
                    Text("$working working $label recorded")
                    Text("Active time ${formatElapsed(execution.activeMillis())}")
                    if (execution.partial())
                        Text(
                            "Finished early · actual sets and the original prescription are preserved."
                        )
                    if (execution.origin() != GymExecution.Origin.PLANNED)
                        Text(
                            if (
                                state.association?.current()?.choice() ==
                                    org.racedaydreams.domain.strength.GymWorkoutAssociation.Choice
                                        .COUNT_TOWARD_SESSION
                            )
                                "Associated with your explicitly reviewed programme session. Actual performance is retained separately."
                            else
                                "Saved as an extra workout. It has not completed a programme session."
                        )
                } else Text("This stage does not count as completed training or PB evidence.")
            }
            if (execution.status() == GymExecution.Status.FINISHED) {
                if (execution.origin() != GymExecution.Origin.PLANNED)
                    SharedGymAssociationCard(
                        formatDate = formatAssociationDate,
                        formatTarget = formatTarget,
                        view = state.association,
                        enabled = enabled,
                        onChoose = model::reviewAssociation,
                        onRefresh = model::refreshAssociation,
                    )
                Text(
                    "Execution as recorded · later actual edits remain in workout history.",
                    style = MaterialTheme.typography.bodySmall,
                )
                execution
                    .actualSets()
                    .groupBy { it.exercise() }
                    .forEach { (exercise, rows) ->
                        Text(exercise.name(), style = MaterialTheme.typography.titleMedium)
                        StrengthSetTable(
                            exercise,
                            state.massUnit,
                            rows.map { it.actual() },
                            StrengthRowState.SAVED,
                        )
                    }
            }
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text("Done")
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Tiny)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                    Text(
                        gymStatusLabel(execution.status()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val activeTime = formatElapsed(execution.activeMillis())
                    Text(
                        activeTime,
                        style = activityNumberStyle(),
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Active workout time $activeTime"
                            },
                    )
                }
                Text("Recorded elapsed ${formatElapsed(execution.recordedElapsedMillis())}")
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
                val working = currentSets.count { !it.actual().warmUp() }
                val target = slot.target()
                val progress =
                    if (target == null)
                        "$working working ${if (working == 1) "set" else "sets"} saved"
                    else "$working of ${target.sets()} target working sets saved"
                StrengthExerciseHeader(
                    slot.exercise().name(),
                    previous?.lastOrNull()?.let { formatActual(it, slot.exercise()) },
                    progress =
                        "Exercise ${execution.slots().indexOf(slot) + 1} of ${execution.slots().size} · $progress",
                ) {
                    if (active)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                            ActivityAction(
                                "Equipment busy?",
                                onClick = {
                                    model.openSubstitutions(
                                        ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY
                                    )
                                },
                                enabled = enabled,
                                role = ActivityActionRole.SECONDARY,
                            )
                            ActivityAction(
                                "Swap exercise",
                                onClick = {
                                    model.openSubstitutions(
                                        ExerciseSubstitutionEngine.Reason.ALTERNATIVES
                                    )
                                },
                                enabled = enabled,
                                role = ActivityActionRole.TERTIARY,
                            )
                        }
                }

                if (active) {
                    if (slot.supersetGroup() != null)
                        SemanticCard(Tile.Strength) {
                            Text("Superset", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Work through these exercises in order, then repeat the round. You can rest or skip a movement when needed."
                            )
                            val next =
                                org.racedaydreams.domain.strength.GymSupersets.next(execution)
                            Text(
                                execution
                                    .slots()
                                    .filter { it.supersetGroup() == slot.supersetGroup() }
                                    .mapIndexed { index, movement ->
                                        "${index+1}. ${movement.exercise().name()}"
                                    }
                                    .joinToString("\n")
                            )
                            Text(
                                next?.instruction()
                                    ?: "No further movement is suggested in this group. Skips are not recorded as completed sets."
                            )
                            Text(
                                "Keep each exercise's actual sets separate. Rest targets are optional; you may rest between movements or rounds."
                            )
                            if (next != null && next.slotId() != slot.id())
                                Button(onClick = model::nextSuperset, enabled = enabled) {
                                    Text("Continue to next movement · round ${next.round()}")
                                }
                            else if (next != null)
                                Text(
                                    "Record this movement's actual set below. Then we'll suggest the next movement."
                                )
                            TextButton(onClick = model::skipMovement, enabled = enabled) {
                                Text("Skip this movement today")
                            }
                            TextButton(onClick = model::breakSuperset, enabled = enabled) {
                                Text("Use ordinary exercises today")
                            }
                        }
                }

                val timerVisible =
                    state.restTimer?.phase() in
                        setOf(GymRestTimer.Phase.RUNNING, GymRestTimer.Phase.PAUSED)
                if (active && timerVisible) restControls(slot, enabled)

                if (currentSets.isNotEmpty()) {
                    Text(
                        "Saved sets",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    StrengthSetTable(
                        slot.exercise(),
                        state.massUnit,
                        currentSets.map { it.actual() },
                        StrengthRowState.SAVED,
                        previous = previous.orEmpty(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                    SectionTitle("Current set", "Set ${currentSets.size + 1}")
                    val profile = slot.exercise().measurement()
                    val hasLoad =
                        profile.usesLoad() &&
                            slot.exercise().mode() != StrengthExercise.Mode.BODYWEIGHT
                    StrengthSetFields(
                        load =
                            if (hasLoad)
                                { modifier ->
                                    GymActualField(
                                        state.form.kilograms,
                                        { value ->
                                            model.updateForm { it.copy(kilograms = value) }
                                        },
                                        "Actual load · ${org.racedaydreams.domain.presentation.MassText.symbol(state.massUnit)}",
                                        KeyboardType.Decimal,
                                        enabled && active,
                                        modifier,
                                    )
                                }
                            else null,
                        amount = { modifier ->
                            Column(
                                modifier,
                                verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
                            ) {
                                if (profile.usesReps())
                                    GymActualField(
                                        state.form.repetitions,
                                        { value ->
                                            model.updateForm { it.copy(repetitions = value) }
                                        },
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
                            }
                        },
                    )
                    val detailsLabel =
                        if (state.form.warmUp) "Warm-up set · effort options"
                        else "Warm-up and effort (optional)"
                    StrengthDetailsToggle(detailsLabel, effortDetails) {
                        effortDetails = !effortDetails
                    }
                    if (effortDetails) {
                        if (profile.usesReps()) {
                            FlowRow(
                                Modifier.selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                            ) {
                                listOf(false, true).forEach { useRir ->
                                    val selected = state.form.rir == useRir
                                    val scale = if (useRir) "RIR" else "RPE"
                                    CoachFilterChip(
                                        selected = selected,
                                        onClick = {
                                            if (!selected)
                                                model.updateForm { it.copy(rir = useRir) }
                                        },
                                        label = { Text(if (selected) scale else "Use $scale") },
                                        enabled = enabled && active,
                                    )
                                }
                            }
                        }
                        Text(
                            if (state.form.rir && profile.usesReps())
                                "RIR counts how many more repetitions you could have completed. Leave it blank if you do not know."
                            else
                                "RPE is how hard the set felt, from 1 to 10. Leave it blank if you do not know."
                        )
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

                if (active && !timerVisible) restControls(slot, enabled)
                Text(formatExercise(slot.exercise()), style = MaterialTheme.typography.bodySmall)
                SharedPlainWorkoutTarget(slot, formatTarget)
                if (previous != null && previous.size > 1) {
                    WhyDetails("Previous working sets") {
                        StrengthSetTable(
                            slot.exercise(),
                            state.massUnit,
                            previous.take(6),
                            StrengthRowState.PREVIOUS,
                        )
                        if (previous.size > 6)
                            Text("${previous.size - 6} more working sets in that workout")
                    }
                }
                if (active) warmupGuidance(slot, enabled)

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
                                StrengthSetTable(
                                    exercise,
                                    state.massUnit,
                                    sets.map { it.actual() },
                                    StrengthRowState.SAVED,
                                )
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

            if (active || execution.slots().size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                    if (execution.slots().size > 1)
                        for (exerciseSlot in execution.slots()) {
                            val current = exerciseSlot.id() == execution.selectedSlotId()
                            CoachFilterChip(
                                selected = current,
                                enabled = enabled && active && !current,
                                onClick = { model.select(exerciseSlot.id()) },
                                label = {
                                    Text(
                                        (if (current) "Current · " else "") +
                                            exerciseSlot.exercise().name()
                                    )
                                },
                            )
                        }
                    if (active)
                        ActivityAction(
                            "Add exercise",
                            onClick = { chooseExercise = true },
                            enabled = enabled && execution.slots().size < 100,
                            role = ActivityActionRole.SECONDARY,
                        )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                ActivityAction(
                    if (active) "Pause workout" else "Resume workout",
                    onClick = { if (active) model.pause() else model.resume() },
                    enabled = enabled,
                    role = if (active) ActivityActionRole.TERTIARY else ActivityActionRole.PRIMARY,
                )
                ActivityAction(
                    "Finish and save workout",
                    onClick = { model.finish() },
                    enabled = enabled && execution.actualSets().isNotEmpty(),
                    role = ActivityActionRole.SECONDARY,
                )
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

    state.substitution?.let { swap ->
        SharedGymSubstitutionSheet(
            swap = swap,
            busy = state.busy,
            error = state.error,
            enabled = !state.busy && state.view?.pending() == null,
            onClose = model::closeSubstitutions,
            onReasonChange = model::changeSubstitutionReason,
            onEquipmentChange = model::changeSubstitutionEquipment,
            onShowMore = model::showMoreSubstitutions,
            onReplace = model::replaceForToday,
            exerciseContext = formatExercise,
        )
    }

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
fun SharedGymPendingWriteCard(model: SharedGymNowCoordinator, enabled: Boolean) {
    var confirmReload by rememberSaveable { mutableStateOf(false) }
    SemanticCard(Tile.Warning) {
        Text("Saving not yet confirmed", style = MaterialTheme.typography.titleMedium)
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
                    "Use this if an exact retry keeps reporting a conflict. Values whose save has not been confirmed stay in the form; they are not presented as saved. An unfinished workout will require an explicit resume; completed association choices are reloaded without restarting execution."
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
            state.excludedExerciseIds,
        ) {
            ExerciseSearch.search(
                    state.exercises,
                    query,
                    state.favouriteExerciseIds,
                    recent,
                    state.archivedExerciseIds,
                )
                .filter { query.isNotBlank() || it.id() !in state.excludedExerciseIds }
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
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    StrengthEntryField(label, value, change, keyboard, modifier, enabled)
}

private fun gymOriginLabel(origin: GymExecution.Origin): String =
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
