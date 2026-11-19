package org.racedaydreams.storage

import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.LocalDate

/** Canonical ISO date emitted by the existing Android codecs, including extended years. */
internal object StoredDate {
    fun parse(text: String): LocalDate {
        val parts =
            Regex("([+-]?[0-9]{4,9})-([0-9]{2})-([0-9]{2})").matchEntire(text)?.groupValues
                ?: throw IllegalArgumentException(
                    "Invalid stored calendar date; data was retained."
                )
        val date = CalendarDates.of(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
        require(date.toString() == text)
        return date
    }
}
