package org.racedaydreams.domain.storage;

import java.time.Instant;
import java.util.Objects;

/** Exact evidence fingerprint for explicit archive restoration, never invented route data. */
public record RemovedRecordingDetail(
        String sessionId,
        String activityId,
        String provider,
        String recordId,
        Instant endedAt,
        Instant removedAt,
        DetailRetention.Policy policy,
        Instant startedAt,
        long startedMono,
        long lastMono,
        long points,
        long events,
        String pointsHash,
        String eventsHash) {
    public RemovedRecordingDetail {
        for (String id : new String[] {sessionId, activityId, recordId})
            if (id == null || id.isBlank() || id.length() > 256)
                throw new IllegalArgumentException("Invalid removed-detail identity.");
        if (!Objects.equals(sessionId, recordId)
                || provider == null
                || !(provider.equals("rdd") || provider.equals("gpx")))
            throw new IllegalArgumentException("Only app-owned recording detail can be removed.");
        Objects.requireNonNull(endedAt);
        Objects.requireNonNull(removedAt);
        Objects.requireNonNull(startedAt);
        Objects.requireNonNull(policy);
        if (policy == DetailRetention.Policy.KEEP_FOREVER
                || endedAt.isAfter(removedAt)
                || startedAt.isAfter(endedAt)
                || startedMono < 0
                || lastMono < startedMono
                || points < 0
                || events < 0
                || points == 0 && events == 0)
            throw new IllegalArgumentException("Invalid removed-detail evidence.");
        for (String hash : new String[] {pointsHash, eventsHash})
            if (hash == null || !hash.matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("Missing original detail fingerprint.");
    }
}
