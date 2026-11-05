package org.racedaydreams.domain.strength;

import java.time.Instant;
import java.util.*;

/** Measured and estimated records retain exact source actuals. No prescription is an input. */
public final class StrengthRecords {
    private StrengthRecords() {}

    public enum Kind {
        HEAVIEST_LOAD,
        EXACT_REPETITIONS_LOAD,
        REPETITIONS_AT_LOAD,
        BODYWEIGHT_REPETITIONS,
        DURATION,
        DISTANCE,
        ESTIMATED_ONE_REP_MAX
    }

    public record Key(
            String exerciseId,
            StrengthExercise.Mode mode,
            StrengthExercise.MeasurementProfile measurement,
            Kind kind,
            Integer repetitions,
            Double load) {}

    public record Point(
            Key key,
            double value,
            StrengthExercise exercise,
            StrengthSession.SetEntry actual,
            String workoutId,
            Instant at,
            java.time.LocalDate date) {
        public boolean estimated() {
            return key.kind() == Kind.ESTIMATED_ONE_REP_MAX;
        }
    }

    public static List<Point> progression(List<StrengthSession> sessions, Instant asOf) {
        var sorted = new ArrayList<>(sessions);
        sorted.sort(
                Comparator.comparing(StrengthSession::startedAt)
                        .thenComparing(StrengthSession::id));
        var seen = new HashMap<String, StrengthSession>();
        var best = new LinkedHashMap<Key, Double>();
        var result = new ArrayList<Point>();
        for (var session : sorted) {
            var old = seen.putIfAbsent(session.id(), session);
            if (old != null) {
                if (!old.equals(session))
                    throw new IllegalArgumentException("Conflicting actual workout evidence.");
                continue;
            }
            if (session.startedAt().isAfter(asOf)) continue;
            for (var block : session.exercises())
                for (var set : block.sets()) {
                    if (set.warmUp()) continue;
                    var e = block.exercise();
                    Double comparableLoad =
                            e.measurement().usesLoad()
                                    ? Math.rint(set.kilograms() * 1_000_000) / 1_000_000
                                    : null;
                    if (e.measurement().usesReps()) {
                        if (e.mode() == StrengthExercise.Mode.BODYWEIGHT)
                            award(
                                    result,
                                    best,
                                    e,
                                    set,
                                    session,
                                    Kind.BODYWEIGHT_REPETITIONS,
                                    null,
                                    null,
                                    set.reps());
                        else {
                            award(
                                    result,
                                    best,
                                    e,
                                    set,
                                    session,
                                    Kind.HEAVIEST_LOAD,
                                    null,
                                    null,
                                    set.kilograms());
                            award(
                                    result,
                                    best,
                                    e,
                                    set,
                                    session,
                                    Kind.EXACT_REPETITIONS_LOAD,
                                    set.reps(),
                                    null,
                                    set.kilograms());
                            award(
                                    result,
                                    best,
                                    e,
                                    set,
                                    session,
                                    Kind.REPETITIONS_AT_LOAD,
                                    null,
                                    comparableLoad,
                                    set.reps());
                            // One actual repetition is a measured load, never another estimated
                            // record.
                            Double estimate =
                                    set.reps() >= 2
                                            ? StrengthAnalytics.estimatedOneRepMax(e, set)
                                            : null;
                            if (estimate != null)
                                award(
                                        result,
                                        best,
                                        e,
                                        set,
                                        session,
                                        Kind.ESTIMATED_ONE_REP_MAX,
                                        null,
                                        null,
                                        estimate);
                        }
                    } else if (e.measurement().usesDuration())
                        award(
                                result,
                                best,
                                e,
                                set,
                                session,
                                Kind.DURATION,
                                null,
                                comparableLoad,
                                set.durationSeconds());
                    else if (e.measurement().usesDistance())
                        award(
                                result,
                                best,
                                e,
                                set,
                                session,
                                Kind.DISTANCE,
                                null,
                                comparableLoad,
                                set.distanceMetres());
                }
        }
        return org.racedaydreams.domain.coaching.Immutable.copyList(result);
    }

    private static void award(
            List<Point> result,
            Map<Key, Double> best,
            StrengthExercise e,
            StrengthSession.SetEntry actual,
            StrengthSession session,
            Kind kind,
            Integer reps,
            Double load,
            double value) {
        var key = new Key(e.id(), e.mode(), e.measurement(), kind, reps, load);
        if (value <= 0 || value <= best.getOrDefault(key, -1.0) + 0.0000001) return;
        best.put(key, value);
        result.add(
                new Point(
                        key,
                        value,
                        e,
                        actual,
                        session.id(),
                        session.startedAt(),
                        session.startedAt().atOffset(session.offset()).toLocalDate()));
    }

    public static List<Point> best(List<Point> history) {
        var best = new LinkedHashMap<Key, Point>();
        for (var point : history)
            best.merge(point.key(), point, (a, b) -> b.value() > a.value() ? b : a);
        return org.racedaydreams.domain.coaching.Immutable.copyList(best.values());
    }

    public static String estimateExplanation() {
        return "Epley estimate: recorded load × (1 + repetitions ÷ 30), for eligible external-load"
                + " working sets of 2–10 repetitions only. It is not a measured lift or a"
                + " promise. Technique, effort and individual differences affect the estimate;"
                + " no maximal test is required.";
    }
}
