package org.racedaydreams.domain.recovery;

import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.health.HealthIntelligence;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/**
 * Conservative original coaching rules, not a readiness score, diagnosis or automatic plan editor.
 */
public final class TrainingRecovery {
    private TrainingRecovery() {}

    public record Evidence(String kind, String id, Instant at, String explanation) {}

    public record Review(
            LocalDate date,
            String heading,
            String instruction,
            boolean lighterWeek,
            boolean reviewToday,
            List<Evidence> evidence,
            List<String> limitations) {
        public Review {
            evidence = Immutable.copyList(evidence);
            limitations = Immutable.copyList(limitations);
        }
    }

    public static Review review(
            List<StrengthSession> strength,
            List<ActivityRecord> runs,
            List<HealthIntelligence.Observation> health,
            Instant now,
            ZoneOffset offset) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(offset);
        LocalDate today = now.atOffset(offset).toLocalDate();
        var evidence = new ArrayList<Evidence>();
        var highDates = new TreeSet<LocalDate>();
        int knownEffort = 0, unknownEffort = 0;
        boolean recentDemand = false;
        var seen = new HashMap<String, StrengthSession>();
        for (var session : strength) {
            var duplicate = seen.putIfAbsent(session.id(), session);
            if (duplicate != null) {
                if (!duplicate.equals(session))
                    throw new IllegalArgumentException("Conflicting strength evidence identity.");
                continue;
            }
            if (session.startedAt().plus(session.duration()).isAfter(now)
                    || session.startedAt().isBefore(now.minus(Duration.ofDays(14)))) continue;
            int working = 0, effort = 0, demanding = 0;
            for (var block : session.exercises())
                for (var set : block.sets())
                    if (!set.warmUp()) {
                        working++;
                        if (set.rpe() != null || set.repetitionsInReserve() != null) effort++;
                        if (set.rpe() != null && set.rpe() >= 9
                                || set.repetitionsInReserve() != null
                                        && set.repetitionsInReserve() <= 1) demanding++;
                    }
            if (working == 0) continue;
            if (session.rpe() != null || effort > 0) knownEffort++;
            else unknownEffort++;
            boolean high =
                    session.rpe() != null && session.rpe() >= 8
                            || effort >= 2 && demanding >= 2 && demanding * 2 >= effort;
            if (high) {
                highDates.add(session.startedAt().atOffset(session.offset()).toLocalDate());
                String detail =
                        session.rpe() != null && session.rpe() >= 8
                                ? "Strength workout recorded with perceived effort "
                                        + session.rpe()
                                        + " out of 10."
                                : "Several working sets recorded very high perceived effort or no"
                                        + " more than one repetition left before failure. These are"
                                        + " separate recorded effort scales, not a measured fatigue"
                                        + " score.";
                evidence.add(new Evidence("strength", session.id(), session.startedAt(), detail));
                if (!session.startedAt().isBefore(now.minus(Duration.ofHours(48))))
                    recentDemand = true;
            }
        }
        var seenRuns = new HashMap<String, ActivityRecord>();
        for (var run : runs) {
            var duplicate = seenRuns.putIfAbsent(run.id(), run);
            if (duplicate != null) {
                if (!duplicate.equals(run))
                    throw new IllegalArgumentException("Conflicting running evidence identity.");
                continue;
            }
            if (!run.end().isAfter(now)
                    && !run.start().isBefore(now.minus(Duration.ofHours(48)))
                    && run.rpe() != null
                    && run.rpe() >= 8) {
                recentDemand = true;
                evidence.add(
                        new Evidence(
                                "run",
                                run.id(),
                                run.start(),
                                "Run recorded with perceived effort "
                                        + run.rpe()
                                        + " out of 10 in the last two days."));
            }
        }
        var validHealth = new ArrayList<HealthIntelligence.Observation>();
        for (var row : health)
            if (!row.start().isAfter(now)
                    && !row.end().isAfter(now)
                    && !row.importedAt().isAfter(now)) validHealth.add(row);
        int corroborating = 0;
        for (var trend : HealthIntelligence.evaluate(validHealth, today)) {
            if (trend.recentDays() < 4
                    || trend.referenceDays() < 14
                    || trend.recent() == null
                    || trend.baseline() == null) continue;
            boolean signal =
                    org.racedaydreams.domain.health.RecoveryCoaching.adversePersonalTrend(trend);
            if (!signal) continue;
            var latest =
                    validHealth.stream()
                            .filter(
                                    o ->
                                            o.metric() == trend.metric()
                                                    && o.source().equals(trend.source()))
                            .max(Comparator.comparing(HealthIntelligence.Observation::end))
                            .orElse(null);
            if (latest == null || latest.end().isBefore(now.minus(Duration.ofDays(3)))) continue;
            corroborating++;
            evidence.add(
                    new Evidence(
                            "health",
                            latest.id(),
                            latest.end(),
                            trend.metric().name().replace('_', ' ')
                                    + ": "
                                    + trend.interpretation()
                                    + " from comparable source "
                                    + trend.source()
                                    + " ("
                                    + trend.recentDays()
                                    + " recent and "
                                    + trend.referenceDays()
                                    + " reference days). Supporting context only, not a"
                                    + " diagnosis."));
        }
        boolean lighter =
                highDates.size() >= 3
                        && java.time.temporal.ChronoUnit.DAYS.between(
                                        highDates.first(), highDates.last())
                                >= 6;
        // Health signals never establish a deload on their own or make missing workout effort easy.
        boolean todayReview = recentDemand || corroborating >= 2 && !highDates.isEmpty();
        var limitations = new ArrayList<String>();
        limitations.add(
                "Recorded effort is subjective. Unlogged training and missing measurements remain"
                        + " unknown; this does not diagnose recovery, illness or injury.");
        limitations.add(
                knownEffort
                        + " recent strength workouts have some effort evidence; "
                        + unknownEffort
                        + " do not. Warm-up-only sessions are excluded.");
        if (corroborating < 2)
            limitations.add(
                    "There are not two fresh comparable health trends supporting a recovery review."
                            + " Health Connect is optional.");
        evidence.sort(
                Comparator.comparing(Evidence::at)
                        .reversed()
                        .thenComparing(Evidence::kind)
                        .thenComparing(Evidence::id));
        return new Review(
                today,
                lighter
                        ? "An optional lighter week is available for review"
                        : todayReview
                                ? "Consider a lighter approach today"
                                : "No programme change is suggested from these records",
                lighter
                        ? "Your recent logs include several very demanding strength workouts across"
                                + " at least a week. You can keep your programme or review one week"
                                + " with fewer working sets."
                        : todayReview
                                ? "You recently recorded demanding training. You may use fewer"
                                      + " optional working sets or keep more effort in reserve"
                                      + " today. Log what you actually perform; your programme has"
                                      + " not changed."
                                : "Keep your current programme if it feels appropriate. There is"
                                      + " not enough supported evidence here to recommend a lighter"
                                      + " week; you can still choose an easier session yourself.",
                lighter,
                todayReview,
                evidence,
                limitations);
    }

    public enum Timing {
        SAME_DAY,
        BEFORE,
        AFTER,
        RACE_WEEK
    }

    public record Conflict(
            String workoutId,
            LocalDate gymDate,
            String runId,
            LocalDate runDate,
            WorkoutCategory category,
            Timing timing,
            String explanation) {}

    public static List<Conflict> conflicts(
            StrengthProgramme programme,
            List<PlannedWorkout> running,
            LocalDate today,
            Set<String> protectedWorkouts) {
        var result = new ArrayList<Conflict>();
        for (var gym : programme.workouts()) {
            if (gym.date().isBefore(today)
                    || gym.date().isAfter(today.plusDays(20))
                    || protectedWorkouts.contains(gym.id())
                    || !meaningfulLower(gym)) continue;
            for (var run : running) {
                if (!demanding(run) || run.date().isBefore(today)) continue;
                long separation =
                        java.time.temporal.ChronoUnit.DAYS.between(gym.date(), run.date());
                Timing timing =
                        separation == 0
                                ? Timing.SAME_DAY
                                : separation == 1
                                        ? Timing.BEFORE
                                        : separation == -1
                                                ? Timing.AFTER
                                                : run.category() == WorkoutCategory.RACE
                                                                && separation >= 2
                                                                && separation <= 6
                                                        ? Timing.RACE_WEEK
                                                        : null;
                if (timing == null) continue;
                String context =
                        timing == Timing.SAME_DAY
                                ? "Lower-body gym work shares a day with a demanding planned run."
                                : timing == Timing.BEFORE
                                        ? "Lower-body gym work is immediately before a demanding"
                                                + " planned run."
                                        : timing == Timing.AFTER
                                                ? "Lower-body gym work is immediately after a"
                                                      + " demanding planned run; this is softer"
                                                      + " scheduling context, not proof of fatigue."
                                                : "Lower-body gym work falls in the days before"
                                                        + " your planned race.";
                String priority =
                        switch (programme.settings().priority()) {
                            case STRENGTH_FIRST ->
                                    "Strength First is selected. Keeping the gym session is a valid"
                                            + " choice; neither discipline is silently sacrificed.";
                            case RUNNING_FIRST ->
                                    "Running First is selected. Moving gym work or reducing"
                                        + " optional volume may leave more room for the run, if you"
                                        + " choose.";
                            case BALANCED ->
                                    "Balanced is selected. Review the trade-off; both sessions may"
                                            + " be kept.";
                        };
                result.add(
                        new Conflict(
                                gym.id(),
                                gym.date(),
                                run.id(),
                                run.date(),
                                run.category(),
                                timing,
                                context + " " + priority));
            }
        }
        result.sort(
                Comparator.comparing(Conflict::gymDate)
                        .thenComparing(Conflict::workoutId)
                        .thenComparing(Conflict::runDate)
                        .thenComparing(Conflict::runId));
        return Immutable.copyList(result);
    }

    public static boolean meaningfulLower(StrengthProgramme.Workout workout) {
        int sets = 0;
        for (var slot : workout.definition().slots()) {
            var canonical = ExerciseCatalog.definition(slot.exercise().id());
            var metadata =
                    canonical != null && canonical.exercise().equals(slot.exercise())
                            ? ExerciseMetadataCatalog.find(slot.exercise().id())
                            : null;
            boolean lower =
                    metadata != null
                            ? metadata.primaryAreas().stream()
                                    .anyMatch(
                                            a ->
                                                    a == ExerciseMetadata.BodyArea.QUADS
                                                            || a
                                                                    == ExerciseMetadata.BodyArea
                                                                            .HAMSTRINGS
                                                            || a == ExerciseMetadata.BodyArea.GLUTES
                                                            || a == ExerciseMetadata.BodyArea.CALVES
                                                            || a
                                                                    == ExerciseMetadata.BodyArea
                                                                            .LOWER_LEG)
                            : slot.exercise().region() == StrengthExercise.Region.LOWER;
            if (lower && slot.target() != null) sets += slot.target().sets();
        }
        return sets >= 2;
    }

    public static boolean demanding(PlannedWorkout run) {
        return switch (run.category()) {
            case LONG_RUN, TEMPO_THRESHOLD_RUN, INTERVALS, RACE_PACE_RUN, TIME_TRIAL, RACE -> true;
            default -> false;
        };
    }
}
