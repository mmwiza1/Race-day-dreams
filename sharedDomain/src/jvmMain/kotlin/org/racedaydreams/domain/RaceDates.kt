package org.racedaydreams.domain

import java.time.Clock
import java.time.LocalDate

/** Existing caller-selected Clock/zone boundary; calendar arithmetic is shared. */
object RaceDates {
    @JvmStatic
    fun daysUntil(date: LocalDate?, clock: Clock?): Long {
        val race = date ?: throw NullPointerException("A race date is required.")
        val source = clock ?: throw NullPointerException("A clock is required.")
        return RaceCalendar.daysUntil(race, LocalDate.now(source))
    }

    @JvmStatic fun weeksUntil(date: LocalDate?, clock: Clock?): Long = daysUntil(date, clock) / 7
}
