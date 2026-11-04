package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** Evidence is reviewed again on save; no prescribed task is ever an actual set. */
@Composable
internal fun ProgrammeProgressionSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val review = state.progressionReview ?: return
    val enabled = !state.busy
    AlertDialog(
        onDismissRequest = { if (enabled) model.closeProgression() },
        title = { Text("Optional progression") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(review.slot.exercise().name(), style = MaterialTheme.typography.titleLarge)
                Text("Current target: ${gymTargetDescription(review.slot)}")
                Text(review.advice.reason())
                Text(
                    "Reviewed up to 20 recent logged sessions; only recent exact-exercise working sets with comparable task and recorded effort support an increase. Missing effort remains unknown."
                )
                Text(
                    "${review.advice.supportingSessionIds().size} supporting exact-exercise sessions"
                )
                review.advice.proposedTarget()?.let { target ->
                    SemanticCard(Tile.Planned) {
                        Text("Optional proposal", style = MaterialTheme.typography.titleMedium)
                        Text(
                            gymTargetDescription(
                                GymWorkoutDefinition.Slot(
                                    review.slot.id(),
                                    review.slot.exercise(),
                                    target,
                                    review.slot.supersetGroup(),
                                )
                            )
                        )
                        Text(
                            "Only this dated target changes in the saved preview. Your active programme, entered sets and other weeks remain unchanged until explicit activation."
                        )
                    }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (review.advice.proposedTarget() != null)
                TextButton(onClick = model::saveProgressionProposal, enabled = enabled) {
                    Text("Save optional target proposal")
                }
        },
        dismissButton = {
            TextButton(onClick = model::closeProgression, enabled = enabled) {
                Text("Keep current target")
            }
        },
    )
}
