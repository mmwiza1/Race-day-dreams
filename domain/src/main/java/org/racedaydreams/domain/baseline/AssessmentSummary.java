package org.racedaydreams.domain.baseline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Null secondary metrics are unavailable; no race prediction is made from this result. */
public record AssessmentSummary(double averagePaceSecondsPerKm, Double firstKilometrePace,
        Double finalKilometrePace, Double paceFadePercent, Double averageHeartRate, Double cadence,
        BaselineConfidence confidence, double evidenceWeight, List<String> findings) {
    public AssessmentSummary { findings = Collections.unmodifiableList(new ArrayList<>(findings)); }
}
