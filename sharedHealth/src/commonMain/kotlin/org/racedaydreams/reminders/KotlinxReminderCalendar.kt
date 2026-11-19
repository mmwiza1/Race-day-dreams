package org.racedaydreams.reminders

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.racedaydreams.domain.reminders.LocalReminderTime
import org.racedaydreams.domain.reminders.ReminderCalendar
import org.racedaydreams.domain.time.*

/** Actual cross-platform regional calendar using the application's existing datetime dependency. */
object KotlinxReminderCalendar : ReminderCalendar {
    override fun resolve(date: LocalDate, nanoOfDay: Long, zoneId: String): LocalReminderTime {
        val second = nanoOfDay / 1000000000
        val wall =
            kotlinx.datetime.LocalDateTime(
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                (second / 3600).toInt(),
                (second % 3600 / 60).toInt(),
                (second % 60).toInt(),
                (nanoOfDay % 1000000000).toInt(),
            )
        val instant = wall.toInstant(TimeZone.of(zoneId))
        return at(
            Instants.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond.toLong()),
            zoneId,
        )
    }

    override fun at(instant: Instant, zoneId: String): LocalReminderTime {
        val native =
            kotlinx.datetime.Instant.fromEpochSeconds(instant.getEpochSecond(), instant.getNano())
        val wall = native.toLocalDateTime(TimeZone.of(zoneId))
        val nano =
            ((wall.hour * 3600L + wall.minute * 60L + wall.second) * 1000000000) + wall.nanosecond
        return LocalReminderTime(
            instant,
            CalendarDates.of(wall.year, wall.monthNumber, wall.dayOfMonth),
            nano,
        )
    }
}
