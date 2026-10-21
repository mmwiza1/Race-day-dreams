package org.racedaydreams.domain.reminders;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

/** Local, opt-in reminder preferences. A null quiet-hours value explicitly disables quiet hours. */
public record ReminderPreferences(Mode mode, LocalTime eveningTime, QuietHours quietHours, ZoneId zone) {
    public enum Mode { DISABLED, ALL, LONG_HARD_ONLY }

    public record QuietHours(LocalTime start, LocalTime end) {
        public QuietHours {
            Objects.requireNonNull(start, "quiet-hours start");
            Objects.requireNonNull(end, "quiet-hours end");
            if (start.equals(end)) throw new IllegalArgumentException("Quiet hours need different start and end times.");
        }

        public boolean contains(LocalTime time) {
            return start.isBefore(end)
                    ? !time.isBefore(start) && time.isBefore(end)
                    : !time.isBefore(start) || time.isBefore(end);
        }
    }

    public ReminderPreferences {
        Objects.requireNonNull(mode, "reminder mode");
        Objects.requireNonNull(eveningTime, "reminder time");
        Objects.requireNonNull(zone, "local time zone");
    }

    public static ReminderPreferences defaults(ZoneId zone) {
        return new ReminderPreferences(Mode.DISABLED, LocalTime.of(19, 0),
                new QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0)), zone);
    }
}
