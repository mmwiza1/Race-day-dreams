package org.racedaydreams.domain.activity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Small transparent aggregations; no fitness trend is invented from a single activity. */
public final class ActivityAnalytics {
    private ActivityAnalytics() {}
    public static List<RecordedWeek> weekly(List<ActivityRecord> activities, LocalDate today, ZoneId displayZone, int weeks) {
        if (weeks < 1 || weeks > 104) throw new IllegalArgumentException("Choose between one and 104 weeks.");
        LocalDate current = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<RecordedWeek> result = new ArrayList<>();
        for (int offset = weeks - 1; offset >= 0; offset--) {
            LocalDate start = current.minusWeeks(offset);
            double distance = 0;
            int count = 0;
            int unknown = 0;
            Double longest = null;
            Set<LocalDate> days = new HashSet<>();
            for (ActivityRecord activity : activities) {
                LocalDate date = activity.start().atZone(displayZone).toLocalDate();
                if (date.isBefore(start) || !date.isBefore(start.plusWeeks(1)) || date.isAfter(today)) continue;
                count++;
                days.add(date);
                if (activity.distanceMetres() == null) unknown++;
                else {
                    distance += activity.distanceMetres();
                    longest = longest == null ? activity.distanceMetres() : Math.max(longest, activity.distanceMetres());
                }
            }
            result.add(new RecordedWeek(start, count > 0 && unknown == count ? null : distance,
                    count, days.size(), unknown, longest));
        }
        return Collections.unmodifiableList(result);
    }
}
