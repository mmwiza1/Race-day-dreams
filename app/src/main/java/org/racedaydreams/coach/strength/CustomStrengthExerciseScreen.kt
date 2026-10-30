@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

/**
 * Modest structured metadata entry. These definitions never acquire fabricated technique content.
 */
@Composable
fun CustomStrengthExerciseScreen(
    model: StrengthProgrammeViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf<String?>(null) }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var task by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var equipmentFree by rememberSaveable { mutableStateOf(false) }
    var showEquipment by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val enabled = !state.loading && !state.busy
    CoachPage {
        TextButton(onClick = onBack, enabled = !state.busy) { Text("Back") }
        CoachHeading("Custom exercise")
        Text(
            "Define the movement you want to log or add to your own programme. Existing exercises and history are unchanged."
        )
        if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        (error ?: state.error)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        OutlinedTextField(
            name,
            { name = it.take(120) },
            label = { Text("Exercise name") },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        CustomMetadataChoice(
            "Body area",
            ExerciseMetadata.BodyArea.values().map { it.name to it.label() },
            area,
            enabled,
        ) {
            area = it
        }
        CustomMetadataChoice(
            "Movement role",
            ExerciseMetadata.Role.values().map { it.name to it.label() },
            role,
            enabled,
        ) {
            role = it
        }
        CustomMetadataChoice("Measurement", customTaskChoices(), task, enabled) { task = it }
        TextButton(onClick = { showEquipment = !showEquipment }, enabled = enabled) {
            Text(
                if (equipmentFree) "Equipment · none required"
                else "Equipment · ${equipment.size} selected"
            )
        }
        if (showEquipment) {
            Text(
                "Select everything needed for this exercise, or explicitly choose no equipment. This does not change your programme's available equipment."
            )
            Row {
                Checkbox(
                    equipmentFree,
                    {
                        equipmentFree = it
                        if (it) equipment = emptyList()
                    },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = "No equipment required" },
                )
                TextButton(
                    onClick = {
                        equipmentFree = !equipmentFree
                        if (equipmentFree) equipment = emptyList()
                    },
                    enabled = enabled,
                ) {
                    Text("No equipment required")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ExerciseMetadata.Equipment.values().forEach { item ->
                    FilterChip(
                        item.name in equipment,
                        {
                            equipmentFree = false
                            equipment =
                                if (item.name in equipment) equipment - item.name
                                else equipment + item.name
                        },
                        enabled = enabled,
                        label = { Text(programmeLabel(item.name)) },
                    )
                }
            }
        }
        OutlinedTextField(
            notes,
            { notes = it.take(2000) },
            label = { Text("Notes · optional") },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        SemanticCard(Tile.Insight) {
            Text("Your definition", style = MaterialTheme.typography.titleMedium)
            Text(
                "Logging, history and valid recorded PBs are supported. There is no app-authored technique guide, estimated 1RM or automatic progression for a custom movement. Generated programmes continue to use the canonical catalogue."
            )
        }
        Button(
            onClick = {
                try {
                    require(name.isNotBlank()) { "Enter an exercise name." }
                    require(area != null && role != null && task != null) {
                        "Choose body area, movement role and measurement."
                    }
                    require(equipmentFree || equipment.isNotEmpty()) {
                        "Select the required equipment, or explicitly choose no equipment."
                    }
                    val weightedBodyweight = task == "WEIGHTED_BODYWEIGHT"
                    val measurement =
                        if (weightedBodyweight) StrengthExercise.MeasurementProfile.LOAD_REPS
                        else StrengthExercise.MeasurementProfile.valueOf(task!!)
                    error = null
                    model.createCustom(
                        name,
                        ExerciseMetadata.BodyArea.valueOf(area!!),
                        ExerciseMetadata.Role.valueOf(role!!),
                        measurement,
                        weightedBodyweight,
                        equipment.map { ExerciseMetadata.Equipment.valueOf(it) }.toSet(),
                        notes,
                        onSaved,
                    )
                } catch (failure: IllegalArgumentException) {
                    error = failure.message ?: "Review the exercise details."
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save custom exercise")
        }
    }
}

@Composable
private fun CustomMetadataChoice(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    enabled: Boolean,
    select: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { expanded = !expanded },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("$title · ${options.firstOrNull { it.first == selected }?.second ?: "Choose"}")
    }
    if (expanded) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (id, label) ->
                FilterChip(
                    id == selected,
                    {
                        select(id)
                        expanded = false
                    },
                    label = { Text(label) },
                    enabled = enabled,
                )
            }
        }
    }
}

private fun customTaskChoices() =
    listOf(
        "LOAD_REPS" to "Load + repetitions",
        "REPS_ONLY" to "Repetitions",
        "WEIGHTED_BODYWEIGHT" to "Bodyweight + added load + repetitions",
        "DURATION" to "Duration",
        "LOAD_DURATION" to "Load + duration",
        "DISTANCE" to "Distance",
        "LOAD_DISTANCE" to "Load + distance",
    )

@Composable
internal fun CustomExerciseMetadataSummary(metadata: CustomStrengthExercise) {
    Text("${metadata.bodyArea().label()} · ${metadata.role().label()}")
    metadata.notes()?.let { Text(it) }
    Text(
        "User-defined movement. No app-authored technique, estimated 1RM or automatic progression is inferred."
    )
}
