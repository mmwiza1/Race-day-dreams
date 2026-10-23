package org.racedaydreams.domain.health;
import java.util.*;

/** Corroborated context only. No diagnosis, readiness number or plan mutation. */
public final class RecoveryCoaching {
    public static org.racedaydreams.domain.PlannedWorkout easyAlternative(org.racedaydreams.domain.PlannedWorkout w,String reason){
        if(w.category()==org.racedaydreams.domain.WorkoutCategory.RACE)throw new IllegalArgumentException("Your race goal is changed only through goal setup.");
        var segment=new org.racedaydreams.domain.WorkoutSegment(org.racedaydreams.domain.WorkoutSegment.Kind.RUN,w.distanceKm(),w.duration(),null,null,null,3.0,"Conversational effort; stop or shorten if needed. No catch-up mileage.");
        return new org.racedaydreams.domain.PlannedWorkout(w.id(),w.date(),org.racedaydreams.domain.WorkoutCategory.EASY_RUN,w.distanceKm(),w.duration(),Collections.singletonList(segment),"Runner chose an easy alternative. "+reason);
    }
    public record Context(List<String> evidence,List<String> options,boolean reviewSuggested) {}
    public static Context evaluate(List<HealthIntelligence.Trend> trends,boolean highRunningLoad,boolean repeatedHighRpe,boolean hardLowerBody){
        var reasons=new ArrayList<String>();boolean sleep=false,hrv=false,rhr=false;
        for(var t:trends){if(t.recentDays()<4||t.referenceDays()<14||t.recent()==null||t.baseline()==null)continue;
            boolean low=t.interpretation().equals("Below recent personal baseline"),high=t.interpretation().equals("Above recent personal baseline");
            if(t.metric()==HealthIntelligence.Metric.SLEEP_MINUTES&&low){sleep=true;reasons.add("Sleep duration is below your recent personal range.");}
            if(t.metric()==HealthIntelligence.Metric.HRV_RMSSD&&low){hrv=true;reasons.add("Comparable-source HRV is below your recent range.");}
            if(t.metric()==HealthIntelligence.Metric.RESTING_HR&&high){rhr=true;reasons.add("Resting heart rate is above your recent baseline.");}
            if(t.metric()==HealthIntelligence.Metric.STEPS&&high)reasons.add("General activity has been higher than usual; it is not converted into running stress.");
        }
        if(highRunningLoad)reasons.add("Recent running load is above your usual workload.");if(repeatedHighRpe)reasons.add("Several recent sessions had high reported effort.");if(hardLowerBody)reasons.add("Hard lower-body strength was recorded recently.");
        boolean suggest=sleep&&rhr&&highRunningLoad||hrv&&repeatedHighRpe||sleep&&repeatedHighRpe&&hardLowerBody;
        if(reasons.isEmpty())reasons.add("No corroborated recovery concern is available. Missing signals remain unknown.");
        return new Context(Collections.unmodifiableList(reasons),suggest?Arrays.asList("Keep planned workout","Run by effort","Choose easy instead","Move workout","Rest"):Collections.singletonList("Keep planned workout"),suggest);
    }
}
