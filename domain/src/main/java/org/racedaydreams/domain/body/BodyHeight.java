package org.racedaydreams.domain.body;

import java.time.*;
import java.util.Objects;

/** An optional observation; height never determines protein requirements or training readiness. */
public record BodyHeight(
        String id,
        double metres,
        Instant at,
        ZoneOffset offset,
        String provider,
        String sourceId,
        String origin,
        Instant importedAt) {
    public BodyHeight {
        if (id == null
                || id.trim().isEmpty()
                || id.length() > 256
                || !Double.isFinite(metres)
                || metres <= 0
                || metres > 3
                || provider == null
                || provider.trim().isEmpty()
                || provider.length() > 256
                || sourceId == null
                || sourceId.trim().isEmpty()
                || sourceId.length() > 512
                || origin != null && origin.length() > 512)
            throw new IllegalArgumentException(
                    "Height needs a valid measurement and source identity.");
        Objects.requireNonNull(at);
    }
}
