package org.racedaydreams.domain.presentation;

import java.util.Locale;
import org.racedaydreams.domain.health.HealthIntelligence;

/** Display-only translation. Personal evidence gates remain in HealthIntelligence. */
public record HealthContextSummary(String title, String value, String context) {
    public static HealthContextSummary of(HealthIntelligence.Trend t) {
        String title=switch(t.metric()) {case SLEEP_MINUTES->"Sleep";case RESTING_HR->"Resting heart rate";case HRV_RMSSD->"HRV";case STEPS->"General activity";};
        String value="Not available";
        if(t.recent()!=null) value=switch(t.metric()) {
            case SLEEP_MINUTES->{long minutes=Math.round(t.recent());yield minutes/60+"h "+minutes%60+"m";}
            case RESTING_HR->Math.round(t.recent())+" bpm";
            case HRV_RMSSD->String.format(Locale.UK,"%.0f ms RMSSD",t.recent());
            case STEPS->String.format(Locale.UK,"%,d steps/day",Math.round(t.recent()));
        };
        String context=t.recentDays()<4||t.referenceDays()<14||t.baseline()==null?"Insufficient comparable data":
            t.interpretation().startsWith("Within")?"Around your usual range":
            t.interpretation().startsWith("Above")?"Above your recent baseline":
            t.interpretation().startsWith("Below")?"Below your usual range":"Insufficient comparable data";
        return new HealthContextSummary(title,value,context);
    }
}
