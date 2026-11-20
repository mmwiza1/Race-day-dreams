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
            versionName = neighbourVersion(real.versionName, -1),
            versionCode = real.versionCode - 1,
            releaseCertificateSha256 = cert,
        )
    }

    /**
     * The release name beside [name] ("0.15.0-test1", -1 -> "0.14.0-test1"). Each release so far
     * raised the minor version and the versionCode by one, so labels match the real history.
     */
    fun neighbourVersion(name: String, minorDelta: Int): String {
        val parts = requireNotNull(Regex("(\\d+)\\.(\\d+)\\.\\d+(-test\\d+)?").matchEntire(name)).groupValues
        return "${parts[1]}.${parts[2].toInt() + minorDelta}.0-test1"
    }

    /** A newer release (next version and versionCode) signed with [certificate]. */
    fun nextRelease(context: Context, certificate: String): AvailableUpdate {
        val current = announcedCurrentBuild(context)
        val name = neighbourVersion(current.versionName, 1)
        return current.copy(
            versionName = name,
            versionCode = current.versionCode + 1,
            apkName = "Race-Day-Dreams-$name.apk",
            apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-$name.apk",
            certificateSha256 = certificate,
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
        // Streamed from the installed APK file, never held in memory (it is ~60 MB).
        val apk = UpdateFixtures.ownApk(context)
        val length = apk.length() - truncateBy
        return FakeConnection(
            url,
            200,
            source = { apk.inputStream() },
            length = length,
            corruptAt = if (corrupt) length / 2 else null,
            failure = downloadFailure,
            pauseAt = pauseAtBytes,
            paused = paused,
            resume = resume,
        )
    }
}

/** A minimal HTTPS response: a status, a streamed body, an optional failure part-way, a pause. */
class FakeConnection(
    url: java.net.URL,
    private val status: Int,
    private val source: () -> java.io.InputStream,
    private val length: Long,
    private val corruptAt: Long? = null,
    private val failure: IOException? = null,
    private val pauseAt: Long? = null,
    private val paused: CountDownLatch? = null,
    private val resume: CountDownLatch? = null,
) : java.net.HttpURLConnection(url) {
    constructor(
        url: java.net.URL,
        status: Int,
        body: ByteArray,
    ) : this(url, status, { body.inputStream() }, body.size.toLong())

    override fun connect() = Unit

    override fun disconnect() = Unit

    override fun usingProxy() = false

    override fun getResponseCode() = status

    override fun getInputStream(): java.io.InputStream =
        object : java.io.InputStream() {
            private val input = source()
            private var position = 0L

            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (pauseAt != null && position >= pauseAt && paused?.count == 1L) {
                    paused.countDown()
                    resume?.await(60, TimeUnit.SECONDS)
                }
                if (failure != null && position >= this@FakeConnection.length / 3) throw failure
                val remaining = this@FakeConnection.length - position
                if (remaining <= 0) return -1
                val count = input.read(buffer, offset, minOf(length.toLong(), remaining, 1L shl 16).toInt())
                if (count < 0) return -1
                if (corruptAt != null && corruptAt in position until position + count) {
                    val index = offset + (corruptAt - position).toInt()
                    buffer[index] = (buffer[index] + 1).toByte()
                }
                position += count
                return count
            }

            override fun close() = input.close()
        }
}
