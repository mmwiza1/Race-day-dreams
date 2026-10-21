package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Two linear sweeps per distance inspect both sample-boundary starts and ends. */
public final class BestEffortCalculator {
    private BestEffortCalculator() {}
    public static BestEffortAnalysis calculate(ActivityRecord activity) { return calculate(activity, PerformanceDistance.CORE); }

    public static BestEffortAnalysis calculate(ActivityRecord activity, List<PerformanceDistance> distances) {
        Objects.requireNonNull(activity, "Activity");
        Objects.requireNonNull(distances, "Distances");
        SampleSeries series = new SampleSeries(activity);
        List<BestEffort> efforts = new ArrayList<>();
        if (!series.usable()) return new BestEffortAnalysis(efforts, series.quality, series.reasons);
        for (PerformanceDistance distance : distances) {
            Objects.requireNonNull(distance, "Distance");
            Candidate best = null;
            for (SampleSeries.Segment segment : series.segments) {
                int end = segment.first();
                // A piecewise-linear duration window reaches its minimum where a start or end
                // meets a recorded distance point. Considering only starts misses valid minima.
                for (int start = segment.first(); start <= segment.last(); start++) {
                    double target = series.samples.get(start).distanceMetres() + distance.metres();
                    while (end <= segment.last() && series.samples.get(end).distanceMetres() < target - .000001) end++;
                    if (end > segment.last()) break;
                    double finish = interpolate(series.samples, end, target);
                    best = better(best, ActivityChecks.seconds(series.samples.get(start).elapsed()), finish, end - start + 1);
                }
                int start = segment.first();
                for (int endIndex = segment.first(); endIndex <= segment.last(); endIndex++) {
                    double target = series.samples.get(endIndex).distanceMetres() - distance.metres();
                    if (target < series.samples.get(segment.first()).distanceMetres() - .000001) continue;
                    while (start < endIndex && series.samples.get(start + 1).distanceMetres() <= target + .000001) start++;
                    double begin;
                    if (Math.abs(series.samples.get(start).distanceMetres() - target) < .000001) begin = ActivityChecks.seconds(series.samples.get(start).elapsed());
                    else begin = interpolate(series.samples, start + 1, target);
                    best = better(best, begin, ActivityChecks.seconds(series.samples.get(endIndex).elapsed()), endIndex - start + 1);
                }
            }
            if (best == null) continue;
            List<String> reasons = new ArrayList<>();
            SampleQuality quality = SampleQuality.QUALIFIED;
            if (best.samples < 5) {
                quality = SampleQuality.UNCERTAIN;
                reasons.add("Fewer than five distance points cover this effort; it is not awarded as an automatic PB.");
            }
            if (series.totalsDisagree) {
                quality = SampleQuality.UNCERTAIN;
                reasons.add("Sample distance and total distance disagree; this candidate is not awarded as an automatic PB.");
            }
            reasons.add("Derived from a continuous recorded interval with boundaries interpolated between nearby samples; not an official race result.");
            Duration begin = ActivityChecks.duration(best.start);
            Duration finish = ActivityChecks.duration(best.end);
            efforts.add(new BestEffort(distance, finish.minus(begin), begin, finish, quality, reasons));
        }
        List<String> reasons = new ArrayList<>(series.reasons);
        if (efforts.isEmpty()) reasons.add("No continuously recorded interval was long enough for the requested distances.");
        return new BestEffortAnalysis(efforts, series.quality, reasons);
    }

    /** Stable IDs let imports update/recompute efforts atomically without duplicate PB evidence. */
    public static List<RecordedResult> qualifiedResults(ActivityRecord activity) {
        List<RecordedResult> results = new ArrayList<>();
        for (BestEffort effort : calculate(activity).efforts()) {
            if (effort.quality() != SampleQuality.QUALIFIED) continue;
            results.add(new RecordedResult(activity.id() + ":" + effort.distance().code(), effort.distance(),
                    effort.duration(), activity.start().atOffset(activity.startOffset() == null ? ZoneOffset.UTC : activity.startOffset()).toLocalDate(),
                    null, ResultSource.ROLLING, activity.id(), "Calculated rolling effort from retained continuous samples."));
        }
        return Collections.unmodifiableList(results);
    }

    private record Candidate(double start, double end, int samples) {}
    private static Candidate better(Candidate previous, double start, double end, int samples) {
        if (end <= start) return previous;
        if (previous == null || end - start < previous.end - previous.start - .000000001
                || (Math.abs(end - start - (previous.end - previous.start)) <= .000000001 && start < previous.start)) return new Candidate(start, end, samples);
        return previous;
    }
    private static double interpolate(List<DistanceSample> samples, int end, double target) {
        DistanceSample right = samples.get(end);
        if (Math.abs(right.distanceMetres() - target) < .000001) return ActivityChecks.seconds(right.elapsed());
        DistanceSample left = samples.get(end - 1);
        double fraction = (target - left.distanceMetres()) / (right.distanceMetres() - left.distanceMetres());
        return ActivityChecks.seconds(left.elapsed()) + fraction * ActivityChecks.seconds(right.elapsed().minus(left.elapsed()));
    }
}
