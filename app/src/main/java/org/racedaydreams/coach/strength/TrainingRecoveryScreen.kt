package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.*

@Composable
fun TrainingRecoveryScreen(
    model: TrainingRecoveryViewModel,
    onBack: () -> Unit,
    onReview: (String) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    TrainingRecoveryContent(
        state,
        onBack,
        model::refresh,
        model::keep,
        { model.prepareLighterWeek(onReview) },
        { id, date -> model.prepareMove(id, date, onReview) },
    )
}

@Composable
fun TrainingRecoveryContent(
    state: TrainingRecoveryState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onKeep: () -> Unit,
    onLighterWeek: () -> Unit,
    onMove: (String, java.time.LocalDate) -> Unit,
) {
    var evidence by rememberSaveable { mutableStateOf(false) }
    var scheduling by rememberSaveable { mutableStateOf(false) }
    val date = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK) }
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        CoachHeading("Recovery & training balance")
        Text("Useful options from your recorded training. You decide what feels appropriate.")
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        state.notice?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.review?.let { review ->
            SemanticCard(Tile.Strength) {
                Text(review.heading(), style = MaterialTheme.typography.titleLarge)
                Text("Here's what to do", style = MaterialTheme.typography.titleMedium)
                Text(review.instruction())
                Text(
                    "No targets or future weeks have changed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (state.canReviewLighterWeek)
                    Button(onClick = onLighterWeek, enabled = !state.busy) {
                        Text("Preview a lighter week")
                    }
                OutlinedButton(onClick = onKeep, enabled = !state.busy) {
                    Text("Keep my current programme")
                }
            }
            SemanticCard(Tile.Progress) {
                Text("Why this guidance?", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (review.lighterWeek())
                        "Several recorded strength workouts were very demanding across at least a week. A lighter week is an option, not a requirement."
                    else if (review.reviewToday())
                        "Recent recorded training or corroborating personal trends suggest reviewing today's effort. This is supporting context, not a recovery diagnosis."
                    else
                        "The available records do not establish a reason to reduce your programme. Missing training or effort stays unknown."
                )
                TextButton(
                    onClick = { evidence = !evidence },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (evidence) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(if (evidence) "Supporting evidence ▴" else "Supporting evidence ▾")
                }
                if (evidence) {
                    if (review.evidence().isEmpty())
                        Text("No qualifying demanding-training evidence is available.")
                    review.evidence().forEach { row ->
                        Text(
                            date.format(row.at().atZone(java.time.ZoneId.systemDefault())) +
                                " · " +
                                row.explanation()
                        )
                    }
                    review.limitations().forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (state.conflicts.isNotEmpty()) {
            SemanticCard(Tile.Planned) {
                Text("Running & gym schedule", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your lower-body gym work is close to a demanding planned run. Both sessions can stay; review the trade-off if you want."
                )
                TextButton(
                    onClick = { scheduling = !scheduling },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (scheduling) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(
                        if (scheduling) "Review schedule options ▴" else "Review schedule options ▾"
                    )
                }
                if (scheduling)
                    state.conflicts
                        .groupBy { it.workoutId() }
                        .forEach { (id, conflicts) ->
                            HorizontalDivider()
                            val workout = state.programme?.workouts()?.firstOrNull { it.id() == id }
                            Text(
                                "${workout?.definition()?.title() ?: "Gym workout"} · ${date.format(conflicts.first().gymDate())}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            conflicts.forEach { Text(it.explanation()) }
                            val options = state.alternatives[id].orEmpty()
                            if (options.isEmpty())
                                Text(
                                    "No less-conflicting date is available in this programme week. Keep the schedule or review future programming manually."
                                )
                            options.take(3).forEach { day ->
                                OutlinedButton(
                                    onClick = { onMove(id, day) },
                                    enabled = !state.busy,
                                ) {
                                    Text("Preview move to ${date.format(day)}")
                                }
                            }
                        }
                Text(
                    "Saving a preview does not move a session. Review and activate separately; your running plan stays unchanged."
                )
            }
        }
        if (state.review?.lighterWeek() == true && state.programme == null)
            Text(
                "This programme uses the earlier programme format. You can keep it or review a new programme through Gym programme; existing workouts and history remain available."
            )
        OutlinedButton(onClick = onRefresh, enabled = !state.busy) {
            Text("Refresh local evidence")
        }
    }
}
