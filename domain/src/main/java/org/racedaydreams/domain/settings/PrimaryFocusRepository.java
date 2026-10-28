package org.racedaydreams.domain.settings;

import java.util.Optional;

/** A focus preference never creates, deletes or changes a training programme. */
public interface PrimaryFocusRepository {
    Optional<PrimaryFocus> load();

    void save(PrimaryFocus focus);
}
