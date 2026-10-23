import java.nio.file.*;
import java.util.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.support.RunningRoutines;

/** Real catalogue/guide/resource completeness gate, run after scripts/test-domain.sh. */
class ExerciseGuidanceAudit {
    public static void main(String[] args) throws Exception {
        boolean report=Arrays.asList(args).contains("--report");
        Path root=Path.of("").toAbsolutePath();var missing=new ArrayList<String>();int complete=0;
        for(var definition:ExerciseCatalog.definitions()) {
            String id=definition.exercise().id();var guide=TechniqueGuide.forExercise(id);var art=TechniqueIllustrations.forExercise(id);
            if(guide==null||art==null){missing.add(id);continue;}
            if(guide.setup().trim().isEmpty()||guide.movement().trim().isEmpty()||guide.cues().isEmpty()||guide.mistakes().isEmpty()||guide.muscles().trim().isEmpty())throw new IllegalStateException("Incomplete text: "+id);
            check(root,art);complete++;
        }
        var routineMissing=new ArrayList<String>();
        for(var movement:RunningRoutines.MOVEMENTS) {var art=TechniqueIllustrations.forRoutine(movement.id());if(art==null)routineMissing.add(movement.id());else check(root,art);}
        System.out.println("Accepted canonical visual guides: "+complete+"/"+ExerciseCatalog.definitions().size());
        System.out.println("Missing canonical IDs: "+String.join(", ",missing));
        System.out.println("Missing routine art: "+String.join(", ",routineMissing));
        if(!report&&(!missing.isEmpty()||!routineMissing.isEmpty()))throw new IllegalStateException("Visual coverage incomplete. Report mode is for development only; releases require every canonical exercise and routine.");
    }
    static void check(Path root,TechniqueIllustrations.Artwork art) throws Exception {
        Path file=root.resolve("app/src/main/res/drawable-nodpi/"+art.asset()+".webp");
        if(art.description().trim().isEmpty()||art.phases().stream().anyMatch(String::isBlank)||!Files.isRegularFile(file)||Files.size(file)<1024||art.asset().matches(".*(placeholder|demo|schematic|fallback).*"))throw new IllegalStateException("Missing real instructional asset: "+art.asset());
    }
}
