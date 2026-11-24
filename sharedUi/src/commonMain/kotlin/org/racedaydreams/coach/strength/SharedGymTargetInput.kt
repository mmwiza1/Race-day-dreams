package org.racedaydreams.coach.strength

import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.presentation.MassText
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise

data class GymSlotInput(
    val open: Boolean = false,
    val sets: String = "3",
    val minimumReps: String = "",
    val maximumReps: String = "",
    val seconds: String = "",
    val metres: String = "",
    val minimumEffort: String = "",
    val maximumEffort: String = "",
    val rir: Boolean = false,
    val restSeconds: String = "",
    val referenceKilograms: String = "",
)

/**
 * A reference load typed in [unit], as the kilogram text [gymSlotFromInput] parses (audit B-05).
 * Kilogram entries and anything that is not a number pass through unchanged, so the existing
 * validation message still names the field.
 */
fun referenceKilogramsText(typed: String, unit: MassUnit): String {
    if (unit == MassUnit.KG) return typed
    val value = typed.trim().toDoubleOrNull() ?: return typed
    if (!value.isFinite() || value < 0) return typed
    return unit.toKilograms(value).toString()
}

/** A set load typed in [unit] converted to kilograms for storage (audit B-05). */
fun gymLoadKilograms(typed: String, unit: MassUnit): Double = MassText.parse(typed, unit, "load")

/** One parser for programme and one-off intentions; actual set entry remains separate. */
fun gymSlotFromInput(
    slotId: String?,
    exercise: StrengthExercise,
    input: GymSlotInput,
    newId: () -> String,
): GymWorkoutDefinition.Slot {
    val target =
        if (input.open) null
        else {
            val profile = exercise.measurement()
            val lower = optionalNumber(input.minimumEffort, "minimum effort")
            val upper = optionalNumber(input.maximumEffort, "maximum effort")
            require((lower == null) == (upper == null)) {
                "Complete both effort bounds, or leave both blank."
            }
            GymWorkoutDefinition.Target(
                whole(input.sets, "working sets"),
                if (profile.usesReps()) whole(input.minimumReps, "minimum repetitions") else null,
                if (profile.usesReps()) whole(input.maximumReps, "maximum repetitions") else null,
                if (profile.usesDuration()) wholeLong(input.seconds, "duration in seconds")
                else null,
                if (profile.usesDistance()) number(input.metres, "distance in metres") else null,
                if (lower == null) null
                else
                    GymWorkoutDefinition.EffortRange(
                        if (input.rir) GymWorkoutDefinition.EffortScale.RIR
                        else GymWorkoutDefinition.EffortScale.RPE,
                        lower,
                        upper!!,
                    ),
                optionalWhole(input.restSeconds, "rest in seconds"),
                if (profile.usesLoad() && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT)
                    optionalNumber(input.referenceKilograms, "reference load")
                else null,
            )
        }
    return GymWorkoutDefinition.Slot(slotId ?: newId(), exercise, target, null)
}

private fun number(value: String, label: String): Double =
    value.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        ?: throw IllegalArgumentException("Enter a valid $label.")

private fun whole(value: String, label: String): Int =
    value.trim().toIntOrNull() ?: throw IllegalArgumentException("Enter a whole number for $label.")

private fun wholeLong(value: String, label: String): Long =
    value.trim().toLongOrNull()
        ?: throw IllegalArgumentException("Enter a whole number for $label.")

private fun optionalNumber(value: String, label: String): Double? =
    if (value.isBlank()) null else number(value, label)

private fun optionalWhole(value: String, label: String): Int? =
    if (value.isBlank()) null else whole(value, label)
