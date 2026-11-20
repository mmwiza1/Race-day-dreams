package org.racedaydreams.update

import android.os.StatFs
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AvailableUpdate
import org.racedaydreams.storage.AppUpdateManifestJson

/** Why a check or download did not complete. Every case leaves the installed app unchanged. */
sealed class UpdateFailure(val message: String) {
    object Offline :
        UpdateFailure("Couldn't reach GitHub. Check your connection and try again.")

    object NoRelease :
        UpdateFailure("No update-ready release has been published yet. You're on the latest version available.")

    class Server(code: Int) :
        UpdateFailure("GitHub didn't respond as expected (error $code). Try again later.")

    class BadManifest(reason: String) :
        UpdateFailure("The update information was not accepted: $reason")

    object Storage :
        UpdateFailure("Not enough free storage to download the update. Free some space and try again.")

    object Interrupted :
        UpdateFailure("The download was interrupted. Nothing was installed; you can try again.")

    object Cancelled : UpdateFailure("Download cancelled. Nothing was installed.")

    class Rejected(reason: String) : UpdateFailure(reason)
}

class UpdateException(val failure: UpdateFailure) : Exception(failure.message)

/**
 * The only network access in the app: two HTTPS GET requests to the official GitHub release, one
 * for the small manifest and, only when the person chooses to download, one for the APK. No
 * account, identifier or personal data is sent; there is no background polling.
 */
open class AppUpdateClient(private val manifestUrl: String = AppUpdates.MANIFEST_URL) {
    open fun fetchManifest(packageName: String, userAgent: String): AvailableUpdate {
        val connection = connect(manifestUrl, userAgent)
        try {
            when (val code = connection.responseCode) {
                200 -> Unit
                404 -> throw UpdateException(UpdateFailure.NoRelease)
                else -> throw UpdateException(UpdateFailure.Server(code))
            }
            val text = connection.inputStream.use { readBounded(it, AppUpdateManifestJson.MAX_CHARS) }
            return try {
                AppUpdateManifestJson.decode(text, packageName)
            } catch (invalid: IllegalArgumentException) {
                throw UpdateException(UpdateFailure.BadManifest(invalid.message ?: "invalid format"))
            }
        } catch (failure: UpdateException) {
            throw failure
        } catch (_: IOException) {
            throw UpdateException(UpdateFailure.Offline)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Streams the APK to [target] (via a `.part` file), hashing as it goes. Never writes more than
     * the announced size; a short, long or damaged file is deleted.
     */
    open fun download(
        update: AvailableUpdate,
        target: File,
        userAgent: String,
        cancelled: () -> Boolean,
        progress: (Long, Long) -> Unit,
    ): String {
        val directory = target.parentFile ?: throw UpdateException(UpdateFailure.Storage)
        directory.mkdirs()
        val part = File(directory, target.name + ".part")
        part.delete()
        target.delete()
        if (freeBytes(directory) < update.apkBytes + RESERVE_BYTES)
            throw UpdateException(UpdateFailure.Storage)
        val connection = connect(update.apkUrl, userAgent)
        try {
            val code = connection.responseCode
            if (code != 200) throw UpdateException(UpdateFailure.Server(code))
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        if (cancelled()) throw UpdateException(UpdateFailure.Cancelled)
                        val read = input.read(buffer)
                        if (read < 0) break
                        written += read
                        if (written > update.apkBytes)
                            throw UpdateException(
                                UpdateFailure.Rejected("The download is larger than announced and was rejected.")
                            )
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                        progress(written, update.apkBytes)
                    }
                    output.fd.sync()
                }
            }
            if (written != update.apkBytes) throw UpdateException(UpdateFailure.Interrupted)
            val sha = ApkVerifier.hex(digest.digest())
            if (sha != update.apkSha256)
                throw UpdateException(
                    UpdateFailure.Rejected(
                        "The download is damaged or does not match the published release. It was deleted; try again."
                    )
                )
            if (!part.renameTo(target)) throw UpdateException(UpdateFailure.Storage)
            return sha
        } catch (failure: UpdateException) {
            part.delete()
            throw failure
        } catch (io: IOException) {
            part.delete()
            val noSpace = io.message?.contains("ENOSPC") == true
            throw UpdateException(if (noSpace) UpdateFailure.Storage else UpdateFailure.Interrupted)
        } finally {
            connection.disconnect()
        }
    }

    protected open fun freeBytes(directory: File): Long = StatFs(directory.path).availableBytes

    protected open fun open(url: URL): HttpURLConnection = url.openConnection() as HttpURLConnection

    private fun connect(url: String, userAgent: String): HttpURLConnection {
        // HTTPS only; Android also blocks cleartext for this app. HttpURLConnection never follows
        // a redirect from HTTPS to HTTP, so GitHub's download redirect stays encrypted.
        if (!url.startsWith("https://")) throw UpdateException(UpdateFailure.Offline)
        val connection =
            try {
                open(URL(url))
            } catch (_: IOException) {
                throw UpdateException(UpdateFailure.Offline)
            }
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", userAgent)
        connection.setRequestProperty("Accept", "application/octet-stream, application/json")
        return connection
    }

    companion object {
        /** Keep room for Android to install the update after the download. */
        const val RESERVE_BYTES = 50L * 1024 * 1024

        internal fun readBounded(input: InputStream, maxChars: Int): String {
            val bytes = input.readNBytesCompat(maxChars * 4 + 1)
            val text = bytes.toString(Charsets.UTF_8)
            if (text.length > maxChars) throw UpdateException(UpdateFailure.BadManifest("too large"))
            return text
        }

        private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (out.size() < limit) {
                val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
                if (read < 0) break
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
    }
}
