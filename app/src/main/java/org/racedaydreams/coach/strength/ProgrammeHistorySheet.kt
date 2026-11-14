package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.friendlyDate

/** Android lifecycle/window boundary; the real retained-version screen is shared Compose. */
@Composable
internal fun ProgrammeHistorySheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    if (!state.historyOpen) return
    Dialog(
        onDismissRequest = { if (!state.busy) model.closeHistory() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SharedProgrammeHistoryContent(
            state =
                ProgrammeHistoryReview(
                    state.historicalProgramme,
                    state.history,
                    state.historyPage,
                    state.historyMore,
                    state.busy,
                    state.error,
                ),
            closeHistory = model::closeHistory,
            reviewHistorical = model::reviewHistorical,
            changePage = model::historyPage,
            historicalList = model::historicalList,
            formatDate = ::friendlyDate,
            formatTarget = ::gymTargetDescription,
        )
    }
}
