package org.racedaydreams.domain.presentation;

import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.StrengthProgressionAdvisor;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/** Identity, recency and unknown-evidence regressions for the Strength-first Today projection. */
public final class StrengthTodaySummaryTestSuite {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");
    private static int checks;

    public static void main(String[] args) {
        authoredPrescriptionsAndEvidence();
        standaloneWithoutProgramme();
        actualDoesNotImplyAdherence();
        retainedVersionAssociations();
        localWeekAndFutureEvidence();
        recommendationsNeedRecentComparableEvidence();
        estimatesAreNotRecentActualPbs();
        System.out.println("StrengthTodaySummaryTestSuite: " + checks + " checks passed.");
    }

    private static void authoredPrescriptionsAndEvidence() {
        var target =
                new GymWorkoutDefinition.Target(
                        2,
                        6,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, 1.5, 3.5),
                        null,
                        80.0);
        var slot =
                new GymWorkoutDefinition.Slot(
                        "bench",
                        ExerciseCatalog.definition("bench-press").exercise(),
                        target,
                        null);
        var workout =
                new StrengthProgramme.Workout(
                        "today",
                        TODAY,
                        StrengthWorkoutBudget.workoutMinutes(List.of(slot)),
                        new GymWorkoutDefinition(
                                "frozen",
                                1,
                                NOW.minusSeconds(86400),
                                "Athlete-authored bench",
                                List.of(slot)),
                        null);
        var programme =
                new StrengthProgramme(
                        "authored",
                        2,
                        1,
                        NOW.minusSeconds(86400),
                        "Athlete plan",
                        StrengthProgramme.Origin.CUSTOM,
                        null,
                        new StrengthProgramme.Settings(
                                GymProgramme.Goal.STRENGTH,
                                GymProgramme.Priority.BALANCED,
                                GymProgramme.Experience.INTERMEDIATE,
                                Set.of(TODAY.getDayOfWeek()),
                                1,
                                45,
                                Set.of(
                                        ExerciseMetadata.Equipment.BARBELL,
                                        ExerciseMetadata.Equipment.BENCH,
                                        ExerciseMetadata.Equipment.SQUAT_RACK)),
                        List.of(
                                new StrengthProgramme.Block(
                                        "block",
                                        StrengthProgramme.Phase.FOUNDATION,
                                        "Foundation",
                                        TODAY,
                                        1,
                                        List.of(workout),
                                        "Repeat technique")),
                        TODAY,
                        "Athlete accepted revision",
                        List.of());
        var recent = actual("recent", NOW.minusSeconds(3600), 7.0);
        var prior = actual("prior", NOW.minus(Duration.ofDays(6)), 7.0);
        var summary =
                StrengthTodaySummary.fromAuthored(
                        programme, List.of(recent, prior), List.of(), TODAY, NOW);
        ok(
                summary.today() == null,
                "Authored prescription is not coerced into integer-RPE legacy targets");
        ok(summary.authoredToday().equals(workout), "Frozen authored session is projected exactly");
        ok(
                summary.authoredToday().definition().slots().get(0).target().effort().minimum()
                        == 1.5,
                "Fractional RIR survives Today");
        ok(
                summary.authoredToday().definition().slots().get(0).target().restSeconds() == null,
                "Unspecified rest remains unknown");
        ok(
                summary.plannedThisWeek() == 1 && !summary.todayCompleted(),
                "Actuals never silently complete authored work");
        ok(
                summary.recommendation() != null
                        && summary.recommendation().action()
                                == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Same conservative exact-task progression can inform authored Today");
        var base =
                List.of(
                        new CalendarData.Entry(
                                CalendarData.Category.REST,
                                CalendarData.State.PLANNED,
                                "Rest",
                                null,
                                null),
                        new CalendarData.Entry(
                                CalendarData.Category.STRENGTH,
                                CalendarData.State.PLANNED,
                                "Strength routine",
                                null,
                                null));
        var calendar =
                CalendarData.withAuthoredGym(TODAY, TODAY, base, programme, java.util.Map.of());
        ok(
                calendar.size() == 1 && calendar.get(0).id().equals("today"),
                "Authored session replaces generic strength/rest calendar hints");
        ok(
                calendar.get(0).state() == CalendarData.State.PLANNED,
                "Planned authored session does not imply actual evidence");
        ok(
                CalendarData.withAuthoredGym(
                                        TODAY,
                                        TODAY.plusDays(1),
                                        base,
                                        programme,
                                        java.util.Map.of())
                                .get(0)
                                .state()
                        == CalendarData.State.NOT_COMPLETED,
                "Past unlinked authored work retains calendar semantics");
        ok(
                CalendarData.withAuthoredGym(
                                TODAY, TODAY, base, programme, java.util.Map.of("today", "actual"))
                        .isEmpty(),
                "Explicit linked actual suppresses duplicate planned calendar entry");
        ok(
                CalendarData.withAuthoredGym(
                                TODAY.minusDays(1), TODAY, base, programme, java.util.Map.of())
                        .equals(base),
                "Authored calendar does not invent sessions outside programme");
        var linked =
                StrengthTodaySummary.fromAuthored(
                        programme,
                        List.of(recent),
                        List.of(link("recent", "authored:v1", "today")),
                        TODAY,
                        NOW);
        ok(
                linked.todayCompleted() && linked.completedThisWeek() == 1,
                "Retained explicit prior-revision association counts once");
        for (String key :
                List.of(
                        "authored:v3",
                        "authored:v0",
                        "authored:vbad",
                        "other:v1",
                        "authored-extra:v1")) {
            var invalid =
                    StrengthTodaySummary.fromAuthored(
                            programme,
                            List.of(recent),
                            List.of(link("recent", key, "today")),
                            TODAY,
                            NOW);
            ok(!invalid.todayCompleted(), "Foreign/future/corrupt authored link rejected: " + key);
        }
        ok(
                !StrengthTodaySummary.fromAuthored(
                                programme,
                                List.of(),
                                List.of(link("deleted", "authored:v1", "today")),
                                TODAY,
                                NOW)
                        .todayCompleted(),
                "Deleted actual cannot complete authored Today");
        var future = actual("future", NOW.plusSeconds(3600), 7.0);
        var missing =
                StrengthTodaySummary.fromAuthored(
                        programme,
                        List.of(recent, future),
                        List.of(link("future", "authored:v1", "today")),
                        TODAY,
                        NOW);
        ok(
                !missing.todayCompleted() && missing.recommendation() == null,
                "Future evidence cannot complete or progress authored session");
        ok(
                StrengthTodaySummary.fromAuthored(
                                        programme,
                                        List.of(recent),
                                        List.of(),
                                        TODAY.plusDays(1),
                                        NOW)
                                .authoredToday()
                        == null,
                "No authored workout tomorrow is not fabricated into a session");
    }

    private static void standaloneWithoutProgramme() {
        var summary = summary(null, List.of(), List.of());
        ok(summary.today() == null, "No programme is a valid standalone state");
        ok(summary.plannedThisWeek() == 0, "No prescription means no invented planned sessions");
        ok(summary.recommendation() == null, "No prescription means no load recommendation");
        ok(
                summary.actualWorkoutsThisWeek() == 0,
                "Retained session count is zero for no recorded workouts");
        var session = actual("independent", NOW.minusSeconds(3600), 8.0);
        var free = summary(null, List.of(session), List.of());
        ok(
                free.actualWorkoutsThisWeek() == 1,
                "Standalone actual workout counts without race or programme");
        ok(free.recentPbExercises() == 1, "Actual working evidence may establish a PB");
        ok(
                free.completedThisWeek() == 0,
                "Standalone workout cannot complete an absent prescription");
    }

    private static void actualDoesNotImplyAdherence() {
        var programme = programme(1);
        var actual = actual("spontaneous", NOW.minusSeconds(3600), 8.0);
        var unlinked = summary(programme, List.of(actual), List.of());
        ok(unlinked.today().id().equals("today"), "Today's actual prescription is selected");
        ok(
                unlinked.plannedThisWeek() == 2,
                "Current calendar week includes its two preserved sessions");
        ok(
                unlinked.actualWorkoutsThisWeek() == 1,
                "Spontaneous workout remains useful actual evidence");
        ok(!unlinked.todayCompleted(), "Same date is not an explicit association");
        ok(unlinked.completedThisWeek() == 0, "Spontaneous workout does not satisfy adherence");
        var linked =
                summary(
                        programme,
                        List.of(actual),
                        List.of(link("spontaneous", "block:v1", "today")));
        ok(
                linked.todayCompleted(),
                "Explicit retained actual/prescription association completes today");
        ok(linked.completedThisWeek() == 1, "Explicit completed prescription contributes once");
        ok(programme.version() == 1, "Summary never rewrites programme version");
        ok(
                actual.exercises().get(0).sets().get(0).kilograms() == 80,
                "Actual loads remain untouched");
    }

    private static void retainedVersionAssociations() {
        var actual = actual("actual", NOW.minusSeconds(3600), 8.0);
        var current = programme(2);
        ok(
                summary(current, List.of(actual), List.of(link("actual", "block:v1", "today")))
                        .todayCompleted(),
                "An old preserved version's completed session remains completed after an immutable"
                        + " edit");
        for (String invalid :
                List.of("other:v1", "block:v3", "block:v0", "block:vbad", "block:v999999999999")) {
            ok(
                    !summary(current, List.of(actual), List.of(link("actual", invalid, "today")))
                            .todayCompleted(),
                    "Foreign, future or malformed programme identity cannot complete today's"
                            + " session: "
                            + invalid);
        }
        ok(
                !summary(current, List.of(), List.of(link("deleted-actual", "block:v1", "today")))
                        .todayCompleted(),
                "Deleted actual evidence cannot leave a completed badge");
        ok(
                summary(current, List.of(actual), List.of(link("actual", "block:v1", "missing")))
                                .completedThisWeek()
                        == 0,
                "Association to another prescription does not complete a current session");
        var duplicates =
                summary(
                        current,
                        List.of(actual),
                        List.of(
                                link("actual", "block:v1", "today"),
                                link("actual", "block:v2", "today")));
        ok(
                duplicates.completedThisWeek() == 1,
                "Retained associations do not double count a prescription");
    }

    private static void localWeekAndFutureEvidence() {
        var sundayUtc = actual("local-monday", Instant.parse("2026-10-25T23:30:00Z"), 7.0);
        var plusOne =
                new StrengthSession(
                        sundayUtc.id(),
                        sundayUtc.startedAt(),
                        ZoneOffset.ofHours(1),
                        sundayUtc.duration(),
                        sundayUtc.rpe(),
                        sundayUtc.notes(),
                        sundayUtc.exercises());
        var future = actual("future", NOW.plusSeconds(3600), 7.0);
        var summary =
                summary(
                        programme(1),
                        List.of(plusOne, future),
                        List.of(link("future", "block:v1", "today")));
        ok(summary.weekStart().equals(LocalDate.of(2026, 10, 26)), "Week starts on Monday");
        ok(
                summary.actualWorkoutsThisWeek() == 1,
                "Session offset determines local week, not UTC day");
        ok(!summary.todayCompleted(), "Future actual timestamps cannot complete a prescription");
        ok(summary.recentPbExercises() == 1, "Future PB evidence is excluded");
    }

    private static void recommendationsNeedRecentComparableEvidence() {
        var recent = actual("recent", NOW.minus(Duration.ofDays(2)), 7.0);
        var prior = actual("prior", NOW.minus(Duration.ofDays(6)), 7.0);
        var suggested = summary(programme(1), List.of(recent, prior), List.of()).recommendation();
        ok(
                suggested != null
                        && suggested.action()
                                == StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                "Two recent top-range working sessions may surface an optional progression review");
        ok(
                suggested.exerciseId().equals("bench-press"),
                "Recommendation preserves exact exercise identity");
        ok(
                summary(programme(1), List.of(recent), List.of()).recommendation() == null,
                "One successful session is not progression evidence");
        var stale = actual("stale", NOW.minus(Duration.ofDays(60)), 7.0);
        ok(
                summary(programme(1), List.of(recent, stale), List.of()).recommendation() == null,
                "Stale second observation cannot support an increase");
        var future = actual("future", NOW.plus(Duration.ofDays(1)), 7.0);
        ok(
                summary(programme(1), List.of(recent, future), List.of()).recommendation() == null,
                "Future observation cannot support an increase");
        var unknownEffort = actual("unknown", NOW.minus(Duration.ofDays(6)), null);
        ok(
                summary(programme(1), List.of(recent, unknownEffort), List.of()).recommendation()
                        == null,
                "Missing effort is not assumed easy");
        var hard = actual("hard", NOW.minus(Duration.ofHours(1)), 9.0);
        var recovery =
                summary(programme(1), List.of(hard, recent, prior), List.of()).recommendation();
        ok(
                recovery != null
                        && recovery.action()
                                == StrengthProgressionAdvisor.Action.REDUCE_OR_SIMPLIFY,
                "Recent excessive effort takes precedence over an increase");
    }

    private static void estimatesAreNotRecentActualPbs() {
        var old = actual("first", NOW.minus(Duration.ofDays(60)), 7.0);
        var recent = actual("same", NOW.minus(Duration.ofDays(1)), 7.0);
        ok(
                summary(null, List.of(old, recent), List.of()).recentPbExercises() == 0,
                "Repeating old working evidence does not manufacture a recent PB");
        var exercise = ExerciseCatalog.definition("bench-press").exercise();
        var warm =
                new StrengthSession(
                        "warm",
                        NOW.minusSeconds(1200),
                        ZoneOffset.UTC,
                        Duration.ofMinutes(20),
                        null,
                        "",
                        List.of(
                                new StrengthSession.ExerciseSets(
                                        exercise,
                                        List.of(
                                                new StrengthSession.SetEntry(
                                                        "warm-set", 8, 100, null, true)))));
        ok(
                summary(null, List.of(warm), List.of()).recentPbExercises() == 0,
                "Warm-up sets are not PB evidence");
    }

    private static StrengthTodaySummary summary(
            GymProgramme programme,
            List<StrengthSession> actual,
            List<StrengthTodaySummary.Association> links) {
        return StrengthTodaySummary.from(programme, actual, links, TODAY, NOW);
    }

    private static StrengthTodaySummary.Association link(
            String actual, String version, String prescribed) {
        return new StrengthTodaySummary.Association(actual, version, prescribed);
    }

    private static GymProgramme programme(int version) {
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
                        2,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        var prescription =
                new GymProgramme.Prescription(
                        "bench-press", 2, 6, 8, null, null, 6, 8, 120, "Preserved target");
        return new GymProgramme(
                "block",
                version,
                NOW.minus(Duration.ofDays(7)),
                "Athlete accepted",
                settings,
                TODAY.minusDays(7),
                TODAY.plusDays(34),
                List.of(
                        new GymProgramme.Session(
                                "today", TODAY, "Upper", List.of(prescription), "Preserved"),
                        new GymProgramme.Session(
                                "next",
                                TODAY.plusDays(2),
                                "Upper",
                                List.of(prescription),
                                "Preserved")),
                List.of());
    }

    private static StrengthSession actual(String id, Instant at, Double effort) {
        var exercise = ExerciseCatalog.definition("bench-press").exercise();
        return new StrengthSession(
                id,
                at,
                ZoneOffset.UTC,
                Duration.ofMinutes(40),
                effort,
                "Actual training",
                List.of(
                        new StrengthSession.ExerciseSets(
                                exercise,
                                List.of(
                                        new StrengthSession.SetEntry(
                                                id + "-1", 8, 80, effort, false),
                                        new StrengthSession.SetEntry(
                                                id + "-2", 8, 80, effort, false)))));
    }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
