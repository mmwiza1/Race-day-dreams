package org.racedaydreams.domain.strength

import java.util.Locale
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.body.MassUnit

/** JVM record/locale adapter; shared input and duration rules never alter retained evidence. */
object StrengthMeasurements {
    @JvmStatic fun seconds(input: String): Long = StrengthMeasurementPolicy.seconds(input)

    @JvmStatic fun duration(seconds: Long): String = StrengthMeasurementPolicy.duration(seconds)

    @JvmStatic
    fun display(set: StrengthSession.SetEntry, mass: MassUnit, units: UnitSystem): String {
        val load =
            String.format(
                Locale.UK,
                "%.1f %s × ",
                mass.fromKilograms(set.kilograms()),
                mass.name.lowercase(Locale.ROOT),
            )
        val prefix = if (set.kilograms() > 0) load else ""
        val duration = set.durationSeconds()
        if (duration != null) return prefix + duration(duration)
        val distance = set.distanceMetres()
        if (distance != null)
            return prefix +
                String.format(
                    Locale.UK,
                    "%.1f %s",
                    if (units == UnitSystem.IMPERIAL) distance / 0.9144 else distance,
                    if (units == UnitSystem.IMPERIAL) "yd" else "m",
                )
        return prefix + set.reps() + " reps"
    }
}
