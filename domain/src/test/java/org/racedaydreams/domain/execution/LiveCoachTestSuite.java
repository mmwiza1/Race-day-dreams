package org.racedaydreams.domain.execution;
import java.util.*;
import java.time.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.health.*;
import org.racedaydreams.domain.race.*;
import org.racedaydreams.domain.support.*;
import org.racedaydreams.domain.activity.*;

public final class LiveCoachTestSuite {
    private static int checks;private static void check(boolean b){checks++;if(!b)throw new AssertionError("Check "+checks);}
    private static WorkoutExecutionEngine.Step distance(double m){return new WorkoutExecutionEngine.Step("Run",WorkoutSegment.Kind.RUN,m,null,295.0,305.0,7.0);}
    private static WorkoutExecutionEngine.Input in(double m,long t,Double p,boolean reliable,boolean paused){return new WorkoutExecutionEngine.Input(m,t,t+5000,p,reliable,paused);}
    public static void main(String[] args){
        var steps=new ArrayList<WorkoutExecutionEngine.Step>();steps.add(distance(1000));steps.addAll(WorkoutExecutionEngine.repetitions(3,distance(400),new WorkoutExecutionEngine.Step("Recovery",WorkoutSegment.Kind.RECOVERY,null,90000L,null,null,2.0)));steps.add(distance(1000));
        var e=new WorkoutExecutionEngine(steps);double m=0;long t=0;e.update(in(0,0,null,false,false));
        for(var s:steps){int before=e.state().index();long duration=s.metres()==null?s.millis():Math.round(s.metres()*300);double length=s.metres()==null?duration/1000.0*1.5:s.metres();for(long n=1000;n<=duration;n+=1000){e.update(in(m+length*n/duration,t+n,300.0,true,false));check(e.state().index()>=before);}m+=length;t+=duration;check(e.state().index()==before+1);}
        check(e.state().complete());check(e.events().stream().filter(x->x.type().equals("STEP_COMPLETED")).count()==7);check(e.events().stream().filter(x->x.type().equals("WORKOUT_COMPLETED")).count()==1);e.update(in(m,t+1000,null,false,false));check(e.events().size()==16);
        var over=new WorkoutExecutionEngine(Arrays.asList(distance(1000),distance(400)));over.update(in(1050,105000,100.0,true,false));check(Math.abs(over.state().metresRemaining()-350)<.01);check(over.events().get(2).activeMillis()==100000);
        var paused=new WorkoutExecutionEngine(Collections.singletonList(distance(100)));paused.update(in(0,0,null,false,false));paused.update(in(100,0,null,false,true));check(!paused.state().complete());paused.skip();check(paused.state().complete());
        var early=new WorkoutExecutionEngine(steps);early.update(in(50,15000,null,false,false));early.finishEarly();early.update(in(5000,1500000,null,false,false));check(early.state().early()&&!early.state().complete());
        var c=new CoachCues();var state=new WorkoutExecutionEngine(Collections.singletonList(distance(5000))).state();check(c.pace(state,in(0,0,270.0,true,false),30000)==null);check(c.pace(state,in(100,20000,270.0,true,false),30000)==null);check(c.pace(state,in(120,25000,300.0,true,false),30000)==null);check(c.pace(state,in(130,26000,270.0,true,false),30000)==null);check(c.pace(state,in(200,57000,270.0,true,false),30000)!=null);check(c.pace(state,in(210,58000,270.0,true,false),30000)==null);check(c.pace(state,in(220,59000,300.0,true,false),30000)!=null);check(c.pace(state,in(230,60000,270.0,false,false),30000)==null);
        var picked=CoachCues.select(Arrays.asList(new CoachCues.Cue("split","Split",5,100,false),new CoachCues.Cue("transition","Recovery",1,100,true)),101);check(picked.id().equals("transition"));check(CoachCues.select(Collections.singletonList(picked),20000)==null);
        var low=new HealthIntelligence.Trend(HealthIntelligence.Metric.SLEEP_MINUTES,"a",350.0,480.0,5,20,"Below recent personal baseline","");var hr=new HealthIntelligence.Trend(HealthIntelligence.Metric.RESTING_HR,"a",65.0,50.0,5,20,"Above recent personal baseline","");check(!RecoveryCoaching.evaluate(Collections.singletonList(low),false,false,false).reviewSuggested());check(RecoveryCoaching.evaluate(Arrays.asList(low,hr),true,false,false).reviewSuggested());check(!RecoveryCoaching.evaluate(Collections.emptyList(),false,false,false).reviewSuggested());
        var now=LocalDate.of(2026,10,23);check(RacePredictionEngine.predict(42195,Collections.emptyList(),Collections.emptyList(),now).lowSeconds()==null);
        var results=Arrays.asList(new RecordedResult("1",PerformanceDistance.TEN_K,Duration.ofMinutes(50),now.minusDays(7),"",ResultSource.OFFICIAL_CHIP,null,""),new RecordedResult("2",PerformanceDistance.TEN_K,Duration.ofMinutes(51),now.minusDays(20),"",ResultSource.MANUAL,null,""));check(RacePredictionEngine.predict(42195,results,Collections.emptyList(),now).lowSeconds()==null);check(RacePredictionEngine.predict(10000,results,Collections.emptyList(),now).lowSeconds()!=null);check(RaceStrategyEngine.create(21097.5,7200L,false).size()==3);check(RaceStrategyEngine.create(21097.5,7200L,true).get(0).fastPace()==null);
        var runs=new ArrayList<ActivityRecord>();
        for(int i=1;i<=28;i+=2){double km=i%6==1?18:8;runs.add(new ActivityRecord("a"+i,new SourceIdentity("manual","a"+i,null),now.minusDays(i).atStartOfDay(ZoneOffset.UTC).toInstant(),now.minusDays(i).atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((long)(km*360)),ZoneOffset.UTC,ZoneOffset.UTC,km*1000,null,null,null,null,null,3.0,null,Collections.emptyList(),Collections.emptyList(),null,null));}
        var hm=RacePredictionEngine.predict(21097.5,results,runs,now);check(hm.lowSeconds()!=null&&hm.highSeconds()>hm.lowSeconds());
        var old=results.stream().map(r->new RecordedResult(r.id(),r.distance(),r.duration(),now.minusDays(200),"",r.source(),null,"")).collect(java.util.stream.Collectors.toList());check(RacePredictionEngine.predict(21097.5,old,runs,now).lowSeconds()==null);
        var isolated=Collections.singletonList(results.get(0));check(RacePredictionEngine.predict(10000,isolated,runs,now).lowSeconds()==null);
        var rolling=results.stream().map(r->new RecordedResult(r.id(),PerformanceDistance.ONE_K,Duration.ofMinutes(3),r.date(),"",ResultSource.ROLLING,"recorded-"+r.id(),"")).collect(java.util.stream.Collectors.toList());check(RacePredictionEngine.predict(42195,rolling,runs,now).lowSeconds()==null);
        var duplicate=Arrays.asList(results.get(0),new RecordedResult("duplicate",results.get(0).distance(),results.get(0).duration(),results.get(0).date(),"",ResultSource.MANUAL,null,""));check(RacePredictionEngine.predict(10000,duplicate,runs,now).lowSeconds()==null);
        var hrv=new HealthIntelligence.Trend(HealthIntelligence.Metric.HRV_RMSSD,"a",30.0,50.0,5,20,"Below recent personal baseline","");check(RecoveryCoaching.evaluate(Collections.singletonList(hrv),false,true,false).reviewSuggested());check(!RecoveryCoaching.evaluate(Collections.singletonList(hrv),false,false,false).reviewSuggested());
        for(long minutes:new long[]{120,240,330}){var longSession=new WorkoutExecutionEngine(Collections.singletonList(new WorkoutExecutionEngine.Step("Long",WorkoutSegment.Kind.RUN,null,minutes*60000,null,null,3.0)));for(long n=0;n<=minutes*60000;n+=1000)longSession.update(in(n/1000.0*3,n,null,false,false));check(longSession.state().complete());}
        var prescribed=new PlannedWorkout("preserved",now,WorkoutCategory.INTERVALS,3.0,null,Arrays.asList(new WorkoutSegment(WorkoutSegment.Kind.RUN,.8,null,295.0,305.0,null,7.0,"Work"),new WorkoutSegment(WorkoutSegment.Kind.RECOVERY,null,Duration.ofSeconds(90),null,null,null,2.0,"Recovery")),"Original");
        var activity=new ActivityRecord("structured",new SourceIdentity("rdd","structured",null),Instant.EPOCH,Instant.EPOCH.plusSeconds(1500),ZoneOffset.UTC,ZoneOffset.UTC,3000.0,null,null,null,null,null,7.0,null,Collections.emptyList(),Collections.emptyList(),"preserved",null);
        var rawStep=new org.racedaydreams.domain.coaching.RunExecution.StepEvidence(0,WorkoutSegment.Kind.RUN,800,240000,295.0,305.0,true,true);
        var execution=new org.racedaydreams.domain.coaching.RunExecution("structured","preserved","v",1,true,"",Collections.singletonList(rawStep));
        var feedback=org.racedaydreams.domain.coaching.RunScoreEngine.analyse(prescribed,activity,execution);
        check(feedback.components().stream().filter(x->x.name().equals("Target pace")).findFirst().orElseThrow().fraction()==1.0);
        check(execution.steps().get(0).activeMillis()==240000);
        var sinkEvents=new ArrayList<String>();CoachCues.Sink sink=cue->sinkEvents.add(cue.id());sink.deliver(picked);check(sinkEvents.equals(Collections.singletonList("transition")));
        for(double distance:new double[]{1000,5000,10000,21097.5,42195,43000})for(int seed=0;seed<50;seed++){var assignment=DreamRoutes.assign(distance,new Random(seed));check(assignment.startMetres()+Math.min(distance,42195)<=42195.001);check(assignment.equals(DreamRoutes.assign(distance,new Random(seed))));}
        for(var r:RunningRoutines.all()){check(!r.movements().isEmpty()&&r.minutes()>0);for(var movement:r.movements())check(!movement.instructions().isEmpty()&&!movement.pose().isEmpty());}
        // Four-hour execution timing uses long counters, with no UI/GPS dependency.
        var longRun=new WorkoutExecutionEngine(Collections.singletonList(new WorkoutExecutionEngine.Step("Long",WorkoutSegment.Kind.RUN,null,14400000L,null,null,3.0)));for(long n=0;n<=14400000;n+=1000)longRun.update(in(n/1000.0*3,n,null,false,false));check(longRun.state().complete());
        System.out.println("LiveCoachTestSuite: "+checks+" checks passed (execution, cues, recovery, races, routines, Dream Routes).");
    }
}
