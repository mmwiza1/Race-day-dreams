package org.racedaydreams.domain.strength;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reviewed original movement copy and art for catalogue additions; never a generic fallback. */
public final class ExpandedExerciseGuides {
    private ExpandedExerciseGuides() {}

    private record Entry(TechniqueGuide guide, TechniqueIllustrations.Artwork artwork) {}

    private static final Map<String, Entry> ENTRIES = build();

    public static Set<String> identities() {
        return ENTRIES.keySet();
    }

    public static TechniqueGuide guide(String id) {
        var entry = ENTRIES.get(id);
        return entry == null ? null : entry.guide();
    }

    public static TechniqueIllustrations.Artwork artwork(String id) {
        var entry = ENTRIES.get(id);
        return entry == null ? null : entry.artwork();
    }

    private static Map<String, Entry> build() {
        var out = new LinkedHashMap<String, Entry>();
        add(
                out,
                "hip-abductor-machine",
                "hip-abduction",
                "Sit against the backrest with feet supported, knees bent and pads against the"
                        + " outside of your thighs. Adjust the starting width to a comfortable"
                        + " position.",
                "Press your knees apart without lifting your pelvis or rocking your trunk, then"
                    + " bring the pads back together slowly. Use the same machine and setup when"
                    + " comparing logged loads.",
                "Glutes and other outer-hip muscles",
                "Keep your back supported|Keep feet on the supports|Move through a comfortable"
                        + " range",
                "Rocking to move the stack|Snapping the pads back together|Forcing a wider range"
                        + " than you can control",
                "Side-Lying Hip Abduction|Side Plank",
                "Pads outside thighs; knees close",
                "Press knees apart; return slowly");
        add(
                out,
                "hip-adductor-machine",
                "hip-adduction",
                "Sit against the backrest with feet supported and knees bent. Place the pads"
                    + " against the inside of your thighs and select a comfortable open starting"
                    + " position.",
                "Bring your knees towards each other without shifting your pelvis, then open the"
                    + " pads slowly. Avoid forcing the starting stretch; compare loads only on the"
                    + " same machine and setup.",
                "Inner-thigh muscles",
                "Adjust the opening before starting|Keep your pelvis supported|Control the return"
                        + " to the open position",
                "Forcing a wide starting stretch|Bouncing the weight stack|Sliding forwards off the"
                        + " backrest",
                "Cossack Squat|Lateral Lunge",
                "Pads inside thighs; start comfortably apart",
                "Bring knees together; controlled return");
        add(
                out,
                "wall-push-up",
                "press",
                "Face a solid wall with hands at shoulder height, slightly wider than your"
                    + " shoulders. Step back to a comfortable distance with secure foot contact and"
                    + " a straight body line.",
                "Bend your elbows to bring your chest towards the wall, then press away smoothly."
                    + " Keep your head, trunk and hips aligned; move closer to the wall to reduce"
                    + " the challenge.",
                "Chest, triceps and front shoulders",
                "Use a solid wall|Keep hips aligned with your trunk|Bend elbows diagonally rather"
                        + " than straight out",
                "Folding at the hips|Moving your face without your chest|Using a slippery wall or"
                        + " floor",
                "Knee Push-Up|Push-Up",
                "Hands on wall; comfortable stance",
                "Bend elbows; press away");
        add(
                out,
                "knee-push-up",
                "press",
                "Place your hands slightly wider than your shoulders on a mat. Support both knees"
                    + " well behind your hips so your head, hips and knees form one inclined line.",
                "Lower your chest by bending the elbows diagonally back, then press up without"
                    + " bending at the waist. Keep your knees supported and finish before your body"
                    + " line loses control.",
                "Chest, triceps and front shoulders with trunk support",
                "Pad your knees|Move chest and hips together|Keep your neck in line with your"
                        + " trunk",
                "Leaving hips directly above knees|Letting the lower back sag|Flaring elbows"
                        + " straight out",
                "Wall Push-Up|Push-Up",
                "Knees supported; body aligned",
                "Lower chest; press up together");
        add(
                out,
                "side-lying-hip-abduction",
                "hip-abduction",
                "Lie on your side with your head supported, legs straight and hips stacked. Rest"
                        + " your upper hand in front of you for balance.",
                "Lift the upper leg a short distance without rolling your pelvis backwards, then"
                    + " lower slowly. Keep the toes facing forwards and repeat on the other side as"
                    + " prescribed.",
                "Glutes and other outer-hip muscles",
                "Keep hips stacked|Use a small controlled lift|Keep your trunk supported",
                "Rolling backwards to lift higher|Turning the toes towards the ceiling|Swinging the"
                        + " leg",
                "Hip Abductor Machine|Side Plank",
                "Legs together; hips stacked",
                "Lift upper leg; lower slowly");
        add(
                out,
                "chair-sit-to-stand",
                "squat",
                "Place a firm chair against a wall or otherwise secure it from sliding. Sit towards"
                        + " the front with feet flat, knees comfortably apart and arms reaching"
                        + " forwards for balance.",
                "Lean your trunk forwards slightly and stand through both feet. Reach your hips"
                    + " back to sit down slowly, keeping the chair behind you. Choose a higher seat"
                    + " or hand support if needed.",
                "Quadriceps and glutes with trunk support",
                "Check the chair is stable|Keep both feet supported|Control the return to the seat",
                "Using a wheeled or unstable chair|Dropping onto the seat|Letting knees collapse"
                        + " inwards",
                "Bodyweight Squat|Goblet Squat",
                "Sit with feet supported",
                "Stand tall; sit down under control");
        add(
                out,
                "cable-hip-abduction",
                "hip-abduction",
                "Stand side-on to a low pulley and hold a stable support. Secure the cuff to the"
                        + " ankle furthest from the tower, with the cable clear of your supporting"
                        + " foot.",
                "Move the cuffed leg sideways away from the tower through a small range, then"
                        + " return slowly. Keep hips level and trunk still; change sides as"
                        + " prescribed.",
                "Glutes and other outer-hip muscles with trunk support",
                "Cuff the far ankle|Keep the cable clear of your support foot|Keep your pelvis"
                        + " level",
                "Leaning sideways to lift higher|Swinging the leg forwards|Using an unsecured ankle"
                        + " cuff",
                "Hip Abductor Machine|Side-Lying Hip Abduction",
                "Far ankle cuffed; hold support",
                "Move leg away; return slowly");
        add(
                out,
                "cable-hip-adduction",
                "hip-adduction",
                "Stand side-on to a low pulley and hold a stable support. Secure the cuff to the"
                        + " ankle nearest the tower; begin with that leg slightly open towards the"
                        + " pulley and the other foot planted.",
                "Draw the cuffed leg inward just across the front of your supporting leg, against"
                    + " the cable pull. Return slowly without rotating your pelvis; change sides as"
                    + " prescribed.",
                "Inner-thigh muscles with trunk support",
                "Cuff the near ankle|Use a small controlled crossing|Keep the supporting foot"
                        + " stable",
                "Cuffing the far leg so the cable assists the motion|Twisting the pelvis|Crossing"
                        + " so far that balance is lost",
                "Hip Adductor Machine|Lateral Lunge",
                "Near ankle cuffed; leg slightly open",
                "Draw leg inward; controlled return");
        add(
                out,
                "cable-glute-kickback",
                "hip-extension",
                "Face a low pulley and hold its stable support. Secure an ankle cuff, soften your"
                    + " supporting knee and incline slightly from the hips with a steady trunk.",
                "Move the cuffed leg a short distance backwards from the hip, then return slowly."
                        + " Keep the knee softly bent and pelvis still; stop before your lower back"
                        + " starts arching.",
                "Glutes, with hamstring and trunk involvement",
                "Keep the cuff secure|Keep the cable clear of the other foot|Use a small movement"
                        + " from the hip",
                "Arching the back to lift higher|Swinging the leg|Twisting the pelvis",
                "Glute Bridge|Hip Thrust",
                "Face pulley; hold support",
                "Extend hip; return without arching");
        add(
                out,
                "seated-dumbbell-calf-raise",
                "calf",
                "Sit on a firm bench with knees bent and forefeet secure on a low stable step. Rest"
                    + " a manageable dumbbell across each thigh, away from the kneecap, and hold"
                    + " the weights securely.",
                "Raise your heels while keeping the knees bent, then lower slowly through a"
                    + " comfortable range. Keep your forefeet supported. Record load consistently"
                    + " as the total across both dumbbells, not a machine-equivalent load.",
                "Calves, with emphasis on the bent-knee task",
                "Secure the dumbbells with your hands|Keep knees bent|Control the lowering",
                "Placing weight directly on the kneecaps|Bouncing the heels|Letting a dumbbell roll"
                        + " off the thigh",
                "Seated Calf Raise|Bodyweight Single-Leg Calf Raise",
                "Weights secure; forefeet on step",
                "Raise heels; lower smoothly");
        return Map.copyOf(out);
    }

    private static void add(
            Map<String, Entry> out,
            String id,
            String pattern,
            String setup,
            String movement,
            String muscles,
            String cues,
            String mistakes,
            String alternatives,
            String... phases) {
        var guide =
                new TechniqueGuide(
                        pattern,
                        setup,
                        movement,
                        List.of(cues.split("\\|")),
                        List.of(mistakes.split("\\|")),
                        muscles,
                        List.of(alternatives.split("\\|")));
        var art =
                new TechniqueIllustrations.Artwork(
                        "guide_" + id.replace('-', '_'), setup + " " + movement, List.of(phases));
        if (out.put(id, new Entry(guide, art)) != null)
            throw new IllegalStateException("Duplicate expanded guidance: " + id);
    }
}
