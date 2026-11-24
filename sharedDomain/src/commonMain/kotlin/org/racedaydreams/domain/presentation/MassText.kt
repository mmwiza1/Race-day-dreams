package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import kotlin.math.round
import org.racedaydreams.domain.body.MassUnit

/**
 * Display and entry of loads in the user's mass unit. Storage stays in kilograms at full precision:
 * a load typed in pounds is converted exactly and is never rounded before it is saved, so 225 lb is
 * stored as 102.05828325 kg and shown again as 225 lb.
 */
object MassText {
    /** "kg" or "lb". */
    @JvmStatic fun symbol(unit: MassUnit): String = unit.name.lowercase()

    /** A stored kilogram value in [unit], rounded to 0.01 for display. */
    @JvmStatic
    fun value(kilograms: Double, unit: MassUnit): Double =
        round(unit.fromKilograms(kilograms) * 100) / 100

    /** A stored kilogram value as plain display text in [unit], without trailing zeros. */
    @JvmStatic
    fun number(kilograms: Double, unit: MassUnit): String =
        prescriptionNumber(value(kilograms, unit))

    /** A stored kilogram value with its unit, such as "225 lb". */
    @JvmStatic
    fun label(kilograms: Double, unit: MassUnit): String =
        "${number(kilograms, unit)} ${symbol(unit)}"

    /** The text to prefill an entry field with for a stored kilogram value. */
    @JvmStatic fun entry(kilograms: Double, unit: MassUnit): String = number(kilograms, unit)

    /** Typed text in [unit] converted to kilograms for storage, or an error naming the field. */
    @JvmStatic
    fun parse(text: String, unit: MassUnit, label: String = "load"): Double {
        val typed =
            text.trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("Enter $label as a number.")
        require(typed.isFinite() && typed >= 0) { "Enter $label as a non-negative number." }
        return unit.toKilograms(typed)
    }

    /**
     * The smallest change a typical pair of plates makes: 2.5 kg (2 × 1.25 kg) or 5 lb (2 × 2.5
     * lb).
     */
    @JvmStatic fun plateStep(unit: MassUnit): Double = if (unit == MassUnit.LB) 5.0 else 2.5

    /**
     * A suggested warm-up load (stored kg) rounded down to a loadable step in [unit] (audit B-R8).
     * Rounding down keeps a practice set lighter than suggested, never heavier, and never below one
     * step. Returned in [unit].
     */
    @JvmStatic
    fun warmup(kilograms: Double, unit: MassUnit): Double {
        val step = plateStep(unit)
        return maxOf(step, floor(unit.fromKilograms(kilograms) / step + 1e-9) * step)
    }

    /**
     * A record category such as "Duration seconds at 20.0 kg" with its load shown in [unit]. The
     * category itself is an identity key and is never changed in storage.
     */
    @JvmStatic
    fun category(category: String, unit: MassUnit): String {
        if (unit == MassUnit.KG) return category
        val match = Regex(""" at ([0-9]+(?:\.[0-9]+)?) kg$""").find(category) ?: return category
        val kilograms = match.groupValues[1].toDouble()
        return category.substring(0, match.range.first) + " at ${label(kilograms, unit)}"
    }
}
