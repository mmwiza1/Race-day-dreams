package org.racedaydreams.domain.strength;

import java.util.HashSet;
import java.util.List;

/** Catalogue-wide content/identity gate. Packaged file integrity also runs in ExerciseGuidanceAudit. */
public final class TechniqueGuidanceTestSuite {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var known = new HashSet<String>();
        for (var definition : ExerciseCatalog.definitions()) {
            String id = definition.exercise().id();
            known.add(id);
            var art = TechniqueIllustrations.forExercise(id);
            check(art != null, "Every canonical exercise requires reviewed illustrated guidance: " + id);
            var guide = TechniqueGuide.forExercise(id);
            check(guide != null, "Illustration must have corresponding movement copy: " + id);
            check(!guide.setup().isBlank() && !guide.movement().isBlank(), "Setup and movement: " + id);
            check(!guide.cues().isEmpty() && !guide.mistakes().isEmpty(), "Cues and mistakes: " + id);
            check(art.description().length() > 60, "Meaningful nonvisual instructions: " + id);
            check(!art.phases().isEmpty() && art.phases().stream().noneMatch(String::isBlank), "Frame captions: " + id);
        }
        for (String id : List.of("front-squat", "leg-press", "reverse-lunge", "step-up", "calf-raise",
                "standing-calf-raise", "side-plank", "farmers-carry", "lying-leg-curl", "kettlebell-swing",
                "dumbbell-shoulder-press", "lat-pulldown")) {
            check(known.contains(id), "Real catalogue ID: " + id);
            check(TechniqueIllustrations.forExercise(id) != null, "Reviewed priority artwork retained: " + id);
        }
        check(TechniqueGuide.forExercise("front-squat").setup().contains("front of your shoulders"), "Front rack differs from back squat");
        check(TechniqueGuide.forExercise("dumbbell-shoulder-press").setup().contains("bench"), "Copy matches seated illustration");
        check(!TechniqueIllustrations.forExercise("calf-raise").asset().equals(TechniqueIllustrations.forRoutine("calf").asset()), "Machine guidance must not use unloaded routine image");
        check(TechniqueIllustrations.forExercise("side-plank").phases().size() == 1, "Static hold needs no fake movement frames");
        check(TechniqueIllustrations.forExercise("custom-unrecognised") == null, "Custom exercise must not inherit an unrelated pose");
        check(TechniqueGuide.forExercise("custom-unrecognised") == null, "Custom copy must remain unknown");
        check(!TechniqueGuide.forExercise("kettlebell-swing").mistakes().contains("Turning the hinge into a squat"), "Swing-specific advice retained");
        check(TechniqueIllustrations.forExercise("kettlebell-swing").description().contains("not an overhead"), "Distinct from American swing");
        for(String id:List.of("low-bar-squat","belt-squat","hack-squat","sumo-deadlift","single-leg-rdl",
                "bodyweight-squat","weighted-side-plank","wall-sit","weighted-wall-sit","cable-crunch",
                "weighted-sit-up","weighted-pull-up","weighted-chin-up","chin-up","neutral-grip-pull-up",
                "pendlay-row","t-bar-row","smith-machine-bench-press","weighted-push-up","pec-deck")) {
            check(known.contains(id),"Expanded guide retains canonical identity: "+id);
            check(TechniqueIllustrations.forExercise(id)!=null,"Reviewed catalogue artwork: "+id);
        }
        check(TechniqueGuide.forExercise("weighted-push-up").setup().contains("vest"),"Weighted push-up uses secured vest rather than a loose plate");
        check(TechniqueGuide.forExercise("sumo-deadlift").setup().contains("inside your knees"),"Sumo stance and grip differ from conventional");
        check(TechniqueGuide.forExercise("low-bar-squat").setup().contains("below the upper traps"),"Low-bar shelf is explicit");
        check(TechniqueGuide.forExercise("cable-crunch").movement().contains("curling the trunk"),"Cable crunch is trunk movement, not an arm pressdown");
        check(TechniqueGuide.forExercise("pendlay-row").movement().contains("reset"),"Floor reset retained for Pendlay row");
        check(!TechniqueIllustrations.forExercise("chin-up").asset().equals(TechniqueIllustrations.forExercise("weighted-chin-up").asset()),"Weighted chin-up shows actual belt equipment");
        for(String id:List.of("wall-sit","weighted-wall-sit","weighted-side-plank"))
            check(TechniqueIllustrations.forExercise(id).phases().size()==1,"Static hold has a single meaningful frame: "+id);
        check(TechniqueIllustrations.forExercise("high-bar-squat").asset().equals(TechniqueIllustrations.forExercise("back-squat").asset()),"Exact high-bar equipment/pose may share art");
        check(TechniqueGuide.forExercise("pause-squat").movement().contains("pause"),"Shared squat pose retains variant-specific timing");
        check(ExerciseCatalog.definition("arnold-press").equipment().equals("dumbbells"),"Arnold press metadata agrees with its actual equipment");
        check(!GymProgrammeGenerator.available("arnold-press",java.util.Set.of("barbell")),"Barbell alone cannot satisfy a dumbbell movement");
        check(GymProgrammeGenerator.available("arnold-press",java.util.Set.of("dumbbells")),"Correct dumbbell equipment remains available");
        check(!GymProgrammeGenerator.available("pallof-press",java.util.Set.of("barbell")),"Barbell is not a substitute for a cable attachment");
        check(GymProgrammeGenerator.available("pallof-press",java.util.Set.of("cables")),"Pallof press retains cable availability");
        check(!GymProgrammeGenerator.available("sandbag-carry",java.util.Set.of("dumbbells")),"A sandbag carry requires the actual implement");
        check(!GymProgrammeGenerator.available("swiss-ball-leg-curl",java.util.Set.of("bodyweight")),"A ball curl requires a ball rather than bodyweight alone");
        System.out.println("TechniqueGuidanceTestSuite: " + checks + " checks passed.");
    }
}
