package org.racedaydreams.coach.application

import android.app.Application
import android.net.Uri
import java.io.ByteArrayOutputStream
import org.racedaydreams.data.BackupRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.reminders.ReminderPreferences
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.settings.PrimaryFocusRepository
import org.racedaydreams.portability.BackupManager
import org.racedaydreams.reminders.EveningReminders

/** User-selected portability/settings operations over the existing shared transaction. */
class SettingsCoordinator(
    private val app: Application,
    private val operations: CoachOperationRunner,
    private val backups: BackupRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val focus: PrimaryFocusRepository,
    private val profiles: SqliteUserPreferencesRepository,
    private val reminders: EveningReminders,
    private val recordingActive: () -> Boolean,
) {
    fun startingPath(path: String, onSuccess: () -> Unit = {}) =
        operations.submit(onSuccess = onSuccess) {
            require(path in listOf("record", "race", "hybrid"))
            settings.putSetting("starting.path", path)
            focus.save(PrimaryFocus.fromLegacyStartingPath(path).get())
        }

    fun primaryFocus(selected: PrimaryFocus, onSuccess: () -> Unit = {}) =
        operations.submit(onSuccess = onSuccess) { focus.save(selected) }

    fun setting(key: String, value: String) =
        operations.submit {
            require(key in setOf("calendar.view", "mass.unit", "provider.preference"))
            settings.putSetting(key, value)
        }

    fun exportBackup(uri: Uri) =
        operations.submit("Backup saved to the file you selected. Keep it somewhere you trust.") {
            // Prepare before opening an existing document in truncating mode.
            val prepared =
                ByteArrayOutputStream()
                    .also { BackupManager(app, backups).exportTo(it) }
                    .toByteArray()
            val output =
                app.contentResolver.openOutputStream(uri, "wt")
                    ?: throw IllegalArgumentException("The selected document could not be opened.")
            output.use { it.write(prepared) }
        }

    fun restoreBackup(uri: Uri, onSuccess: () -> Unit, onFailure: () -> Unit) =
        operations.submit(
            "Backup restored. Data from Health Connect can be imported again.",
            onSuccess,
            onFailure,
        ) {
            reminders.cancelAllScheduled()
            try {
                val input =
                    app.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException(
                            "The selected backup could not be opened."
                        )
                input.use { BackupManager(app, backups).restoreFrom(it) }
            } finally {
                // Invalid backups retain the old plan; its alarms must also be reconciled.
                reminders.reconcile()
            }
        }

    fun canReset(): Boolean {
        if (!recordingActive()) return true
        operations.update {
            it.copy(error = "Finish the active recording before deleting app data.")
        }
        return false
    }

    fun reset(onSuccess: () -> Unit, onFailure: () -> Unit) =
        operations.submit("Local app data deleted.", onSuccess, onFailure) {
            reminders.cancelAllScheduled()
            try {
                profiles.clear()
            } finally {
                reminders.reconcile()
            }
        }

    fun setMilestonesEnabled(enabled: Boolean) =
        operations.submit { settings.putSetting("milestones.enabled", enabled.toString()) }

    fun saveReminderPreferences(preferences: ReminderPreferences) =
        operations.submit(
            "Reminder preferences saved. Reminders only apply to actual scheduled workouts."
        ) {
            reminders.savePreferences(preferences)
        }
}
