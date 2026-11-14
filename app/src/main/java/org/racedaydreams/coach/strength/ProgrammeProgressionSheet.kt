package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.friendlyDate

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
) =
    SharedProgrammeProgressionContent(
        review,
        busy,
        error,
        keepCurrent,
        saveSuggestion,
        ::friendlyDate,
    )
