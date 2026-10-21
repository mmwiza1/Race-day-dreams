package org.racedaydreams.domain.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.racedaydreams.domain.UnitSystem;

/** Splits derive only from retained distance samples; aggregate time/distance is insufficient. */
public final class SplitCalculator {
    private SplitCalculator() {}
    public static SplitAnalysis calculate(ActivityRecord activity, UnitSystem units) {
        return calculate(activity, units == UnitSystem.IMPERIAL ? 1_609.344 : 1_000);
    }

    public static SplitAnalysis calculate(ActivityRecord activity, double splitMetres) {
        Objects.requireNonNull(activity, "Activity");
        if (!Double.isFinite(splitMetres) || splitMetres <= 0) throw new IllegalArgumentException("Split distance must be positive.");
        SampleSeries series = new SampleSeries(activity);
        List<RunSplit> result = new ArrayList<>();
        List<String> reasons = new ArrayList<>(series.reasons);
        if (!series.usable()) return new SplitAnalysis(result, series.quality, reasons);
        double last = series.samples.get(series.samples.size() - 1).distanceMetres();
        if (last / splitMetres > 10_000) return new SplitAnalysis(result, SampleQuality.REJECTED,
                java.util.Collections.singletonList("The sample range is too large for a sensible split list."));
        int segmentIndex = 0;
        int count = (int) Math.ceil(last / splitMetres - .000000001);
        boolean missing = false;
        for (int number = 1; number <= count; number++) {
            double from = (number - 1) * splitMetres;
            double to = Math.min(number * splitMetres, last);
            if (to - from < .001) continue;
            while (segmentIndex < series.segments.size()
                    && series.samples.get(series.segments.get(segmentIndex).last()).distanceMetres() < to - .000001) segmentIndex++;
            if (segmentIndex == series.segments.size()) { missing = true; continue; }
            SampleSeries.Segment segment = series.segments.get(segmentIndex);
            if (series.samples.get(segment.first()).distanceMetres() > from + .000001) { missing = true; continue; }
            // Adjacent elapsed splits share the first observed crossing of their boundary.
            // A stationary interval at that boundary belongs to the following split; using
            // the latest start (appropriate for rolling efforts) would silently discard it.
            SampleSeries.Point start = series.at(segment, from, false);
            // Include observed stationary time after the final distance was reached too.
            SampleSeries.Point end = series.at(segment, to, to == last);
            double startSeconds = start.seconds();
            if (end.seconds() <= startSeconds) { missing = true; continue; }
            Double elevation = start.elevation() == null || end.elevation() == null ? null : end.elevation() - start.elevation();
            result.add(new RunSplit(number, to - from, ActivityChecks.duration(end.seconds() - startSeconds),
                    series.sampleMean(startSeconds, end.seconds(), true), elevation,
                    series.sampleMean(startSeconds, end.seconds(), false), to - from < splitMetres - .001));
        }
        if (missing) reasons.add("Some distance splits could not be derived because their full interval was not continuously recorded.");
        reasons.add("Splits use observed elapsed time, including stationary time within continuously recorded sections. Boundaries are interpolated between nearby recorded points. Heart-rate/cadence values are means of available samples, not invented complete recordings.");
        return new SplitAnalysis(result, missing ? SampleQuality.UNCERTAIN : series.quality, reasons);
    }
}
