package org.racedaydreams.domain.simulation;

import org.racedaydreams.domain.*;
import org.racedaydreams.domain.achievements.TrainingMilestones;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.polish.StartingLoad;
import org.racedaydreams.domain.recording.RunMetricsEngine;
import org.racedaydreams.domain.recovery.*;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.storage.DetailRetention;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

/** Synthetic recorded evidence, not a physiological model or a claim of training outcomes. */
public final class SixMonthAthleteTestSuite {
    private static int checks;
    private static final Map<String, Long> EVENT_COUNTS = new HashMap<>();
    private static final LocalDate END = LocalDate.of(2026, 11, 5), START = END.minusMonths(6);
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private record Athlete(
            String id,
            double kg,
            double goalKm,
            PrimaryFocus focus,
            GymProgramme.Experience experience,
            Set<DayOfWeek> gymDays,
            Set<DayOfWeek> runDays,
            double load) {}

    public static void main(String[] args) {
        simulate(
                new Athlete(
                        "beginner-first-5k",
                        100,
                        5,
                        PrimaryFocus.RUNNING,
                        GymProgramme.Experience.BEGINNER,
                        Set.of(DayOfWeek.FRIDAY),
                        Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY),
                        8));
        simulate(
                new Athlete(
                        "hybrid-half",
                        87,
                        21.0975,
                        PrimaryFocus.HYBRID,
                        GymProgramme.Experience.INTERMEDIATE,
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
                        Set.of(
                                DayOfWeek.MONDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.SUNDAY),
                        20));
        simulate(
                new Athlete(
                        "experienced-marathon",
                        70,
                        42.195,
                        PrimaryFocus.HYBRID,
                        GymProgramme.Experience.EXPERIENCED,
                        Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        Set.of(
                                DayOfWeek.MONDAY,
                                DayOfWeek.WEDNESDAY,
                                DayOfWeek.THURSDAY,
                                DayOfWeek.SUNDAY),
                        24));
        System.out.println(
                "SixMonthAthleteTestSuite: "
                        + checks
                        + " checks passed; three calendar-six-month histories, no physiological"
                        + " outcome claim.");
    }

    private static void simulate(Athlete a) {
        var gym = new ArrayList<StrengthSession>();
        var runs = new ArrayList<ActivityRecord>();
        var weights = new ArrayList<BodyWeight>();
        ProteinGuidance.Basis basis = null;
        var gapStart = START.plusDays(49).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var prescription =
                new GymProgramme.Prescription(
                        "goblet-squat", 2, 8, 10, null, null, 5, 8, 120, "Optional reference only");
        for (LocalDate day = START; !day.isAfter(END); day = day.plusDays(1)) {
            var now = day.atTime(18, 0).toInstant(UTC);
            if (day.getDayOfMonth() == START.getDayOfMonth()) {
                var w =
                        new BodyWeight(
                                a.id() + "-weight-" + day,
                                a.kg(),
                                day.atTime(6, 0).toInstant(UTC),
                                UTC,
                                "manual",
                                a.id() + day,
                                null);
                weights.add(w);
                basis = ProteinGuidance.Basis.from(w); // Explicit synthetic athlete choice.
            }
            boolean missingWeek = !day.isBefore(gapStart) && day.isBefore(gapStart.plusDays(7));
            if (!missingWeek && a.gymDays().contains(day.getDayOfWeek())) gym.add(gym(a, day));
            if (!missingWeek && a.runDays().contains(day.getDayOfWeek())) {
                long week = ChronoUnit.WEEKS.between(START, day);
                double km =
                        a.goalKm() == 5
                                ? Math.min(5, .8 + week * .18)
                                : day.getDayOfWeek() == DayOfWeek.SUNDAY
                                        ? Math.min(a.goalKm() == 42.195 ? 30 : 18, 6 + week * .7)
                                        : a.goalKm() == 42.195 ? 8 : 5;
                runs.add(
                        run(
                                a.id() + "-run-" + day,
                                day,
                                km * 1000,
                                a.experience() == GymProgramme.Experience.BEGINNER ? 600 : 360,
                                a.experience() != GymProgramme.Experience.BEGINNER
                                                && day.getDayOfWeek() == DayOfWeek.THURSDAY
                                        ? 8.0
                                        : null));
            }
            var frozenGym = List.copyOf(gym);
            var frozenRuns = List.copyOf(runs);
            var review = TrainingRecovery.review(gym, runs, List.of(), now, UTC);
            check(review.date().equals(day), "Advice belongs to the actual review day");
            check(
                    !review.heading().isBlank() && !review.instruction().isBlank(),
                    "Action and explanation remain readable");
            if (a.experience() == GymProgramme.Experience.BEGINNER) {
                check(!review.lighterWeek(), "Missing effort/health is not a deload requirement");
                check(
                        StartingLoad.suggest(prescription, gym, now) == null,
                        "Unknown effort cannot fabricate a starting load");
                check(
                        StrengthProgressionAdvisor.review(prescription, gym, now).action()
                                != StrengthProgressionAdvisor.Action.CONSIDER_INCREASE,
                        "Missing effort cannot justify a load increase");
            }
            check(
                    !ProteinGuidance.evaluate(false, basis, weights, a.focus(), now).available(),
                    "Nutrition remains optional");
            var advice = ProteinGuidance.evaluate(true, basis, weights, a.focus(), now);
            check(
                    advice.available() && advice.basis().kilograms() == a.kg(),
                    "Explicit recent manual weight works without height/HC");
            check(
                    advice.minimumGrams() > 0 && advice.maximumGrams() >= advice.minimumGrams(),
                    "General range is bounded, not a calorie prescription");
            check(
                    gym.equals(frozenGym) && runs.equals(frozenRuns),
                    "Review/progression never mutate actuals");
        }
        var now = END.atTime(18, 0).toInstant(UTC);
        check(
                ChronoUnit.DAYS.between(START, END) >= 181,
                "Histories cover six calendar months, not a few fixtures");
        check(
                gym.size() >= 24 && runs.size() >= 70,
                "Realistic repeated recorded sessions across the whole period");
        check(
                gym.get(0).startedAt().isBefore(START.plusWeeks(1).atStartOfDay().toInstant(UTC)),
                "Evidence begins in the first week");
        check(
                gym.get(gym.size() - 1)
                        .startedAt()
                        .isAfter(END.minusWeeks(1).atStartOfDay().toInstant(UTC)),
                "Evidence continues in the final week");
        int weeks =
                (int)
                                ChronoUnit.WEEKS.between(
                                        START.with(
                                                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                                        END.with(
                                                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                        + 1;
        var volume = StrengthVolume.weekly(gym, START, weeks, now);
        check(
                volume.stream().mapToInt(StrengthVolume.Week::workingSets).sum() == gym.size() * 4,
                "Working sets count actuals and exclude all warm-ups");
        check(
                volume.stream()
                        .anyMatch(
                                w ->
                                        w.monday().equals(gapStart)
                                                && w.workouts() == 0
                                                && w.workingSets() == 0),
                "An intentional evidence gap remains no recorded training");
        var records = StrengthRecords.progression(gym, now);
        check(
                !records.isEmpty(),
                "Unknown effort does not erase genuinely recorded PB measurements");
        check(
                TrainingMilestones.eligible(gym, runs, now)
                        .equals(TrainingMilestones.eligible(gym, runs, now)),
                "Recognition is deterministic from actual retained sources");
        var old = weights.get(0);
        check(
                !ProteinGuidance.evaluate(
                                true, ProteinGuidance.Basis.from(old), weights, a.focus(), now)
                        .available(),
                "Newer observations cannot silently replace a stale chosen weight");
        check(
                !ProteinGuidance.evaluate(true, null, weights, a.focus(), now).available(),
                "Missing selection produces no numeric protein advice");
        var row = gym.get(gym.size() - 1);
        var reference = StartingLoad.suggest(prescription, gym, now);
        if (reference != null)
            check(
                    reference.highKg() <= a.load(),
                    "Optional exact-task reference never exceeds the recorded load");
        check(
                StartingLoad.suggest(
                                new GymProgramme.Prescription(
                                        "bench-press",
                                        2,
                                        8,
                                        10,
                                        null,
                                        null,
                                        5,
                                        8,
                                        120,
                                        "Different exercise"),
                                gym,
                                now)
                        == null,
                "No load transfer from squats to bench");
        var settings =
                new StrengthProgramme.Settings(
                        a.experience() == GymProgramme.Experience.INTERMEDIATE
                                ? GymProgramme.Goal.GENERAL
                                : GymProgramme.Goal.SUPPORT_RUNNING,
                        a.experience() == GymProgramme.Experience.INTERMEDIATE
                                ? GymProgramme.Priority.BALANCED
                                : GymProgramme.Priority.RUNNING_FIRST,
                        a.experience(),
                        a.gymDays(),
                        a.gymDays().size(),
                        45,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class));
        var programme =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        a.id() + "-future",
                                        now,
                                        "Next block",
                                        settings,
                                        END,
                                        6,
                                        null,
                                        a.focus(),
                                        Set.of(),
                                        END.plusWeeks(10),
                                        Set.of(),
                                        Set.of(),
                                        false));
        var before = List.copyOf(programme.workouts());
        var lower =
                programme.workouts().stream()
                        .filter(TrainingRecovery::meaningfulLower)
                        .findFirst()
                        .orElseThrow();
        var quality =
                new PlannedWorkout(
                        a.id() + "-long",
                        lower.date().plusDays(1),
                        WorkoutCategory.LONG_RUN,
                        a.goalKm() == 5 ? 5.0 : a.goalKm() == 42.195 ? 28.0 : 16.0,
                        null,
                        List.of(),
                        null);
        var conflicts = TrainingRecovery.conflicts(programme, List.of(quality), END, Set.of());
        check(
                !conflicts.isEmpty(),
                "Hybrid construction surfaces a deliberate gym/long-run conflict");
        var alternatives =
                RecoveryProgrammeProposal.alternatives(
                        programme, lower.id(), List.of(quality), END, Set.of());
        for (var date : alternatives)
            check(
                    a.gymDays().contains(date.getDayOfWeek()) && !date.isBefore(END),
                    "Alternatives respect athlete availability");
        var review = TrainingRecovery.review(gym, runs, List.of(), now, UTC);
        if (a.experience() == GymProgramme.Experience.INTERMEDIATE) {
            check(
                    review.lighterWeek(),
                    "Repeated recent high recorded effort supports an optional lighter-week"
                            + " review");
            var proposal =
                    RecoveryProgrammeProposal.lighterWeek(programme, review, now, UTC, Set.of());
            check(!proposal.proposed().equals(programme), "Lighter week is a distinct proposal");
        }
        check(
                programme.workouts().equals(before),
                "Keeping the schedule never silently activates a proposal");
        var inventory = new ArrayList<DetailRetention.Recording>();
        for (var r : runs) inventory.add(detail(r));
        check(
                DetailRetention.preview(
                                DetailRetention.Policy.LAST_24_MONTHS, inventory, now, "dataset")
                        .recordings()
                        .isEmpty(),
                "The six-month history cannot expire under a24month choice");
        var legacy =
                run(
                        a.id() + "-legacy",
                        START.minusYears(3),
                        a.goalKm() == 5 ? 1500 : 5000,
                        400,
                        null);
        inventory.add(detail(legacy));
        var preview =
                DetailRetention.preview(
                        DetailRetention.Policy.LAST_24_MONTHS, inventory, now, "dataset");
        check(
                preview.recordings().size() == 1
                        && preview.recordings().get(0).activityId().equals(legacy.id()),
                "Only the explicit older raw backlog is eligible");
        boolean protectedExisting = false;
        try {
            DetailRetention.approve(
                    preview, DetailRetention.Preferences.existing(), now, "dataset", false, false);
        } catch (IllegalArgumentException expected) {
            protectedExisting = true;
        }
        check(protectedExisting, "Existing raw data cannot expire without informed consent");
        var approved =
                DetailRetention.approve(
                        preview,
                        DetailRetention.Preferences.acknowledged(
                                DetailRetention.Policy.LAST_24_MONTHS, now, "choice"),
                        now,
                        "dataset",
                        false,
                        false);
        DetailRetention.checkBatch(
                approved,
                DetailRetention.Preferences.acknowledged(
                        DetailRetention.Policy.LAST_24_MONTHS, now, "choice"),
                now,
                "dataset",
                preview.recordings(),
                false,
                false);
        check(
                !BestEffortCalculator.qualifiedResults(legacy).isEmpty(),
                "Legacy raw points contain real qualified synthetic PB evidence");
        check(
                TrainingRecovery.review(gym, runs, List.of(), now, UTC).equals(review),
                "Retention advice itself does not rewrite training history/recovery");
        System.out.println(
                a.id()
                        + ": "
                        + runs.size()
                        + " recorded runs, "
                        + gym.size()
                        + " gym workouts, "
                        + volume.size()
                        + " weekly windows; user decisions unchanged.");
    }

    private static StrengthSession gym(Athlete a, LocalDate day) {
        var id = a.id() + "-gym-" + day;
        Double effort =
                a.experience() == GymProgramme.Experience.BEGINNER
                        ? null
                        : a.experience() == GymProgramme.Experience.INTERMEDIATE
                                        && day.isAfter(END.minusDays(15))
                                ? 9.0
                                : 6.0;
        var squat = ExerciseCatalog.definition("goblet-squat").exercise();
        var pull = ExerciseCatalog.definition("dumbbell-row").exercise();
        return new StrengthSession(
                id,
                day.atTime(7, 0).toInstant(UTC),
                UTC,
                Duration.ofMinutes(35),
                null,
                "Synthetic recorded evidence, not a prescription",
                List.of(
                        new StrengthSession.ExerciseSets(
                                squat,
                                List.of(
                                        new StrengthSession.SetEntry(
                                                id + "-warm", 5, 4, null, true),
                                        new StrengthSession.SetEntry(
                                                id + "-a", 10, a.load(), effort, false),
                                        new StrengthSession.SetEntry(
                                                id + "-b", 10, a.load(), effort, false))),
                        new StrengthSession.ExerciseSets(
                                pull,
                                List.of(
                                        new StrengthSession.SetEntry(
                                                id + "-c", 10, a.load(), effort, false),
                                        new StrengthSession.SetEntry(
                                                id + "-d", 10, a.load(), effort, false)))));
    }

    private static ActivityRecord run(
            String id, LocalDate day, double metres, double secondsPerKm, Double effort) {
        var start = day.atTime(9, 0).toInstant(UTC);
        long seconds = (long) Math.ceil(metres / 1000 * secondsPerKm / 5) * 5;
        var engine = new RunMetricsEngine(0, start);
        for (long t = 0; t <= seconds; t += 5)
            engine.accept(
                    new RunMetricsEngine.Fix(
                            start.plusSeconds(t),
                            t * 1000,
                            0,
                            metres * t / seconds / 111194.9266,
                            3.0,
                            null,
                            null,
                            1000 / secondsPerKm));
        var samples = new ArrayList<DistanceSample>();
        for (var p : engine.points())
            samples.add(
                    new DistanceSample(
                            Duration.ofMillis(p.fix().monotonicMillis()),
                            p.cumulativeMetres(),
                            null,
                            null,
                            null,
                            p.discontinuity()));
        double actual = engine.points().get(engine.points().size() - 1).cumulativeMetres();
        check(
                Math.abs(actual - metres) < metres * .005,
                "Clean synthetic trace stays within deterministic distance tolerance, not a device"
                        + " accuracy claim");
        EVENT_COUNTS.put(id, (long) engine.events().size());
        return new ActivityRecord(
                "rdd:" + id,
                new SourceIdentity("rdd", id, "org.racedaydreams"),
                start,
                start.plusSeconds(seconds),
                UTC,
                UTC,
                actual,
                Duration.ofSeconds(seconds),
                null,
                null,
                null,
                null,
                effort,
                "Synthetic accepted route",
                samples,
                List.of(),
                null,
                null);
    }

    private static DetailRetention.Recording detail(ActivityRecord r) {
        return new DetailRetention.Recording(
                r.source().recordId(),
                r.id(),
                r.source().provider(),
                r.source().recordId(),
                r.end(),
                true,
                r.samples().size(),
                EVENT_COUNTS.get(r.source().recordId()),
                0L);
    }

    private static void check(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
}
