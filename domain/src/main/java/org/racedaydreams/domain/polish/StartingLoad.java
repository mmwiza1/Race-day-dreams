package org.racedaydreams.domain.polish;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.strength.*;
/** Reference only, never an actual set or an automatic progression. */
public final class StartingLoad {
    public record Reference(double lowKg,double highKg,String reason) {}
    public static Reference suggest(GymProgramme.Prescription p,List<StrengthSession> history,Instant now){
        if(p.minimumReps()<1)return null;
        var eligible=new ArrayList<StrengthSession>();
        for(var s:history)if(!s.startedAt().isAfter(now)&&!s.startedAt().isBefore(now.minus(Duration.ofDays(28))))eligible.add(s);
        eligible.sort(Comparator.comparing(StrengthSession::startedAt).reversed());
        for(var s:eligible){var loads=new ArrayList<Double>();for(var e:s.exercises())if(e.exercise().id().equals(p.exerciseId())&&e.exercise().mode()==StrengthExercise.Mode.WEIGHTED)for(var set:e.sets())if(!set.warmUp()&&set.kilograms()>0&&set.reps()>=p.minimumReps()&&set.reps()<=p.maximumReps()+2&&set.coachingRpe()!=null&&set.coachingRpe()>=4&&set.coachingRpe()<=p.maximumRpe())loads.add(set.kilograms());
            if(loads.size()>=2){double proven=Collections.min(loads);return new Reference(Math.floor(proven*.95*2)/2,proven,"Optional reference from "+loads.size()+" recent exact-exercise working sets at recorded effort. Keep another manageable load if preferred.");}}
        return null;
    }
}
