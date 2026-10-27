package org.racedaydreams.data;

import android.content.Context;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.settings.PrimaryFocusRepository;
import org.racedaydreams.domain.settings.SettingsRepository;
import java.util.Objects;
import java.util.Optional;

/** Typed view over existing settings; shares their SQLite transaction and backup format. */
public final class SqlitePrimaryFocusRepository implements PrimaryFocusRepository {
    private static final String KEY = "primary.focus";
    private final SettingsRepository settings;

    public SqlitePrimaryFocusRepository(Context context) {
        this(new SqliteSettingsRepository(context));
    }

    public SqlitePrimaryFocusRepository(SettingsRepository settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    @Override
    public Optional<PrimaryFocus> load() {
        String selected = settings.getSetting(KEY);
        return selected == null
                ? PrimaryFocus.fromLegacyStartingPath(settings.getSetting("starting.path"))
                : PrimaryFocus.fromStorageValue(selected);
    }

    @Override
    public void save(PrimaryFocus focus) {
        settings.putSetting(KEY, Objects.requireNonNull(focus).storageValue());
    }
}
