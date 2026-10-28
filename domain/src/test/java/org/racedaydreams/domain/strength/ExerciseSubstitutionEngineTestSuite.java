package org.racedaydreams.domain.strength;

import static org.racedaydreams.domain.strength.ExerciseMetadata.Equipment.*;
import static org.racedaydreams.domain.strength.ExerciseSubstitutionEngine.Reason.*;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.ExerciseMetadata.Equipment;
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine.Context;
import org.racedaydreams.domain.strength.ExerciseSubstitutionEngine.Reason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Real canonical metadata, not regional/name matching or invented equivalent loads. */
public final class ExerciseSubstitutionEngineTestSuite {
    private static int checks;

    public static void main(String[] args) {
        intentIsMoreSpecificThanRegion();
        busyEquipmentOffersDifferentCompleteSetups();
        capabilityRequirementsAreCompleteAndSpecific();
        measurementFamiliesAreHonest();
        easierAndHarderNeedReviewedEvidenceAndExperience();
        exclusionsFavouritesAndHistoryStayWithinTrainingIntent();
        discomfortDoesNotClaimMedicalSuitability();
        immutableInputsAndBoundedResults();
        qualityMatrixAcrossTheCanonicalCatalogue();
        unknownMetadataNeverInventsAlternatives();
        System.out.println("ExerciseSubstitutionEngineTestSuite: " + checks + " checks passed.");
    }

    private static void intentIsMoreSpecificThanRegion() {
        var context = context("bench-press", allEquipment(), ALTERNATIVES);
        var results = ExerciseSubstitutionEngine.rank(context, 50);
        ok(
                contains(results, "dumbbell-bench-press"),
                "A matching dumbbell horizontal push is offered");
        ok(
                contains(results, "machine-chest-press"),
                "A complete matching machine setup is offered");
        ok(
                contains(results, "push-up"),
                "Bodyweight horizontal pushing can be an explicit rep-task option");
        ok(
                !contains(results, "barbell-row"),
                "Being upper body does not make a row a bench substitute");
        ok(
                !contains(results, "overhead-press"),
                "Vertical pushing does not silently replace a horizontal push role");
        ok(
                !contains(results, "cable-fly"),
                "Chest isolation does not silently replace the compound's role");
        var curls =
                ExerciseSubstitutionEngine.rank(
                        context("barbell-curl", allEquipment(), ALTERNATIVES), 50);
        ok(contains(curls, "dumbbell-curl"), "A same-role biceps isolation is offered");
        ok(
                !contains(curls, "triceps-pressdown"),
                "The broad isolation role still requires primary area overlap");
        var hinge =
                ExerciseSubstitutionEngine.rank(
                        context("romanian-deadlift", allEquipment(), ALTERNATIVES), 50);
        ok(
                !contains(hinge, "hip-thrust"),
                "Glute involvement alone is not enough to replace a hinge");
    }

    private static void busyEquipmentOffersDifferentCompleteSetups() {
        var available = Immutable.set(BARBELL, SQUAT_RACK, BENCH, DUMBBELLS, CHEST_PRESS_MACHINE);
        var alternatives =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", available, EQUIPMENT_BUSY), 4);
        equal(
                4,
                alternatives.size(),
                "Busy flow is bounded to four genuine options when available");
        equal(
                "machine-chest-press",
                alternatives.get(0).exercise().id(),
                "A different machine setup avoids the busy barbell/bench/rack first");
        equal(
                "push-up",
                alternatives.get(1).exercise().id(),
                "Equipment-free fallback outranks more shared setup");
        equal(
                "dumbbell-bench-press",
                alternatives.get(2).exercise().id(),
                "Dumbbell option shares only the bench and explains that limitation");
        for (var suggestion : alternatives) {
            ok(
                    suggestion.explanation().contains("Check that this setup is free"),
                    "Busy is not a guarantee of free equipment");
            ok(
                    suggestion.explanation().contains("shared equipment may still be busy"),
                    "A shared bench remains an explicit limitation");
            ok(
                    suggestion.explanation().contains("Uses "),
                    "Every suggestion identifies its complete equipment setup");
        }
        var filtered =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", Collections.emptySet(), EQUIPMENT_UNAVAILABLE), 4);
        equal(
                1,
                filtered.size(),
                "No equipment does not manufacture a list of unsuitable alternatives");
        equal(
                "push-up",
                filtered.get(0).exercise().id(),
                "Only the real equipment-free horizontal push is offered");
        ok(
                filtered.get(0).measurementChanged(),
                "Load-and-reps to bodyweight reps is explicitly flagged");
    }

    private static void capabilityRequirementsAreCompleteAndSpecific() {
        var dumbbellsOnly =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", Immutable.set(DUMBBELLS), ALTERNATIVES), 50);
        ok(
                !contains(dumbbellsOnly, "dumbbell-bench-press"),
                "Owning dumbbells does not invent a bench");
        var complete =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", Immutable.set(DUMBBELLS, BENCH), ALTERNATIVES), 50);
        ok(
                contains(complete, "dumbbell-bench-press"),
                "Both parts of the taught dumbbell setup are required");
        ok(
                !contains(complete, "incline-dumbbell-press"),
                "A flat bench does not invent an adjustable bench");
        var wrongMachine =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", Immutable.set(LEG_PRESS_MACHINE), ALTERNATIVES), 50);
        ok(
                !contains(wrongMachine, "machine-chest-press"),
                "One machine is not a wildcard for a different machine");
        var rightMachine =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", Immutable.set(CHEST_PRESS_MACHINE), ALTERNATIVES),
                        50);
        ok(
                contains(rightMachine, "machine-chest-press"),
                "The specifically selected capability is respected");
        var goblet =
                ExerciseSubstitutionEngine.rank(
                        context("back-squat", Immutable.set(KETTLEBELLS), ALTERNATIVES), 50);
        ok(
                contains(goblet, "goblet-squat"),
                "A complete alternative kettlebell setup satisfies OR-of-AND metadata");
        ok(!contains(goblet, "front-squat"), "A kettlebell cannot imply a barbell and rack");
    }

    private static void measurementFamiliesAreHonest() {
        for (var original : StrengthExercise.MeasurementProfile.values())
            for (var replacement : StrengthExercise.MeasurementProfile.values()) {
                boolean expected =
                        original.usesReps() && replacement.usesReps()
                                || original.usesDuration() && replacement.usesDuration()
                                || original.usesDistance() && replacement.usesDistance();
                equal(
                        expected,
                        ExerciseSubstitutionEngine.compatibleTask(original, replacement),
                        "A shared region cannot change the actual measurement task");
            }
        var plank =
                ExerciseSubstitutionEngine.rank(context("plank", allEquipment(), ALTERNATIVES), 50);
        ok(contains(plank, "side-plank"), "Another retained timed core task can be offered");
        ok(
                !contains(plank, "dead-bug"),
                "Repetition-based core does not inherit a duration prescription");
        var bench =
                ExerciseSubstitutionEngine.rank(
                                context("bench-press", Collections.emptySet(), ALTERNATIVES), 4)
                        .get(0);
        ok(bench.measurementChanged(), "A different measurement profile needs a fresh target");
        ok(
                bench.explanation().contains("previous targets and loads do not transfer"),
                "No barbell load or target is transferred to bodyweight");
        var dumbbell =
                ExerciseSubstitutionEngine.rank(
                                context(
                                        "bench-press",
                                        Immutable.set(DUMBBELLS, BENCH),
                                        ALTERNATIVES),
                                50)
                        .stream()
                        .filter(s -> s.exercise().id().equals("dumbbell-bench-press"))
                        .findFirst()
                        .orElseThrow();
        ok(
                !dumbbell.measurementChanged(),
                "Same task is distinct from same actual loading relationship");
        ok(
                dumbbell.explanation().contains("Previous loads do not transfer"),
                "Even load-and-reps alternatives get no load transfer");
    }

    private static void easierAndHarderNeedReviewedEvidenceAndExperience() {
        var weighted = context("weighted-push-up", allEquipment(), EASIER);
        var easier = ExerciseSubstitutionEngine.rank(weighted, 50);
        ok(contains(easier, "push-up"), "A reviewed removal of added loading is an easier option");
        var experienced = context("push-up", allEquipment(), HARDER);
        ok(
                contains(ExerciseSubstitutionEngine.rank(experienced, 50), "weighted-push-up"),
                "Added-load progression is available for appropriate experience");
        var beginner =
                new Context(
                        experienced.original(),
                        experienced.goal(),
                        GymProgramme.Experience.BEGINNER,
                        experienced.availableEquipment(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        HARDER);
        ok(
                !contains(ExerciseSubstitutionEngine.rank(beginner, 50), "weighted-push-up"),
                "Harder never overrides selected beginner suitability");
        var squat =
                new Context(
                        exercise("bodyweight-squat"),
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        Immutable.set(DUMBBELLS),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        HARDER);
        ok(
                contains(ExerciseSubstitutionEngine.rank(squat, 50), "goblet-squat"),
                "A reviewed beginner-friendly optional external-load progression can exist without"
                        + " technical escalation");
        for (var suggestion : ExerciseSubstitutionEngine.rank(experienced, 50)) {
            ok(
                    suggestion
                            .explanation()
                            .contains("Difficulty still depends on load, control and execution"),
                    "Technical complexity is not falsely presented as measured effort");
            ok(
                    ExerciseMetadataCatalog.require(suggestion.exercise().id())
                            .suitability()
                            .appropriateFor(experienced.experience()),
                    "A harder request retains the experience gate");
        }
        var noEvidence = context("machine-chest-press", Immutable.set(CHEST_PRESS_MACHINE), HARDER);
        ok(
                ExerciseSubstitutionEngine.rank(noEvidence, 4).isEmpty(),
                "An unreviewed harder variation is not fabricated to fill the screen");
    }

    private static void exclusionsFavouritesAndHistoryStayWithinTrainingIntent() {
        var base =
                context(
                        "bench-press",
                        Immutable.set(DUMBBELLS, BENCH, CHEST_PRESS_MACHINE),
                        ALTERNATIVES);
        var preference =
                new Context(
                        base.original(),
                        base.goal(),
                        base.experience(),
                        base.availableEquipment(),
                        Set.of(),
                        Set.of("dumbbell-bench-press", "back-squat", "barbell-row"),
                        Set.of(),
                        ALTERNATIVES);
        var result = ExerciseSubstitutionEngine.rank(preference, 4);
        equal(
                "dumbbell-bench-press",
                result.get(0).exercise().id(),
                "A favourite wins among equally intent-compatible options");
        ok(
                result.get(0).explanation().contains("saved as a favourite"),
                "The real preference is explained");
        ok(
                !contains(result, "back-squat") && !contains(result, "barbell-row"),
                "Inappropriate favourites never bypass role requirements");
        var excluded =
                new Context(
                        base.original(),
                        base.goal(),
                        base.experience(),
                        base.availableEquipment(),
                        Set.of("dumbbell-bench-press"),
                        preference.favouriteIds(),
                        Set.of("dumbbell-bench-press"),
                        ALTERNATIVES);
        ok(
                !contains(ExerciseSubstitutionEngine.rank(excluded, 50), "dumbbell-bench-press"),
                "An exclusion overrides favourites and prior evidence");
        var restored =
                new Context(
                        base.original(),
                        base.goal(),
                        base.experience(),
                        base.availableEquipment(),
                        Set.of(),
                        preference.favouriteIds(),
                        Set.of(),
                        ALTERNATIVES);
        ok(
                contains(ExerciseSubstitutionEngine.rank(restored, 50), "dumbbell-bench-press"),
                "Removing an exclusion restores immediate eligibility");
        var known =
                new Context(
                        base.original(),
                        base.goal(),
                        base.experience(),
                        base.availableEquipment(),
                        Set.of(),
                        Set.of(),
                        Set.of("dumbbell-bench-press"),
                        ALTERNATIVES);
        var recorded =
                ExerciseSubstitutionEngine.rank(known, 50).stream()
                        .filter(s -> s.exercise().id().equals("dumbbell-bench-press"))
                        .findFirst()
                        .orElseThrow();
        ok(
                recorded.explanation().contains("not a starting-load recommendation"),
                "History does not infer a starting weight");
    }

    private static void discomfortDoesNotClaimMedicalSuitability() {
        String guidance = ExerciseSubstitutionEngine.discomfortGuidance();
        ok(
                guidance.contains("Avoid movements that reproduce pain"),
                "Discomfort guidance keeps athlete control");
        ok(
                guidance.contains("cannot determine which exercise is medically appropriate"),
                "No diagnosis or medical clearance is claimed");
        var suggestions =
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", allEquipment(), UNCOMFORTABLE), 4);
        ok(!suggestions.isEmpty(), "A discomfort reason may still show explained alternatives");
        for (var suggestion : suggestions) {
            ok(
                    suggestion.explanation().contains("not medical clearance"),
                    "A suggestion itself retains the medical limitation");
            ok(
                    !suggestion.explanation().toLowerCase().contains("safe for"),
                    "No individual injury-safety claim");
        }
    }

    private static void immutableInputsAndBoundedResults() {
        var equipment = EnumSet.of(DUMBBELLS, BENCH);
        var exclusions = new HashSet<String>();
        var favourites = new HashSet<>(Set.of("dumbbell-bench-press"));
        var context =
                new Context(
                        exercise("bench-press"),
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Experience.INTERMEDIATE,
                        equipment,
                        exclusions,
                        favourites,
                        Set.of(),
                        ALTERNATIVES);
        equipment.clear();
        exclusions.add("dumbbell-bench-press");
        favourites.clear();
        ok(
                context.availableEquipment().contains(DUMBBELLS),
                "Mutable equipment caller cannot rewrite a prepared context");
        ok(context.excludedIds().isEmpty(), "Mutable exclusions cannot rewrite prepared choices");
        ok(
                context.favouriteIds().contains("dumbbell-bench-press"),
                "Favourite evidence is frozen for deterministic ranking");
        var result = ExerciseSubstitutionEngine.rank(context, 1);
        equal(1, result.size(), "Caller limit is enforced");
        rejectsUnsupported(() -> result.clear(), "Results are immutable");
        rejectsUnsupported(
                () -> context.availableEquipment().clear(), "Equipment context is immutable");
        rejects(
                () -> ExerciseSubstitutionEngine.rank(context, 0),
                "Zero is not a fake successful suggestion request");
        rejects(
                () -> ExerciseSubstitutionEngine.rank(context, 51),
                "Huge result requests are rejected");
        var invalid = new HashSet<Equipment>();
        invalid.add(null);
        rejects(
                () ->
                        new Context(
                                exercise("bench-press"),
                                context.goal(),
                                context.experience(),
                                invalid,
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                ALTERNATIVES),
                "Unknown equipment cannot imply a complete setup");
    }

    private static void qualityMatrixAcrossTheCanonicalCatalogue() {
        int requests = 0;
        var exercises = ExerciseCatalog.exercises();
        var excluded = new LinkedHashSet<String>();
        var favourites = new LinkedHashSet<String>();
        var recent = new LinkedHashSet<String>();
        for (int i = 0; i < exercises.size(); i++) {
            String id = exercises.get(i).id();
            if (i % 7 == 0) excluded.add(id);
            if (i % 5 == 0) favourites.add(id);
            if (i % 3 == 0) recent.add(id);
        }
        for (int index = 0; index < exercises.size(); index++) {
            var exercise = exercises.get(index);
            var original = ExerciseMetadataCatalog.require(exercise.id());
            for (var goal : GymProgramme.Goal.values())
                for (var experience : GymProgramme.Experience.values())
                    for (var equipment :
                            List.of(allEquipment(), Collections.<Equipment>emptySet())) {
                        var reason =
                                Reason.values()[
                                        (index + goal.ordinal() + experience.ordinal())
                                                % Reason.values().length];
                        var context =
                                new Context(
                                        exercise,
                                        goal,
                                        experience,
                                        equipment,
                                        excluded,
                                        favourites,
                                        recent,
                                        reason);
                        var result = ExerciseSubstitutionEngine.rank(context, 4);
                        equal(
                                result,
                                ExerciseSubstitutionEngine.rank(context, 4),
                                "Every explicit request is deterministic");
                        ok(result.size() <= 4, "No padded unbounded busy-gym list");
                        var seen = new HashSet<String>();
                        for (var candidate : result) {
                            var metadata =
                                    ExerciseMetadataCatalog.require(candidate.exercise().id());
                            ok(seen.add(candidate.exercise().id()), "No duplicate alternatives");
                            ok(
                                    !candidate.exercise().id().equals(exercise.id()),
                                    "Original exercise is never offered as its own swap");
                            ok(
                                    !excluded.contains(candidate.exercise().id()),
                                    "Exclusions remain a hard gate");
                            equal(
                                    original.primaryRole(),
                                    metadata.primaryRole(),
                                    "Training role is not replaced by broad region matching");
                            ok(
                                    !Collections.disjoint(
                                            original.primaryAreas(), metadata.primaryAreas()),
                                    "Same generic role also preserves meaningful primary"
                                            + " involvement");
                            ok(
                                    metadata.equipment().availableWith(equipment),
                                    "Every complete AND setup fits the selected capabilities");
                            ok(
                                    metadata.suitability().appropriateFor(experience),
                                    "Reason/goal/favourite cannot bypass experience");
                            ok(
                                    ExerciseSubstitutionEngine.compatibleTask(
                                            exercise.measurement(),
                                            candidate.exercise().measurement()),
                                    "Actual measurement task remains compatible");
                            equal(
                                    exercise.measurement() != candidate.exercise().measurement(),
                                    candidate.measurementChanged(),
                                    "Changed targets are explicitly reported");
                            ok(
                                    candidate.explanation().contains("Uses ")
                                            && candidate.explanation().contains("do not transfer"),
                                    "Every ranked option explains equipment and load independence");
                        }
                        if (requests % 100 == 0) {
                            var reversed = new ArrayList<>(favourites);
                            Collections.reverse(reversed);
                            var again =
                                    new Context(
                                            exercise,
                                            goal,
                                            experience,
                                            equipment,
                                            excluded,
                                            new LinkedHashSet<>(reversed),
                                            recent,
                                            reason);
                            equal(
                                    result,
                                    ExerciseSubstitutionEngine.rank(again, 4),
                                    "Input set iteration order never becomes random tie-breaking");
                        }
                        requests++;
                    }
        }
        equal(
                exercises.size()
                        * GymProgramme.Goal.values().length
                        * GymProgramme.Experience.values().length
                        * 2,
                requests,
                "Every canonical exercise crosses all goals/experience/equipment cases");
        ok(requests > 5000, "Quality fixtures exercise thousands of deterministic contexts");
    }

    private static void unknownMetadataNeverInventsAlternatives() {
        var custom =
                new StrengthExercise(
                        "custom-unknown",
                        "Athlete's custom movement",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false);
        var context =
                new Context(
                        custom,
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.EXPERIENCED,
                        allEquipment(),
                        Set.of(),
                        Set.of("bench-press"),
                        Set.of(),
                        ALTERNATIVES);
        ok(
                ExerciseSubstitutionEngine.rank(context, 4).isEmpty(),
                "Unknown custom movement roles do not inherit a guess from its region/name");
        var frozen =
                new StrengthExercise(
                        "bench-press",
                        "Frozen historical name",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.DURATION);
        var historical =
                new Context(
                        frozen,
                        context.goal(),
                        context.experience(),
                        context.availableEquipment(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ALTERNATIVES);
        ok(
                ExerciseSubstitutionEngine.rank(historical, 4).isEmpty(),
                "A frozen actual measurement is never reconstructed through today's rep catalogue");
        equal(
                "Frozen historical name",
                frozen.name(),
                "Ranking does not mutate frozen definitions");
        var wrongRepTask =
                new StrengthExercise(
                        "bench-press",
                        "Custom overlapping identity",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.REPS_ONLY);
        var wrongRepContext =
                new Context(
                        wrongRepTask,
                        context.goal(),
                        context.experience(),
                        context.availableEquipment(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ALTERNATIVES);
        ok(
                ExerciseSubstitutionEngine.rank(wrongRepContext, 4).isEmpty(),
                "A different rep-family profile cannot borrow a canonical role merely through an"
                        + " overlapping ID");
        var wrongMode =
                new StrengthExercise(
                        "bench-press",
                        "Old bodyweight definition",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        ok(
                ExerciseSubstitutionEngine.rank(
                                new Context(
                                        wrongMode,
                                        context.goal(),
                                        context.experience(),
                                        context.availableEquipment(),
                                        Set.of(),
                                        Set.of(),
                                        Set.of(),
                                        ALTERNATIVES),
                                4)
                        .isEmpty(),
                "A mode mismatch is not authenticated by the current catalogue ID");
        var wrongRegion =
                new StrengthExercise(
                        "bench-press",
                        "Old lower-body definition",
                        StrengthExercise.Region.LOWER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        ok(
                ExerciseSubstitutionEngine.rank(
                                new Context(
                                        wrongRegion,
                                        context.goal(),
                                        context.experience(),
                                        context.availableEquipment(),
                                        Set.of(),
                                        Set.of(),
                                        Set.of(),
                                        ALTERNATIVES),
                                4)
                        .isEmpty(),
                "Different retained movement context remains unknown instead of being"
                        + " reconstructed");
        var renamed =
                new StrengthExercise(
                        "bench-press",
                        "Athlete's historical bench label",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        var renamedContext =
                new Context(
                        renamed,
                        context.goal(),
                        context.experience(),
                        context.availableEquipment(),
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ALTERNATIVES);
        equal(
                ExerciseSubstitutionEngine.rank(
                        context("bench-press", allEquipment(), ALTERNATIVES), 4),
                ExerciseSubstitutionEngine.rank(
                        new Context(
                                renamed,
                                GymProgramme.Goal.STRENGTH,
                                context.experience(),
                                context.availableEquipment(),
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                ALTERNATIVES),
                        4),
                "Historical display labels and E1RM flags do not block otherwise matching frozen"
                        + " task metadata");
        ok(
                !ExerciseSubstitutionEngine.rank(renamedContext, 4).isEmpty(),
                "A matching historical alias remains eligible without rewriting its name");
    }

    private static Context context(String id, Set<Equipment> equipment, Reason reason) {
        return new Context(
                exercise(id),
                GymProgramme.Goal.STRENGTH,
                GymProgramme.Experience.EXPERIENCED,
                equipment,
                Set.of(),
                Set.of(),
                Set.of(),
                reason);
    }

    private static StrengthExercise exercise(String id) {
        return ExerciseCatalog.definition(id).exercise();
    }

    private static Set<Equipment> allEquipment() {
        return EnumSet.allOf(Equipment.class);
    }

    private static boolean contains(
            List<ExerciseSubstitutionEngine.Suggestion> suggestions, String id) {
        return suggestions.stream().anyMatch(s -> s.exercise().id().equals(id));
    }

    private static void equal(Object expected, Object actual, String reason) {
        ok(
                Objects.equals(expected, actual),
                reason + ": expected=" + expected + ", actual=" + actual);
    }

    private static void ok(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
        checks++;
    }

    private static void rejects(Runnable operation, String reason) {
        try {
            operation.run();
            throw new AssertionError(reason);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void rejectsUnsupported(Runnable operation, String reason) {
        try {
            operation.run();
            throw new AssertionError(reason);
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
    }
}
