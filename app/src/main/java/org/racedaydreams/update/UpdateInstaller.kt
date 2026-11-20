package org.racedaydreams.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import java.io.IOException

/**
 * Hands a verified APK to Android's standard installer. Android always asks the person to approve
 * the update; nothing is installed silently, and the APK never leaves this app's private storage.
 */
object UpdateInstaller {
    const val ACTION_STATUS = "org.racedaydreams.update.INSTALL_STATUS"

    /** Android lets each person decide whether Race Day Dreams may install updates. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        )

    /** Starts an install session; the result arrives in [UpdateInstallReceiver]. */
    @Throws(IOException::class)
    fun commit(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                if (Build.VERSION.SDK_INT >= 31)
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        val id = installer.createSession(params)
        val session = installer.openSession(id)
        try {
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output, 1 shl 16)
                    session.fsync(output)
                }
            }
            val callback =
                Intent(context, UpdateInstallReceiver::class.java)
                    .setAction(ACTION_STATUS)
                    .setPackage(context.packageName)
            // Mutable so Android can attach the status; the explicit target keeps it private.
            val flags =
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, id, callback, flags).intentSender)
        } catch (failure: Exception) {
            session.abandon()
            throw failure as? IOException ?: IOException(failure)
        } finally {
            session.close()
        }
    }

    /** Plain-language outcome of an install session status. */
    sealed class Outcome {
        data class NeedsApproval(val confirmation: Intent) : Outcome()

        object Installed : Outcome()

        object Cancelled : Outcome()

        data class Failed(val message: String, val retryWithSameFile: Boolean) : Outcome()
    }

    fun outcome(status: Int, confirmation: Intent?): Outcome =
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION ->
                confirmation?.let { Outcome.NeedsApproval(it) }
                    ?: Outcome.Failed("Android could not show the update approval. Try again.", true)
            PackageInstaller.STATUS_SUCCESS -> Outcome.Installed
            PackageInstaller.STATUS_FAILURE_ABORTED -> Outcome.Cancelled
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                Outcome.Failed(
                    "Android needs more free storage to install the update. Free some space and try again.",
                    true,
                )
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                Outcome.Failed(
                    "Android refused the update because it does not match this installation's signing key or device. Your current version and data are unchanged.",
                    false,
                )
            PackageInstaller.STATUS_FAILURE_INVALID ->
                Outcome.Failed("Android reported the update file as invalid. Download it again.", false)
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                Outcome.Failed(
                    "This device's settings or policy blocked the installation. Your current version and data are unchanged.",
                    true,
                )
            else ->
                Outcome.Failed(
                    "The installation did not complete. Your current version and data are unchanged; you can try again.",
                    true,
                )
        }
}

/** Receives install session results and reports them to the update coordinator. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UpdateInstaller.ACTION_STATUS) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        @Suppress("DEPRECATION")
        val confirmation =
            if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            else intent.getParcelableExtra(Intent.EXTRA_INTENT)
        AppUpdateCoordinator.get(context).installResult(UpdateInstaller.outcome(status, confirmation))
    }
}
