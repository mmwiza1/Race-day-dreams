package org.racedaydreams.domain.execution;

import java.util.*;
import org.racedaydreams.domain.*;

/** One deterministic projection over the recorder's accepted metres and active time.
 * Never records locations or generates a prescription. Distance wins when a legacy
 * segment contains both distance and estimated duration. Overshoot is apportioned
 * at the interpolated boundary, rather than counted again in the following step. */
public final class WorkoutExecutionEngine {
    public record Step(String label, WorkoutSegment.Kind kind, Double metres, Long millis,
                       Double fastPace, Double slowPace, Double rpe) {
        public Step {
            if(label==null||label.length()>500||kind==null)throw new IllegalArgumentException("Invalid workout step");
            if(metres!=null&&(!Double.isFinite(metres)||metres<=0)||millis!=null&&millis<=0)
                throw new IllegalArgumentException("Step targets must be positive");
            if(fastPace!=null&&(!Double.isFinite(fastPace)||fastPace<=0)||slowPace!=null&&(!Double.isFinite(slowPace)||slowPace<=0)||fastPace!=null&&slowPace!=null&&fastPace>slowPace)
                throw new IllegalArgumentException("Invalid pace range");
            if(rpe!=null&&(!Double.isFinite(rpe)||rpe<1||rpe>10))throw new IllegalArgumentException("Invalid effort");
        }
    }
    public record Input(double metres, long activeMillis, long elapsedMillis, Double smoothedPace,
                        boolean reliablePace, boolean paused) {
        public Input {if(!Double.isFinite(metres)||metres<0||activeMillis<0||elapsedMillis<activeMillis||smoothedPace!=null&&(!Double.isFinite(smoothedPace)||smoothedPace<=0))throw new IllegalArgumentException("Invalid recorder metrics");}
    }
    public record Event(String type,int step,double metres,long activeMillis) {}
    public record State(int index,int total,Step current,Step next,Double metresRemaining,Long millisRemaining,boolean complete,boolean early,boolean paused) {}
    private final List<Step> steps;private final List<Event> events=new ArrayList<>();
    private int index;private double startMetres;private long startMillis;private Input previous;private boolean early,started;
    public WorkoutExecutionEngine(List<Step> steps){if(steps.isEmpty()||steps.size()>500)throw new IllegalArgumentException("Choose 1–500 steps");this.steps=Collections.unmodifiableList(new ArrayList<>(steps));}
    public static List<Step> from(PlannedWorkout w){
        List<Step> out=new ArrayList<>();for(var s:w.segments())out.add(new Step(s.guidance()==null?s.kind().name().replace('_',' '):s.guidance(),s.kind(),s.distanceKm()==null?null:s.distanceKm()*1000,s.duration()==null?null:s.duration().toMillis(),s.minimumSecondsPerKm(),s.maximumSecondsPerKm(),s.rpe()));
        if(out.isEmpty())out.add(new Step(w.category().name().replace('_',' '),WorkoutSegment.Kind.RUN,w.distanceKm()==null?null:w.distanceKm()*1000,w.duration()==null?null:w.duration().toMillis(),null,null,null));
        return out;
    }
    /** Flatten reusable repetitions, without inserting a recovery after the last rep. */
    public static List<Step> repetitions(int count,Step work,Step recovery){if(count<1||count>100)throw new IllegalArgumentException("Invalid repetition count");var out=new ArrayList<Step>();for(int i=0;i<count;i++){out.add(work);if(i+1<count&&recovery!=null)out.add(recovery);}return out;}
    public State update(Input in){
        if(previous!=null&&(in.metres()<previous.metres()||in.activeMillis()<previous.activeMillis()||in.elapsedMillis()<previous.elapsedMillis()))throw new IllegalArgumentException("Recorder metrics moved backwards");
        if(!started){started=true;events.add(new Event("WORKOUT_STARTED",0,0,0));events.add(new Event("STEP_STARTED",0,0,0));}
        if(!in.paused()&&!early){while(index<steps.size()){
            Step s=steps.get(index);double dm=in.metres()-startMetres;long dt=in.activeMillis()-startMillis;
            boolean done=s.metres()!=null?dm>=s.metres():s.millis()!=null&&dt>=s.millis();if(!done)break;
            // Interpolate within the latest observation; never assume whole-session constant pace.
            double beforeM=previous==null?startMetres:Math.max(startMetres,previous.metres());long beforeT=previous==null?startMillis:Math.max(startMillis,previous.activeMillis());
            double fraction=s.metres()!=null?(startMetres+s.metres()-beforeM)/Math.max(.000001,in.metres()-beforeM):(double)(startMillis+s.millis()-beforeT)/Math.max(1,in.activeMillis()-beforeT);
            fraction=Math.max(0,Math.min(1,fraction));double boundaryM=beforeM+(in.metres()-beforeM)*fraction;long boundaryT=beforeT+Math.round((in.activeMillis()-beforeT)*fraction);
            advance("STEP_COMPLETED",boundaryM,boundaryT);
        }}
        previous=in;return state();
    }
    private void advance(String type,double m,long t){events.add(new Event(type,index,m,t));index++;startMetres=m;startMillis=t;events.add(new Event(index==steps.size()?"WORKOUT_COMPLETED":"STEP_STARTED",index,m,t));}
    public void skip(){if(previous!=null&&index<steps.size()&&!early)advance("STEP_SKIPPED",previous.metres(),previous.activeMillis());}
    public void finishEarly(){if(index<steps.size()&&!early){early=true;events.add(new Event("WORKOUT_FINISHED_EARLY",index,previous==null?0:previous.metres(),previous==null?0:previous.activeMillis()));}}
    public State state(){boolean complete=index==steps.size();Step s=complete?null:steps.get(index);double m=previous==null?0:previous.metres();long t=previous==null?0:previous.activeMillis();return new State(index,steps.size(),s,index+1<steps.size()?steps.get(index+1):null,s==null||s.metres()==null?null:Math.max(0,s.metres()-(m-startMetres)),s==null||s.metres()!=null||s.millis()==null?null:Math.max(0,s.millis()-(t-startMillis)),complete,early,previous!=null&&previous.paused());}
    public List<Event> events(){return Collections.unmodifiableList(new ArrayList<>(events));}
    public List<Step> steps(){return steps;}
}
