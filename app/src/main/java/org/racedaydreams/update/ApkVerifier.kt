package org.racedaydreams.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import java.io.File
import java.security.MessageDigest
import org.racedaydreams.domain.update.AvailableUpdate

/** This installation's identity, read from Android rather than from build constants. */
data class InstalledApp(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    /** SHA-256 of the current signing certificate, lower-case hex; null if unreadable. */
    val certificateSha256: String?,
    /** The project's release certificate fingerprint built into the app; empty if not set up. */
    val releaseCertificateSha256: String,
) {
    /**
     * Signed with the project's release key, so later releases can update it in place. Test
     * builds (temporary debug keys) are not, and never check for updates on their own.
     */
    val updateCapable: Boolean
        get() =
            releaseCertificateSha256.isNotEmpty() && certificateSha256 == releaseCertificateSha256

    companion object {
        const val RELEASE_CERTIFICATE_KEY = "org.racedaydreams.RELEASE_CERTIFICATE_SHA256"

        fun read(context: Context): InstalledApp {
            val info =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA,
                )
            val pinned =
                context.packageManager
                    .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                    .metaData
                    ?.getString(RELEASE_CERTIFICATE_KEY)
                    .orEmpty()
                    .trim()
                    .lowercase()
            return InstalledApp(
                context.packageName,
                info.versionName ?: "",
                info.longVersionCode,
                ApkVerifier.signerSha256(info),
                if (AppUpdatesPin.valid(pinned)) pinned else "",
            )
        }
    }
}

internal object AppUpdatesPin {
    fun valid(value: String) = org.racedaydreams.domain.update.AppUpdates.validSha256(value)
}

/**
 * Checks a downloaded APK before Android is asked to install it. Android enforces the same
 * signing rule at install time; checking first gives a clear message and never hands the system
 * installer a file that does not match the published release.
 */
object ApkVerifier {
    sealed class Result {
        object Verified : Result()

        data class Rejected(val reason: String) : Result()
    }

    fun verify(
        context: Context,
        apk: File,
        update: AvailableUpdate,
        installed: InstalledApp,
    ): Result {
        if (!apk.isFile || apk.length() != update.apkBytes)
            return Result.Rejected("The download is incomplete. Try again.")
        if (sha256(apk) != update.apkSha256)
            return Result.Rejected(
                "The download is damaged or does not match the published release. It was deleted; try again."
            )
        @Suppress("DEPRECATION")
        val info =
            context.packageManager.getPackageArchiveInfo(
                apk.path,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ) ?: return Result.Rejected("The download is not a valid Android app.")
        if (info.packageName != installed.packageName)
            return Result.Rejected("The download is for a different app.")
        if (info.longVersionCode != update.versionCode)
            return Result.Rejected("The download does not match the announced version.")
        if (info.longVersionCode <= installed.versionCode)
            return Result.Rejected("The download is not newer than the installed version.")
        val signer =
            signerSha256(info)
                ?: return Result.Rejected("The download is not signed as expected and was rejected.")
        if (installed.certificateSha256 == null || signer != installed.certificateSha256)
            return Result.Rejected(
                "The download is signed with a different key from this installation, so Android cannot update it in place. Nothing was changed."
            )
        if (signer != update.certificateSha256 || signer != installed.releaseCertificateSha256)
            return Result.Rejected("The download is not signed as expected and was rejected.")
        return Result.Verified
    }

    /** The single current signer; multi-signer or unreadable signing data is never trusted. */
    fun signerSha256(info: PackageInfo): String? {
        val signing = info.signingInfo ?: return null
        if (signing.hasMultipleSigners()) return null
        val signers: Array<Signature> = signing.apkContentsSigners ?: return null
        if (signers.size != 1) return null
        return hex(MessageDigest.getInstance("SHA-256").digest(signers[0].toByteArray()))
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return hex(digest.digest())
    }

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
