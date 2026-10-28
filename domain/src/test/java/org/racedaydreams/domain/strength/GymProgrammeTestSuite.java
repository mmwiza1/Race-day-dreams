package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.*;

import java.time.*;
import java.util.*;

public final class GymProgrammeTestSuite {
    static int checks;

    static void ok(boolean b, String why) {
        checks++;
        if (!b) throw new AssertionError(why);
    }

    static final LocalDate START = LocalDate.of(2026, 10, 26);
    static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

    public static void main(String[] args) {
        var generator = new GymProgrammeGenerator();
        for (int frequency = 1; frequency <= 6; frequency++)
            for (var goal : GymProgramme.Goal.values())
                for (var priority : GymProgramme.Priority.values()) {
                    int minutes = new int[] {30, 45, 60, 75}[(frequency + goal.ordinal()) % 4];
                    var experience =
                            GymProgramme.Experience.values()[(frequency + priority.ordinal()) % 3];
                    Set<String> equipment =
                            frequency % 2 == 0 ? Set.of("full gym") : Set.of("bodyweight");
                    var settings =
                            new GymProgramme.Settings(
                                    goal,
                                    priority,
                                    experience,
                                    EnumSet.allOf(DayOfWeek.class),
                                    frequency,
                                    equipment,
                                    minutes,
                                    GymProgramme.Effort.RPE);
                    var runs =
                            List.of(
                                    new PlannedWorkout(
                                            "key",
                                            START.plusDays(3),
                                            WorkoutCategory.INTERVALS,
                                            7.0,
                                            null,
                                            List.of(),
                                            "Quality"),
                                    new PlannedWorkout(
                                            "long",
                                            START.plusDays(6),
                                            WorkoutCategory.LONG_RUN,
                                            14.0,
                                            null,
                                            List.of(),
                                            "Long"));
                    var plan =
                            generator.generate(
                                    "p",
                                    1,
                                    NOW,
                                    "Runner created a block",
                                    settings,
                                    START,
                                    runs,
                                    List.of());
                    ok(
                            plan.equals(
                                    generator.generate(
                                            "p",
                                            1,
                                            NOW,
                                            "Runner created a block",
                                            settings,
                                            START,
                                            runs,
                                            List.of())),
                            "Deterministic complete programme");
                    ok(plan.sessions().size() == frequency * 6, "Exact requested frequency");
                    var dates = new HashSet<LocalDate>();
                    for (var session : plan.sessions()) {
                        ok(dates.add(session.date()), "No duplicated dates");
                        ok(
                                settings.days().contains(session.date().getDayOfWeek()),
                                "Available dates only");
                        int sets = 0;
                        for (var p : session.exercises()) {
                            ok(
                                    ExerciseCatalog.definition(p.exerciseId()) != null,
                                    "Canonical exercise exists");
                            ok(
                                    GymProgrammeGenerator.available(p.exerciseId(), equipment),
                                    "No missing equipment");
                            sets += p.sets();
                            ok(p.minimumRpe() <= p.maximumRpe(), "Valid effort range");
                        }
                        ok(sets <= 18, "Reasonable session volume");
                        if (minutes == 30)
                            ok(session.exercises().size() <= 4, "Short session budget");
                    }
                }
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.SUPPORT_RUNNING,
                        GymProgramme.Priority.RUNNING_FIRST,
                        GymProgramme.Experience.BEGINNER,
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        2,
                        Set.of("bodyweight"),
                        30,
                        GymProgramme.Effort.RIR);
        var plan =
                generator.generate(
                        "support",
                        1,
                        NOW,
                        "Runner chose running support",
                        settings,
                        START,
                        List.of(),
                        List.of());
        ok(
                plan.sessions().stream()
                        .allMatch(
                                s ->
                                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY)
                                                .contains(s.date().getDayOfWeek())),
                "Required weekdays respected");
        var p =
                new GymProgramme.Prescription(
                        "back-squat", 2, 5, 8, null, null, 6, 8, 120, "Controlled reps");
        ok(
                StrengthProgressionAdvisor.review(p, List.of()).action()
                        == StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                "No fabricated baseline");
        var first = session("a", NOW.minusSeconds(3600), 8, 7.0);
        var second = session("b", NOW.minusSeconds(86400), 8, 7.0);
        var advice = StrengthProgressionAdvisor.review(p, List.of(first, second));
        ok(
                advice.action() == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Repeated controlled top reps");
        ok(advice.options().contains("Keep current load"), "No forced increase");
        ok(first.exercises().get(0).sets().get(0).kilograms() == 80, "Evidence immutable");
        ok(
                StrengthProgressionAdvisor.review(p, List.of(session("hard", NOW, 5, 9.0), second))
                                .action()
                        == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                "High effort -> reduce option");
        ok(
                StrengthProgressionAdvisor.review(
                                        p, List.of(session("unknown", NOW, 8, null), second))
                                .action()
                        == StrengthProgressionAdvisor.Action.HOLD,
                "Missing effort -> hold");
        ok(
                StrengthProgressionAdvisor.review(
                                        p, List.of(first, second), NOW.plus(Duration.ofDays(45)))
                                .action()
                        == StrengthProgressionAdvisor.Action.HOLD,
                "Stale evidence cannot drive a load increase");
        progressionRequiresRecentSessions(p);
        ok(!GymProgrammeGenerator.available("back-squat", Set.of("barbell")), "Squat needs rack");
        ok(
                !GymProgrammeGenerator.available("bench-press", Set.of("barbell", "squat rack")),
                "Bench needs bench");
        ok(
                !GymProgrammeGenerator.available("goblet-squat", Set.of("barbell")),
                "A barbell is not a goblet weight");
        ok(
                !GymProgrammeGenerator.available("split-squat", Set.of("barbell")),
                "Rear-foot elevated split squat needs support");
        ok(
                !GymProgrammeGenerator.available("hip-thrust", Set.of("barbell")),
                "Hip thrust needs a bench");
        ok(
                !GymProgrammeGenerator.available("weighted-pull-up", Set.of("barbell")),
                "Weighted pull-up also needs a pull-up bar");
        ok(
                GymProgrammeGenerator.available("goblet-squat", Set.of("kettlebells")),
                "Goblet kettlebell alternative");
        for (var equipment :
                List.of(Set.of("barbell"), Set.of("dumbbells", "bench"), Set.of("kettlebells"))) {
            var restricted =
                    new GymProgramme.Settings(
                            GymProgramme.Goal.SUPPORT_RUNNING,
                            GymProgramme.Priority.RUNNING_FIRST,
                            GymProgramme.Experience.BEGINNER,
                            Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                            2,
                            equipment,
                            45,
                            GymProgramme.Effort.RPE);
            var programme =
                    generator.generate(
                            "restricted",
                            1,
                            NOW,
                            "Equipment audit",
                            restricted,
                            START,
                            List.of(),
                            List.of());
            for (var session : programme.sessions())
                for (var prescribed : session.exercises()) {
                    ok(
                            GymProgrammeGenerator.available(prescribed.exerciseId(), equipment),
                            "Restricted equipment respected");
                    if (prescribed.exerciseId().equals("goblet-squat"))
                        ok(
                                equipment.contains("dumbbells")
                                        || equipment.contains("kettlebells"),
                                "Independent goblet requirement");
                    if (prescribed.exerciseId().equals("split-squat")
                            || prescribed.exerciseId().equals("hip-thrust"))
                        ok(equipment.contains("bench"), "Independent bench requirement");
                }
        }
        System.out.println("GymProgrammeTestSuite: " + checks + " checks passed.");
    }

    static void progressionRequiresRecentSessions(GymProgramme.Prescription prescription) {
        var fresh = session("fresh", NOW.minusSeconds(3600), 8, 7.0);
        var recent = session("recent", NOW.minus(Duration.ofDays(7)), 8, 7.0);
        var stale = session("stale", NOW.minus(Duration.ofDays(29)), 8, 7.0);
        var future = session("future", NOW.plusSeconds(1), 8, 7.0);
        var repeated = StrengthProgressionAdvisor.review(prescription, List.of(fresh, recent), NOW);
        ok(
                repeated.action() == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Two recent comparable successes support an optional increase");
        ok(
                repeated.suggestedKilograms() == 82.0,
                "Recent successes retain the same small increase");
        ok(
                repeated.options().contains("Keep current load"),
                "Recency filtering never applies an increase");
        for (var history :
                List.of(
                        List.of(fresh, stale),
                        List.of(stale),
                        List.of(future, fresh),
                        List.of(future))) {
            var advice = StrengthProgressionAdvisor.review(prescription, history, NOW);
            ok(
                    advice.action() == StrengthProgressionAdvisor.Action.HOLD,
                    "One recent session cannot combine with stale or future-dated evidence");
            ok(
                    advice.suggestedKilograms() == null,
                    "Ineligible evidence never produces a numeric increase");
        }
        ok(
                StrengthProgressionAdvisor.review(prescription, List.of(future, fresh, recent), NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "A future entry does not hide two genuine recent successes");
        var boundary = session("boundary", NOW.minus(Duration.ofDays(28)), 8, 7.0);
        ok(
                StrengthProgressionAdvisor.review(prescription, List.of(fresh, boundary), NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Evidence exactly 28 days old remains eligible");
        var outside = session("outside", NOW.minus(Duration.ofDays(28)).minusSeconds(1), 8, 7.0);
        ok(
                StrengthProgressionAdvisor.review(prescription, List.of(fresh, outside), NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.HOLD,
                "The 28-day window does not round away extra stale hours");
        ok(
                StrengthProgressionAdvisor.review(
                                        prescription,
                                        List.of(session("current", NOW, 8, 7.0), fresh),
                                        NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Evidence at the as-of instant is eligible");
        ok(
                StrengthProgressionAdvisor.review(
                                        prescription,
                                        List.of(session("unknown-effort", NOW, 8, null), recent),
                                        NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.HOLD,
                "Fresh evidence still requires recorded effort");
        ok(
                StrengthProgressionAdvisor.review(
                                        prescription,
                                        List.of(session("hard", NOW, 5, 9.0), stale),
                                        NOW)
                                .action()
                        == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                "A current hard session still supports a conservative reduction option");
        ok(
                StrengthProgressionAdvisor.review(prescription, List.of(), NOW).action()
                        == StrengthProgressionAdvisor.Action.GATHER_EVIDENCE,
                "An empty dated history does not invent a previous load");
        ok(
                StrengthProgressionAdvisor.review(prescription, List.of(fresh, stale)).action()
                        == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "The undated compatibility API retains its historical contract");
        ok(
                fresh.exercises().get(0).sets().get(0).kilograms() == 80,
                "Recency checks leave actual evidence untouched");
    }

    static StrengthSession session(String id, Instant at, int reps, Double rpe) {
        var e = ExerciseCatalog.definition("back-squat").exercise();
        return new StrengthSession(
                id,
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(40),
                rpe,
                "",
                List.of(
                        new StrengthSession.ExerciseSets(
                                e,
                                List.of(
                                        new StrengthSession.SetEntry(
                                                id + "1", reps, 80, rpe, false),
                                        new StrengthSession.SetEntry(
                                                id + "2", reps, 80, rpe, false)))));
    }
}
