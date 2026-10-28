package org.racedaydreams.domain.settings;

/** Small local application preferences. Unknown keys/values are never fabricated. */
public interface SettingsRepository {
    String getSetting(String key);

    void putSetting(String key, String value);

    void removeSetting(String key);
}
