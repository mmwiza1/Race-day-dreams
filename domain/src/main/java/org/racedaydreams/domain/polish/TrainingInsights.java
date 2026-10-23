package org.racedaydreams.domain.polish;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.activity.*;
/** Four-week windows of retained observations. Absence of a record is not a missed session. */
public final class TrainingInsights {
    public record Insight(String title,String evidence){}
    /** A review prompt, not a diagnosis or automatic adjustment. Tiny extra runs stay neutral. */
    public static boolean extraRunDemand(ActivityRecord run){return run.distanceMetres()!=null&&run.distanceMetres()>=8000||run.rpe()!=null&&run.rpe()>=7||run.movingTime()!=null&&run.movingTime().compareTo(Duration.ofMinutes(45))>=0;}
    public static List<Insight> from(List<ActivityRecord> runs,Instant now){
        var recent=new ArrayList<ActivityRecord>();var prior=new ArrayList<ActivityRecord>();
        for(var r:runs)if(!r.start().isAfter(now)&&r.distanceMetres()!=null){if(!r.start().isBefore(now.minus(Duration.ofDays(28))))recent.add(r);else if(!r.start().isBefore(now.minus(Duration.ofDays(56))))prior.add(r);}
        var out=new ArrayList<Insight>();if(recent.size()<4||prior.size()<4)return out;
        double a=recent.stream().mapToDouble(ActivityRecord::distanceMetres).sum()/1000,b=prior.stream().mapToDouble(ActivityRecord::distanceMetres).sum()/1000;
        out.add(new Insight(Math.abs(a-b)<Math.max(2,b*.05)?"Recorded running volume is broadly stable":a>b?"Recorded running volume increased":"Recorded running volume decreased",String.format(Locale.UK,"Recent 28 days: %.1f km across %d runs. Previous 28 days: %.1f km across %d runs. Recording coverage can differ; this is not an injury-risk assessment.",a,recent.size(),b,prior.size())));
        long high=recent.stream().filter(r->r.rpe()!=null&&r.rpe()>=8).count();if(high>=3)out.add(new Insight("Several high-effort runs recorded",high+" runs at reported RPE 8–10 in the last 28 days. Purpose and your own response matter; no automatic change is made."));
        return Collections.unmodifiableList(out);
    }
    public static List<String> shoes(Shoe shoe,List<ActivityRecord> runs,Instant now,Set<String> discomfort){
        int allLong=0,shoeLong=0,recent=0,noted=0;double longest=0;for(var r:runs){if(r.distanceMetres()!=null&&r.distanceMetres()>=12000){allLong++;if(shoe.id().equals(r.shoeId()))shoeLong++;}if(shoe.id().equals(r.shoeId())){if(discomfort.contains(r.id()))noted++;if(!r.start().isBefore(now.minus(Duration.ofDays(28)))&&!r.start().isAfter(now)){recent++;if(r.distanceMetres()!=null)longest=Math.max(longest,r.distanceMetres());}}}
        var out=new ArrayList<String>();out.add(recent+" assigned runs in the last 28 days.");if(longest>0)out.add(String.format(Locale.UK,"Longest recent assigned run: %.1f km.",longest/1000));if(allLong>0)out.add(shoeLong+" of "+allLong+" recorded runs of at least 12 km used this shoe. The 12 km grouping is a display heuristic.");if(noted>0)out.add("You noted discomfort on "+noted+" run"+(noted==1?"":"s")+" while wearing this shoe. This does not establish a cause.");return out;
    }
}
