package org.racedaydreams.coach.strength

import org.racedaydreams.domain.recovery.TrainingRecovery
import org.racedaydreams.domain.strength.StrengthProgramme
import org.racedaydreams.domain.time.LocalDate

/** Existing prepared recovery state; no platform service or activation authority. */
data class TrainingRecoveryState(
    val busy: Boolean = true,
    val review: TrainingRecovery.Review? = null,
    val programme: StrengthProgramme? = null,
    val conflicts: List<TrainingRecovery.Conflict> = emptyList(),
    val alternatives: Map<String, List<LocalDate>> = emptyMap(),
    val canReviewLighterWeek: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)
