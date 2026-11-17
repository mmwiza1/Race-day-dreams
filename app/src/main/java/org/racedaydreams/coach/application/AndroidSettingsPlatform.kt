package org.racedaydreams.coach.application

import android.app.Application
import android.net.Uri
import java.io.ByteArrayOutputStream
import org.racedaydreams.data.BackupRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.reminders.ReminderPreferences
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.portability.BackupManager
import org.racedaydreams.reminders.EveningReminders

/** Original Android document, backup and alarm boundaries; no second restore/reset engine. */
class AndroidSettingsPlatform(
    private val app: Application,
    private val backups: BackupRepository,
    private val profiles: SqliteUserPreferencesRepository,
    private val reminders: EveningReminders,
) : SettingsPlatform<Uri> {
    override val restoredMessage =
        "Backup restored. Data from Health Connect can be imported again."

    override fun exportBackup(file: Uri) {
        // Prepare before opening an existing document in truncating mode.
        val prepared =
            ByteArrayOutputStream().also { BackupManager(app, backups).exportTo(it) }.toByteArray()
        val output =
            app.contentResolver.openOutputStream(file, "wt")
                ?: throw IllegalArgumentException("The selected document could not be opened.")
        output.use { it.write(prepared) }
    }

    override fun restoreBackup(file: Uri) {
        reminders.cancelAllScheduled()
        try {
            val input =
                app.contentResolver.openInputStream(file)
                    ?: throw IllegalArgumentException("The selected backup could not be opened.")
            input.use { BackupManager(app, backups).restoreFrom(it) }
        } finally {
            // Invalid backups retain the old plan; its alarms must also be reconciled.
            reminders.reconcile()
        }
    }

    override fun reset() {
        reminders.cancelAllScheduled()
        try {
            profiles.clear()
        } finally {
            reminders.reconcile()
        }
    }

    override fun saveReminderPreferences(preferences: ReminderSettings) {
        reminders.savePreferences(ReminderPreferences.fromShared(preferences))
    }
}
