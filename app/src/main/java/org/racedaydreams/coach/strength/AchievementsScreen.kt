package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*

@Composable
fun AchievementsScreen(
    model: AchievementsViewModel,
    onSource: (org.racedaydreams.domain.achievements.TrainingMilestones.Proof) -> Unit,
    onBack: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        CoachHeading("Recorded milestones")
        Text(
            "Recognise your training without streaks, penalties or pressure to train through a break."
        )
        if (state.loading) CircularProgressIndicator()
        state.error?.let { SemanticCard(Tile.Warning) { Text(it) } }
        state.notice?.let {
            SemanticCard(Tile.Progress) {
                Text(it)
                TextButton(onClick = model::dismiss) { Text("Dismiss celebration") }
            }
        }
        SemanticCard(Tile.Neutral) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Checkbox(
                    checked = state.celebrations,
                    onCheckedChange = { model.refresh(it) },
                    enabled = !state.loading,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Celebrate new milestones in app"
                        },
                )
                Text(
                    "Celebrate new milestones in app",
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
            }
            Text(
                "Optional and off by default. No system notifications or missed-training reminders. Your records remain available either way.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { model.refresh() }, enabled = !state.loading) {
                Text("Refresh recorded evidence")
            }
        }
        if (!state.loading && state.awards.isEmpty())
            SemanticCard(Tile.Insight) {
                Text("No eligible milestones recorded yet.")
                Text(
                    "A completed workout with a working set, or a recorded run, is enough to begin. Historical training can qualify; no maximal test is needed."
                )
            }
        state.awards.forEach { stored ->
            val award = stored.award()
            var proof by rememberSaveable(award.id()) { mutableStateOf(false) }
            SemanticCard(Tile.Progress) {
                Text("✓ ${award.kind().title()}", style = MaterialTheme.typography.titleLarge)
                Text(award.kind().explanation())
                Text(
                    "Recognised ${friendlyDate(stored.recognizedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate())}",
                    style = MaterialTheme.typography.labelMedium,
                )
                TextButton(
                    onClick = { proof = !proof },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (proof) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(if (proof) "Hide milestone evidence" else "View milestone evidence")
                }
                if (proof) {
                    Text(
                        "Retained recognition from ${award.evidence().size} recorded sources. Later imports do not award it again or silently redate it."
                    )
                    award.evidence().forEach { evidence ->
                        HorizontalDivider()
                        val known =
                            (evidence.source() to evidence.recordId()) in state.availableSources
                        val date =
                            evidence.offset()?.let {
                                friendlyDate(evidence.at().atOffset(it).toLocalDate())
                            } ?: "${evidence.at()} · local time zone unavailable"
                        Text("${evidence.source().name.lowercase()} · $date")
                        if (known)
                            TextButton(
                                onClick = { onSource(evidence) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    if (
                                        evidence.source() ==
                                            org.racedaydreams.domain.achievements.TrainingMilestones
                                                .Source
                                                .RUN
                                    )
                                        "Open recorded run"
                                    else "Open recorded workout"
                                )
                            }
                        if (!known)
                            Text(
                                "Retained source reference; this source is not currently available on this device."
                            )
                    }
                    Text(
                        "Deleting source records may leave a retained recognition reference. This is evidence from recognition time, not a claim that deleted details can be restored.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
