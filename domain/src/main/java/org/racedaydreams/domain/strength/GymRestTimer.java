package org.racedaydreams.domain.strength;

import java.util.Objects;

/** Advisory monotonic countdown, independent of workout actuals and active exercise time. */
public record GymRestTimer(
        String executionId,
        String triggerId,
        Phase phase,
        String epoch,
        long observedMillis,
        long remainingMillis,
        long durationMillis) {
    public enum Phase {
        RUNNING,
        PAUSED,
        COMPLETE,
        SKIPPED,
        INTERRUPTED
    }

    public record Clock(String epoch, long millis) {
        public Clock {
            GymWorkoutDefinition.requireId(epoch, "rest clock epoch");
            if (millis < 0) throw new IllegalArgumentException("Invalid monotonic clock.");
        }
    }

    public record Preferences(
            int defaultSeconds,
            boolean suggestAfterSet,
            boolean notifications,
            boolean vibration,
            boolean audio) {
        public Preferences {
            if (defaultSeconds < 0 || defaultSeconds > 1800)
                throw new IllegalArgumentException("Choose 0–30 minutes of optional rest.");
        }

        public static Preferences defaults() {
            return new Preferences(90, true, false, false, false);
        }
    }

    public GymRestTimer {
        GymWorkoutDefinition.requireId(executionId, "rest workout");
        GymWorkoutDefinition.requireId(triggerId, "rest start");
        Objects.requireNonNull(phase);
        new Clock(epoch, observedMillis);
        if (observedMillis > Long.MAX_VALUE - durationMillis
                || durationMillis < 0
                || durationMillis > 3_600_000
                || remainingMillis < 0
                || remainingMillis > durationMillis
                || (phase == Phase.COMPLETE || phase == Phase.SKIPPED) && remainingMillis != 0)
            throw new IllegalArgumentException("Invalid rest duration/state.");
    }

    public static int suggestedSeconds(GymWorkoutDefinition.Slot slot, Preferences preferences) {
        return slot != null && slot.target() != null && slot.target().restSeconds() != null
                ? slot.target().restSeconds()
                : preferences.defaultSeconds();
    }

    public static GymRestTimer start(String execution, String trigger, int seconds, Clock now) {
        if (seconds < 0 || seconds > 1800)
            throw new IllegalArgumentException("Choose 0–30 minutes.");
        return new GymRestTimer(
                execution,
                trigger,
                seconds == 0 ? Phase.COMPLETE : Phase.RUNNING,
                now.epoch(),
                now.millis(),
                seconds * 1000L,
                seconds * 1000L);
    }

    /** Read-only observation; wall-clock changes never change remaining rest. */
    public GymRestTimer observe(Clock now) {
        Objects.requireNonNull(now);
        if (phase != Phase.RUNNING) return this;
        if (!epoch.equals(now.epoch()) || now.millis() < observedMillis)
            return copy(Phase.INTERRUPTED, now, remainingMillis, durationMillis);
        long remaining = Math.max(0, remainingMillis - (now.millis() - observedMillis));
        return copy(
                remaining == 0 ? Phase.COMPLETE : Phase.RUNNING, now, remaining, durationMillis);
    }

    public GymRestTimer pause(Clock now) {
        var current = observe(now);
        return current.phase == Phase.RUNNING
                ? current.copy(Phase.PAUSED, now, current.remainingMillis, durationMillis)
                : current;
    }

    /** Resuming an interruption is explicit; no unknown elapsed interval is invented. */
    public GymRestTimer resume(Clock now) {
        if (phase != Phase.PAUSED && phase != Phase.INTERRUPTED) return observe(now);
        return copy(
                remainingMillis == 0 ? Phase.COMPLETE : Phase.RUNNING,
                now,
                remainingMillis,
                durationMillis);
    }

    public GymRestTimer reset(Clock now) {
        return copy(
                durationMillis == 0 ? Phase.COMPLETE : Phase.RUNNING,
                now,
                durationMillis,
                durationMillis);
    }

    public GymRestTimer skip(Clock now) {
        return copy(Phase.SKIPPED, now, 0, durationMillis);
    }

    public GymRestTimer adjust(int seconds, Clock now) {
        if (seconds < -1800 || seconds > 1800)
            throw new IllegalArgumentException("Adjust up to30minutes.");
        var current = observe(now);
        long remaining =
                Math.max(0, Math.min(3_600_000, current.remainingMillis + seconds * 1000L));
        Phase next =
                remaining == 0
                        ? Phase.COMPLETE
                        : current.phase == Phase.PAUSED || current.phase == Phase.INTERRUPTED
                                ? current.phase
                                : Phase.RUNNING;
        return copy(next, now, remaining, Math.max(current.durationMillis, remaining));
    }

    public long secondsRemaining() {
        return (remainingMillis + 999) / 1000;
    }

    private GymRestTimer copy(Phase next, Clock now, long remaining, long duration) {
        return new GymRestTimer(
                executionId, triggerId, next, now.epoch(), now.millis(), remaining, duration);
    }
}
