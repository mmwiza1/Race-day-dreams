package org.racedaydreams.domain.baseline;

import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.RunSplit;
import org.racedaydreams.domain.activity.SampleQuality;
import org.racedaydreams.domain.activity.SplitAnalysis;
import org.racedaydreams.domain.activity.SplitCalculator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Assessment interpretation with explicit evidence limits, not a single-run fitness oracle. */
public final class AssessmentAnalyzer {
    private AssessmentAnalyzer() { }

    public static AssessmentRecommendation recommend(RunnerBaseline baseline) {
        Objects.requireNonNull(baseline, "baseline");
        ArrayList<String> reasons = new ArrayList<>();
        int distance;
        boolean unnecessary = baseline.confidence() == BaselineConfidence.STRONG;
        if (unnecessary) {
            distance = 5;
            reasons.add("Your recent running history already supports a useful baseline. An additional assessment is optional, not required.");
        } else if (baseline.runsPerWeek() == null || baseline.runsPerWeek() < 1
                || baseline.longestRunKm() == null || baseline.longestRunKm() < 2
                || (baseline.consistentWeeks() != null && baseline.consistentWeeks() < 2)) {
            distance = 1;
            reasons.add("For a new or returning routine, a short controlled 1K provides a starting point without asking for an unnecessary maximal 5K.");
        } else if (baseline.longestRunKm() < 3 || baseline.runsPerWeek() < 2) {
            distance = 2;
            reasons.add("A controlled 2K fits the limited endurance evidence currently available.");
        } else if (baseline.longestRunKm() >= 6 && baseline.runsPerWeek() >= 3
                && baseline.consistentWeeks() != null && baseline.consistentWeeks() >= 6) {
            distance = 5;
            reasons.add("Your established routine supports offering a 5K assessment if you want one.");
        } else {
            distance = 3;
            reasons.add("A controlled 3K can fill gaps in an occasional running history without requiring a maximal 5K.");
        }
        reasons.add("You may choose any distance from 1K to 5K, choose controlled effort or best effort, or skip the assessment.");
        return new AssessmentRecommendation(distance, unnecessary, reasons);
    }

    public static AssessmentSummary analyze(AssessmentResult result, ActivityRecord activity, RunnerBaseline history) {
        Objects.requireNonNull(result, "result"); Objects.requireNonNull(history, "history");
        ArrayList<String> findings = new ArrayList<>();
        double seconds = result.duration().getSeconds() + result.duration().getNano() / 1_000_000_000.0;
        double averagePace = seconds / result.assessment().distanceKm();
        Double first = null, last = null, fade = null, hr = null, cadence = null;
        if (activity != null) {
            if (result.activityId() == null || !result.activityId().equals(activity.id())) throw new IllegalArgumentException("Assessment activity link does not match.");
            boolean comparable = activity.distanceMetres() != null && Math.abs(activity.distanceMetres() - result.assessment().distanceKm() * 1000.0)
                    <= Math.max(50, result.assessment().distanceKm() * 20.0);
            if (comparable) {
                hr = activity.averageHeartRate(); cadence = activity.cadence();
            }
            SplitAnalysis analysis = SplitCalculator.calculate(activity, 1000.0);
            ArrayList<RunSplit> full = new ArrayList<>();
            if (comparable && analysis.quality() == SampleQuality.QUALIFIED) for (RunSplit split : analysis.splits()) if (!split.partial()) full.add(split);
            if (!full.isEmpty() && full.size() >= result.assessment().distanceKm()) {
                first = full.get(0).secondsPerKilometre(); last = full.get(full.size() - 1).secondsPerKilometre();
                if (full.size() >= 2) {
                    fade = (last - first) / first * 100;
                    double variation = (maximum(full) - minimum(full)) / average(full);
                    if (variation <= 0.10) findings.add("Your recorded kilometre paces stayed relatively stable.");
                    else findings.add("Your recorded kilometre paces varied; terrain, pauses and effort may all contribute.");
                    if (last < first * 0.98) findings.add("The final recorded kilometre was faster than the first.");
                    else if (last > first * 1.05) findings.add("The final recorded kilometre was slower than the first. One result cannot establish the cause.");
                }
            } else findings.add("Reliable kilometre samples are unavailable, so pace fade and finishing splits have not been invented.");
        } else findings.add("Only the entered distance, time and optional RPE are known. Splits, HR, cadence and elevation remain unavailable.");
        if (result.assessment().mode() == AssessmentMode.CONTROLLED) findings.add("This was a controlled assessment, not a claim of maximal race capability.");
        else findings.add("This was recorded as a best effort, but one short result is not proof of longer-distance endurance.");
        if (result.rpe() != null) findings.add("Your effort rating is manually entered and kept separate from measured sensor data.");
        double weight = 1.0 / (1.0 + history.activityCount() / 3.0);
        findings.add("This assessment is one piece of evidence. Its relative weight decreases as completed running history accumulates.");
        findings.add("Training pace prescriptions and race predictions are pending; average assessment pace is not automatically labelled easy pace.");
        return new AssessmentSummary(averagePace, first, last, fade, hr, cadence,
                history.confidence() == BaselineConfidence.STRONG ? BaselineConfidence.STRONG
                        : history.confidence() == BaselineConfidence.MODERATE ? BaselineConfidence.MODERATE : BaselineConfidence.LOW,
                weight, findings);
    }

    private static double maximum(List<RunSplit> splits) {
        double value = 0; for (RunSplit split : splits) value = Math.max(value, split.secondsPerKilometre()); return value;
    }
    private static double minimum(List<RunSplit> splits) {
        double value = Double.MAX_VALUE; for (RunSplit split : splits) value = Math.min(value, split.secondsPerKilometre()); return value;
    }
    private static double average(List<RunSplit> splits) {
        double value = 0; for (RunSplit split : splits) value += split.secondsPerKilometre(); return value / splits.size();
    }
}
