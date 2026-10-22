package org.racedaydreams.domain.coaching;

import java.time.*;
import java.util.List;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.strength.*;

/** Session-RPE is a transparent internal-load context signal, never an injury prediction. */
public record TrainingLoadEvidence(Double runningLoad, int runsWithRpe, int runsObserved,
        Double strengthLoad, int strengthWithRpe, int strengthObserved, Instant from, Instant to) {
    public static TrainingLoadEvidence window(List<ActivityRecord> runs, List<StrengthSession> strength, Instant from, Instant to) {
        if(!to.isAfter(from))throw new IllegalArgumentException("Workload window must have positive duration.");
        double running=0,gym=0;int n=0,k=0,observed=0,gymObserved=0;
        for(var r:runs)if(!r.end().isBefore(from)&&r.end().isBefore(to)){observed++;if(r.rpe()!=null){n++;running+=r.elapsedTime().toMillis()/60000.0*r.rpe();}}
        for(var s:strength)if(!s.startedAt().isBefore(from)&&s.startedAt().isBefore(to)){gymObserved++;Double load=StrengthAnalytics.sessionLoad(s);if(load!=null){k++;gym+=load;}}
        return new TrainingLoadEvidence(n==0?null:running,n,observed,k==0?null:gym,k,gymObserved,from,to);
    }
}
