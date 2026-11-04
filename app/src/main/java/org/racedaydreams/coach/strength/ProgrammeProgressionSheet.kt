package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** Review the optional target before evidence; flowing controls remain usable at 200% text. */
@Composable
internal fun ProgrammeProgressionSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val review = state.progressionReview ?: return
    val enabled = !state.busy
    Dialog(
        onDismissRequest = { if (enabled) model.closeProgression() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TextButton(onClick = model::closeProgression, enabled = enabled) {
                        Text("Keep current target")
                    }
                    Text(
                        "Optional progression",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(review.slot.exercise().name(), style = MaterialTheme.typography.titleLarge)
                }
                val target = review.advice.proposedTarget()
                if (target != null)
                    item {
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
                            Text("A saved proposal for this date. Activation is a separate choice.")
                        }
                    }
                item {
                    Text("Current target: ${gymTargetDescription(review.slot)}")
                    if (target == null) Text(review.advice.reason())
                    else WhyDetails(review.advice.reason(), "Why this proposal?")
                    Text(
                        "${review.advice.supportingSessionIds().size} supporting exact-exercise sessions"
                    )
                    WhyDetails(
                        "Reviewed up to 20 recent logged sessions. Only recent exact-exercise working sets with a comparable task and intended recorded effort support an increase. Missing effort remains unknown. Saving rechecks evidence. Your active programme, entered sets and other weeks do not change when saving this proposal.",
                        "Evidence & safeguards",
                    )
                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (target != null)
                    item {
                        Button(
                            onClick = model::saveProgressionProposal,
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save optional target proposal")
                        }
                    }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
