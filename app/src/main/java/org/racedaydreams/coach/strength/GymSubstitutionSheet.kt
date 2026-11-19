package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Platform lifecycle adapter; shared production UI renders the existing acknowledged flow. */
@Composable
fun GymSubstitutionSheet(model: GymNowViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val swap = state.substitution ?: return
    SharedGymSubstitutionSheet(
        swap = swap,
        busy = state.busy,
        error = state.error,
        enabled = !state.busy && state.view?.pending() == null,
        onClose = model::closeSubstitutions,
        onReasonChange = model::changeSubstitutionReason,
        onEquipmentChange = model::changeSubstitutionEquipment,
        onShowMore = model::showMoreSubstitutions,
        onReplace = model::replaceForToday,
        exerciseContext = ::gymExerciseContext,
    )
}
