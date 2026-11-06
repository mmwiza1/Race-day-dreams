package org.racedaydreams.domain.strength;

import static org.racedaydreams.domain.strength.ExerciseMetadata.BodyArea.*;
import static org.racedaydreams.domain.strength.ExerciseMetadata.Equipment.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/** New tasks must be useful, honestly measured and reachable through the existing catalogue. */
public final class CatalogueExpansionTestSuite {
    private static int checks;

    public static void main(String[] args) {
        hipMachinesHaveDifferentCapabilitiesAndBodyAreas();
        actualsAndWarmupsStaySeparate();
        supportedProgressionsExplainLeverageNotInventedLoad();
        cableAlternativesNeedTheAttachmentAndKeepHipDirection();
        System.out.println("CatalogueExpansionTestSuite: " + checks + " checks passed.");
    }

    private static void hipMachinesHaveDifferentCapabilitiesAndBodyAreas() {
        var abduct = ExerciseMetadataCatalog.require("hip-abductor-machine");
        var adduct = ExerciseMetadataCatalog.require("hip-adductor-machine");
        check(abduct.primaryAreas().equals(Set.of(GLUTES)));
        check(adduct.primaryAreas().equals(Set.of(HIP_ADDUCTORS)));
        check(abduct.equipment().availableWith(Set.of(HIP_ABDUCTOR_MACHINE)));
        check(!abduct.equipment().availableWith(Set.of(HIP_ADDUCTOR_MACHINE)));
        check(!adduct.equipment().availableWith(Set.of(HIP_ABDUCTOR_MACHINE)));
        check(!adduct.equipment().availableWith(Set.of(LEG_EXTENSION_MACHINE)));
        for (var id : List.of("hip-abductor-machine", "hip-adductor-machine")) {
            var exercise = ExerciseCatalog.definition(id).exercise();
            check(exercise.measurement() == StrengthExercise.MeasurementProfile.LOAD_REPS);
            check(!exercise.estimateOneRepMax());
            var illustration = TechniqueIllustrations.forExercise(id);
            check(illustration.phases().size() == 2);
            check(illustration.asset().equals("guide_" + id.replace('-', '_')));
            check(TechniqueGuide.forExercise(id).movement().contains("same machine"));
        }
        check(
                ExerciseSearch.search(
                                ExerciseCatalog.exercises(),
                                "Outer Thigh Machine",
                                Set.of(),
                                List.of(),
                                Set.of())
                        .get(0)
                        .id()
                        .equals("hip-abductor-machine"));
        check(
                ExerciseSearch.search(
                                ExerciseCatalog.exercises(),
                                "Inner Thigh Machine",
                                Set.of(),
                                List.of(),
                                Set.of())
                        .get(0)
                        .id()
                        .equals("hip-adductor-machine"));
        check(
                !TechniqueIllustrations.forExercise("hip-abductor-machine")
                        .asset()
                        .equals(
                                TechniqueIllustrations.forExercise("hip-adductor-machine")
                                        .asset()));
    }

    private static void actualsAndWarmupsStaySeparate() {
        var now = Instant.parse("2026-11-05T12:00:00Z");
        for (var id :
                List.of(
                        "hip-abductor-machine",
                        "hip-adductor-machine",
                        "cable-hip-abduction",
                        "cable-hip-adduction",
                        "cable-glute-kickback",
                        "seated-dumbbell-calf-raise")) {
            var exercise = ExerciseCatalog.definition(id).exercise();
            var working = new StrengthSession.SetEntry("working", 12, 25, 7.0, false);
            var warmup = new StrengthSession.SetEntry("warmup", 8, 30, 5.0, true);
            var session =
                    new StrengthSession(
                            "logged-" + id,
                            now,
                            ZoneOffset.UTC,
                            Duration.ofMinutes(10),
                            null,
                            "Same selected machine setup",
                            List.of(
                                    new StrengthSession.ExerciseSets(
                                            exercise, List.of(warmup, working))));
            var records = StrengthRecords.progression(List.of(session), now);
            check(!records.isEmpty());
            check(records.stream().allMatch(p -> p.actual().equals(working)));
            check(records.stream().noneMatch(StrengthRecords.Point::estimated));
            check(records.stream().allMatch(p -> p.workoutId().equals(session.id())));
            check(ExerciseCatalog.definition(id).exercise().equals(exercise));
        }
    }

    private static void cableAlternativesNeedTheAttachmentAndKeepHipDirection() {
        var equipment = Set.of(CABLE_STATION, CABLE_ANKLE_CUFF);
        for (String id :
                List.of("cable-hip-abduction", "cable-hip-adduction", "cable-glute-kickback")) {
            var metadata = ExerciseMetadataCatalog.require(id);
            check(metadata.equipment().availableWith(equipment));
            check(!metadata.equipment().availableWith(Set.of(CABLE_STATION)));
            check(!metadata.equipment().availableWith(Set.of(CABLE_ANKLE_CUFF)));
            check(metadata.roles().contains(ExerciseMetadata.Role.UNILATERAL));
        }
        var context =
                new ExerciseSubstitutionEngine.Context(
                        ExerciseCatalog.definition("hip-adductor-machine").exercise(),
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        equipment,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ExerciseSubstitutionEngine.Reason.EQUIPMENT_UNAVAILABLE);
        var suggestions = ExerciseSubstitutionEngine.rank(context, 50);
        check(suggestions.size() == 1);
        check(suggestions.get(0).exercise().id().equals("cable-hip-adduction"));
        check(suggestions.get(0).explanation().contains("Previous loads do not transfer"));
        check(
                !TechniqueIllustrations.forExercise("cable-hip-abduction")
                        .asset()
                        .equals(TechniqueIllustrations.forExercise("cable-hip-adduction").asset()));
        check(
                TechniqueGuide.forExercise("cable-hip-adduction")
                        .setup()
                        .contains("nearest the tower"));
        check(
                TechniqueGuide.forExercise("cable-hip-abduction")
                        .setup()
                        .contains("furthest from the tower"));
        check(
                !ExerciseMetadataCatalog.require("seated-dumbbell-calf-raise")
                        .equipment()
                        .availableWith(Set.of(DUMBBELLS, BENCH)));
        check(
                ExerciseMetadataCatalog.require("seated-dumbbell-calf-raise")
                        .equipment()
                        .availableWith(Set.of(DUMBBELLS, BENCH, STABLE_STEP)));
        check(
                TechniqueGuide.forExercise("seated-dumbbell-calf-raise")
                        .movement()
                        .contains("total across both"));
    }

    private static void supportedProgressionsExplainLeverageNotInventedLoad() {
        var all = java.util.EnumSet.allOf(ExerciseMetadata.Equipment.class);
        var source = ExerciseCatalog.definition("push-up").exercise();
        var context =
                new ExerciseSubstitutionEngine.Context(
                        source,
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        all,
                        Set.of(),
                        Set.of(),
                        Set.of(),
                        ExerciseSubstitutionEngine.Reason.EASIER);
        var choices = ExerciseSubstitutionEngine.rank(context, 50);
        for (String id : List.of("wall-push-up", "knee-push-up")) {
            var choice =
                    choices.stream()
                            .filter(c -> c.exercise().id().equals(id))
                            .findFirst()
                            .orElseThrow();
            check(choice.explanation().contains("more support or less demanding leverage"));
            check(!choice.explanation().contains("added-loading"));
            check(!choice.measurementChanged());
            check(choice.exercise().mode() == StrengthExercise.Mode.BODYWEIGHT);
        }
        var excluded =
                new ExerciseSubstitutionEngine.Context(
                        source,
                        context.goal(),
                        context.experience(),
                        all,
                        Set.of("wall-push-up", "knee-push-up"),
                        Set.of(),
                        Set.of(),
                        context.reason());
        check(
                ExerciseSubstitutionEngine.rank(excluded, 50).stream()
                        .noneMatch(c -> excluded.excludedIds().contains(c.exercise().id())));
        check(!ExerciseMetadataCatalog.require("wall-push-up").equipment().availableWith(Set.of()));
        check(ExerciseMetadataCatalog.require("knee-push-up").equipment().availableWith(Set.of()));
        check(
                ExerciseMetadataCatalog.require("chair-sit-to-stand")
                        .equipment()
                        .availableWith(Set.of(STABLE_CHAIR)));
        check(
                !ExerciseMetadataCatalog.require("chair-sit-to-stand")
                        .equipment()
                        .availableWith(Set.of(STABLE_STEP)));
        for (String id :
                List.of(
                        "wall-push-up",
                        "knee-push-up",
                        "side-lying-hip-abduction",
                        "chair-sit-to-stand")) {
            check(!ExerciseCatalog.definition(id).exercise().measurement().usesLoad());
            check(TechniqueIllustrations.forExercise(id).phases().size() == 2);
            check(TechniqueGuide.forExercise(id).mistakes().size() == 3);
        }
    }

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Catalogue expansion check " + checks);
    }
}
