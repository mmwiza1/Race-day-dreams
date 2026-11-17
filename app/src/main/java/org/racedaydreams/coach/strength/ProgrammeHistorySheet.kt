package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.friendlyDate

@Composable
internal fun ProgrammeHistorySheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedProgrammeHistoryDialog(model.shared, state, ::friendlyDate, ::gymTargetDescription)
}
