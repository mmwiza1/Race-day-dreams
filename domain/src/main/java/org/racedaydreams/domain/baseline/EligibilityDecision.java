package org.racedaydreams.domain.baseline;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Recomputed evidence decision. validUntil is a review deadline, not an exercise prohibition. */
public record EligibilityDecision(boolean eligible, List<String> reasons, Instant validUntil) {
    public EligibilityDecision {
        reasons = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(reasons, "reasons")));
        Objects.requireNonNull(validUntil, "validUntil");
    }
}
