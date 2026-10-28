package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise

/** One reusable editor for frozen workout intentions. Set logging remains a separate flow. */
@Composable
fun GymWorkoutBuilderScreen(model: GymNowViewModel, onWorkout: () -> Unit, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val draft = state.builder
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingSlotId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.loading && !state.busy && state.view?.pending() == null

    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Back to Gym Now")
        }
        CoachHeading("Build a workout")
        Text("Choose the exercises and targets you want. Actual sets are entered when you train.")
        if (state.loading) CircularProgressIndicator()
        state.error?.let { error -> SemanticCard(Tile.Warning) { Text(error) } }
        state.notice?.let { notice -> SemanticCard(Tile.Insight) { Text(notice) } }
        OutlinedTextField(
            value = draft.title,
            onValueChange = model::builderTitle,
            label = { Text("Workout name") },
            enabled = enabled,
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.slots.isEmpty()) {
            SemanticCard(Tile.Strength) {
                Text("Start with your first exercise", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Add an exercise, then choose working sets, a repetition range, duration or distance. You can also leave an exercise open."
                )
            }
        } else {
            val prescribedSets = draft.slots.mapNotNull { it.target()?.sets() }.sum()
            val openCount = draft.slots.count { it.target() == null }
            Text(
                "${draft.slots.size} exercises · $prescribedSets prescribed sets" +
                    if (openCount > 0) " · $openCount open" else "",
                style = MaterialTheme.typography.titleMedium,
            )
            draft.slots.forEachIndexed { index, slot ->
                key(slot.id()) {
                    SemanticCard(Tile.Strength) {
                        Text(
                            "${index + 1}. ${slot.exercise().name()}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            gymExerciseContext(slot.exercise()),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(gymTargetDescription(slot))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    model.clearMessage()
                                    editingSlotId = slot.id()
                                    editorOpen = true
                                },
                                enabled = enabled,
                                modifier =
                                    Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Edit ${slot.exercise().name()}"
                                    },
                            ) {
                                Text("Edit")
                            }
                            OutlinedButton(
                                onClick = { model.builderDuplicate(slot.id()) },
                                enabled = enabled && draft.slots.size < 100,
                                modifier =
                                    Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Duplicate ${slot.exercise().name()}"
                                    },
                            ) {
                                Text("Duplicate")
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(
                                onClick = { model.builderMove(slot.id(), -1) },
                                enabled = enabled && index > 0,
                                modifier =
                                    Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Move ${slot.exercise().name()} up"
                                    },
                            ) {
                                Text("Move up")
                            }
                            TextButton(
                                onClick = { model.builderMove(slot.id(), 1) },
                                enabled = enabled && index < draft.slots.lastIndex,
                                modifier =
                                    Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                        contentDescription = "Move ${slot.exercise().name()} down"
                                    },
                            ) {
                                Text("Move down")
                            }
                        }
                        TextButton(
                            onClick = { model.builderRemove(slot.id()) },
                            enabled = enabled,
                            modifier =
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                                    contentDescription =
                                        "Remove ${slot.exercise().name()} from this draft"
                                },
                        ) {
                            Text("Remove exercise")
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                model.clearMessage()
                editingSlotId = null
                editorOpen = true
            },
            enabled = enabled && draft.slots.size < 100,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Add exercise")
        }
        SemanticCard(Tile.Neutral) {
            Text("Your workout preview", style = MaterialTheme.typography.titleMedium)
            Text(
                if (draft.slots.isEmpty()) "Add at least one exercise to preview and save."
                else
                    draft.slots.joinToString("\n") {
                        "${it.exercise().name()} · ${gymTargetDescription(it)}"
                    }
            )
            Text(
                "A reference load is optional guidance. It never becomes an actual logged set automatically."
            )
        }
        Button(
            onClick = { model.startBuilder(onWorkout) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text("Start this workout")
        }
        OutlinedButton(
            onClick = model::saveBuilderFavourite,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save as a favourite")
        }
        Text(
            "Saving an edit preserves previous workout revisions and training history.",
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = {
                if (draft.slots.isNotEmpty() || draft.title.isNotBlank()) confirmReset = true
                else model.newBuilder()
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("New workout")
        }
    }

    if (editorOpen) {
        GymSlotEditor(
            slot = draft.slots.firstOrNull { it.id() == editingSlotId },
            exercises = state.exercises,
            favourites = state.favouriteExerciseIds,
            recent =
                state.recent
                    .flatMap { session -> session.exercises().map { it.exercise().id() } }
                    .distinct(),
            archived = state.archivedExerciseIds,
            error = state.error,
            onDismiss = {
                editorOpen = false
                model.clearMessage()
            },
            onSave = { exercise, input ->
                if (model.builderPutSlot(editingSlotId, exercise, input)) editorOpen = false
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Start a new workout draft?") },
            text = {
                Text(
                    "This clears the current editor. Saved favourites, previous revisions and actual workouts remain."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        model.newBuilder()
                    }
                ) {
                    Text("New draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun GymSlotEditor(
    slot: GymWorkoutDefinition.Slot?,
    exercises: List<StrengthExercise>,
    favourites: Set<String>,
    recent: List<String>,
    archived: Set<String>,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (StrengthExercise, GymSlotInput) -> Unit,
) {
    val identity = slot?.id() ?: "new"
    val target = slot?.target()
    var selectedId by rememberSaveable(identity) { mutableStateOf(slot?.exercise()?.id()) }
    var query by rememberSaveable(identity) { mutableStateOf("") }
    var open by rememberSaveable(identity) { mutableStateOf(slot != null && target == null) }
    var sets by rememberSaveable(identity) { mutableStateOf(target?.sets()?.toString() ?: "3") }
    var minimumReps by
        rememberSaveable(identity) { mutableStateOf(target?.minimumReps()?.toString() ?: "") }
    var maximumReps by
        rememberSaveable(identity) { mutableStateOf(target?.maximumReps()?.toString() ?: "") }
    var seconds by
        rememberSaveable(identity) { mutableStateOf(target?.seconds()?.toString() ?: "") }
    var metres by
        rememberSaveable(identity) { mutableStateOf(target?.metres()?.let(::gymNumber) ?: "") }
    var minimumEffort by
        rememberSaveable(identity) {
            mutableStateOf(target?.effort()?.minimum()?.let(::gymNumber) ?: "")
        }
    var maximumEffort by
        rememberSaveable(identity) {
            mutableStateOf(target?.effort()?.maximum()?.let(::gymNumber) ?: "")
        }
    var rir by
        rememberSaveable(identity) {
            mutableStateOf(target?.effort()?.scale() == GymWorkoutDefinition.EffortScale.RIR)
        }
    var rest by
        rememberSaveable(identity) { mutableStateOf(target?.restSeconds()?.toString() ?: "") }
    var reference by
        rememberSaveable(identity) {
            mutableStateOf(target?.referenceLoadKg()?.let(::gymNumber) ?: "")
        }
    val selected =
        exercises.firstOrNull { it.id() == selectedId }
            ?: slot?.exercise()?.takeIf { it.id() == selectedId }
    val results =
        remember(exercises, query, favourites, recent, archived) {
            ExerciseSearch.search(exercises, query, favourites, recent, archived).take(12)
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (slot == null) "Add exercise" else "Edit exercise",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text("Choose an exercise", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search by name or alias") },
                    modifier = Modifier.fillMaxWidth(),
                )
                selected?.let {
                    SemanticCard(Tile.Strength) {
                        Text(it.name(), style = MaterialTheme.typography.titleLarge)
                        Text(gymExerciseContext(it))
                        TextButton(
                            onClick = {
                                selectedId = null
                                query = ""
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Choose another exercise")
                        }
                    }
                }
                if (selected == null) {
                    if (results.isEmpty()) Text("No matching exercises. Try another name or alias.")
                    results.forEach { exercise ->
                        OutlinedButton(
                            onClick = {
                                if (
                                    exercise.id() != selectedId &&
                                        exercise.id() != slot?.exercise()?.id()
                                ) {
                                    // Different exercises have different loading relationships. No
                                    // target or
                                    // reference is transferred merely because they share an
                                    // equipment type.
                                    open = false
                                    sets = "3"
                                    minimumReps = ""
                                    maximumReps = ""
                                    seconds = ""
                                    metres = ""
                                    minimumEffort = ""
                                    maximumEffort = ""
                                    rir = false
                                    rest = ""
                                    reference = ""
                                }
                                selectedId = exercise.id()
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(exercise.name())
                                Text(
                                    gymExerciseContext(exercise),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    if (query.isBlank())
                        Text(
                            "Relevant recent and favourite exercises appear first. Search to explore the catalogue.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                }
                if (selected != null) {
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = open,
                            onCheckedChange = { open = it },
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Open exercise without prescribed targets"
                                },
                        )
                        Column(Modifier.weight(1f).padding(top = 12.dp)) {
                            Text("Open exercise")
                            Text(
                                "Choose sets and measurements while training.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (!open) {
                        GymBuilderField(sets, { sets = it }, "Working sets", KeyboardType.Number)
                        val measurement = selected.measurement()
                        if (measurement.usesReps()) {
                            GymBuilderField(
                                minimumReps,
                                { minimumReps = it },
                                "Minimum repetitions",
                                KeyboardType.Number,
                            )
                            GymBuilderField(
                                maximumReps,
                                { maximumReps = it },
                                "Maximum repetitions",
                                KeyboardType.Number,
                            )
                        }
                        if (measurement.usesDuration())
                            GymBuilderField(
                                seconds,
                                { seconds = it },
                                "Duration per set · seconds",
                                KeyboardType.Number,
                            )
                        if (measurement.usesDistance())
                            GymBuilderField(
                                metres,
                                { metres = it },
                                "Distance per set · metres",
                                KeyboardType.Decimal,
                            )
                        Text(
                            "Intensity guidance · optional",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Leave both effort fields blank to train without an effort target.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (measurement.usesReps()) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { rir = false },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    enabled = rir,
                                ) {
                                    Text(if (!rir) "RPE selected" else "Use RPE")
                                }
                                OutlinedButton(
                                    onClick = { rir = true },
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                    enabled = !rir,
                                ) {
                                    Text(if (rir) "RIR selected" else "Use RIR")
                                }
                            }
                        }
                        val effortLabel = if (rir && measurement.usesReps()) "RIR" else "RPE"
                        GymBuilderField(
                            minimumEffort,
                            { minimumEffort = it },
                            "Minimum $effortLabel · optional",
                            KeyboardType.Decimal,
                        )
                        GymBuilderField(
                            maximumEffort,
                            { maximumEffort = it },
                            "Maximum $effortLabel · optional",
                            KeyboardType.Decimal,
                        )
                        GymBuilderField(
                            rest,
                            { rest = it },
                            "Rest between sets · seconds, optional",
                            KeyboardType.Number,
                        )
                        if (
                            measurement.usesLoad() &&
                                selected.mode() != StrengthExercise.Mode.BODYWEIGHT
                        ) {
                            GymBuilderField(
                                reference,
                                { reference = it },
                                "Reference load · kg, optional",
                                KeyboardType.Decimal,
                            )
                            Text(
                                "A reference is guidance only. You will enter the actual load when training.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    error?.let { SemanticCard(Tile.Warning) { Text(it) } }
                    Button(
                        onClick = {
                            onSave(
                                selected,
                                GymSlotInput(
                                    open,
                                    sets,
                                    minimumReps,
                                    maximumReps,
                                    seconds,
                                    metres,
                                    minimumEffort,
                                    maximumEffort,
                                    rir && selected.measurement().usesReps(),
                                    rest,
                                    reference,
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Save exercise")
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun GymBuilderField(
    value: String,
    change: (String) -> Unit,
    label: String,
    keyboard: KeyboardType,
) {
    OutlinedTextField(
        value = value,
        onValueChange = change,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

internal fun gymExerciseContext(exercise: StrengthExercise): String =
    ExerciseCatalog.definition(exercise.id())?.let { "${it.category()} · ${it.equipment()}" }
        ?: "Custom exercise · ${exercise.measurement().name().lowercase(Locale.UK).replace('_', ' ')}"

internal fun gymTargetDescription(slot: GymWorkoutDefinition.Slot): String {
    val target = slot.target() ?: return "Open · record the sets you choose"
    val measurement =
        target.minimumReps()?.let { minimum ->
            val maximum = target.maximumReps()
            if (minimum == maximum) "$minimum reps" else "$minimum–$maximum reps"
        }
            ?: target.seconds()?.let { "${it}s per set" }
            ?: target.metres()?.let { "${gymNumber(it)} m per set" }
            ?: ""
    val effort =
        target.effort()?.let {
            " · ${it.scale().name()} ${gymNumber(it.minimum())}–${gymNumber(it.maximum())}"
        } ?: ""
    val rest = target.restSeconds()?.let { " · ${it}s rest" } ?: ""
    val reference = target.referenceLoadKg()?.let { " · ${gymNumber(it)} kg reference" } ?: ""
    return "${target.sets()} × $measurement$effort$rest$reference"
}

private fun gymNumber(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
