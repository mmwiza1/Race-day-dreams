package org.racedaydreams.domain.strength;

import java.util.Objects;
import org.racedaydreams.domain.coaching.Immutable;

/** One user-entered actual set, including the exercise definition used for that set. */
public record GymExecutionSet(String slotId, StrengthExercise exercise, StrengthSession.SetEntry actual) {
    public GymExecutionSet {
        GymWorkoutDefinition.requireId(slotId, "exercise slot");
        Objects.requireNonNull(exercise, "exercise");
        Objects.requireNonNull(actual, "actual set");
        new StrengthSession.ExerciseSets(exercise, Immutable.list(actual));
    }
}
