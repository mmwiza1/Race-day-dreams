package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.SharedBackupSettings
import org.racedaydreams.coach.SharedPrimaryFocusSettings
import org.racedaydreams.coach.documents.AppleBackupDocuments
import org.racedaydreams.domain.storage.DetailRetention
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
        TextButton(
            enabled = !importing && !exporting && !state.busy,
            onClick = {
                importing = true
                message = null
                try {
                    session.backupDocuments.chooseGpx(presenter) { url ->
                        if (url == null) importing = false
                        else
                            scope.launch {
                                try {
                                    session.backupDocuments.importGpx(url)
                                    session.services.operations.refresh()
                                    message = "The GPX run is available in history."
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    message = failure.message ?: "Could not import the GPX track."
                                } finally {
                                    importing = false
                                }
                            }
                    }
                } catch (failure: Exception) {
                    importing = false
                    message = failure.message ?: "Could not open the GPX picker."
                }
            },
        ) {
            Text("Import recorded route (GPX)")
        }
        AppleScheduleSettings(session.runningSupport)
        AppleDreamSettings(session.runningSupport)
        AppleRecordingStorage(session.backupDocuments)
        ApplePrivacyAndLicences()
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
                AppleRetentionPreferences(documents) { usage = it }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleRetentionPreferences(
    documents: AppleBackupDocuments,
    onInventory: (SqliteRecordingInventory.Usage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var preferences by remember { mutableStateOf<DetailRetention.Preferences?>(null) }
    var selected by remember { mutableStateOf(DetailRetention.Policy.KEEP_FOREVER) }
    var preview by remember { mutableStateOf<DetailRetention.Preview?>(null) }
    var confirming by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var cleanupReview by remember { mutableStateOf<DetailRetention.Preview?>(null) }
    var cleanupStatus by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        val saved = documents.retentionPreferences()
        preferences = saved
        selected = saved.policy()
    }
    fun operate(action: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error =
                    "Could not access retention settings. Try again when other data operations finish."
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(documents) { operate { load() } }

    Text("Detailed recording retention")
    val saved = preferences
    if (saved != null) {
        Text("Saved choice: ${retentionLabel(saved.policy())}")
        if (saved.choice() == DetailRetention.Choice.EXISTING_UNREVIEWED) {
            Text("Your existing recordings are protected until you review a retention choice.")
        }
        Text(
            "A shorter choice can make old map routes and detailed sensor readings eligible for future cleanup. Workout summaries and training history stay."
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DetailRetention.Policy.entries.forEach { policy ->
                FilterChip(
                    selected = selected == policy,
                    onClick = {
                        selected = policy
                        preview = null
                    },
                    label = { Text(retentionLabel(policy)) },
                    enabled = !busy,
                )
            }
        }
        TextButton(onClick = { confirming = true }, enabled = !busy) {
            Text("Review retention choice")
        }
        TextButton(
            onClick = { operate { preview = documents.previewRetention(selected) } },
            enabled = !busy,
        ) {
            Text("Preview eligible detail")
        }
    } else if (error != null) {
        TextButton(onClick = { operate { load() } }, enabled = !busy) {
            Text("Retry retention settings")
        }
    }
    preview?.let { inspected ->
        Text(
            "${inspected.recordings().size} finished recordings eligible for the selected policy. ${inspected.points()} recorded points and ${inspected.events()} events."
        )
        if (
            saved != null &&
                saved.choice() != DetailRetention.Choice.EXISTING_UNREVIEWED &&
                saved.policy() == inspected.policy() &&
                inspected.recordings().isNotEmpty()
        ) {
            TextButton(onClick = { cleanupReview = inspected }, enabled = !busy) {
                Text("Review removal")
            }
        }
    }
    cleanupStatus?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    cleanupReview?.let { inspected ->
        AlertDialog(
            onDismissRequest = { cleanupReview = null },
            title = { Text("Remove detailed recordings?") },
            text = {
                Text(
                    "Remove GPS routes and sensor detail from ${inspected.recordings().size} finished recordings? Saved summaries, personal bests and training history stay. Removed detail can only be restored from a compatible backup that contains it."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        cleanupReview = null
                        preview = null
                        cleanupStatus = null
                        operate {
                            var removed = 0
                            try {
                                documents.cleanRetention(inspected) {
                                    removed = it
                                    cleanupStatus =
                                        "Removed detail from $removed recordings so far."
                                }
                                cleanupStatus =
                                    "Removed detail from $removed recordings. Saved summaries and training history remain."
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                cleanupStatus =
                                    "Cleanup stopped after $removed recordings. Finish any open run or data operation, then check a fresh preview."
                            }
                            onInventory(documents.recordingInventory().usage)
                        }
                    }
                ) {
                    Text("Remove recording detail")
                }
            },
            dismissButton = { TextButton(onClick = { cleanupReview = null }) { Text("Cancel") } },
        )
    }
    error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Save ${retentionLabel(selected)}?") },
            text = {
                Text(
                    "With a shorter policy, future cleanup can permanently remove detailed routes and sensor readings. Saving this choice does not delete any recordings."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        operate {
                            documents.acknowledgeRetention(selected)
                            load()
                        }
                    }
                ) {
                    Text("Save retention choice")
                }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }
}

private fun retentionLabel(policy: DetailRetention.Policy): String =
    when (policy) {
        DetailRetention.Policy.LAST_24_MONTHS -> "24 months"
        DetailRetention.Policy.LAST_5_YEARS -> "5 years"
        DetailRetention.Policy.KEEP_FOREVER -> "Keep forever"
    }
