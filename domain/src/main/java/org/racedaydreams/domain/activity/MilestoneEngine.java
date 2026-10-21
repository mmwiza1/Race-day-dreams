package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Optional recorded achievements only. No streaks, notifications or guesses about lifetime firsts. */
public final class MilestoneEngine {
    private MilestoneEngine() {}
    public static List<Milestone> calculate(boolean enabled, List<ActivityRecord> activities, List<RecordedResult> results) {
        if (!enabled) return Collections.emptyList();
        List<Milestone> milestones = new ArrayList<>();
        PerformanceDistance[] firsts = {PerformanceDistance.FIVE_K, PerformanceDistance.TEN_K,
                PerformanceDistance.HALF_MARATHON, PerformanceDistance.MARATHON};
        for (PerformanceDistance distance : firsts) {
            Milestone earliest = null;
            for (ActivityRecord activity : activities) {
                if (activity.distanceMetres() == null || activity.distanceMetres() < distance.metres()
                        || activity.distanceMetres() / ActivityChecks.seconds(activity.elapsedTime()) > SampleSeries.MAX_SPEED_METRES_PER_SECOND
                        || (!activity.samples().isEmpty() && new SampleSeries(activity).quality == SampleQuality.REJECTED)) continue;
                LocalDate date = activity.start().atOffset(activity.startOffset() == null ? ZoneOffset.UTC : activity.startOffset()).toLocalDate();
                Milestone candidate = new Milestone("first_" + distance.code(), "First recorded " + distance.label(), date,
                        activity.id(), null, "First qualifying activity in the app's retained history, not a claim about your lifetime running history.");
                if (earliest == null || date.isBefore(earliest.date())) earliest = candidate;
            }
            for (RecordedResult result : results) {
                if (result.distance().metres() < distance.metres()) continue;
                if (earliest == null || result.date().isBefore(earliest.date())) earliest = new Milestone("first_" + distance.code(),
                        "First recorded " + distance.label(), result.date(), result.activityId(), result.id(), "Based on a retained result and its stated provenance.");
            }
            if (earliest != null) milestones.add(earliest);
        }
        addThreshold(milestones, results, PerformanceDistance.FIVE_K, 30, "Sub-30 5K");
        addThreshold(milestones, results, PerformanceDistance.FIVE_K, 25, "Sub-25 5K");
        addThreshold(milestones, results, PerformanceDistance.TEN_K, 60, "Sub-60 10K");
        addThreshold(milestones, results, PerformanceDistance.TEN_K, 55, "Sub-55 10K");
        addThreshold(milestones, results, PerformanceDistance.TEN_K, 50, "Sub-50 10K");
        addThreshold(milestones, results, PerformanceDistance.HALF_MARATHON, 120, "Sub-2-hour Half Marathon");
        milestones.sort(Comparator.comparing(Milestone::date).thenComparing(Milestone::code));
        return Collections.unmodifiableList(milestones);
    }

    private static void addThreshold(List<Milestone> milestones, List<RecordedResult> results,
            PerformanceDistance distance, long minutes, String label) {
        RecordedResult earliest = null;
        for (RecordedResult result : results) {
            if (!result.distance().code().equals(distance.code()) || result.duration().compareTo(Duration.ofMinutes(minutes)) >= 0) continue;
            if (earliest == null || result.date().isBefore(earliest.date())
                    || (result.date().equals(earliest.date()) && result.id().compareTo(earliest.id()) < 0)) earliest = result;
        }
        if (earliest != null) milestones.add(new Milestone("sub_" + minutes + "_" + distance.code(), label,
                earliest.date(), earliest.activityId(), earliest.id(), "A recorded result met this threshold; its source is retained with the result."));
    }
}
