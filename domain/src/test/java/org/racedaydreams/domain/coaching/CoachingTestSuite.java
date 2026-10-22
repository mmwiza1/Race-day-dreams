package org.racedaydreams.domain.coaching;

import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.*;
import org.racedaydreams.domain.strength.*;

public final class CoachingTestSuite {
    static int checks;
    static final LocalDate START = LocalDate.of(2026, 10, 26);
    static final Instant NOW = START.atTime(12, 0).toInstant(ZoneOffset.UTC);
    public static void main(String[] args) {
        plans(); scores(); strength(); adaptations();
        System.out.println("CoachingTestSuite: " + checks + " checks passed (production generator, constraints, adaptation, scores, strength).");
    }
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static void rejects(Runnable test) { checks++; try { test.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("Expected validation rejection"); }
    static RunnerBaseline baseline(double weekly, double longest) {
        return new RunnerBaseline(weekly, 4.0, longest, 45.0, null, 10, null, null, null, 30, 10,
            NOW.minus(Duration.ofDays(84)), NOW, NOW.minus(Duration.ofDays(1)), BaselineConfidence.STRONG,
            Map.of(), List.of(), List.of(), List.of());
    }
    static UserProfile profile(RaceDistance distance, int days, int weeks, TrainingStrategy strategy) {
        Set<DayOfWeek> available = days == 3 ? EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY)
            : days == 4 ? EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            : EnumSet.range(DayOfWeek.TUESDAY, DayOfWeek.SUNDAY);
        return new UserProfile(new RaceGoal(distance, "", distance == RaceDistance.GENERAL_FITNESS ? null : START.plusWeeks(weeks).minusDays(1),
            distance == RaceDistance.HALF_MARATHON ? GoalMode.TARGET_TIME : GoalMode.FINISH,
            distance == RaceDistance.HALF_MARATHON ? Duration.ofSeconds(7199) : null),
            new TrainingPreferences(strategy, available, days, DayOfWeek.SUNDAY, EnumSet.of(DayOfWeek.WEDNESDAY), ScheduleType.FLEXIBLE, UnitSystem.METRIC), true);
    }
    static void plans() {
        var generator = new OriginalTrainingPlanGenerator();
        for (var distance : RaceDistance.values()) for (int days : new int[]{3, 4, 6}) for (int weeks : new int[]{1, 6, 12, 22, 52}) {
            var profile = profile(distance, days, weeks, TrainingStrategy.BALANCED);
            var baseline = baseline(distance == RaceDistance.MARATHON ? 45 : 25, distance == RaceDistance.MARATHON ? 18 : 10);
            var request = new PlanRequest(profile, baseline, START, Set.of(START.plusDays(3)), List.of());
            var plan = generator.generate(request);
            check(plan.equals(generator.generate(request)), "Whole plan is deterministic");
            Set<LocalDate> seen = new HashSet<>(); Map<LocalDate, Integer> frequency = new HashMap<>();
            LocalDate lastHard = null;
            for (var workout : plan.workouts()) {
                check(seen.add(workout.date()), "No duplicated placement");
                check(profile.preferences().runningDays().contains(workout.date().getDayOfWeek()), "Availability is hard");
                check(!request.unavailableDates().contains(workout.date()), "Unavailable dates are hard");
                check(!workout.date().isAfter(plan.endDate()), "No workouts after race");
                check(workout.note() != null && workout.note().contains("evidence") || workout.category() == WorkoutCategory.RACE, "Explanation reflects evidence");
                var monday = workout.date().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                frequency.merge(monday, 1, Integer::sum);
                if (AdaptiveCoachingEngine.isDemanding(workout.category()) && workout.category() != WorkoutCategory.RACE) {
                    check(lastHard == null || java.time.temporal.ChronoUnit.DAYS.between(lastHard, workout.date()) >= 2, "No consecutive demanding sessions");
                    lastHard = workout.date();
                }
            }
            check(frequency.values().stream().allMatch(n -> n <= days), "Maximum frequency is hard");
            check(plan.phases().get(0).startDate().equals(START), "Phases cover start");
            check(plan.phases().get(plan.phases().size()-1).endDate().equals(plan.endDate()), "Phases cover end");
            if (profile.raceGoal().raceDate() != null) check(plan.workouts().get(plan.workouts().size()-1).category() == WorkoutCategory.RACE, "Race is anchored");
        }
        var empty = RunnerBaseline.empty(NOW.minus(Duration.ofDays(84)), NOW);
        var beginner = generator.generate(profile(RaceDistance.FIVE_K, 3, 12, TrainingStrategy.DISTANCE_FIRST), empty, START);
        check(beginner.workouts().stream().filter(w -> w.category() != WorkoutCategory.RACE).allMatch(w -> w.distanceKm() == null && w.duration() != null), "Missing evidence produces time-based run/walk, no fake mileage");
        var zone = generator.generate(profile(RaceDistance.GENERAL_FITNESS, 4, 12, TrainingStrategy.ZONE_2), baseline(20, 8), START);
        check(zone.workouts().stream().noneMatch(w -> AdaptiveCoachingEngine.isQuality(w.category())), "Aerobic strategy has no unnecessary intensity");
        var shortPlan = generator.generate(profile(RaceDistance.MARATHON, 3, 6, TrainingStrategy.BALANCED), baseline(12, 7), START);
        check(shortPlan.workouts().stream().anyMatch(w -> w.note().contains("less preparation time")), "Short marathon horizon explained");
        rejects(() -> generator.generate(new PlanRequest(profile(RaceDistance.FIVE_K, 3, 12, TrainingStrategy.BALANCED), empty, START,
            Set.of(START.plusWeeks(12).minusDays(1)), List.of())));
        // Snapshot of the entire generated programme, readable and reviewable rather than only aggregates.
        var canonical = generator.generate(profile(RaceDistance.HALF_MARATHON, 4, 12, TrainingStrategy.BALANCED), baseline(25, 12), START);
        String snapshot = canonical.workouts().stream().map(w -> w.date()+"|"+w.category()+"|"+w.distanceKm()+"|"+w.duration()).collect(java.util.stream.Collectors.joining("\n")) + "\n";
        try {
            String expected = new String(Objects.requireNonNull(CoachingTestSuite.class.getResourceAsStream("/coaching/half-marathon.txt")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            check(snapshot.equals(expected), "Production half-marathon snapshot differs; inspect complete diff before updating fixture");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
    static PlannedWorkout planned(WorkoutCategory category) { return new PlannedWorkout("run", START, category, 8.0, null,
        List.of(new WorkoutSegment(WorkoutSegment.Kind.RUN, 8.0, null, null, null, null, 3.0, "Conversational")), "Purpose"); }
    static ActivityRecord actual(String id, double km, Double rpe, String link) {
        return new ActivityRecord(id, new SourceIdentity("manual", id, null), NOW.minus(Duration.ofMinutes(50)), NOW,
            ZoneOffset.UTC, ZoneOffset.UTC, km*1000, null, null, null, null, null, rpe, "", List.of(), List.of(), link, null);
    }
    static void scores() {
        var response = new RunExecution("a", "run", "v1", null, null, "");
        for (var category : List.of(WorkoutCategory.EASY_RUN, WorkoutCategory.RECOVERY_RUN, WorkoutCategory.LONG_RUN, WorkoutCategory.ZONE_2_RUN)) {
            check(RunScoreEngine.analyse(planned(category), actual("a", 8, 3.0, "run"), response).score() == 10, "Controlled execution earns 10 without HR");
            check(RunScoreEngine.analyse(planned(category), actual("a", 8, 9.0, "run"), response).score() < 7, "Harder is not better");
        }
        check(RunScoreEngine.analyse(planned(WorkoutCategory.LONG_RUN), actual("a", 4, 8.0, "run"), response).score() < 7, "Incomplete hard long run differs from objective");
        check(RunScoreEngine.analyse(planned(WorkoutCategory.EASY_RUN), actual("a", 8, null, "run"), response).score() == null, "Completion alone does not manufacture execution quality");
        check(RunScoreEngine.analyse(null, actual("a", 8, 3.0, null), null).score() == null, "Unplanned run has analysis, no adherence score");
        check(RunScoreEngine.analyse(planned(WorkoutCategory.EASY_RUN), actual("a", 12, 3.0, "run"), response).wentWell().stream().noneMatch(s -> s.contains("prescribed amount")), "Extra distance not celebrated");
    }
    static StrengthSession strengthSession(String id, double kg, int reps, LocalDate day) {
        var e = StrengthExercise.starters().get(0);
        return new StrengthSession(id, day.atTime(18,0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC, Duration.ofMinutes(50), 8.0, "",
            List.of(new StrengthSession.ExerciseSets(e, List.of(new StrengthSession.SetEntry(id+"1", reps, kg, 8.0, false),
                new StrengthSession.SetEntry(id+"2", reps, kg, 8.0, false), new StrengthSession.SetEntry(id+"3", reps, kg, 8.0, false)))));
    }
    static void strength() {
        var first = strengthSession("s1", 80, 5, START); var second = strengthSession("s2", 90, 5, START.plusDays(7));
        check(StrengthAnalytics.context(first).lowerBody() == StrengthAnalytics.Involvement.HIGH, "Hard lower-body context");
        check(StrengthAnalytics.sessionLoad(first) == 400, "Duration × RPE load");
        var history = StrengthAnalytics.progression(List.of(second, first));
        check(history.stream().filter(r -> r.category().equals("Load × 5")).count() == 2, "Full five-rep PB progression");
        check(history.get(0).at().equals(first.startedAt()), "Dates retained and sorted");
        check(StrengthAnalytics.progression(List.of(first)).stream().noneMatch(r -> r.sessionId().equals("s2")), "Deleted sets cannot retain PB");
        check(Math.round(StrengthAnalytics.estimatedOneRepMax(first.exercises().get(0).exercise(), first.exercises().get(0).sets().get(0))) == 93, "Epley rounded estimate");
        check(StrengthAnalytics.estimatedOneRepMax(first.exercises().get(0).exercise(), new StrengthSession.SetEntry("high", 20, 40, null, false)) == null, "No high-rep e1RM");
        var custom = new StrengthExercise("custom", "My row", StrengthExercise.Region.UPPER, StrengthExercise.Mode.WEIGHTED, false);
        check(custom.name().equals("My row"), "Custom exercise is not a closed enum");
        rejects(() -> new StrengthSession.SetEntry("bad", 0, 10, null, false));
        rejects(() -> new StrengthSession.SetEntry("bad", 5, Double.NaN, null, false));
    }
    static void adaptations() {
        var profile = profile(RaceDistance.HALF_MARATHON, 4, 12, TrainingStrategy.BALANCED);
        var plan = new OriginalTrainingPlanGenerator().generate(profile, baseline(25,12), START);
        var version = new PlanVersion("v1", plan.id(), 1, NOW, "Initial", profile, "Test evidence", plan);
        check(AdaptiveCoachingEngine.review(version, List.of(), List.of(), List.of(), START, NOW).isEmpty(), "Perfect/quiet week needs no change");
        var quality = plan.workouts().stream().filter(w -> AdaptiveCoachingEngine.isQuality(w.category())).findFirst().orElseThrow();
        var gym = strengthSession("gym", 90, 5, quality.date().minusDays(1));
        var proposal = AdaptiveCoachingEngine.review(version, List.of(), List.of(), List.of(gym), quality.date().minusDays(1), gym.startedAt()).orElseThrow();
        check(proposal.options().stream().anyMatch(o -> o.action() == AdjustmentProposal.Action.KEEP), "Runner can reject recommendation");
        var keep = proposal.options().stream().filter(o -> o.action() == AdjustmentProposal.Action.KEEP).findFirst().orElseThrow();
        check(AdaptiveCoachingEngine.apply(plan, proposal, keep, Set.of()).equals(plan), "Keeping original does not rewrite programme");
        var easy = proposal.options().stream().filter(o -> o.id().equals("easy")).findFirst().orElseThrow();
        var changed = AdaptiveCoachingEngine.apply(plan, proposal, easy, Set.of());
        check(changed.workouts().stream().filter(w -> !plan.workouts().contains(w)).count() == 1, "Accepted change affects one workout only");
        check(plan.workouts().contains(quality), "Original prescription retained");
        rejects(() -> AdaptiveCoachingEngine.apply(plan, proposal, easy, Set.of(quality.id())));
    }
}
