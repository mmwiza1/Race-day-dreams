package org.racedaydreams.coach.strength

import org.racedaydreams.domain.strength.*

/** A prepared, bounded today-only choice. None of these capabilities change saved gym settings. */
data class GymSwapState(
    val executionId: String,
    val slotId: String,
    val original: StrengthExercise,
    val goal: GymProgramme.Goal,
    val experience: GymProgramme.Experience,
    val equipment: Set<ExerciseMetadata.Equipment>,
    val contextExplanation: String,
    val reason: ExerciseSubstitutionEngine.Reason,
    val suggestions: List<ExerciseSubstitutionEngine.Suggestion>,
    val visibleLimit: Int = 4,
)
