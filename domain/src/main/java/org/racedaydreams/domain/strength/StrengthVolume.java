package org.racedaydreams.domain.strength;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Recorded evidence only. Primary set shares are a taxonomy, never measured biological stimulus.
 */
public final class StrengthVolume {
    private StrengthVolume() {}

    public record ExerciseWeek(
            StrengthExercise exercise,
            int workouts,
            int workingSets,
            long repetitions,
            long seconds,
            double metres,
            Double loadRepetitions) {}

    public record Week(
            LocalDate monday,
            int workouts,
            int workingSets,
            int unclassifiedSets,
            Map<ExerciseMetadata.BodyArea, Double> primarySetShares,
            Map<ExerciseMetadata.BodyArea, Integer> secondaryExposures,
            List<ExerciseWeek> exercises) {
        public Week {
            var primary =
                    new EnumMap<ExerciseMetadata.BodyArea, Double>(ExerciseMetadata.BodyArea.class);
            primary.putAll(primarySetShares);
            primarySetShares = Collections.unmodifiableMap(primary);
            var secondary =
                    new EnumMap<ExerciseMetadata.BodyArea, Integer>(
                            ExerciseMetadata.BodyArea.class);
            secondary.putAll(secondaryExposures);
            secondaryExposures = Collections.unmodifiableMap(secondary);
            exercises = org.racedaydreams.domain.coaching.Immutable.copyList(exercises);
        }
    }

    public static String attribution() {
        return "One recorded working set is shared equally between its documented primary areas."
                + " Secondary involvement is shown separately as exposure, not added full sets."
                + " These are broad exercise classifications, not exact muscle stimulus or"
                + " training stress. Warm-ups are excluded. Unknown or changed exercise metadata"
                + " remains unclassified.";
    }

    /**
     * Complete Monday-based windows, including explicitly empty weeks. Future observations
     * excluded.
     */
    public static List<Week> weekly(
            List<StrengthSession> sessions, LocalDate firstWeek, int weeks, Instant asOf) {
        Objects.requireNonNull(firstWeek);
        Objects.requireNonNull(asOf);
        if (weeks < 1 || weeks > 2600) throw new IllegalArgumentException("Choose 1–2600 weeks.");
        LocalDate first = firstWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var buckets = new ArrayList<List<StrengthSession>>();
        for (int i = 0; i < weeks; i++) buckets.add(new ArrayList<>());
        var seen = new HashMap<String, StrengthSession>();
        for (var s : sessions) {
            var previous = seen.putIfAbsent(s.id(), s);
            if (previous != null) {
                if (!previous.equals(s))
                    throw new IllegalArgumentException("Conflicting actual workout evidence.");
                continue;
            }
            if (s.startedAt().isAfter(asOf)) continue;
            LocalDate date = s.startedAt().atOffset(s.offset()).toLocalDate();
            long days = java.time.temporal.ChronoUnit.DAYS.between(first, date);
            if (days >= 0 && days < weeks * 7L) buckets.get((int) (days / 7)).add(s);
        }
        var result = new ArrayList<Week>();
        for (int i = 0; i < weeks; i++) result.add(summarise(first.plusWeeks(i), buckets.get(i)));
        return org.racedaydreams.domain.coaching.Immutable.copyList(result);
    }

    private static Week summarise(LocalDate monday, List<StrengthSession> sessions) {
        var primary =
                new EnumMap<ExerciseMetadata.BodyArea, Double>(ExerciseMetadata.BodyArea.class);
        var secondary =
                new EnumMap<ExerciseMetadata.BodyArea, Integer>(ExerciseMetadata.BodyArea.class);
        var exercises = new LinkedHashMap<StrengthExercise, Totals>();
        int working = 0, unknown = 0;
        for (var session : sessions)
            for (var block : session.exercises()) {
                var exercise = block.exercise();
                var totals = exercises.computeIfAbsent(exercise, e -> new Totals());
                var canonical = ExerciseCatalog.definition(exercise.id());
                var metadata =
                        canonical != null && canonical.exercise().equals(exercise)
                                ? ExerciseMetadataCatalog.find(exercise.id())
                                : null;
                for (var set : block.sets()) {
                    if (set.warmUp()) continue;
                    working++;
                    totals.sets++;
                    totals.sessions.add(session.id());
                    totals.reps += set.reps();
                    if (set.durationSeconds() != null) totals.seconds += set.durationSeconds();
                    if (set.distanceMetres() != null) totals.metres += set.distanceMetres();
                    // External-load repetition volume only, per exact exercise; no body-mass
                    // inference.
                    if (exercise.mode() == StrengthExercise.Mode.WEIGHTED
                            && exercise.measurement()
                                    == StrengthExercise.MeasurementProfile.LOAD_REPS) {
                        totals.hasLoadReps = true;
                        totals.loadReps += set.kilograms() * set.reps();
                    }
                    if (metadata == null || metadata.primaryAreas().isEmpty()) {
                        unknown++;
                        continue;
                    }
                    double share = 1.0 / metadata.primaryAreas().size();
                    for (var area : metadata.primaryAreas())
                        primary.merge(area, share, Double::sum);
                    for (var area : metadata.secondaryAreas())
                        secondary.merge(area, 1, Integer::sum);
                }
            }
        var rows = new ArrayList<ExerciseWeek>();
        exercises.forEach(
                (e, t) -> {
                    if (t.sets > 0)
                        rows.add(
                                new ExerciseWeek(
                                        e,
                                        t.sessions.size(),
                                        t.sets,
                                        t.reps,
                                        t.seconds,
                                        t.metres,
                                        t.hasLoadReps ? t.loadReps : null));
                });
        rows.sort(
                Comparator.comparing((ExerciseWeek e) -> e.exercise().name())
                        .thenComparing(e -> e.exercise().id()));
        return new Week(monday, sessions.size(), working, unknown, primary, secondary, rows);
    }

    private static final class Totals {
        int sets;
        long reps, seconds;
        double metres, loadReps;
        boolean hasLoadReps;
        final Set<String> sessions = new HashSet<>();
    }
}
