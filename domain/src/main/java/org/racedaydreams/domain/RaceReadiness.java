package org.racedaydreams.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Transparent internal score; never a probability or guarantee of race success. */
public record RaceReadiness(int score, Instant assessedAt, List<Indicator> indicators) {
    public record Indicator(String category, int score, String explanation) {
        public Indicator {
            category = DomainChecks.id(category, "readiness category");
            checkScore(score);
            explanation = DomainChecks.id(explanation, "indicator explanation");
        }
    }

    public RaceReadiness {
        checkScore(score);
        Objects.requireNonNull(assessedAt, "assessedAt");
        indicators = DomainChecks.immutableList(indicators);
    }

    private static void checkScore(int value) {
        if (value < 0 || value > 100) throw new IllegalArgumentException("Score must be 0 to 100");
    }
}
