package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

public final class StrengthRecordsTestSuite {
    static int checks;
    static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        var bench = ExerciseCatalog.definition("bench-press").exercise();
        var first =
                session(
                        "first",
                        NOW.minusSeconds(86400),
                        bench,
                        List.of(
                                set("warm", 1, 150, true),
                                set("a", 6, 80, false),
                                set("b", 4, 80, false)));
        var second =
                session(
                        "second",
                        NOW,
                        bench,
                        List.of(
                                set("c", 7, 80, false),
                                set("d", 1, 90, false),
                                set("e", 11, 70, false)));
        var single = session("single-only", NOW, bench, List.of(set("single", 1, 90, false)));
        eq(
                null,
                StrengthAnalytics.estimatedOneRepMax(
                        bench, single.exercises().get(0).sets().get(0)));
        check(
                StrengthAnalytics.progression(List.of(single)).stream()
                        .noneMatch(p -> p.category().equals("Estimated 1RM")));
        check(
                StrengthAnalytics.progression(List.of(single)).stream()
                        .anyMatch(p -> p.category().equals("Load × 1") && p.kilograms() == 90));
        var history = StrengthRecords.progression(List.of(second, first, first), NOW);
        var best = StrengthRecords.best(history);
        var heaviest =
                best.stream()
                        .filter(p -> p.key().kind() == StrengthRecords.Kind.HEAVIEST_LOAD)
                        .findFirst()
                        .orElseThrow();
        eq(90.0, heaviest.value());
        eq("second", heaviest.workoutId());
        eq("d", heaviest.actual().id());
        eq(false, heaviest.estimated());
        eq(
                7.0,
                best.stream()
                        .filter(
                                p ->
                                        p.key().kind() == StrengthRecords.Kind.REPETITIONS_AT_LOAD
                                                && p.key().load() == 80.0)
                        .findFirst()
                        .orElseThrow()
                        .value());
        for (var p : history) {
            check(!p.actual().warmUp());
            if (p.estimated()) {
                check(p.actual().reps() >= 2 && p.actual().reps() <= 10);
                eq(bench, p.exercise());
            }
        }
        eq(
                0,
                StrengthRecords.progression(
                                List.of(
                                        session(
                                                "future",
                                                NOW.plusSeconds(1),
                                                bench,
                                                List.of(set("x", 1, 200, false)))),
                                NOW)
                        .size());
        var plank = ExerciseCatalog.definition("plank").exercise();
        var holds =
                StrengthRecords.best(
                        StrengthRecords.progression(
                                List.of(
                                        session(
                                                "hold",
                                                NOW,
                                                plank,
                                                List.of(
                                                        new StrengthSession.SetEntry(
                                                                "h", 0, 0, null, false, 45L,
                                                                null)))),
                                NOW));
        eq(1, holds.size());
        eq(StrengthRecords.Kind.DURATION, holds.get(0).key().kind());
        eq(45.0, holds.get(0).value());
        var carry =
                new StrengthExercise(
                        "carry",
                        "Carry",
                        StrengthExercise.Region.FULL,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_DISTANCE);
        var carries =
                StrengthRecords.best(
                        StrengthRecords.progression(
                                List.of(
                                        session(
                                                "carry",
                                                NOW,
                                                carry,
                                                List.of(
                                                        new StrengthSession.SetEntry(
                                                                "l", 0, 20, null, false, null,
                                                                50.0),
                                                        new StrengthSession.SetEntry(
                                                                "heavy", 0, 40, null, false, null,
                                                                30.0)))),
                                NOW));
        eq(2, carries.size());
        check(carries.stream().noneMatch(StrengthRecords.Point::estimated));
        var body = ExerciseCatalog.definition("push-up").exercise();
        var bw =
                StrengthRecords.best(
                        StrengthRecords.progression(
                                List.of(
                                        session(
                                                "body",
                                                NOW,
                                                body,
                                                List.of(
                                                        new StrengthSession.SetEntry(
                                                                "reps", 15, 0, null, false)))),
                                NOW));
        eq(1, bw.size());
        eq(StrengthRecords.Kind.BODYWEIGHT_REPETITIONS, bw.get(0).key().kind());
        eq(
                List.of(set("warm", 1, 150, true), set("a", 6, 80, false), set("b", 4, 80, false)),
                first.exercises().get(0).sets());
        System.out.println("StrengthRecordsTestSuite: " + checks + " checks passed.");
    }

    static StrengthSession.SetEntry set(String id, int reps, double kg, boolean warm) {
        return new StrengthSession.SetEntry(id, reps, kg, null, warm);
    }

    static StrengthSession session(
            String id, Instant at, StrengthExercise e, List<StrengthSession.SetEntry> sets) {
        return new StrengthSession(
                id,
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                List.of(new StrengthSession.ExerciseSets(e, sets)));
    }

    static void eq(Object a, Object b) {
        checks++;
        if (!Objects.equals(a, b)) throw new AssertionError(a + " != " + b);
    }

    static void check(boolean yes) {
        checks++;
        if (!yes) throw new AssertionError("Invalid record evidence");
    }
}
