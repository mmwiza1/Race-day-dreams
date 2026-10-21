package org.racedaydreams.domain.activity;

import java.util.List;
import java.util.Objects;

public record SplitAnalysis(List<RunSplit> splits, SampleQuality quality, List<String> reasons) {
    public SplitAnalysis {
        splits = ActivityChecks.copy(splits);
        Objects.requireNonNull(quality, "Split quality");
        reasons = ActivityChecks.copy(reasons);
    }
}
