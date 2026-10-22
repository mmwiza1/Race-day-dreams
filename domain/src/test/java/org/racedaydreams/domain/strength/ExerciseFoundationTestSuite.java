package org.racedaydreams.domain.strength;
import java.util.*;
import java.time.*;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.body.MassUnit;
public final class ExerciseFoundationTestSuite {
    private static int checks;
    private static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static void rejects(Runnable r){try{r.run();throw new AssertionError("Expected validation");}catch(IllegalArgumentException e){checks++;}}
    private static List<StrengthExercise> search(String q){return ExerciseSearch.search(StrengthExercise.starters(),q,Set.of(),List.of(),Set.of());}
    private static StrengthExercise e(String id){return ExerciseCatalog.definition(id).exercise();}
    public static void main(String[] args){
        ok(StrengthExercise.starters().size()>=150,"Comprehensive catalogue");var ids=new HashSet<String>();for(var e:StrengthExercise.starters()){ok(ids.add(e.id()),"Unique stable ID");ok(ExerciseCatalog.definition(e.id())!=null,"Metadata available");}
        ok(search("deadlift").get(0).id().equals("deadlift"),"Exact first");ok(search("de").indexOf(e("deadlift")) < search("de").indexOf(e("romanian-deadlift")),"Name prefixes before word prefixes");
        ok(search("rDl").stream().anyMatch(e->e.id().equals("romanian-deadlift")),"Alias case insensitive");ok(search("military").get(0).id().equals("overhead-press"),"OHP alias");ok(search("hex deadlift").get(0).id().equals("trap-bar-deadlift"),"Hex alias");ok(search("RFESS").get(0).id().equals("split-squat"),"RFESS canonical ID");ok(search("press").stream().anyMatch(e->e.id().equals("bench-press")),"Substring");
        var ranked=ExerciseSearch.search(StrengthExercise.starters(),"",Set.of("romanian-deadlift"),List.of("front-squat"),Set.of("deadlift"));ok(ranked.get(0).id().equals("romanian-deadlift"),"Favourites first when relevance ties");ok(ranked.get(1).id().equals("front-squat"),"Recents second");ok(ranked.stream().noneMatch(e->e.id().equals("deadlift")),"Archived hidden but canonical retained");ok(e("deadlift")!=null,"Archived evidence still readable");
        ok(StrengthMeasurements.seconds("1:30")==90,"Natural time");ok(StrengthMeasurements.seconds("45")==45,"Seconds");rejects(()->StrengthMeasurements.seconds("1:60"));rejects(()->StrengthMeasurements.seconds("-1:20"));rejects(()->StrengthMeasurements.seconds("999999999999999:59"));
        var timed=new StrengthSession.SetEntry("a",0,0,null,false,135L,null);var loaded=new StrengthSession.SetEntry("b",0,20,7.0,false,90L,null);var carry=new StrengthSession.SetEntry("c",0,40,8.0,false,null,60.0);
        new StrengthSession.ExerciseSets(e("plank"),List.of(timed));checks++;new StrengthSession.ExerciseSets(e("weighted-plank"),List.of(loaded));checks++;new StrengthSession.ExerciseSets(e("farmers-carry"),List.of(carry));checks++;
        rejects(()->new StrengthSession.ExerciseSets(e("back-squat"),List.of(timed)));rejects(()->new StrengthSession.SetEntry("bad",1,0,null,false,30L,null));rejects(()->new StrengthSession.SetEntry("bad",0,0,null,false));rejects(()->new StrengthSession.ExerciseSets(e("plank"),List.of(loaded)));
        ok(StrengthMeasurements.display(timed,MassUnit.KG,UnitSystem.METRIC).equals("2 min 15 sec"),"No fake reps");ok(StrengthMeasurements.display(carry,MassUnit.LB,UnitSystem.IMPERIAL).contains("yd"),"Imperial distance");
        ok(StrengthAnalytics.estimatedOneRepMax(e("weighted-plank"),loaded)==null,"No timed e1RM");
        var session=new StrengthSession("s",Instant.parse("2026-01-01T12:00:00Z"),ZoneOffset.UTC,Duration.ofMinutes(30),7.0,"",List.of(new StrengthSession.ExerciseSets(e("plank"),List.of(timed)),new StrengthSession.ExerciseSets(e("weighted-plank"),List.of(loaded)),new StrengthSession.ExerciseSets(e("farmers-carry"),List.of(carry))));
        var records=StrengthAnalytics.progression(List.of(session));ok(records.size()==3,"Timed/distance PB evidence");ok(records.stream().noneMatch(p->p.category().contains("1RM")),"No invalid strength predictions");ok(StrengthAnalytics.progression(List.of()).isEmpty(),"Deletion removes derived PBs");
        System.out.println("ExerciseFoundationTestSuite: "+checks+" checks passed.");
    }
}
