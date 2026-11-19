package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.LocalDate

/** UK short-month presentation; calculations and stored date identities remain unchanged. */
internal actual fun formatGymProgrammeDate(day: LocalDate): String {
    val month =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")[
            day.getMonthValue() - 1]
    val y = day.getYear()
    val year =
        when {
            y < 0 -> "-" + (-y).toString().padStart(4, '0')
            y > 9999 -> "+$y"
            else -> y.toString().padStart(4, '0')
        }
    return "${day.getDayOfMonth()} $month $year"
}
