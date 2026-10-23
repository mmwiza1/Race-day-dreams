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
        case "glute-bridge" -> new Artwork("guide_glute_bridge","Bodyweight glute bridge: begin lying on a mat with bent knees and flat feet; then lift the hips while the shoulders remain supported, keeping ribs controlled.",Arrays.asList("Feet flat; knees bent","Lift hips under control"));
        case "bench-press" -> new Artwork("guide_bench_press","Bench press: lie supported with feet planted, lower the bar over the chest with bent elbows, then press above the chest with arms extended.",Arrays.asList("Controlled lowering","Press above the chest"));
        case "walking-lunge" -> new Artwork("guide_walking_lunge","Walking lunge: begin standing; step forward and bend both knees with the front foot flat, torso upright and back knee lowered under control.",Arrays.asList("Stand tall","Step and lower"));
        default -> null;
    };}
    public static Artwork forRoutine(String id) {return switch(id) {
        case "lunge" -> forExercise("walking-lunge");
        case "bridge" -> forExercise("glute-bridge");
        case "swing" -> new Artwork("guide_leg_swings","Supported leg swings: hold a stable rail, move one leg gently forward and back while the other foot stays planted and trunk stays upright.",Arrays.asList("Swing gently forward","Return gently behind"));
        case "walk","jog" -> new Artwork("guide_walk","Easy walking option: use a comfortable stride, upright relaxed posture and natural arm movement. An easy jog is optional if comfortable.",Arrays.asList("Walk comfortably"));
        case "march" -> new Artwork("guide_march","Controlled marching: stand tall, then lift one knee comfortably with the opposite arm; lower and alternate sides without leaning back.",Arrays.asList("Stand tall","Lift; lower and alternate"));
        case "calf" -> new Artwork("guide_calf_raise","Supported calf raises: hold a stable rail, start with heels down, then lift both heels gently onto the forefoot and lower under control.",Arrays.asList("Heels down","Rise and lower slowly"));
        case "hip" -> new Artwork("guide_hip_flexor","Hip-flexor stretch: half-kneel on a mat with the front foot flat, keep the trunk upright and gently move the pelvis forward without arching the back.",Arrays.asList("Half-kneel; gentle forward shift"));
        case "quad" -> new Artwork("guide_quad_stretch","Supported quad stretch: hold a stable rail, bend the other knee and gently support the ankle behind you, keeping balance and avoiding force.",Arrays.asList("Stand supported; hold gently"));
        case "glute" -> new Artwork("guide_seated_glute","Seated glute stretch: sit on a stable chair with one foot flat, rest the other ankle across the opposite thigh and hinge forward gently without forcing the knee down.",Arrays.asList("Sit securely; gentle stretch"));
        case "stride" -> new Artwork("guide_strides","Controlled strides: build gradually into relaxed brisk running with smooth steps and arm movement, then ease down and walk to recover. Not maximal sprinting.",Arrays.asList("Build smoothly","Relax and ease down"));
        case "circles" -> new Artwork("guide_ankle_circles","Ankle circles: hold a stable rail, lift one foot slightly and rotate the ankle slowly while the supporting leg and trunk stay steady.",Arrays.asList("Supported gentle ankle rotation"));
        case "calf-stretch" -> new Artwork("guide_calf_stretch","Calf stretch: hold a stable wall, front knee softly bent and back leg comfortable with the whole back foot including heel down; lean gently without bouncing.",Arrays.asList("Back heel down; gentle stretch"));
        case "ankle" -> new Artwork("guide_ankle_rocks","Ankle rocks: half-kneeling with the front foot flat, move the front knee gently forward over the toes while keeping the heel down.",Arrays.asList("Half-kneeling","Rock gently; heel down"));
        default -> null;
    };}
}
