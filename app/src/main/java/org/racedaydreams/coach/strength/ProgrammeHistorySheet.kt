package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

/** Bounded SQLite summaries; opening one revision loads only its preserved prescription. */
@Composable
internal fun ProgrammeHistorySheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    if (!state.historyOpen) return
    val programme = state.historicalProgramme
    var workoutId by
        rememberSaveable(programme?.id(), programme?.revision()) { mutableStateOf<String?>(null) }
    val enabled = !state.busy
    val listState = rememberLazyListState()
    LaunchedEffect(programme?.id(), programme?.revision(), workoutId, state.historyPage) {
        listState.scrollToItem(0)
    }
    Dialog(
        onDismissRequest = { if (enabled) model.closeHistory() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(onClick = model::closeHistory, enabled = enabled) {
                        Text("Back to programme library")
                    }
                    Text(
                        "Programme revision history",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Preserved prescriptions, not completed workouts. Reviewing history never reactivates a programme."
                    )
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                if (programme == null) {
                    if (state.history.isEmpty())
                        item {
                            Text(
                                "No accepted authored programme revisions yet. Legacy gym history remains available in Strength."
                            )
                        }
                    items(state.history, key = { "${it.id()}:${it.revision()}" }) { row ->
                        SemanticCard(Tile.Strength) {
                            Text(row.name(), style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Revision ${row.revision()} · ${friendlyDate(row.start())} – ${friendlyDate(row.end())}"
                            )
                            OutlinedButton(
                                onClick = { model.reviewHistorical(row.id(), row.revision()) },
                                enabled = enabled,
                            ) {
                                Text("Review ${row.name()} revision ${row.revision()}")
                            }
                        }
                    }
                    item {
                        Text("History page ${state.historyPage + 1}")
                        if (state.historyPage > 0)
                            TextButton(onClick = { model.historyPage(-1) }, enabled = enabled) {
                                Text("Newer programme revisions")
                            }
                        if (state.historyMore)
                            TextButton(onClick = { model.historyPage(1) }, enabled = enabled) {
                                Text("Older programme revisions")
                            }
                    }
                } else {
                    item {
                        TextButton(onClick = model::historicalList, enabled = enabled) {
                            Text("All programme revisions")
                        }
                        Text(
                            "${programme.name()} · revision ${programme.revision()}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "${friendlyDate(programme.start())} – ${friendlyDate(programme.end())} · preserved read-only"
                        )
                        WhyDetails(programme.reason(), "Why this revision?")
                        if (programme.tradeoffs().isNotEmpty())
                            WhyDetails(
                                programme.tradeoffs().joinToString("\n"),
                                "Programme trade-offs",
                            )
                    }
                    val workout = programme.workouts().firstOrNull { it.id() == workoutId }
                    if (workout != null) {
                        item {
                            TextButton(onClick = { workoutId = null }) {
                                Text("All dated prescriptions")
                            }
                            Text(
                                workout.definition().title(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "${friendlyDate(workout.date())} · about ${workout.expectedMinutes()} min"
                            )
                            workout.notes()?.let { WhyDetails(it, "Workout guidance") }
                        }
                        items(workout.definition().slots(), key = { it.id() }) { slot ->
                            SemanticCard(Tile.Strength) {
                                Text(
                                    slot.exercise().name(),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(gymTargetDescription(slot))
                            }
                        }
                    } else {
                        programme.blocks().forEach { block ->
                            item(key = "block:${block.id()}") {
                                Text(block.name(), style = MaterialTheme.typography.titleLarge)
                                WhyDetails(block.explanation(), "Block emphasis")
                            }
                            items(
                                block.workouts().sortedBy { it.date() },
                                key = { "workout:${it.id()}" },
                            ) { row ->
                                TextButton(onClick = { workoutId = row.id() }) {
                                    Text(
                                        "${friendlyDate(row.date())} · ${row.definition().title()}"
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
