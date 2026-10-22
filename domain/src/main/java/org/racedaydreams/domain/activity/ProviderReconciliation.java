package org.racedaydreams.domain.activity;
import java.time.Duration;
import java.util.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Conservative cross-provider matching. Raw records remain retained; canonical choices carry both identities. */
public final class ProviderReconciliation {
    private ProviderReconciliation() { }
    public enum Preference { AUTOMATIC, SAMSUNG_EXPERIMENTAL, HEALTH_CONNECT }
    public record Canonical(ActivityRecord activity,List<SourceIdentity> sources) { }
    public static boolean sameWorkout(ActivityRecord a,ActivityRecord b) {
        if(a.source().provider().equals(b.source().provider()))return false;
        if(!samsung(a.source())||!samsung(b.source()))return false;
        if(Math.abs(Duration.between(a.start(),b.start()).getSeconds())>30||Math.abs(Duration.between(a.end(),b.end()).getSeconds())>60)return false;
        if(a.distanceMetres()==null||b.distanceMetres()==null)return false;
        return Math.abs(a.distanceMetres()-b.distanceMetres())<=Math.max(50,Math.min(a.distanceMetres(),b.distanceMetres())*.01);
    }
    private static boolean samsung(SourceIdentity s){return "samsung-health".equals(s.provider())||"com.sec.android.app.shealth".equals(s.originPackage());}
    public static List<Canonical> reconcile(List<ActivityRecord> records,Preference preference){
        var sorted=new ArrayList<>(records);sorted.sort(Comparator.comparing(ActivityRecord::start).thenComparing(ActivityRecord::id));
        var out=new ArrayList<Canonical>();Set<String> used=new HashSet<>();
        for(int i=0;i<sorted.size();i++){var a=sorted.get(i);if(!used.add(a.id()))continue;var candidates=new ArrayList<ActivityRecord>();
            for(int j=i+1;j<sorted.size()&&Duration.between(a.start(),sorted.get(j).start()).getSeconds()<=30;j++)if(!used.contains(sorted.get(j).id())&&sameWorkout(a,sorted.get(j)))candidates.add(sorted.get(j));
            // Ambiguous clusters are deliberately not merged.
            if(candidates.size()!=1){out.add(new Canonical(a,Immutable.list(a.source())));continue;}
            var b=candidates.get(0);long matches=0;int index=Collections.binarySearch(sorted,b,Comparator.comparing(ActivityRecord::start).thenComparing(ActivityRecord::id));
            for(int k=index-1;k>=0&&Duration.between(sorted.get(k).start(),b.start()).getSeconds()<=30;k--)if(sameWorkout(b,sorted.get(k)))matches++;
            for(int k=index+1;k<sorted.size()&&Duration.between(b.start(),sorted.get(k).start()).getSeconds()<=30;k++)if(sameWorkout(b,sorted.get(k)))matches++;
            if(matches!=1){out.add(new Canonical(a,Immutable.list(a.source())));continue;}
            used.add(b.id());ActivityRecord best;
            if(preference==Preference.HEALTH_CONNECT)best="health-connect".equals(a.source().provider())?a:b;
            else if(preference==Preference.SAMSUNG_EXPERIMENTAL)best="samsung-health".equals(a.source().provider())?a:b;
            else best=quality(b)>quality(a)?b:a;
            out.add(new Canonical(best,Immutable.list(a.source(),b.source())));
        }return Immutable.copyList(out);
    }
    private static int quality(ActivityRecord a){var s=new SampleSeries(a);return s.quality==SampleQuality.QUALIFIED?100000+Math.min(a.samples().size(),500000):a.laps().size();}
    public static String friendly(SourceIdentity s){if(samsung(s))return "Samsung Health";if("com.fitbit.FitbitMobile".equals(s.originPackage()))return "Fitbit";return "health-connect".equals(s.provider())?"Health Connect":"manual".equals(s.provider())?"Manual entry":"Activity provider";}
}
