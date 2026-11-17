package org.racedaydreams.coach.application

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SharedBackupSettings
import org.racedaydreams.coach.SharedPrimaryFocusSettings
import org.racedaydreams.coach.documents.AppleBackupDocuments
import org.racedaydreams.storage.LocalBackupOperations.ReviewedBackup
import org.racedaydreams.storage.SqliteRecordingInventory
import platform.UIKit.UIViewController

/** iOS settings content wired to the real session and system document picker. */
@Composable
fun AppleSettingsScreen(
    session: AppleCoachSession<*>,
    presenter: UIViewController,
    onRestore: (ReviewedBackup) -> Unit,
    health: @Composable () -> Unit = {},
) {
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

    var importing by remember { mutableStateOf(false) }
    var reviewed by remember { mutableStateOf<ReviewedBackup?>(null) }

    fun dismissReview() {
        val previous = reviewed
        reviewed = null
        previous?.close()
    }
    DisposableEffect(Unit) { onDispose { dismissReview() } }

    fun importApp() {
        importing = true
        message = null
        try {
            session.backupDocuments.chooseBackup(presenter) { url ->
                if (url == null) importing = false
                else
                    scope.launch {
                        try {
                            reviewed = session.backupDocuments.review(url)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            message = failure.message ?: "Could not read the backup."
                        } finally {
                            importing = false
                        }
                    }
            }
        } catch (failure: Exception) {
            importing = false
            message = failure.message ?: "Could not open the backup picker."
        }
    }

    reviewed?.let { backup ->
        AlertDialog(
            onDismissRequest = ::dismissReview,
            title = { Text("Replace app data?") },
            text = {
                Text(
                    "Backup created ${backup.createdAt}. Restoring replaces your current app data with this backup. " +
                        (if (backup.includesRecordings) "This backup includes detailed recordings. "
                        else "") +
                        "Finish any open run or gym workout first."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reviewed = null
                        onRestore(backup)
                    }
                ) {
                    Text("Replace app data")
                }
            },
            dismissButton = { TextButton(onClick = ::dismissReview) { Text("Cancel") } },
        )
    }

    CoachPage {
        CoachHeading("Settings")
        health()
        SharedPrimaryFocusSettings(state) { session.services.settings.primaryFocus(it) }
        SharedBackupSettings(
            busy = state.busy || exporting || importing,
            restore = ::importApp,
            export = { export(AppleBackupDocuments.Kind.APP) },
            completeExport = { export(AppleBackupDocuments.Kind.COMPLETE) },
            description =
                "The app backup saves your goals, workouts and settings. " +
                    "The complete backup also includes detailed recordings still on this device. " +
                    "These backups are not encrypted by the app. Keep them somewhere you trust.",
        )
        AppleRecordingStorage(session.backupDocuments)
        val feedback = message ?: state.error ?: state.message ?: reminderFailure
        if (feedback != null) {
            Text(feedback, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
}

@Composable
private fun AppleRecordingStorage(documents: AppleBackupDocuments) {
    var expanded by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf<SqliteRecordingInventory.Usage?>(null) }
    var unavailable by remember { mutableStateOf(false) }
    LaunchedEffect(expanded, documents) {
        if (expanded) {
            usage = null
            unavailable = false
            try {
                usage = documents.recordingInventory().usage
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                unavailable = true
            }
        }
    }
    TextButton(
        onClick = { expanded = !expanded },
        modifier =
            Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
    ) {
        Text("Recording storage")
    }
    if (expanded) {
        val current = usage
        when {
            unavailable ->
                Text(
                    "Recording storage is temporarily unavailable. Close and reopen this section to try again."
                )
            current == null -> Text("Checking recording storage…")
            else -> {
                Text("${current.activityRows} activity summaries retained")
                Text(
                    "${current.pointRows} recorded points and ${current.eventRows} recording events"
                )
                Text("Detailed recording content: ${current.detailPayloadBytes} bytes")
                Text(
                    "This counts stored recording content. Database files, backups and other app data use additional space."
                )
            }
        }
    }
}
