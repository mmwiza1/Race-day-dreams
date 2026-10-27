package org.racedaydreams.domain.execution;

import org.racedaydreams.domain.WorkoutSegment;
import org.racedaydreams.domain.recording.RunMetricsEngine;

import java.util.List;

/** Recorder/execution orchestration regressions; no Android clock, audio or sensors. */
public final class RecordingCoordinationTestSuite {
    private static int checks;

    public static void main(String[] args) {
        executionEventsArePublishedOnceAndPreserved();
        manualActionsRetainActualBoundaries();
        criticalAndTransitionCuesWinWithoutBacklog();
        raceContextUsesElapsedAndMilestonesAreOnceOnly();
        paceGuidanceRequiresSustainedReliableActiveEvidence();
        hillPauseAndGpsEvidenceSuppressPace();
        splitPreferencesDoNotReplayMissedAnnouncements();
        heartRateLossOnlyFollowsARealConnection();
        System.out.println(
                "RecordingCoordinationTestSuite: "
                        + checks
                        + " checks passed (execution deltas, cue arbitration, race timing, sensor"
                        + " loss).");
    }

    private static void executionEventsArePublishedOnceAndPreserved() {
        var session =
                new WorkoutExecutionCoordinator(
                        List.of(
                                distance("Warm-up", 1000),
                                distance("Interval", 400),
                                timed("Recovery", 90000)));
        var start = session.update(status(0, 0, null));
        check(start.state().index() == 0);
        check(types(start).equals(List.of("WORKOUT_STARTED", "STEP_STARTED")));
        check(session.update(status(400, 120000, 300.0)).newEvents().isEmpty());
        var boundary = session.update(status(1100, 330000, 300.0));
        check(boundary.state().index() == 1);
        check(types(boundary).equals(List.of("STEP_COMPLETED", "STEP_STARTED")));
        check(Math.abs(boundary.newEvents().get(0).metres() - 1000) < .0001);
        check(boundary.newEvents().get(0).activeMillis() == 300000);
        check(session.update(status(1100, 330000, 300.0)).newEvents().isEmpty());
        var workDone = session.update(status(1400, 420000, 300.0));
        check(workDone.state().index() == 2);
        check(types(workDone).equals(List.of("STEP_COMPLETED", "STEP_STARTED")));
        var done = session.update(status(1600, 510000, 450.0));
        check(done.state().complete());
        check(types(done).equals(List.of("STEP_COMPLETED", "WORKOUT_COMPLETED")));
        check(session.update(status(1700, 540000, 300.0)).newEvents().isEmpty());
        check(session.events().size() == 8);
        check(start.newEvents().size() == 2);
        try {
            start.newEvents().clear();
            throw new AssertionError("Published evidence was mutable");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void manualActionsRetainActualBoundaries() {
        var open =
                new WorkoutExecutionEngine.Step(
                        "Run by effort", WorkoutSegment.Kind.RUN, null, null, null, null, 5.0);
        var session =
                new WorkoutExecutionCoordinator(
                        List.of(open, distance("Work", 500), distance("Cool-down", 1000)));
        session.update(status(0, 0, null));
        session.update(status(100, 30000, 300.0));
        session.completeOpen();
        var completed = session.update(status(100, 30000, 300.0));
        check(types(completed).equals(List.of("STEP_COMPLETED", "STEP_STARTED")));
        check(completed.newEvents().get(0).metres() == 100);
        check(completed.newEvents().get(0).activeMillis() == 30000);
        var paused =
                session.update(
                        new RunMetricsEngine.Status(
                                100, 240000, 30000, null, "GPS available · paused", true));
        check(paused.state().paused());
        check(paused.state().index() == 1);
        check(paused.newEvents().isEmpty());
        session.skip();
        var skipped =
                session.update(
                        new RunMetricsEngine.Status(
                                100, 240000, 30000, null, "GPS available · paused", true));
        check(types(skipped).equals(List.of("STEP_SKIPPED", "STEP_STARTED")));
        check(skipped.newEvents().get(0).activeMillis() == 30000);
        session.finishEarly();
        var early =
                session.update(
                        new RunMetricsEngine.Status(
                                100, 240000, 30000, null, "GPS available · paused", true));
        check(early.state().early());
        check(!early.state().complete());
        check(types(early).equals(List.of("WORKOUT_FINISHED_EARLY")));
        session.finishEarly();
        check(
                session.update(
                                new RunMetricsEngine.Status(
                                        100, 240000, 30000, null, "GPS available · paused", true))
                        .newEvents()
                        .isEmpty());
    }

    private static void criticalAndTransitionCuesWinWithoutBacklog() {
        var session =
                new WorkoutExecutionCoordinator(
                        List.of(distance("Warm-up", 1000), distance("Work", 2000)));
        var cues = new RecordingCueCoordinator();
        var start = status(0, 0, null);
        check(
                cues.update(
                                start,
                                session.update(start),
                                session.steps(),
                                null,
                                false,
                                45000,
                                false,
                                true)
                        .selected()
                        .id()
                        .equals("step-0"));
        var boundary = status(1000, 300000, 300.0);
        var transition =
                cues.update(
                        boundary,
                        session.update(boundary),
                        session.steps(),
                        null,
                        false,
                        45000,
                        false,
                        true);
        check(transition.selected().id().equals("step-1"));
        check(transition.selected().haptic());
        check(
                cues.update(
                                        boundary,
                                        session.update(boundary),
                                        session.steps(),
                                        null,
                                        false,
                                        45000,
                                        false,
                                        true)
                                .selected()
                        == null);
        var lost =
                new RunMetricsEngine.Status(
                        3000,
                        900000,
                        900000,
                        300.0,
                        "GPS interrupted · distance incomplete",
                        false);
        var finish = session.update(lost);
        check(finish.state().complete());
        var critical = cues.update(lost, finish, session.steps(), null, false, 45000, false, true);
        check(critical.selected().id().equals("gps-lost"));
        check(critical.selected().priority() == 0);
        check(
                cues.update(
                                        lost,
                                        session.update(lost),
                                        session.steps(),
                                        null,
                                        false,
                                        45000,
                                        false,
                                        true)
                                .selected()
                        == null);
        check(
                session.events().stream().filter(e -> e.type().equals("WORKOUT_COMPLETED")).count()
                        == 1);
    }

    private static void raceContextUsesElapsedAndMilestonesAreOnceOnly() {
        var cues = new RecordingCueCoordinator();
        var race = new RecordingCueCoordinator.RaceTarget(20000, 6000L);
        var halfway =
                new RunMetricsEngine.Status(10000, 3100000, 3000000, 300.0, "GPS available", false);
        var half = cues.update(halfway, null, List.of(), race, false, 45000, false, true);
        check(half.selected().id().equals("race-half"));
        check(half.raceContext().startsWith("100 sec behind"));
        check(
                cues.update(halfway, null, List.of(), race, false, 45000, false, true).selected()
                        == null);
        var five =
                cues.update(
                        status(15000, 4500000, 300.0),
                        null,
                        List.of(),
                        race,
                        false,
                        45000,
                        false,
                        true);
        check(five.selected().id().equals("race-five"));
        check(five.raceContext().startsWith("0 sec ahead of"));
        check(
                cues.update(
                                        status(15000, 4500000, 300.0),
                                        null,
                                        List.of(),
                                        race,
                                        false,
                                        45000,
                                        false,
                                        true)
                                .selected()
                        == null);
        var noTime =
                new RecordingCueCoordinator()
                        .update(
                                status(0, 0, null),
                                null,
                                List.of(),
                                new RecordingCueCoordinator.RaceTarget(5000, null),
                                false,
                                45000,
                                false,
                                true);
        check(noTime.raceContext().equals("Race elapsed time includes pauses."));
        check(noTime.selected() == null);
    }

    private static void paceGuidanceRequiresSustainedReliableActiveEvidence() {
        var session = targetSession();
        var cues = new RecordingCueCoordinator();
        update(cues, session, status(0, 0, 270.0), false);
        check(update(cues, session, status(100, 20000, 270.0), false).selected() == null);
        check(update(cues, session, status(120, 25000, 300.0), false).selected() == null);
        check(update(cues, session, status(150, 30000, 270.0), false).selected() == null);
        var sustained = update(cues, session, status(300, 60000, 270.0), false);
        check(sustained.selected().text().equals("Slightly fast. Ease off."));
        check(sustained.paceEvent().equals("TARGET_RANGE_EXITED"));
        check(update(cues, session, status(310, 61000, 270.0), false).selected() == null);
        var regained = update(cues, session, status(320, 62000, 300.0), false);
        check(regained.selected().text().equals("Back in target range."));
        check(regained.paceEvent().equals("TARGET_RANGE_REENTERED"));
        check(update(cues, session, status(330, 63000, 270.0), false).selected() == null);
        check(update(cues, session, status(500, 100000, 270.0), false).selected() == null);
    }

    private static void hillPauseAndGpsEvidenceSuppressPace() {
        for (String gps : List.of("GPS jump rejected", "GPS interrupted · distance incomplete")) {
            var session = targetSession();
            var cues = new RecordingCueCoordinator();
            update(cues, session, status(0, 0, 270.0), false);
            update(cues, session, status(100, 20000, 270.0), false);
            var bad = new RunMetricsEngine.Status(200, 100000, 100000, 270.0, gps, false);
            check(update(cues, session, bad, false).paceEvent() == null);
        }
        var hills = targetSession();
        var hillyCues = new RecordingCueCoordinator();
        update(hillyCues, hills, status(0, 0, 270.0), true);
        check(update(hillyCues, hills, status(100, 20000, 270.0), true).selected() == null);
        check(update(hillyCues, hills, status(400, 120000, 270.0), true).selected() == null);
        var paused = targetSession();
        var pausedCues = new RecordingCueCoordinator();
        update(pausedCues, paused, status(0, 0, 270.0), false);
        update(pausedCues, paused, status(100, 20000, 270.0), false);
        var result =
                update(
                        pausedCues,
                        paused,
                        new RunMetricsEngine.Status(
                                100, 200000, 20000, 270.0, "GPS available", true),
                        false);
        check(result.selected() == null);
        check(result.paceEvent() == null);
    }

    private static void splitPreferencesDoNotReplayMissedAnnouncements() {
        var cues = new RecordingCueCoordinator();
        check(
                cues.update(
                                        status(1000, 300000, 300.0),
                                        null,
                                        List.of(),
                                        null,
                                        false,
                                        45000,
                                        false,
                                        false)
                                .selected()
                        == null);
        check(
                cues.update(
                                        status(1000, 300000, 300.0),
                                        null,
                                        List.of(),
                                        null,
                                        false,
                                        45000,
                                        false,
                                        true)
                                .selected()
                        == null);
        var second =
                cues.update(
                        status(2000, 600000, 300.0),
                        null,
                        List.of(),
                        null,
                        false,
                        45000,
                        false,
                        true);
        check(second.selected().text().equals("2 kilometres recorded."));
        var miles = new RecordingCueCoordinator();
        check(
                miles.update(
                                        status(1609, 500000, 300.0),
                                        null,
                                        List.of(),
                                        null,
                                        false,
                                        45000,
                                        true,
                                        true)
                                .selected()
                        == null);
        var mile =
                miles.update(
                        status(1609.344, 500100, 300.0),
                        null,
                        List.of(),
                        null,
                        false,
                        45000,
                        true,
                        true);
        check(mile.selected().text().equals("1 miles recorded."));
        check(mile.raceContext() == null);
    }

    private static void heartRateLossOnlyFollowsARealConnection() {
        var cues = new RecordingCueCoordinator();
        check(!cues.heartRateConnection(false));
        check(!cues.heartRateConnection(true));
        check(cues.heartRateConnection(false));
        check(!cues.heartRateConnection(false));
        check(!cues.heartRateConnection(true));
        check(cues.heartRateConnection(false));
        var result =
                cues.update(
                        status(1000, 300000, 300.0),
                        null,
                        List.of(),
                        null,
                        false,
                        45000,
                        false,
                        true);
        check(result.selected().id().equals("split-1"));
    }

    private static RecordingCueCoordinator.Update update(
            RecordingCueCoordinator cues,
            WorkoutExecutionCoordinator session,
            RunMetricsEngine.Status status,
            boolean hilly) {
        return cues.update(
                status, session.update(status), session.steps(), null, hilly, 30000, false, true);
    }

    private static WorkoutExecutionCoordinator targetSession() {
        return new WorkoutExecutionCoordinator(
                List.of(
                        new WorkoutExecutionEngine.Step(
                                "Controlled work",
                                WorkoutSegment.Kind.RUN,
                                5000.0,
                                null,
                                295.0,
                                305.0,
                                7.0)));
    }

    private static WorkoutExecutionEngine.Step distance(String label, double metres) {
        return new WorkoutExecutionEngine.Step(
                label, WorkoutSegment.Kind.RUN, metres, null, null, null, null);
    }

    private static WorkoutExecutionEngine.Step timed(String label, long millis) {
        return new WorkoutExecutionEngine.Step(
                label, WorkoutSegment.Kind.RECOVERY, null, millis, null, null, null);
    }

    private static RunMetricsEngine.Status status(double metres, long active, Double pace) {
        return new RunMetricsEngine.Status(metres, active, active, pace, "GPS available", false);
    }

    private static List<String> types(WorkoutExecutionCoordinator.Update update) {
        return update.newEvents().stream().map(WorkoutExecutionEngine.Event::type).toList();
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Recording coordination check " + checks);
    }
}
