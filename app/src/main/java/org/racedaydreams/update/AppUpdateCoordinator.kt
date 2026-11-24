package org.racedaydreams.update

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.racedaydreams.R
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AppUpdates.Decision
import org.racedaydreams.domain.update.AppUpdates.Incompatibility
import org.racedaydreams.domain.update.AvailableUpdate
import org.racedaydreams.domain.update.UpdatePreferences

/** What the App updates card shows. */
sealed class UpdateStatus {
    object Idle : UpdateStatus()

    object Checking : UpdateStatus()

    object UpToDate : UpdateStatus()

    /** No consistently signed, update-ready build has been published yet. */
    object NoRelease : UpdateStatus()

    data class Available(val update: AvailableUpdate, val prompt: Boolean) : UpdateStatus()

    data class Incompatible(val update: AvailableUpdate, val reason: Incompatibility) :
        UpdateStatus()

    data class Downloading(val update: AvailableUpdate, val bytes: Long, val total: Long) :
        UpdateStatus()

    /** Downloaded and verified; [note] explains a cancelled or failed install attempt. */
    data class Ready(val update: AvailableUpdate, val note: String? = null) : UpdateStatus()

    data class NeedsInstallPermission(val update: AvailableUpdate) : UpdateStatus()

    data class Installing(val update: AvailableUpdate) : UpdateStatus()

    /** Android's approval screen must be shown from the foreground app. */
    data class AwaitingApproval(val update: AvailableUpdate, val confirmation: Intent) :
        UpdateStatus()

    data class Failed(val message: String, val update: AvailableUpdate?, val retry: Retry) :
        UpdateStatus()

    enum class Retry {
        CHECK,
        DOWNLOAD,
        INSTALL,
    }
}

data class AppUpdateState(
    val installed: InstalledApp,
    val preferences: UpdatePreferences,
    val status: UpdateStatus = UpdateStatus.Idle,
    /** In-progress work that an update must not interrupt, if any. */
    val activeWork: String? = null,
)

/**
 * Checks, downloads, verifies and hands updates to Android's installer. One instance per process
 * so the install result receiver and the Settings card share state. Runs only when the app is
 * opened or the person acts: no background service, alarm, wake lock or periodic job.
 */
class AppUpdateCoordinator(
    context: Context,
    private val client: AppUpdateClient = AppUpdateClient(),
    private val nowEpochSecond: () -> Long = { System.currentTimeMillis() / 1000 },
    private val installedApp: () -> InstalledApp = { InstalledApp.read(context) },
    private val activeWork: () -> String? = { org.racedaydreams.data.ActiveWork.describe(context) },
) {
    private val app = context.applicationContext
    private val store = UpdatePreferencesStore(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cancelRequested = AtomicBoolean(false)
    private var job: Job? = null
    private val mutable = MutableStateFlow(AppUpdateState(installedApp(), store.read()))
    val state: StateFlow<AppUpdateState> = mutable.asStateFlow()

    private val directory = File(app.cacheDir, "updates")

    private fun file(update: AvailableUpdate) = File(directory, update.apkName)

    private val userAgent
        get() = "RaceDayDreams/${mutable.value.installed.versionName} (Android; update check)"

    private fun preferences(change: (UpdatePreferences) -> UpdatePreferences) {
        val updated = change(store.read())
        store.write(updated)
        mutable.update { it.copy(preferences = updated) }
    }

    private fun status(value: UpdateStatus) = mutable.update { it.copy(status = value) }

    private fun busy(): Boolean =
        when (mutable.value.status) {
            UpdateStatus.Checking,
            is UpdateStatus.Downloading,
            is UpdateStatus.Installing,
            is UpdateStatus.AwaitingApproval -> true
            else -> false
        }

    /** Called when the app comes to the foreground: cleans up and checks at most once a day. */
    fun onAppStarted() {
        scope.launch {
            val installed = installedApp()
            mutable.update { it.copy(installed = installed) }
            // After a successful update the process restarts with no update in hand, and the
            // downloaded file is no longer needed. Never remove a download that is still in use,
            // e.g. when returning from Android's approval or "Install unknown apps" screens.
            if (currentUpdate() == null) directory.listFiles()?.forEach { it.delete() }
            val work = activeWork()
            mutable.update { it.copy(activeWork = work) }
            // Only builds signed with the release key check on their own; test builds never do.
            if (
                installed.updateCapable &&
                    AppUpdates.shouldCheckAutomatically(store.read(), nowEpochSecond(), work != null)
            )
                check(manual = false)
        }
    }

    /** Re-reads whether a run, workout or bouldering session is in progress. */
    fun refreshActiveWork() {
        scope.launch { mutable.update { it.copy(activeWork = activeWork()) } }
    }

    fun check(manual: Boolean = true) {
        if (busy()) return
        status(UpdateStatus.Checking)
        job =
            scope.launch {
                val installed = installedApp()
                mutable.update { it.copy(installed = installed, activeWork = activeWork()) }
                val update =
                    try {
                        client.fetchManifest(installed.packageName, userAgent)
                    } catch (failure: UpdateException) {
                        preferences { it.checked(nowEpochSecond()) }
                        status(
                            when {
                                failure.failure is UpdateFailure.NoRelease -> UpdateStatus.NoRelease
                                // An automatic check that fails offline stays quiet.
                                manual ->
                                    UpdateStatus.Failed(
                                        failure.failure.message,
                                        null,
                                        UpdateStatus.Retry.CHECK,
                                    )
                                else -> UpdateStatus.Idle
                            }
                        )
                        return@launch
                    }
                preferences { it.checked(nowEpochSecond()) }
                val decision =
                    AppUpdates.evaluate(
                        installed.versionCode,
                        installed.certificateSha256 ?: "",
                        Build.VERSION.SDK_INT,
                        update,
                        store.read(),
                        nowEpochSecond(),
                    )
                status(
                    when (decision) {
                        Decision.UpToDate -> UpdateStatus.UpToDate
                        is Decision.Incompatible ->
                            UpdateStatus.Incompatible(decision.update, decision.reason)
                        is Decision.Available ->
                            if (verifiedDownload(decision.update, installed))
                                UpdateStatus.Ready(decision.update)
                            else UpdateStatus.Available(decision.update, decision.prompt)
                    }
                )
                if (!manual && AppUpdates.shouldNotify(store.read(), decision)) {
                    if (notify((decision as Decision.Available).update))
                        preferences { it.notified(decision.update.versionCode) }
                }
            }
    }

    private fun verifiedDownload(update: AvailableUpdate, installed: InstalledApp): Boolean {
        val apk = file(update)
        return apk.isFile &&
            ApkVerifier.verify(app, apk, update, installed) == ApkVerifier.Result.Verified
    }

    fun download() {
        val update = currentUpdate() ?: return
        if (busy()) return
        cancelRequested.set(false)
        status(UpdateStatus.Downloading(update, 0, update.apkBytes))
        job =
            scope.launch {
                activeWork()?.let { work ->
                    mutable.update { it.copy(activeWork = work) }
                    status(
                        UpdateStatus.Failed(
                            "$work Updates wait until it is finished or saved, so nothing is interrupted.",
                            update,
                            UpdateStatus.Retry.DOWNLOAD,
                        )
                    )
                    return@launch
                }
                val target = file(update)
                directory.listFiles()?.forEach { if (it.name != target.name) it.delete() }
                var lastPercent = -1L
                try {
                    client.download(update, target, userAgent, cancelRequested::get) { bytes, total ->
                        val percent = bytes * 100 / total
                        if (percent != lastPercent) {
                            lastPercent = percent
                            status(UpdateStatus.Downloading(update, bytes, total))
                        }
                    }
                } catch (failure: UpdateException) {
                    target.delete()
                    status(
                        if (failure.failure is UpdateFailure.Cancelled)
                            UpdateStatus.Available(update, prompt = false)
                        else
                            UpdateStatus.Failed(
                                failure.failure.message,
                                update,
                                UpdateStatus.Retry.DOWNLOAD,
                            )
                    )
                    return@launch
                }
                when (val result = ApkVerifier.verify(app, target, update, installedApp())) {
                    ApkVerifier.Result.Verified -> status(UpdateStatus.Ready(update))
                    is ApkVerifier.Result.Rejected -> {
                        target.delete()
                        status(UpdateStatus.Failed(result.reason, update, UpdateStatus.Retry.DOWNLOAD))
                    }
                }
            }
    }

    fun cancelDownload() = cancelRequested.set(true)

    /** Re-verifies the file, then asks Android to install it with the person's approval. */
    fun install() {
        val update = currentUpdate() ?: return
        if (busy()) return
        status(UpdateStatus.Installing(update))
        job =
            scope.launch {
                activeWork()?.let { work ->
                    mutable.update { it.copy(activeWork = work) }
                    status(
                        UpdateStatus.Ready(
                            update,
                            "$work Install the update after it is finished or saved.",
                        )
                    )
                    return@launch
                }
                val apk = file(update)
                val verified = ApkVerifier.verify(app, apk, update, installedApp())
                if (verified is ApkVerifier.Result.Rejected) {
                    apk.delete()
                    status(UpdateStatus.Failed(verified.reason, update, UpdateStatus.Retry.DOWNLOAD))
                    return@launch
                }
                if (!UpdateInstaller.canInstall(app)) {
                    status(UpdateStatus.NeedsInstallPermission(update))
                    return@launch
                }
                try {
                    UpdateInstaller.commit(app, apk, update.apkSha256)
                } catch (_: UpdateInstaller.ChangedAfterVerification) {
                    apk.delete()
                    status(
                        UpdateStatus.Failed(
                            "The download is damaged or does not match the published release. It was deleted; try again.",
                            update,
                            UpdateStatus.Retry.DOWNLOAD,
                        )
                    )
                } catch (_: Exception) {
                    status(
                        UpdateStatus.Failed(
                            "Android couldn't start the installation. Your current version and data are unchanged; try again.",
                            update,
                            UpdateStatus.Retry.INSTALL,
                        )
                    )
                }
            }
    }

    /** Result from Android's installer (see [UpdateInstallReceiver]). */
    fun installResult(outcome: UpdateInstaller.Outcome) {
        val update = currentUpdate() ?: return
        status(
            when (outcome) {
                is UpdateInstaller.Outcome.NeedsApproval ->
                    UpdateStatus.AwaitingApproval(update, outcome.confirmation)
                // Android replaces the process on success; this is shown only if it is still running.
                UpdateInstaller.Outcome.Installed -> UpdateStatus.UpToDate
                UpdateInstaller.Outcome.Cancelled ->
                    UpdateStatus.Ready(
                        update,
                        "Installation cancelled. Your current version and data are unchanged.",
                    )
                is UpdateInstaller.Outcome.Failed ->
                    if (outcome.retryWithSameFile)
                        UpdateStatus.Failed(outcome.message, update, UpdateStatus.Retry.INSTALL)
                    else {
                        file(update).delete()
                        UpdateStatus.Failed(outcome.message, update, UpdateStatus.Retry.DOWNLOAD)
                    }
            }
        )
    }

    /** The approval screen was shown; Android reports the person's choice next. */
    fun approvalShown() {
        val current = mutable.value.status
        if (current is UpdateStatus.AwaitingApproval) status(UpdateStatus.Installing(current.update))
    }

    /** Back from Android's "Install unknown apps" setting. */
    fun installPermissionReturned() {
        val current = mutable.value.status
        if (current is UpdateStatus.NeedsInstallPermission) {
            if (UpdateInstaller.canInstall(app)) {
                status(UpdateStatus.Ready(current.update))
                install()
            } else
                status(
                    UpdateStatus.Ready(
                        current.update,
                        "Race Day Dreams needs your permission to install updates. You can still download them from the release page instead.",
                    )
                )
        }
    }

    fun later() {
        val update = currentUpdate() ?: return
        preferences { it.snooze(update.versionCode, nowEpochSecond()) }
        if (mutable.value.status is UpdateStatus.Available) status(UpdateStatus.Available(update, false))
    }

    fun skip() {
        val update = currentUpdate() ?: return
        preferences { it.skip(update.versionCode) }
        if (mutable.value.status is UpdateStatus.Available) status(UpdateStatus.Available(update, false))
    }

    fun setAutomaticChecks(enabled: Boolean) = preferences { it.copy(automaticChecks = enabled) }

    fun setNotifications(enabled: Boolean) = preferences { it.copy(notifications = enabled) }

    private fun currentUpdate(): AvailableUpdate? =
        when (val current = mutable.value.status) {
            is UpdateStatus.Available -> current.update
            is UpdateStatus.Downloading -> current.update
            is UpdateStatus.Ready -> current.update
            is UpdateStatus.NeedsInstallPermission -> current.update
            is UpdateStatus.Installing -> current.update
            is UpdateStatus.AwaitingApproval -> current.update
            is UpdateStatus.Failed -> current.update
            else -> null
        }

    /** Posts one quiet notification, only if Android notifications are already allowed. */
    private fun notify(update: AvailableUpdate): Boolean {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        )
            return false
        val manager = app.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) return false
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "App updates", NotificationManager.IMPORTANCE_LOW)
        )
        val launch =
            app.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                PendingIntent.getActivity(
                    app,
                    NOTIFICATION_ID,
                    it.putExtra(EXTRA_OPEN_UPDATES, true),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val text = "Version ${update.versionName} is ready to download from Settings › App updates."
        manager.notify(
            NOTIFICATION_ID,
            Notification.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Race Day Dreams update available")
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .apply { launch?.let(::setContentIntent) }
                .build(),
        )
        return true
    }

    companion object {
        const val CHANNEL = "app_updates"
        const val NOTIFICATION_ID = 7_001
        const val EXTRA_OPEN_UPDATES = "openAppUpdates"

        @Volatile private var instance: AppUpdateCoordinator? = null

        /** Tests substitute a coordinator with a fake network client; null restores the default. */
        @androidx.annotation.VisibleForTesting
        internal fun replaceForTesting(coordinator: AppUpdateCoordinator?) {
            synchronized(this) { instance = coordinator }
        }

        fun get(context: Context): AppUpdateCoordinator =
            instance
                ?: synchronized(this) {
                    instance ?: AppUpdateCoordinator(context.applicationContext).also { instance = it }
                }
    }
}
