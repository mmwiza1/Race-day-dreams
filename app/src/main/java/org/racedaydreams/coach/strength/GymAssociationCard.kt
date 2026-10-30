package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.domain.strength.GymAssociationCoordinator
import org.racedaydreams.domain.strength.GymWorkoutAssociation

/** The actual and exact reviewed prescription stay separate; there is no automatic completion. */
@Composable
fun GymAssociationCard(
    view: GymAssociationCoordinator.View?,
    enabled: Boolean,
    onChoose: (GymWorkoutAssociation.Proposal, GymWorkoutAssociation.Choice) -> Unit,
    onRefresh: () -> Unit,
) {
    if (view == null || view.actualId() == null) return
    var confirmation by
        remember(view.actualId()) {
            mutableStateOf<Pair<GymWorkoutAssociation.Proposal, GymWorkoutAssociation.Choice>?>(
                null
            )
        }
    val current = view.current()
    val counted = current?.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
    if (view.proposals().isEmpty() && current == null) return
    SemanticCard(Tile.Insight) {
        Text(
            if (counted) "Your programme association" else "Keep extra or associate?",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "A spontaneous workout is extra unless you explicitly choose a programme association. This does not change actual sets, the original prescription or the workout's origin."
        )
        if (current != null) {
            Text(
                if (counted)
                    "Counted toward ${current.proposal().title()} · ${associationDate(current.proposal())}"
                else "Kept as extra workout",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Reviewed programme version ${current.proposal().programmeRevision()}. Association is separate from whether every prescribed set was performed."
            )
        }
        if (counted)
            view.currentReview()?.let { review ->
                OutlinedButton(
                    onClick = { confirmation = review to GymWorkoutAssociation.Choice.KEEP_EXTRA },
                    enabled = enabled,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("gym-association-unlink"),
                ) {
                    Text("Keep as extra instead")
                }
            }
        view.proposals().forEach { proposal ->
            Text(
                "${proposal.title()} · ${associationDate(proposal)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Programme version ${proposal.programmeRevision()} · ${proposal.prescribedExerciseCount()} prescribed exercises"
            )
            Button(
                onClick = {
                    confirmation = proposal to GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
                },
                enabled = enabled,
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("gym-association-count-${proposal.sessionId()}"),
            ) {
                Text("Review association")
            }
        }
        if (current == null)
            view.proposals().firstOrNull()?.let { proposal ->
                OutlinedButton(
                    onClick = {
                        confirmation = proposal to GymWorkoutAssociation.Choice.KEEP_EXTRA
                    },
                    enabled = enabled,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("gym-association-keep"),
                ) {
                    Text("Keep as extra workout")
                }
            }
        TextButton(
            onClick = onRefresh,
            enabled = enabled,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Refresh evidence and choices")
        }
    }
    confirmation?.let { (proposal, choice) ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = {
                Text(
                    if (choice == GymWorkoutAssociation.Choice.KEEP_EXTRA) "Keep as extra workout?"
                    else "Count toward this session?"
                )
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Actual workout · ${proposal.actual().startedAt().atOffset(proposal.actual().offset()).toLocalDate().format(ASSOCIATION_DAY)}"
                    )
                    proposal.actual().exercises().forEach { actual ->
                        Text(
                            "${actual.exercise().name()} · ${actual.sets().count { !it.warmUp() }} working sets"
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "Preserved prescription · ${proposal.title()} · ${associationDate(proposal)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Programme version ${proposal.programmeRevision()}")
                    if (proposal.authored()) {
                        proposal.authoredPrescription().definition().slots().forEach { slot ->
                            Text("${slot.exercise().name()} · ${gymTargetDescription(slot)}")
                        }
                    } else {
                        proposal.prescription().exercises().forEach { target ->
                            Text("${target.exerciseId().replace('-',' ')} · ${target.sets()} sets")
                        }
                    }
                    Text(
                        if (choice == GymWorkoutAssociation.Choice.KEEP_EXTRA)
                            "This workout remains training evidence without satisfying the programme session."
                        else
                            "Only this explicitly reviewed association changes. Actual evidence and the original target stay intact. This is not a claim that all target sets were completed."
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmation = null
                        onChoose(proposal, choice)
                    },
                    enabled = enabled,
                    modifier = Modifier.testTag("gym-association-confirm"),
                ) {
                    Text(
                        if (choice == GymWorkoutAssociation.Choice.KEEP_EXTRA) "Keep as extra"
                        else "Count toward session"
                    )
                }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } },
        )
    }
}

private val ASSOCIATION_DAY = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)

private fun associationDate(proposal: GymWorkoutAssociation.Proposal) =
    proposal.date().format(ASSOCIATION_DAY)
