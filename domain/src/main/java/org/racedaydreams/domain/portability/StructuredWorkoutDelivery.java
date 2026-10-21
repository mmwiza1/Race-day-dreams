package org.racedaydreams.domain.portability;

import java.util.Objects;
import org.racedaydreams.domain.PlannedWorkout;

/** Future phone/watch transport shares the exact same prescription instead of a second engine. */
public record StructuredWorkoutDelivery(PlannedWorkout workout, long revision) {
    public StructuredWorkoutDelivery {
        Objects.requireNonNull(workout, "shared prescription");
        if (revision < 1) throw new IllegalArgumentException("A prescription revision is required.");
    }
}
