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
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

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

    /** The file changed between verification and the install session (audit R-7). */
    class ChangedAfterVerification : IOException("The update file changed after it was verified")

    /**
     * Copies [input] to [output] while hashing exactly the bytes written, and throws
     * [ChangedAfterVerification] unless they match [expectedSha256] and [expectedBytes]. This closes
     * the gap between [ApkVerifier.verify] and the bytes Android actually receives.
     */
    @Throws(IOException::class)
    fun copyVerified(
        input: InputStream,
        output: OutputStream,
        expectedSha256: String,
        expectedBytes: Long,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1 shl 16)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > expectedBytes) throw ChangedAfterVerification()
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
        }
        if (total != expectedBytes || ApkVerifier.hex(digest.digest()) != expectedSha256)
            throw ChangedAfterVerification()
    }

    /**
     * Starts an install session; the result arrives in [UpdateInstallReceiver]. The bytes written
     * to the session must still be the verified [expectedSha256]; otherwise the session is
     * abandoned before commit and [ChangedAfterVerification] is thrown.
     */
    @Throws(IOException::class)
    fun commit(context: Context, apk: File, expectedSha256: String) {
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
            val size = apk.length()
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, size).use { output ->
                    copyVerified(input, output, expectedSha256, size)
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
