package org.racedaydreams.domain.strength;

import java.util.List;

/** Explicit reviewed movement-to-asset mapping. Never selects artwork from a generic pattern. */
final class CatalogueTechniqueIllustrations {
    private CatalogueTechniqueIllustrations() {}
    static TechniqueIllustrations.Artwork forExercise(String id) {
        return switch (id) {
            // These named variants have identical visible equipment and positions; timing/copy differ.
            case "high-bar-squat" -> art(id,"back_squat","Bar on upper traps; brace","Lower with control; stand");
            case "pause-squat" -> art(id,"back_squat","Brace; lower smoothly","Pause while braced; stand");
            case "box-step-up" -> art(id,"step_up","Whole foot on stable box","Stand; step down slowly");
            case "row" -> art(id,"barbell_row","Hinge; trunk steady","Pull towards lower ribs");
            case "box-squat" -> art(id,"box_squat","Stable box behind you","Touch down; stand without rocking");
            case "hack-squat" -> art(id,"hack_squat","Lower carriage under control","Press with back supported");
            case "pendulum-squat" -> art(id,"pendulum_squat","Set stops; steady stance","Lower with the swinging carriage");
            case "zercher-squat" -> art(id,"zercher_squat","Cradle bar in elbow creases","Squat; keep bar close");
            case "smith-machine-squat" -> art(id,"smith_machine_squat","Set guide-rail safeties","Squat; return to hooks");
            case "single-leg-press" -> art(id,"single_leg_press","One foot on platform","Press; keep pelvis supported");
            case "sissy-squat" -> art(id,"sissy_squat","Hold a stable support","Knees forwards; controlled return");
            case "sumo-deadlift" -> art(id,"sumo_deadlift","Wide stance; grip inside knees","Stand tall; lower smoothly");
            case "single-leg-rdl" -> art(id,"single_leg_rdl","Balance; soften working knee","Hinge; free leg reaches back");
            case "good-morning" -> art(id,"good_morning","Bar on upper back; brace","Hinge hips back; return");
            case "low-bar-squat" -> art(id,"low_bar_squat","Bar on rear-shoulder shelf","Hips back; stay braced");
            case "belt-squat" -> art(id,"belt_squat","Secure belt and cable","Squat with centred hip load");
            case "bodyweight-squat" -> art(id,"bodyweight_squat","Stand balanced","Squat; keep feet planted");
            case "reverse-lunge-bodyweight" -> art(id,"reverse_lunge_bodyweight","Stand tall","Step back; lower with control");
            case "single-leg-calf-raise-bodyweight" -> art(id,"single_leg_calf_raise_bodyweight","Forefoot supported; hold rail","Raise heel; lower slowly");
            case "wall-sit" -> art(id,"wall_sit","Back supported; breathe steadily");
            case "weighted-wall-sit" -> art(id,"weighted_wall_sit","Secure plate; hold and breathe");
            case "weighted-side-plank" -> art(id,"weighted_side_plank","Elbow under shoulder; secure plate");
            case "hanging-knee-raise" -> art(id,"hanging_knee_raise","Steady hang","Raise knees; no swinging");
            case "hanging-leg-raise" -> art(id,"hanging_leg_raise","Steady hang","Raise legs; lower slowly");
            case "toes-to-bar" -> art(id,"toes_to_bar","Secure grip; steady hang","Raise toes; controlled return");
            case "russian-twist" -> art(id,"russian_twist","Feet supported; hold ball close","Rotate gently; alternate sides");
            case "weighted-sit-up" -> art(id,"weighted_sit_up","Plate held at chest","Curl up; lower with control");
            case "chin-up" -> art(id,"chin_up","Underhand grip; steady hang","Pull chin above bar; lower");
            case "neutral-grip-pull-up" -> art(id,"neutral_grip_pull_up","Palms face each other","Pull; lower without swinging");
            case "weighted-pull-up" -> art(id,"weighted_pull_up","Overhand grip; secure belt","Pull; keep plate steady");
            case "weighted-chin-up" -> art(id,"weighted_chin_up","Underhand grip; secure belt","Pull; lower smoothly");
            case "inverted-row" -> art(id,"inverted_row","Heels supported; arms extended","Pull chest towards bar");
            case "close-grip-pulldown" -> art(id,"close_grip_pulldown","Thighs secured; neutral grip","Pull towards upper chest");
            case "straight-arm-pulldown" -> art(id,"straight_arm_pulldown","Soft elbows; brace trunk","Sweep arms towards thighs");
            case "face-pull" -> art(id,"face_pull","Rope at face height","Separate hands beside face");
            case "pendlay-row" -> art(id,"pendlay_row","Brace over floor-supported bar","Row; return and reset");
            case "chest-supported-row" -> art(id,"chest_supported_row","Chest supported on bench","Pull towards ribs; lower");
            case "t-bar-row" -> art(id,"t_bar_row","Secure landmine; hinge and brace","Row without heaving");
            case "machine-row" -> art(id,"machine_row","Chest pad supports trunk","Pull handles towards ribs");
            case "close-grip-bench-press" -> art(id,"close_grip_bench_press","Shoulder-width grip; controlled lower","Press above chest");
            case "incline-bench-press" -> art(id,"incline_bench_press","Supported incline; lower smoothly","Press above upper chest");
            case "decline-bench-press" -> art(id,"decline_bench_press","Feet secure; lower smoothly","Press; keep body supported");
            case "decline-dumbbell-press" -> art(id,"decline_dumbbell_press","Feet secured; weights beside chest","Press with control");
            case "machine-chest-press" -> art(id,"machine_chest_press","Back supported; handles at chest","Press; controlled return");
            case "smith-machine-bench-press" -> art(id,"smith_machine_bench_press","Set safety stops; lower smoothly","Press; re-hook deliberately");
            case "dip" -> art(id,"dip","Support on secure bars","Lower through comfortable range");
            case "weighted-dip" -> art(id,"weighted_dip","Secure belt; support on bars","Lower; keep plate steady");
            case "cable-fly" -> art(id,"cable_fly","Stable stance; softly bent elbows","Bring hands together in an arc");
            case "pec-deck" -> art(id,"pec_deck","Adjust pads; support back","Bring pads together; return");
            case "wide-grip-bench-press" -> art(id,"wide_grip_bench_press","Comfortable wider grip; lower smoothly","Press; shoulders stay supported");
            case "weighted-push-up" -> art(id,"weighted_push_up","Secure weighted vest; aligned body","Lower; press without sagging");
            case "cable-crunch" -> art(id,"cable_crunch","Kneel; hold rope beside head","Curl ribs towards pelvis");
            default -> null;
        };
    }
    private static TechniqueIllustrations.Artwork art(String id,String asset,String... phases) {
        var guide=TechniqueGuide.forExercise(id);
        if(guide==null)throw new IllegalStateException("Illustration without movement guidance: "+id);
        return new TechniqueIllustrations.Artwork("guide_"+asset,guide.setup()+" "+guide.movement(),List.of(phases));
    }
}
