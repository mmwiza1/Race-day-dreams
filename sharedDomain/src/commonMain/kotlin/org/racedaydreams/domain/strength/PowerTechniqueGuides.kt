package org.racedaydreams.domain.strength

/** Original movement-specific catalogue copy; unknown movements do not inherit guidance. */
internal object PowerTechniqueGuides {
    fun forExercise(id: String): TechniqueGuide? {

        return when (id) {
            "power-clean" ->
                g(
                    "power",
                    "Start with a light floor-supported bar, grip outside the legs and brace with hips and knees bent.",
                    "Drive the floor away, extend hips and knees, then receive the bar across the front shoulders in a shallow squat; stand and reset safely.",
                    "Legs, hips, upper back and trunk",
                    "Learn the receiving position first|Keep the bar close|Use light coached practice",
                    "Pulling early with the arms|Catching on the wrists|Using load before technique is controlled",
                    "Hang Clean|High Pull",
                )
            "clean" ->
                g(
                    "power",
                    "Practise with a light bar and qualified coaching; start from the floor with a secure grip outside the legs.",
                    "Drive through the legs and hips, then move under the bar into a controlled front-squat catch with elbows forwards; stand before resetting.",
                    "Legs, hips, upper back and trunk",
                    "Keep the bar close|Practise a stable front rack|Use only a controlled receiving depth",
                    "Catching on the wrists|Rounding to lift|Loading beyond receiving control",
                    "Power Clean|Front Squat",
                )
            "hang-clean" ->
                g(
                    "power",
                    "Hold a light bar above the knees with hips hinged and grip outside the legs; establish a stable front rack before practising.",
                    "Extend hips and knees, guide the bar close and receive it on the front shoulders with knees bent; stand and reset.",
                    "Legs, hips, upper back and trunk",
                    "Keep the bar close|Drive with the hips and legs|Receive with elbows forwards",
                    "Swinging the bar away|Pulling mainly with arms|Catching on straight legs",
                    "Power Clean|High Pull",
                )
            "clean-jerk" ->
                g(
                    "power",
                    "Use light coached practice and a clear lifting area; start over the bar with hips and knees bent and a secure grip.",
                    "Clean the bar to the front shoulders and stand; dip and drive to receive it overhead in a stable split, then recover the feet deliberately.",
                    "Legs, hips, shoulders, triceps and trunk",
                    "Learn each phase separately|Keep the bar controlled|Use a stable overhead position",
                    "Rushing an unstable clean into a jerk|Losing trunk control|Loading before technique is reliable",
                    "Power Clean|Push Press",
                )
            "power-snatch" ->
                g(
                    "power",
                    "Use a light bar and coached practice; start at the floor with a comfortable wide grip and braced trunk.",
                    "Drive through the legs and hips, then receive the bar overhead in a shallow squat with arms extended; stand and reset safely.",
                    "Legs, hips, shoulders, upper back and trunk",
                    "Practise the overhead position first|Keep the bar close|Use a manageable wide grip",
                    "Swinging the bar far forwards|Catching without control|Adding load before stable receiving",
                    "Hang Snatch|High Pull",
                )
            "snatch" ->
                g(
                    "power",
                    "Learn with a light bar and qualified coaching; take a comfortable wide grip with hips and knees bent over the floor-supported bar.",
                    "Extend through the legs and hips, then move under the bar into a controlled overhead squat; stand with the bar stable before resetting.",
                    "Legs, hips, shoulders, upper back and trunk",
                    "Keep the bar close|Use only a stable overhead depth|Practise with light load",
                    "Forcing shoulder range|Dropping into an unstable catch|Loading before technique is controlled",
                    "Power Snatch|High Pull",
                )
            "hang-snatch" ->
                g(
                    "power",
                    "Hold a light bar above the knees with a comfortable wide grip, hips hinged and trunk braced.",
                    "Drive through hips and knees, guide the bar close and receive it overhead with knees bent; stand and reset under control.",
                    "Legs, hips, shoulders and trunk",
                    "Learn the overhead catch first|Keep the bar close|Use light coached practice",
                    "Swinging the bar away|Forcing overhead range|Catching without balance",
                    "Power Snatch|High Pull",
                )
            "high-pull" ->
                g(
                    "power",
                    "Hold a light bar above the knees with hips hinged, softly bent knees and a comfortable wider grip.",
                    "Extend hips and knees, then guide elbows out as the bar rises near the lower chest; lower and reset without catching it overhead.",
                    "Hips, legs, upper back and shoulders",
                    "Drive with the legs and hips|Keep the bar close|Use a comfortable height",
                    "Yanking with the arms first|Leaning backwards|Forcing the elbows too high",
                    "Hang Clean|Kettlebell Swing",
                )
            "push-jerk" ->
                g(
                    "power",
                    "Hold a light bar across the front shoulders with feet stable; practise the overhead receiving position first.",
                    "Dip upright, drive through the legs and receive the bar overhead with a shallow knee bend; stand before lowering safely.",
                    "Legs, shoulders, triceps and trunk",
                    "Keep the dip upright|Receive with stable feet|Use light coached practice",
                    "Tipping forwards in the dip|Catching with loose elbows|Losing trunk control",
                    "Push Press|Overhead Press",
                )
            "split-jerk" ->
                g(
                    "power",
                    "Hold a light bar in a secure front rack; leave clear space for a stable split stance and learn the footwork first.",
                    "Dip upright and drive, receiving the bar overhead with feet split; recover the feet in small deliberate steps before lowering.",
                    "Legs, shoulders, triceps and trunk",
                    "Practise foot placement|Keep ribs controlled|Use a secure overhead position",
                    "A narrow unstable split|Rushing the recovery|Loading beyond balance",
                    "Push Jerk|Push Press",
                )
            "push-press" ->
                g(
                    "power",
                    "Hold a manageable bar across the front shoulders with feet stable and trunk braced.",
                    "Use a short upright dip and leg drive to press overhead; finish standing tall without a second knee dip, then lower smoothly.",
                    "Legs, shoulders and triceps",
                    "Keep the dip upright|Drive before pressing|Keep ribs controlled",
                    "Leaning backwards|Dipping too deeply|Re-bending knees into a jerk",
                    "Overhead Press|Dumbbell Shoulder Press",
                )
            "american-kettlebell-swing" ->
                g(
                    "power",
                    "Use a light kettlebell with a secure two-hand grip and clear space overhead; choose a lower swing if overhead range is uncomfortable.",
                    "Hinge hips back, drive them forwards and allow the bell to travel overhead with a controlled trunk; guide it back into the hinge.",
                    "Hips, hamstrings, shoulders and trunk",
                    "Drive from the hips|Keep ribs controlled|Use a comfortable overhead range",
                    "Squatting instead of hinging|Overarching at the top|Pulling a heavy bell overhead with the arms",
                    "Kettlebell Swing|Romanian Deadlift",
                )
            else -> FunctionalTechniqueGuides.forExercise(id)
        }
    }

    private fun g(
        pattern: String,
        setup: String,
        movement: String,
        muscles: String,
        cues: String,
        mistakes: String,
        alternatives: String,
    ): TechniqueGuide =
        TechniqueGuide(
            pattern,
            setup,
            movement,
            techniqueWords(cues),
            techniqueWords(mistakes),
            muscles,
            techniqueWords(alternatives),
        )
}
