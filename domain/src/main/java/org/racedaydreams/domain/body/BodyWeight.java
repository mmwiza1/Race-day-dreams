package org.racedaydreams.domain.body;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
/** A measurement, never a training-readiness signal. Provider identity supports update/delete. */
public record BodyWeight(String id, double kilograms, Instant at, ZoneOffset offset, String provider, String sourceId, String origin) {
    public BodyWeight {
        if (id == null || id.trim().isEmpty() || !Double.isFinite(kilograms) || kilograms <= 0 || kilograms > 1000 || provider == null || sourceId == null)
            throw new IllegalArgumentException("Weight needs an identity, positive mass and provenance.");
        Objects.requireNonNull(at); Objects.requireNonNull(offset);
    }
}
