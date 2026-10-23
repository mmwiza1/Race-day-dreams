package org.racedaydreams.domain.strength;
import java.util.*;

/** Curated original artwork identifiers, independent of Android resources. No generic fallback. */
public final class TechniqueIllustrations {
    private TechniqueIllustrations() {}
    public record Artwork(String asset,String description,List<String> phases) {
        public Artwork {
            if(asset==null||!asset.matches("guide_[a-z_]+")||description==null||description.trim().isEmpty()||phases==null||phases.isEmpty())throw new IllegalArgumentException("Incomplete instructional art");
            phases=Collections.unmodifiableList(new ArrayList<>(phases));
        }
    }
    public static Artwork forExercise(String id) {return switch(id) {
        case "romanian-deadlift" -> new Artwork("guide_romanian_deadlift","Romanian deadlift: standing with the bar close to the thighs; then hips move backwards with softly bent knees, a neutral back and the bar close to the shins.",Arrays.asList("Stand tall","Hinge at the hips"));
        case "bench-press" -> new Artwork("guide_bench_press","Bench press: lie supported with feet planted, lower the bar over the chest with bent elbows, then press above the chest with arms extended.",Arrays.asList("Controlled lowering","Press above the chest"));
        case "walking-lunge" -> new Artwork("guide_walking_lunge","Walking lunge: begin standing; step forward and bend both knees with the front foot flat, torso upright and back knee lowered under control.",Arrays.asList("Stand tall","Step and lower"));
        default -> null;
    };}
    public static Artwork forRoutine(String id) {return switch(id) {
        case "lunge" -> forExercise("walking-lunge");
        case "ankle" -> new Artwork("guide_ankle_rocks","Ankle rocks: half-kneeling with the front foot flat, move the front knee gently forward over the toes while keeping the heel down.",Arrays.asList("Half-kneeling","Rock gently; heel down"));
        default -> null;
    };}
}
