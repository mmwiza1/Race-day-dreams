package org.racedaydreams.domain.strength;
import java.time.*;
import java.util.*;
/** Presentation projection; retains provenance without inventing chronology for unknown dates. */
public final class StrengthProgression {
    private StrengthProgression() { }
    public record Point(String id,String exerciseId,String category,double value,double kilograms,int reps,LocalDate date,
            HistoricalStrengthRecord.DatePrecision precision,String provenance,String sessionId,String notes) { }
    public static List<Point> timeline(List<StrengthSession> sessions,List<HistoricalStrengthRecord> manual) {
        var out=new ArrayList<Point>();var byId=new HashMap<String,StrengthSession>();for(var s:sessions)byId.put(s.id(),s);
        for(var r:StrengthAnalytics.progression(sessions)){
            var session=Objects.requireNonNull(byId.get(r.sessionId()),"PB source session");
            out.add(new Point(r.setId()+r.category(),r.exerciseId(),r.category(),r.value(),r.kilograms(),r.reps(),r.at().atOffset(session.offset()).toLocalDate(),HistoricalStrengthRecord.DatePrecision.EXACT,"Recorded workout",r.sessionId(),r.notes()));
        }
        for(var r:manual){var mode=r.exercise().mode();String prefix=mode==StrengthExercise.Mode.WEIGHTED_BODYWEIGHT?"Added load":"Load";
            if(mode==StrengthExercise.Mode.BODYWEIGHT)add(out,r,"Bodyweight reps",r.reps());
            else {add(out,r,prefix,r.kilograms());add(out,r,prefix+" × "+r.reps(),r.kilograms());
                Double estimate=StrengthAnalytics.estimatedOneRepMax(r.exercise(),new StrengthSession.SetEntry(r.id(),r.reps(),r.kilograms(),null,false));if(estimate!=null)add(out,r,"Estimated 1RM",estimate);}
        }
        out.sort(Comparator.comparing(Point::date,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Point::id));return Collections.unmodifiableList(out);
    }
    private static void add(List<Point> list,HistoricalStrengthRecord r,String category,double value){list.add(new Point(r.id(),r.exercise().id(),category,value,r.kilograms(),r.reps(),r.date(),r.precision(),"Historical/manual PB",null,r.notes()));}
    public static Point best(List<Point> timeline,String exerciseId,String category){return timeline.stream().filter(p->p.exerciseId().equals(exerciseId)&&p.category().equals(category)).max(Comparator.comparingDouble(Point::value)).orElse(null);}
}
