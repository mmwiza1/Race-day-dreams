package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Android lifecycle adapter; same typed custom definitions and explicit saves. */
@Composable
fun CustomStrengthExerciseScreen(
    model: StrengthProgrammeViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedCustomStrengthExerciseScreen(model.shared, state, onBack, onSaved, appBarBack = true)
}
