package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable

/** Original offline coaching copy and diagram specifications; no third-party assets. */
data class TechniqueGuide(
    private val pattern: String?,
    private val setup: String?,
    private val movement: String?,
    private val cues: List<String>?,
    private val mistakes: List<String>?,
    private val muscles: String?,
    private val alternatives: List<String>?,
) {
    fun pattern(): String? = pattern

    fun setup(): String? = setup

    fun movement(): String? = movement

    fun cues(): List<String>? = cues

    fun mistakes(): List<String>? = mistakes

    fun muscles(): String? = muscles

    fun alternatives(): List<String>? = alternatives

    override fun toString(): String =
        "TechniqueGuide[pattern=$pattern, setup=$setup, movement=$movement, cues=$cues, mistakes=$mistakes, muscles=$muscles, alternatives=$alternatives]"

    companion object {
        @JvmStatic
        fun forExercise(id: String?): TechniqueGuide? {
            if (id == null) throw NullPointerException()

            val expanded = FocusedCatalogueAdditions.guide(id) ?: ExpandedExerciseGuides.guide(id)
            if (expanded != null) return expanded
            var pattern: String
            var setup: String
            var movement: String
            var muscles: String
            var alternatives: List<String>
            when (id) {
                "back-squat",
                "front-squat",
                "leg-press" -> {
                    pattern = "squat"
                    setup =
                        if ((id == "leg-press"))
                            "Sit with your back supported and feet stable on the platform."
                        else "Set your feet comfortably apart and brace; support the load securely."
                    movement =
                        if ((id == "leg-press"))
                            "Lower the platform under control, then press without locking your knees."
                        else
                            "Bend hips and knees together to a controlled depth, then stand through your whole foot."
                    muscles = "Quadriceps and glutes"
                    alternatives = Immutable.list("Goblet Squat", "Leg Press")
                }
                "deadlift",
                "romanian-deadlift",
                "trap-bar-deadlift",
                "kettlebell-swing" -> {
                    pattern = "hinge"
                    setup =
                        if ((id == "trap-bar-deadlift"))
                            "Stand inside the bar with the handles alongside you; brace before lifting."
                        else if ((id == "kettlebell-swing"))
                            "Start with the bell just ahead of you; hinge and grip with both hands."
                        else "Stand close to the load, soften your knees and brace your trunk."
                    movement =
                        if ((id == "romanian-deadlift"))
                            "Push hips back with a small knee bend; lower the bar close to your legs, then extend the hips."
                        else if ((id == "kettlebell-swing"))
                            "Hike the bell back, then extend your hips to float it forward; guide it back into a hinge."
                        else
                            "Push through your feet and extend hips and knees together, then lower under control."
                    muscles = "Glutes, hamstrings and back extensors"
                    alternatives = Immutable.list("Romanian Deadlift", "Hip Thrust")
                }
                "split-squat",
                "reverse-lunge",
                "walking-lunge",
                "step-up" -> {
                    pattern = "split"
                    setup =
                        if ((id == "split-squat"))
                            "Place the rear foot on a stable low support; leave room to bend the front knee."
                        else if ((id == "step-up"))
                            "Place the whole working foot on a stable step of manageable height."
                        else "Stand tall with space around you and your feet comfortably apart."
                    movement =
                        if ((id == "step-up"))
                            "Drive through the foot on the step to stand up, then lower under control."
                        else if ((id == "reverse-lunge"))
                            "Step back, bend both knees, then push through the front foot to return."
                        else if ((id == "walking-lunge"))
                            "Step forward, bend both knees under control, then step through into the next lunge."
                        else
                            "Lower with control through the front leg, then stand without bouncing off the rear foot."
                    muscles = "Quadriceps and glutes"
                    alternatives = Immutable.list("Reverse Lunge", "Step-Up", "Goblet Squat")
                }
                "bench-press",
                "dumbbell-bench-press" -> {
                    pattern = "press"
                    setup =
                        "Lie on a stable bench with feet supported; hold the load over your chest. Use appropriate rack safeties or a spotter for a barbell."
                    movement =
                        "Lower toward the chest with forearms supported beneath the load; press back up under control."
                    muscles = "Chest, triceps and front shoulders"
                    alternatives = Immutable.list("Push-Up", "Machine Chest Press")
                }
                "overhead-press",
                "dumbbell-shoulder-press" -> {
                    pattern = "overhead"
                    setup = "Hold the load at shoulder level with a stable stance and braced trunk."
                    movement =
                        "Press overhead without leaning back excessively, then return to shoulder height under control."
                    muscles = "Shoulders and triceps"
                    alternatives = Immutable.list("Landmine Press", "Machine Shoulder Press")
                }
                "barbell-row",
                "seated-cable-row" -> {
                    pattern = "row"
                    setup =
                        if ((id == "barbell-row"))
                            "Hinge at the hips with a braced trunk and arms hanging below the shoulders."
                        else
                            "Sit with feet supported, hold the handle and keep a comfortable upright trunk."
                    movement =
                        "Pull toward the lower ribs while keeping your trunk steady; extend the arms under control."
                    muscles = "Upper back and elbow flexors"
                    alternatives = Immutable.list("Chest-Supported Row", "Dumbbell Row")
                }
                "lat-pulldown",
                "pull-up" -> {
                    pattern = "pull"
                    setup =
                        if ((id == "pull-up")) "Grip a secure bar and start from a controlled hang."
                        else "Sit with thighs secured and take a comfortable grip on the bar."
                    movement =
                        if ((id == "pull-up"))
                            "Pull your body upward without swinging, then lower with control."
                        else
                            "Pull the bar toward your upper chest, then allow a controlled return overhead."
                    muscles = "Lats, upper back and elbow flexors"
                    alternatives = Immutable.list("Lat Pulldown", "Inverted Row")
                }
                "hip-thrust",
                "glute-bridge" -> {
                    pattern = "bridge"
                    setup =
                        if ((id == "glute-bridge"))
                            "Lie on your back on a mat with knees bent, feet flat and arms alongside your body."
                        else
                            "Support your upper back on a stable bench and plant your feet; pad the load over the hips."
                    movement =
                        "Extend the hips with your ribs controlled; pause near level, then lower without overextending the back."
                    muscles = "Glutes"
                    alternatives = Immutable.list("Barbell Glute Bridge", "Romanian Deadlift")
                }
                "calf-raise",
                "standing-calf-raise" -> {
                    pattern = "calf"
                    setup =
                        "Stand with stable foot support and hold something for balance if needed."
                    movement =
                        "Rise onto the balls of your feet, pause briefly, then lower your heels slowly through a comfortable range."
                    muscles = "Calves"
                    alternatives = Immutable.list("Seated Calf Raise", "Single-Leg Calf Raise")
                }
                "plank",
                "side-plank" -> {
                    pattern = "plank"
                    setup =
                        if ((id == "side-plank"))
                            "Lie on your side with the elbow beneath the shoulder and legs stacked or staggered."
                        else
                            "Place your forearms on the floor with elbows under the shoulders and legs extended."
                    movement =
                        if ((id == "side-plank"))
                            "Lift the hips into a straight supported line and breathe steadily; lower to finish."
                        else
                            "Lift into a supported line, brace gently and breathe; end the hold when control becomes difficult."
                    muscles = "Trunk stabilisers"
                    alternatives = Immutable.list("Dead Bug", "Bird Dog")
                }
                "farmers-carry" -> {
                    pattern = "carry"
                    setup =
                        "Stand between two manageable loads and lift with a braced trunk. Record whether load is per hand in your notes."
                    movement =
                        "Walk with controlled steps and steady posture; place the weights down under control."
                    muscles = "Grip, trunk and upper back"
                    alternatives = Immutable.list("Suitcase Carry", "Front Rack Carry")
                }
                "lying-leg-curl" -> {
                    pattern = "curl"
                    setup =
                        "Adjust the machine pivot near your knee and the pad above your heels; keep your hips supported."
                    movement =
                        "Bend the knees to curl the pad toward you, then extend under control without lifting the hips."
                    muscles = "Hamstrings"
                    alternatives = Immutable.list("Seated Leg Curl", "Sliding Leg Curl")
                }
                else -> {
                    return AdditionalTechniqueGuides.forExercise(id)
                }
            }
            var cues: List<String>
            var mistakes: List<String>
            when (pattern) {
                "hinge" -> {
                    cues =
                        Immutable.list(
                            "Brace your trunk",
                            "Hinge at the hips",
                            "Keep the load close",
                            "Control the lowering phase",
                        )
                    mistakes =
                        Immutable.list(
                            "Turning the hinge into a squat",
                            "Letting the load drift forward",
                            "Excessive back rounding or uncontrolled lowering",
                        )
                }
                "squat" -> {
                    cues =
                        Immutable.list(
                            "Keep your whole foot supported",
                            "Brace before each repetition",
                            "Let knees follow a comfortable line with your feet",
                        )
                    mistakes =
                        Immutable.list(
                            "Losing foot contact or balance",
                            "Bouncing beyond a controllable range",
                            "Choosing a load that changes your intended movement",
                        )
                }
                "split" -> {
                    cues =
                        Immutable.list(
                            "Keep the working foot stable",
                            "Lower with control",
                            "Use a manageable step or support height",
                        )
                    mistakes =
                        Immutable.list(
                            "Pushing mainly from the trailing leg",
                            "Using an unstable support",
                            "Rushing the lowering phase",
                        )
                }
                "press",
                "overhead" -> {
                    cues =
                        Immutable.list(
                            "Keep wrists supported beneath the load",
                            "Maintain a stable trunk and support",
                            "Press smoothly and lower under control",
                        )
                    mistakes =
                        Immutable.list(
                            "Bouncing or jerking the load",
                            "Excessive back arching to finish a repetition",
                            "Losing stable contact with the bench or floor",
                        )
                }
                "row",
                "pull" -> {
                    cues =
                        Immutable.list(
                            "Keep your trunk steady",
                            "Lead the pull with controlled elbow movement",
                            "Return the load smoothly",
                        )
                    mistakes =
                        Immutable.list(
                            "Swinging the body for momentum",
                            "Jerking at the start",
                            "Pulling beyond a comfortable range",
                        )
                }
                "plank" -> {
                    cues =
                        Immutable.list(
                            "Place the elbow beneath the shoulder",
                            "Keep a controlled supported body line",
                            "Breathe throughout the hold",
                        )
                    mistakes =
                        Immutable.list(
                            "Letting the hips sag or twist",
                            "Holding your breath",
                            "Extending the hold after control deteriorates",
                        )
                }
                "bridge" -> {
                    cues =
                        Immutable.list(
                            "Keep ribs and pelvis controlled",
                            "Drive through stable feet",
                            "Finish with hip extension rather than back extension",
                        )
                    mistakes =
                        Immutable.list(
                            "Overarching the lower back",
                            "Losing stable shoulder or foot support",
                            "Bouncing through the bottom",
                        )
                }
                "carry" -> {
                    cues =
                        Immutable.list(
                            "Stand tall without excessive leaning",
                            "Use controlled steps",
                            "Set the loads down deliberately",
                        )
                    mistakes =
                        Immutable.list(
                            "Leaning heavily to one side",
                            "Rushing with unstable steps",
                            "Losing grip or posture to extend the distance",
                        )
                }
                "curl" -> {
                    cues =
                        Immutable.list(
                            "Adjust the pad and pivot to your leg length",
                            "Keep the hips supported",
                            "Control both directions",
                        )
                    mistakes =
                        Immutable.list(
                            "Lifting the hips to move the load",
                            "Swinging the lower legs",
                            "Using an uncomfortable machine setup",
                        )
                }
                else -> {
                    cues =
                        Immutable.list(
                            "Use a stable support",
                            "Pause and control the lowering",
                            "Choose a comfortable range",
                        )
                    mistakes =
                        Immutable.list(
                            "Bouncing instead of controlled movement",
                            "Losing balance",
                            "Using a load that changes the intended action",
                        )
                }
            }
            if ((id == "deadlift") || (id == "trap-bar-deadlift")) {
                cues =
                    Immutable.list(
                        "Brace before lifting",
                        "Keep the whole foot stable",
                        "Extend hips and knees together",
                        "Lower under control",
                    )
                mistakes =
                    Immutable.list(
                        "Jerking the load from the floor",
                        "Leaning backwards at the finish",
                        "Letting the back position change uncontrollably",
                    )
                if ((id == "deadlift"))
                    setup =
                        "Place the bar over your mid-foot close to the shins; bend hips and knees, grip outside your legs and brace before lifting."
            }
            if ((id == "back-squat"))
                setup =
                    "Set suitable rack safeties, support the bar across the upper back rather than the neck, grip securely and take a stable stance before bending."
            if ((id == "dumbbell-shoulder-press"))
                setup =
                    "Sit against an upright bench with feet planted and a dumbbell at each shoulder; keep the trunk supported and brace gently."
            if ((id == "front-squat"))
                setup =
                    "Set suitable rack safeties, support the bar across the front of your shoulders with elbows pointing forwards and hands steadying it; choose a grip you can maintain without discomfort."
            if ((id == "calf-raise") || (id == "standing-calf-raise"))
                setup =
                    "Adjust the machine shoulder pads, support your forefeet on its platform and use the handles for balance; choose a comfortable ankle range."
            if ((id == "kettlebell-swing")) {
                cues =
                    Immutable.list(
                        "Brace with ribs controlled",
                        "Drive the swing with hip extension",
                        "Let the bell float to around chest height",
                        "Guide the returning bell into a hinge",
                    )
                mistakes =
                    Immutable.list(
                        "Lifting the bell mainly with the arms",
                        "Leaning backwards at the top",
                        "Turning the swing into a deep squat",
                        "Using a bell you cannot stop safely",
                    )
            }
            if ((id == "pull-up"))
                cues =
                    Immutable.list(
                        "Grip a secure bar",
                        "Keep your trunk and legs controlled",
                        "Lower your body smoothly without swinging",
                    )
            return TechniqueGuide(pattern, setup, movement, cues, mistakes, muscles, alternatives)
        }
    }
}
