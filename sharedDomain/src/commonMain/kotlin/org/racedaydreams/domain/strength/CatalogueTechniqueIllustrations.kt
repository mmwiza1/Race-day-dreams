package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable

/** Explicit reviewed movement-to-asset mapping; never chooses from a generic pattern. */
internal object CatalogueTechniqueIllustrations {
    fun forExercise(id: String): TechniqueIllustrations.Artwork? {

        return when (id) {
            // These named variants have identical visible equipment and positions; timing/copy
            // differ.
            "high-bar-squat" ->
                art(id, "back_squat", "Bar on upper traps; brace", "Lower with control; stand")
            "pause-squat" ->
                art(id, "back_squat", "Brace; lower smoothly", "Pause while braced; stand")
            "box-step-up" ->
                art(id, "step_up", "Whole foot on stable box", "Stand; step down slowly")
            "row" -> art(id, "barbell_row", "Hinge; trunk steady", "Pull towards lower ribs")
            "box-squat" ->
                art(id, "box_squat", "Stable box behind you", "Touch down; stand without rocking")
            "hack-squat" ->
                art(id, "hack_squat", "Lower carriage under control", "Press with back supported")
            "pendulum-squat" ->
                art(
                    id,
                    "pendulum_squat",
                    "Set stops; steady stance",
                    "Lower with the swinging carriage",
                )
            "zercher-squat" ->
                art(id, "zercher_squat", "Cradle bar in elbow creases", "Squat; keep bar close")
            "smith-machine-squat" ->
                art(id, "smith_machine_squat", "Set guide-rail safeties", "Squat; return to hooks")
            "single-leg-press" ->
                art(id, "single_leg_press", "One foot on platform", "Press; keep pelvis supported")
            "sissy-squat" ->
                art(id, "sissy_squat", "Hold a stable support", "Knees forwards; controlled return")
            "sumo-deadlift" ->
                art(
                    id,
                    "sumo_deadlift",
                    "Wide stance; grip inside knees",
                    "Stand tall; lower smoothly",
                )
            "single-leg-rdl" ->
                art(
                    id,
                    "single_leg_rdl",
                    "Balance; soften working knee",
                    "Hinge; free leg reaches back",
                )
            "good-morning" ->
                art(id, "good_morning", "Bar on upper back; brace", "Hinge hips back; return")
            "low-bar-squat" ->
                art(id, "low_bar_squat", "Bar on rear-shoulder shelf", "Hips back; stay braced")
            "belt-squat" ->
                art(id, "belt_squat", "Secure belt and cable", "Squat with centred hip load")
            "bodyweight-squat" ->
                art(id, "bodyweight_squat", "Stand balanced", "Squat; keep feet planted")
            "reverse-lunge-bodyweight" ->
                art(id, "reverse_lunge_bodyweight", "Stand tall", "Step back; lower with control")
            "single-leg-calf-raise-bodyweight" ->
                art(
                    id,
                    "single_leg_calf_raise_bodyweight",
                    "Forefoot supported; hold rail",
                    "Raise heel; lower slowly",
                )
            "wall-sit" -> art(id, "wall_sit", "Back supported; breathe steadily")
            "weighted-wall-sit" -> art(id, "weighted_wall_sit", "Secure plate; hold and breathe")
            "weighted-side-plank" ->
                art(id, "weighted_side_plank", "Elbow under shoulder; secure plate")
            "hanging-knee-raise" ->
                art(id, "hanging_knee_raise", "Steady hang", "Raise knees; no swinging")
            "hanging-leg-raise" ->
                art(id, "hanging_leg_raise", "Steady hang", "Raise legs; lower slowly")
            "toes-to-bar" ->
                art(id, "toes_to_bar", "Secure grip; steady hang", "Raise toes; controlled return")
            "russian-twist" ->
                art(
                    id,
                    "russian_twist",
                    "Feet supported; hold ball close",
                    "Rotate gently; alternate sides",
                )
            "weighted-sit-up" ->
                art(id, "weighted_sit_up", "Plate held at chest", "Curl up; lower with control")
            "chin-up" ->
                art(id, "chin_up", "Underhand grip; steady hang", "Pull chin above bar; lower")
            "neutral-grip-pull-up" ->
                art(
                    id,
                    "neutral_grip_pull_up",
                    "Palms face each other",
                    "Pull; lower without swinging",
                )
            "weighted-pull-up" ->
                art(id, "weighted_pull_up", "Overhand grip; secure belt", "Pull; keep plate steady")
            "weighted-chin-up" ->
                art(id, "weighted_chin_up", "Underhand grip; secure belt", "Pull; lower smoothly")
            "inverted-row" ->
                art(id, "inverted_row", "Heels supported; arms extended", "Pull chest towards bar")
            "close-grip-pulldown" ->
                art(
                    id,
                    "close_grip_pulldown",
                    "Thighs secured; neutral grip",
                    "Pull towards upper chest",
                )
            "straight-arm-pulldown" ->
                art(
                    id,
                    "straight_arm_pulldown",
                    "Soft elbows; brace trunk",
                    "Sweep arms towards thighs",
                )
            "face-pull" -> art(id, "face_pull", "Rope at face height", "Separate hands beside face")
            "pendlay-row" ->
                art(id, "pendlay_row", "Brace over floor-supported bar", "Row; return and reset")
            "chest-supported-row" ->
                art(
                    id,
                    "chest_supported_row",
                    "Chest supported on bench",
                    "Pull towards ribs; lower",
                )
            "t-bar-row" ->
                art(id, "t_bar_row", "Secure landmine; hinge and brace", "Row without heaving")
            "machine-row" ->
                art(id, "machine_row", "Chest pad supports trunk", "Pull handles towards ribs")
            "close-grip-bench-press" ->
                art(
                    id,
                    "close_grip_bench_press",
                    "Shoulder-width grip; controlled lower",
                    "Press above chest",
                )
            "incline-bench-press" ->
                art(
                    id,
                    "incline_bench_press",
                    "Supported incline; lower smoothly",
                    "Press above upper chest",
                )
            "decline-bench-press" ->
                art(
                    id,
                    "decline_bench_press",
                    "Feet secure; lower smoothly",
                    "Press; keep body supported",
                )
            "decline-dumbbell-press" ->
                art(
                    id,
                    "decline_dumbbell_press",
                    "Feet secured; weights beside chest",
                    "Press with control",
                )
            "machine-chest-press" ->
                art(
                    id,
                    "machine_chest_press",
                    "Back supported; handles at chest",
                    "Press; controlled return",
                )
            "smith-machine-bench-press" ->
                art(
                    id,
                    "smith_machine_bench_press",
                    "Set safety stops; lower smoothly",
                    "Press; re-hook deliberately",
                )
            "dip" -> art(id, "dip", "Support on secure bars", "Lower through comfortable range")
            "weighted-dip" ->
                art(id, "weighted_dip", "Secure belt; support on bars", "Lower; keep plate steady")
            "cable-fly" ->
                art(
                    id,
                    "cable_fly",
                    "Stable stance; softly bent elbows",
                    "Bring hands together in an arc",
                )
            "pec-deck" ->
                art(id, "pec_deck", "Adjust pads; support back", "Bring pads together; return")
            "wide-grip-bench-press" ->
                art(
                    id,
                    "wide_grip_bench_press",
                    "Comfortable wider grip; lower smoothly",
                    "Press; shoulders stay supported",
                )
            "weighted-push-up" ->
                art(
                    id,
                    "weighted_push_up",
                    "Secure weighted vest; aligned body",
                    "Lower; press without sagging",
                )
            "cable-crunch" ->
                art(id, "cable_crunch", "Kneel; hold rope beside head", "Curl ribs towards pelvis")
            "seated-barbell-press" ->
                art(
                    id,
                    "seated_barbell_press",
                    "Front shoulders; supported bench",
                    "Press overhead; lower smoothly",
                )
            "arnold-press" ->
                art(
                    id,
                    "arnold_press",
                    "Palms towards you; elbows forward",
                    "Rotate gently while pressing",
                )
            "machine-shoulder-press" ->
                art(
                    id,
                    "machine_shoulder_press",
                    "Adjust seat; back supported",
                    "Press handles overhead",
                )
            "cable-lateral-raise" ->
                art(
                    id,
                    "cable_lateral_raise",
                    "Low pulley; soft elbow",
                    "Raise sideways towards shoulder height",
                )
            "front-raise" ->
                art(id, "front_raise", "Weights at thighs; brace", "Raise forwards; lower smoothly")
            "rear-delt-fly" ->
                art(id, "rear_delt_fly", "Hinge; arms below chest", "Open arms; keep trunk steady")
            "reverse-pec-deck" ->
                art(id, "reverse_pec_deck", "Face chest pad; adjust reach", "Open arms outwards")
            "incline-dumbbell-curl" ->
                art(
                    id,
                    "incline_dumbbell_curl",
                    "Back supported; arms hanging",
                    "Curl; upper arms stay steady",
                )
            "preacher-curl" ->
                art(id, "preacher_curl", "Upper arms on pad", "Curl without lifting off pad")
            "cable-curl" ->
                art(
                    id,
                    "cable_curl",
                    "Low pulley; elbows beside trunk",
                    "Curl; control cable return",
                )
            "concentration-curl" ->
                art(
                    id,
                    "concentration_curl",
                    "Brace upper arm inside thigh",
                    "Curl towards same shoulder",
                )
            "spider-curl" ->
                art(id, "spider_curl", "Chest supported; arms hanging", "Curl; stay on the pad")
            "rope-pressdown" ->
                art(id, "rope_pressdown", "Elbows bent beside trunk", "Extend towards thighs")
            "overhead-cable-extension" ->
                art(
                    id,
                    "overhead_cable_extension",
                    "Rope behind head; brace",
                    "Extend elbows overhead",
                )
            "dumbbell-overhead-extension" ->
                art(
                    id,
                    "dumbbell_overhead_extension",
                    "Secure grip above head",
                    "Bend elbows; lower behind head",
                )
            "bench-dip" ->
                art(
                    id,
                    "bench_dip",
                    "Hands secure; feet supported",
                    "Lower through short comfortable range",
                )
            "upright-row" ->
                art(
                    id,
                    "upright_row",
                    "Light weights; comfortable wrists",
                    "Elbows below shoulder height",
                )
            "landmine-press" ->
                art(
                    id,
                    "landmine_press",
                    "Face secure anchor; bar at shoulder",
                    "Press forwards and upwards",
                )
            "skull-crusher" ->
                art(id, "skull_crusher", "Light EZ bar above chest", "Bend elbows; bar clears head")
            "jm-press" ->
                art(
                    id,
                    "jm_press",
                    "Light load; comfortable narrow grip",
                    "Fold elbows; controlled shallow arc",
                )
            "stiff-leg-deadlift" ->
                art(id, "stiff_leg_deadlift", "Soft knees; brace", "Hinge hips back; bar close")
            "deficit-deadlift" ->
                art(
                    id,
                    "deficit_deadlift",
                    "Stable low riser; bar below feet",
                    "Stand; lower and reset",
                )
            "snatch-grip-deadlift" ->
                art(id, "snatch_grip_deadlift", "Wide grip; brace over bar", "Stand with bar close")
            "cable-pull-through" ->
                art(
                    id,
                    "cable_pull_through",
                    "Face away; rope between legs",
                    "Extend hips; keep ribs controlled",
                )
            "barbell-glute-bridge" ->
                art(
                    id,
                    "barbell_glute_bridge",
                    "Upper back on mat; secure padded bar",
                    "Lift hips; lower smoothly",
                )
            "lateral-lunge" ->
                art(
                    id,
                    "lateral_lunge",
                    "Balanced stance; weights at sides",
                    "Step sideways; sit hips back",
                )
            "curtsy-lunge" ->
                art(id, "curtsy_lunge", "Stand balanced", "Small diagonal step; comfortable knee")
            "deficit-reverse-lunge" ->
                art(
                    id,
                    "deficit_reverse_lunge",
                    "Stand on low stable platform",
                    "Step back to floor; lower smoothly",
                )
            "front-foot-elevated-split-squat" ->
                art(
                    id,
                    "front_foot_elevated_split_squat",
                    "Front foot on low block",
                    "Lower both knees; control return",
                )
            "cossack-squat" ->
                art(
                    id,
                    "cossack_squat",
                    "Wide comfortable stance",
                    "Shift to one side; working heel down",
                )
            "pistol-squat" ->
                art(
                    id,
                    "pistol_squat",
                    "Hold secure support; free leg forwards",
                    "Lower within control; use support",
                )
            "swiss-ball-leg-curl" ->
                art(
                    id,
                    "swiss_ball_leg_curl",
                    "Heels on ball; hips controlled",
                    "Curl ball towards hips",
                )
            "sliding-leg-curl" ->
                art(
                    id,
                    "sliding_leg_curl",
                    "Heels on sliders; hips raised",
                    "Draw heels in; slide out slowly",
                )
            "rack-pull" ->
                art(
                    id,
                    "rack_pull",
                    "Bar supported on equal safety pins",
                    "Stand; return gently to pins",
                )
            "block-pull" ->
                art(
                    id,
                    "block_pull",
                    "Plates supported on equal low blocks",
                    "Lift clear; lower onto blocks",
                )
            "tibialis-raise" ->
                art(
                    id,
                    "tibialis_raise",
                    "Wall support; heels on floor",
                    "Lift forefeet; keep heels down",
                )
            "barbell-lunge" ->
                art(
                    id,
                    "barbell_lunge",
                    "Secure bar on upper back",
                    "Step forwards; controlled lunge",
                )
            "high-step-up" ->
                art(id, "high_step_up", "Whole foot on stable box", "Stand; step down slowly")
            "standing-leg-curl" ->
                art(
                    id,
                    "standing_leg_curl",
                    "Adjust calf roller; hold supports",
                    "Bend knee; keep thigh steady",
                )
            "nordic-hamstring-curl" ->
                art(
                    id,
                    "nordic_hamstring_curl",
                    "Secure ankles; padded knees",
                    "Lean from knees; use hand support",
                )
            "glute-ham-raise" ->
                art(
                    id,
                    "glute_ham_raise",
                    "Ankles secure; thighs supported",
                    "Bend knees; lift torso",
                )
            "leg-press-calf-raise" ->
                art(
                    id,
                    "leg_press_calf_raise",
                    "Forefeet secure; knees steady",
                    "Raise heels; lower slowly",
                )
            "single-leg-calf-raise" ->
                art(
                    id,
                    "single_leg_calf_raise",
                    "One forefoot supported; hold handles",
                    "Raise heel; keep balance",
                )
            "donkey-calf-raise" ->
                art(
                    id,
                    "donkey_calf_raise",
                    "Hip pad secure; forearms supported",
                    "Raise heels; controlled lower",
                )
            "dumbbell-fly" ->
                art(
                    id,
                    "dumbbell_fly",
                    "Soft elbows; weights over chest",
                    "Open in comfortable arc",
                )
            "meadows-row" ->
                art(
                    id,
                    "meadows_row",
                    "Secure landmine; brace on thigh",
                    "Row towards hip; trunk steady",
                )
            "kettlebell-goblet-squat" ->
                art(
                    id,
                    "kettlebell_goblet_squat",
                    "Hold bell close by horns",
                    "Squat; keep heels planted",
                )
            "front-rack-carry" ->
                art(
                    id,
                    "front_rack_carry",
                    "Secure two-bell front rack",
                    "Walk with short controlled steps",
                )
            "forward-lunge",
            "lunge" ->
                art(
                    id,
                    "barbell_lunge",
                    "Secure bar on upper back",
                    "Step forwards; controlled lunge",
                )
            "dumbbell-lunge" ->
                art(
                    id,
                    "reverse_lunge",
                    "Dumbbells steady; stand balanced",
                    "Illustrated reverse variation; lower smoothly",
                )
            "power-clean" ->
                art(
                    id,
                    "power_clean",
                    "Floor start; brace",
                    "Drive through hips and legs",
                    "Receive in shallow front squat",
                )
            "clean" ->
                art(
                    id,
                    "clean",
                    "Floor start; secure grip",
                    "Extend with bar close",
                    "Receive in controlled front squat",
                )
            "hang-clean" ->
                art(
                    id,
                    "hang_clean",
                    "Hang above knees; brace",
                    "Drive through hips and legs",
                    "Receive in front rack",
                )
            "clean-jerk" ->
                art(
                    id,
                    "clean_jerk",
                    "Floor start; brace",
                    "Clean to front rack; stand",
                    "Jerk to stable overhead split",
                )
            "power-snatch" ->
                art(
                    id,
                    "power_snatch",
                    "Wide grip; floor start",
                    "Extend with bar close",
                    "Receive overhead in shallow squat",
                )
            "snatch" ->
                art(
                    id,
                    "snatch",
                    "Wide grip; floor start",
                    "Extend with bar close",
                    "Receive in controlled overhead squat",
                )
            "hang-snatch" ->
                art(
                    id,
                    "hang_snatch",
                    "Wide grip; hang above knees",
                    "Drive with bar close",
                    "Receive overhead; stand",
                )
            "high-pull" ->
                art(
                    id,
                    "high_pull",
                    "Hang; brace",
                    "Drive through legs and hips",
                    "Guide elbows out; no catch",
                )
            "push-jerk" ->
                art(
                    id,
                    "push_jerk",
                    "Secure front rack",
                    "Short upright dip",
                    "Receive overhead; knees bent",
                )
            "split-jerk" ->
                art(
                    id,
                    "split_jerk",
                    "Secure front rack",
                    "Short upright dip",
                    "Receive in stable split",
                )
            "push-press" ->
                art(
                    id,
                    "push_press",
                    "Secure front rack",
                    "Short upright dip",
                    "Press; finish standing tall",
                )
            "american-kettlebell-swing" ->
                art(
                    id,
                    "american_kettlebell_swing",
                    "Hinge with secure two-hand grip",
                    "Drive from hips",
                    "Comfortable overhead finish",
                )
            "kettlebell-clean" ->
                art(
                    id,
                    "kettlebell_clean",
                    "Hinge; secure one-hand grip",
                    "Drive; guide bell close",
                    "Receive softly in front rack",
                )
            "kettlebell-snatch" ->
                art(
                    id,
                    "kettlebell_snatch",
                    "Hinge; clear overhead space",
                    "Drive; guide bell close",
                    "Turn through to stable overhead",
                )
            "thruster" ->
                art(
                    id,
                    "thruster",
                    "Dumbbells at front shoulders",
                    "Controlled front squat",
                    "Drive into overhead press",
                )
            "wall-ball" ->
                art(
                    id,
                    "wall_ball",
                    "Ball at chest; clear target",
                    "Squat with control",
                    "Stand and throw; receive softly",
                )
            "devil-press" ->
                art(
                    id,
                    "devil_press",
                    "Stable hex dumbbells; push-up position",
                    "Bring feet in; reset hinge",
                    "Drive to controlled overhead",
                )
            "dumbbell-snatch" ->
                art(
                    id,
                    "dumbbell_snatch",
                    "Floor start; brace",
                    "Drive; keep weight close",
                    "Receive overhead; lower safely",
                )
            "dumbbell-clean-jerk" ->
                art(
                    id,
                    "dumbbell_clean_jerk",
                    "Hinge with light dumbbells",
                    "Clean to front rack",
                    "Dip and drive; receive overhead",
                )
            "sled-push" ->
                art(
                    id,
                    "sled_push",
                    "Secure grip; brace",
                    "Short controlled steps",
                    "Keep arms and trunk steady",
                )
            "sled-pull" ->
                art(
                    id,
                    "sled_pull",
                    "Secure straps; clear path behind",
                    "Step backwards under control",
                    "Keep straps taut; trunk steady",
                )
            "sandbag-carry" ->
                art(
                    id,
                    "sandbag_carry",
                    "Secure bear hug",
                    "Walk with controlled steps",
                    "Breathe; set down before grip fades",
                )
            "sandbag-clean" ->
                art(
                    id,
                    "sandbag_clean",
                    "Secure floor grip",
                    "Bring to lap; reset grip",
                    "Stand into front bear hug",
                )
            "ghd-sit-up" ->
                art(
                    id,
                    "ghd_sit_up",
                    "Adjust support; feet secure",
                    "Lean back within controlled range",
                    "Return without jerking",
                )
            else -> null
        }
    }

    private fun art(
        id: String,
        asset: String,
        vararg phases: String,
    ): TechniqueIllustrations.Artwork {
        val guide =
            TechniqueGuide.forExercise(id)
                ?: throw IllegalStateException("Illustration without movement guidance: " + id)
        return TechniqueIllustrations.Artwork(
            "guide_" + asset,
            guide.setup() + " " + guide.movement(),
            Immutable.list(*phases),
        )
    }
}
