package org.racedaydreams.domain.strength;

import java.util.HashSet;
import java.util.List;

/** Content/identity regressions. Full asset coverage is separately enforced by ExerciseGuidanceAudit. */
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
            if (art == null) continue;
            var guide = TechniqueGuide.forExercise(id);
            check(guide != null, "Illustration must have corresponding movement copy: " + id);
            check(!guide.setup().isBlank() && !guide.movement().isBlank(), "Setup and movement: " + id);
            check(!guide.cues().isEmpty() && !guide.mistakes().isEmpty(), "Cues and mistakes: " + id);
            check(art.description().length() > 60, "Meaningful nonvisual instructions: " + id);
            check(art.phases().stream().noneMatch(String::isBlank), "Frame captions: " + id);
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
        System.out.println("TechniqueGuidanceTestSuite: " + checks + " checks passed.");
    }
}
