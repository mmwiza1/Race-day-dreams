package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.StrengthProgramme.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/** Programme construction and revision invariants, using the real canonical catalogue. */
public final class StrengthProgrammeTestSuite {
    static int checks;
    static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    static final LocalDate START = LocalDate.of(2026, 10, 27);
    static final StrengthProgrammeGenerator ENGINE = new StrengthProgrammeGenerator();
    static final Set<ExerciseMetadata.Equipment> ALL =
            EnumSet.allOf(ExerciseMetadata.Equipment.class);

    public static void main(String[] args) {
        validationAndImmutability();
        startAnchoredWeeks();
        templatesAndBlocks();
        goalsAndExperience();
        futureOnlyEditsAndSubstitutions();
        customDraftAuthoring();
        schedulingAndRaceWeek();
        futureRegeneration();
        simulator();
        System.out.println("StrengthProgrammeTestSuite: " + checks + " checks passed.");
    }

    static Settings settings(
            GymProgramme.Goal goal,
            GymProgramme.Experience experience,
            GymProgramme.Priority priority,
            int frequency,
            int minutes,
            Set<ExerciseMetadata.Equipment> equipment) {
        return new Settings(
                goal,
                priority,
                experience,
                EnumSet.allOf(DayOfWeek.class),
                frequency,
                minutes,
                equipment);
    }

    static StrengthProgrammeGenerator.Request request(
            Settings s,
            int weeks,
            String template,
            Set<String> excluded,
            PrimaryFocus focus,
            Set<LocalDate> runs,
            LocalDate race,
            boolean recovery) {
        return new StrengthProgrammeGenerator.Request(
                "test",
                NOW,
                "My programme",
                s,
                START,
                weeks,
                template,
                focus,
                runs,
                race,
                excluded,
                Set.of("bench-press"),
                recovery);
    }

    static StrengthProgramme generate(Settings s, int weeks) {
        return ENGINE.generate(
                request(s, weeks, null, Set.of(), PrimaryFocus.STRENGTH, Set.of(), null, false));
    }

    static void validationAndImmutability() {
        var equipment = EnumSet.allOf(ExerciseMetadata.Equipment.class);
        var settings =
                settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        GymProgramme.Priority.BALANCED,
                        2,
                        45,
                        equipment);
        equipment.clear();
        check(!settings.equipment().isEmpty(), "settings copied");
        rejects(() -> settings.equipment().clear());
        rejects(
                () ->
                        settings(
                                GymProgramme.Goal.GENERAL,
                                GymProgramme.Experience.BEGINNER,
                                GymProgramme.Priority.BALANCED,
                                0,
                                45,
                                ALL));
        rejects(
                () ->
                        settings(
                                GymProgramme.Goal.GENERAL,
                                GymProgramme.Experience.BEGINNER,
                                GymProgramme.Priority.BALANCED,
                                2,
                                19,
                                ALL));
        var p = generate(settings, 6);
        rejects(() -> p.blocks().clear());
        rejects(() -> p.workouts().clear());
        rejects(
                () ->
                        new StrengthProgramme(
                                p.id(),
                                2,
                                0,
                                NOW,
                                p.name(),
                                p.origin(),
                                p.templateId(),
                                p.settings(),
                                p.blocks(),
                                p.start(),
                                "Invalid lineage",
                                p.tradeoffs()));
        rejects(
                () ->
                        new StrengthProgramme(
                                p.id(),
                                1,
                                0,
                                NOW,
                                " ",
                                p.origin(),
                                null,
                                p.settings(),
                                p.blocks(),
                                p.start(),
                                "Invalid title",
                                p.tradeoffs()));
        var b = p.blocks().get(0);
        rejects(
                () ->
                        new Block(
                                b.id(),
                                b.phase(),
                                b.name(),
                                b.start(),
                                0,
                                b.workouts(),
                                b.explanation()));
        var w = b.workouts().get(0);
        var slots = new ArrayList<>(w.definition().slots());
        var old = slots.get(0);
        slots.add(new GymWorkoutDefinition.Slot("duplicate", old.exercise(), old.target(), null));
        rejects(
                () ->
                        new Workout(
                                w.id(),
                                w.date(),
                                45,
                                new GymWorkoutDefinition("dupe", 1, NOW, "Duplicate", slots),
                                null));
    }

    static void startAnchoredWeeks() {
        for (int day = 0; day < 7; day++)
            for (int frequency = 1; frequency <= 6; frequency++) {
                var s =
                        settings(
                                GymProgramme.Goal.GENERAL,
                                GymProgramme.Experience.INTERMEDIATE,
                                GymProgramme.Priority.BALANCED,
                                frequency,
                                60,
                                ALL);
                var r = request(s, 6, null, Set.of(), PrimaryFocus.STRENGTH, Set.of(), null, false);
                r =
                        new StrengthProgrammeGenerator.Request(
                                r.id(),
                                r.at(),
                                r.name(),
                                r.settings(),
                                START.plusDays(day),
                                r.weeks(),
                                r.templateId(),
                                r.focus(),
                                r.demandingRuns(),
                                r.raceDate(),
                                r.exclusions(),
                                r.favourites(),
                                false);
                var p = ENGINE.generate(r);
                check(p.workouts().size() == 6 * frequency, "exact six-week allocation");
                check(
                        p.end().equals(p.start().plusWeeks(6).minusDays(1)),
                        "no seventh calendar week allocation");
                check(
                        new HashSet<>(
                                                p.workouts().stream()
                                                        .map(Workout::date)
                                                        .collect(Collectors.toList()))
                                        .size()
                                == p.workouts().size(),
                        "unique dates");
            }
    }

    static void templatesAndBlocks() {
        check(StrengthProgrammeTemplates.all().size() == 9, "nine deliberate starting structures");
        var ids = new HashSet<String>();
        for (var t : StrengthProgrammeTemplates.all()) {
            check(ids.add(t.id()), "stable unique template ID");
            var s =
                    settings(
                            t.defaultGoal(),
                            GymProgramme.Experience.INTERMEDIATE,
                            GymProgramme.Priority.BALANCED,
                            t.frequency(),
                            t.suggestedMinutes(),
                            ALL);
            var p =
                    ENGINE.generate(
                            request(
                                    s,
                                    10,
                                    t.id(),
                                    Set.of(),
                                    PrimaryFocus.STRENGTH,
                                    Set.of(),
                                    null,
                                    true));
            check(
                    p.origin() == Origin.TEMPLATE && p.templateId().equals(t.id()),
                    "template provenance");
            check(p.workouts().size() == 10 * t.frequency(), "template frequency");
            check(
                    p.blocks().stream()
                            .map(Block::phase)
                            .collect(Collectors.toSet())
                            .equals(EnumSet.allOf(Phase.class)),
                    "four real phases");
            var first = p.blocks().get(0).workouts().get(0).definition().slots().get(0).target();
            var development =
                    p.blocks().get(1).workouts().get(0).definition().slots().get(0).target();
            check(!first.equals(development), "development changes effort/volume, not only label");
            var recovery = p.blocks().get(3).workouts().get(0).definition().slots().get(0).target();
            check(
                    recovery.sets() == 1 && recovery.effort().maximum() <= 6,
                    "optional assessment not max testing");
            if (t.frequency() == 6) {
                var week = p.blocks().get(0).workouts().subList(0, 6);
                check(
                        !week.get(0)
                                .definition()
                                .slots()
                                .get(0)
                                .exercise()
                                .id()
                                .equals(week.get(3).definition().slots().get(0).exercise().id()),
                        "push A/B primaries vary");
                check(
                        !week.get(2)
                                .definition()
                                .slots()
                                .get(0)
                                .exercise()
                                .id()
                                .equals(week.get(5).definition().slots().get(0).exercise().id()),
                        "squat and hinge days vary");
            }
        }
        rejects(() -> StrengthProgrammeTemplates.require("made-up"));
        var s =
                settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        GymProgramme.Priority.BALANCED,
                        2,
                        45,
                        ALL);
        rejects(
                () ->
                        request(
                                s,
                                6,
                                "push-pull-legs-6",
                                Set.of(),
                                PrimaryFocus.STRENGTH,
                                Set.of(),
                                null,
                                false));
    }

    static void goalsAndExperience() {
        var goalTargets = new HashSet<GymWorkoutDefinition.Target>();
        for (var goal : GymProgramme.Goal.values()) {
            var s =
                    settings(
                            goal,
                            GymProgramme.Experience.EXPERIENCED,
                            GymProgramme.Priority.STRENGTH_FIRST,
                            3,
                            75,
                            ALL);
            var p = generate(s, 10);
            var w = p.blocks().get(1).workouts().get(0);
            var first = w.definition().slots().get(0);
            goalTargets.add(first.target());
            if (goal == GymProgramme.Goal.STRENGTH)
                check(
                        first.target().maximumReps() <= 6 && first.target().restSeconds() >= 180,
                        "strength primary");
            if (goal == GymProgramme.Goal.POWER)
                check(
                        ExerciseMetadataCatalog.require(first.exercise().id()).primaryRole()
                                        == ExerciseMetadata.Role.POWER
                                && first.target().maximumReps() <= 5,
                        "genuine power exposure");
            if (goal == GymProgramme.Goal.SUPPORT_RUNNING) {
                var roles =
                        w.definition().slots().stream()
                                .map(
                                        x ->
                                                ExerciseMetadataCatalog.require(x.exercise().id())
                                                        .primaryRole())
                                .collect(Collectors.toSet());
                check(
                        roles.containsAll(
                                Set.of(
                                        ExerciseMetadata.Role.UNILATERAL,
                                        ExerciseMetadata.Role.CALF,
                                        ExerciseMetadata.Role.CORE)),
                        "running support includes real supportive exposure");
            }
        }
        check(goalTargets.size() >= 4, "meaningfully different goal targets");
        var beginner =
                generate(
                        settings(
                                GymProgramme.Goal.GENERAL,
                                GymProgramme.Experience.BEGINNER,
                                GymProgramme.Priority.BALANCED,
                                3,
                                75,
                                ALL),
                        6);
        var experienced =
                generate(
                        settings(
                                GymProgramme.Goal.GENERAL,
                                GymProgramme.Experience.EXPERIENCED,
                                GymProgramme.Priority.BALANCED,
                                3,
                                75,
                                ALL),
                        6);
        check(
                beginner.workouts().get(0).definition().slots().size()
                        < experienced.workouts().get(0).definition().slots().size(),
                "different complexity and exercise count");
        check(
                !beginner.workouts()
                        .get(0)
                        .definition()
                        .slots()
                        .get(0)
                        .exercise()
                        .equals(
                                experienced
                                        .workouts()
                                        .get(0)
                                        .definition()
                                        .slots()
                                        .get(0)
                                        .exercise()),
                "different exercise selection");
    }

    static void futureOnlyEditsAndSubstitutions() {
        var s =
                settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Experience.INTERMEDIATE,
                        GymProgramme.Priority.BALANCED,
                        2,
                        75,
                        ALL);
        var p = generate(s, 6);
        var w = p.workouts().get(0);
        var from =
                w.definition().slots().stream()
                        .filter(x -> x.exercise().id().equals("bench-press"))
                        .findFirst()
                        .orElseThrow();
        var context =
                new ExerciseSubstitutionEngine.Context(
                        from.exercise(),
                        s.goal(),
                        s.experience(),
                        s.equipment(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY);
        var target =
                new GymWorkoutDefinition.Target(
                        2, 8, 12, null, null, from.target().effort(), 90, null);
        var proposal =
                StrengthProgrammeEditor.substitute(
                        p,
                        w.id(),
                        from.id(),
                        context,
                        "push-up",
                        target,
                        p.start(),
                        NOW.plusSeconds(1),
                        Set.of());
        check(
                proposal.proposed().revision() == 2 && proposal.proposed().parentRevision() == 1,
                "explicit revision lineage");
        check(proposal.substitutions().size() == 1, "preserved substitution decision");
        check(p.workouts().get(0).equals(w), "original prescription unchanged");
        var changed =
                proposal.proposed().workouts().get(0).definition().slots().stream()
                        .filter(x -> x.id().equals(from.id()))
                        .findFirst()
                        .orElseThrow();
        check(
                changed.exercise().id().equals("push-up")
                        && changed.target().referenceLoadKg() == null,
                "no load transfer");
        rejects(
                () ->
                        StrengthProgrammeEditor.substitute(
                                p,
                                w.id(),
                                from.id(),
                                context,
                                "push-up",
                                target,
                                p.start(),
                                NOW.plusSeconds(1),
                                Set.of(w.id())));
        rejects(
                () ->
                        StrengthProgrammeEditor.substitute(
                                p,
                                w.id(),
                                from.id(),
                                context,
                                "push-up",
                                target,
                                w.date().plusDays(1),
                                NOW.plusSeconds(1),
                                Set.of()));
        rejects(
                () ->
                        StrengthProgrammeEditor.substitute(
                                p,
                                w.id(),
                                from.id(),
                                context,
                                "push-up",
                                null,
                                p.start(),
                                NOW.plusSeconds(1),
                                Set.of()));
        var excluded =
                new ExerciseSubstitutionEngine.Context(
                        from.exercise(),
                        s.goal(),
                        s.experience(),
                        s.equipment(),
                        Set.of("push-up"),
                        Set.of(),
                        Set.of(),
                        ExerciseSubstitutionEngine.Reason.EQUIPMENT_BUSY);
        rejects(
                () ->
                        StrengthProgrammeEditor.substitute(
                                p,
                                w.id(),
                                from.id(),
                                excluded,
                                "push-up",
                                target,
                                p.start(),
                                NOW.plusSeconds(1),
                                Set.of()));
        rejects(() -> new StrengthProgrammeEditor.Proposal(p, p, Set.of(), List.of()));
    }

    static void futureRegeneration() {
        var original =
                generate(
                        settings(
                                GymProgramme.Goal.STRENGTH,
                                GymProgramme.Experience.INTERMEDIATE,
                                GymProgramme.Priority.BALANCED,
                                2,
                                60,
                                ALL),
                        6);
        var settings =
                new Settings(
                        GymProgramme.Goal.MUSCLE,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        3,
                        60,
                        ALL);
        var candidate = generate(settings, 10);
        var cutoff = START.plusWeeks(2);
        String completedId = original.workouts().get(8).id();
        var proposal =
                StrengthProgrammeEditor.replaceFuture(
                        original,
                        candidate,
                        cutoff,
                        NOW.plusSeconds(30),
                        Set.of(completedId),
                        "Athlete changed days and muscle goal");
        check(proposal.proposed().revision() == 2, "future regeneration is an explicit revision");
        var retained = new HashMap<String, Workout>();
        for (var w : proposal.proposed().workouts()) retained.put(w.id(), w);
        for (var w : original.workouts()) {
            if (w.date().isBefore(cutoff) || w.id().equals(completedId))
                check(
                        w.equals(retained.get(w.id())),
                        "past/actual-linked prescription byte-equivalent");
        }
        for (var w : proposal.proposed().workouts()) {
            if (!w.date().isBefore(cutoff) && !w.id().equals(completedId))
                check(
                        settings.days().contains(w.date().getDayOfWeek()),
                        "changed future availability honoured");
        }
        check(
                original.revision() == 1
                        && original.settings().goal() == GymProgramme.Goal.STRENGTH,
                "proposal never mutates original programme");
        var shorter = generate(settings, 3);
        rejects(
                () ->
                        StrengthProgrammeEditor.replaceFuture(
                                original,
                                shorter,
                                cutoff,
                                NOW.plusSeconds(30),
                                Set.of(completedId),
                                "Would lose completed history"));
        rejects(
                () ->
                        StrengthProgrammeEditor.replaceFuture(
                                original,
                                candidate,
                                cutoff,
                                NOW.minusSeconds(1),
                                Set.of(),
                                "Time travel"));
        rejects(
                () ->
                        StrengthProgrammeEditor.replaceFuture(
                                original,
                                candidate,
                                cutoff,
                                NOW.plusSeconds(1),
                                Set.of("unknown-session"),
                                "Unknown evidence"));
        var duration = new GymWorkoutDefinition.Target(3, null, null, 30L, null, null, 60, null);
        check(
                StrengthWorkoutBudget.exerciseMinutes(duration) == 5,
                "duration/rest/transition budget");
        rejects(() -> StrengthWorkoutBudget.exerciseMinutes(null));
    }

    static void schedulingAndRaceWeek() {
        var s =
                settings(
                        GymProgramme.Goal.SUPPORT_RUNNING,
                        GymProgramme.Experience.INTERMEDIATE,
                        GymProgramme.Priority.RUNNING_FIRST,
                        2,
                        60,
                        ALL);
        var run = START.plusDays(2);
        var hybrid =
                ENGINE.generate(
                        request(
                                s,
                                6,
                                null,
                                Set.of(),
                                PrimaryFocus.HYBRID,
                                Set.of(run),
                                null,
                                false));
        check(
                hybrid.workouts().stream()
                        .filter(w -> w.date().isBefore(START.plusWeeks(1)))
                        .noneMatch(w -> w.date().equals(run) || w.date().plusDays(1).equals(run)),
                "hybrid prefers spacing from run");
        var race = START.plusWeeks(4);
        var plan =
                ENGINE.generate(
                        request(s, 6, null, Set.of(), PrimaryFocus.HYBRID, Set.of(), race, false));
        for (var w : plan.workouts())
            if (!w.date().isBefore(race.minusDays(7)) && !w.date().isAfter(race))
                check(
                        w.definition().slots().stream().allMatch(x -> x.target().sets() == 1),
                        "race week not highest volume");
    }

    static void customDraftAuthoring() {
        var s =
                settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        GymProgramme.Priority.BALANCED,
                        2,
                        45,
                        ALL);
        var draft =
                new StrengthProgrammeDraft(
                        "draft",
                        1,
                        "new-plan",
                        0,
                        "My custom plan",
                        s,
                        START,
                        List.of(),
                        Origin.CUSTOM,
                        null);
        final var blank = draft;
        rejects(() -> blank.preview(NOW, Set.of(), Map.of()));
        var bench = ExerciseCatalog.definition("bench-press").exercise();
        var row = ExerciseCatalog.definition("dumbbell-row").exercise();
        var target =
                new GymWorkoutDefinition.Target(
                        3,
                        6,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RIR, 2, 4),
                        120,
                        null);
        var a = new GymWorkoutDefinition.Slot("bench", bench, target, null);
        var b = new GymWorkoutDefinition.Slot("row", row, target, null);
        draft =
                draft.replaceBlock(
                        new StrengthProgrammeDraft.WeekBlock(
                                "foundation",
                                Phase.FOUNDATION,
                                "Learn the routine",
                                3,
                                List.of(
                                        new StrengthProgrammeDraft.Day(
                                                "a",
                                                DayOfWeek.TUESDAY,
                                                "Session A",
                                                List.of(a, b),
                                                "Keep effort in reserve."))));
        final var incomplete = draft;
        rejects(() -> incomplete.preview(NOW, Set.of(), Map.of()));
        draft = draft.duplicateDay("foundation", "a", "b", DayOfWeek.FRIDAY);
        var preview = draft.preview(NOW, Set.of(), Map.of());
        check(preview.workouts().size() == 6, "manual programme expands exact weeks");
        check(
                preview.workouts().get(0).definition().slots().get(0).target().effort().scale()
                        == GymWorkoutDefinition.EffortScale.RIR,
                "authored RIR preserved");
        check(
                preview.workouts().get(0).notes().equals("Keep effort in reserve."),
                "manual notes preserved");
        var original = draft;
        draft = draft.moveExercise("foundation", "a", "foundation", "a", "bench", 1);
        check(
                draft.blocks().get(0).days().get(0).slots().get(0).id().equals("row"),
                "reorder is domain operation");
        check(
                original.blocks().get(0).days().get(0).slots().get(0).id().equals("bench"),
                "original draft immutable");
        var emptyDay =
                new StrengthProgrammeDraft.Day(
                        "c", DayOfWeek.WEDNESDAY, "Third session", List.of(), null);
        draft =
                draft.replaceDay("foundation", emptyDay)
                        .moveExercise("foundation", "a", "foundation", "c", "bench", 0);
        check(
                draft.blocks().get(0).days().get(2).slots().get(0).id().equals("bench"),
                "move between workouts");
        draft = draft.deleteDay("foundation", "c");
        draft = draft.duplicateBlock("foundation", "development");
        check(
                draft.blocks().size() == 2
                        && draft.preview(NOW, Set.of(), Map.of()).workouts().size() == 12,
                "duplicate complete block");
        draft = draft.reorderBlock("development", 0);
        check(draft.blocks().get(0).id().equals("development"), "reorder blocks");
        final var valid = draft;
        rejects(() -> valid.preview(NOW, Set.of("bench-press"), Map.of()));
        rejects(() -> valid.moveExercise("foundation", "a", "foundation", "a", "missing", 0));
        rejects(() -> valid.reorderBlock("foundation", 99));
        // A user-created task gets real logging targets, never invented canonical guidance.
        var definition =
                new StrengthExercise(
                        "custom-band-press",
                        "My band press",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.REPS_ONLY);
        var custom =
                new CustomStrengthExercise(
                        definition,
                        ExerciseMetadata.BodyArea.CHEST,
                        ExerciseMetadata.Role.HORIZONTAL_PUSH,
                        new ExerciseMetadata.EquipmentRequirement(List.of(Set.of())),
                        "My own setup.");
        check(ExerciseCatalog.definition(custom.exercise().id()) == null, "custom never canonical");
        var customTarget = new GymWorkoutDefinition.Target(2, 8, 12, null, null, null, null, null);
        var customDay =
                new StrengthProgrammeDraft.Day(
                        "a",
                        DayOfWeek.TUESDAY,
                        "My movement",
                        List.of(
                                new GymWorkoutDefinition.Slot(
                                        "my-slot", definition, customTarget, null)),
                        null);
        var withCustom = original.replaceDay("foundation", customDay);
        rejects(() -> withCustom.preview(NOW, Set.of(), Map.of()));
        check(
                withCustom
                        .preview(NOW, Set.of(), Map.of(definition.id(), custom))
                        .workouts()
                        .get(0)
                        .definition()
                        .slots()
                        .get(0)
                        .exercise()
                        .equals(definition),
                "custom exercise frozen for logging");
        rejects(
                () ->
                        new CustomStrengthExercise(
                                bench,
                                ExerciseMetadata.BodyArea.CHEST,
                                ExerciseMetadata.Role.HORIZONTAL_PUSH,
                                custom.equipment(),
                                null));
    }

    static void simulator() {
        int plans = 0, honestFailures = 0;
        List<Set<ExerciseMetadata.Equipment>> setups =
                List.of(ALL, Set.of(ExerciseMetadata.Equipment.DUMBBELLS), Set.of());
        for (var goal : GymProgramme.Goal.values())
            for (var experience : GymProgramme.Experience.values())
                for (var priority : GymProgramme.Priority.values())
                    for (int frequency = 1; frequency <= 6; frequency++)
                        for (int minutes : List.of(30, 60))
                            for (var equipment : setups) {
                                var s =
                                        settings(
                                                goal,
                                                experience,
                                                priority,
                                                frequency,
                                                minutes,
                                                equipment);
                                Set<String> excluded =
                                        Set.of("bench-press", "romanian-deadlift", "power-clean");
                                var r =
                                        request(
                                                s,
                                                2,
                                                null,
                                                excluded,
                                                PrimaryFocus.HYBRID,
                                                Set.of(START.plusDays(1)),
                                                null,
                                                false);
                                StrengthProgramme p;
                                try {
                                    p = ENGINE.generate(r);
                                } catch (IllegalArgumentException e) {
                                    check(!equipment.equals(ALL), "full setup must generate");
                                    check(
                                            e.getMessage().contains("Not enough eligible"),
                                            "honest constrained failure");
                                    honestFailures++;
                                    continue;
                                }
                                plans++;
                                check(
                                        p.equals(ENGINE.generate(r)),
                                        "deterministic entire programme");
                                check(p.workouts().size() == frequency * 2, "frequency preserved");
                                if (equipment.equals(ALL)
                                        && minutes == 60
                                        && goal == GymProgramme.Goal.SUPPORT_RUNNING) {
                                    var roles =
                                            p.workouts().stream()
                                                    .flatMap(w -> w.definition().slots().stream())
                                                    .map(
                                                            slot ->
                                                                    ExerciseMetadataCatalog.require(
                                                                                    slot.exercise()
                                                                                            .id())
                                                                            .primaryRole())
                                                    .collect(Collectors.toSet());
                                    check(
                                            roles.containsAll(
                                                    Set.of(
                                                            ExerciseMetadata.Role.SQUAT,
                                                            ExerciseMetadata.Role.HINGE,
                                                            ExerciseMetadata.Role.UNILATERAL,
                                                            ExerciseMetadata.Role.CALF,
                                                            ExerciseMetadata.Role.CORE)),
                                            "running support covers real lower-body, lower-leg and"
                                                    + " trunk roles");
                                    check(
                                            roles.contains(ExerciseMetadata.Role.HORIZONTAL_PULL),
                                            "running support preserves pulling maintenance");
                                }
                                for (var w : p.workouts()) {
                                    if (goal == GymProgramme.Goal.POWER) {
                                        check(
                                                w.definition().slots().size() <= 5,
                                                "power quality over excessive exercise count");
                                        check(
                                                w.definition().slots().stream()
                                                                .mapToInt(
                                                                        slot ->
                                                                                slot.target()
                                                                                        .sets())
                                                                .sum()
                                                        <= 11,
                                                "power support volume remains controlled");
                                    }
                                    var majorRoles =
                                            EnumSet.of(
                                                    ExerciseMetadata.Role.SQUAT,
                                                    ExerciseMetadata.Role.HINGE,
                                                    ExerciseMetadata.Role.HORIZONTAL_PUSH,
                                                    ExerciseMetadata.Role.VERTICAL_PUSH,
                                                    ExerciseMetadata.Role.HORIZONTAL_PULL,
                                                    ExerciseMetadata.Role.VERTICAL_PULL);
                                    check(
                                            w.definition().slots().stream()
                                                            .filter(
                                                                    slot ->
                                                                            majorRoles.contains(
                                                                                    ExerciseMetadataCatalog
                                                                                            .require(
                                                                                                    slot.exercise()
                                                                                                            .id())
                                                                                            .primaryRole()))
                                                            .count()
                                                    <= 4,
                                            "not five major compounds in one session");
                                    check(
                                            w.expectedMinutes() <= minutes,
                                            "bounded realistic duration");
                                    var seen = new HashSet<String>();
                                    for (var slot : w.definition().slots()) {
                                        check(
                                                seen.add(slot.exercise().id()),
                                                "no duplicate exercise");
                                        check(
                                                !excluded.contains(slot.exercise().id()),
                                                "exclusion beats favourite");
                                        var m =
                                                ExerciseMetadataCatalog.require(
                                                        slot.exercise().id());
                                        check(
                                                m.equipment().availableWith(equipment),
                                                "complete equipment setup available");
                                        check(
                                                m.suitability().appropriateFor(experience),
                                                "experience appropriate");
                                        check(
                                                slot.target().referenceLoadKg() == null,
                                                "no fabricated loading");
                                        if (experience == GymProgramme.Experience.BEGINNER)
                                            check(
                                                    m.suitability()
                                                            == ExerciseMetadata.Suitability
                                                                    .BEGINNER_FRIENDLY,
                                                    "beginner technical ceiling");
                                    }
                                }
                            }
        var hardDates = new HashSet<LocalDate>();
        for (int day = 0; day < 26 * 7; day++) hardDates.add(START.plusDays(day));
        var full =
                settings(
                        GymProgramme.Goal.SUPPORT_RUNNING,
                        GymProgramme.Experience.INTERMEDIATE,
                        GymProgramme.Priority.RUNNING_FIRST,
                        6,
                        60,
                        ALL);
        var longPlan =
                ENGINE.generate(
                        request(
                                full,
                                26,
                                null,
                                Set.of(),
                                PrimaryFocus.HYBRID,
                                hardDates,
                                null,
                                false));
        check(
                longPlan.workouts().size() == 156 && longPlan.tradeoffs().size() == 100,
                "long conflicted plan has bounded summary");
        check(
                longPlan.workouts().stream().allMatch(w -> w.notes().contains("demanding run")),
                "dated conflict evidence not truncated");
        check(plans + honestFailures == 1620, "simulator covers full configuration space");
        check(
                plans > 800 && honestFailures > 0,
                "useful generation plus explicit impossible contexts");
    }

    static void check(boolean ok, String why) {
        checks++;
        if (!ok) throw new AssertionError(why);
    }

    static void rejects(Runnable action) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            return;
        }
        throw new AssertionError("Invalid operation accepted");
    }
}
