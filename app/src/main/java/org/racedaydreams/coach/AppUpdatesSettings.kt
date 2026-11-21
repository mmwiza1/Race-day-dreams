package org.racedaydreams.coach

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.domain.update.AppUpdates.Incompatibility
import org.racedaydreams.domain.update.AvailableUpdate
import org.racedaydreams.update.AppUpdateCoordinator
import org.racedaydreams.update.AppUpdateState
import org.racedaydreams.update.UpdateInstaller
import org.racedaydreams.update.UpdateStatus

const val RELEASE_PAGE = "https://github.com/mmwiza1/Race-day-dreams/releases/tag/development"

/**
 * Settings › App updates, bound to the process-wide update coordinator. The installer hand-offs
 * ([effects]) must stay composed for the whole Settings screen, even while the section that shows
 * the [card] is collapsed, so Settings composes them separately from the card.
 */
@Composable
fun AppUpdatesSettings(effects: Boolean = true, card: Boolean = true) {
    val context = LocalContext.current
    val coordinator = remember { AppUpdateCoordinator.get(context) }
    val state by coordinator.state.collectAsStateWithLifecycle()
    if (effects) {
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            coordinator.refreshActiveWork()
            coordinator.installPermissionReturned()
        }
        val status = state.status
        if (status is UpdateStatus.AwaitingApproval)
            LaunchedEffect(status) {
                // Android's own approval screen; the person decides whether to install.
                context.startActivity(status.confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                coordinator.approvalShown()
            }
        if (status is UpdateStatus.NeedsInstallPermission)
            LaunchedEffect(status) {
                context.startActivity(UpdateInstaller.installPermissionIntent(context))
            }
    }
    if (!card) return
    AppUpdatesCard(
        state,
        AppUpdateActions(
            check = { coordinator.check(manual = true) },
            download = coordinator::download,
            cancel = coordinator::cancelDownload,
            install = coordinator::install,
            later = coordinator::later,
            skip = coordinator::skip,
            automatic = coordinator::setAutomaticChecks,
            notifications = coordinator::setNotifications,
            openReleasePage = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE)))
            },
        ),
    )
}

class AppUpdateActions(
    val check: () -> Unit = {},
    val download: () -> Unit = {},
    val cancel: () -> Unit = {},
    val install: () -> Unit = {},
    val later: () -> Unit = {},
    val skip: () -> Unit = {},
    val automatic: (Boolean) -> Unit = {},
    val notifications: (Boolean) -> Unit = {},
    val openReleasePage: () -> Unit = {},
)

private fun megabytes(bytes: Long) = "%.1f MB".format(bytes / 1_000_000.0)

private fun versionLabel(update: AvailableUpdate) =
    "${update.versionName} (build ${update.versionCode})"

/** Stateless card so every status can be tested and captured without network access. */
@Composable
fun AppUpdatesCard(state: AppUpdateState, actions: AppUpdateActions) =
    CoachCard {
        val installed = state.installed
        Text(
            "App updates",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("app-updates"),
        )
        Text(
            "Installed: ${installed.versionName} (build ${installed.versionCode})",
            modifier = Modifier.testTag("app-updates-installed"),
        )
        val status = state.status
        val update: AvailableUpdate? =
            when (status) {
                is UpdateStatus.Available -> status.update
                is UpdateStatus.Downloading -> status.update
                is UpdateStatus.Ready -> status.update
                is UpdateStatus.NeedsInstallPermission -> status.update
                is UpdateStatus.Installing -> status.update
                is UpdateStatus.AwaitingApproval -> status.update
                else -> null
            }
        StatusText(status)
        val blocked = state.activeWork != null && update != null
        if (blocked)
            Text(
                "${state.activeWork} Updates wait until it's finished or saved, so nothing is interrupted.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("app-updates-active-work"),
            )
        update?.let { ReleaseNotes(it) }
        when (status) {
            is UpdateStatus.Available -> {
                Button(
                    actions.download,
                    enabled = !blocked,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("app-updates-download"),
                ) {
                    Text("Download update (${megabytes(status.update.apkBytes)})")
                }
                // Stacked, so both stay readable at 200% text on narrow phones.
                if (status.prompt) {
                    TextButton(actions.later, Modifier.heightIn(min = 48.dp)) {
                        Text("Remind me later")
                    }
                    TextButton(actions.skip, Modifier.heightIn(min = 48.dp)) {
                        Text("Skip this version")
                    }
                }
            }
            is UpdateStatus.Downloading -> {
                val fraction =
                    if (status.total > 0) (status.bytes.toFloat() / status.total).coerceIn(0f, 1f)
                    else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            contentDescription = "Download ${(fraction * 100).toInt()} percent"
                        },
                )
                OutlinedButton(actions.cancel, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Cancel download")
                }
            }
            is UpdateStatus.Ready -> {
                status.note?.let { Text(it) }
                Button(
                    actions.install,
                    enabled = !blocked,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("app-updates-install"),
                ) {
                    Text("Install update")
                }
                Text(
                    "Android will ask you to approve the update. Your training data stays on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            is UpdateStatus.Failed -> {
                Button(
                    when (status.retry) {
                        UpdateStatus.Retry.CHECK -> actions.check
                        UpdateStatus.Retry.DOWNLOAD -> actions.download
                        UpdateStatus.Retry.INSTALL -> actions.install
                    },
                    enabled = status.retry == UpdateStatus.Retry.CHECK || status.update != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Try again")
                }
            }
            is UpdateStatus.Incompatible ->
                if (status.reason == Incompatibility.SIGNING) MoveToUpdateReadyVersion(actions)
            else -> Unit
        }
        if (!installed.updateCapable && status !is UpdateStatus.Incompatible)
            Text(
                "This copy is a test build signed with a temporary key. It never checks for updates on its own, and newer versions need a one-time reinstall with a backup.",
                style = MaterialTheme.typography.bodyMedium,
            )
        val idle =
            when (status) {
                UpdateStatus.Checking,
                is UpdateStatus.Downloading,
                is UpdateStatus.Installing,
                is UpdateStatus.AwaitingApproval -> false
                else -> true
            }
        if (idle)
            OutlinedButton(
                actions.check,
                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("app-updates-check"),
            ) {
                Text("Check for updates")
            }
        CheckRow(
            "Check automatically once a day when I open the app",
            state.preferences.automaticChecks,
            actions.automatic,
        )
        CheckRow(
            "Notify me when an update is available",
            state.preferences.notifications,
            actions.notifications,
        )
        Text(
            "Checking reads the latest release information from GitHub over an encrypted connection. No account, identifier or training data is sent. Downloads happen only when you choose.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

@Composable
private fun StatusText(status: UpdateStatus) {
    val text =
        when (status) {
            UpdateStatus.Idle -> "Check whether a newer version is available."
            UpdateStatus.Checking -> "Checking for updates…"
            UpdateStatus.UpToDate -> "You have the latest version."
            UpdateStatus.NoRelease ->
                "No update-ready release has been published yet. You have the latest version available."
            is UpdateStatus.Available -> "Update available: ${versionLabel(status.update)}."
            is UpdateStatus.Incompatible ->
                when (status.reason) {
                    Incompatibility.SIGNING ->
                        "Version ${versionLabel(status.update)} is available, but this copy was signed with a different key, so Android can't install it as an update."
                    Incompatibility.ANDROID_VERSION ->
                        "Version ${versionLabel(status.update)} needs a newer version of Android than this phone has. Your current version keeps working."
                }
            is UpdateStatus.Downloading ->
                "Downloading ${versionLabel(status.update)}… ${megabytes(status.bytes)} of ${megabytes(status.total)}"
            is UpdateStatus.Ready -> "${versionLabel(status.update)} is downloaded and verified."
            is UpdateStatus.NeedsInstallPermission ->
                "Allow Race Day Dreams to install updates in the Android screen that opened, then return here."
            is UpdateStatus.Installing,
            is UpdateStatus.AwaitingApproval -> "Waiting for Android to install the update…"
            is UpdateStatus.Failed -> status.message
        }
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color =
            if (status is UpdateStatus.Failed) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("app-updates-status"),
    )
}

@Composable
private fun ReleaseNotes(update: AvailableUpdate) {
    if (update.notes.isBlank()) return
    var open by rememberSaveable(update.versionCode) { mutableStateOf(false) }
    TextButton({ open = !open }, Modifier.heightIn(min = 48.dp)) {
        Text(if (open) "Hide what's new" else "What's new in ${update.versionName}")
    }
    if (open) Text(update.notes, modifier = Modifier.testTag("app-updates-notes"))
}

@Composable
private fun MoveToUpdateReadyVersion(actions: AppUpdateActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton({ open = !open }, Modifier.heightIn(min = 48.dp)) {
        Text(if (open) "Hide the steps" else "How to move to the update-ready version safely")
    }
    if (!open) return
    Text(
        "Android only updates an app in place when both copies are signed with the same key. Moving once keeps your data if you follow these steps:",
    )
    listOf(
            "1. Finish or save any run, gym workout or bouldering session.",
            "2. In Record a run, choose Complete backup (app + recordings) and save the file somewhere you trust, ideally copied off this phone too.",
            "3. Check the file is in your chosen folder and is not empty. Keep it until step 6 is done.",
            "4. Uninstall this copy of Race Day Dreams. Uninstalling deletes its on-phone data, which is why the backup comes first.",
            "5. Install the update-ready version from the release page and open it.",
            "6. In Record a run, choose Restore complete backup…, pick your file and select Validate and restore. Check your history, plans, gym and bouldering sessions, shoes and measurements are there.",
            "Health Connect permissions are granted again in the new copy. After this one move, future versions install as normal updates.",
        )
        .forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    OutlinedButton(actions.openReleasePage, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Open the release page")
    }
}
