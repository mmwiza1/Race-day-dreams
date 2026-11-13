package org.racedaydreams.coach.strength

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.racedaydreams.coach.WhyDetails
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.GymWorkoutDefinition

/** The existing structured text formatter, with the exact unchanged target available on demand. */
@Composable
internal fun PlainWorkoutTarget(slot: GymWorkoutDefinition.Slot) {
    StrengthPrescriptionText.instructions(slot).forEach { Text(it) }
    WhyDetails(
        if (slot.exercise().measurement().usesLoad()) "Training targets and weight reference"
        else "Exact training targets"
    ) {
        Text(gymTargetDescription(slot))
        StrengthPrescriptionText.weight(slot, null).forEach { Text(it) }
        slot.target()?.effort()?.let { Text(StrengthPrescriptionText.effortDetails(it)) }
    }
}
