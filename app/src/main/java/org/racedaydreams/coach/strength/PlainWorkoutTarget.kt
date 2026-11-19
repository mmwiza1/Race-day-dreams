package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** Original exact target formatter remains the Android presentation binding. */
@Composable
internal fun PlainWorkoutTarget(slot: GymWorkoutDefinition.Slot) =
    SharedPlainWorkoutTarget(slot, ::gymTargetDescription)
