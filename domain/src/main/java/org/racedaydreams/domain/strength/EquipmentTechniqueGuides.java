package org.racedaydreams.domain.strength;

import java.util.List;

/** Original equipment-specific guidance for carries, pulls and accessory movements. */
final class EquipmentTechniqueGuides {
    private EquipmentTechniqueGuides() {}
    static TechniqueGuide forExercise(String id) {
        return switch(id) {
            case "dumbbell-fly" -> g("fly","Lie on a stable flat bench, feet planted, holding manageable dumbbells above the chest with soft elbows.","Open the arms in a controlled arc through a comfortable chest range, then bring the weights back together.","Chest and front shoulders","Keep a soft elbow bend|Use a comfortable depth|Start lighter than a press","Overstretching the shoulders|Turning it into a press|Dropping the weights","Cable Fly|Pec Deck");
            case "meadows-row" -> g("row","Secure a landmine bar; stand side-on with hips hinged, free hand braced on the thigh and working hand gripping the loaded sleeve.","Row the loaded end towards the hip, then lower smoothly without rotating the trunk; change sides.","Upper back, lats and grip","Check the anchor|Brace the trunk|Use a manageable sleeve grip","An unsecured bar|Twisting to lift|Heaving with the hips","Dumbbell Row|T-Bar Row");
            case "kettlebell-goblet-squat" -> g("squat","Hold one kettlebell securely by its horns close to the chest and stand with feet comfortably apart.","Squat through a controlled depth with heels supported, then stand while keeping the bell close.","Quadriceps and glutes","Keep the bell close|Let knees follow toes|Use a controlled depth","Holding the bell far away|Heels lifting|Dropping into the bottom","Goblet Squat|Leg Press");
            case "front-rack-carry" -> g("carry","Bring two manageable kettlebells into a secure front rack at the shoulders with wrists comfortable and elbows close.","Walk with short controlled steps, breathing steadily; set the bells down safely when posture or grip becomes difficult.","Trunk, upper back and legs","Keep ribs controlled|Use a clear path|Keep both bells secure","Leaning backwards|Holding the breath|Walking beyond grip control","Farmer's Carry|Suitcase Carry");
            default -> PowerTechniqueGuides.forExercise(id);
        };
    }
    private static TechniqueGuide g(String pattern,String setup,String movement,String muscles,String cues,String mistakes,String alternatives) {
        return new TechniqueGuide(pattern,setup,movement,List.of(cues.split("\\|")),List.of(mistakes.split("\\|")),muscles,List.of(alternatives.split("\\|")));
    }
}
