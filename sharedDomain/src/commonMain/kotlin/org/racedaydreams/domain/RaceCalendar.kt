package org.racedaydreams.domain

import org.racedaydreams.domain.time.LocalDate

/** Original calendar-day horizon, explicitly supplied local today; never elapsed UTC hours. */
object RaceCalendar {
    fun daysUntil(date: LocalDate, today: LocalDate): Long =
        maxOf(0, date.toEpochDay() - today.toEpochDay())

    fun weeksUntil(date: LocalDate, today: LocalDate): Long = daysUntil(date, today) / 7
}
