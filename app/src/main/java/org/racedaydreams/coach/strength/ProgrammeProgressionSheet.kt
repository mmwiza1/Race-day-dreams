package org.racedaydreams.coach.strength

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.*
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** Explain the task first; review remains separate from saving and programme activation. */
@Composable
internal fun ProgrammeProgressionSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val review = state.progressionReview ?: return
    Dialog(
        onDismissRequest = { if (!state.busy) model.closeProgression() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProgrammeProgressionContent(
            review,
            state.busy,
            state.error,
            model::closeProgression,
            model::saveProgressionProposal,
        )
    }
}

@Composable
internal fun ProgrammeProgressionContent(
    review: ProgrammeProgressionReview,
    busy: Boolean,
    error: String?,
    keepCurrent: () -> Unit,
    saveSuggestion: () -> Unit,
) {
    var advanced by
        rememberSaveable(review.draftId, review.draftRevision, review.slot.id()) {
            mutableStateOf(false)
        }
    val target = review.advice.proposedTarget()
    val suggestedSlot =
        if (target == null) review.slot
        else
            GymWorkoutDefinition.Slot(
                review.slot.id(),
                review.slot.exercise(),
                target,
                review.slot.supersetGroup(),
            )
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("progression-review"),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TextButton(
                    onClick = keepCurrent,
                    enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Keep my current target")
                }
                Text(
                    "Your workout suggestion",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    review.slot.exercise().name(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "Here's how we'd suggest approaching your next workout.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                SemanticCard(Tile.Planned) {
                    Text(
                        "Here's what to do",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (
                        review.advice.action() ==
                            org.racedaydreams.domain.strength.StrengthProgressionAdvisor.Action
                                .REDUCE_OR_SIMPLIFY
                    ) {
                        Text(review.advice.reason())
                        Text(
                            "Review the current target in Advanced details before choosing your next task."
                        )
                    } else {
                        StrengthPrescriptionText.instructions(suggestedSlot).forEach { Text(it) }
                        if (target == null)
                            Text("Your current target is shown; no increase is suggested.")
                    }
                }
            }
            item {
                SemanticCard(Tile.Progress) {
                    Text(
                        "About the weight",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    StrengthPrescriptionText.weight(review.slot, target).forEach { Text(it) }
                }
            }
            item {
                SemanticCard(Tile.Insight) {
                    Text(
                        "Why are we suggesting this?",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (target != null) {
                        val count = review.advice.supportingSessionIds().size
                        Text(
                            "We've reviewed $count comparable ${review.slot.exercise().name()} workouts you've recorded. Your recorded working sets and effort support this optional change."
                        )
                    } else {
                        Text("Your recent workouts do not yet support an increase for this target.")
                        Text(review.advice.reason())
                    }
                }
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable(
                                role = Role.Button,
                                onClickLabel =
                                    if (advanced) "Collapse advanced details"
                                    else "Expand advanced details",
                            ) {
                                advanced = !advanced
                            }
                            .semantics {
                                stateDescription = if (advanced) "Expanded" else "Collapsed"
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Advanced details", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Training targets and supporting evidence",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            if (advanced) "⌃" else "⌄",
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                    }
                }
            }
            if (advanced) {
                item {
                    SemanticCard(Tile.Neutral) {
                        Text("Current target", style = MaterialTheme.typography.titleMedium)
                        ProgressionTargetDetails(review.slot.target())
                        if (target != null) {
                            Text("Proposed target", style = MaterialTheme.typography.titleMedium)
                            ProgressionTargetDetails(target)
                        }
                        Text(
                            "How this suggestion was calculated",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(review.advice.reason())
                    }
                }
                item {
                    SemanticCard(Tile.Neutral) {
                        Text("Supporting workouts", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${review.advice.supportingSessionIds().size} comparable recorded workouts considered."
                        )
                        review.supportingWorkouts.forEach { workout ->
                            Text(
                                friendlyDate(
                                    workout.startedAt().atOffset(workout.offset()).toLocalDate()
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            workout
                                .exercises()
                                .filter { block ->
                                    block.exercise().id() == review.slot.exercise().id() &&
                                        block.exercise().mode() == review.slot.exercise().mode() &&
                                        block.exercise().measurement() ==
                                            review.slot.exercise().measurement()
                                }
                                .forEach { block ->
                                    block
                                        .sets()
                                        .filterNot { it.warmUp() }
                                        .forEach { set ->
                                            val task =
                                                when {
                                                    set.durationSeconds() != null ->
                                                        StrengthPrescriptionText.duration(
                                                            set.durationSeconds()
                                                        )
                                                    set.distanceMetres() != null ->
                                                        "${StrengthPrescriptionText.number(set.distanceMetres())} metres"
                                                    else -> "${set.reps()} repetitions"
                                                }
                                            val load =
                                                if (block.exercise().measurement().usesLoad())
                                                    "${StrengthPrescriptionText.number(set.kilograms())} kg recorded · "
                                                else ""
                                            Text(load + task)
                                            Text(
                                                when {
                                                    set.rpe() != null ->
                                                        "Recorded perceived effort (RPE): ${StrengthPrescriptionText.number(set.rpe())}/10"
                                                    set.repetitionsInReserve() != null ->
                                                        "Recorded repetitions left before failure (RIR): ${StrengthPrescriptionText.number(set.repetitionsInReserve())}"
                                                    else -> "Effort was not recorded."
                                                }
                                            )
                                        }
                                }
                        }
                        Text(
                            "Only recent working sets for this exact exercise and a comparable task can support an increase. Warm-up sets do not count. Missing effort remains unknown. Saving checks the evidence again; a stale suggestion cannot be saved."
                        )
                    }
                }
            }
            item {
                Text(
                    "Saving this suggestion won't change your active programme. You can review and activate it separately."
                )
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (target != null)
                item {
                    Button(
                        onClick = saveSuggestion,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text("Save this suggestion for review")
                    }
                }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ProgressionTargetDetails(target: GymWorkoutDefinition.Target?) {
    if (target == null) {
        Text("No structured target is specified.")
        return
    }
    Text("Sets: ${target.sets()}")
    target.minimumReps()?.let { minimum ->
        val repetitions =
            if (minimum == target.maximumReps()) "$minimum" else "$minimum–${target.maximumReps()}"
        Text("Repetitions: $repetitions")
    }
    target.seconds()?.let { Text("Time per set: ${StrengthPrescriptionText.duration(it)}") }
    target.metres()?.let { Text("Distance per set: ${StrengthPrescriptionText.number(it)} metres") }
    Text(StrengthPrescriptionText.effortDetails(target.effort()))
    target.restSeconds()?.let { Text("Rest: ${StrengthPrescriptionText.duration(it.toLong())}") }
        ?: Text("Rest is not specified.")
    target.referenceLoadKg()?.let {
        Text(
            "Optional weight reference: ${StrengthPrescriptionText.number(it)} kg; this is not an actual logged weight."
        )
    } ?: Text("No weight reference is specified.")
}
