package org.racedaydreams.coach.strength

import org.racedaydreams.domain.strength.*

data class ProgrammeProgressionReview(
    val draftId: String,
    val draftRevision: Int,
    val workoutId: String,
    val slot: GymWorkoutDefinition.Slot,
    val advice: StrengthTargetProgression.Advice,
    val supportingWorkouts: List<StrengthSession> = emptyList(),
)
