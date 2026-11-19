package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/** Curated original artwork identifiers, independent of Android resources. No generic fallback. */
object TechniqueIllustrations {
    data class Artwork(
        private val asset: String?,
        private val description: String?,
        private var phases: List<String>?,
    ) {
        init {
            require(
                asset != null &&
                    Regex("guide_[a-z_]+").matches(asset) &&
                    description != null &&
                    description.trim { it <= ' ' }.isNotEmpty() &&
                    phases != null &&
                    phases!!.isNotEmpty()
            ) {
                "Incomplete instructional art"
            }
            // The original unmodifiable ArrayList allowed null elements from raw Java lists.
            // Snapshot and expose no mutators, without adding a new element-validation rule.
            val rows = ArrayList(phases!!)
            phases =
                object : AbstractList<String>() {
                    override val size: Int
                        get() = rows.size

                    override fun get(index: Int): String = rows[index]
                }
        }

        fun asset(): String = asset!!

        fun description(): String = description!!

        fun phases(): List<String> = phases!!

        override fun toString(): String =
            "Artwork[asset=$asset, description=$description, phases=$phases]"
    }

    @JvmStatic
    fun forExercise(id: String?): Artwork? {
        if (id == null) throw NullPointerException()

        val expanded = ExpandedExerciseGuides.artwork(id)
        if (expanded != null) return expanded
        return when (id) {
            "dead-hang" ->
                Artwork(
                    "guide_dead_hang",
                    "Dead hang: both hands grip a secure overhead bar, arms extended and shoulders controlled. Feet are off the floor with knees gently bent. Breathe steadily without swinging; use a stable step to reach or leave the bar safely.",
                    Immutable.list("Secure grip; controlled hang"),
                )
            "seated-leg-curl" ->
                Artwork(
                    "guide_seated_leg_curl",
                    "Seated leg curl: thighs are secured beneath the upper restraint, back supported and knee near the machine pivot. With legs extended, the lower calves rest on the roller below them. Bend the knees to pull the roller down and back beneath the seat, then extend under control.",
                    Immutable.list(
                        "Calves on lower roller; thighs secured",
                        "Bend knees; return smoothly",
                    ),
                )
            "incline-dumbbell-press" ->
                Artwork(
                    "guide_incline_dumbbell_press",
                    "Incline dumbbell press: sit back against a moderately inclined bench with feet planted and weights beside the upper chest. Press the dumbbells above the upper chest, then lower smoothly without arching off the backrest.",
                    Immutable.list("Back supported; weights by chest", "Press; controlled return"),
                )
            "dumbbell-curl" ->
                Artwork(
                    "guide_dumbbell_curl",
                    "Dumbbell curl: stand with palms facing forwards and arms beside the trunk. Bend both elbows to lift the weights toward the shoulders, then lower smoothly without rocking or moving the upper arms forwards.",
                    Immutable.list("Palms forward; elbows by sides", "Curl; lower without swinging"),
                )
            "ez-bar-curl" ->
                Artwork(
                    "guide_ez_bar_curl",
                    "EZ-bar curl: grip the angled sections of the bar with palms turned upwards and upper arms alongside the trunk. Bend the elbows to lift the bar, then lower smoothly without leaning backwards.",
                    Immutable.list("Comfortable angled grip", "Curl; controlled return"),
                )
            "leg-extension" ->
                Artwork(
                    "guide_leg_extension",
                    "Leg extension: sit with back supported and knees aligned near the machine pivot; the roller rests in front of lower shins above the ankles. Extend the knees to lift it, then lower without kicking or lifting the hips.",
                    Immutable.list("Pad in front of shins", "Extend knees; lower slowly"),
                )
            "suitcase-carry" ->
                Artwork(
                    "guide_suitcase_carry",
                    "Suitcase carry: hold one dumbbell at the side with the opposite hand empty. Walk with controlled steps while shoulders remain level and trunk steady, then set the load down safely and change sides.",
                    Immutable.list("One load; shoulders level", "Walk steadily; change sides"),
                )
            "weighted-plank" ->
                Artwork(
                    "guide_weighted_plank",
                    "Weighted forearm plank: forearms and toes support the body with elbows beneath shoulders and a small flat plate securely positioned over the mid-back. Keep a controlled line and breathe. Get help to place and remove the load; choose an unloaded hold if it cannot be secured.",
                    Immutable.list("Secure small load; hold and breathe"),
                )
            "pallof-press" ->
                Artwork(
                    "guide_pallof_press",
                    "Pallof press: stand with a chest-height cable coming from the side and hold the handle at the sternum in both hands. Extend the hands straight forward while resisting rotation; return to the chest. Keep the same forward trunk direction, then train the other side.",
                    Immutable.list(
                        "Handle at chest; cable from side",
                        "Press forwards; resist rotation",
                    ),
                )
            "ab-wheel" ->
                Artwork(
                    "guide_ab_wheel",
                    "Kneeling ab-wheel rollout: begin on a mat with both hands gripping the wheel axle beneath shoulders. Roll forwards a manageable distance while keeping knees supported and trunk controlled, then return without sagging the back.",
                    Immutable.list(
                        "Kneel; wheel beneath shoulders",
                        "Roll forward; return under control",
                    ),
                )
            "triceps-pressdown" ->
                Artwork(
                    "guide_triceps_pressdown",
                    "Triceps pressdown: face a high cable pulley holding the short bar with elbows beside the ribs. From bent elbows, extend the forearms down until arms are comfortably straight, then return slowly while keeping upper arms still.",
                    Immutable.list(
                        "Elbows beside ribs; bar raised",
                        "Extend down; controlled return",
                    ),
                )

            "goblet-squat" ->
                Artwork(
                    "guide_goblet_squat",
                    "Goblet squat: hold one dumbbell vertically against the chest with both hands. From standing, bend hips and knees into a controlled squat with feet flat; return to standing without losing the close hold.",
                    Immutable.list("Hold at chest; brace", "Squat; stand under control"),
                )
            "dumbbell-row" ->
                Artwork(
                    "guide_dumbbell_row",
                    "One-arm dumbbell row: support one hand and knee on a stable bench with the opposite foot on the floor. Let the weighted arm hang, then draw its elbow toward the hip and return without twisting the trunk. Repeat on the other side.",
                    Immutable.list("Support on bench; arm extended", "Pull toward hip; lower"),
                )
            "push-up" ->
                Artwork(
                    "guide_push_up",
                    "Push-up: top frame shows hands beneath shoulders, straight arms and supported toes with trunk aligned. Bottom frame shows chest lowered near the mat with elbows bent, while shoulders, hips and ankles stay in a controlled line. Press back up.",
                    Immutable.list("Straight arms; steady body", "Lower chest; press back up"),
                )
            "barbell-curl" ->
                Artwork(
                    "guide_barbell_curl",
                    "Barbell curl: stand holding a straight bar with palms upwards and elbows beside the trunk. Bend the elbows to raise the bar toward the chest, then lower without swinging the hips or leaning backwards.",
                    Immutable.list("Underhand grip; arms down", "Bend elbows; lower slowly"),
                )
            "hammer-curl" ->
                Artwork(
                    "guide_hammer_curl",
                    "Hammer curl: hold two dumbbells beside the thighs with palms facing inward. Bend the elbows while retaining the neutral grip, then lower smoothly without rocking the trunk.",
                    Immutable.list("Neutral grip; elbows by sides", "Curl; controlled return"),
                )
            "lateral-raise" ->
                Artwork(
                    "guide_lateral_raise",
                    "Dumbbell lateral raise: stand holding weights beside the thighs, then raise the arms outward to about shoulder height with softly bent elbows. Lower under control without heaving or shrugging the weights.",
                    Immutable.list("Weights beside thighs", "Raise outward; lower smoothly"),
                )
            "dead-bug" ->
                Artwork(
                    "guide_dead_bug",
                    "Dead bug: top frame shows lying on a mat with arms raised and knees bent above hips. Bottom frame shows an opposite arm and leg extended while the other arm and knee stay raised. Return and alternate sides, shortening the reach if the trunk moves.",
                    Immutable.list("Arms up; knees over hips", "Opposite arm and leg reach"),
                )
            "bird-dog" ->
                Artwork(
                    "guide_bird_dog",
                    "Bird dog: begin on hands and knees with hands under shoulders and knees under hips. Extend the opposite arm and leg into a long line, keeping pelvis level. Return to all fours and alternate without arching the back.",
                    Immutable.list("Hands and knees supported", "Reach opposite arm and leg"),
                )
            "seated-calf-raise" ->
                Artwork(
                    "guide_seated_calf_raise",
                    "Seated machine calf raise: sit with knees bent and thigh pad secured above the knees; forefeet rest on the raised footplate. Lower heels comfortably, then raise them to lift the resistance without bouncing.",
                    Immutable.list(
                        "Forefeet on plate; thighs secured",
                        "Raise heels; return slowly",
                    ),
                )

            "leg-press" ->
                Artwork(
                    "guide_leg_press",
                    "Leg press: sit with back and pelvis against the machine pads and both feet flat on the platform. Lower by bending hips and knees only as far as you can keep the pelvis supported, then press without snapping the knees straight. Use the machine safeties.",
                    Immutable.list(
                        "Back supported; bend under control",
                        "Press; avoid locking knees",
                    ),
                )
            "dumbbell-shoulder-press" ->
                Artwork(
                    "guide_dumbbell_shoulder_press",
                    "Seated dumbbell shoulder press: sit against an upright bench with feet planted and a dumbbell beside each shoulder. Press the weights above the shoulders, then lower smoothly, keeping ribs controlled and wrists beneath the weights.",
                    Immutable.list(
                        "Feet planted; weights at shoulders",
                        "Press overhead; return smoothly",
                    ),
                )
            "lat-pulldown" ->
                Artwork(
                    "guide_lat_pulldown",
                    "Lat pulldown: sit with thighs secured beneath the pads and hold the overhead bar with a comfortable overhand grip. Pull toward the upper chest in front of the face with elbows moving down, then return overhead without rocking the trunk.",
                    Immutable.list("Thighs secured; arms overhead", "Pull in front to upper chest"),
                )
            "front-squat" ->
                Artwork(
                    "guide_front_squat",
                    "Front squat: support the bar across the front of the shoulders with elbows pointing forwards and hands steadying it. Stand braced, then bend hips and knees together, keeping heels planted and torso controlled; stand without allowing the bar to roll.",
                    Immutable.list("Front rack; elbows forwards", "Lower with control; stand"),
                )
            "reverse-lunge" ->
                Artwork(
                    "guide_reverse_lunge",
                    "Dumbbell reverse lunge: stand holding a manageable dumbbell beside each hip. Step one foot backwards, bend both knees with front foot flat and trunk steady, then drive through the front leg to return. Alternate sides if prescribed.",
                    Immutable.list("Stand; loads at sides", "Step back; lower and return"),
                )
            "step-up" ->
                Artwork(
                    "guide_step_up",
                    "Dumbbell step-up: place the whole working foot on a stable low step while holding dumbbells at the sides. Stand through that leg without jumping from the floor; the trailing knee may rise for balance. Lower under control before changing sides.",
                    Immutable.list("Whole foot on a stable step", "Stand through the working leg"),
                )
            "calf-raise" ->
                Artwork(
                    "guide_machine_calf_raise",
                    "Standing machine calf raise: adjust the shoulder pads, stand with forefeet supported on the machine platform and hold the handles for balance. With knees softly extended, raise the heels and lower slowly through a comfortable ankle range without bouncing.",
                    Immutable.list("Set shoulder pads; heels down", "Raise heels; lower slowly"),
                )
            "standing-calf-raise" ->
                Artwork(
                    "guide_machine_calf_raise",
                    "Standing machine calf raise: adjust the shoulder pads, support the forefeet on the footplate and steady yourself on the handles. Lift both heels without bending the knees to propel the load, then lower slowly within a comfortable range.",
                    Immutable.list(
                        "Forefeet supported; steady trunk",
                        "Lift heels; controlled return",
                    ),
                )
            "side-plank" ->
                Artwork(
                    "guide_side_plank",
                    "Side plank: support on one forearm with elbow directly below shoulder and feet stacked. Lift the hips so shoulder, hip and ankle form a steady line; keep breathing and lower to finish. Repeat on the other side as prescribed.",
                    Immutable.list("Elbow under shoulder; hips lifted"),
                )
            "farmers-carry" ->
                Artwork(
                    "guide_farmers_carry",
                    "Farmer\u2019s carry: hold a manageable dumbbell in each hand beside the hips, stand tall and walk with controlled steps. Keep shoulders level without swinging the weights. Set them down deliberately; record whether the entered load is per hand.",
                    Immutable.list("Stand tall; loads at sides", "Walk steadily; set down safely"),
                )
            "lying-leg-curl" ->
                Artwork(
                    "guide_lying_leg_curl",
                    "Lying leg curl: lie face down on the machine with knees near the pivot and ankle pad just above the heels. Keep hips supported as the knees bend to lift the pad, then extend slowly without swinging the legs.",
                    Immutable.list(
                        "Align knee pivot; hips supported",
                        "Curl the pad; lower smoothly",
                    ),
                )
            "kettlebell-swing" ->
                Artwork(
                    "guide_kettlebell_swing",
                    "Two-handed kettlebell swing: hinge with the bell passing back between the legs, then extend hips to float the bell to around chest height with relaxed arms. Keep feet planted and ribs controlled; return to a hinge as the bell falls. This is not an overhead swing.",
                    Immutable.list("Hinge; bell travels behind", "Extend hips; bell floats forward"),
                )

            "hip-thrust" ->
                Artwork(
                    "guide_hip_thrust",
                    "Barbell hip thrust: support the upper back on a stable bench, plant both feet and steady a padded bar across the hip crease. Lift the hips until the torso and thighs are near level, keeping ribs controlled; lower the hips without sliding off the bench or overarching the back.",
                    Immutable.list("Stable bench; padded bar", "Lift hips; lower under control"),
                )
            "trap-bar-deadlift" ->
                Artwork(
                    "guide_trap_bar_deadlift",
                    "Trap-bar deadlift: stand inside the hexagonal frame with feet planted and grip the side handles. Start with hips back, knees bent and a braced trunk; extend hips and knees together to stand tall without leaning back, then lower the bar under control.",
                    Immutable.list("Brace inside the frame", "Stand tall; lower smoothly"),
                )
            "barbell-row" ->
                Artwork(
                    "guide_barbell_row",
                    "Barbell row: hinge forward with softly bent knees, hips back and a steady neutral trunk. Begin with arms extended below the shoulders, pull the bar toward the lower ribs with elbows travelling behind, then lower without standing up or rocking.",
                    Immutable.list("Hinge; arms extended", "Pull to lower ribs; return"),
                )
            "pull-up" ->
                Artwork(
                    "guide_pull_up",
                    "Overhand pull-up: grip a secure overhead bar and begin in a controlled hang. Pull upward until the chin comfortably clears the bar, then lower smoothly. Keep legs controlled without swinging or using a kick.",
                    Immutable.list("Controlled hang", "Pull; lower without swinging"),
                )
            "dumbbell-bench-press" ->
                Artwork(
                    "guide_dumbbell_bench_press",
                    "Dumbbell bench press: lie supported on a flat bench with feet planted and a separate dumbbell in each hand. Begin with bent elbows and weights beside the chest, press above the chest, then lower under control with forearms beneath the weights.",
                    Immutable.list("Lower beside the chest", "Press above the chest"),
                )
            "deadlift" ->
                Artwork(
                    "guide_deadlift",
                    "Conventional deadlift: start with bar on the floor close to the shins, hips back, knees bent, arms straight and hands outside the legs. Stand by extending hips and knees together while the bar stays close, finish tall without leaning back, then lower under control.",
                    Immutable.list("Brace; bar close to shins", "Stand tall; lower under control"),
                )
            "overhead-press" ->
                Artwork(
                    "guide_overhead_press",
                    "Standing barbell overhead press: begin with the bar at shoulder level and a braced upright trunk. Press the bar above the shoulders with feet planted and no leg drive, then lower under control without leaning back.",
                    Immutable.list("Brace; bar at shoulders", "Press overhead; return"),
                )
            "seated-cable-row" ->
                Artwork(
                    "guide_seated_cable_row",
                    "Seated cable row: sit with feet supported and knees softly bent, hold the low cable handle with arms extended, then pull toward the lower ribs with elbows close and a steady trunk. Return the handle under control without rocking backwards.",
                    Immutable.list("Arms extended; feet supported", "Pull to lower ribs; return"),
                )
            "back-squat" ->
                Artwork(
                    "guide_back_squat",
                    "Back squat: stand with the bar supported across the upper back and both hands gripping it; lower hips and knees together with feet flat and a controlled trunk, then stand. Use suitable rack safeties or a spotter.",
                    Immutable.list("Brace; bar on upper back", "Lower under control; stand"),
                )
            "split-squat" ->
                Artwork(
                    "guide_bulgarian_split_squat",
                    "Bulgarian split squat: rear foot rests on a stable low bench, front foot flat with space to bend the knee; lower through the front leg with the knee following the toes, then rise without pushing off the bench. Bodyweight movement shown; optional loads are not illustrated.",
                    Immutable.list("Stable rear support", "Lower through the front leg"),
                )
            "plank" ->
                Artwork(
                    "guide_plank",
                    "Forearm plank: prepare with forearms supported and elbows under shoulders; lift knees and support on forearms and toes with shoulders, hips and ankles aligned, breathing steadily. Lower knees to end the hold.",
                    Immutable.list("Prepare on forearms", "Lift and hold; breathe"),
                )
            "romanian-deadlift" ->
                Artwork(
                    "guide_romanian_deadlift",
                    "Romanian deadlift: standing with the bar close to the thighs; then hips move backwards with softly bent knees, a neutral back and the bar close to the shins.",
                    Immutable.list("Stand tall", "Hinge at the hips"),
                )
            "glute-bridge" ->
                Artwork(
                    "guide_glute_bridge",
                    "Bodyweight glute bridge: begin lying on a mat with bent knees and flat feet; then lift the hips while the shoulders remain supported, keeping ribs controlled.",
                    Immutable.list("Feet flat; knees bent", "Lift hips under control"),
                )
            "bench-press" ->
                Artwork(
                    "guide_bench_press",
                    "Bench press: lie supported with feet planted, lower the bar over the chest with bent elbows, then press above the chest with arms extended.",
                    Immutable.list("Controlled lowering", "Press above the chest"),
                )
            "walking-lunge" ->
                Artwork(
                    "guide_walking_lunge",
                    "Walking lunge: begin standing; step forward and bend both knees with the front foot flat, torso upright and back knee lowered under control.",
                    Immutable.list("Stand tall", "Step and lower"),
                )
            else -> CatalogueTechniqueIllustrations.forExercise(id)
        }
    }

    @JvmStatic
    fun forRoutine(id: String?): Artwork? {
        if (id == null) throw NullPointerException()
        return when (id) {
            "lunge" -> forExercise("walking-lunge")
            "bridge" -> forExercise("glute-bridge")
            "swing" ->
                Artwork(
                    "guide_leg_swings",
                    "Supported leg swings: hold a stable rail, move one leg gently forward and back while the other foot stays planted and trunk stays upright.",
                    Immutable.list("Swing gently forward", "Return gently behind"),
                )
            "walk",
            "jog" ->
                Artwork(
                    "guide_walk",
                    "Easy walking option: use a comfortable stride, upright relaxed posture and natural arm movement. An easy jog is optional if comfortable.",
                    Immutable.list("Walk comfortably"),
                )
            "march" ->
                Artwork(
                    "guide_march",
                    "Controlled marching: stand tall, then lift one knee comfortably with the opposite arm; lower and alternate sides without leaning back.",
                    Immutable.list("Stand tall", "Lift; lower and alternate"),
                )
            "calf" ->
                Artwork(
                    "guide_calf_raise",
                    "Supported calf raises: hold a stable rail, start with heels down, then lift both heels gently onto the forefoot and lower under control.",
                    Immutable.list("Heels down", "Rise and lower slowly"),
                )
            "hip" ->
                Artwork(
                    "guide_hip_flexor",
                    "Hip-flexor stretch: half-kneel on a mat with the front foot flat, keep the trunk upright and gently move the pelvis forward without arching the back.",
                    Immutable.list("Half-kneel; gentle forward shift"),
                )
            "quad" ->
                Artwork(
                    "guide_quad_stretch",
                    "Supported quad stretch: hold a stable rail, bend the other knee and gently support the ankle behind you, keeping balance and avoiding force.",
                    Immutable.list("Stand supported; hold gently"),
                )
            "glute" ->
                Artwork(
                    "guide_seated_glute",
                    "Seated glute stretch: sit on a stable chair with one foot flat, rest the other ankle across the opposite thigh and hinge forward gently without forcing the knee down.",
                    Immutable.list("Sit securely; gentle stretch"),
                )
            "stride" ->
                Artwork(
                    "guide_strides",
                    "Controlled strides: build gradually into relaxed brisk running with smooth steps and arm movement, then ease down and walk to recover. Not maximal sprinting.",
                    Immutable.list("Build smoothly", "Relax and ease down"),
                )
            "circles" ->
                Artwork(
                    "guide_ankle_circles",
                    "Ankle circles: hold a stable rail, lift one foot slightly and rotate the ankle slowly while the supporting leg and trunk stay steady.",
                    Immutable.list("Supported gentle ankle rotation"),
                )
            "calf-stretch" ->
                Artwork(
                    "guide_calf_stretch",
                    "Calf stretch: hold a stable wall, front knee softly bent and back leg comfortable with the whole back foot including heel down; lean gently without bouncing.",
                    Immutable.list("Back heel down; gentle stretch"),
                )
            "ankle" ->
                Artwork(
                    "guide_ankle_rocks",
                    "Ankle rocks: half-kneeling with the front foot flat, move the front knee gently forward over the toes while keeping the heel down.",
                    Immutable.list("Half-kneeling", "Rock gently; heel down"),
                )
            else -> null
        }
    }
}
