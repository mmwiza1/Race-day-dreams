package org.racedaydreams.domain.baseline;

import java.util.Objects;

/** An optional runner-selected assessment; its recommendation never restricts the choice. */
public record BaselineAssessment(int distanceKm, AssessmentMode mode) {
    public BaselineAssessment {
        if (distanceKm < 1 || distanceKm > 5) throw new IllegalArgumentException("Choose an assessment from 1K to 5K.");
        Objects.requireNonNull(mode, "Assessment mode");
    }
}
