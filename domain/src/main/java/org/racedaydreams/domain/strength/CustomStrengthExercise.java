package org.racedaydreams.domain.strength;

import java.util.Objects;

/** Athlete-authored metadata. No invented technique, suitability, or load-transfer evidence. */
public record CustomStrengthExercise(
        StrengthExercise exercise,
        ExerciseMetadata.BodyArea bodyArea,
        ExerciseMetadata.Role role,
        ExerciseMetadata.EquipmentRequirement equipment,
        String notes) {
    public CustomStrengthExercise {
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(bodyArea);
        Objects.requireNonNull(role);
        Objects.requireNonNull(equipment);
        if (!exercise.id().startsWith("custom-")
                || ExerciseCatalog.definition(exercise.id()) != null
                || !exercise.id().matches("[a-z0-9]+(?:-[a-z0-9]+)*"))
            throw new IllegalArgumentException("Use a distinct stable custom exercise identity.");
        if (exercise.estimateOneRepMax())
            throw new IllegalArgumentException(
                    "Custom exercises do not infer estimated strength or automatic progression.");
        StrengthProgramme.text(notes, 2000, true, "custom exercise notes");
    }
}
