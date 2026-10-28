package org.racedaydreams.domain.strength;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** An explicit, reversible programming preference; never deletion of exercise evidence. */
public interface ExerciseExclusionRepository {
    enum Reason {
        PREFERENCE,
        EQUIPMENT_UNAVAILABLE,
        UNCOMFORTABLE,
        OTHER
    }

    record Exclusion(String exerciseId, Reason reason, Instant excludedAt) {
        public Exclusion {
            if (exerciseId == null || exerciseId.isBlank() || exerciseId.length() > 200)
                throw new IllegalArgumentException("Invalid excluded exercise.");
            Objects.requireNonNull(reason, "exclusion reason");
            Objects.requireNonNull(excludedAt, "exclusion date");
        }
    }

    List<Exclusion> exclusions();

    void exclude(Exclusion exclusion);

    void restoreExercise(String exerciseId);
}
