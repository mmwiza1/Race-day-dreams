package org.racedaydreams.domain.execution;

import org.racedaydreams.domain.recording.RunMetricsEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Session-local cue arbitration, separate from Android audio and the recorder. Metrics use the
 * recorder's active clock. Android state announcements retain their separate monotonic clock in the
 * service's audio adapter.
 */
public final class RecordingCueCoordinator {
    public record RaceTarget(double metres, Long targetSeconds) {}

    public record Update(CoachCues.Cue selected, String paceEvent, String raceContext) {}

    private final CoachCues paceCues = new CoachCues();
    private int lastSplit;
    private boolean gpsWasLost;
    private boolean hadHeartRate;
    private boolean raceHalf;
    private boolean raceFive;

    /** Initial connection failure is not announced as loss of a connected sensor. */
    public boolean heartRateConnection(boolean connected) {
        if (connected) {
            hadHeartRate = true;
        } else if (hadHeartRate) {
            hadHeartRate = false;
            return true;
        }
        return false;
    }

    public Update update(
            RunMetricsEngine.Status metrics,
            WorkoutExecutionCoordinator.Update execution,
            List<WorkoutExecutionEngine.Step> steps,
            RaceTarget race,
            boolean hilly,
            long sustainedMillis,
            boolean miles,
            boolean announceSplits) {
        var candidates = new ArrayList<CoachCues.Cue>();
        long active = metrics.movingMillis();
        boolean lost = metrics.gps().contains("interrupted");
        if (lost && !gpsWasLost) {
            candidates.add(
                    new CoachCues.Cue(
                            "gps-lost",
                            "GPS interrupted. Distance is incomplete; continue by effort.",
                            0,
                            active,
                            false));
        }
        gpsWasLost = lost;

        String raceContext = null;
        if (race != null) {
            if (race.targetSeconds() == null) {
                raceContext = "Race elapsed time includes pauses.";
            } else {
                double margin =
                        race.targetSeconds() * metrics.metres() / race.metres()
                                - metrics.elapsedMillis() / 1000.0;
                raceContext =
                        (int) Math.abs(margin)
                                + " sec "
                                + (margin >= 0 ? "ahead of" : "behind")
                                + " chosen average, relative to recorded GPS distance. Elapsed"
                                + " timing includes pauses.";
            }
            if (!raceHalf && metrics.metres() >= race.metres() / 2) {
                raceHalf = true;
                candidates.add(
                        new CoachCues.Cue(
                                "race-half",
                                "Halfway by recorded distance. Keep your effort controlled.",
                                2,
                                active,
                                false));
            }
            if (!raceFive && race.metres() > 10000 && metrics.metres() >= race.metres() - 5000) {
                raceFive = true;
                candidates.add(
                        new CoachCues.Cue(
                                "race-five",
                                "Five kilometres remaining by recorded distance.",
                                2,
                                active,
                                false));
            }
        }

        String paceEvent = null;
        if (execution != null) {
            for (var event : execution.newEvents()) {
                if (event.type().equals("STEP_STARTED") && event.step() < steps.size()) {
                    var step = steps.get(event.step());
                    candidates.add(
                            new CoachCues.Cue(
                                    "step-" + event.step(),
                                    "Step "
                                            + (event.step() + 1)
                                            + " of "
                                            + steps.size()
                                            + ". "
                                            + step.label(),
                                    1,
                                    active,
                                    true));
                } else if (event.type().equals("WORKOUT_COMPLETED")) {
                    candidates.add(
                            new CoachCues.Cue(
                                    "complete",
                                    "Workout complete. Recording continues until you finish and"
                                        + " save.",
                                    1,
                                    active,
                                    true));
                }
            }
            if (!hilly) {
                var input =
                        new WorkoutExecutionEngine.Input(
                                metrics.metres(),
                                active,
                                metrics.elapsedMillis(),
                                metrics.secondsPerKm(),
                                metrics.hasReliablePace(),
                                metrics.paused());
                var pace = paceCues.pace(execution.state(), input, sustainedMillis);
                if (pace != null) {
                    candidates.add(pace);
                    paceEvent =
                            pace.id().startsWith("pace-in")
                                    ? "TARGET_RANGE_REENTERED"
                                    : "TARGET_RANGE_EXITED";
                }
            }
        }

        double unit = miles ? 1609.344 : 1000.0;
        int split = (int) (metrics.metres() / unit);
        if (split > lastSplit) {
            lastSplit = split;
            if (announceSplits) {
                candidates.add(
                        new CoachCues.Cue(
                                "split-" + split,
                                split + (miles ? " miles" : " kilometres") + " recorded.",
                                5,
                                active,
                                false));
            }
        }
        return new Update(CoachCues.select(candidates, active), paceEvent, raceContext);
    }
}
