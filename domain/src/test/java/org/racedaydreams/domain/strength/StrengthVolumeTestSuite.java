package org.racedaydreams.domain.strength;

import java.time.*;
import java.util.*;

public final class StrengthVolumeTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    public static void main(String[] args) {
        var bench = ExerciseCatalog.definition("bench-press").exercise();
        var sets =
                List.of(
                        new StrengthSession.SetEntry("warm", 5, 20, null, true),
                        new StrengthSession.SetEntry("work", 6, 60, 7.0, false));
        var session = session("one", NOW, bench, sets);
        var weeks =
                StrengthVolume.weekly(
                        List.of(session, session), LocalDate.of(2026, 10, 26), 2, NOW);
        eq(0, weeks.get(0).workingSets());
        eq(1, weeks.get(1).workingSets());
        eq(1, weeks.get(1).workouts());
        var week = weeks.get(1);
        eq(0, week.unclassifiedSets());
        near(1, week.primarySetShares().values().stream().mapToDouble(Double::doubleValue).sum());
        eq(6L, week.exercises().get(0).repetitions());
        near(360, week.exercises().get(0).loadRepetitions());
        var custom =
                new StrengthExercise(
                        "custom",
                        "Custom hold",
                        StrengthExercise.Region.FULL,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.DURATION);
        var hold =
                session(
                        "hold",
                        NOW,
                        custom,
                        List.of(new StrengthSession.SetEntry("h", 0, 0, null, false, 30L, null)));
        var unknown =
                StrengthVolume.weekly(List.of(hold), LocalDate.of(2026, 11, 2), 1, NOW).get(0);
        eq(1, unknown.unclassifiedSets());
        eq(30L, unknown.exercises().get(0).seconds());
        eq(null, unknown.exercises().get(0).loadRepetitions());
        eq(0, unknown.primarySetShares().size());
        eq(0, unknown.secondaryExposures().size());
        var future = session("future", NOW.plusSeconds(1), bench, sets);
        eq(
                0,
                StrengthVolume.weekly(List.of(future), LocalDate.of(2026, 11, 2), 1, NOW)
                        .get(0)
                        .workouts());
        var midnight = session("offset", Instant.parse("2026-11-01T23:30:00Z"), bench, sets);
        var east =
                new StrengthSession(
                        midnight.id(),
                        midnight.startedAt(),
                        ZoneOffset.ofHours(2),
                        midnight.duration(),
                        null,
                        "",
                        midnight.exercises());
        eq(
                1,
                StrengthVolume.weekly(List.of(east), LocalDate.of(2026, 11, 2), 1, NOW)
                        .get(0)
                        .workingSets());
        rejects(
                () ->
                        StrengthVolume.weekly(
                                List.of(session, session("one", NOW.minusSeconds(1), bench, sets)),
                                LocalDate.of(2026, 11, 2),
                                1,
                                NOW));
        rejects(() -> StrengthVolume.weekly(List.of(), LocalDate.now(), 0, NOW));
        for (var e : ExerciseCatalog.exercises()) {
            var profile = e.measurement();
            var actual =
                    new StrengthSession.SetEntry(
                            "actual",
                            profile.usesReps() ? 6 : 0,
                            profile.usesLoad() ? 20 : 0,
                            null,
                            false,
                            profile.usesDuration() ? 30L : null,
                            profile.usesDistance() ? 25.0 : null);
            var row =
                    StrengthVolume.weekly(
                                    List.of(session("catalogue", NOW, e, List.of(actual))),
                                    LocalDate.of(2026, 11, 2),
                                    1,
                                    NOW)
                            .get(0);
            eq(1, row.workingSets());
            near(
                    1,
                    row.unclassifiedSets()
                            + row.primarySetShares().values().stream()
                                    .mapToDouble(Double::doubleValue)
                                    .sum());
            eq(
                    profile == StrengthExercise.MeasurementProfile.LOAD_REPS
                            && e.mode() == StrengthExercise.Mode.WEIGHTED,
                    row.exercises().get(0).loadRepetitions() != null);
        }
        // Six-month regular histories, with explicit gaps and mixed warm-up/working evidence.
        for (int athlete = 0; athlete < 3; athlete++) {
            var history = new ArrayList<StrengthSession>();
            int expected = 0;
            for (int day = 0; day < 182; day++)
                if (day % (athlete + 2) == 0 && day % 17 != 0) {
                    history.add(
                            session(
                                    "a" + athlete + "d" + day,
                                    NOW.minus(Duration.ofDays(day)),
                                    bench,
                                    sets));
                    expected++;
                }
            var result = StrengthVolume.weekly(history, LocalDate.of(2026, 4, 27), 28, NOW);
            eq(expected, result.stream().mapToInt(StrengthVolume.Week::workingSets).sum());
            eq(expected, result.stream().mapToInt(StrengthVolume.Week::workouts).sum());
            near(
                    expected,
                    result.stream()
                            .flatMap(w -> w.primarySetShares().values().stream())
                            .mapToDouble(Double::doubleValue)
                            .sum());
        }
        System.out.println("StrengthVolumeTestSuite: " + checks + " checks passed.");
    }

    private static StrengthSession session(
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

    private static void eq(Object a, Object b) {
        checks++;
        if (!Objects.equals(a, b)) throw new AssertionError(a + " != " + b);
    }

    private static void near(double a, double b) {
        checks++;
        if (Math.abs(a - b) > 1e-7) throw new AssertionError(a + " != " + b);
    }

    private static void rejects(Runnable r) {
        checks++;
        try {
            r.run();
            throw new AssertionError("Invalid evidence accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
