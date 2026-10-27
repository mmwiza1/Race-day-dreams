package org.racedaydreams.domain.strength;
import java.util.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Evidence-backed suggestions, never an automatic load increment or a plan mutation. */
public final class StrengthProgressionAdvisor {
    private StrengthProgressionAdvisor() { }
    public enum Action { GATHER_EVIDENCE, HOLD, CONSIDER_INCREASE, REDUCE_OR_SIMPLIFY }
    public record Advice(Action action,String reason,Double previousKilograms,Double suggestedKilograms,List<String> options){}
    public static Advice review(GymProgramme.Prescription prescription,List<StrengthSession> history,java.time.Instant asOf){
        Objects.requireNonNull(asOf);
        var earliest=asOf.minus(java.time.Duration.ofDays(28));
        // Each supporting session must be recent; a fresh latest session cannot refresh old evidence.
        var recent=history.stream()
                .filter(s->!s.startedAt().isBefore(earliest)&&!s.startedAt().isAfter(asOf))
                .collect(java.util.stream.Collectors.toList());
        boolean anyWorkingEvidence=history.stream().anyMatch(s->s.exercises().stream()
                .anyMatch(e->e.exercise().id().equals(prescription.exerciseId())
                        &&e.sets().stream().anyMatch(set->!set.warmUp())));
        var result=review(prescription,recent);
        if(result.action()==Action.GATHER_EVIDENCE&&anyWorkingEvidence)
            return advice(Action.HOLD,"Recent comparable strength evidence is needed before increasing the load. Older or future-dated sessions do not count as recent evidence.",null,null);
        return result;
    }
    public static Advice review(GymProgramme.Prescription prescription,List<StrengthSession> history){
        var sessions=new ArrayList<>(history);sessions.sort(Comparator.comparing(StrengthSession::startedAt).reversed().thenComparing(StrengthSession::id));
        var observations=new ArrayList<List<StrengthSession.SetEntry>>();
        for(var session:sessions){var sets=new ArrayList<StrengthSession.SetEntry>();for(var block:session.exercises())if(block.exercise().id().equals(prescription.exerciseId()))for(var set:block.sets())if(!set.warmUp())sets.add(set);if(!sets.isEmpty()){observations.add(sets);if(observations.size()==2)break;}}
        if(observations.isEmpty())return advice(Action.GATHER_EVIDENCE,"Record working sets first. No load has been inferred.",null,null);
        var latest=observations.get(0);double load=latest.get(0).kilograms();
        if(latest.size()<prescription.sets())return advice(Action.HOLD,"The latest session had fewer working sets than prescribed. Hold the load and review how the session felt.",load,null);
        if(latest.stream().anyMatch(s->s.coachingRpe()!=null&&s.coachingRpe()>prescription.maximumRpe()+.5||s.reps()>0&&s.reps()<prescription.minimumReps()))return advice(Action.REDUCE_OR_SIMPLIFY,"Recent working sets were harder or shorter than the intended range. Consider less load or fewer sets.",load,null);
        if(observations.size()<2)return advice(Action.HOLD,"One completed session is useful evidence; repeat before considering progression.",load,null);
        if(prescription.minimumReps()==0)return advice(Action.HOLD,"Time and distance progression needs repeated task-specific evidence; retain the current prescription.",load,null);
        boolean top=observations.stream().allMatch(sets->sets.size()>=prescription.sets()&&sets.stream().allMatch(s->s.reps()>=prescription.maximumReps()&&s.coachingRpe()!=null&&s.coachingRpe()<=prescription.maximumRpe()&&s.kilograms()==load));
        if(top&&load>0&&Math.round(load*1.025*2)/2.0>load&&Math.round(load*1.025*2)/2.0<=1500)return advice(Action.CONSIDER_INCREASE,"Two recorded sessions reached the top of the rep range at the intended effort and the same load. A small increase is an option, not a requirement.",load,Math.round(load*1.025*2)/2.0);
        return advice(Action.HOLD,"Current evidence supports repeating the load; missing effort is not assumed to be easy.",load,null);
    }
    private static Advice advice(Action action,String reason,Double prior,Double suggested){return new Advice(action,reason,prior,suggested,action==Action.CONSIDER_INCREASE?Immutable.list("Keep current load","Try a small increase"):Immutable.list("Keep current prescription","Use a lighter or simpler option"));}
}
