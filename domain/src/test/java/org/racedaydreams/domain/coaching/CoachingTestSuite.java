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
        plans(); scores(); strength(); adaptations(); extraEvidence(); regressionProfiles();
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
    static void extraEvidence() {
        var generator=new OriginalTrainingPlanGenerator();
        for(int days:new int[]{1,2}){
            var prefs=new TrainingPreferences(TrainingStrategy.BALANCED,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.SUNDAY),days,DayOfWeek.SUNDAY,EnumSet.noneOf(DayOfWeek.class),ScheduleType.FLEXIBLE,UnitSystem.METRIC);
            var p=new UserProfile(new RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),prefs,true);
            var plan=generator.generate(p,baseline(60,15),START);
            check(plan.workouts().stream().allMatch(w->w.distanceKm()<=15),"Reduced availability cannot concentrate 60 km into one/two runs");
        }
        var intervalSegments=new ArrayList<WorkoutSegment>();
        for(int i=0;i<5;i++){intervalSegments.add(new WorkoutSegment(WorkoutSegment.Kind.RUN,1.0,null,290.0,310.0,null,7.0,"Rep"));intervalSegments.add(new WorkoutSegment(WorkoutSegment.Kind.RECOVERY,null,Duration.ofSeconds(90),null,null,null,2.0,"Recovery"));}
        var intervals=new PlannedWorkout("run",START,WorkoutCategory.INTERVALS,8.0,null,intervalSegments,"Five controlled reps");
        var complete=RunScoreEngine.analyse(intervals,actual("a",8,7.0,"run"),new RunExecution("a","run","v1",5,true,""));
        var missed=RunScoreEngine.analyse(intervals,actual("a",8,7.0,"run"),new RunExecution("a","run","v1",3,false,""));
        check(complete.score()>missed.score()&&missed.score()<=7,"Missed reps/recoveries reduce purpose-based score");
        var stable=actual("stable",8,6.0,"run");var samples=new ArrayList<DistanceSample>();
        for(int second=0;second<=3000;second+=10)samples.add(new DistanceSample(Duration.ofSeconds(second),8000.0*second/3000,null,null,null,false));
        stable=new ActivityRecord(stable.id(),stable.source(),stable.start(),stable.end(),stable.startOffset(),stable.endOffset(),stable.distanceMetres(),null,null,null,null,null,6.0,"",samples,List.of(),"run",null);
        var progression=RunScoreEngine.analyse(planned(WorkoutCategory.PROGRESSION_RUN),stable,new RunExecution("stable","run","v1",null,null,""));
        check(progression.score()<=7,"Stable pace does not achieve a progression objective");
        var p=profile(RaceDistance.HALF_MARATHON,4,12,TrainingStrategy.BALANCED);
        var easy1=planned(WorkoutCategory.EASY_RUN);var easy2=new PlannedWorkout("run2",START.minusDays(1),WorkoutCategory.EASY_RUN,8.0,null,easy1.segments(),"Easy");
        var quality=new PlannedWorkout("quality",START.plusDays(2),WorkoutCategory.INTERVALS,8.0,null,intervalSegments,"Quality");
        var plan=new TrainingPlan("adapt",START.minusDays(1),START.plusDays(6),List.of(),List.of(easy1,easy2,quality));
        var v=new PlanVersion("adapt-v1","adapt",1,NOW,"Initial",p,"Evidence",plan);
        check(AdaptiveCoachingEngine.review(v,List.of(actual("a",8,3.0,"run")),List.of(),List.of(),START,NOW).isEmpty(),"Good completion with low RPE needs no change");
        check(AdaptiveCoachingEngine.review(v,List.of(actual("a",8,7.0,"run")),List.of(),List.of(),START,NOW).isEmpty(),"One noisy hard run alone needs no change");
        check(AdaptiveCoachingEngine.review(v,List.of(actual("a",8,7.0,"run"),actual("b",8,7.0,"run2")),List.of(),List.of(),START,NOW).isPresent(),"Repeated effort mismatch proposes a small change");
        check(AdaptiveCoachingEngine.review(v,List.of(actual("a",4,9.0,"run")),List.of(),List.of(),START,NOW).isPresent(),"Low completion plus high RPE supplies two signals");
        var load=TrainingLoadEvidence.window(List.of(actual("a",8,3.0,null),actual("b",8,null,null)),List.of(),NOW.minus(Duration.ofDays(7)),NOW.plusSeconds(1));
        check(load.runningLoad()==150&&load.runsWithRpe()==1&&load.runsObserved()==2,"Missing RPE excluded with coverage, not zero-filled");
        check(load.strengthLoad()==null,"Missing strength workload is unknown, not zero");
        var weightedBody=new StrengthExercise("weighted","Weighted Pull-Up",StrengthExercise.Region.UPPER,StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,false);
        var bwSession=new StrengthSession("bw",NOW.minusSeconds(3600),ZoneOffset.UTC,Duration.ofMinutes(30),null,"",List.of(new StrengthSession.ExerciseSets(weightedBody,List.of(new StrengthSession.SetEntry("bw-set",5,20,null,false)))));
        check(StrengthAnalytics.progression(List.of(bwSession)).stream().anyMatch(r->r.category().equals("Added load × 5")),"Weighted-bodyweight PB retains added load and reps");
        check(StrengthAnalytics.estimatedOneRepMax(weightedBody,bwSession.exercises().get(0).sets().get(0))==null,"No misleading bodyweight 1RM estimate");
    }

    static Map<String, TrainingPlan> representativePlans() {
        var g=new OriginalTrainingPlanGenerator();var plans=new LinkedHashMap<String,TrainingPlan>();
        plans.put("beginner-5k",g.generate(profile(RaceDistance.FIVE_K,3,12,TrainingStrategy.DISTANCE_FIRST),RunnerBaseline.empty(NOW.minus(Duration.ofDays(84)),NOW),START));
        plans.put("established-10k",g.generate(profile(RaceDistance.TEN_K,4,12,TrainingStrategy.BALANCED),baseline(25,10),START));
        plans.put("sub2-half",g.generate(profile(RaceDistance.HALF_MARATHON,4,18,TrainingStrategy.BALANCED),baseline(25,13),START));
        plans.put("marathon",g.generate(profile(RaceDistance.MARATHON,4,22,TrainingStrategy.PERFORMANCE),baseline(45,18),START));
        plans.put("aerobic-manual-fallback",g.generate(profile(RaceDistance.GENERAL_FITNESS,3,12,TrainingStrategy.ZONE_2),RunnerBaseline.empty(NOW.minus(Duration.ofDays(84)),NOW),START));
        plans.put("three-days",g.generate(profile(RaceDistance.HALF_MARATHON,3,12,TrainingStrategy.BALANCED),baseline(20,10),START));
        plans.put("six-days",g.generate(profile(RaceDistance.TEN_K,6,12,TrainingStrategy.BALANCED),baseline(40,14),START));
        plans.put("heavy-lower-body",g.generate(new PlanRequest(profile(RaceDistance.TEN_K,6,12,TrainingStrategy.BALANCED),baseline(30,12),START.plusDays(1),Set.of(),List.of(strengthSession("heavy",90,5,START)))));
        plans.put("returning-runner",g.generate(profile(RaceDistance.FIVE_K,3,12,TrainingStrategy.DISTANCE_FIRST),baseline(5,3),START));
        plans.put("short-horizon",g.generate(profile(RaceDistance.MARATHON,3,3,TrainingStrategy.BALANCED),baseline(12,7),START));
        plans.put("long-horizon",g.generate(profile(RaceDistance.MARATHON,4,28,TrainingStrategy.BALANCED),baseline(45,18),START));
        return plans;
    }
    static String planSnapshot(TrainingPlan plan) {
        StringBuilder out=new StringBuilder();
        for(var phase:plan.phases())out.append("PHASE ").append(phase.kind()).append(" ").append(phase.startDate()).append("..").append(phase.endDate()).append("\n");
        for(var w:plan.workouts()){
            out.append(w.date()).append("|").append(w.category()).append("|").append(w.distanceKm()).append(" km|").append(w.duration()).append("\n");
            for(var segment:w.segments())out.append("  ").append(segment.kind()).append("|").append(segment.distanceKm()).append(" km|").append(segment.duration()).append("|RPE ").append(segment.rpe()).append("|pace ").append(segment.minimumSecondsPerKm()).append("..").append(segment.maximumSecondsPerKm()).append("\n");
        }
        return out.toString();
    }
    static void regressionProfiles(){
        for(var entry:representativePlans().entrySet())try{
            String expected=new String(Objects.requireNonNull(CoachingTestSuite.class.getResourceAsStream("/coaching/"+entry.getKey()+".txt")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            check(expected.equals(planSnapshot(entry.getValue())),"Review whole-plan diff for "+entry.getKey());
        }catch(java.io.IOException e){throw new AssertionError(e);}
        var prefs=profile(RaceDistance.TEN_K,4,12,TrainingStrategy.BALANCED).preferences();
        var schedule=ConstraintScheduler.select(START,START.plusDays(6),4,prefs,Set.of(),Set.of(START),true,null);
        check(!START.plusDays(1).equals(schedule.quality()),"Feasible alternative avoids quality the day after hard legs");
    }

}
