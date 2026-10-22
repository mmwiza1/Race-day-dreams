package org.racedaydreams.domain.coaching;

import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.strength.*;

/** Product heuristics over raw evidence. Reviews/proposals never mutate a plan. No score or body mass input. */
public final class WeeklyCoaching {
    private WeeklyCoaching() { }
    public enum State { IMPROVING, STABLE, NEEDS_ATTENTION, INSUFFICIENT_EVIDENCE }
    public record Channel(String name,State state,String reason) { }
    public record Choice(String id,String label,List<PlannedWorkout> replacements) {
        public Choice { replacements=Immutable.copyList(replacements); }
    }
    public record Review(String id,String baseVersionId,LocalDate week,int planned,int completed,double kilometres,
            int strengthSessions,Double longestKm,List<String> observations,List<Channel> channels,String recommendation,List<Choice> choices) {
        public Review { observations=Immutable.copyList(observations);channels=Immutable.copyList(channels);choices=Immutable.copyList(choices); }
    }
    public record Interruption(LocalDate from,LocalDate returned,String reason) {
        public Interruption { if(from==null||returned==null||returned.isBefore(from)||reason==null)throw new IllegalArgumentException("Check interruption dates and reason."); }
    }
    /** Evaluates the latest fully finished Monday–Sunday week; up to four prior weeks provide context. */
    public static Review review(PlanVersion version,List<PlanVersion> history,List<ActivityRecord> runs,List<RunExecution> links,
            List<StrengthSession> strength,List<RecordedResult> results,LocalDate today,Interruption interruption) {
        return review(version,history,runs,links,strength,results,today,interruption,Immutable.list());
    }
    public static Review review(PlanVersion version,List<PlanVersion> history,List<ActivityRecord> runs,List<RunExecution> links,
            List<StrengthSession> strength,List<RecordedResult> results,LocalDate today,Interruption interruption,List<WorkoutDecision> decisions) {
        LocalDate end=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),from=end.minusWeeks(1),window=end.minusWeeks(4);
        Map<String,PlannedWorkout> original=new HashMap<>();
        // Earliest prescription supports moved-then-completed pattern explanations.
        var versions=new ArrayList<>(history);versions.sort(Comparator.comparing(PlanVersion::createdAt));
        for(var v:versions)for(var w:v.plan().workouts())original.putIfAbsent(w.id(),w);
        for(var w:version.plan().workouts())original.putIfAbsent(w.id(),w);
        Map<String,String> linked=new HashMap<>();for(var l:links)if(l.plannedWorkoutId()!=null)linked.put(l.activityId(),l.plannedWorkoutId());
        Map<String,ActivityRecord> actual=new HashMap<>();for(var a:runs){String key=linked.getOrDefault(a.id(),a.plannedWorkoutId());if(key!=null&&!a.end().isAfter(today.atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant()))actual.put(key,a);}
        int planned=0,completed=0,recentCount=0,priorCount=0,shortened=0,hard=0;double km=0,longest=0,recentEffort=0,priorEffort=0;
        List<String> observations=new ArrayList<>();List<Channel> channels=new ArrayList<>();
        int faded=0,durable=0;
        Set<String> countedRuns=new HashSet<>();
        for(var a:runs){LocalDate date=date(a);if(!date.isBefore(from)&&date.isBefore(end)&&countedRuns.add(a.id())&&a.distanceMetres()!=null){km+=a.distanceMetres()/1000;longest=Math.max(longest,a.distanceMetres()/1000);}}
        Map<WorkoutCategory,Integer> difficultTypes=new EnumMap<>(WorkoutCategory.class);int longShortened=0,longDurationMisses=0,strengthPrecededMisses=0;
        Set<LocalDate> recordedHeavy=StrengthAnalytics.hardLowerBodyDates(strength);
        Map<DayOfWeek,Set<LocalDate>> difficult=new EnumMap<>(DayOfWeek.class);Map<DayOfWeek,Integer> moved=new EnumMap<>(DayOfWeek.class);
        var reviewedWorkouts=new ArrayList<>(version.plan().workouts());
        for(var decision:decisions)if(decision.kind()==WorkoutDecision.Kind.SKIPPED&&original.containsKey(decision.workoutId())&&reviewedWorkouts.stream().noneMatch(w->w.id().equals(decision.workoutId())))reviewedWorkouts.add(original.get(decision.workoutId()));
        for(var w:reviewedWorkouts) {
            if(w.category()==WorkoutCategory.RACE)continue;
            var a=actual.get(w.id());
            if(!w.date().isBefore(from)&&w.date().isBefore(end)){planned++;if(a!=null)completed++;}
            if(w.date().isBefore(window)||!w.date().isBefore(end))continue;
            var first=original.getOrDefault(w.id(),w);
            boolean wasMoved=!first.date().equals(w.date())||a!=null&&!date(a).equals(first.date());
            if(a==null){difficultTypes.merge(w.category(),1,Integer::sum);if(w.duration()!=null&&w.duration().toMinutes()>=60)longDurationMisses++;if(recordedHeavy.contains(first.date().minusDays(1)))strengthPrecededMisses++;}
            if(a==null||wasMoved){var day=first.date().getDayOfWeek();difficult.computeIfAbsent(day,d->new HashSet<>()).add(first.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));if(wasMoved&&a!=null)moved.merge(day,1,Integer::sum);}
            if(a==null)continue;
            if(a.rpe()!=null){if(w.date().isBefore(end.minusWeeks(2))){priorEffort+=a.rpe();priorCount++;}else{recentEffort+=a.rpe();recentCount++;}if(a.rpe()>(AdaptiveCoachingEngine.isQuality(w.category())?8:5))hard++;}
            if(w.distanceKm()!=null&&a.distanceMetres()!=null&&a.distanceMetres()<w.distanceKm()*800){shortened++;if(w.category()==WorkoutCategory.LONG_RUN)longShortened++;}
            if(w.category()==WorkoutCategory.LONG_RUN){var d=LongRunDurability.analyse(a);if(d.sufficient()){durable++;if(d.paceFadePercent()>8)faded++;}}
        }
        for(var entry:difficult.entrySet())if(entry.getValue().size()>=3){int n=moved.getOrDefault(entry.getKey(),0);observations.add(entry.getKey().toString()+" was missed or moved in "+entry.getValue().size()+" of the last four weeks ("+n+" moved and completed). Consider editing your regular days; no catch-up mileage is suggested.");}
        for(var entry:difficultTypes.entrySet())if(entry.getValue()>=3)observations.add(entry.getValue()+" "+entry.getKey().name().toLowerCase(java.util.Locale.UK).replace('_',' ')+" sessions were not recorded recently. Consider whether this session type fits your week.");
        if(longDurationMisses>=3)observations.add("Several prescribed sessions of at least an hour were not recorded. Shorter sessions may fit your routine better.");
        if(strengthPrecededMisses>=3)observations.add("At least three unrecorded runs followed actual hard lower-body work. Consider separating those sessions.");
        if(longShortened>=3)observations.add("At least three long runs were shortened. Repeating the current endurance step may fit better than extending it.");
        boolean rising=recentCount>=3&&priorCount>=2&&recentEffort/recentCount-priorEffort/priorCount>=1.5;
        boolean strain=(rising&&shortened>=2)||hard>=3||faded>=2;
        boolean consistent=planned>=2&&completed==planned;
        long explicitSkips=decisions.stream().filter(d->d.kind()==WorkoutDecision.Kind.SKIPPED&&!d.originalDate().isBefore(window)&&d.originalDate().isBefore(end)).count();
        if(explicitSkips>0)observations.add(explicitSkips+" sessions were explicitly skipped; this is distinct from an unrecorded workout.");
        observations.add(completed+" / "+planned+" planned runs completed in the reviewed week.");
        if(shortened>=2)observations.add(shortened+" recent sessions were below 80% of their planned distance.");
        if(rising)observations.add("Reported effort increased across several sessions. Workout mix and conditions may contribute.");
        if(faded>=2)observations.add("At least two long runs slowed by over 8% in the second half. Review terrain, conditions and effort before progressing duration.");
        channels.add(new Channel("Volume tolerance",strain?State.NEEDS_ATTENTION:completed>=2?State.STABLE:State.INSUFFICIENT_EVIDENCE,"Completion and reported response considered together; distance alone does not establish tolerance."));
        channels.add(new Channel("Intensity tolerance",hard>=3?State.NEEDS_ATTENTION:recentCount>=3?State.STABLE:State.INSUFFICIENT_EVIDENCE,"Uses session-specific RPE context, not Run Score."));
        channels.add(new Channel("Endurance durability",faded>=2?State.NEEDS_ATTENTION:durable>=2?State.STABLE:State.INSUFFICIENT_EVIDENCE,durable+" recent long runs had qualified continuous samples."));
        channels.add(new Channel("Consistency",consistent?State.STABLE:planned>=2?State.NEEDS_ATTENTION:State.INSUFFICIENT_EVIDENCE,"Moved and later completed sessions count as completed."));
        long gyms=strength.stream().filter(s->!s.startedAt().atOffset(s.offset()).toLocalDate().isBefore(from)&&s.startedAt().atOffset(s.offset()).toLocalDate().isBefore(end)).count();
        var lower=StrengthAnalytics.hardLowerBodyDates(strength);
        boolean interference=version.plan().workouts().stream().anyMatch(w->!w.date().isBefore(end)&&w.date().isBefore(end.plusWeeks(1))&&AdaptiveCoachingEngine.isDemanding(w.category())&&lower.contains(w.date().minusDays(1)));
        channels.add(new Channel("Strength interference",interference?State.NEEDS_ATTENTION:strength.isEmpty()?State.INSUFFICIENT_EVIDENCE:State.STABLE,"Only logged exercises/sets and effort count as completed strength work."));
        channels.add(new Channel("Subjective response",rising?State.NEEDS_ATTENTION:recentCount>=3?State.STABLE:State.INSUFFICIENT_EVIDENCE,"RPE is optional; missing entries remain unknown."));
        boolean major=majorPerformance(results,end)&&consistent&&recentCount>=3&&!rising&&hard==0;
        channels.add(new Channel("Performance trend",major?State.IMPROVING:State.INSUFFICIENT_EVIDENCE,major?"A substantial comparable result improvement is supported by completion and controlled effort.":"No corroborated major performance change established."));
        List<Choice> choices=new ArrayList<>();choices.add(new Choice("keep","Keep original plan",Immutable.list()));
        String recommendation="No adjustment required. Continue the planned progression.";
        if(strain){recommendation="Hold the next two weeks rather than increasing workload; reducing remains an option.";choices.add(new Choice("hold","Hold volume for two weeks",scaled(version,end,2,km)));choices.add(new Choice("reduce","Reduce the next two weeks",scaled(version,end,2,km*.8)));}
        LocalDate last=runs.stream().filter(a->!date(a).isAfter(today)).map(WeeklyCoaching::date).max(LocalDate::compareTo).orElse(null);
        long gap=last==null?0:java.time.temporal.ChronoUnit.DAYS.between(last,today);
        if(interruption!=null&&interruption.returned().isBefore(today.plusDays(1))&&!interruption.returned().isBefore(today.minusWeeks(3)))gap=Math.max(gap,java.time.temporal.ChronoUnit.DAYS.between(interruption.from(),interruption.returned()));
        if(gap>=14){recommendation="Consider a three-week return bridge: reduced easy running, rebuild frequency, then reassess quality. Previous fitness still matters; this is not medical clearance.";choices.add(new Choice("return","Use a three-week return bridge",bridge(version,today,gap)));}
        if(major){observations.add("Recent performance may support reviewing quality paces. Easy/recovery effort and your race target remain unchanged.");var pace=qualityPaces(version,end);if(!pace.isEmpty())choices.add(new Choice("paces","Review and accept 3% faster quality ranges",pace));}
        String signature=version.id()+from+observations+channels+choices;
        return new Review(UUID.nameUUIDFromBytes(signature.getBytes(StandardCharsets.UTF_8)).toString(),version.id(),from,planned,completed,km,(int)gyms,longest==0?null:longest,observations,channels,recommendation,choices);
    }
    private static LocalDate date(ActivityRecord a){return a.start().atOffset(a.startOffset()==null?ZoneOffset.UTC:a.startOffset()).toLocalDate();}
    private static boolean majorPerformance(List<RecordedResult> results,LocalDate end){
        for(var r:results){if(r.source()==ResultSource.ROLLING||r.date().isBefore(end.minusWeeks(4))||!r.date().isBefore(end))continue;
            var previous=results.stream().filter(old->old.distance().equals(r.distance())&&old.source()!=ResultSource.ROLLING&&old.date().isBefore(r.date())&&!old.date().isBefore(end.minusMonths(6))).map(RecordedResult::duration).min(Duration::compareTo);
            if(previous.isPresent()&&r.duration().toMillis()<previous.get().toMillis()*.95)return true;
        }return false;
    }
    private static List<PlannedWorkout> scaled(PlanVersion v,LocalDate start,int weeks,double ceiling){var out=new ArrayList<PlannedWorkout>();for(int i=0;i<weeks;i++){LocalDate from=start.plusWeeks(i),to=from.plusWeeks(1);double total=v.plan().workouts().stream().filter(w->!w.date().isBefore(from)&&w.date().isBefore(to)&&w.category()!=WorkoutCategory.RACE&&w.distanceKm()!=null).mapToDouble(w->w.distanceKm()).sum();double factor=total>0&&ceiling>0?Math.min(1,ceiling/total):.8;for(var w:v.plan().workouts())if(!w.date().isBefore(from)&&w.date().isBefore(to)&&w.category()!=WorkoutCategory.RACE)out.add(resize(w,factor,false));}return out;}
    private static List<PlannedWorkout> bridge(PlanVersion v,LocalDate start,long gap){var out=new ArrayList<PlannedWorkout>();for(var w:v.plan().workouts())if(!w.date().isBefore(start)&&w.date().isBefore(start.plusWeeks(3))&&w.category()!=WorkoutCategory.RACE){long week=java.time.temporal.ChronoUnit.DAYS.between(start,w.date())/7;double f=week==0?(gap>=28?.5:.65):week==1?.75:.85;out.add(resize(w,f,true));}return out;}
    private static PlannedWorkout resize(PlannedWorkout w,double factor,boolean easy){Double km=w.distanceKm()==null?null:Math.max(.1,Math.floor(w.distanceKm()*factor*10)/10.0);Duration d=w.duration()==null?null:Duration.ofSeconds(Math.max(60,(long)(w.duration().getSeconds()*factor)));var segments=new ArrayList<WorkoutSegment>();if(easy)segments.add(new WorkoutSegment(WorkoutSegment.Kind.RUN,km,d,null,null,null,3.0,"Conversational effort; no catch-up mileage."));else for(var s:w.segments())segments.add(new WorkoutSegment(s.kind(),s.distanceKm()==null?null:s.distanceKm()*factor,s.duration()==null?null:Duration.ofSeconds(Math.max(1,(long)(s.duration().getSeconds()*factor))),s.minimumSecondsPerKm(),s.maximumSecondsPerKm(),s.heartRateZone(),s.rpe(),s.guidance()));return new PlannedWorkout(w.id(),w.date(),easy?WorkoutCategory.EASY_RUN:w.category(),km,d,segments,"Runner-selected reduced workload. Original prescription is retained in plan history.");}
    private static List<PlannedWorkout> qualityPaces(PlanVersion v,LocalDate start){var out=new ArrayList<PlannedWorkout>();for(var w:v.plan().workouts())if(!w.date().isBefore(start)&&w.date().isBefore(start.plusWeeks(4))&&AdaptiveCoachingEngine.isQuality(w.category())){var segments=new ArrayList<WorkoutSegment>();boolean changed=false;for(var s:w.segments()){boolean update=s.kind()==WorkoutSegment.Kind.RUN&&s.minimumSecondsPerKm()!=null&&s.maximumSecondsPerKm()!=null;changed|=update;segments.add(update?new WorkoutSegment(s.kind(),s.distanceKm(),s.duration(),s.minimumSecondsPerKm()*.97,s.maximumSecondsPerKm()*.97,s.heartRateZone(),s.rpe(),s.guidance()):s);}if(changed)out.add(new PlannedWorkout(w.id(),w.date(),w.category(),w.distanceKm(),w.duration(),segments,"Runner-approved modest quality pace review; easy effort and race target unchanged."));}return out;}
    public static TrainingPlan apply(PlanVersion v,Review review,Choice choice,Set<String> completed){if(!v.id().equals(review.baseVersionId())||!review.choices().contains(choice))throw new IllegalArgumentException("Review no longer matches the plan.");Map<String,PlannedWorkout> changes=new HashMap<>();for(var w:choice.replacements()){if(completed.contains(w.id()))throw new IllegalArgumentException("Completed sessions cannot change.");changes.put(w.id(),w);}var out=new ArrayList<PlannedWorkout>();for(var w:v.plan().workouts())out.add(changes.getOrDefault(w.id(),w));return new TrainingPlan(v.planId(),v.plan().startDate(),v.plan().endDate(),v.plan().phases(),out);}
}
