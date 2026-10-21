package org.racedaydreams.domain.baseline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AssessmentRecommendation(int distanceKm, boolean unnecessary, List<String> reasons) {
    public AssessmentRecommendation {
        if (distanceKm < 1 || distanceKm > 5) throw new IllegalArgumentException("Assessment distance must be 1–5K.");
        reasons = Collections.unmodifiableList(new ArrayList<>(reasons));
    }
}
