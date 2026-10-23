package org.racedaydreams.domain.execution;

import java.util.*;

/** Sustained smoothed evidence, transition grace, hysteresis and bounded cue arbitration. */
public final class CoachCues {
    public record Cue(String id,String text,int priority,long atMillis,boolean haptic) {
        public Cue {if(id==null||text==null||priority<0||atMillis<0)throw new IllegalArgumentException("Invalid cue");}
    }
    private int step=-1,direction;private long since=-1,lastCue=-120000,graceUntil;private boolean alerted;
    public Cue pace(WorkoutExecutionEngine.State state,WorkoutExecutionEngine.Input input,long sustainedMillis){
        if(sustainedMillis<10000||sustainedMillis>120000)throw new IllegalArgumentException("Invalid sensitivity");
        if(step!=state.index()){step=state.index();graceUntil=input.activeMillis()+15000;reset();}
        var s=state.current();if(s==null||state.early()||input.paused()||!input.reliablePace()||input.smoothedPace()==null||s.fastPace()==null||s.slowPace()==null||input.activeMillis()<graceUntil){reset();return null;}
        double pace=input.smoothedPace(),margin=Math.max(8,s.fastPace()*.03);
        int d=pace<s.fastPace()-margin?-1:pace>s.slowPace()+margin?1:0;
        if(d==0){if(alerted&&pace>=s.fastPace()&&pace<=s.slowPace()){reset();return new Cue("pace-in-"+input.activeMillis(),"Back in target range.",4,input.activeMillis(),false);}if(!alerted)reset();return null;}
        if(d!=direction){direction=d;since=input.activeMillis();alerted=false;}
        if(!alerted&&input.activeMillis()-since>=sustainedMillis&&input.activeMillis()-lastCue>=90000){alerted=true;lastCue=input.activeMillis();return new Cue("pace-"+lastCue,d<0?"Slightly fast. Ease off.":"A little below target pace. Run by effort if conditions require.",4,lastCue,false);}return null;
    }
    private void reset(){direction=0;since=-1;alerted=false;}
    /** Only the most important fresh cue survives a batch. No stale speech backlog. */
    public static Cue select(List<Cue> cues,long now){return cues.stream().filter(c->now>=c.atMillis()&&now-c.atMillis()<=10000).min(Comparator.comparingInt(Cue::priority).thenComparing(Comparator.comparingLong(Cue::atMillis).reversed()).thenComparing(Cue::id)).orElse(null);}
}
