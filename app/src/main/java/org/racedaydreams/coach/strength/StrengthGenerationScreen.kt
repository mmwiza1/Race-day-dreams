package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.util.UUID

/** Original lifecycle, OS date/identity and back effects for the shared template/revision UI. */
@Composable
fun StrengthGenerationScreen(
    model: StrengthGenerationViewModel,
    onBack: () -> Unit,
    onActivated: () -> Unit,
    initialDraftId: String? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedStrengthGenerationScreen(
        model.shared,
        state,
        programmePresentation(),
        LocalDate::now,
        { UUID.randomUUID().toString() },
        { action -> BackHandler(onBack = action) },
        onBack,
        onActivated,
        initialDraftId,
    )
}
