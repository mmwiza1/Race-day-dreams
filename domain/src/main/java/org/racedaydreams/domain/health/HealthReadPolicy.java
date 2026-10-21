package org.racedaydreams.domain.health;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Permission/data-coverage decisions use provider-neutral facts, not Android permission names. */
public final class HealthReadPolicy {
    public enum Access { UNAVAILABLE, DENIED, PARTIAL, READY }
    private HealthReadPolicy() {}

    public static Access access(boolean available, boolean exerciseAllowed, boolean distanceAllowed) {
        if (!available) return Access.UNAVAILABLE;
        if (!exerciseAllowed) return Access.DENIED;
        return distanceAllowed ? Access.READY : Access.PARTIAL;
    }

    public static Instant windowStart(Instant now, boolean extendedHistoryGranted) {
        Objects.requireNonNull(now);
        // Conservative recent bound avoids silently claiming inaccessible pre-grant history.
        return now.minus(Duration.ofDays(extendedHistoryGranted ? 84 : 29));
    }
}
