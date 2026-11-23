package org.racedaydreams.coach

import org.racedaydreams.domain.UnitSystem

/**
 * Display-only distance and pace conversion for the user's unit setting. Storage stays in
 * kilometres, metres and seconds per kilometre; these helpers only change what is shown.
 */
object DisplayUnits {
    const val METRES_PER_MILE = 1609.344

    fun imperial(state: CoachUiState): Boolean =
        state.profile?.preferences()?.units() == UnitSystem.IMPERIAL

    /** "mi" or "km". */
    fun distanceUnit(imperial: Boolean): String = if (imperial) "mi" else "km"

    /** A distance stored in kilometres, in the display unit. */
    fun distance(kilometres: Double, imperial: Boolean): Double =
        if (imperial) kilometres * 1000 / METRES_PER_MILE else kilometres

    /** A pace stored in seconds per kilometre, in seconds per display unit. */
    fun pace(secondsPerKilometre: Double, imperial: Boolean): Double =
        if (imperial) secondsPerKilometre * METRES_PER_MILE / 1000 else secondsPerKilometre
}
