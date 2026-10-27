package org.racedaydreams.data;

import org.json.JSONObject;

/**
 * Android application portability boundary. JSON stays out of the pure domain; validation precedes
 * replacement, and every feature participates in the same SQLite transaction.
 */
public interface BackupRepository {
    /** Validate an untrusted complete snapshot without reading or modifying storage. */
    void validateSnapshot(JSONObject data);

    JSONObject exportUserData();

    void restoreUserData(JSONObject data, boolean merge);
}
