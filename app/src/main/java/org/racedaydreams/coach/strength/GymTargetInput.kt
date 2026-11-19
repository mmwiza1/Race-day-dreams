package org.racedaydreams.coach.strength

import java.util.UUID
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise

/** Original Android API and UUID source; the single parser and model are shared. */
internal fun gymSlotFromInput(
    slotId: String?,
    exercise: StrengthExercise,
    input: GymSlotInput,
): GymWorkoutDefinition.Slot =
    gymSlotFromInput(slotId, exercise, input) { UUID.randomUUID().toString() }
