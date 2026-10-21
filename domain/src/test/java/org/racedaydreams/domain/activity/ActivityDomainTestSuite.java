package org.racedaydreams.domain.activity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.racedaydreams.domain.UnitSystem;

/** Deterministic sample fixtures test real calculations without Android, GPS, accounts or libraries. */
public final class ActivityDomainTestSuite {
    private static final Instant START = Instant.parse("2026-10-21T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(START, ZoneOffset.UTC);
    private static int checks;

    public static void main(String[] args) {
        records();
        rollingEfforts();
        quality();
        splits();
        progression();
        shoes();
        milestones();
        analytics();
        System.out.println("ActivityDomainTestSuite: " + checks + " checks passed.");
    }

    private static void records() {
        ActivityRecord missing = activity("missing", null, Duration.ofMinutes(30), new ArrayList<>(), null);
        equal(null, missing.distanceMetres(), "Missing distance remains unavailable");
        equal(null, missing.averageHeartRate(), "Missing HR remains unavailable");
        equal(null, missing.movingTime(), "Moving time not inferred from elapsed time");
        equal(Duration.ofMinutes(30), missing.elapsedTime(), "Elapsed session time exact");
        List<DistanceSample> mutable = uniform(2_000, 4, 10);
        ActivityRecord copied = activity("copy", 2_000.0, Duration.ofSeconds(500), mutable, null);
        mutable.clear();
        check(copied.samples().size() > 0, "Samples defensively copied");
        expect(UnsupportedOperationException.class, () -> copied.samples().clear(), "Samples immutable");
        rejects(() -> activity("bad", -1.0, Duration.ofSeconds(30), Collections.emptyList(), null), "Negative total distance rejected");
        rejects(() -> new DistanceSample(Duration.ofSeconds(-1), 0, null, null, null, false), "Negative sample time rejected");
        rejects(() -> new DistanceSample(Duration.ZERO, Double.NaN, null, null, null, false), "Non-finite distance rejected");
        rejects(() -> new DistanceSample(Duration.ZERO, 0, 0.0, null, null, false), "Unavailable HR cannot be fabricated as zero");
        rejects(() -> new ActivityLap("lap", START, START, null, "Rep", "s1"), "Zero-duration lap rejected");
        PerformanceDistance custom = new PerformanceDistance("4k", 4_000, "4K");
        equal(4_000.0, custom.metres(), "Additional PB distances need no enum/schema change");
        equal(8, PerformanceDistance.CORE.size(), "Eight core distances");
        equal(1_609.344, PerformanceDistance.ONE_MILE.metres(), "Exact mile");
        equal(21_097.5, PerformanceDistance.HALF_MARATHON.metres(), "Exact half marathon");
        equal(42_195.0, PerformanceDistance.MARATHON.metres(), "Exact marathon");
    }

    private static void rollingEfforts() {
        ActivityRecord run = elevenK();
        BestEffortAnalysis analysis = BestEffortCalculator.calculate(run);
        BestEffort five = effort(analysis, PerformanceDistance.FIVE_K);
        equal(Duration.ofMinutes(25), five.duration(), "Fastest 5K inside an 11K activity");
        equal(Duration.ofMinutes(12), five.startElapsed(), "Best 5K begins inside the activity");
        equal(SampleQuality.QUALIFIED, five.quality(), "Dense continuous effort qualified");
        equal(null, effortOrNull(analysis, PerformanceDistance.HALF_MARATHON), "Insufficient distance cannot create HM PB");
        equal(6, BestEffortCalculator.qualifiedResults(run).size(), "Core efforts only where continuous distance supports them");
        RecordedResult result = BestEffortCalculator.qualifiedResults(run).get(4);
        equal("long:5k", result.id(), "Stable rolling result ID");
        equal(ResultSource.ROLLING, result.source(), "Derived provenance retained");
        equal("long", result.activityId(), "Effort links actual activity");
        equal(LocalDate.of(2026, 10, 21), result.date(), "Recorded timezone date retained");
        expect(UnsupportedOperationException.class, () -> BestEffortCalculator.qualifiedResults(run).clear(), "Derived result list immutable");

        List<DistanceSample> accelerating = new ArrayList<>();
        double[] distances = {0, 90, 190, 300, 420, 550, 690, 840, 1_000, 1_170, 1_350, 1_540};
        for (int index = 0; index < distances.length; index++) accelerating.add(point(index * 20, distances[index]));
        ActivityRecord fastFinish = activity("accelerating", 1_540.0, Duration.ofSeconds(220), accelerating, null);
        BestEffort endingAtSample = effort(BestEffortCalculator.calculate(fastFinish), PerformanceDistance.ONE_K);
        near(121.538461538, seconds(endingAtSample.duration()), .000001, "End-boundary sweep finds minimum with an interpolated start");
        equal(Duration.ofSeconds(220), endingAtSample.endElapsed(), "Fastest effort ends at final sample");

        List<DistanceSample> stationary = new ArrayList<>();
        stationary.add(point(0, 0)); stationary.add(point(10, 0)); stationary.add(point(20, 0));
        for (int index = 1; index <= 100; index++) stationary.add(point(20 + index * 2, index * 10));
        ActivityRecord stopAtStart = activity("stationary", 1_000.0, Duration.ofSeconds(220), stationary, null);
        BestEffort continuous = effort(BestEffortCalculator.calculate(stopAtStart), PerformanceDistance.ONE_K);
        equal(Duration.ofSeconds(200), continuous.duration(), "Rolling effort excludes stationary time before its start");
        equal(Duration.ofSeconds(20), continuous.startElapsed(), "Distance plateau uses latest effort start");
        equal(Duration.ofSeconds(220), SplitCalculator.calculate(stopAtStart, UnitSystem.METRIC).splits().get(0).duration(), "First fixed split preserves stationary elapsed time");

        ActivityRecord noTotal = activity("samples-only", null, run.elapsedTime(), run.samples(), null);
        check(!BestEffortCalculator.qualifiedResults(noTotal).isEmpty(), "Real samples can support efforts without an aggregate total");
        equal(null, noTotal.distanceMetres(), "Calculating samples does not overwrite missing measured total");
    }

    private static void quality() {
        ActivityRecord aggregateOnly = activity("aggregate", 5_000.0, Duration.ofMinutes(20), Collections.emptyList(), null);
        equal(0, BestEffortCalculator.qualifiedResults(aggregateOnly).size(), "Totals alone cannot fabricate rolling PBs");
        equal(SampleQuality.UNAVAILABLE, BestEffortCalculator.calculate(aggregateOnly).quality(), "No samples explicitly unavailable");
        equal(0, SplitCalculator.calculate(aggregateOnly, UnitSystem.METRIC).splits().size(), "No fabricated splits from totals");

        List<DistanceSample> jump = new ArrayList<>();
        jump.add(point(0, 0)); jump.add(point(10, 30)); jump.add(point(20, 600));
        for (int index = 1; index <= 100; index++) jump.add(point(20 + index * 5, 600 + index * 20));
        ActivityRecord gpsJump = activity("jump", 2_600.0, Duration.ofSeconds(520), jump, null);
        equal(SampleQuality.REJECTED, BestEffortCalculator.calculate(gpsJump).quality(), "Impossible instantaneous speed rejected");
        equal(0, BestEffortCalculator.qualifiedResults(gpsJump).size(), "GPS jump cannot award an extraordinary PB");
        equal(0, SplitCalculator.calculate(gpsJump, UnitSystem.METRIC).splits().size(), "Corrupt samples do not create splits");
        check(BestEffortCalculator.calculate(gpsJump).reasons().get(0).contains("12 m/s"), "GPS quality threshold explained");

        ActivityRecord decreasing = activity("decreasing", 1_000.0, Duration.ofMinutes(10), Arrays.asList(point(0, 0), point(10, 100), point(20, 90)), null);
        equal(SampleQuality.REJECTED, BestEffortCalculator.calculate(decreasing).quality(), "Decreasing cumulative distance rejected");
        ActivityRecord unordered = activity("unordered", 1_000.0, Duration.ofMinutes(10), Arrays.asList(point(0, 0), point(20, 50), point(10, 100)), null);
        equal(SampleQuality.REJECTED, BestEffortCalculator.calculate(unordered).quality(), "Unordered timestamps rejected without sorting away corruption");
        ActivityRecord beyondEnd = activity("beyond", 100.0, Duration.ofSeconds(10), Arrays.asList(point(0, 0), point(20, 100)), null);
        equal(SampleQuality.REJECTED, BestEffortCalculator.calculate(beyondEnd).quality(), "Samples beyond activity rejected");

        ActivityRecord original = elevenK();
        List<DistanceSample> paused = new ArrayList<>(original.samples());
        DistanceSample middle = paused.get(100);
        paused.set(100, new DistanceSample(middle.elapsed(), middle.distanceMetres(), null, null, null, true));
        ActivityRecord pause = activity("pause", 11_000.0, original.elapsedTime(), paused, null);
        BestEffortAnalysis pauseAnalysis = BestEffortCalculator.calculate(pause);
        equal(SampleQuality.UNCERTAIN, pauseAnalysis.quality(), "Pause discontinuity reported");
        BestEffort pausedFive = effort(pauseAnalysis, PerformanceDistance.FIVE_K);
        check(pausedFive.startElapsed().compareTo(middle.elapsed()) >= 0, "No five-kilometre effort crosses pause");
        check(pausedFive.duration().compareTo(Duration.ofMinutes(25)) > 0, "Cannot concatenate faster fragments across pause");

        List<DistanceSample> gapped = new ArrayList<>();
        for (int index = 0; index < original.samples().size(); index++) {
            DistanceSample sample = original.samples().get(index);
            gapped.add(new DistanceSample(sample.elapsed().plusSeconds(index >= 100 ? 30 : 0), sample.distanceMetres(), null, null, null, false));
        }
        ActivityRecord gap = activity("gap", 11_000.0, original.elapsedTime().plusSeconds(30), gapped, null);
        equal(SampleQuality.UNCERTAIN, BestEffortCalculator.calculate(gap).quality(), "Missing period reported");
        check(effort(BestEffortCalculator.calculate(gap), PerformanceDistance.FIVE_K).duration().compareTo(Duration.ofMinutes(25)) > 0, "Rolling effort cannot bridge missing samples");

        ActivityRecord disagree = activity("disagree", 15_000.0, original.elapsedTime(), original.samples(), null);
        equal(SampleQuality.UNCERTAIN, BestEffortCalculator.calculate(disagree).quality(), "Conflicting aggregate distance flags uncertainty");
        equal(0, BestEffortCalculator.qualifiedResults(disagree).size(), "Conflicting distance does not award PBs");
        ActivityRecord twoPoints = activity("sparse", 100.0, Duration.ofSeconds(20), Arrays.asList(point(0, 0), point(20, 100)), null);
        BestEffort sparse = BestEffortCalculator.calculate(twoPoints, Collections.singletonList(new PerformanceDistance("100m", 100, "100 m"))).efforts().get(0);
        equal(SampleQuality.UNCERTAIN, sparse.quality(), "Sparse candidates are explicitly uncertain");
        rejects(() -> SplitCalculator.calculate(original, 0), "Zero split distance rejected");
    }

    private static void splits() {
        ActivityRecord run = elevenK();
        SplitAnalysis metric = SplitCalculator.calculate(run, UnitSystem.METRIC);
        equal(11, metric.splits().size(), "Kilometre splits cover an 11K run");
        equal(Duration.ofMinutes(6), metric.splits().get(0).duration(), "Easy first kilometre");
        equal(Duration.ofMinutes(5), metric.splits().get(2).duration(), "Faster middle kilometre");
        equal(Duration.ofSeconds(390), metric.splits().get(10).duration(), "Slower final kilometre");
        equal(null, metric.splits().get(0).averageHeartRate(), "Unavailable split HR remains null");
        equal(null, metric.splits().get(0).cadence(), "Unavailable cadence remains null");
        equal(null, metric.splits().get(0).elevationChangeMetres(), "Unavailable elevation remains null");
        near(300, metric.splits().get(2).secondsPerKilometre(), .000001, "Split pace derived");
        SplitAnalysis imperial = SplitCalculator.calculate(run, UnitSystem.IMPERIAL);
        equal(7, imperial.splits().size(), "Mile splits include final partial");
        check(imperial.splits().get(6).partial(), "Last incomplete mile labelled partial");
        near(1_343.936, imperial.splits().get(6).distanceMetres(), .000001, "Partial mile retains actual distance");

        List<DistanceSample> metrics = uniform(2_500, 5, 10);
        List<DistanceSample> enriched = new ArrayList<>();
        for (DistanceSample sample : metrics) enriched.add(new DistanceSample(sample.elapsed(), sample.distanceMetres(), 150.0, 170.0, -10 + sample.distanceMetres() / 100, false));
        ActivityRecord enrichedRun = activity("enriched", 2_500.0, Duration.ofSeconds(500), enriched, null);
        SplitAnalysis measured = SplitCalculator.calculate(enrichedRun, 1_000);
        equal(3, measured.splits().size(), "Partial final kilometre supported");
        equal(150.0, measured.splits().get(0).averageHeartRate(), "Mean from recorded HR samples");
        equal(170.0, measured.splits().get(0).cadence(), "Mean from recorded cadence samples");
        near(10, measured.splits().get(0).elevationChangeMetres(), .000001, "Net elevation change from available endpoints");
        near(500, measured.splits().get(2).distanceMetres(), .000001, "Partial split distance correct");
        ActivityLap lap = new ActivityLap("lap", START, START.plusSeconds(200), 1_000.0, "Rep 1", "prescribed-rep-1");
        ActivityRecord lapped = new ActivityRecord("lapped", enrichedRun.source(), START, START.plusSeconds(500), ZoneOffset.UTC, ZoneOffset.UTC,
                2_500.0, null, null, null, null, null, null, null, enrichedRun.samples(), Collections.singletonList(lap), "plan-1", null);
        equal(1, lapped.laps().size(), "Actual laps retained separately");
        equal(3, SplitCalculator.calculate(lapped, 1_000).splits().size(), "Distance splits do not overwrite laps");
        equal("prescribed-rep-1", lapped.laps().get(0).plannedSegmentId(), "Lap can reference planned segment without overwriting it");
        expect(UnsupportedOperationException.class, () -> lapped.laps().clear(), "Lap list immutable");
    }

    private static void progression() {
        List<RecordedResult> progression = new ArrayList<>();
        int[] times = {1_782, 1_696, 1_658, 1_612, 1_560};
        int[] months = {1, 3, 5, 9, 11};
        for (int index = 0; index < times.length; index++) progression.add(result("r" + index, times[index], LocalDate.of(2025, months[index], 1), ResultSource.MANUAL, null));
        Collections.reverse(progression);
        List<PersonalBestEntry> history = PersonalBestCalculator.history(progression);
        equal(5, history.size(), "Complete PB progression retained");
        equal(null, history.get(0).previousBest(), "First evidence has no invented previous PB");
        equal(null, history.get(0).improvement(), "First evidence has no invented improvement");
        equal(Duration.ofSeconds(86), history.get(1).improvement(), "Improvement amount exact");
        equal(Duration.ofSeconds(52), history.get(4).improvement(), "Latest improvement exact");
        equal(1, PersonalBestCalculator.current(progression).size(), "Current best separate from complete history");
        equal(Duration.ofMinutes(26), PersonalBestCalculator.current(progression).get(0).result().duration(), "Current PB exact");
        equal("5:12/km", PersonalBestCalculator.current(progression).get(0).pace(UnitSystem.METRIC), "PB pace uses stored distance");
        expect(UnsupportedOperationException.class, () -> history.clear(), "PB history immutable");
        progression.add(result("slower", 1_800, LocalDate.of(2025, 12, 1), ResultSource.MANUAL, null));
        equal(5, PersonalBestCalculator.history(progression).size(), "Slower evidence retained outside improvement timeline");

        RecordedResult rolling = result("gps", 1_440, LocalDate.of(2025, 4, 1), ResultSource.ROLLING, "race-run");
        RecordedResult official = result("chip", 1_500, LocalDate.of(2025, 4, 1), ResultSource.OFFICIAL_CHIP, "race-run");
        RecordedResult manual = result("manual", 1_490, LocalDate.of(2025, 4, 1), ResultSource.MANUAL, "race-run");
        List<RecordedResult> sources = Arrays.asList(rolling, manual, official);
        equal("chip", PersonalBestCalculator.current(sources).get(0).result().id(), "Official chip time authoritative over linked GPS/manual result");
        equal(3, sources.size(), "Authority calculation preserves underlying records");
        List<RecordedResult> sameDay = Arrays.asList(result("a", 1_600, LocalDate.of(2025, 1, 1), ResultSource.MANUAL, null), result("b", 1_500, LocalDate.of(2025, 1, 1), ResultSource.MANUAL, null));
        equal(1, PersonalBestCalculator.history(sameDay).size(), "Date-only evidence does not invent within-day progression");
        equal("b", PersonalBestCalculator.history(sameDay).get(0).result().id(), "Daily best deterministic regardless of input ordering");
        rejects(() -> PersonalBestCalculator.history(Arrays.asList(result("same", 1_500, LocalDate.of(2025, 1, 1), ResultSource.MANUAL, null), result("same", 1_600, LocalDate.of(2025, 1, 1), ResultSource.MANUAL, null))), "Conflicting duplicate IDs rejected");
        RecordedResult wrongDistance = new RecordedResult("wrong", new PerformanceDistance("5k", 4_000, "Incorrect"), Duration.ofMinutes(20), LocalDate.of(2025, 1, 2), null, ResultSource.MANUAL, null, null);
        rejects(() -> PersonalBestCalculator.history(Arrays.asList(official, wrongDistance)), "Distance code cannot silently change physical distance");
        equal("8:00/mi", PerformancePace.format(Duration.ofMinutes(8), PerformanceDistance.ONE_MILE, UnitSystem.IMPERIAL), "Mile PB pace exact");
    }

    private static void shoes() {
        Shoe shoe = new Shoe("shoe", "Brand", "Model", LocalDate.of(2026, 10, 1), null, false, null, true);
        List<ActivityRecord> activities = Arrays.asList(activity("a", 5_000.0, Duration.ofMinutes(30), Collections.emptyList(), "shoe"),
                activity("b", 5_500.0, Duration.ofMinutes(35), Collections.emptyList(), "shoe"),
                activity("unknown", null, Duration.ofMinutes(20), Collections.emptyList(), "shoe"),
                activity("different", 50_000.0, Duration.ofHours(5), Collections.emptyList(), "other"));
        ShoeMetrics metrics = ShoeMetricsCalculator.calculate(shoe, activities, CLOCK);
        equal(10_500.0, metrics.recordedDistanceMetres(), "Mileage uses assigned known distances only");
        equal(null, metrics.totalDistanceMetres(), "Missing earlier mileage does not become zero lifetime mileage");
        equal(3, metrics.runCount(), "Assigned runs counted even when distance unavailable");
        equal(2, metrics.runsWithKnownDistance(), "Known distance coverage explicit");
        equal(5_250.0, metrics.averageRunDistanceMetres(), "Average excludes missing distance rather than adding zero");
        equal(5_500.0, metrics.longestRunMetres(), "Longest assigned run exact");
        equal(20L, metrics.ageDays(), "Shoe age uses injected local date");
        equal(START, metrics.firstRecordedRun(), "First recorded run based on assignment history");
        check(metrics.wearGuidance().contains("incomplete"), "Missing distance explained");
        equal(0.0, ShoeMetricsCalculator.calculate(shoe, Collections.emptyList(), CLOCK).recordedDistanceMetres(), "No recorded use means zero recorded mileage, not invented activity");
        equal(null, ShoeMetricsCalculator.calculate(shoe, Collections.emptyList(), CLOCK).averageRunDistanceMetres(), "No runs means unavailable average");
        List<ActivityRecord> changed = new ArrayList<>(activities);
        changed.remove(0);
        equal(5_500.0, ShoeMetricsCalculator.calculate(shoe, changed, CLOCK).recordedDistanceMetres(), "Deletion/reassignment recomputes mileage without stale counters");
        Shoe highUse = new Shoe("shoe", "Brand", "Model", null, 600_000.0, true, null, false);
        ShoeMetrics high = ShoeMetricsCalculator.calculate(highUse, activities, CLOCK);
        equal(610_500.0, high.totalDistanceMetres(), "Manual starting mileage retained with recorded mileage");
        check(high.wearGuidance().contains("no universal replacement distance"), "Wear guidance avoids hard replacement rules");
        Shoe check = new Shoe("shoe", "", "Model", null, 400_000.0, false, null, false);
        check(ShoeMetricsCalculator.calculate(check, activities, CLOCK).wearGuidance().contains("Consider checking"), "Wear check begins around 400–500K");
        Shoe future = new Shoe("future", "", "Model", LocalDate.of(2030, 1, 1), null, false, null, false);
        equal(null, ShoeMetricsCalculator.calculate(future, Collections.emptyList(), CLOCK).ageDays(), "Invalid future purchase cannot create negative age");
        rejects(() -> new Shoe("s", "", "Model", null, -1.0, false, null, false), "Negative starting mileage rejected");
        rejects(() -> new Shoe("s", "", "Model", null, null, true, null, true), "Retired shoe cannot be default");
    }

    private static void milestones() {
        List<RecordedResult> result = Collections.singletonList(result("5k", 1_490, LocalDate.of(2025, 5, 1), ResultSource.MANUAL, null));
        equal(0, MilestoneEngine.calculate(false, Collections.singletonList(elevenK()), result).size(), "Milestones opt-in");
        List<Milestone> milestones = MilestoneEngine.calculate(true, Collections.singletonList(elevenK()), result);
        check(milestones.stream().anyMatch(m -> m.code().equals("sub_25_5k")), "Sub25 5K from explicit result");
        check(milestones.stream().anyMatch(m -> m.label().equals("First recorded 10K")), "First recorded distance from measured activity");
        check(milestones.stream().noneMatch(m -> m.code().equals("first_marathon")), "No invented marathon achievement");
        ActivityRecord impossible = activity("impossible", 42_195.0, Duration.ofSeconds(10), Collections.emptyList(), null);
        equal(0, MilestoneEngine.calculate(true, Collections.singletonList(impossible), Collections.emptyList()).size(), "Impossible activity cannot award distance milestone");
    }

    private static void analytics() {
        ActivityRecord unknown = activity("unknown", null, Duration.ofMinutes(20), Collections.emptyList(), null);
        List<RecordedWeek> weeks = ActivityAnalytics.weekly(Collections.singletonList(unknown), LocalDate.of(2026, 10, 21), ZoneOffset.UTC, 12);
        equal(12, weeks.size(), "Twelve-week recorded-mileage series");
        equal(null, weeks.get(11).distanceMetres(), "Unknown distance is not a zero-mileage week");
        equal(1, weeks.get(11).missingDistanceCount(), "Unknown distance count exposed");
        equal(0.0, weeks.get(0).distanceMetres(), "No retained runs means zero recorded mileage, without claiming no exercise occurred");
        List<RecordedWeek> mixed = ActivityAnalytics.weekly(Arrays.asList(unknown, elevenK()), LocalDate.of(2026, 10, 21), ZoneOffset.UTC, 1);
        equal(11_000.0, mixed.get(0).distanceMetres(), "Partial known mileage retained");
        equal(1, mixed.get(0).missingDistanceCount(), "Partial coverage remains explicit");
        equal(1, mixed.get(0).runningDays(), "Multiple sessions in one day do not fabricate extra running days");
    }

    private static RecordedResult result(String id, long seconds, LocalDate date, ResultSource source, String activityId) {
        return new RecordedResult(id, PerformanceDistance.FIVE_K, Duration.ofSeconds(seconds), date, null, source, activityId, null);
    }

    private static ActivityRecord elevenK() {
        List<DistanceSample> points = new ArrayList<>();
        for (int metres = 0; metres <= 11_000; metres += 50) {
            double seconds = metres <= 2_000 ? metres * .36 : metres <= 7_000 ? 720 + (metres - 2_000) * .30 : 2_220 + (metres - 7_000) * .39;
            points.add(point(seconds, metres));
        }
        return activity("long", 11_000.0, Duration.ofSeconds(3_780), points, null);
    }

    private static List<DistanceSample> uniform(double metres, double speed, double sampleSeconds) {
        List<DistanceSample> points = new ArrayList<>();
        double duration = metres / speed;
        for (double seconds = 0; seconds < duration; seconds += sampleSeconds) points.add(point(seconds, seconds * speed));
        points.add(point(duration, metres));
        return points;
    }

    private static DistanceSample point(double seconds, double metres) {
        return new DistanceSample(Duration.ofNanos(Math.round(seconds * 1_000_000_000.0)), metres, null, null, null, false);
    }
    private static ActivityRecord activity(String id, Double metres, Duration duration, List<DistanceSample> points, String shoe) {
        return new ActivityRecord(id, new SourceIdentity("test", id, null), START, START.plus(duration), ZoneOffset.UTC, ZoneOffset.UTC,
                metres, null, null, null, null, null, null, null, points, Collections.emptyList(), null, shoe);
    }
    private static BestEffort effort(BestEffortAnalysis analysis, PerformanceDistance distance) {
        BestEffort result = effortOrNull(analysis, distance);
        if (result == null) throw new AssertionError("Expected an effort for " + distance.code());
        return result;
    }
    private static BestEffort effortOrNull(BestEffortAnalysis analysis, PerformanceDistance distance) {
        for (BestEffort effort : analysis.efforts()) if (effort.distance().code().equals(distance.code())) return effort;
        return null;
    }
    private static double seconds(Duration duration) { return duration.getSeconds() + duration.getNano() / 1_000_000_000.0; }
    private static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    private static void equal(Object expected, Object actual, String name) { check(Objects.equals(expected, actual), name + ": expected " + expected + ", got " + actual); }
    private static void near(double expected, double actual, double tolerance, String name) { check(Math.abs(expected - actual) <= tolerance, name + ": expected " + expected + ", got " + actual); }
    private static void rejects(Runnable operation, String name) { expect(IllegalArgumentException.class, operation, name); }
    private static void expect(Class<? extends Throwable> type, Runnable operation, String name) {
        checks++;
        try { operation.run(); } catch (Throwable failure) { if (type.isInstance(failure)) return; throw new AssertionError(name + ": wrong exception", failure); }
        throw new AssertionError(name + ": expected " + type.getSimpleName());
    }
}
