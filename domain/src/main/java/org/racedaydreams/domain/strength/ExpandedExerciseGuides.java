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
        add(
                out,
                "bear-plank",
                "plank",
                "Begin on hands and knees with hands under shoulders and knees under hips. Tuck"
                        + " your toes and keep your back comfortably level.",
                "Lift both knees just clear of the mat and hold while breathing. Keep knees bent"
                    + " and hips near shoulder height; lower the knees before trunk control fades."
                    + " Record the hold duration, not repetitions.",
                "Trunk, with shoulder and thigh support",
                "Keep the knees close to the mat|Press through stable hands and toes|Breathe during"
                        + " the hold",
                "Lifting the hips into a pike|Letting the lower back sag|Holding your breath",
                "Plank|Dead Bug",
                "Knees hover; hold and breathe");
        add(
                out,
                "scapular-push-up",
                "scapular-control",
                "Set up in a high plank with hands beneath your shoulders and legs extended. Keep"
                        + " elbows straight and your head, trunk and hips aligned.",
                "Let your chest settle a small distance between your shoulders, then push the floor"
                    + " away to spread your shoulder blades. Keep the elbows straight throughout;"
                    + " this is shoulder-blade movement rather than a full push-up.",
                "Muscles controlling the shoulder blades, with trunk support",
                "Keep the elbows straight|Use a small shoulder-blade movement|Keep hips and trunk"
                        + " steady",
                "Bending elbows into an ordinary push-up|Arching the lower back|Rounding the whole"
                        + " spine to exaggerate the movement",
                "Wall Push-Up|Push-Up",
                "Straight arms; chest settles slightly",
                "Push floor away; spread shoulder blades");
        add(
                out,
                "copenhagen-plank-short-lever",
                "plank",
                "Lie on your side beside a secure padded bench. Place the inside of your upper knee"
                    + " on its seat with that knee bent. Support your lower forearm on a mat, elbow"
                    + " beneath the shoulder.",
                "Lift your hips into a controlled side-plank line while the upper knee stays"
                    + " supported and the lower leg clears the floor. Hold while breathing, then"
                    + " lower deliberately and change sides. Choose an ordinary side plank if this"
                    + " setup is too demanding.",
                "Inner-thigh and trunk muscles, with shoulder support",
                "Keep the supported knee padded|Keep the whole forearm grounded|Begin with a short"
                        + " manageable hold",
                "Supporting on a fist instead of your forearm|Letting the bench slide|Twisting or"
                        + " dropping the hips",
                "Side Plank|Copenhagen Plank (Long Lever)",
                "Upper knee supported; hold with forearm grounded");
        add(
                out,
                "copenhagen-plank-long-lever",
                "plank",
                "Lie on your side beside a secure padded bench. Place the inside of your upper"
                        + " ankle on the seat with that leg straight. Ground the lower forearm on a"
                        + " mat, elbow beneath the shoulder.",
                "Lift into a side-plank line through the straight supported leg and trunk, with the"
                    + " lower leg clear of the floor. Breathe and lower before control fades. Use"
                    + " the knee-supported version when the longer lever is too demanding; change"
                    + " sides as prescribed.",
                "Inner-thigh and trunk muscles, with shoulder support",
                "Keep the upper ankle supported|Keep the whole forearm grounded|Use a short"
                        + " controlled hold",
                "Losing secure ankle contact|Holding after hips start sagging|Starting with a long"
                        + " hold beyond your current control",
                "Copenhagen Plank (Short Lever)|Side Plank",
                "Upper ankle supported; hold with forearm grounded");
        add(
                out,
                "dead-bug-heel-tap",
                "core",
                "Lie on your back with head supported, arms reaching upwards and both hips and"
                        + " knees comfortably bent to a tabletop position. Keep your pelvis still.",
                "Lower one bent leg until the heel gently touches the mat, then return to tabletop"
                    + " and alternate sides. Keep your arms still and use a shorter lowering range"
                    + " if your back starts arching.",
                "Trunk stabilisers with hip-flexor involvement",
                "Keep the knee bent|Keep your pelvis still|Move slowly while breathing",
                "Straightening the moving leg unnecessarily|Arching the lower back|Pressing"
                        + " forcefully into the floor with the tapping foot",
                "Dead Bug|Bird Dog",
                "Both legs in tabletop; arms still",
                "Tap one heel; return and alternate");
        add(
                out,
                "single-leg-glute-bridge",
                "bridge",
                "Lie on your back with one foot planted near your hips and the other leg lifted"
                        + " clear. Rest your arms alongside you and keep both sides of your pelvis"
                        + " level.",
                "Press through the planted foot to lift your hips into a controlled"
                        + " shoulder-to-knee line, then lower slowly. Keep the other leg lifted and"
                        + " stop before your lower back arches; change sides as prescribed.",
                "Glutes with hamstring and trunk involvement",
                "Keep the working foot stable|Keep both hips level|Lift from the hip rather than"
                        + " the lower back",
                "Twisting the pelvis|Pushing through the lifted leg|Overarching at the top",
                "Glute Bridge|Hip Thrust",
                "One foot planted; other leg lifted",
                "Raise hips level; lower smoothly");
        add(
                out,
                "standing-pallof-hold",
                "core",
                "Stand side-on to a chest-height cable and hold the handle with both hands. Take a"
                    + " stable stance and reach your hands straight ahead without turning towards"
                    + " the pulley.",
                "Hold the arms extended while keeping ribs and pelvis facing forwards. Breathe,"
                    + " return the handle to your chest to finish and change sides as prescribed."
                    + " Record the actual cable load and hold duration, not repetitions.",
                "Trunk stabilisers with shoulder support",
                "Keep hips and ribs facing forwards|Use a manageable cable load|Breathe during the"
                        + " hold",
                "Turning towards the pulley|Leaning sideways|Continuing after trunk control fades",
                "Side Plank|Pallof Press",
                "Hold hands forward; resist rotation");
        add(
                out,
                "overhead-dumbbell-carry",
                "carry",
                "With clear overhead space and a clear walking path, bring two manageable dumbbells"
                        + " to a stable overhead position. Keep wrists above shoulders and ribs"
                        + " comfortably over your pelvis.",
                "Walk with small controlled steps while keeping both weights steady overhead, then"
                    + " lower them deliberately. End before arm or trunk control fades. Record"
                    + " distance and total load across both dumbbells; these loads do not transfer"
                    + " from another carry.",
                "Shoulders and trunk, with grip and upper-back involvement",
                "Start with manageable overhead loads|Keep breathing and ribs controlled|Keep both"
                        + " weights steady",
                "Leaning backwards to hold the weights|Walking beneath obstacles|Continuing when"
                        + " arms or grip become unstable",
                "Farmer’s Carry|Front Rack Carry",
                "Stable two-dumbbell overhead support",
                "Walk steadily; lower deliberately");
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
