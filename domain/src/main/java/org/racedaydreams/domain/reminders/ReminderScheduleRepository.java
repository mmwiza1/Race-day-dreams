package org.racedaydreams.domain.reminders;

import java.util.List;

/** A backend replaces the complete schedule, cancelling every previous entry absent from the new set. */
public interface ReminderScheduleRepository {
    void replaceSchedule(List<EveningReminder> currentReminders);
}
