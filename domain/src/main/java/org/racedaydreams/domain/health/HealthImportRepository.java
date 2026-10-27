package org.racedaydreams.domain.health;

import org.racedaydreams.domain.activity.ActivityRecord;

import java.util.List;

/** Provider-neutral, synchronous import transaction boundary. Call from an IO dispatcher. */
public interface HealthImportRepository {
    List<ActivityRecord> activities();

    String importToken(String provider);

    /** Upserts, source-record deletions and the nullable checkpoint must commit atomically. */
    void applyImport(
            String provider,
            List<ActivityRecord> upserts,
            List<String> deletedRecordIds,
            String nextToken);
}
