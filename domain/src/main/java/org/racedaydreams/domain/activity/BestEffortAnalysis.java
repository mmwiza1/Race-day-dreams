package org.racedaydreams.domain.activity;

import java.util.List;
import java.util.Objects;

public record BestEffortAnalysis(List<BestEffort> efforts, SampleQuality quality, List<String> reasons) {
    public BestEffortAnalysis {
        efforts = ActivityChecks.copy(efforts);
        Objects.requireNonNull(quality, "Sample quality");
        reasons = ActivityChecks.copy(reasons);
    }
}
