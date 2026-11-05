package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

/** Deterministic lifecycle/clock, warm-up provenance and grouped actual-set regressions. */
public final class GymToolsTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        timer();
        warmups();
        supersets();
        System.out.println("GymToolsTestSuite: " + checks + " checks passed.");
    }

    private static GymRestTimer.Clock clock(long ms) {
        return new GymRestTimer.Clock("boot-one", ms);
    }

    private static void timer() {
        var t = GymRestTimer.start("workout", "set-one", 180, clock(1000));
        eq(179L, t.observe(clock(2000)).secondsRemaining(), "monotonic second");
        eq(180L, t.secondsRemaining(), "observation immutable");
        var paused = t.pause(clock(31000));
        eq(
                150L,
                paused.observe(clock(9_000_000)).secondsRemaining(),
                "locked/paused countdown frozen");
        eq(
                149L,
                paused.resume(clock(9_000_000)).observe(clock(9_001_000)).secondsRemaining(),
                "resume new anchor");
        eq(
                GymRestTimer.Phase.COMPLETE,
                t.observe(clock(999999)).phase(),
                "navigation/lock deadline");
        eq(
                GymRestTimer.Phase.INTERRUPTED,
                t.observe(new GymRestTimer.Clock("new-boot", 2000)).phase(),
                "reboot never bridges");
        eq(GymRestTimer.Phase.INTERRUPTED, t.observe(clock(500)).phase(), "clock regression");
        eq(180L, t.skip(clock(2000)).reset(clock(3000)).secondsRemaining(), "skip/reset");
        eq(209L, t.adjust(30, clock(2000)).secondsRemaining(), "adjust elapsed rest");
        eq(0L, t.adjust(-180, clock(2000)).secondsRemaining(), "no negative rest");
        eq(0L, GymRestTimer.start("w", "s", 0, clock(0)).secondsRemaining(), "explicit no rest");
        rejects(() -> GymRestTimer.start("w", "s", 1801, clock(0)), "bounded rest");
        rejects(
                () -> new GymRestTimer.Preferences(-1, true, false, false, false),
                "invalid default");
        rejects(
                () -> GymRestTimer.start("w", "s", 180, clock(Long.MAX_VALUE - 100)),
                "deadline overflow");
        for (int seconds = 0; seconds <= 1800; seconds += 15) {
            var original = GymRestTimer.start("w", "s", seconds, clock(0));
            long previous = seconds;
            for (int ms = 0; ms <= 2_000_000; ms += 31001) {
                long remaining = original.observe(clock(ms)).secondsRemaining();
                check(remaining >= 0 && remaining <= previous, "bounded monotonic long countdown");
                previous = remaining;
            }
        }
    }

    private static GymWorkoutDefinition.Slot slot(
            String id, String exercise, int sets, String group) {
        return new GymWorkoutDefinition.Slot(
                id,
                ExerciseCatalog.definition(exercise).exercise(),
                new GymWorkoutDefinition.Target(sets, 5, 8, null, null, null, 90, 80.0),
                group);
    }

    private static StrengthSession history(Instant at, boolean warm, Double effort) {
        var exercise = ExerciseCatalog.definition("bench-press").exercise();
        return new StrengthSession(
                "h",
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                List.of(
                        new StrengthSession.ExerciseSets(
                                exercise,
                                List.of(
                                        new StrengthSession.SetEntry("1", 6, 80, effort, warm),
                                        new StrengthSession.SetEntry("2", 6, 80, effort, warm)))));
    }

    private static void warmups() {
        var s = slot("bench", "bench-press", 3, null);
        var valid =
                GymWarmup.suggest(
                        s,
                        List.of(history(NOW.minusSeconds(86400), false, 7.0)),
                        List.of(),
                        GymProgramme.Experience.BEGINNER,
                        NOW);
        check(!valid.ramps().isEmpty(), "recent exact working evidence supports optional ramp");
        for (var ramp : valid.ramps())
            check(
                    ramp.kilograms() < 80 && ramp.repetitions() <= 6,
                    "light low-fatigue preparation");
        for (var h :
                List.of(
                        history(NOW.minus(Duration.ofDays(29)), false, 7.0),
                        history(NOW, false, null),
                        history(NOW, true, 7.0),
                        history(NOW.plusSeconds(1), false, 7.0)))
            check(
                    GymWarmup.suggest(
                                    s, List.of(h), List.of(), GymProgramme.Experience.BEGINNER, NOW)
                            .ramps()
                            .isEmpty(),
                    "stale/missing effort/warmup/future no kg");
        check(
                GymWarmup.suggest(s, List.of(), List.of(), GymProgramme.Experience.BEGINNER, NOW)
                        .ramps()
                        .isEmpty(),
                "no evidence general preparation");
        var actual =
                new GymExecutionSet(
                        s.id(),
                        s.exercise(),
                        new StrengthSession.SetEntry("today", 5, 70, 7.0, false));
        check(
                GymWarmup.suggest(
                                s,
                                List.of(history(NOW, false, 7.0)),
                                List.of(actual),
                                GymProgramme.Experience.BEGINNER,
                                NOW)
                        .ramps()
                        .isEmpty(),
                "no redundant ramp after working set");
    }

    private static GymExecution.Tick tick(long ms) {
        return new GymExecution.Tick("clock", ms, NOW.plusMillis(ms));
    }

    private static void supersets() {
        var slots =
                GymSupersets.group(
                        List.of(
                                slot("bench", "bench-press", 2, null),
                                slot("row", "barbell-row", 2, null)),
                        0,
                        2,
                        "pair");
        var definition = new GymWorkoutDefinition("d", 1, NOW, "Paired workout", slots);
        var e =
                GymExecutionEngine.start(
                        "w", GymExecution.Origin.CUSTOM, definition, null, ZoneOffset.UTC, tick(0));
        eq("bench", GymSupersets.next(e.snapshot()).slotId(), "first movement");
        e.saveSet("bench", new StrengthSession.SetEntry("warm", 5, 20, null, true), tick(1));
        eq(
                "bench",
                GymSupersets.next(e.snapshot()).slotId(),
                "warmups never advance working round");
        e.saveSet("bench", new StrengthSession.SetEntry("b1", 5, 80, 7.0, false), tick(2));
        eq("row", GymSupersets.next(e.snapshot()).slotId(), "next movement before round rest");
        e.selectExercise("row", tick(3));
        e.saveSet("row", new StrengthSession.SetEntry("r1", 5, 50, 7.0, false), tick(4));
        check(GymSupersets.next(e.snapshot()).restBeforeNext(), "rest between rounds");
        eq(2, GymSupersets.next(e.snapshot()).round(), "round two");
        e.replaceExercise(
                "bench", ExerciseCatalog.definition("dumbbell-bench-press").exercise(), tick(5));
        eq(
                2,
                GymSupersets.next(e.snapshot()).round(),
                "replacement retains slot's completed round evidence");
        var before = e.snapshot().actualSets();
        e.skipExercise("bench", tick(6));
        eq("row", GymSupersets.next(e.snapshot()).slotId(), "skipped movement stays skippable");
        e.breakSuperset("row", tick(7));
        eq(null, GymSupersets.next(e.snapshot()), "break into ordinary movements");
        eq(before, e.snapshot().actualSets(), "no changed historical actuals");
        eq(definition, e.snapshot().prescription(), "frozen original prescription retained");
        var unequal =
                new GymWorkoutDefinition(
                        "unequal",
                        1,
                        NOW,
                        "Unequal rounds",
                        List.of(
                                slot("bench", "bench-press", 1, "group"),
                                slot("row", "barbell-row", 3, "group")));
        var u =
                GymExecutionEngine.start(
                        "u", GymExecution.Origin.CUSTOM, unequal, null, ZoneOffset.UTC, tick(0));
        u.saveSet("bench", new StrengthSession.SetEntry("ub", 5, 40, 7.0, false), tick(1));
        u.selectExercise("row", tick(2));
        u.saveSet("row", new StrengthSession.SetEntry("ur", 5, 30, 7.0, false), tick(3));
        eq("row", GymSupersets.next(u.snapshot()).slotId(), "finished movement need not repeat");
        check(GymSupersets.next(u.snapshot()).restBeforeNext(), "unequal rounds retain rest");
        rejects(() -> GymSupersets.group(slots, 0, 2, "again"), "do not corrupt existing group");
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static void eq(Object a, Object b, String m) {
        check(Objects.equals(a, b), m + ": " + a + " != " + b);
    }

    private static void rejects(Runnable action, String m) {
        try {
            action.run();
            throw new AssertionError(m);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
