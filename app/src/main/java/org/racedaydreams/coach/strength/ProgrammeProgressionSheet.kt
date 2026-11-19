package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.friendlyDate

/** Same optional proposal; saving and activation remain separate. */
@Composable
internal fun ProgrammeProgressionSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedProgrammeProgressionDialog(model.shared, state, ::friendlyDate)
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
