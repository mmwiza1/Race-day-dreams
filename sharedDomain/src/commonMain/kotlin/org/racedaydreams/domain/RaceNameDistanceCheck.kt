package org.racedaydreams.domain

import kotlin.jvm.JvmStatic

/**
 * Flags a race name that clearly names a different distance from the one selected, such as "Hackney
 * Half" with 10K selected. Only a warning: the selected distance is never changed from free text,
 * and names without a clear distance word produce no warning.
 */
object RaceNameDistanceCheck {
    private val half = Regex("""\b(half|hm|13\.1|21(\.1)?\s*k(m)?)\b""")
    private val marathon = Regex("""\b(marathon|26\.2|42(\.2)?\s*k(m)?)\b""")
    private val tenK = Regex("""\b10\s*k(m)?\b""")
    private val fiveK = Regex("""\b(5\s*k(m)?|parkrun)\b""")

    /** The distance a race name explicitly names, or null when it names none or several. */
    @JvmStatic
    fun named(raceName: String?): RaceDistance? {
        val name = raceName?.lowercase()?.trim().orEmpty()
        if (name.isEmpty()) return null
        val found = buildSet {
            when {
                half.containsMatchIn(name) -> add(RaceDistance.HALF_MARATHON)
                marathon.containsMatchIn(name) -> add(RaceDistance.MARATHON)
            }
            if (tenK.containsMatchIn(name)) add(RaceDistance.TEN_K)
            if (fiveK.containsMatchIn(name)) add(RaceDistance.FIVE_K)
        }
        return found.singleOrNull()
    }

    /** The distance the name suggests when it differs from [selected]; null when consistent. */
    @JvmStatic
    fun conflict(raceName: String?, selected: RaceDistance): RaceDistance? =
        named(raceName)?.takeIf { it != selected && selected != RaceDistance.GENERAL_FITNESS }
}
