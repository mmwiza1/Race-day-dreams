package org.racedaydreams.domain.baseline;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

/** Original conservative selection heuristics, not medical advice or a permanent profile flag. */
public final class PerformanceEligibilityEvaluator {
    private PerformanceEligibilityEvaluator() { }

    public static EligibilityDecision evaluate(RunnerBaseline baseline, Instant now) {
        return evaluate(baseline, now, baseline.manualRecordedAt());
    }

    public static EligibilityDecision evaluate(RunnerBaseline baseline, Instant now, Instant manualReviewedAt) {
        Objects.requireNonNull(baseline, "baseline"); Objects.requireNonNull(now, "now");
        ArrayList<String> reasons = new ArrayList<>();
        boolean manual = isManual(baseline, RunnerBaseline.WEEKLY_DISTANCE)
                || isManual(baseline, RunnerBaseline.RUNS_PER_WEEK)
                || isManual(baseline, RunnerBaseline.LONGEST_RUN)
                || isManual(baseline, RunnerBaseline.CONSISTENT_WEEKS);
        boolean recentRecords = baseline.lastActivityAt() != null && !baseline.lastActivityAt().isAfter(now)
                && Duration.between(baseline.lastActivityAt(), now).compareTo(Duration.ofDays(14)) <= 0;
        boolean recentManual = manualReviewedAt != null && !manualReviewedAt.isAfter(now)
                && Duration.between(manualReviewedAt, now).compareTo(Duration.ofDays(14)) <= 0;
        boolean entirelyManual = isManual(baseline, RunnerBaseline.WEEKLY_DISTANCE)
                && isManual(baseline, RunnerBaseline.RUNS_PER_WEEK)
                && isManual(baseline, RunnerBaseline.LONGEST_RUN)
                && isManual(baseline, RunnerBaseline.CONSISTENT_WEEKS);
        boolean current = manual ? recentManual && (entirelyManual || recentRecords) : recentRecords;
        if (!current) reasons.add(manual ? "Review your current running details before choosing Performance; manual review or the supporting recorded evidence is missing or more than two weeks old."
                : "Recent running evidence is missing or more than two weeks old. Performance remains available for reassessment as history builds.");
        if (baseline.weeklyDistanceKm() == null || baseline.weeklyDistanceKm() < 25) reasons.add("Performance currently asks for evidence of roughly 25 km or more per observed week.");
        if (baseline.runsPerWeek() == null || baseline.runsPerWeek() < 3) reasons.add("Performance currently asks for an established routine of at least three runs per week.");
        if (baseline.longestRunKm() == null || baseline.longestRunKm() < 8) reasons.add("A recent endurance run of around 8 km or more has not yet been supported.");
        if (baseline.consistentWeeks() == null || baseline.consistentWeeks() < 6) reasons.add("At least six weeks of a consistent routine would give a stronger basis for extra intensity.");
        boolean eligible = reasons.isEmpty();
        if (eligible) reasons.add("Your recent workload, routine and endurance evidence support offering Performance. This is an adjustable training preference, not a medical assurance.");
        if (manual) reasons.add("This decision includes your manually entered history. Imported or completed workouts will refine it over time.");
        reasons.add("These thresholds are transparent product heuristics, not universal training rules. Other strategies and runner choices remain available.");
        Instant evidenceAt = manual ? manualReviewedAt : baseline.lastActivityAt();
        Instant deadline = evidenceAt == null ? now : evidenceAt.plus(Duration.ofDays(14));
        if (deadline.isBefore(now)) deadline = now;
        return new EligibilityDecision(eligible, reasons, deadline);
    }

    private static boolean isManual(RunnerBaseline baseline, String key) {
        return baseline.provenance().get(key) == BaselineProvenance.MANUAL;
    }
}
