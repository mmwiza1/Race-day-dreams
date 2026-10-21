package org.racedaydreams.domain.health;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.racedaydreams.domain.activity.ActivityRecord;

/** Deterministic validation and bounded snapshot reconciliation. No Android/provider SDK types. */
public final class HealthImportPolicy {
    private HealthImportPolicy() {}

    public record Batch(String provider, List<ActivityRecord> upserts,
                        List<String> deletedRecordIds, String nextToken,
                        Instant windowStart, Instant windowEnd, boolean completeSnapshot) {
        public Batch {
            if (provider == null || provider.trim().isEmpty()) throw new IllegalArgumentException("An import provider is required.");
            Objects.requireNonNull(windowStart);
            Objects.requireNonNull(windowEnd);
            if (!windowEnd.isAfter(windowStart)) throw new IllegalArgumentException("The import window must have positive duration.");
            upserts = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(upserts)));
            deletedRecordIds = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(deletedRecordIds)));
            Set<String> recordIds = new HashSet<>();
            for (ActivityRecord activity : upserts) {
                if (!provider.equals(activity.source().provider())) throw new IllegalArgumentException("A batch cannot overwrite a different provider.");
                if (!recordIds.add(activity.source().recordId())) throw new IllegalArgumentException("The batch contains a duplicate source record.");
            }
        }
    }

    /**
     * A missing record is a deletion only inside a completely read, authorized snapshot window.
     * Older/inaccessible data and other providers are retained. Explicit change-feed IDs can
     * delete matching records outside the snapshot window without knowing provider SDK types.
     */
    public static List<String> deletions(List<ActivityRecord> existing, Batch batch) {
        Set<String> seen = new HashSet<>();
        for (ActivityRecord activity : batch.upserts()) seen.add(activity.source().recordId());
        Set<String> deleted = new LinkedHashSet<>(batch.deletedRecordIds());
        if (batch.completeSnapshot()) {
            for (ActivityRecord activity : existing) {
                if (!batch.provider().equals(activity.source().provider())) continue;
                boolean whollyInside = !activity.start().isBefore(batch.windowStart())
                        && !activity.end().isAfter(batch.windowEnd());
                if (whollyInside && !seen.contains(activity.source().recordId())) {
                    deleted.add(activity.source().recordId());
                }
            }
        }
        // A snapshot/newer upsertion wins over an earlier deletion in the same staged batch.
        deleted.removeAll(seen);
        return Collections.unmodifiableList(new ArrayList<>(deleted));
    }

    /** Latest observation wins for the same provider ID; unrelated IDs are never collapsed. */
    public static List<ActivityRecord> unique(List<ActivityRecord> records) {
        Map<SourceKey, ActivityRecord> unique = new LinkedHashMap<>();
        for (ActivityRecord record : records) {
            unique.put(new SourceKey(record.source().provider(), record.source().recordId()), record);
        }
        return Collections.unmodifiableList(new ArrayList<>(unique.values()));
    }

    // Provider identifiers are opaque. A joined string could collapse unrelated identities if
    // either component happened to contain the chosen separator.
    private record SourceKey(String provider, String recordId) { }

    public static void commit(HealthImportRepository repository, Batch batch) {
        List<String> removed = deletions(repository.activities(), batch);
        repository.applyImport(batch.provider(), batch.upserts(), removed, batch.nextToken());
    }
}
