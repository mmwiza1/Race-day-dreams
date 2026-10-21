package org.racedaydreams.domain.plan;

import org.racedaydreams.domain.*;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Executes real whole-plan regression tooling against explicit test-only synthetic schedules. */
public final class PlanRegressionTestSuite {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--write-snapshots")) {
            Path directory = Paths.get(args[1]);
            Files.createDirectories(directory);
            for (SyntheticPlanFixtures.Fixture fixture : SyntheticPlanFixtures.all()) {
                Files.write(directory.resolve(fixture.id() + ".txt"), PlanSnapshot.render(fixture.plan()).getBytes(StandardCharsets.UTF_8));
            }
            System.out.println("Wrote five explicit synthetic whole-plan regression snapshots.");
            return;
        }
        List<SyntheticPlanFixtures.Fixture> fixtures = SyntheticPlanFixtures.all();
        check(fixtures.size() == 5, "Five representative contexts");
        for (SyntheticPlanFixtures.Fixture fixture : fixtures) {
            PlanRegressionReport report = inspect(fixture, fixture.plan());
            check(report.passed(), fixture.id() + " fixture invariants: " + report.issues());
            check(fixture.plan().workouts().size() >= 20, "Full multi-week fixture, not a single-workout assertion");
            String expected = resource("plan-snapshots/" + fixture.id() + ".txt");
            String actual = PlanSnapshot.render(fixture.plan());
            check(expected.equals(actual), "Whole-plan snapshot changed for " + fixture.id() + ":\n" + PlanSnapshot.difference(expected, actual));
            check(actual.contains(SyntheticPlanFixtures.NOTE), "Synthetic data explicitly labelled");
            List<PlannedWorkout> reversed = new ArrayList<>(fixture.plan().workouts());
            Collections.reverse(reversed);
            TrainingPlan reordered = new TrainingPlan(fixture.plan().id(), fixture.plan().startDate(), fixture.plan().endDate(), fixture.plan().phases(), reversed);
            check(actual.equals(PlanSnapshot.render(reordered)), "Input collection order does not change snapshot");
        }
        mutationChecks(fixtures.get(1));
        schedulingChecks(fixtures.get(2));
        boundaries(fixtures.get(1));
        deterministicContextIsolation(fixtures);
        System.out.println("PlanRegressionTestSuite: " + checks + " checks passed (synthetic fixtures; production generation remains pending).");
    }

    private static void mutationChecks(SyntheticPlanFixtures.Fixture fixture) {
        TrainingPlan original = fixture.plan();
        List<PlannedWorkout> values = new ArrayList<>(original.workouts());
        PlannedWorkout a = values.get(0);
        PlannedWorkout b = values.get(1);
        values.set(0, copy(a, a.id(), a.date(), WorkoutCategory.INTERVALS, a.distanceKm()));
        values.set(1, copy(b, b.id(), b.date(), WorkoutCategory.TEMPO_THRESHOLD_RUN, b.distanceKm()));
        check(inspect(fixture, plan(original, values)).has("HARD_DAY_SPACING"), "Adjacent hard days detected");

        values = new ArrayList<>(original.workouts());
        PlannedWorkout spike = values.get(8);
        values.set(8, copy(spike, spike.id(), spike.date(), WorkoutCategory.LONG_RUN, 80.0));
        PlanRegressionReport spikes = inspect(fixture, plan(original, values));
        check(spikes.has("WEEKLY_DISTANCE_SPIKE"), "Extreme weekly increase detected");
        check(spikes.has("LONG_RUN_SPIKE"), "Long-run spike detected");
        check(spikes.has("SINGLE_RUN_DOMINATES"), "Long-run share imbalance detected");

        values = new ArrayList<>(original.workouts());
        values.add(copy(a, a.id(), a.date(), a.category(), a.distanceKm()));
        PlanRegressionReport duplicates = inspect(fixture, plan(original, values));
        check(duplicates.has("DUPLICATE_WORKOUT_ID"), "Duplicate IDs detected");
        check(duplicates.has("MULTIPLE_RUNS_ON_DAY"), "Impossible duplicate day detected");
        check(duplicates.has("RUN_FREQUENCY"), "Weekly chosen frequency respected");

        values = new ArrayList<>(original.workouts());
        values.set(0, new PlannedWorkout(a.id(), a.date(), a.category(), null, Duration.ofMinutes(20), Collections.emptyList(), "Time-only fixture"));
        PlanRegressionReport unknown = inspect(fixture, plan(original, values));
        check(unknown.has("UNKNOWN_WEEKLY_DISTANCE"), "Unknown distance explicitly unverifiable");
        check(unknown.weeks().get(0).trainingDistanceKm() == null, "Unknown distance never becomes zero");
        values.set(0, new PlannedWorkout(a.id(), a.date(), a.category(), null, null, Collections.emptyList(), "Empty fixture"));
        check(inspect(fixture, plan(original, values)).has("EMPTY_PRESCRIPTION"), "Empty prescription detected");

        values = new ArrayList<>(original.workouts());
        LocalDate taperStart = original.endDate().minusWeeks(2);
        for (int index = 0; index < values.size(); index++) {
            PlannedWorkout current = values.get(index);
            if (!current.date().isBefore(taperStart) && current.category() != WorkoutCategory.RACE) values.set(index,
                    copy(current, current.id(), current.date(), current.category(), 10.0));
        }
        check(inspect(fixture, plan(original, values)).has("TAPER_VOLUME"), "Broken taper detected");
        List<TrainingPhase> missing = new ArrayList<>();
        for (TrainingPhase phase : original.phases()) if (phase.kind() != TrainingPhase.Kind.TAPER) missing.add(phase);
        TrainingPlan gap = new TrainingPlan(original.id(), original.startDate(), original.endDate(), missing, original.workouts());
        check(inspect(fixture, gap).has("PHASE_GAP"), "Phase gap detected");
        check(inspect(fixture, gap).has("TAPER_MISSING"), "Missing taper detected");
        List<TrainingPhase> overlapping = new ArrayList<>(original.phases());
        overlapping.add(original.phases().get(0));
        check(inspect(fixture, new TrainingPlan(original.id(), original.startDate(), original.endDate(), overlapping, original.workouts())).has("PHASE_OVERLAP"), "Phase overlap detected");
    }

    private static void schedulingChecks(SyntheticPlanFixtures.Fixture fixture) {
        TrainingPlan original = fixture.plan();
        PlannedWorkout first = original.workouts().get(0);
        List<PlannedWorkout> values = new ArrayList<>(original.workouts());
        // Explicit gym context is Tuesday/Friday. A Wednesday hard session follows Tuesday.
        PlannedWorkout wednesday = values.get(2);
        values.set(2, copy(wednesday, wednesday.id(), wednesday.date(), WorkoutCategory.INTERVALS, wednesday.distanceKm()));
        check(inspect(fixture, plan(original, values)).has("HARD_LOWER_BODY_PROXIMITY"), "Known hard lower-body training is considered");
        check(!inspect(fixture, original).has("UNAVAILABLE_DAY"), "Gym availability does not replace run availability");
        values = new ArrayList<>(original.workouts());
        values.set(0, copy(first, first.id(), first.date().plusDays(5), first.category(), first.distanceKm()));
        check(inspect(fixture, plan(original, values)).has("UNAVAILABLE_DAY"), "Unselected Saturday detected");
        values = new ArrayList<>(original.workouts());
        values.set(0, copy(first, first.id(), first.date(), WorkoutCategory.RACE, 42.195));
        check(inspect(fixture, plan(original, values)).has("UNSELECTED_RACE"), "Runner-owned race date retained");
    }

    private static void boundaries(SyntheticPlanFixtures.Fixture fixture) {
        LocalDate start = SyntheticPlanFixtures.ANCHOR;
        for (int count : new int[]{2, 6}) {
            UserProfile availability = SyntheticPlanFixtures.availability(fixture.profile(), count, start.plusWeeks(16));
            SyntheticPlanFixtures.Fixture variant = SyntheticPlanFixtures.build("availability-" + count, availability, start, 20);
            check(inspect(variant, variant.plan()).passed(), count + " runs/week availability: " + inspect(variant, variant.plan()).issues());
        }
        UserProfile tomorrow = SyntheticPlanFixtures.availability(fixture.profile(), 3, start.plusDays(1));
        SyntheticPlanFixtures.Fixture shortWindow = SyntheticPlanFixtures.build("tomorrow", tomorrow, start, 20);
        check(inspect(shortWindow, shortWindow.plan()).passed(), "Tomorrow horizon checked without assuming a long preparation period");
        check(shortWindow.plan().endDate().equals(start.plusDays(1)), "Tomorrow boundary exact");
        UserProfile year = SyntheticPlanFixtures.availability(fixture.profile(), 6, start.plusWeeks(52));
        SyntheticPlanFixtures.Fixture longWindow = SyntheticPlanFixtures.build("year", year, start, 20);
        check(inspect(longWindow, longWindow.plan()).passed(), "One-year complete fixture checked");
        check(longWindow.plan().workouts().size() > 300, "Whole one-year schedule inspected");
        Locale priorLocale = Locale.getDefault(); TimeZone priorZone = TimeZone.getDefault();
        String before = PlanSnapshot.render(longWindow.plan());
        try {
            Locale.setDefault(Locale.FRANCE); TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
            check(before.equals(PlanSnapshot.render(longWindow.plan())), "Snapshots independent of ambient locale and timezone");
        } finally { Locale.setDefault(priorLocale); TimeZone.setDefault(priorZone); }
        check(PlanSnapshot.difference(before, before).isEmpty(), "No diff for identical plan");
        check(PlanSnapshot.difference("one\n", "two\n").contains("-1 one"), "Readable line diff");
    }

    private static void deterministicContextIsolation(List<SyntheticPlanFixtures.Fixture> fixtures) {
        List<SyntheticPlanFixtures.Fixture> regenerated = SyntheticPlanFixtures.all();
        for (int index = 0; index < fixtures.size(); index++) check(PlanSnapshot.render(fixtures.get(index).plan()).equals(PlanSnapshot.render(regenerated.get(index).plan())), "Deterministic repeated construction " + index);
        SyntheticPlanFixtures.Fixture changed = SyntheticPlanFixtures.build(fixtures.get(1).id(), fixtures.get(1).profile(), SyntheticPlanFixtures.ANCHOR, 23);
        check(!PlanSnapshot.render(changed.plan()).equals(PlanSnapshot.render(fixtures.get(1).plan())), "Changed fixture context creates inspectable whole-plan diff");
        for (int index : new int[]{0, 2, 3, 4}) check(PlanSnapshot.render(fixtures.get(index).plan()).equals(PlanSnapshot.render(regenerated.get(index).plan())), "Unrelated runner snapshot unaffected " + index);
    }

    private static PlannedWorkout copy(PlannedWorkout value, String id, LocalDate date, WorkoutCategory category, Double distance) {
        return new PlannedWorkout(id, date, category, distance, value.duration(), value.segments(), value.note());
    }
    private static TrainingPlan plan(TrainingPlan original, List<PlannedWorkout> workouts) { return new TrainingPlan(original.id(), original.startDate(), original.endDate(), original.phases(), workouts); }
    private static PlanRegressionReport inspect(SyntheticPlanFixtures.Fixture fixture, TrainingPlan plan) { return PlanRegression.inspect(plan, fixture.profile(), fixture.lowerBodyDates(), SyntheticPlanFixtures.BUDGET); }
    private static String resource(String path) throws Exception {
        try (InputStream input = PlanRegressionTestSuite.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing snapshot resource " + path + "; include domain/src/test/resources in the test classpath");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private static void check(boolean condition, String reason) { checks++; if (!condition) throw new AssertionError(reason); }
}
