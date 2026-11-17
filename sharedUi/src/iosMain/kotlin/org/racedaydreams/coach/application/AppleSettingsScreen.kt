package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SharedBackupSettings
import org.racedaydreams.coach.SharedPrimaryFocusSettings
import org.racedaydreams.coach.documents.AppleBackupDocuments
import platform.UIKit.UIViewController

/** iOS settings content wired to the real session and system document picker. */
@Composable
fun AppleSettingsScreen(session: AppleCoachSession<*>, presenter: UIViewController) {
    val state by session.services.operations.state.collectAsState()
    val reminderFailure by session.platform.reminderFailure.collectAsState()
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun export(kind: AppleBackupDocuments.Kind) {
        if (exporting) return
        exporting = true
        message = null
        scope.launch {
            try {
                session.backupDocuments.export(kind, presenter) { outcome ->
                    exporting = false
                    message =
                        when (outcome) {
                            AppleBackupDocuments.Outcome.SAVED ->
                                "Backup saved. Keep it somewhere you trust."
                            AppleBackupDocuments.Outcome.CANCELLED -> "Backup export cancelled."
                        }
                }
            } catch (cancelled: CancellationException) {
                exporting = false
                throw cancelled
            } catch (failure: Exception) {
                exporting = false
                message = failure.message ?: "Could not export the backup."
            }
        }
    }

    CoachPage {
        CoachHeading("Settings")
        SharedPrimaryFocusSettings(state) { session.services.settings.primaryFocus(it) }
        SharedBackupSettings(
            busy = state.busy || exporting,
            export = { export(AppleBackupDocuments.Kind.APP) },
            completeExport = { export(AppleBackupDocuments.Kind.COMPLETE) },
            description =
                "The app backup saves your goals, workouts and settings. " +
                    "The complete backup also includes detailed recordings still on this device. " +
                    "These backups are not encrypted by the app. Keep them somewhere you trust.",
        )
        val feedback = message ?: state.error ?: reminderFailure
        if (feedback != null) {
            Text(feedback, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}
