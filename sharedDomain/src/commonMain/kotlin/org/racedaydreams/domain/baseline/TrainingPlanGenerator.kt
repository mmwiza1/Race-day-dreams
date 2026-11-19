package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.TrainingPlan
import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.time.LocalDate

/** Deterministic plan boundary; production implementation remains in domain.coaching. */
fun interface TrainingPlanGenerator {
    fun generate(profile: UserProfile, baseline: RunnerBaseline, startDate: LocalDate): TrainingPlan
}
