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
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.LocalDate

/** A bounded retained-version view; this screen cannot activate or create actuals. */
data class ProgrammeHistoryReview(
    val historicalProgramme: StrengthProgramme?,
    val history: List<StrengthProgrammingRepository.Summary>,
    val historyPage: Int,
    val historyMore: Boolean,
    val busy: Boolean,
    val error: String?,
)

@Composable
fun SharedProgrammeHistoryContent(
    state: ProgrammeHistoryReview,
    closeHistory: () -> Unit,
    reviewHistorical: (String, Int) -> Unit,
    changePage: (Int) -> Unit,
    historicalList: () -> Unit,
    formatDate: (LocalDate) -> String,
    formatTarget: (GymWorkoutDefinition.Slot) -> String,
) {
    val programme = state.historicalProgramme
    var workoutId by
        rememberSaveable(programme?.id(), programme?.revision()) { mutableStateOf<String?>(null) }
    val enabled = !state.busy
    val listState = rememberLazyListState()
    LaunchedEffect(programme?.id(), programme?.revision(), workoutId, state.historyPage) {
        listState.scrollToItem(0)
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TextButton(onClick = closeHistory, enabled = enabled) {
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
                        Text(
                            requireNotNull(row.name()),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "Revision ${row.revision()} · ${formatDate(requireNotNull(row.start()))} – ${formatDate(requireNotNull(row.end()))}"
                        )
                        OutlinedButton(
                            onClick = {
                                reviewHistorical(requireNotNull(row.id()), row.revision())
                            },
                            enabled = enabled,
                        ) {
                            Text("Review ${row.name()} revision ${row.revision()}")
                        }
                    }
                }
                item {
                    Text("History page ${state.historyPage + 1}")
                    if (state.historyPage > 0)
                        TextButton(onClick = { changePage(-1) }, enabled = enabled) {
                            Text("Newer programme revisions")
                        }
                    if (state.historyMore)
                        TextButton(onClick = { changePage(1) }, enabled = enabled) {
                            Text("Older programme revisions")
                        }
                }
            } else {
                item {
                    TextButton(onClick = historicalList, enabled = enabled) {
                        Text("All programme revisions")
                    }
                    Text(
                        "${programme.name()} · revision ${programme.revision()}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        "${formatDate(programme.start())} – ${formatDate(programme.end())} · preserved read-only"
                    )
                    WhyDetails(programme.reason(), "Why this revision?")
                    if (programme.tradeoffs().isNotEmpty())
                        WhyDetails(programme.tradeoffs().joinToString("\n"), "Programme trade-offs")
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
                            "${formatDate(workout.date())} · about ${workout.expectedMinutes()} min"
                        )
                        workout.notes()?.let { WhyDetails(it, "Workout guidance") }
                    }
                    items(workout.definition().slots(), key = { it.id() }) { slot ->
                        SemanticCard(Tile.Strength) {
                            Text(
                                slot.exercise().name(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(formatTarget(slot))
                        }
                    }
                } else {
                    programme.blocks().forEach { block ->
                        item(key = "block:${block.id()}") {
                            Text(block.name(), style = MaterialTheme.typography.titleLarge)
                            WhyDetails(block.explanation(), "Block emphasis")
                        }
                        items(
                            block.workouts().sortedBy { it.date().toEpochDay() },
                            key = { "workout:${it.id()}" },
                        ) { row ->
                            TextButton(onClick = { workoutId = row.id() }) {
                                Text("${formatDate(row.date())} · ${row.definition().title()}")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
