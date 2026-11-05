package org.racedaydreams.domain.recovery;

import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.health.HealthIntelligence;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

public final class TrainingRecoveryTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    public static void main(String[] args) {
        var empty = review(List.of(), List.of(), List.of());
        check(!empty.lighterWeek() && !empty.reviewToday(), "Missing evidence is not fatigue");
        var high =
                List.of(
                        session("a", 1, 9.0, null, false),
                        session("b", 5, 9.0, null, false),
                        session("c", 8, 9.0, null, false));
        var supported = review(high, List.of(), List.of());
        check(
                supported.lighterWeek() && supported.reviewToday(),
                "Repeated demanding actuals across a week permit optional review");
        check(
                review(List.of(high.get(0), high.get(0)), List.of(), List.of()).evidence().size()
                        == 1,
                "Duplicate actual never counts twice");
        rejects(
                () ->
                        review(
                                List.of(high.get(0), session("a", 2, 9.0, null, false)),
                                List.of(),
                                List.of()));
        check(
                !review(List.of(session("old", 15, 10.0, null, false)), List.of(), List.of())
                        .reviewToday(),
                "Stale effort is not current fatigue");
        check(
                !review(List.of(session("future", -1, 10.0, null, false)), List.of(), List.of())
                        .reviewToday(),
                "Future evidence ignored");
        check(
                !review(List.of(session("warm", 1, 10.0, null, true)), List.of(), List.of())
                        .reviewToday(),
                "Warmups are not working exposure");
        check(
                !review(List.of(session("unknown", 1, null, null, false)), List.of(), List.of())
                        .reviewToday(),
                "Missing effort is unknown");
        check(
                review(List.of(session("rir", 1, null, 1.0, false)), List.of(), List.of())
                        .reviewToday(),
                "Recorded rep reserve may support review");
        check(
                !review(List.of(session("easy", 1, null, 4.0, false)), List.of(), List.of())
                        .reviewToday(),
                "Easy recorded effort does not trigger review");
        var concentrated =
                List.of(
                        session("a", 1, 9.0, null, false),
                        session("b", 2, 9.0, null, false),
                        session("c", 3, 9.0, null, false));
        check(
                !review(concentrated, List.of(), List.of()).lighterWeek(),
                "Three consecutive days do not establish sustained deload rule");
        var health = new ArrayList<HealthIntelligence.Observation>();
        for (int day = 0; day < 35; day++)
            for (var metric :
                    List.of(
                            HealthIntelligence.Metric.SLEEP_MINUTES,
                            HealthIntelligence.Metric.RESTING_HR)) {
                var at = NOW.minus(Duration.ofDays(day)).minusSeconds(3600);
                double value =
                        metric == HealthIntelligence.Metric.SLEEP_MINUTES
                                ? (day < 7 ? 300 : 480)
                                : (day < 7 ? 80 : 55);
                health.add(
                        new HealthIntelligence.Observation(
                                metric + "-" + day,
                                metric,
                                TODAY.minusDays(day),
                                at,
                                at,
                                value,
                                "provider",
                                NOW));
            }
        check(
                !review(List.of(), List.of(), health).reviewToday(),
                "Health trends alone do not diagnose fatigue");
        check(
                !review(List.of(), List.of(), health).lighterWeek(),
                "Health trends alone never establish deload");
        check(
                review(List.of(session("a", 4, 9.0, null, false)), List.of(), health).reviewToday(),
                "Two comparable trends can corroborate training context");
        check(
                !review(List.of(session("a", 4, 9.0, null, false)), List.of(), health.subList(0, 2))
                        .reviewToday(),
                "Isolated health readings insufficient");
        var run = run("run", NOW.minusSeconds(7200), 9.0);
        check(
                review(List.of(), List.of(run), List.of()).reviewToday(),
                "Recent demanding run supports today guidance");
        check(
                !review(List.of(), List.of(run), List.of()).lighterWeek(),
                "One run never establishes strength deload");
        check(
                !review(List.of(), List.of(run("future", NOW, 9.0)), List.of()).reviewToday(),
                "Unfinished future-ended run ignored");
        programmeTests(supported);
        System.out.println("TrainingRecoveryTestSuite: " + checks + " checks passed.");
    }

    private static void programmeTests(TrainingRecovery.Review supported) {
        for (var priority : GymProgramme.Priority.values()) {
            var settings =
                    new StrengthProgramme.Settings(
                            GymProgramme.Goal.STRENGTH,
                            priority,
                            GymProgramme.Experience.INTERMEDIATE,
                            EnumSet.allOf(DayOfWeek.class),
                            3,
                            60,
                            EnumSet.allOf(ExerciseMetadata.Equipment.class));
            var p =
                    new StrengthProgrammeGenerator()
                            .generate(
                                    new StrengthProgrammeGenerator.Request(
                                            "recovery",
                                            NOW.minus(Duration.ofDays(10)),
                                            "My programme",
                                            settings,
                                            TODAY.minusDays(7),
                                            6,
                                            null,
                                            PrimaryFocus.HYBRID,
                                            Set.of(),
                                            null,
                                            Set.of(),
                                            Set.of(),
                                            false));
            var eligible =
                    p.workouts().stream()
                            .filter(
                                    w ->
                                            !w.date().isBefore(TODAY)
                                                    && !w.date().isAfter(TODAY.plusDays(6)))
                            .toList();
            var protectedIds = Set.of(eligible.get(0).id());
            var proposal =
                    RecoveryProgrammeProposal.lighterWeek(
                            p, supported, NOW, ZoneOffset.UTC, protectedIds);
            check(proposal.previous().equals(p), "Immutable original retained");
            check(proposal.proposed().revision() == p.revision() + 1, "Forward reviewed revision");
            for (var old : p.workouts()) {
                var next =
                        proposal.proposed().workouts().stream()
                                .filter(w -> w.id().equals(old.id()))
                                .findFirst()
                                .orElseThrow();
                if (old.date().isBefore(TODAY)
                        || old.date().isAfter(TODAY.plusDays(6))
                        || protectedIds.contains(old.id()))
                    check(old.equals(next), "Past/protected/later weeks exact");
                else
                    for (int i = 0; i < old.definition().slots().size(); i++) {
                        var a = old.definition().slots().get(i);
                        var b = next.definition().slots().get(i);
                        check(
                                a.exercise().equals(b.exercise())
                                        && Objects.equals(a.supersetGroup(), b.supersetGroup()),
                                "Exercise identity/group preserved");
                        if (a.target() != null) {
                            var t = a.target();
                            var expected =
                                    new GymWorkoutDefinition.Target(
                                            Math.max(1, t.sets() - 1),
                                            t.minimumReps(),
                                            t.maximumReps(),
                                            t.seconds(),
                                            t.metres(),
                                            t.effort(),
                                            t.restSeconds(),
                                            t.referenceLoadKg());
                            check(
                                    expected.equals(b.target()),
                                    "Only working-set count changes; no invented load");
                        }
                    }
            }
            rejects(
                    () ->
                            RecoveryProgrammeProposal.lighterWeek(
                                    p,
                                    supported,
                                    NOW.plus(Duration.ofDays(1)),
                                    ZoneOffset.UTC,
                                    protectedIds));
            var lower =
                    p.workouts().stream()
                            .filter(
                                    w ->
                                            !w.date().isBefore(TODAY)
                                                    && TrainingRecovery.meaningfulLower(w))
                            .findFirst()
                            .orElseThrow();
            var runs =
                    List.of(
                            new PlannedWorkout(
                                    "interval",
                                    lower.date(),
                                    WorkoutCategory.INTERVALS,
                                    5.0,
                                    null,
                                    List.of(),
                                    null));
            var conflicts = TrainingRecovery.conflicts(p, runs, TODAY, Set.of());
            check(
                    conflicts.stream()
                            .anyMatch(
                                    c ->
                                            c.workoutId().equals(lower.id())
                                                    && c.timing()
                                                            == TrainingRecovery.Timing.SAME_DAY),
                    "Full body lower work counts");
            check(
                    TrainingRecovery.conflicts(p, runs, TODAY, Set.of(lower.id())).stream()
                            .noneMatch(c -> c.workoutId().equals(lower.id())),
                    "Started/completed session not offered for editing");
            var dates =
                    RecoveryProgrammeProposal.alternatives(p, lower.id(), runs, TODAY, Set.of());
            check(!dates.isEmpty(), "Available lower-conflict date offered");
            var moved =
                    RecoveryProgrammeProposal.move(
                            p, lower.id(), dates.get(0), runs, TODAY, NOW, Set.of());
            for (var old : p.workouts()) {
                var next =
                        moved.proposed().workouts().stream()
                                .filter(w -> w.id().equals(old.id()))
                                .findFirst()
                                .orElseThrow();
                check(
                        old.definition().equals(next.definition()),
                        "Move never changes frozen targets");
                check(
                        old.id().equals(lower.id())
                                ? next.date().equals(dates.get(0))
                                : old.equals(next),
                        "Only selected dated workout moves");
            }
            rejects(
                    () ->
                            RecoveryProgrammeProposal.move(
                                    p, lower.id(), lower.date(), runs, TODAY, NOW, Set.of()));
            check(
                    RecoveryProgrammeProposal.alternatives(
                                    p, lower.id(), runs, TODAY, Set.of(lower.id()))
                            .isEmpty(),
                    "Protected session has no move suggestions");
            for (int gap = -7; gap <= 7; gap++) {
                var r =
                        new PlannedWorkout(
                                "race",
                                lower.date().plusDays(gap),
                                WorkoutCategory.RACE,
                                null,
                                null,
                                List.of(),
                                null);
                var c = TrainingRecovery.conflicts(p, List.of(r), TODAY, Set.of());
                boolean expected = !r.date().isBefore(TODAY) && (gap >= -1 && gap <= 6);
                check(
                        c.stream().anyMatch(x -> x.workoutId().equals(lower.id())) == expected,
                        "Race context bounded, day gap " + gap);
            }
        }
    }

    private static TrainingRecovery.Review review(
            List<StrengthSession> gym,
            List<ActivityRecord> runs,
            List<HealthIntelligence.Observation> health) {
        return TrainingRecovery.review(gym, runs, health, NOW, ZoneOffset.UTC);
    }

    private static StrengthSession session(
            String id, int days, Double effort, Double reserve, boolean warmup) {
        var sets =
                List.of(
                        new StrengthSession.SetEntry(
                                id + "-1", 5, 60, effort, warmup, null, null, reserve),
                        new StrengthSession.SetEntry(
                                id + "-2", 5, 60, effort, warmup, null, null, reserve));
        return new StrengthSession(
                id,
                NOW.minus(Duration.ofDays(days)),
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "",
                List.of(
                        new StrengthSession.ExerciseSets(
                                ExerciseCatalog.definition("bench-press").exercise(), sets)));
    }

    private static ActivityRecord run(String id, Instant start, Double rpe) {
        return new ActivityRecord(
                id,
                new SourceIdentity("native", id, null),
                start,
                start.plusSeconds(3600),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                10000.0,
                Duration.ofHours(1),
                null,
                null,
                null,
                null,
                rpe,
                null,
                List.of(),
                List.of(),
                null,
                null);
    }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }

    private static void rejects(Runnable r) {
        checks++;
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected guarded refusal");
    }
}
