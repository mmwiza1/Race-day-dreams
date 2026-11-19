package org.racedaydreams.coach.application

import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.storage.SqlitePrimaryFocus

/** User-selected portability/settings operations over the existing shared transaction. */
class SettingsCoordinator<File>(
    private val operations: CoachOperationRunner,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val focus: SqlitePrimaryFocus,
    private val platform: SettingsPlatform<File>,
    private val recordingActive: () -> Boolean,
) {
    fun startingPath(path: String, onSuccess: () -> Unit = {}) =
        operations.submit(onSuccess = onSuccess) {
            require(path in listOf("record", "race", "hybrid"))
            settings.putSetting("starting.path", path)
            focus.save(requireNotNull(PrimaryFocus.fromLegacyStartingPath(path)))
        }

    fun primaryFocus(selected: PrimaryFocus, onSuccess: () -> Unit = {}) =
        operations.submit(onSuccess = onSuccess) { focus.save(selected) }

    fun setting(key: String, value: String) =
        operations.submit {
            require(key in setOf("calendar.view", "mass.unit", "provider.preference"))
            settings.putSetting(key, value)
        }

    fun exportBackup(file: File) =
        operations.submit("Backup saved to the file you selected. Keep it somewhere you trust.") {
            platform.exportBackup(file)
        }

    fun restoreBackup(file: File, onSuccess: () -> Unit, onFailure: () -> Unit) =
        operations.submit(platform.restoredMessage, onSuccess, onFailure) {
            platform.restoreBackup(file)
        }

    fun canReset(): Boolean {
        if (!recordingActive()) return true
        operations.update {
            it.copy(error = "Finish the active recording before deleting app data.")
        }
        return false
    }

    fun reset(onSuccess: () -> Unit, onFailure: () -> Unit) =
        operations.submit("Local app data deleted.", onSuccess, onFailure) { platform.reset() }

    fun setMilestonesEnabled(enabled: Boolean) =
        operations.submit { settings.putSetting("milestones.enabled", enabled.toString()) }

    fun saveReminderPreferences(preferences: ReminderSettings) =
        operations.submit(
            "Reminder preferences saved. Reminders only apply to actual scheduled workouts."
        ) {
            platform.saveReminderPreferences(preferences)
        }
}

/** Actual document IO, original destructive-data owner and OS reminder scheduling remain native. */
interface SettingsPlatform<File> {
    val restoredMessage: String

    fun exportBackup(file: File)

    fun restoreBackup(file: File)

    fun reset()

    fun saveReminderPreferences(preferences: ReminderSettings)
}
