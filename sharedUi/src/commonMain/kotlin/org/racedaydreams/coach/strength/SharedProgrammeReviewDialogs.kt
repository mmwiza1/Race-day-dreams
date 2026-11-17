package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedProgrammeProgressionDialog(
    model: SharedStrengthGenerationCoordinator,
    state: StrengthGenerationState,
    formatDate: (LocalDate) -> String,
) {
    val review = state.progressionReview ?: return
    Dialog(
        onDismissRequest = { if (!state.busy) model.closeProgression() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SharedProgrammeProgressionContent(
            review,
            state.busy,
            state.error,
            model::closeProgression,
            model::saveProgressionProposal,
            formatDate,
        )
    }
}

@Composable
fun SharedProgrammeHistoryDialog(
    model: SharedStrengthGenerationCoordinator,
    state: StrengthGenerationState,
    formatDate: (LocalDate) -> String,
    formatTarget: (GymWorkoutDefinition.Slot) -> String,
) {
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
            formatDate = formatDate,
            formatTarget = formatTarget,
        )
    }
}
