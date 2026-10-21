package org.racedaydreams.domain;

import java.util.Optional;

/** Synchronous local persistence boundary. Android callers execute these methods off the UI thread. */
public interface UserPreferencesRepository {
    Optional<UserProfile> load();
    void save(UserProfile profile);
    void clear();
}
