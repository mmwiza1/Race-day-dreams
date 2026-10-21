package org.racedaydreams.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** May share a day with running. Future import provenance is represented explicitly. */
public record GymSession(String id, Instant startedAt, Type type, Duration duration,
        Effort effort, String notes, Source source, String externalId) {
    public enum Type { UPPER_BODY, LOWER_BODY, FULL_BODY, FUNCTIONAL_FITNESS, OTHER }
    public enum Effort { EASY, MODERATE, HARD }
    public enum Source { MANUAL, HEALTH_CONNECT, OTHER_PROVIDER }

    public GymSession {
        id = DomainChecks.id(id, "gym session id");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(duration, "duration");
        DomainChecks.nonNegative(duration, "duration");
        Objects.requireNonNull(effort, "effort");
        Objects.requireNonNull(source, "source");
    }
}
