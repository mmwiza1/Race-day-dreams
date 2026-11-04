@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

/** A programme proposal, not a live execution replacement or an automatic programme change. */
@Composable
internal fun ProgrammeSwapSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val review = state.swapReview ?: return
    val enabled = !state.busy
    var selectedId by
        rememberSaveable(review.draftId, review.slotId) { mutableStateOf<String?>(null) }
    var allFuture by rememberSaveable(review.draftId, review.slotId) { mutableStateOf(false) }
    var showReasons by rememberSaveable(review.draftId, review.slotId) { mutableStateOf(false) }
    var visible by rememberSaveable(review.draftId, review.slotId) { mutableIntStateOf(4) }
    var targetEditor by remember { mutableStateOf(false) }
    val chosen = review.suggestions.firstOrNull { it.exercise().id() == selectedId }
    Dialog(
        onDismissRequest = { if (enabled) model.closeProgrammeSwap() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(onClick = model::closeProgrammeSwap, enabled = enabled) {
                        Text("Back to programme preview")
                    }
                    Text(
                        "Programme replacement",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(review.original.name(), style = MaterialTheme.typography.titleLarge)
                    Text("Save a future proposal. Your programme changes only after activation.")
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    OutlinedButton(onClick = { showReasons = !showReasons }, enabled = enabled) {
                        Text("${gymSwapReason(review.reason)} · Change reason")
                    }
                    if (showReasons)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExerciseSubstitutionEngine.Reason.values().forEach { reason ->
                                FilterChip(
                                    reason == review.reason,
                                    {
                                        selectedId = null
                                        visible = 4
                                        showReasons = false
                                        model.programmeSwapReason(reason)
                                    },
                                    enabled = enabled,
                                    label = { Text(gymSwapReason(reason)) },
                                )
                            }
                        }
                    if (review.reason == ExerciseSubstitutionEngine.Reason.UNCOMFORTABLE)
                        SemanticCard(Tile.Warning) {
                            Text(ExerciseSubstitutionEngine.discomfortGuidance())
                        }
                    WhyDetails(
                        "Options use this reviewed programme's equipment, experience and goal. To change available equipment across the programme, use future regeneration. Favourites only help rank already comparable options. No load transfers between exercises.",
                        "Equipment & choice context",
                    )
                }
                if (chosen == null) {
                    item {
                        Text("Comparable alternatives", style = MaterialTheme.typography.titleLarge)
                        if (review.suggestions.isEmpty()) {
                            val canonical =
                                ExerciseCatalog.definition(review.original.id())?.exercise()
                            val supported =
                                canonical != null &&
                                    canonical.mode() == review.original.mode() &&
                                    canonical.measurement() == review.original.measurement() &&
                                    canonical.region() == review.original.region()
                            Text(
                                if (supported)
                                    "No comparable alternative matches this context. Review equipment, exclusions or the programme before changing it."
                                else
                                    "No automatic match is inferred for this custom or retained task. Add a replacement with fresh targets to the dated workout, then remove this slot. History remains preserved."
                            )
                        }
                    }
                    items(review.suggestions.take(visible), key = { it.exercise().id() }) {
                        suggestion ->
                        SemanticCard(Tile.Strength) {
                            Text(
                                suggestion.exercise().name(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(gymExerciseContext(suggestion.exercise()))
                            Text(
                                suggestion.explanation().substringBefore(". ") +
                                    if (suggestion.explanation().contains(". ")) "." else ""
                            )
                            WhyDetails(suggestion.explanation(), "Why this alternative?")
                            OutlinedButton(
                                onClick = {
                                    selectedId = suggestion.exercise().id()
                                    allFuture = false
                                },
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Choose ${suggestion.exercise().name()}")
                            }
                        }
                    }
                    if (visible < review.suggestions.size)
                        item {
                            TextButton(onClick = { visible += 4 }, enabled = enabled) {
                                Text("Show more alternatives")
                            }
                        }
                } else
                    item {
                        SemanticCard(Tile.Planned) {
                            Text(
                                chosen.exercise().name(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(chosen.explanation())
                            if (!chosen.measurementChanged()) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .toggleable(
                                            allFuture,
                                            enabled = enabled,
                                            role = Role.Checkbox,
                                            onValueChange = { allFuture = it },
                                        )
                                ) {
                                    Checkbox(allFuture, null, enabled = enabled)
                                    Text(
                                        "Apply to all eligible future occurrences of ${review.original.name()}",
                                        Modifier.weight(1f),
                                    )
                                }
                                Text(
                                    if (allFuture)
                                        "Earlier and completed workouts are excluded. Each phase retains its own rep, effort and rest targets."
                                    else
                                        "Only this dated workout changes. Other future workouts remain as prescribed."
                                )
                                Text(
                                    "Load references are cleared. Choose and record your actual load when training."
                                )
                                Button(
                                    onClick = {
                                        model.useProgrammeSwap(
                                            chosen.exercise().id(),
                                            null,
                                            allFuture,
                                        )
                                    },
                                    enabled = enabled,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Save programme replacement proposal")
                                }
                            } else {
                                Text(
                                    "This changes the measurement task. Review a fresh target for this dated workout; no weight reference transfers."
                                )
                                Button(
                                    onClick = { targetEditor = true },
                                    enabled = enabled,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Review new measurement target")
                                }
                            }
                            TextButton(onClick = { selectedId = null }, enabled = enabled) {
                                Text("Back to alternatives")
                            }
                        }
                    }
                item {
                    TextButton(
                        onClick = {
                            selectedId = null
                            model.programmeSwapReason(review.reason)
                        },
                        enabled = enabled,
                    ) {
                        Text("Refresh replacement choices")
                    }
                    Text(
                        "For a change only during an active workout, use Gym Now's today-only replacement controls."
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    if (targetEditor && chosen != null) {
        GymSlotEditor(
            GymWorkoutDefinition.Slot(review.slotId, chosen.exercise(), null, null),
            listOf(chosen.exercise()),
            emptySet(),
            emptyList(),
            emptySet(),
            emptySet(),
            state.error,
            allowOpen = false,
            allowExerciseChange = false,
            allowReferenceLoad = false,
            onDismiss = {
                if (enabled) {
                    targetEditor = false
                    model.clearMessage()
                }
            },
            onSave = { _, input -> model.useProgrammeSwap(chosen.exercise().id(), input, false) },
        )
    }
}
