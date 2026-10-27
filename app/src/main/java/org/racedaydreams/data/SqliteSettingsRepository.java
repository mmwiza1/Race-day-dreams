package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.racedaydreams.domain.settings.SettingsRepository;

import java.util.Objects;

/**
 * SQLite adapter for settings data. Shares the application database and its transaction boundary.
 */
public final class SqliteSettingsRepository extends SqliteStoreAccess
        implements SettingsRepository {
    public SqliteSettingsRepository(Context context) {
        super(context);
    }

    public String getSetting(String key) {
        try (Cursor c =
                read().rawQuery(
                                "SELECT setting_value FROM settings WHERE setting_key=?",
                                new String[] {required(key, "setting key")})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    public void putSetting(String key, String value) {
        putSetting(write(), key, value);
    }

    static void putSetting(SQLiteDatabase db, String key, String value) {
        ContentValues v = values("setting_key", required(key, "setting key"));
        v.put(
                "setting_value",
                "provider.preference".equals(key)
                        ? org.racedaydreams.domain.activity.ActivitySources
                                .normalizeLegacyPreference(value)
                        : Objects.requireNonNull(value));
        replace(db, "settings", v);
    }

    public void removeSetting(String key) {
        write().delete("settings", "setting_key=?", new String[] {required(key, "setting key")});
    }
}
