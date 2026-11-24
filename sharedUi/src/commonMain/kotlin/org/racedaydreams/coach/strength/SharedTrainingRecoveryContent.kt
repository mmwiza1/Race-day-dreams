package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** Existing production content; platforms supply date labels and explicit preview callbacks. */
@Composable
fun SharedTrainingRecoveryContent(
    state: TrainingRecoveryState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onKeep: () -> Unit,
    onLighterWeek: () -> Unit,
    onMove: (String, LocalDate) -> Unit,
    dateLabel: (LocalDate) -> String,
    instantLabel: (Instant) -> String,
    appBarBack: Boolean = false,
) {
    var evidence by rememberSaveable { mutableStateOf(false) }
    var scheduling by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        // Android hosts pass appBarBack = true: the shell's app bar already has Back for this
        // route (audit C-04). iOS has no app bar Back, so it keeps this in-content control.
        if (!appBarBack)
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        ScreenHeader(Discipline.RECOVERY, Glyph.BODY, "Recovery", "Recovery & training balance")
        QuietNote("Useful options from your recorded training. You decide what feels appropriate.")
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
            // The guidance leads as a strong headline with its one primary choice.
            Panel(Modifier.testTag("recovery-guidance")) {
                Text(
                    requireNotNull(review.heading()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Here's what to do",
                    style = MaterialTheme.typography.titleMedium,
                    color = Discipline.RECOVERY.accent(),
                )
                Text(
                    requireNotNull(review.instruction()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                QuietNote("No targets or future weeks have changed.")
                if (state.canReviewLighterWeek)
                    Button(
                        onClick = onLighterWeek,
                        enabled = !state.busy,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Discipline.RECOVERY.accent(),
                                contentColor = Discipline.RECOVERY.onAccent(),
                            ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Text("Preview a lighter week")
                    }
                OutlinedButton(
                    onClick = onKeep,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text("Keep my current programme")
                }
            }
            ExpressiveSection("Why this guidance?", Glyph.TREND, ChartHue.TEAL) {
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
                    DisclosureLabel("Supporting evidence", evidence)
                }
                if (evidence) {
                    if (review.evidence().isEmpty())
                        Text("No qualifying demanding-training evidence is available.")
                    review.evidence().forEach { row ->
                        Text(instantLabel(requireNotNull(row.at())) + " · " + row.explanation())
                    }
                    review.limitations().forEach { QuietNote(it) }
                }
            }
        }
        if (state.conflicts.isNotEmpty()) {
            ExpressiveSection("Running & gym schedule", Glyph.CALENDAR, ChartHue.BLUE) {
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
                    DisclosureLabel("Review schedule options", scheduling)
                }
                if (scheduling)
                    state.conflicts
                        .groupBy { requireNotNull(it.workoutId()) }
                        .forEach { (id, conflicts) ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            val workout = state.programme?.workouts()?.firstOrNull { it.id() == id }
                            Text(
                                "${workout?.definition()?.title() ?: "Gym workout"} · ${dateLabel(requireNotNull(conflicts.first().gymDate()))}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            conflicts.forEach { Text(requireNotNull(it.explanation())) }
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
                                    Text("Preview move to ${dateLabel(day)}")
                                }
                            }
                        }
                QuietNote(
                    "Saving a preview does not move a session. Review and activate separately; your running plan stays unchanged."
                )
            }
        }
        if (state.review?.lighterWeek() == true && state.programme == null)
            QuietNote(
                "This programme uses the earlier programme format. You can keep it or review a new programme through Gym programme; existing workouts and history remain available."
            )
        OutlinedButton(
            onClick = onRefresh,
            enabled = !state.busy,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Refresh local evidence")
        }
    }
}

/**
 * A disclosure button's label with its ▾/▴ glyph hidden from screen readers (audit C-R05): the
 * button's "Expanded"/"Collapsed" state already says what the glyph shows.
 */
@Composable
private fun DisclosureLabel(label: String, expanded: Boolean) {
    Text(label)
    Text(if (expanded) " ▴" else " ▾", modifier = Modifier.clearAndSetSemantics {})
}
