package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun ProgrammeSwapSheet(model: StrengthGenerationViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedProgrammeSwapDialog(model.shared, state, programmePresentation())
}
