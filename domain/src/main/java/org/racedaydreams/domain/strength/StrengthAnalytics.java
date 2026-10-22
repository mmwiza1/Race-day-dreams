package org.racedaydreams.domain.strength;

import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

/** Recomputed from retained sets after edits/deletions: old PBs cannot survive deleted evidence. */
public final class StrengthAnalytics {
    private StrengthAnalytics() { }
    public enum Involvement { LOW, MODERATE, HIGH }
    public record Context(StrengthExercise.Region region, String effort, Involvement lowerBody, List<String> reasons) { }
    public record Record(String exerciseId, String category, double value, double kilograms, int reps,
            Instant at, String sessionId, String setId, String notes) { }
    public static Context context(StrengthSession session) {
        int lower = 0, upper = 0; boolean hard = session.rpe() != null && session.rpe() >= 7;
        for (var e : session.exercises()) for (var set : e.sets()) if (!set.warmUp()) {
            if (e.exercise().region() != StrengthExercise.Region.UPPER) lower++;
            if (e.exercise().region() != StrengthExercise.Region.LOWER) upper++;
            if (set.rpe() != null && set.rpe() >= 8) hard = true;
        }
        var region = lower == 0 ? StrengthExercise.Region.UPPER : upper == 0 ? StrengthExercise.Region.LOWER : StrengthExercise.Region.FULL;
        var involvement = lower >= 3 && hard ? Involvement.HIGH : lower > 0 ? Involvement.MODERATE : Involvement.LOW;
        return new Context(region, hard ? "Hard" : session.rpe() == null ? "Effort not recorded" : session.rpe() <= 3 ? "Light" : "Moderate",
                involvement, List.of(lower + " working sets involved the lower body.",
                "Effort uses recorded session/set RPE; load alone does not establish fatigue. This is coaching context, not a physiological measurement."));
    }
    public static Double sessionLoad(StrengthSession session) {
        return session.rpe() == null ? null : session.duration().toMillis() / 60000.0 * session.rpe();
    }
    /** Epley estimate, restricted to 2–10 reps; a single rep is recorded load, not a prediction. */
    public static Double estimatedOneRepMax(StrengthExercise exercise, StrengthSession.SetEntry set) {
        if (!exercise.estimateOneRepMax() || exercise.mode() != StrengthExercise.Mode.WEIGHTED
                || set.warmUp() || set.reps() > 10 || set.kilograms() <= 0) return null;
        return set.reps() == 1 ? set.kilograms() : set.kilograms() * (1 + set.reps() / 30.0);
    }
    public static List<Record> progression(List<StrengthSession> sessions) {
        var sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparing(StrengthSession::startedAt).thenComparing(StrengthSession::id));
        Map<String, Double> best = new HashMap<>(); var history = new ArrayList<Record>();
        for (var session : sorted) for (var e : session.exercises()) for (var set : e.sets()) if (!set.warmUp()) {
            if (e.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT) award(history, best, e.exercise(), "Bodyweight reps", set.reps(), set, session);
            else {
                String prefix = e.exercise().mode() == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT ? "Added load" : "Load";
                award(history, best, e.exercise(), prefix, set.kilograms(), set, session);
                if (List.of(1, 3, 5, 8, 10).contains(set.reps()))
                    award(history, best, e.exercise(), prefix + " × " + set.reps(), set.kilograms(), set, session);
                Double estimate = estimatedOneRepMax(e.exercise(), set);
                if (estimate != null) award(history, best, e.exercise(), "Estimated 1RM", estimate, set, session);
            }
        }
        return List.copyOf(history);
    }
    private static void award(List<Record> history, Map<String, Double> best, StrengthExercise e, String category,
            double value, StrengthSession.SetEntry set, StrengthSession session) {
        String key = e.id() + ":" + category;
        if (value <= 0 || value <= best.getOrDefault(key, -1.0)) return;
        best.put(key, value); history.add(new Record(e.id(), category, value, set.kilograms(), set.reps(),
                session.startedAt(), session.id(), set.id(), session.notes()));
    }
    public static Set<LocalDate> hardLowerBodyDates(List<StrengthSession> sessions) {
        Set<LocalDate> dates = new HashSet<>();
        for (var s : sessions) if (context(s).lowerBody() == Involvement.HIGH)
            dates.add(s.startedAt().atOffset(s.offset()).toLocalDate());
        return Set.copyOf(dates);
    }
}
