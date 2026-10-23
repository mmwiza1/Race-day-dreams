package org.racedaydreams.domain.polish;

import java.util.*;
import org.racedaydreams.domain.execution.WorkoutExecutionEngine;
import org.racedaydreams.domain.coaching.Immutable;

/** User-authored immutable prescription. No programme membership or completion side effect. */
public record CustomWorkout(String id,String title,List<Group> groups) {
    public record Group(int repeats,List<WorkoutExecutionEngine.Step> steps) {
        public Group { if(repeats<1||repeats>100||steps==null||steps.isEmpty())throw new IllegalArgumentException("Choose steps and 1–100 repeats");steps=Immutable.copyList(steps); }
    }
    public record Totals(double knownMetres,long knownMillis,boolean distanceComplete,boolean durationComplete) {}
    public CustomWorkout {
        if(id==null||id.trim().isEmpty()||id.length()>256||title==null||title.trim().isEmpty()||title.length()>120||groups==null||groups.isEmpty())throw new IllegalArgumentException("Name the workout and add a step");
        groups=Immutable.copyList(groups);for(var g:groups)for(var s:g.steps()){Objects.requireNonNull(s);if(s.metres()!=null&&s.metres()>300000||s.millis()!=null&&s.millis()>21600000)throw new IllegalArgumentException("A step must be at most 300 km or six hours");}int n=0;for(var g:groups)n+=g.repeats()*g.steps().size();if(n>500)throw new IllegalArgumentException("At most 500 executed steps");
    }
    public List<WorkoutExecutionEngine.Step> flattened(){var out=new ArrayList<WorkoutExecutionEngine.Step>();for(var g:groups)for(int i=0;i<g.repeats();i++)out.addAll(g.steps());return Immutable.copyList(out);}
    public Totals totals(){double m=0;long t=0;boolean allM=true,allT=true;for(var s:flattened()){if(s.metres()!=null)m+=s.metres();else allM=false;if(s.millis()!=null)t+=s.millis();else allT=false;}return new Totals(m,t,allM,allT);}
    public CustomWorkout move(int from,int to){var rows=new ArrayList<>(groups);var g=rows.remove(from);rows.add(to,g);return new CustomWorkout(id,title,rows);}
}
