package org.racedaydreams.domain.coaching;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.activity.*;
import org.racedaydreams.domain.baseline.*;
/** Conservative product heuristics, not validated physiological thresholds. No goal or easy-pace mutation. */
public final class PerformanceReassessment {
    private PerformanceReassessment() { }
    public enum Confidence { STRONG, MODERATE, LIMITED }
    public record Evidence(String identity,double metres,Duration duration,LocalDate date,String source,double authority) { }
    public record Review(boolean propose,Confidence confidence,double fasterFraction,List<String> reasons) { }
    public static Review review(List<RecordedResult> results,List<AssessmentResult> assessments,LocalDate end,
            BaselineConfidence baseline,double completion,int controlledResponses,boolean effortConcern) {
        var evidence=new ArrayList<Evidence>();
        for(var r:results)evidence.add(new Evidence(r.activityId()==null?"result:"+r.id():r.activityId(),r.distance().metres(),r.duration(),r.date(),r.source()==ResultSource.OFFICIAL_CHIP?"Official race":r.source()==ResultSource.ROLLING?"Rolling best effort":"Manual result",r.source()==ResultSource.OFFICIAL_CHIP?3:r.source()==ResultSource.ROLLING?.5:2));
        for(var a:assessments)if(a.assessment().mode()==AssessmentMode.BEST_EFFORT)evidence.add(new Evidence(a.activityId()==null?"assessment:"+a.id():a.activityId(),a.assessment().distanceKm()*1000.0,a.duration(),a.completedAt().atOffset(ZoneOffset.UTC).toLocalDate(),"Deliberate assessment",3));
        // One activity can contain many rolling distances; it is never many independent performances.
        var improvements=new HashMap<String,Double>();var strengths=new HashMap<String,Double>();var reasons=new ArrayList<String>();
        for(var r:evidence){if(r.date().isBefore(end.minusWeeks(4))||!r.date().isBefore(end)||r.metres()<1000)continue;
            var old=evidence.stream().filter(p->Math.abs(p.metres()-r.metres())<1&&p.date().isBefore(r.date())&&!p.date().isBefore(end.minusMonths(6))&&p.authority()>=2).min(Comparator.comparing(Evidence::duration));
            if(old.isEmpty())continue;
            double gain=1-r.duration().toMillis()/(double)old.get().duration().toMillis();
            if(gain<.02)continue;
            double weight=r.authority()*(r.metres()<3000?.65:1)*(r.date().isBefore(end.minusWeeks(2))?.75:1);
            if(gain>improvements.getOrDefault(r.identity(),0.0)) {improvements.put(r.identity(),gain);strengths.put(r.identity(),weight);}
        }
        double gain=improvements.values().stream().mapToDouble(Double::doubleValue).max().orElse(0),authority=strengths.values().stream().mapToDouble(Double::doubleValue).sum();
        boolean corroborated=completion>=.8&&controlledResponses>=3&&!effortConcern;
        boolean credible=authority>=1.5&&(gain>=.05||improvements.size()>=2&&gain>=.025);
        Confidence confidence=!corroborated||!credible||baseline==BaselineConfidence.LOW?Confidence.LIMITED:authority>=3&&baseline==BaselineConfidence.STRONG?Confidence.STRONG:Confidence.MODERATE;
        boolean propose=confidence!=Confidence.LIMITED;
        double fraction=propose?Math.min(.03,gain*.4):0;
        var described=new HashSet<String>();
        for(var e:evidence)if(improvements.containsKey(e.identity())&&described.add(e.identity()))reasons.add(String.format(Locale.UK,"%s over %.1f km: approximately %.1f%% faster than prior comparable evidence.",e.source(),e.metres()/1000,improvements.get(e.identity())*100));
        reasons.add(improvements.size()+" independent recent comparable performance improvements; same-activity efforts count once.");
        reasons.add("Source, distance and recency affect confidence: official results and deliberate best-effort assessments carry more weight than incidental rolling efforts. Manual results are not independently verified.");
        reasons.add("Baseline confidence: "+baseline.name().toLowerCase(Locale.UK)+". Completion: "+Math.round(completion*100)+"%; controlled reported responses: "+controlledResponses+".");
        if(effortConcern)reasons.add("Recent effort concerns argue against faster ranges.");
        reasons.add(propose?"Offer a bounded quality-pace review only. Easy/recovery effort, mileage and race goal stay unchanged.":"Keep current training ranges; more corroborating evidence is needed.");
        return new Review(propose,confidence,fraction,Collections.unmodifiableList(reasons));
    }
}
