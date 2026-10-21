package org.racedaydreams.domain.activity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shared conservative quality gate. Invalid intervals are never interpolated across. */
final class SampleSeries {
    static final double MAX_GAP_SECONDS = 30;
    static final double MAX_SPEED_METRES_PER_SECOND = 12;
    final List<DistanceSample> samples;
    final List<Segment> segments = new ArrayList<>();
    final List<String> reasons = new ArrayList<>();
    SampleQuality quality;
    boolean totalsDisagree;
    private double[] secondsIndex;
    private double[] heartRateTotals;
    private double[] cadenceTotals;
    private int[] heartRateCounts;
    private int[] cadenceCounts;

    record Segment(int first, int last) {}
    record Point(double seconds, Double elevation) {}

    SampleSeries(ActivityRecord activity) {
        samples = activity.samples();
        if (samples.size() < 2) {
            quality = SampleQuality.UNAVAILABLE;
            reasons.add("Continuous distance/time samples are unavailable; whole-activity totals cannot establish rolling efforts or distance splits.");
            return;
        }
        if (samples.size() > 500_000) {
            reject("The activity contains too many samples to analyse safely.");
            return;
        }
        double elapsed = ActivityChecks.seconds(activity.elapsedTime());
        if (ActivityChecks.seconds(samples.get(samples.size() - 1).elapsed()) > elapsed + 0.001) {
            reject("Distance samples extend beyond the recorded activity duration.");
            return;
        }
        quality = SampleQuality.QUALIFIED;
        int first = 0;
        for (int index = 1; index < samples.size(); index++) {
            DistanceSample previous = samples.get(index - 1);
            DistanceSample current = samples.get(index);
            double seconds = ActivityChecks.seconds(current.elapsed().minus(previous.elapsed()));
            double distance = current.distanceMetres() - previous.distanceMetres();
            if (seconds <= 0 || distance < 0) {
                reject("Samples have unordered timestamps or decreasing cumulative distance.");
                return;
            }
            if (distance / seconds > MAX_SPEED_METRES_PER_SECOND) {
                reject("An implausible distance jump exceeds the running sample quality limit (12 m/s); no automatic PB is awarded.");
                return;
            }
            if (seconds > MAX_GAP_SECONDS || current.discontinuity()) {
                if (index - 1 > first) segments.add(new Segment(first, index - 1));
                first = index;
                quality = SampleQuality.UNCERTAIN;
            }
        }
        if (samples.size() - 1 > first) segments.add(new Segment(first, samples.size() - 1));
        if (quality == SampleQuality.UNCERTAIN) reasons.add("Pause/discontinuity markers or gaps over 30 seconds split the samples into continuous sections. No split or effort crosses them.");
        double lastDistance = samples.get(samples.size() - 1).distanceMetres();
        if (activity.distanceMetres() != null && Math.abs(lastDistance - activity.distanceMetres()) > Math.max(100, activity.distanceMetres() * .05)) {
            totalsDisagree = true;
            quality = SampleQuality.UNCERTAIN;
            reasons.add("Sample distance and the recorded activity total differ materially; calculated efforts remain uncertain.");
        }
        indexMetrics();
    }

    private void reject(String reason) {
        quality = SampleQuality.REJECTED;
        segments.clear();
        reasons.clear();
        reasons.add(reason);
    }

    boolean usable() { return quality != SampleQuality.REJECTED && quality != SampleQuality.UNAVAILABLE; }

    /** Start uses the latest time at a distance plateau; end uses the earliest. */
    Point at(Segment segment, double distance, boolean start) {
        int low = segment.first();
        int high = segment.last();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (samples.get(middle).distanceMetres() < distance) low = middle + 1;
            else high = middle;
        }
        int index = low;
        DistanceSample right = samples.get(index);
        if (Math.abs(right.distanceMetres() - distance) < .000001) {
            if (start) {
                while (index < segment.last() && Math.abs(samples.get(index + 1).distanceMetres() - distance) < .000001) index++;
                right = samples.get(index);
            }
            return new Point(ActivityChecks.seconds(right.elapsed()), right.elevationMetres());
        }
        DistanceSample left = samples.get(index - 1);
        double fraction = (distance - left.distanceMetres()) / (right.distanceMetres() - left.distanceMetres());
        double seconds = ActivityChecks.seconds(left.elapsed()) + fraction * ActivityChecks.seconds(right.elapsed().minus(left.elapsed()));
        Double elevation = left.elevationMetres() != null && right.elevationMetres() != null
                ? left.elevationMetres() + fraction * (right.elevationMetres() - left.elevationMetres()) : null;
        return new Point(seconds, elevation);
    }

    Double sampleMean(double startSeconds, double endSeconds, boolean heartRate) {
        int first = lowerTime(startSeconds, false);
        int last = lowerTime(endSeconds, true);
        double[] totals = heartRate ? heartRateTotals : cadenceTotals;
        int[] counts = heartRate ? heartRateCounts : cadenceCounts;
        int count = counts[last] - counts[first];
        return count == 0 ? null : (totals[last] - totals[first]) / count;
    }

    private void indexMetrics() {
        int size = samples.size();
        secondsIndex = new double[size];
        heartRateTotals = new double[size + 1];
        cadenceTotals = new double[size + 1];
        heartRateCounts = new int[size + 1];
        cadenceCounts = new int[size + 1];
        for (int index = 0; index < size; index++) {
            DistanceSample sample = samples.get(index);
            secondsIndex[index] = ActivityChecks.seconds(sample.elapsed());
            heartRateTotals[index + 1] = heartRateTotals[index];
            cadenceTotals[index + 1] = cadenceTotals[index];
            heartRateCounts[index + 1] = heartRateCounts[index];
            cadenceCounts[index + 1] = cadenceCounts[index];
            if (sample.heartRate() != null) {
                heartRateTotals[index + 1] += sample.heartRate();
                heartRateCounts[index + 1]++;
            }
            if (sample.cadence() != null) {
                cadenceTotals[index + 1] += sample.cadence();
                cadenceCounts[index + 1]++;
            }
        }
    }

    private int lowerTime(double seconds, boolean inclusive) {
        int low = 0;
        int high = secondsIndex.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (secondsIndex[middle] < seconds || (inclusive && secondsIndex[middle] <= seconds)) low = middle + 1;
            else high = middle;
        }
        return low;
    }
}
