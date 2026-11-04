package org.racedaydreams.domain.recording;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Original synthetic traces; these assert evidence semantics, not universal device accuracy. */
public final class GpsReliabilityTestSuite {
    private static int checks;
    private static final List<String> failures = new ArrayList<>();

    private static void check(boolean value, String message) {
        checks++;
        if (!value) failures.add(message);
    }

    private static RunMetricsEngine.Fix fix(
            long time, double metres, Double accuracy, Double speed) {
        return new RunMetricsEngine.Fix(
                Instant.EPOCH.plusMillis(time),
                time,
                0,
                metres / 111194.9266,
                accuracy,
                null,
                null,
                speed);
    }

    private static void stationary() {
        var engine = new RunMetricsEngine(0, Instant.EPOCH);
        for (int i = 0; i <= 120; i++) engine.accept(fix(i * 1000L, i % 2 == 0 ? 2 : -2, 3.0, 0.0));
        check(
                engine.status(120000).metres() < 0.01,
                "Stationary four-metre jitter accumulated distance");
        check(
                engine.status(120000).secondsPerKm() == null,
                "Stationary jitter fabricated live pace");
        check(engine.pointCount() == 121, "Stationary original observations were discarded");
    }

    private static void oldFixAfterLoss() {
        var engine = new RunMetricsEngine(0, Instant.EPOCH);
        engine.accept(fix(0, 0, 3.0, 3.0));
        engine.accept(fix(1000, 3, 3.0, 3.0));
        engine.missingFix(20000, Instant.EPOCH.plusSeconds(20));
        check(
                !engine.accept(fix(2000, 6, 3.0, 3.0)),
                "Old observation reopened a declared GPS gap");
        check(engine.pointCount() == 2, "Stale observation entered retained evidence");
    }

    private static void sparseAutoPause() {
        var policy = new AutoPausePolicy();
        policy.observe(fix(0, 0, 3.0, 0.0));
        check(
                policy.observe(fix(60000, 0, 3.0, 0.0)) == AutoPausePolicy.Decision.NONE,
                "Auto-pause inferred continuous stillness across missing observations");
    }

    private static boolean deliver(
            RunMetricsEngine engine, RunMetricsEngine.Fix fix, long received) {
        return engine.qualifyDelivery(fix, received, Instant.EPOCH.plusMillis(received))
                && engine.accept(fix);
    }

    private static void receiptQuality() {
        var engine = new RunMetricsEngine(0, Instant.EPOCH);
        check(deliver(engine, fix(0, 0, 3.0, 3.0), 0), "Initial native observation rejected");
        check(deliver(engine, fix(1000, 3, 3.0, 3.0), 1500), "Ordinary delivery latency rejected");
        check(!deliver(engine, fix(1000, 3, 3.0, 3.0), 1600), "Duplicate accepted");
        check(!deliver(engine, fix(900, 3, 3.0, 3.0), 1700), "Out-of-order observation accepted");
        int eventCount = engine.eventCount();
        check(!deliver(engine, fix(900, 3, 3.0, 3.0), 1800), "Repeated out-of-order accepted");
        check(engine.eventCount() == eventCount, "Repeated rejection diagnostics not coalesced");
        check(!deliver(engine, fix(3000, 9, 3.0, 3.0), 2000), "Future observation accepted");
        check(
                deliver(engine, fix(2000, 6, 3.0, 3.0), 2100),
                "Future timestamp poisoned later valid fix");
        check(
                !deliver(engine, fix(2500, 7, 3.0, 3.0), 20000),
                "Old batch accepted as current evidence");
        engine.missingFix(20000, Instant.EPOCH.plusSeconds(20));
        check(!deliver(engine, fix(19500, 100, 3.0, 3.0), 20000), "Pre-loss batch reopened gap");
        check(deliver(engine, fix(21000, 100, 3.0, 3.0), 21000), "Reacquisition failed");
        check(
                Math.abs(engine.status(21000).metres() - 6) < 0.01,
                "Loss invented a distance bridge");
        check(engine.points().get(3).discontinuity(), "Reacquisition lacks discontinuity");
        check(engine.status(21000).gps().equals("GPS recovered"), "Recovery not explained");
        check(!engine.status(21000).hasReliablePace(), "Pace invented across reacquisition");
        check(
                !deliver(engine, fix(22000, 103, null, 3.0), 22000),
                "Missing native accuracy treated as good");
        check(!deliver(engine, fix(23000, 106, 80.0, 3.0), 23000), "Weak accuracy accepted");
        check(
                deliver(engine, fix(24000, 110, 3.0, 3.0), 24000),
                "Recovery after weak observation failed");
        check(
                engine.events().stream().anyMatch(e -> e.type().equals("GPS_FIX_STALE")),
                "Stale reason missing");
        check(
                engine.events().stream().anyMatch(e -> e.type().equals("GPS_FIX_ACCURACY")),
                "Accuracy reason missing");
        var firstSpeed = new RunMetricsEngine(0, Instant.EPOCH);
        check(!firstSpeed.accept(fix(0, 0, 3.0, 50.0)), "Implausible first speed anchored route");
        check(firstSpeed.pointCount() == 0, "Rejected first fix retained");
    }

    private static void movementAndNoise() {
        // Accurate continuous, walking and very slow movement must not share a stationary deadband.
        for (double speed : new double[] {0.3, 1.0, 3.0, 6.0}) {
            var engine = new RunMetricsEngine(0, Instant.EPOCH);
            for (int i = 0; i <= 600; i++)
                check(
                        engine.accept(fix(i * 1000L, i * speed, 3.0, speed)),
                        "Clean movement dropped");
            check(
                    Math.abs(engine.status(600000).metres() - 600 * speed) < 0.01,
                    "Continuous distance incorrect");
            check(
                    engine.status(600000).hasReliablePace() == (speed >= 1.0),
                    "Qualified pace-window evidence incorrect");
        }
        // Even an underreported speed cannot freeze genuine displacement forever.
        var slow = new RunMetricsEngine(0, Instant.EPOCH);
        for (int i = 0; i <= 600; i++) slow.accept(fix(i * 1000L, i * 0.2, 3.0, 0.0));
        check(
                Math.abs(slow.status(600000).metres() - 120) <= 4.01,
                "Stationary cluster swallowed slow displacement");
        var urban = new RunMetricsEngine(0, Instant.EPOCH);
        for (int i = 0; i <= 600; i++) {
            double offset = i % 2 == 0 ? 0.5 : -0.5;
            check(
                    urban.accept(fix(i * 1000L, i * 3 + offset, 5.0, 3.0)),
                    "Small urban noise rejected genuine movement");
        }
        check(
                Math.abs(urban.status(600000).metres() - 1800) < 1,
                "Mild urban noise inflated distance");
        double before = urban.status(600000).metres();
        check(!deliver(urban, fix(601000, 2800, 3.0, 3.0), 601000), "Good-accuracy spike accepted");
        check(!urban.status(601000).hasReliablePace(), "Spike allowed precise pace");
        check(
                deliver(urban, fix(602000, 1806.5, 3.0, 3.0), 602000),
                "Valid observation after spike rejected");
        check(
                Math.abs(urban.status(602000).metres() - before - 6) < 0.01,
                "Spike inflated retained distance");
        var imported = new RunMetricsEngine(0, Instant.EPOCH);
        check(imported.accept(fix(0, 0, null, null)), "Historic GPX requires live accuracy");
        check(imported.accept(fix(1000, 1, null, null)), "Historic GPX requires live receipt");
        check(imported.status(1000).metres() > 0.99, "Unknown speed falsely implies stationary");
    }

    private static void pausesClockAndLongTrace() {
        var engine = new RunMetricsEngine(0, Instant.EPOCH);
        engine.accept(fix(0, 0, 3.0, 3.0));
        engine.accept(fix(1000, 3, 3.0, 3.0));
        engine.pause(2000, Instant.EPOCH.plusSeconds(2));
        check(!engine.accept(fix(3000, 300, 3.0, 3.0)), "Paused location became distance");
        engine.resume(10000, Instant.EPOCH.minusSeconds(3600));
        check(!engine.accept(fix(9000, 400, 3.0, 3.0)), "Pre-resume batch accepted");
        engine.accept(fix(11000, 500, 3.0, 3.0));
        engine.accept(fix(12000, 503, 3.0, 3.0));
        check(Math.abs(engine.status(12000).metres() - 6) < 0.01, "Pause/resume bridged distance");
        check(
                engine.status(12000).movingMillis() == 4000,
                "Active duration followed wall-clock change");
        check(
                engine.status(12000).elapsedMillis() == 12000,
                "Elapsed duration followed wall-clock change");
        var longRun = new RunMetricsEngine(0, Instant.EPOCH);
        long time = 0;
        double distance = 0;
        double previous = 0;
        for (int i = 0; i < 14400; i++) {
            long step = i % 3 == 0 ? 500 : i % 3 == 1 ? 1000 : 1500;
            time += step;
            distance += step * .003;
            var observation = fix(time, distance, 3.0, 3.0);
            check(deliver(longRun, observation, time + 50), "Irregular long-run observation lost");
            var point = longRun.pointsFrom(longRun.pointCount() - 1).get(0);
            check(point.cumulativeMetres() >= previous, "Cumulative distance decreased");
            check(
                    point.fix().equals(observation),
                    "Recorded coordinates/time were smoothed or rewritten");
            previous = point.cumulativeMetres();
        }
        check(Math.abs(previous - (distance - 1.5)) < 0.01, "Four-hour distance drifted");
        check(longRun.pointCount() == 14400, "Four-hour trace truncated");
        var lastSample = longRun.samples().get(14399);
        check(
                Math.abs(lastSample.distanceMetres() - previous) < 0.000001,
                "Sample/stored cumulative distance disagree");
        check(
                longRun.status(time + 6051).gps().equals("GPS signal weak"),
                "Missing observations still claim ready");
        check(
                longRun.status(time + 6051).secondsPerKm() == null,
                "Old observations fabricate current pace");
    }

    private static void autoPauseContinuity() {
        var policy = new AutoPausePolicy();
        for (int i = 0; i <= 30; i++) {
            var decision = policy.observe(fix(i * 1000L, 0, 3.0, 0.0));
            check(
                    decision
                            == (i == 30
                                    ? AutoPausePolicy.Decision.PAUSE
                                    : AutoPausePolicy.Decision.NONE),
                    "Continuous stationary transition changed");
        }
        policy.observe(fix(31000, 3, 3.0, 3.0));
        check(
                policy.observe(fix(45000, 6, 3.0, 3.0)) == AutoPausePolicy.Decision.NONE,
                "Missing evidence resumed auto-pause");
        check(
                policy.observe(fix(50000, 21, 3.0, 3.0)) == AutoPausePolicy.Decision.RESUME,
                "Five seconds of fresh moving evidence failed to resume");
        policy.reset();
        policy.observe(fix(10000, 0, 3.0, 0.0));
        check(
                policy.observe(fix(9000, 0, 3.0, 0.0)) == AutoPausePolicy.Decision.NONE,
                "Out-of-order auto-pause transition");
        check(
                policy.observe(fix(40000, 0, 3.0, 0.0)) == AutoPausePolicy.Decision.NONE,
                "Out-of-order observation created stillness window");
    }

    public static void main(String[] args) {
        stationary();
        oldFixAfterLoss();
        sparseAutoPause();
        receiptQuality();
        movementAndNoise();
        pausesClockAndLongTrace();
        autoPauseContinuity();
        if (!failures.isEmpty()) throw new AssertionError(String.join("\n", failures));
        System.out.println("GpsReliabilityTestSuite: " + checks + " checks passed.");
    }
}
