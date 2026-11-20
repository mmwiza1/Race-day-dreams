package org.racedaydreams.update

import android.content.Context
import org.racedaydreams.domain.update.UpdatePreferences

/** Device-local update choices. Not exported in backups: they describe this installation only. */
class UpdatePreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun read(): UpdatePreferences =
        UpdatePreferences(
            automaticChecks = prefs.getBoolean(AUTOMATIC, true),
            notifications = prefs.getBoolean(NOTIFY, true),
            skippedVersionCode = long(SKIPPED),
            snoozedVersionCode = long(SNOOZED),
            snoozedUntilEpochSecond = long(SNOOZED_UNTIL),
            notifiedVersionCode = long(NOTIFIED),
            lastCheckEpochSecond = long(LAST_CHECK),
        )

    fun write(value: UpdatePreferences) {
        prefs
            .edit()
            .putBoolean(AUTOMATIC, value.automaticChecks)
            .putBoolean(NOTIFY, value.notifications)
            .apply {
                put(this, SKIPPED, value.skippedVersionCode)
                put(this, SNOOZED, value.snoozedVersionCode)
                put(this, SNOOZED_UNTIL, value.snoozedUntilEpochSecond)
                put(this, NOTIFIED, value.notifiedVersionCode)
                put(this, LAST_CHECK, value.lastCheckEpochSecond)
            }
            .apply()
    }

    private fun long(key: String): Long? = if (prefs.contains(key)) prefs.getLong(key, 0) else null

    private fun put(editor: android.content.SharedPreferences.Editor, key: String, value: Long?) {
        if (value == null) editor.remove(key) else editor.putLong(key, value)
    }

    companion object {
        const val NAME = "app_updates"
        private const val AUTOMATIC = "automatic_checks"
        private const val NOTIFY = "notifications"
        private const val SKIPPED = "skipped_version_code"
        private const val SNOOZED = "snoozed_version_code"
        private const val SNOOZED_UNTIL = "snoozed_until"
        private const val NOTIFIED = "notified_version_code"
        private const val LAST_CHECK = "last_check"
    }
}
