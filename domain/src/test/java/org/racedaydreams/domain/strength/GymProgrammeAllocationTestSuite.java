package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.WorkoutCategory;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Regressions for six start-anchored weeks and exercise-backed scheduling context. */
public final class GymProgrammeAllocationTestSuite {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 26);
    private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
    private static final GymProgrammeGenerator GENERATOR = new GymProgrammeGenerator();
    private static final List<Set<String>> EQUIPMENT =
            List.of(
                    Set.of(),
                    Set.of("bodyweight"),
                    Set.of("dumbbells"),
                    Set.of("barbell"),
                    Set.of("squat rack"),
                    Set.of("bench"),
                    Set.of("machines"),
                    Set.of("cables"),
                    Set.of("kettlebells"),
                    Set.of("pull-up bar"),
                    Set.of("trap bar"),
                    Set.of("sled"),
                    Set.of("full gym"),
                    Set.of("barbell", "squat rack", "bench"),
                    Set.of("dumbbells", "bench"));
    private static int checks;

    public static void main(String[] args) {
        allocationAcrossEveryStartAndFrequency();
        equipmentRemainsAvailable();
        lowerSessionsHaveDistinctEmphases();
        classificationUsesRetainedExercises();
        demandingRunContextIsDirectional();
        schedulingPrefersSeparationAndSofterContext();
        fullBodyAdjacencyCountsAsLowerWork();
        lowerAdjacencyCrossesProgrammeWeekBoundaries();
        unavoidablePlannedAdjacencyRemainsExplicit();
        forcedConflictsKeepTheRequestedProgramme();
        explanationsDescribeRetainedExercises();
        existingImmutableProgrammeIsPreserved();
        System.out.println("GymProgrammeAllocationTestSuite: " + checks + " checks passed.");
    }

    private static void allocationAcrossEveryStartAndFrequency() {
        for (int startOffset = 0; startOffset < 7; startOffset++) {
            var start = MONDAY.plusDays(startOffset);
            for (int frequency = 1; frequency <= 6; frequency++) {
                for (boolean allDays : List.of(false, true)) {
                    var days =
                            allDays
                                    ? EnumSet.allOf(DayOfWeek.class)
                                    : availableDays(start, frequency);
                    var equipment = EQUIPMENT.get((startOffset * 6 + frequency) % EQUIPMENT.size());
                    var settings =
                            settings(
                                    days,
                                    frequency,
                                    equipment,
                                    30,
                                    GymProgramme.Priority.RUNNING_FIRST);
                    var plan = generate(settings, start, List.of());
                    verifyAllocation(plan, settings, start);
                }
            }
        }
        var settings =
                settings(
                        EnumSet.allOf(DayOfWeek.class),
                        2,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Priority.BALANCED);
        var start = MONDAY.plusDays(1);
        ok(
                generate(settings, start, List.of()).equals(generate(settings, start, List.of())),
                "Tuesday allocation remains deterministic");
    }

    private static void equipmentRemainsAvailable() {
        for (var equipment : EQUIPMENT) {
            for (int frequency = 1; frequency <= 6; frequency++) {
                var start = MONDAY.plusDays(frequency % 7);
                var settings =
                        settings(
                                availableDays(start, frequency),
                                frequency,
                                equipment,
                                45,
                                GymProgramme.Priority.BALANCED);
                var plan = generate(settings, start, List.of());
                verifyAllocation(plan, settings, start);
                for (var session : plan.sessions()) {
                    for (var exercise : session.exercises()) {
                        ok(
                                GymProgrammeGenerator.available(exercise.exerciseId(), equipment),
                                "Every retained exercise respects selected equipment: "
                                        + equipment);
                    }
                }
            }
        }
    }

    private static void verifyAllocation(
            GymProgramme plan, GymProgramme.Settings settings, LocalDate start) {
        ok(plan.start().equals(start), "Programme retains its chosen start date");
        ok(plan.end().equals(start.plusDays(41)), "Programme lasts exactly six complete weeks");
        ok(
                plan.settings().equals(settings),
                "Allocation does not change preferences or availability");
        ok(
                plan.sessions().size() == settings.frequency() * 6,
                "Exactly six times requested frequency");
        var dates = new HashSet<LocalDate>();
        int[] weeks = new int[6];
        for (var session : plan.sessions()) {
            ok(dates.add(session.date()), "Session dates are unique");
            ok(
                    !session.date().isBefore(start) && !session.date().isAfter(plan.end()),
                    "Session date stays inside the chosen block");
            ok(
                    settings.days().contains(session.date().getDayOfWeek()),
                    "Only chosen weekdays are used");
            int week = (int) ChronoUnit.DAYS.between(start, session.date()) / 7;
            weeks[week]++;
        }
        for (int count : weeks) {
            ok(
                    count == settings.frequency(),
                    "Each start-anchored week has the requested frequency");
        }
    }

    private static void lowerSessionsHaveDistinctEmphases() {
        for (int frequency : List.of(4, 5, 6)) {
            var settings =
                    settings(
                            availableDays(MONDAY, frequency),
                            frequency,
                            Set.of("full gym"),
                            45,
                            GymProgramme.Priority.BALANCED);
            var plan = generate(settings, MONDAY, List.of());
            for (int week = 0; week < 6; week++) {
                var weekStart = MONDAY.plusWeeks(week);
                var lower =
                        plan.sessions().stream()
                                .filter(
                                        session ->
                                                !session.date().isBefore(weekStart)
                                                        && session.date()
                                                                .isBefore(weekStart.plusWeeks(1)))
                                .filter(session -> session.title().startsWith("Lower"))
                                .toList();
                ok(lower.size() == 2, "Split contains two lower sessions each week");
                var firstPatterns = new HashSet<String>();
                for (var session : lower) {
                    var guide = TechniqueGuide.forExercise(session.exercises().get(0).exerciseId());
                    firstPatterns.add(guide.pattern());
                }
                ok(
                        firstPatterns.equals(Set.of("squat", "hinge")),
                        "Lower-session ordinal provides both squat and hinge emphases");
            }
        }
    }

    private static void classificationUsesRetainedExercises() {
        ok(
                GymProgrammeGenerator.hasLowerBodyWork(List.of(prescription("back-squat"))),
                "Retained squat is lower work");
        ok(
                GymProgrammeGenerator.hasLowerBodyWork(List.of(prescription("kettlebell-swing"))),
                "A meaningful full-region movement is lower work");
        ok(
                GymProgrammeGenerator.hasLowerBodyWork(
                        List.of(prescription("single-leg-calf-raise-bodyweight"))),
                "Retained calf work is lower work");
        ok(
                !GymProgrammeGenerator.hasLowerBodyWork(
                        List.of(prescription("push-up"), prescription("barbell-row"))),
                "Upper exercises alone are not lower work");
        ok(
                !GymProgrammeGenerator.hasLowerBodyWork(
                        List.of(
                                prescription("plank"),
                                prescription("bird-dog"),
                                prescription("dead-bug"))),
                "Core-only exercises are not meaningful lower work");
        ok(
                !GymProgrammeGenerator.hasLowerBodyWork(
                        List.of(prescription("retained-custom-exercise"))),
                "Unknown retained IDs do not invent lower classification");
        ok(
                !GymProgrammeGenerator.hasLowerBodyWork(List.of()),
                "Empty retained list has no lower work");
        var settings =
                settings(
                        Set.of(DayOfWeek.MONDAY),
                        1,
                        Set.of("bodyweight"),
                        30,
                        GymProgramme.Priority.RUNNING_FIRST);
        for (var session : generate(settings, MONDAY, List.of()).sessions()) {
            ok(session.title().startsWith("Full body"), "Low-frequency template is full body");
            ok(
                    GymProgrammeGenerator.hasLowerBodyWork(session.exercises()),
                    "Retained full-body lower movements drive classification");
        }
    }

    private static void demandingRunContextIsDirectional() {
        var demanding =
                EnumSet.of(
                        WorkoutCategory.LONG_RUN,
                        WorkoutCategory.INTERVALS,
                        WorkoutCategory.TEMPO_THRESHOLD_RUN,
                        WorkoutCategory.RACE_PACE_RUN,
                        WorkoutCategory.PROGRESSION_RUN,
                        WorkoutCategory.RACE,
                        WorkoutCategory.TIME_TRIAL);
        for (var category : WorkoutCategory.values()) {
            ok(
                    GymProgrammeGenerator.demanding(category) == demanding.contains(category),
                    "Demanding categories retain their established classification");
            var runs = List.of(run("key", MONDAY.plusDays(2), category));
            for (int offset = -1; offset <= 1; offset++) {
                var day = MONDAY.plusDays(2 + offset);
                var context = GymProgrammeGenerator.conflicts(day, true, runs, Set.of());
                if (demanding.contains(category)) {
                    ok(
                            context.size() == 1,
                            "Each nearby demanding run has one directional context");
                    var expected =
                            offset < 0 ? "day before" : offset == 0 ? "same day as" : "day after";
                    ok(context.get(0).contains(expected), "Context distinguishes " + expected);
                    if (offset > 0) {
                        ok(
                                context.get(0).contains("Review recovery"),
                                "After-run context asks for recovery review");
                    }
                } else {
                    ok(context.isEmpty(), "Ordinary runs do not generate demanding-run warnings");
                }
                ok(
                        GymProgrammeGenerator.conflicts(day, false, runs, Set.of()).isEmpty(),
                        "Sessions without retained lower work have no lower conflict");
            }
            ok(
                    GymProgrammeGenerator.conflicts(MONDAY, true, runs, Set.of()).isEmpty(),
                    "Two-day separation has no neighbouring-run context");
        }
        ok(
                GymProgrammeGenerator.conflicts(
                                        MONDAY, true, List.of(), Set.of(MONDAY.minusDays(1)))
                                .size()
                        == 1,
                "Actually recorded hard lower work still contributes context");
        ok(
                GymProgrammeGenerator.conflicts(
                                MONDAY, false, List.of(), Set.of(MONDAY.minusDays(1)))
                        .isEmpty(),
                "Core and upper sessions do not gain actual-lower warnings");
    }

    private static void schedulingPrefersSeparationAndSofterContext() {
        var runs = new ArrayList<PlannedWorkout>();
        for (int week = 0; week < 6; week++) {
            runs.add(
                    run(
                            "weekly-key-" + week,
                            MONDAY.plusWeeks(week).plusDays(1),
                            WorkoutCategory.INTERVALS));
        }
        for (var priority : GymProgramme.Priority.values()) {
            assertSelectedDay(
                    Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
                    DayOfWeek.WEDNESDAY,
                    priority,
                    runs,
                    "Day after is softer than day before");
            assertSelectedDay(
                    Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                    DayOfWeek.WEDNESDAY,
                    priority,
                    runs,
                    "Day after is softer than same day");
            assertSelectedDay(
                    Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                    DayOfWeek.THURSDAY,
                    priority,
                    runs,
                    "Separated day is preferable to day after");
        }
    }

    private static void assertSelectedDay(
            Set<DayOfWeek> available,
            DayOfWeek expected,
            GymProgramme.Priority priority,
            List<PlannedWorkout> runs,
            String why) {
        var settings = settings(available, 1, Set.of("full gym"), 30, priority);
        var plan = generate(settings, MONDAY, runs);
        verifyAllocation(plan, settings, MONDAY);
        for (var session : plan.sessions()) {
            ok(session.date().getDayOfWeek() == expected, why + " with " + priority);
        }
    }

    private static void fullBodyAdjacencyCountsAsLowerWork() {
        var settings =
                settings(
                        Set.of(
                                DayOfWeek.TUESDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.SATURDAY),
                        3,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Priority.RUNNING_FIRST);
        var plan = generate(settings, MONDAY, List.of());
        verifyAllocation(plan, settings, MONDAY);
        for (int i = 1; i < plan.sessions().size(); i++) {
            ok(
                    ChronoUnit.DAYS.between(
                                    plan.sessions().get(i - 1).date(),
                                    plan.sessions().get(i).date())
                            > 1,
                    "Meaningful full-body lower sessions avoid adjacent dates when separation is"
                            + " available");
        }
    }

    private static void lowerAdjacencyCrossesProgrammeWeekBoundaries() {
        var start = MONDAY.plusDays(1);
        for (var priority : GymProgramme.Priority.values()) {
            var settings =
                    new GymProgramme.Settings(
                            GymProgramme.Goal.GENERAL,
                            priority,
                            GymProgramme.Experience.INTERMEDIATE,
                            Set.of(
                                    DayOfWeek.TUESDAY,
                                    DayOfWeek.WEDNESDAY,
                                    DayOfWeek.FRIDAY,
                                    DayOfWeek.MONDAY),
                            3,
                            Set.of("full gym"),
                            45,
                            GymProgramme.Effort.RPE);
            var plan = generate(settings, start, List.of());
            verifyAllocation(plan, settings, start);
            for (int i = 1; i < plan.sessions().size(); i++) {
                ok(
                        ChronoUnit.DAYS.between(
                                        plan.sessions().get(i - 1).date(),
                                        plan.sessions().get(i).date())
                                > 1,
                        "An available Wednesday avoids Monday-to-Tuesday lower work across block"
                                + " weeks");
            }
            ok(
                    plan.tradeoffs().stream().noneMatch(text -> text.contains("consecutive days")),
                    "A separated block does not claim consecutive lower sessions");
            ok(
                    plan.equals(generate(settings, start, List.of())),
                    "Cross-week allocation remains deterministic");
        }
    }

    private static void unavoidablePlannedAdjacencyRemainsExplicit() {
        var start = MONDAY.plusDays(1);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.BALANCED,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
                        3,
                        Set.of("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        var plan = generate(settings, start, List.of());
        verifyAllocation(plan, settings, start);
        ok(
                plan.tradeoffs().size() == 5,
                "All five unavoidable consecutive block-week boundaries are explained");
        for (int week = 1; week < 6; week++) {
            var day = start.plusWeeks(week);
            var previousLabel =
                    day.minusDays(1)
                            .format(
                                    java.time.format.DateTimeFormatter.ofPattern(
                                            "d MMM uuuu", Locale.UK));
            var dayLabel =
                    day.format(
                            java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK));
            ok(
                    plan.tradeoffs().stream()
                            .anyMatch(
                                    text ->
                                            text.contains("Planned lower-body")
                                                    && text.contains(previousLabel)
                                                    && text.contains(dayLabel)
                                                    && text.contains("nothing has been removed")),
                    "Unavoidable planned adjacency preserves both dates and an explicit choice");
        }
        ok(
                plan.tradeoffs().stream().noneMatch(text -> text.contains("actually recorded")),
                "Planned adjacency is never represented as completed evidence");
    }

    private static void forcedConflictsKeepTheRequestedProgramme() {
        var runs = new ArrayList<PlannedWorkout>();
        for (int day = 0; day < 42; day++) {
            runs.add(run("forced-" + day, MONDAY.plusDays(day), WorkoutCategory.LONG_RUN));
        }
        for (int frequency : List.of(1, 3, 4, 6)) {
            var settings =
                    settings(
                            availableDays(MONDAY, frequency),
                            frequency,
                            Set.of("bodyweight"),
                            30,
                            GymProgramme.Priority.RUNNING_FIRST);
            var plan = generate(settings, MONDAY, runs);
            verifyAllocation(plan, settings, MONDAY);
            for (var session : plan.sessions()) {
                var context =
                        GymProgrammeGenerator.conflicts(
                                session.date(),
                                GymProgrammeGenerator.hasLowerBodyWork(session.exercises()),
                                runs,
                                Set.of());
                ok(
                        plan.tradeoffs().containsAll(context),
                        "All retained lower conflicts remain inspectable");
                if (GymProgrammeGenerator.hasLowerBodyWork(session.exercises())) {
                    ok(
                            !context.isEmpty(),
                            "Unavoidable lower conflict is explained without removing a session");
                }
            }
        }
    }

    private static void explanationsDescribeRetainedExercises() {
        for (var equipment :
                List.of(
                        Set.of("bodyweight"),
                        Set.of("full gym"),
                        Set.of("barbell"),
                        Set.of("dumbbells", "bench"))) {
            for (int minutes : List.of(30, 45, 60, 75)) {
                for (int frequency : List.of(2, 4)) {
                    var settings =
                            settings(
                                    availableDays(MONDAY, frequency),
                                    frequency,
                                    equipment,
                                    minutes,
                                    GymProgramme.Priority.RUNNING_FIRST);
                    var plan = generate(settings, MONDAY, List.of());
                    for (var session : plan.sessions()) {
                        verifyExplanation(session);
                    }
                }
            }
        }
        for (var goal : GymProgramme.Goal.values()) {
            for (var experience : GymProgramme.Experience.values()) {
                var settings =
                        new GymProgramme.Settings(
                                goal,
                                GymProgramme.Priority.STRENGTH_FIRST,
                                experience,
                                Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                                2,
                                Set.of("bodyweight"),
                                30,
                                GymProgramme.Effort.RIR);
                var plan = generate(settings, MONDAY, List.of());
                for (var session : plan.sessions()) {
                    verifyExplanation(session);
                }
                boolean retainedCore =
                        plan.sessions().stream()
                                .flatMap(session -> session.exercises().stream())
                                .anyMatch(
                                        exercise ->
                                                ExerciseCatalog.definition(exercise.exerciseId())
                                                        .category()
                                                        .equals("Core"));
                if (!retainedCore) {
                    ok(
                            plan.tradeoffs().stream()
                                    .noneMatch(tradeoff -> tradeoff.contains("Trunk work fills")),
                            "Equipment trade-off does not claim truncated trunk work");
                }
            }
        }
        var shortSettings =
                settings(
                        Set.of(DayOfWeek.MONDAY),
                        1,
                        Set.of("full gym"),
                        30,
                        GymProgramme.Priority.RUNNING_FIRST);
        var shortSession = generate(shortSettings, MONDAY, List.of()).sessions().get(0);
        ok(
                shortSession.exercises().stream()
                        .noneMatch(p -> movementLabel(p.exerciseId()).equals("calf")),
                "Short budget actually truncates calf work in this regression fixture");
        ok(
                !shortSession.explanation().toLowerCase(Locale.ROOT).contains("calf"),
                "Truncated calf work is not claimed");
        ok(
                !shortSession.explanation().toLowerCase(Locale.ROOT).contains("hinge"),
                "Squat-focused short session does not claim a hinge");
        ok(
                !shortSession.explanation().toLowerCase(Locale.ROOT).contains("trunk"),
                "Short session without retained core work does not claim trunk work");
        var retainedCoreSettings =
                settings(
                        Set.of(DayOfWeek.MONDAY),
                        1,
                        Set.of("bodyweight"),
                        30,
                        GymProgramme.Priority.RUNNING_FIRST);
        var retainedCorePlan = generate(retainedCoreSettings, MONDAY, List.of());
        ok(
                retainedCorePlan.sessions().get(0).exercises().stream()
                        .anyMatch(
                                exercise ->
                                        ExerciseCatalog.definition(exercise.exerciseId())
                                                .category()
                                                .equals("Core")),
                "Positive equipment trade-off fixture actually retains Core work");
        ok(
                retainedCorePlan.tradeoffs().stream()
                        .anyMatch(tradeoff -> tradeoff.contains("Trunk work fills")),
                "Retained trunk fallback remains explainable when no pulling equipment is"
                        + " selected");
    }

    private static void verifyExplanation(GymProgramme.Session session) {
        var labels = new HashSet<String>();
        for (var exercise : session.exercises()) {
            var label = movementLabel(exercise.exerciseId());
            if (!label.isEmpty()) {
                labels.add(label);
            }
        }
        var explanation = session.explanation().toLowerCase(Locale.ROOT);
        for (var label :
                List.of(
                        "squat",
                        "hinge",
                        "unilateral",
                        "hip extension",
                        "calf",
                        "horizontal push",
                        "overhead push",
                        "upper-body pull",
                        "trunk")) {
            ok(
                    !explanation.contains(label) || labels.contains(label),
                    "Explanation only claims retained " + label + " work");
        }
        for (var label : labels) {
            ok(explanation.contains(label), "Retained " + label + " work informs the explanation");
        }
    }

    private static String movementLabel(String exerciseId) {
        var guide = TechniqueGuide.forExercise(exerciseId);
        if (guide == null) {
            return "";
        }
        return switch (guide.pattern()) {
            case "squat" -> "squat";
            case "hinge" -> "hinge";
            case "split" -> "unilateral";
            case "bridge" -> "hip extension";
            case "calf" -> "calf";
            case "press" -> "horizontal push";
            case "overhead" -> "overhead push";
            case "row", "pull" -> "upper-body pull";
            case "core", "plank" -> "trunk";
            default -> "";
        };
    }

    private static void existingImmutableProgrammeIsPreserved() {
        var days = EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY);
        var equipment = new HashSet<>(Set.of("full gym"));
        var settings = settings(days, 2, equipment, 45, GymProgramme.Priority.BALANCED);
        var loadReference =
                new GymProgramme.Prescription(
                        "back-squat",
                        3,
                        4,
                        6,
                        null,
                        null,
                        6,
                        8,
                        150,
                        "Retained original intent",
                        55.0);
        var exercises = new ArrayList<>(List.of(loadReference));
        var oldSession =
                new GymProgramme.Session(
                        "retained-old-session",
                        MONDAY.plusDays(1),
                        "My existing title",
                        exercises,
                        "Original saved explanation");
        var sessions = new ArrayList<>(List.of(oldSession));
        var tradeoffs = new ArrayList<>(List.of("Original saved trade-off"));
        var old =
                new GymProgramme(
                        "existing",
                        3,
                        NOW,
                        "Existing immutable programme",
                        settings,
                        MONDAY,
                        MONDAY.plusDays(41),
                        sessions,
                        tradeoffs);
        var expected =
                new GymProgramme(
                        "existing",
                        3,
                        NOW,
                        "Existing immutable programme",
                        settings,
                        MONDAY,
                        MONDAY.plusDays(41),
                        List.of(oldSession),
                        List.of("Original saved trade-off"));
        exercises.clear();
        sessions.clear();
        tradeoffs.clear();
        days.clear();
        equipment.clear();
        var next =
                GENERATOR.generate(
                        old.id(),
                        old.version() + 1,
                        NOW.plusSeconds(60),
                        "Explicit replacement generation",
                        old.settings(),
                        MONDAY.plusDays(1),
                        List.of(),
                        List.of());
        ok(
                old.equals(expected),
                "Generating a newer block does not rewrite old immutable programme data");
        ok(
                old.sessions().get(0).exercises().get(0).referenceLoadKg().equals(55.0),
                "Old exact-exercise load reference remains unchanged");
        ok(
                old.sessions().get(0).title().equals("My existing title"),
                "Old session title is retained");
        ok(
                old.sessions().get(0).explanation().equals("Original saved explanation"),
                "Old explanation is retained rather than regenerated");
        ok(
                GymProgrammeGenerator.hasLowerBodyWork(old.sessions().get(0).exercises()),
                "Old custom session titles do not hide actual retained lower work");
        ok(
                next.version() == 4 && next.sessions().size() == 12,
                "Explicit new version uses corrected allocation");
        ok(
                next.sessions().stream()
                        .flatMap(s -> s.exercises().stream())
                        .allMatch(p -> p.referenceLoadKg() == null),
                "Generation does not invent or copy load references from an old programme");
    }

    private static GymProgramme.Settings settings(
            Set<DayOfWeek> days,
            int frequency,
            Set<String> equipment,
            int minutes,
            GymProgramme.Priority priority) {
        return new GymProgramme.Settings(
                GymProgramme.Goal.SUPPORT_RUNNING,
                priority,
                GymProgramme.Experience.BEGINNER,
                days,
                frequency,
                equipment,
                minutes,
                GymProgramme.Effort.RPE);
    }

    private static Set<DayOfWeek> availableDays(LocalDate start, int frequency) {
        var days = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < frequency; i++) {
            days.add(start.plusDays(i).getDayOfWeek());
        }
        return days;
    }

    private static GymProgramme generate(
            GymProgramme.Settings settings, LocalDate start, List<PlannedWorkout> runs) {
        return GENERATOR.generate(
                "allocation",
                1,
                NOW,
                "Focused allocation regression",
                settings,
                start,
                runs,
                List.of());
    }

    private static PlannedWorkout run(String id, LocalDate date, WorkoutCategory category) {
        return new PlannedWorkout(id, date, category, 7.0, null, List.of(), "Fixture run");
    }

    private static GymProgramme.Prescription prescription(String exerciseId) {
        var definition = ExerciseCatalog.definition(exerciseId);
        boolean duration = definition != null && definition.exercise().measurement().usesDuration();
        return new GymProgramme.Prescription(
                exerciseId,
                2,
                duration ? 0 : 6,
                duration ? 0 : 10,
                duration ? 30 : null,
                null,
                5,
                7,
                90,
                "Fixture prescription");
    }

    private static void ok(boolean condition, String why) {
        checks++;
        if (!condition) {
            throw new AssertionError(why);
        }
    }
}
