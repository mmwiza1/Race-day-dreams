package org.racedaydreams.domain.reminders

import java.time.*
import java.util.Optional
import kotlin.jvm.JvmStatic

/** Original Java Clock/Optional contract; only platform calendar resolution remains JVM-owned. */
object EveningReminderPolicy {
    @JvmStatic
    fun schedule(
        entries: List<ReminderPlanEntry>?,
        preferences: ReminderPreferences?,
        clock: Clock?,
        notificationPermission: Boolean,
    ): List<EveningReminder> {
        if (entries == null) throw NullPointerException("planned workouts")
        if (clock == null) throw NullPointerException("clock")
        return ReminderRules.schedule(
            entries,
            preferences?.shared(),
            { clock.instant() },
            notificationPermission,
            JvmReminderCalendar,
        )
    }

    @JvmStatic
    fun candidate(
        entry: ReminderPlanEntry?,
        preferences: ReminderPreferences?,
    ): Optional<EveningReminder> =
        Optional.ofNullable(
            ReminderRules.candidate(entry, preferences?.shared(), JvmReminderCalendar)
        )

    @JvmStatic
    fun canDeliver(
        scheduled: EveningReminder?,
        current: ReminderPlanEntry?,
        preferences: ReminderPreferences?,
        clock: Clock?,
        notificationPermission: Boolean,
    ): Boolean =
        ReminderRules.canDeliver(
            scheduled,
            current,
            preferences?.shared(),
            { clock!!.instant() },
            notificationPermission,
            JvmReminderCalendar,
        )
}

internal object JvmReminderCalendar : ReminderCalendar {
    override fun resolve(date: LocalDate, nanoOfDay: Long, zoneId: String): LocalReminderTime {
        val local = date.atTime(LocalTime.ofNanoOfDay(nanoOfDay)).atZone(ZoneId.of(zoneId))
        return LocalReminderTime(
            local.toInstant(),
            local.toLocalDate(),
            local.toLocalTime().toNanoOfDay(),
        )
    }

    override fun at(instant: Instant, zoneId: String): LocalReminderTime {
        val local = instant.atZone(ZoneId.of(zoneId))
        return LocalReminderTime(instant, local.toLocalDate(), local.toLocalTime().toNanoOfDay())
    }
}
