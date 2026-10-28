@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine

/** Intent-ranked, explicitly accepted choices for this workout only. No programme mutation. */
@Composable
fun GymSubstitutionSheet(model: GymNowViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val swap = state.substitution ?: return
    val enabled = !state.busy && state.view?.pending() == null
    var equipmentExpanded by
        rememberSaveable(swap.executionId, swap.slotId) { mutableStateOf(false) }
    var chosenId by
        rememberSaveable(swap.executionId, swap.slotId) { mutableStateOf<String?>(null) }
    val chosen = swap.suggestions.firstOrNull { it.exercise().id() == chosenId }

    Dialog(
        onDismissRequest = { if (enabled) model.closeSubstitutions() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(
                        onClick = model::closeSubstitutions,
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Back to workout")
                    }
                    Text(
                        if (swap.reason == ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY)
                            "Equipment busy?"
                        else "Swap exercise",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(swap.original.name(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Choose an alternative for today. Your programme, saved workout and previous sets stay as recorded."
                    )
                }
                item {
                    Text("Why swap?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExerciseSubstitutionEngine.Reason.values().forEach { reason ->
                            FilterChip(
                                selected = swap.reason == reason,
                                onClick = {
                                    chosenId = null
                                    model.changeSubstitutionReason(reason)
                                },
                                enabled = enabled,
                                label = { Text(gymSwapReason(reason)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
                if (swap.reason == ExerciseSubstitutionEngine.Reason.UNCOMFORTABLE) {
                    item {
                        SemanticCard(Tile.Warning) {
                            Text("Movement comfort", style = MaterialTheme.typography.titleMedium)
                            Text(ExerciseSubstitutionEngine.discomfortGuidance())
                        }
                    }
                }
                item {
                    SemanticCard(Tile.Neutral) {
                        Text(
                            "Equipment for this swap",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(swap.contextExplanation)
                        Text(
                            "Goal: ${gymEquipmentName(swap.goal.name)} · ${swap.experience.name.lowercase(Locale.UK)} experience"
                        )
                        Text(
                            if (swap.equipment.isEmpty())
                                "No external equipment confirmed. Matching bodyweight alternatives can still appear."
                            else "${swap.equipment.size} equipment capabilities selected."
                        )
                        TextButton(
                            onClick = { equipmentExpanded = !equipmentExpanded },
                            enabled = enabled,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(
                                if (equipmentExpanded) "Hide equipment choices"
                                else "Check available equipment"
                            )
                        }
                        if (equipmentExpanded) {
                            Text(
                                "Choose every item needed for a complete setup. A cable station does not imply every attachment, and one machine does not imply all machines. These choices apply only to this swap."
                            )
                            ExerciseMetadata.EquipmentGroup.values().forEach { group ->
                                val equipment =
                                    ExerciseMetadata.Equipment.values().filter {
                                        it.group() == group
                                    }
                                if (equipment.isNotEmpty()) {
                                    Text(
                                        gymEquipmentName(group.name),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                    equipment.forEach { item ->
                                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                            Checkbox(
                                                checked = item in swap.equipment,
                                                onCheckedChange = { checked ->
                                                    chosenId = null
                                                    model.changeSubstitutionEquipment(
                                                        if (checked) swap.equipment + item
                                                        else swap.equipment - item
                                                    )
                                                },
                                                enabled = enabled,
                                                modifier =
                                                    Modifier.semantics {
                                                        contentDescription =
                                                            "${gymEquipmentName(item.name)} available for this swap"
                                                    },
                                            )
                                            Text(
                                                gymEquipmentName(item.name),
                                                modifier = Modifier.weight(1f).padding(top = 12.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    if (state.busy) CircularProgressIndicator()
                    state.error?.let { error -> SemanticCard(Tile.Warning) { Text(error) } }
                    Text("Comparable alternatives", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Matched by movement role, body area, measurement, equipment and experience. Loads do not transfer between exercises."
                    )
                    if (swap.suggestions.isEmpty()) {
                        SemanticCard(Tile.Insight) {
                            Text("No comparable alternative matches these choices.")
                            Text(
                                "Check available equipment or choose Show alternatives. No unrelated exercise has been added to fill this list."
                            )
                        }
                    }
                }
                items(swap.suggestions.take(swap.visibleLimit), key = { it.exercise().id() }) {
                    suggestion ->
                    SemanticCard(Tile.Strength) {
                        Text(
                            suggestion.exercise().name(),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(gymExerciseContext(suggestion.exercise()))
                        Text(suggestion.explanation())
                        OutlinedButton(
                            onClick = { chosenId = suggestion.exercise().id() },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Choose ${suggestion.exercise().name()}")
                        }
                    }
                }
                if (swap.suggestions.size > swap.visibleLimit) {
                    item {
                        TextButton(
                            onClick = model::showMoreSubstitutions,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Show more alternatives")
                        }
                    }
                }
                item {
                    Text(
                        "This flow changes today's execution only. Structural programme changes are reviewed separately in the programme editor.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
            }
        }
    }
    if (chosen != null) {
        AlertDialog(
            onDismissRequest = { if (enabled) chosenId = null },
            title = { Text("Use ${chosen.exercise().name()} just for today?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Your original prescription and all saved sets are preserved. Your programme and favourites will not change."
                    )
                    Text(
                        "Any unsaved current-set inputs will clear after this change is saved. Choose a fresh load or task; previous loading does not transfer."
                    )
                    if (chosen.measurementChanged())
                        Text(
                            "The measurement changes, so the old target is removed. Record what you perform rather than copying the previous target."
                        )
                }
            },
            confirmButton = {
                Button(
                    onClick = { model.replaceForToday(chosen.exercise()) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Use just for today")
                }
            },
            dismissButton = {
                TextButton(onClick = { chosenId = null }, enabled = enabled) {
                    Text("Keep current exercise")
                }
            },
        )
    }
}

internal fun gymSwapReason(reason: ExerciseSubstitutionEngine.Reason): String =
    when (reason) {
        ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY -> "Equipment busy"
        ExerciseSubstitutionEngine.Reason.EQUIPMENT_UNAVAILABLE -> "Equipment unavailable"
        ExerciseSubstitutionEngine.Reason.UNCOMFORTABLE -> "Movement uncomfortable"
        ExerciseSubstitutionEngine.Reason.EASIER -> "Easier variation"
        ExerciseSubstitutionEngine.Reason.HARDER -> "Harder variation"
        ExerciseSubstitutionEngine.Reason.ALTERNATIVES -> "Show alternatives"
    }

private fun gymEquipmentName(value: String): String =
    value.lowercase(Locale.UK).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.UK) }
