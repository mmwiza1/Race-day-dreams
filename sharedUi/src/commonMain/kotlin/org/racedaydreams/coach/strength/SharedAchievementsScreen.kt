package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.achievements.TrainingMilestones

/** Prepared recognition date is supplied by the platform; actual proof remains intact. */
data class SharedRecognizedMilestone(
    val award: TrainingMilestones.Award,
    val recognizedDate: String,
)

/** Existing production milestone UI, independent of Android repositories and lifecycle. */
@Composable
fun SharedAchievementsScreen(
    loading: Boolean,
    awards: List<SharedRecognizedMilestone>,
    celebrations: Boolean,
    availableSources: Set<Pair<TrainingMilestones.Source, String>>,
    notice: String?,
    error: String?,
    sourceDateLabel: (TrainingMilestones.Proof) -> String,
    onSource: (TrainingMilestones.Proof) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: (Boolean?) -> Unit,
) {
    CoachPage {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        CoachHeading("Recorded milestones")
        Text(
            "Recognise your training without streaks, penalties or pressure to train through a break."
        )
        if (loading) CircularProgressIndicator()
        error?.let { SemanticCard(Tile.Warning) { Text(it) } }
        notice?.let {
            SemanticCard(Tile.Progress) {
                Text(it)
                TextButton(onClick = onDismiss) { Text("Dismiss celebration") }
            }
        }
        if (!loading && awards.isEmpty())
            SemanticCard(Tile.Insight) {
                Text("No eligible milestones recorded yet.")
                Text(
                    "A completed workout with a working set, or a recorded run, is enough to begin. Historical training can qualify; no maximal test is needed."
                )
            }
        awards.forEach { stored ->
            val award = stored.award
            var proof by rememberSaveable(award.id()) { mutableStateOf(false) }
            SemanticCard(Tile.Progress) {
                Text("✓ ${award.kind().title()}", style = MaterialTheme.typography.titleLarge)
                Text(
                    when (award.kind()) {
                        TrainingMilestones.Kind.FIRST_GYM ->
                            "Your first completed workout with a recorded working set."
                        TrainingMilestones.Kind.FIVE_GYM ->
                            "Five completed strength workouts in your recorded history."
                        TrainingMilestones.Kind.FIFTY_GYM ->
                            "Fifty completed strength workouts in your recorded history."
                        TrainingMilestones.Kind.FIRST_RUN ->
                            "Your first completed run in your recorded history."
                        TrainingMilestones.Kind.TEN_RUNS ->
                            "Ten completed runs in your recorded history."
                        TrainingMilestones.Kind.FIRST_FIVE_KM_RUN ->
                            "One completed run has at least 5 km of recorded distance."
                        TrainingMilestones.Kind.TWELVE_TRAINING_WEEKS ->
                            "Training recorded in twelve different weeks. The weeks do not need to be consecutive."
                    }
                )
                Text(
                    "Recognised ${stored.recognizedDate}",
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
                    Text(award.kind().explanation())
                    Text(
                        "Retained recognition from ${award.evidence().size} recorded sources. Later imports do not award it again or silently redate it."
                    )
                    award.evidence().forEach { evidence ->
                        HorizontalDivider()
                        val known = (evidence.source() to evidence.recordId()) in availableSources
                        val date = sourceDateLabel(evidence)
                        Text("${evidence.source().name.lowercase()} · $date")
                        if (known)
                            TextButton(
                                onClick = { onSource(evidence) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    if (evidence.source() == TrainingMilestones.Source.RUN)
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
        SemanticCard(Tile.Neutral) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Checkbox(
                    checked = celebrations,
                    onCheckedChange = { onRefresh(it) },
                    enabled = !loading,
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
            TextButton(onClick = { onRefresh(null) }, enabled = !loading) {
                Text("Refresh recorded evidence")
            }
        }
    }
}
