package org.racedaydreams.domain.activity;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Conservative matching on retained evidence. No score implies a physiological outcome. */
public final class ComparableRuns {
    private ComparableRuns() { }
    public record Match(ActivityRecord activity,String confidence,List<String> reasons,double relevance) { }
    public static List<Match> find(ActivityRecord current,List<ActivityRecord> history,List<PlannedWorkout> prescriptions) {
        Map<String,WorkoutCategory> types=new HashMap<>();for(var p:prescriptions)types.put(p.id(),p.category());
        var out=new ArrayList<Match>();
        if(current.distanceMetres()==null||current.distanceMetres()<500)return Immutable.list();
        var purpose=types.get(current.plannedWorkoutId());
        for(var prior:history){if(prior.id().equals(current.id())||!prior.start().isBefore(current.start())||prior.distanceMetres()==null)continue;
            double delta=Math.abs(prior.distanceMetres()-current.distanceMetres())/current.distanceMetres();if(delta>.12)continue;
            double durationDelta=Math.abs(prior.elapsedTime().toMillis()-current.elapsedTime().toMillis())/(double)current.elapsedTime().toMillis();if(durationDelta>.35)continue;
            var other=types.get(prior.plannedWorkoutId());if(purpose!=null&&other!=null&&purpose!=other)continue;
            if(current.elevationGainMetres()!=null&&prior.elevationGainMetres()!=null&&Math.abs(current.elevationGainMetres()-prior.elevationGainMetres())>Math.max(40,current.distanceMetres()*.015))continue;
            long age=Duration.between(prior.start(),current.start()).toDays();if(age>365)continue;
            var reasons=new ArrayList<String>();reasons.add("Distance within "+Math.round(delta*100)+"%");reasons.add("Similar elapsed duration");
            double score=50-delta*100-durationDelta*25-Math.min(age,180)/18.0;
            boolean known=purpose!=null&&purpose==other;if(known){score+=25;reasons.add("Same planned workout type");}
            if(current.elevationGainMetres()!=null&&prior.elevationGainMetres()!=null){score+=5;reasons.add("Similar recorded elevation gain");}
            // Unknown-purpose runs require close distance and duration; never imply route matching.
            if(!known&&(delta>.05||durationDelta>.15))continue;
            out.add(new Match(prior,known&&delta<=.05?"Strong":"Moderate",Immutable.copyList(reasons),score));
        }
        out.sort(Comparator.comparingDouble(Match::relevance).reversed().thenComparing(m->m.activity().id()));return Immutable.copyList(out);
    }
    public static String interpretation(ActivityRecord current,ActivityRecord prior) {
        if(current.distanceMetres()==null||prior.distanceMetres()==null)return "Distance evidence is unavailable.";
        double pace=current.elapsedTime().toMillis()/current.distanceMetres(),old=prior.elapsedTime().toMillis()/prior.distanceMetres();
        String speed=Math.abs(pace-old)<.6?"Similar average pace":pace<old?"Faster average pace":"Slower average pace";
        if(current.rpe()!=null&&prior.rpe()!=null){double effort=current.rpe()-prior.rpe();speed+=effort>=1?" with higher reported effort":effort<=-1?" with lower reported effort":" with similar reported effort";}
        else speed+="; reported effort is incomplete";
        return speed+". This comparison alone does not establish a fitness change.";
    }
}
