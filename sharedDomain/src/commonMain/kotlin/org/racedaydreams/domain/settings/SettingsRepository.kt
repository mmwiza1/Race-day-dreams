package org.racedaydreams.domain.settings

/** Small local preferences. Absent keys remain unknown; platform owns the shared transaction. */
interface SettingsRepository {
    fun getSetting(key: String): String?

    fun putSetting(key: String, value: String)

    fun removeSetting(key: String)
}
