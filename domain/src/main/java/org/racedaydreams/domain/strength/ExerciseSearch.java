package org.racedaydreams.domain.strength;
import java.util.*;
import org.racedaydreams.domain.coaching.Immutable;
/** Relevance first; favourites and recent use break ties without burying an exact match. */
public final class ExerciseSearch {
    private ExerciseSearch() { }
    public static List<StrengthExercise> search(List<StrengthExercise> exercises,String query,Set<String> favourites,List<String> recent,Set<String> archived) {
        String q=normal(query);var out=new ArrayList<StrengthExercise>();
        for(var e:exercises)if(!archived.contains(e.id()) && rank(e,q)<6)out.add(e);
        out.sort(Comparator.comparingInt((StrengthExercise e)->rank(e,q)).thenComparingInt(e->favourites.contains(e.id())?0:1)
            .thenComparingInt(e->{int i=recent.indexOf(e.id());return i<0?Integer.MAX_VALUE:i;}).thenComparingInt(e->e.estimateOneRepMax()?0:1).thenComparing(StrengthExercise::name).thenComparing(StrengthExercise::id));
        return Immutable.copyList(out);
    }
    private static String normal(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replace('’','\'');}
    private static int rank(StrengthExercise e,String q){String n=normal(e.name());if(q.isEmpty()||n.equals(q))return 0;if(n.startsWith(q))return 1;
        for(String word:n.split("[\\s-]+"))if(word.startsWith(q))return 2;if(n.contains(q))return 3;
        var d=ExerciseCatalog.definition(e.id());if(d!=null){for(String a:d.aliases())if(normal(a).contains(q))return 4;if(normal(d.category()).contains(q))return 5;}return 6;}
}
