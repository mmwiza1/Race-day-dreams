package org.racedaydreams.domain.reminders

/** Platform cancels all previous entries absent from this replacement schedule. */
interface ReminderScheduleRepository {
    fun replaceSchedule(currentReminders: List<EveningReminder>)
}
