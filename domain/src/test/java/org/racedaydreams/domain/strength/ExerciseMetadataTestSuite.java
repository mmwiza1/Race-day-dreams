package org.racedaydreams.domain.strength;

import static org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Equipment.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Role.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Suitability.*;

import org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea;
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment;
import org.racedaydreams.domain.strength.ExerciseMetadata.EquipmentRequirement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Catalogue completeness plus concrete training-intent and complete-equipment regressions. */
public final class ExerciseMetadataTestSuite {
    private static int checks;

    public static void main(String[] args) {
        completeCanonicalCoverage();
        preserveDefinitionsAndHistoryContracts();
        movementRolesDescribeTheActualTask();
        bodyAreasAreBroadAndDistinct();
        equipmentAlternativesRequireCompleteSetups();
        bodyweightDoesNotMeanEquipmentFree();
        machinesAreSpecificCapabilities();
        experienceClassificationsMatter();
        metadataIsDefensivelyImmutable();
        unknownAndMalformedRemainUnknownOrRejected();
        System.out.println(
                "ExerciseMetadataTestSuite: "
                        + checks
                        + " checks passed; "
                        + ExerciseMetadataCatalog.entries().size()
                        + " canonical metadata entries.");
    }

    private static void completeCanonicalCoverage() {
        var expected = new HashSet<String>();
        for (var definition : ExerciseCatalog.definitions()) {
            String id = definition.exercise().id();
            expected.add(id);
            var value = ExerciseMetadataCatalog.require(id);
            ok(id.equals(value.exerciseId()), "Metadata retains exact canonical identity: " + id);
            ok(
                    value == ExerciseMetadataCatalog.find(id),
                    "Canonical lookup shares an immutable cached entry");
            ok(
                    value.roles().contains(value.primaryRole()),
                    "Primary movement role is explicit: " + id);
            ok(!value.primaryAreas().isEmpty(), "Primary body areas are explicit: " + id);
            ok(
                    Collections.disjoint(value.primaryAreas(), value.secondaryAreas()),
                    "Areas are not double classified: " + id);
            ok(
                    value.suitability() != null && value.equipment() != null,
                    "Experience and equipment are present: " + id);
            ok(
                    !value.equipment().alternatives().isEmpty(),
                    "At least one reviewed complete setup: " + id);
            ok(
                    TechniqueGuide.forExercise(id) != null,
                    "Metadata does not replace required movement guidance: " + id);
            for (var setup : value.equipment().alternatives()) {
                ok(
                        value.equipment().availableWith(setup),
                        "Each complete setup is sufficient: " + id);
                var expanded = EnumSet.allOf(Equipment.class);
                ok(
                        value.equipment().availableWith(expanded),
                        "Additional equipment never prevents eligibility: " + id);
                for (var item : setup) {
                    var incomplete = EnumSet.noneOf(Equipment.class);
                    incomplete.addAll(setup);
                    incomplete.remove(item);
                    boolean expectedAvailable =
                            value.equipment().alternatives().stream()
                                    .anyMatch(incomplete::containsAll);
                    ok(
                            value.equipment().availableWith(incomplete) == expectedAvailable,
                            "Removing required equipment respects complete alternatives: "
                                    + id
                                    + "/"
                                    + item);
                }
            }
        }
        ok(
                expected.equals(ExerciseMetadataCatalog.entries().keySet()),
                "Registry covers every canonical ID, with no extra or stale entries");
        ok(
                expected.size() == ExerciseMetadataCatalog.entries().size(),
                "Count derives from current catalogue");
        ok(ExerciseMetadataCatalog.VERSION == 1, "Static taxonomy has an explicit version");
    }

    private static void preserveDefinitionsAndHistoryContracts() {
        var canonical = ExerciseCatalog.exercises();
        for (var exercise : canonical) {
            var before = ExerciseCatalog.definition(exercise.id());
            ExerciseMetadataCatalog.require(exercise.id());
            ok(
                    before.exercise().equals(exercise),
                    "Metadata does not replace historical measurement definition");
            ok(
                    before == ExerciseCatalog.definition(exercise.id()),
                    "Lookup does not rewrite canonical definitions");
        }
        ok(
                ExerciseCatalog.definition("weighted-pull-up").exercise().mode()
                        == StrengthExercise.Mode.WEIGHTED_BODYWEIGHT,
                "Weighted bodyweight evidence remains weighted bodyweight");
        ok(
                ExerciseCatalog.definition("farmers-carry").exercise().measurement()
                        == StrengthExercise.MeasurementProfile.LOAD_DISTANCE,
                "Carry logging retains load and distance");
        ok(
                ExerciseCatalog.definition("weighted-plank").exercise().measurement()
                        == StrengthExercise.MeasurementProfile.LOAD_DURATION,
                "Timed loaded work is not converted to repetitions");
        ok(
                ExerciseCatalog.definition("plank").exercise().measurement()
                        == StrengthExercise.MeasurementProfile.DURATION,
                "Static hold retains duration measurement");
    }

    private static void movementRolesDescribeTheActualTask() {
        role("back-squat", SQUAT, PRIMARY_STRENGTH);
        role("single-leg-press", SQUAT, UNILATERAL);
        role("leg-extension", ISOLATION);
        notRole("leg-extension", SQUAT);
        role("romanian-deadlift", HINGE, PRIMARY_STRENGTH);
        role("hip-thrust", HIP_EXTENSION, PRIMARY_STRENGTH);
        role("split-squat", UNILATERAL);
        role("lying-leg-curl", HAMSTRING, ISOLATION);
        role("standing-leg-curl", HAMSTRING, UNILATERAL);
        role("bench-press", HORIZONTAL_PUSH, PRIMARY_STRENGTH);
        role("dumbbell-row", HORIZONTAL_PULL, UNILATERAL);
        role("pull-up", VERTICAL_PULL, PRIMARY_STRENGTH);
        role("overhead-press", VERTICAL_PUSH, PRIMARY_STRENGTH);
        role("pallof-press", CORE);
        notRole("pallof-press", HORIZONTAL_PUSH);
        role("push-press", POWER, VERTICAL_PUSH);
        role("kettlebell-swing", POWER, HINGE);
        role("thruster", POWER, SQUAT, VERTICAL_PUSH);
        role("kettlebell-goblet-squat", SQUAT);
        notRole("kettlebell-goblet-squat", POWER);
        role("suitcase-carry", CARRY, UNILATERAL);
        role("dead-hang", GRIP);
        notRole("dead-hang", VERTICAL_PULL);
        role("wall-sit", SQUAT);
        notRole("wall-sit", CORE);
        role("sled-push", CONDITIONING);
        notRole("sled-push", CARRY);
        role("upright-row", ISOLATION);
        notRole("upright-row", VERTICAL_PULL);
        for (String id : List.of("barbell-curl", "hammer-curl", "triceps-pressdown", "cable-fly")) {
            ok(
                    !ExerciseMetadataCatalog.require(id).roles().contains(PRIMARY_STRENGTH),
                    "An isolation exercise is not a main compound merely because it has load and"
                            + " reps");
        }
    }

    private static void bodyAreasAreBroadAndDistinct() {
        ok(
                ExerciseMetadataCatalog.require("romanian-deadlift")
                        .primaryAreas()
                        .equals(Set.of(HAMSTRINGS, GLUTES)),
                "RDL has explicit posterior-chain primary involvement");
        ok(
                ExerciseMetadataCatalog.require("bench-press").primaryAreas().equals(Set.of(CHEST)),
                "Bench has chest primary involvement");
        ok(
                ExerciseMetadataCatalog.require("bench-press")
                        .secondaryAreas()
                        .equals(Set.of(TRICEPS, SHOULDERS)),
                "Bench supporting muscles are separate from primary involvement");
        ok(
                ExerciseMetadataCatalog.require("face-pull")
                        .primaryAreas()
                        .equals(Set.of(UPPER_BACK, SHOULDERS)),
                "Face pull is not classified as a lat-focused pull simply because legacy category"
                        + " is Back");
        ok(
                ExerciseMetadataCatalog.require("tibialis-raise")
                        .primaryAreas()
                        .equals(Set.of(LOWER_LEG)),
                "Tibialis work is not incorrectly called calf muscle work");
        ok(
                ExerciseMetadataCatalog.require("wall-sit").primaryAreas().contains(QUADS),
                "Wall sit classification corrects legacy Core grouping without modifying historical"
                        + " definition");
        ok(
                ExerciseMetadataCatalog.require("plank").primaryAreas().equals(Set.of(TRUNK)),
                "Core uses a useful broad trunk category");
    }

    private static void equipmentAlternativesRequireCompleteSetups() {
        available("goblet-squat", true, DUMBBELLS);
        available("goblet-squat", true, KETTLEBELLS);
        available("goblet-squat", false, BARBELL);
        available("hip-thrust", true, BARBELL, BENCH);
        available("hip-thrust", true, DUMBBELLS, BENCH);
        available("hip-thrust", false, BARBELL);
        available("hip-thrust", false, BENCH);
        available("split-squat", true, DUMBBELLS, BENCH);
        available("split-squat", true, BARBELL, BENCH, SQUAT_RACK);
        available("split-squat", false, DUMBBELLS);
        available("split-squat", false, BARBELL, BENCH);
        available("bench-press", false, BARBELL);
        available("bench-press", false, BARBELL, BENCH);
        available("bench-press", true, BARBELL, BENCH, SQUAT_RACK);
        available("incline-dumbbell-press", false, DUMBBELLS, BENCH);
        available("incline-dumbbell-press", true, DUMBBELLS, ADJUSTABLE_BENCH);
        available("step-up", false, DUMBBELLS);
        available("step-up", true, DUMBBELLS, STABLE_STEP);
        available("deficit-deadlift", false, BARBELL);
        available("deficit-deadlift", true, BARBELL, DEFICIT_PLATFORM);
        available("rack-pull", false, BARBELL);
        available("rack-pull", true, BARBELL, SQUAT_RACK);
        available("block-pull", false, BARBELL, SQUAT_RACK);
        available("block-pull", true, BARBELL, LIFTING_BLOCKS);
        available("landmine-press", false, BARBELL);
        available("landmine-press", true, BARBELL, LANDMINE_ANCHOR);
        available("rope-pressdown", false, CABLE_STATION);
        available("rope-pressdown", true, CABLE_STATION, CABLE_ROPE);
        available("weighted-pull-up", false, PULL_UP_BAR, DIP_BELT);
        available("weighted-pull-up", true, PULL_UP_BAR, DIP_BELT, WEIGHT_PLATE);
        available("swiss-ball-leg-curl", false);
        available("swiss-ball-leg-curl", true, EXERCISE_BALL);
        available("sliding-leg-curl", false, SLIDERS);
        available("sliding-leg-curl", true, SLIDERS, SLIDING_SURFACE);
        available("sled-pull", false, SLED);
        available("sled-pull", true, SLED, SLED_PULL_STRAP);
        available("ab-wheel", false);
        available("ab-wheel", true, AB_WHEEL);
    }

    private static void bodyweightDoesNotMeanEquipmentFree() {
        for (String id :
                List.of("pull-up", "chin-up", "hanging-knee-raise", "toes-to-bar", "dead-hang")) {
            available(id, false);
            available(id, true, PULL_UP_BAR);
        }
        available("neutral-grip-pull-up", false, PULL_UP_BAR);
        available("neutral-grip-pull-up", true, NEUTRAL_GRIP_BAR);
        available("dip", false);
        available("dip", true, PARALLEL_BARS);
        available("bench-dip", false);
        available("bench-dip", true, BENCH);
        available("inverted-row", false);
        available("inverted-row", true, SECURE_LOW_BAR);
        available("nordic-hamstring-curl", false);
        available("nordic-hamstring-curl", true, SECURE_FOOT_ANCHOR);
        available("wall-sit", false);
        available("wall-sit", true, WALL);
        for (String id :
                List.of(
                        "push-up",
                        "bodyweight-squat",
                        "plank",
                        "side-plank",
                        "dead-bug",
                        "bird-dog",
                        "glute-bridge",
                        "reverse-lunge-bodyweight",
                        "single-leg-calf-raise-bodyweight")) {
            ok(
                    ExerciseMetadataCatalog.require(id).equipment().equipmentFree(),
                    "No external equipment is required: " + id);
            available(id, true);
        }
    }

    private static void machinesAreSpecificCapabilities() {
        available("smith-machine-squat", false, HACK_SQUAT_MACHINE);
        available("smith-machine-squat", true, SMITH_MACHINE);
        available("smith-machine-bench-press", false, SMITH_MACHINE);
        available("smith-machine-bench-press", true, SMITH_MACHINE, BENCH);
        available("leg-extension", false, LEG_PRESS_MACHINE);
        available("leg-extension", true, LEG_EXTENSION_MACHINE);
        available("lying-leg-curl", false, SEATED_LEG_CURL_MACHINE);
        available("lying-leg-curl", true, LYING_LEG_CURL_MACHINE);
        available("machine-chest-press", false, SHOULDER_PRESS_MACHINE);
        available("machine-chest-press", true, CHEST_PRESS_MACHINE);
        available("reverse-pec-deck", false, PEC_DECK_MACHINE);
        available("reverse-pec-deck", true, REVERSE_PEC_DECK_MACHINE);
        available("glute-ham-raise", false, LEG_PRESS_MACHINE);
        available("glute-ham-raise", true, GLUTE_HAM_DEVELOPER);
        ok(
                SMITH_MACHINE.group() == ExerciseMetadata.EquipmentGroup.SMITH_MACHINE,
                "Smith browsing is distinct from generic machines");
        ok(
                CABLE_STATION.group() == ExerciseMetadata.EquipmentGroup.CABLES,
                "Cable equipment has a useful browser category");
    }

    private static void experienceClassificationsMatter() {
        for (String id :
                List.of(
                        "power-clean",
                        "snatch",
                        "clean-jerk",
                        "split-jerk",
                        "pistol-squat",
                        "nordic-hamstring-curl",
                        "toes-to-bar",
                        "good-morning",
                        "jm-press")) {
            var suitability = ExerciseMetadataCatalog.require(id).suitability();
            ok(suitability == TECHNICAL, "Technical variation is explicitly classified: " + id);
            ok(
                    !suitability.appropriateFor(GymProgramme.Experience.BEGINNER),
                    "Not an automatic beginner default: " + id);
            ok(
                    !suitability.appropriateFor(GymProgramme.Experience.INTERMEDIATE),
                    "Technical choice needs experienced selection: " + id);
            ok(
                    suitability.appropriateFor(GymProgramme.Experience.EXPERIENCED),
                    "Technical does not mean prohibited: " + id);
        }
        for (String id :
                List.of(
                        "goblet-squat",
                        "machine-chest-press",
                        "dumbbell-row",
                        "plank",
                        "glute-bridge")) {
            ok(
                    ExerciseMetadataCatalog.require(id).suitability() == BEGINNER_FRIENDLY,
                    "Simple stable variation is available as a beginner choice: " + id);
        }
        ok(
                !INTERMEDIATE.appropriateFor(GymProgramme.Experience.BEGINNER),
                "Intermediate does not collapse to beginner");
        ok(
                INTERMEDIATE.appropriateFor(GymProgramme.Experience.INTERMEDIATE),
                "Intermediate variation is supported");
        ok(
                INTERMEDIATE.appropriateFor(GymProgramme.Experience.EXPERIENCED),
                "Experienced users retain simpler choices");
    }

    private static void metadataIsDefensivelyImmutable() {
        var roles = EnumSet.of(CORE);
        var primary = EnumSet.of(TRUNK);
        var secondary = EnumSet.noneOf(BodyArea.class);
        var setup = EnumSet.of(WEIGHT_PLATE);
        var alternatives = new ArrayList<Set<Equipment>>();
        alternatives.add(setup);
        var requirement = new EquipmentRequirement(alternatives);
        var metadata =
                new ExerciseMetadata(
                        "custom-test", CORE, roles, primary, secondary, INTERMEDIATE, requirement);
        roles.add(POWER);
        primary.add(QUADS);
        secondary.add(SHOULDERS);
        setup.clear();
        alternatives.clear();
        ok(metadata.roles().equals(Set.of(CORE)), "Role input is copied");
        ok(metadata.primaryAreas().equals(Set.of(TRUNK)), "Primary areas are copied");
        ok(metadata.secondaryAreas().isEmpty(), "Secondary areas are copied");
        ok(metadata.equipment().availableWith(Set.of(WEIGHT_PLATE)), "Equipment setup is copied");
        ok(
                !metadata.equipment().equipmentFree(),
                "Mutating setup input cannot remove the requirement");
        immutable(() -> metadata.roles().clear());
        immutable(() -> metadata.primaryAreas().clear());
        immutable(() -> metadata.equipment().alternatives().clear());
        immutable(() -> metadata.equipment().alternatives().get(0).clear());
        immutable(() -> ExerciseMetadataCatalog.entries().clear());
    }

    private static void unknownAndMalformedRemainUnknownOrRejected() {
        ok(ExerciseMetadataCatalog.find(null) == null, "Null lookup does not invent metadata");
        ok(
                ExerciseMetadataCatalog.find("custom-retained-id") == null,
                "Custom history is not guessed from an ID");
        rejects(() -> ExerciseMetadataCatalog.require("custom-retained-id"));
        rejects(() -> ExerciseMetadataCatalog.require(null));
        rejects(() -> new EquipmentRequirement(Collections.emptyList()));
        rejects(() -> new EquipmentRequirement(null));
        rejects(() -> new EquipmentRequirement(Arrays.asList(Set.of(BARBELL), Set.of(BARBELL))));
        rejects(
                () ->
                        new EquipmentRequirement(
                                Arrays.asList(Collections.emptySet(), Set.of(BARBELL))));
        rejects(() -> new EquipmentRequirement(Collections.singletonList(null)));
        rejects(
                () ->
                        new EquipmentRequirement(
                                Collections.singletonList(
                                        new HashSet<>(Arrays.asList(BARBELL, null)))));
        var valid = ExerciseMetadataCatalog.require("plank");
        rejects(
                () ->
                        new ExerciseMetadata(
                                "Bad ID",
                                CORE,
                                Set.of(CORE),
                                Set.of(TRUNK),
                                Set.of(),
                                BEGINNER_FRIENDLY,
                                valid.equipment()));
        rejects(
                () ->
                        new ExerciseMetadata(
                                "valid-id",
                                CORE,
                                Set.of(POWER),
                                Set.of(TRUNK),
                                Set.of(),
                                BEGINNER_FRIENDLY,
                                valid.equipment()));
        rejects(
                () ->
                        new ExerciseMetadata(
                                "valid-id",
                                CORE,
                                Set.of(CORE),
                                Set.of(),
                                Set.of(),
                                BEGINNER_FRIENDLY,
                                valid.equipment()));
        rejects(
                () ->
                        new ExerciseMetadata(
                                "valid-id",
                                CORE,
                                Set.of(CORE),
                                Set.of(TRUNK),
                                Set.of(TRUNK),
                                BEGINNER_FRIENDLY,
                                valid.equipment()));
        rejects(
                () ->
                        new ExerciseMetadata(
                                "valid-id",
                                CORE,
                                null,
                                Set.of(TRUNK),
                                Set.of(),
                                BEGINNER_FRIENDLY,
                                valid.equipment()));
        rejects(() -> valid.equipment().availableWith(new HashSet<>(Arrays.asList(BARBELL, null))));
    }

    private static void role(
            String id, ExerciseMetadata.Role primary, ExerciseMetadata.Role... others) {
        var value = ExerciseMetadataCatalog.require(id);
        ok(value.primaryRole() == primary, "Actual primary movement role: " + id);
        for (var other : others)
            ok(value.roles().contains(other), "Additional intended role: " + id + "/" + other);
    }

    private static void notRole(String id, ExerciseMetadata.Role role) {
        ok(
                !ExerciseMetadataCatalog.require(id).roles().contains(role),
                "No unsupported role: " + id + "/" + role);
    }

    private static void available(String id, boolean expected, Equipment... items) {
        var equipment = EnumSet.noneOf(Equipment.class);
        equipment.addAll(Arrays.asList(items));
        ok(
                ExerciseMetadataCatalog.require(id).equipment().availableWith(equipment)
                        == expected,
                "Complete equipment availability for " + id + " with " + equipment);
    }

    private static void immutable(Runnable mutation) {
        try {
            mutation.run();
            throw new AssertionError("Mutable metadata escaped");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }

    private static void rejects(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("Invalid metadata was accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void ok(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
