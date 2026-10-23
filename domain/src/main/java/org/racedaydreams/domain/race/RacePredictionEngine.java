package org.racedaydreams.domain.race;

import java.time.*;
import java.util.*;
import org.racedaydreams.domain.activity.*;

/** Original conservative product heuristic, not a validated individual race model.
 * Multiple independent recent results and endurance coverage are required. */
public final class RacePredictionEngine {
    public record Prediction(Long lowSeconds,Long highSeconds,String confidence,List<String> reasons) {}
    public static Prediction predict(double metres,List<RecordedResult> results,List<ActivityRecord> runs,LocalDate today){
        if(!Double.isFinite(metres)||metres<1000||metres>100000)throw new IllegalArgumentException("Invalid race distance");
        var reasons=new ArrayList<String>();var estimates=new ArrayList<Double>();var sources=new HashSet<String>();int authoritative=0;
        for(var r:results.stream().sorted(Comparator.comparing(RecordedResult::date).reversed().thenComparing(RecordedResult::id)).collect(java.util.stream.Collectors.toList())){
            long days=java.time.temporal.ChronoUnit.DAYS.between(r.date(),today);if(days<0||days>120||r.distance().metres()<Math.min(5000,metres*.4))continue;
            String key=r.activityId()==null?r.date()+":"+r.distance().code()+":"+r.duration():r.activityId();if(!sources.add(key))continue;
            if(r.source()==ResultSource.ROLLING&&r.distance().metres()<metres*.5)continue;
            // Conventional distance scaling is only a starting signal, gated by actual endurance.
            estimates.add(r.duration().toMillis()/1000.0*Math.pow(metres/r.distance().metres(),1.07)*(1+days/120.0*.02));
            if(r.source()!=ResultSource.ROLLING)authoritative++;
            reasons.add(r.distance().label()+" result from "+days+" days ago ("+r.source().name().toLowerCase().replace('_',' ')+").");
            if(estimates.size()==8)break;
        }
        double total=0,longest=0;Set<LocalDate> weeks=new HashSet<>();
        for(var a:runs){LocalDate d=a.start().atOffset(a.startOffset()==null?ZoneOffset.UTC:a.startOffset()).toLocalDate();if(d.isAfter(today)||d.isBefore(today.minusDays(27))||a.distanceMetres()==null)continue;total+=a.distanceMetres();longest=Math.max(longest,a.distanceMetres());weeks.add(d.minusDays(d.getDayOfWeek().getValue()-1));}
        reasons.add(String.format(Locale.UK,"Recent weekly average %.1f km; longest %.1f km. Endurance coverage is separate from short-distance speed.",total/4000,longest/1000));
        boolean endurance=longest>=metres*(metres>=40000?.6:metres>=20000?.6:.5)&&total/4>=metres*(metres>=40000?.7:.6);
        if(estimates.size()<2||metres>=20000&&!endurance)return new Prediction(null,null,"Insufficient evidence",Collections.unmodifiableList(reasons));
        Collections.sort(estimates);double median=estimates.get(estimates.size()/2),spread=(estimates.get(estimates.size()-1)-estimates.get(0))/median;
        boolean strong=estimates.size()>=3&&authoritative>=2&&weeks.size()>=4&&endurance&&spread<.08;
        double width=strong?.035:endurance?.065:.12;double penalty=endurance?1:1.05;
        if(spread>.1){width=Math.max(width,Math.min(.25,spread));strong=false;reasons.add("Conflicting performances widen the range.");}
        reasons.add("Distance scaling, ageing and range widths are conservative product heuristics. Terrain, weather and race-day response remain unknown. Your goal is unchanged.");
        // Rounded minutes communicate uncertainty, not exact predicted seconds.
        return new Prediction((long)Math.floor(median*penalty*(1-width)/60)*60,(long)Math.ceil(median*penalty*(1+width)/60)*60,strong?"Strong":endurance?"Moderate":"Limited",Collections.unmodifiableList(reasons));
    }
}
