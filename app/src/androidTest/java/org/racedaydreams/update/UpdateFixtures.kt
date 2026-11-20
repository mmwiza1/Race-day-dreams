package org.racedaydreams.update

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AvailableUpdate

/**
 * Real-app update fixtures with no network access. The "published" APK is this test build's own
 * installed APK, so size, SHA-256, package, version and signing checks run on a genuine signed APK.
 */
object UpdateFixtures {
    fun ownApk(context: Context) = File(context.applicationInfo.sourceDir)

    /** This installation as it would look one version earlier, signed with the release key. */
    fun olderReleaseInstall(context: Context): InstalledApp {
        val real = InstalledApp.read(context)
        val cert = requireNotNull(real.certificateSha256)
        return real.copy(
            versionName = "0.13.1-test1",
            versionCode = real.versionCode - 1,
            releaseCertificateSha256 = cert,
        )
    }

    /** The current build described exactly as CI would announce it. */
    fun announcedCurrentBuild(context: Context, certificate: String? = null): AvailableUpdate {
        val real = InstalledApp.read(context)
        val apk = ownApk(context)
        val name = "Race-Day-Dreams-${real.versionName}.apk"
        return AvailableUpdate(
            versionName = real.versionName,
            versionCode = real.versionCode,
            minSdk = 28,
            apkName = name,
            apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + name,
            apkBytes = apk.length(),
            apkSha256 = ApkVerifier.sha256(apk),
            certificateSha256 = certificate ?: requireNotNull(real.certificateSha256),
            commit = "44987a3bd9e0a03686a5caad64524ed7832de908",
            notes =
                "• Check for, download and install updates from Settings › App updates\n" +
                    "• Every update is checked against this installation's signing key before Android asks you to approve it\n" +
                    "• Your runs, plans, gym and bouldering history stay on this phone",
        )
    }
}

/** Serves [update] and the local APK; can pause, truncate, corrupt or fail on request. */
class FakeUpdateClient(
    private val context: Context,
    var update: AvailableUpdate?,
    var manifestFailure: UpdateFailure? = null,
) : AppUpdateClient(manifestUrl = AppUpdates.MANIFEST_URL) {
    val manifestRequests = AtomicInteger()
    var pauseAtBytes: Long? = null
    val paused = CountDownLatch(1)
    val resume = CountDownLatch(1)
    var truncateBy = 0L
    var corrupt = false
    var downloadFailure: IOException? = null
    var freeSpace = Long.MAX_VALUE

    override fun fetchManifest(packageName: String, userAgent: String): AvailableUpdate {
        manifestRequests.incrementAndGet()
        manifestFailure?.let { throw UpdateException(it) }
        return update ?: throw UpdateException(UpdateFailure.NoRelease)
    }

    override fun freeBytes(directory: File): Long = freeSpace

    // download() is not overridden: the real streaming, size and checksum logic runs against
    // the local APK served by open().

    override fun open(url: java.net.URL): java.net.HttpURLConnection {
        val bytes = UpdateFixtures.ownApk(context).readBytes()
        val served = bytes.copyOf((bytes.size - truncateBy).toInt())
        if (corrupt) served[served.size / 2] = (served[served.size / 2] + 1).toByte()
        return FakeConnection(url, 200, served, downloadFailure, pauseAtBytes, paused, resume)
    }
}

/** A minimal HTTPS response: a status, a body, an optional failure part-way, an optional pause. */
class FakeConnection(
    url: java.net.URL,
    private val status: Int,
    private val body: ByteArray,
    private val failure: IOException? = null,
    private val pauseAt: Long? = null,
    private val paused: CountDownLatch? = null,
    private val resume: CountDownLatch? = null,
) : java.net.HttpURLConnection(url) {
    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun usingProxy() = false

    override fun getResponseCode() = status

    override fun getInputStream(): java.io.InputStream =
        object : java.io.InputStream() {
            private var position = 0

            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (pauseAt != null && position >= pauseAt && paused?.count == 1L) {
                    paused.countDown()
                    resume?.await(60, TimeUnit.SECONDS)
                }
                if (failure != null && position >= body.size / 3) throw failure
                if (position >= body.size) return -1
                val count = minOf(length, body.size - position, 1 shl 16)
                System.arraycopy(body, position, buffer, offset, count)
                position += count
                return count
            }
        }
}
