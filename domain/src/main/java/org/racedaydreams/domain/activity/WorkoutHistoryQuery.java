package org.racedaydreams.domain.activity;
import java.time.*;
import java.util.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Bounded result projection shared by real history filters; no imaginary scheduled completions. */
public final class WorkoutHistoryQuery {
    private WorkoutHistoryQuery(){}
    public enum Kind { ALL, RUNNING, STRENGTH }
    public record Entry(String id,Kind kind,Instant at,LocalDate date,String label,ActivityRecord run,StrengthSession strength){}
    public record Page(List<Entry> entries,int total,boolean hasMore){}
    public static Page page(List<ActivityRecord> runs,List<StrengthSession> strength,List<PlannedWorkout> prescriptions,Kind kind,
            WorkoutCategory type,String exerciseQuery,LocalDate from,LocalDate through,int offset,int limit){
        if(offset<0||limit<1||limit>100||from!=null&&through!=null&&through.isBefore(from))throw new IllegalArgumentException("Check history range and page size.");
        var types=new HashMap<String,WorkoutCategory>();for(var p:prescriptions)types.put(p.id(),p.category());String query=exerciseQuery==null?"":exerciseQuery.trim().toLowerCase(Locale.ROOT);var entries=new ArrayList<Entry>();
        if(kind!=Kind.STRENGTH&&query.isEmpty())for(var run:runs){var date=run.start().atOffset(run.startOffset()==null?ZoneOffset.UTC:run.startOffset()).toLocalDate();var category=types.get(run.plannedWorkoutId());if(inRange(date,from,through)&&(type==null||category==type))entries.add(new Entry(run.id(),Kind.RUNNING,run.start(),date,category==null?"Run":category.name().toLowerCase(Locale.ROOT).replace('_',' '),run,null));}
        if(kind!=Kind.RUNNING&&type==null)for(var session:strength){var date=session.startedAt().atOffset(session.offset()).toLocalDate();boolean matches=query.isEmpty();if(!matches)for(var block:session.exercises())if(!ExerciseSearch.search(Immutable.list(block.exercise()),query,Immutable.set(),Immutable.list(),Immutable.set()).isEmpty()){matches=true;break;}if(matches&&inRange(date,from,through))entries.add(new Entry(session.id(),Kind.STRENGTH,session.startedAt(),date,"Strength",null,session));}
        entries.sort(Comparator.comparing(Entry::at).reversed().thenComparing(Entry::id));int start=Math.min(offset,entries.size()),end=Math.min(start+limit,entries.size());return new Page(Immutable.copyList(entries.subList(start,end)),entries.size(),end<entries.size());
    }
    private static boolean inRange(LocalDate d,LocalDate from,LocalDate through){return (from==null||!d.isBefore(from))&&(through==null||!d.isAfter(through));}
}
