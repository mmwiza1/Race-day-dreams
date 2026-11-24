package org.racedaydreams.update

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AvailableUpdate
import org.racedaydreams.storage.AppUpdateManifestJson

/** Real APK verification, download and update rules on the device, with no network access. */
@RunWith(AndroidJUnit4::class)
class AppUpdateTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(context.cacheDir, "update-test")

    @Before
    fun clean() {
        directory.deleteRecursively()
        File(context.cacheDir, "updates").deleteRecursively()
        clearPreferences()
    }

    @After fun cleanup() = clean()

    private fun clearPreferences() =
        context.getSharedPreferences(UpdatePreferencesStore.NAME, 0).edit().clear().commit()

    private fun copyOfOwnApk(name: String = "candidate.apk"): File {
        directory.mkdirs()
        return UpdateFixtures.ownApk(context).copyTo(File(directory, name), overwrite = true)
    }

    private fun waitFor(timeoutMs: Long = 20_000, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > end) fail("Timed out waiting for the update state")
            Thread.sleep(25)
        }
    }

    // ---- APK verification -------------------------------------------------------------------

    @Test
    fun aNewerApkSignedLikeThisInstallationIsAcceptedAndEverythingElseRejected() {
        val older = UpdateFixtures.olderReleaseInstall(context)
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val apk = copyOfOwnApk()
        assertEquals(ApkVerifier.Result.Verified, ApkVerifier.verify(context, apk, update, older))

        fun rejected(result: ApkVerifier.Result, words: String) {
            assertTrue("$result", result is ApkVerifier.Result.Rejected)
            assertTrue((result as ApkVerifier.Result.Rejected).reason, result.reason.contains(words))
        }
        // Not newer than the installed version.
        rejected(
            ApkVerifier.verify(context, apk, update, older.copy(versionCode = update.versionCode)),
            "not newer",
        )
        // Signed with a different key from this installation: never handed to Android.
        rejected(
            ApkVerifier.verify(context, apk, update, older.copy(certificateSha256 = "0".repeat(64))),
            "different key",
        )
        // Correctly signed, but not with the project's release certificate.
        rejected(
            ApkVerifier.verify(context, apk, update, older.copy(releaseCertificateSha256 = "")),
            "not signed as expected",
        )
        // Announced with another certificate or version than the file actually has.
        rejected(
            ApkVerifier.verify(context, apk, update.copy(certificateSha256 = "1".repeat(64)), older),
            "not signed as expected",
        )
        rejected(
            ApkVerifier.verify(context, apk, update.copy(versionCode = update.versionCode + 1), older),
            "announced version",
        )
        // Damaged, truncated or missing files.
        rejected(ApkVerifier.verify(context, apk, update.copy(apkSha256 = "2".repeat(64)), older), "damaged")
        val truncated = File(directory, "short.apk")
        truncated.writeBytes(apk.readBytes().copyOf(apk.length().toInt() - 1000))
        rejected(ApkVerifier.verify(context, truncated, update, older), "incomplete")
        rejected(ApkVerifier.verify(context, File(directory, "missing.apk"), update, older), "incomplete")
        // Same size and checksum claims for a file that is not an Android app at all.
        val junk = File(directory, "junk.apk").apply { writeBytes(ByteArray(apk.length().toInt()) { 7 }) }
        rejected(
            ApkVerifier.verify(context, junk, update.copy(apkSha256 = ApkVerifier.sha256(junk)), older),
            "not a valid Android app",
        )
    }

    @Test
    fun theInstalledIdentityComesFromAndroidAndTestBuildsAreNotUpdateCapable() {
        val installed = InstalledApp.read(context)
        assertEquals(AppUpdates.PACKAGE_NAME, installed.packageName)
        assertTrue(installed.versionCode > 0)
        assertTrue(AppUpdates.validSha256(requireNotNull(installed.certificateSha256)))
        // CI and local test builds use a temporary key that never matches a release fingerprint.
        assertFalse(installed.updateCapable)
    }

    // ---- Network layer: real client logic over scripted HTTP responses ------------------------

    private class Scripted(
        private val status: Int = 200,
        private val body: ByteArray = ByteArray(0),
        private val offline: Boolean = false,
    ) : AppUpdateClient() {
        override fun open(url: URL): HttpURLConnection {
            if (offline) throw IOException("No route to host")
            assertTrue(url.toString().startsWith("https://github.com/mmwiza1/Race-day-dreams/"))
            return FakeConnection(url, status, body)
        }
    }

    private fun manifestFailure(client: AppUpdateClient): UpdateFailure =
        try {
            client.fetchManifest(context.packageName, "test")
            fail("Expected the check to fail")
            throw AssertionError()
        } catch (failure: UpdateException) {
            failure.failure
        }

    @Test
    fun checkingHandlesOfflineMissingReleaseServerErrorsAndBadManifests() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val good = AppUpdateManifestJson.encode(update, context.packageName).toByteArray()
        assertEquals(update, Scripted(body = good).fetchManifest(context.packageName, "test"))
        assertSame(UpdateFailure.Offline, manifestFailure(Scripted(offline = true)))
        assertSame(UpdateFailure.NoRelease, manifestFailure(Scripted(status = 404)))
        assertTrue(manifestFailure(Scripted(status = 503)) is UpdateFailure.Server)
        assertTrue(manifestFailure(Scripted(body = "<html>".toByteArray())) is UpdateFailure.BadManifest)
        val other = AppUpdateManifestJson.encode(update, "com.example.other").toByteArray()
        assertTrue(manifestFailure(Scripted(body = other)) is UpdateFailure.BadManifest)
        val huge = ByteArray(AppUpdateManifestJson.MAX_CHARS * 5) { ' '.code.toByte() }
        assertTrue(manifestFailure(Scripted(body = huge)) is UpdateFailure.BadManifest)
    }

    private fun downloadFailure(client: FakeUpdateClient, update: AvailableUpdate, target: File): UpdateFailure =
        try {
            client.download(update, target, "test", { false }) { _, _ -> }
            fail("Expected the download to fail")
            throw AssertionError()
        } catch (failure: UpdateException) {
            failure.failure
        }

    @Test
    fun downloadsAreSizeAndChecksumVerifiedAndFailuresLeaveNothingBehind() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val target = File(directory, update.apkName)
        val client = FakeUpdateClient(context, update)
        var lastProgress = 0L
        assertEquals(update.apkSha256, client.download(update, target, "test", { false }) { b, _ -> lastProgress = b })
        assertEquals(update.apkBytes, target.length())
        assertEquals(update.apkBytes, lastProgress)

        fun leavesNothing() = assertTrue(directory.listFiles().orEmpty().none { it.name.startsWith(update.apkName) })
        assertSame(UpdateFailure.Interrupted, downloadFailure(FakeUpdateClient(context, update).apply { truncateBy = 4096 }, update, target))
        leavesNothing()
        assertTrue(downloadFailure(FakeUpdateClient(context, update).apply { corrupt = true }, update, target) is UpdateFailure.Rejected)
        leavesNothing()
        assertSame(
            UpdateFailure.Interrupted,
            downloadFailure(FakeUpdateClient(context, update).apply { downloadFailure = IOException("reset") }, update, target),
        )
        leavesNothing()
        assertSame(UpdateFailure.Storage, downloadFailure(FakeUpdateClient(context, update).apply { freeSpace = 10 }, update, target))
        leavesNothing()
        val larger = update.copy(apkBytes = update.apkBytes - 1)
        assertTrue(downloadFailure(FakeUpdateClient(context, update), larger, target) is UpdateFailure.Rejected)
        leavesNothing()
        val cancelled =
            try {
                FakeUpdateClient(context, update).download(update, target, "test", { true }) { _, _ -> }
                null
            } catch (failure: UpdateException) {
                failure.failure
            }
        assertSame(UpdateFailure.Cancelled, cancelled)
        leavesNothing()
    }

    // ---- Coordinator: user choices and safety around active workouts --------------------------

    private fun coordinator(
        client: FakeUpdateClient,
        activeWork: String? = null,
        installed: InstalledApp = UpdateFixtures.olderReleaseInstall(context),
    ) =
        AppUpdateCoordinator(
            context,
            client,
            nowEpochSecond = { 1_800_000_000L },
            installedApp = { installed },
            activeWork = { activeWork },
        )

    @Test
    fun anActiveRunWorkoutOrBoulderingSessionIsNeverInterrupted() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val model = coordinator(FakeUpdateClient(context, update), activeWork = "A gym workout is in progress.")
        model.check()
        waitFor { model.state.value.status is UpdateStatus.Available }
        model.download()
        waitFor { model.state.value.status is UpdateStatus.Failed }
        val failed = model.state.value.status as UpdateStatus.Failed
        assertTrue(failed.message.contains("A gym workout is in progress."))
        assertEquals(UpdateStatus.Retry.DOWNLOAD, failed.retry)
        assertFalse(File(context.cacheDir, "updates/${update.apkName}").exists())
        // Automatic checks never run during active work either (fresh preferences: due today).
        clearPreferences()
        val quiet = FakeUpdateClient(context, update)
        coordinator(quiet, activeWork = "A run is being recorded or needs recovery.").onAppStarted()
        Thread.sleep(500)
        assertEquals(0, quiet.manifestRequests.get())
    }

    @Test
    fun downloadVerifyCancelledInstallAndRetryKeepTheVerifiedFile() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val client = FakeUpdateClient(context, update).apply { pauseAtBytes = update.apkBytes / 3 }
        val model = coordinator(client)
        model.check()
        waitFor { model.state.value.status is UpdateStatus.Available }
        model.download()
        assertTrue(client.paused.await(20, java.util.concurrent.TimeUnit.SECONDS))
        assertTrue(model.state.value.status is UpdateStatus.Downloading)
        client.resume.countDown()
        waitFor { model.state.value.status is UpdateStatus.Ready }
        // Android reported the person cancelled its approval screen: nothing changed, retry is safe.
        model.installResult(UpdateInstaller.Outcome.Cancelled)
        val ready = model.state.value.status as UpdateStatus.Ready
        assertTrue(ready.note!!.contains("unchanged"))
        assertTrue(File(context.cacheDir, "updates/${update.apkName}").isFile)
        // A signing conflict reported by Android discards the file so it is never retried as-is.
        model.installResult(UpdateInstaller.outcome(android.content.pm.PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, null))
        val failed = model.state.value.status as UpdateStatus.Failed
        assertEquals(UpdateStatus.Retry.DOWNLOAD, failed.retry)
        assertFalse(File(context.cacheDir, "updates/${update.apkName}").exists())
    }

    @Test
    fun cancellingADownloadKeepsTheUpdateAvailableWithoutAFile() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val client = FakeUpdateClient(context, update).apply { pauseAtBytes = update.apkBytes / 2 }
        val model = coordinator(client)
        model.check()
        waitFor { model.state.value.status is UpdateStatus.Available }
        model.download()
        assertTrue(client.paused.await(20, java.util.concurrent.TimeUnit.SECONDS))
        model.cancelDownload()
        client.resume.countDown()
        waitFor { model.state.value.status is UpdateStatus.Available }
        assertTrue(File(context.cacheDir, "updates").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun skipAndRemindMeLaterAreRememberedForThatVersion() {
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val model = coordinator(FakeUpdateClient(context, update))
        model.check()
        waitFor { (model.state.value.status as? UpdateStatus.Available)?.prompt == true }
        model.skip()
        assertEquals(UpdateStatus.Available(update, prompt = false), model.state.value.status)
        // A new process reads the stored choice: the skipped version is still listed, not prompted.
        val again = coordinator(FakeUpdateClient(context, update))
        again.check()
        waitFor { again.state.value.status is UpdateStatus.Available }
        assertFalse((again.state.value.status as UpdateStatus.Available).prompt)
        assertEquals(update.versionCode, UpdatePreferencesStore(context).read().skippedVersionCode)
    }

    @Test
    fun aDifferentlySignedReleaseIsExplainedAndAutomaticChecksOnlyRunForReleaseBuilds() {
        val update = UpdateFixtures.announcedCurrentBuild(context, certificate = "3".repeat(64))
        val model = coordinator(FakeUpdateClient(context, update))
        model.check()
        waitFor { model.state.value.status is UpdateStatus.Incompatible }
        assertEquals(
            AppUpdates.Incompatibility.SIGNING,
            (model.state.value.status as UpdateStatus.Incompatible).reason,
        )
        // A test-signed build never contacts the network on its own.
        val testBuild = FakeUpdateClient(context, update)
        coordinator(testBuild, installed = InstalledApp.read(context)).onAppStarted()
        Thread.sleep(500)
        assertEquals(0, testBuild.manifestRequests.get())
        // A release-signed build checks once, then not again the same day.
        clearPreferences()
        val release = FakeUpdateClient(context, update)
        coordinator(release).onAppStarted()
        waitFor { release.manifestRequests.get() == 1 }
        coordinator(release).onAppStarted()
        Thread.sleep(500)
        assertEquals(1, release.manifestRequests.get())
        // Offline automatic checks stay quiet; a manual check explains.
        val offline = FakeUpdateClient(context, update, manifestFailure = UpdateFailure.Offline)
        val manual = coordinator(offline)
        manual.check()
        waitFor { manual.state.value.status is UpdateStatus.Failed }
        assertEquals(UpdateStatus.Retry.CHECK, (manual.state.value.status as UpdateStatus.Failed).retry)
    }

    @Test
    fun notificationPreferencesPersistAndAreDeviceLocal() {
        val store = UpdatePreferencesStore(context)
        assertTrue(store.read().automaticChecks)
        assertTrue(store.read().notifications)
        val model = coordinator(FakeUpdateClient(context, null))
        model.setNotifications(false)
        model.setAutomaticChecks(false)
        assertFalse(store.read().notifications)
        assertFalse(store.read().automaticChecks)
        // Turning automatic checks off stops even a release build from checking.
        val client = FakeUpdateClient(context, UpdateFixtures.announcedCurrentBuild(context))
        coordinator(client).onAppStarted()
        Thread.sleep(500)
        assertEquals(0, client.manifestRequests.get())
    }

    @Test
    fun onlyTheVerifiedBytesReachTheInstallSession() {
        // Audit R-7: the session copy is hashed as it is written, so a file that changed after
        // verification is never committed.
        val bytes = ByteArray(200_000) { (it % 251).toByte() }
        val sha = ApkVerifier.hex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
        val copied = java.io.ByteArrayOutputStream()
        UpdateInstaller.copyVerified(bytes.inputStream(), copied, sha, bytes.size.toLong())
        assertArrayEquals(bytes, copied.toByteArray())

        val changed = bytes.copyOf().also { it[123_456] = (it[123_456] + 1).toByte() }
        fun refused(input: ByteArray, size: Long = bytes.size.toLong()) =
            try {
                UpdateInstaller.copyVerified(input.inputStream(), java.io.ByteArrayOutputStream(), sha, size)
                false
            } catch (_: UpdateInstaller.ChangedAfterVerification) {
                true
            }
        assertTrue(refused(changed))
        assertTrue(refused(bytes.copyOf(bytes.size - 1)))
        assertTrue(refused(bytes + byteArrayOf(0)))
        assertTrue(refused(bytes, size = bytes.size - 1L))
    }

    @Test
    fun aVerifiedUpdateIsHandedToAndroidsOwnApprovalScreen() {
        // The test never changes "Install unknown apps": on Android 12+ revoking that access kills
        // the app process (and with it the test run), as a real person revoking it would.
        val update = UpdateFixtures.announcedCurrentBuild(context)
        val model = coordinator(FakeUpdateClient(context, update))
        AppUpdateCoordinator.replaceForTesting(model)
        try {
            model.check()
            waitFor { model.state.value.status is UpdateStatus.Available }
            model.download()
            waitFor(60_000) { model.state.value.status is UpdateStatus.Ready }
            val apk = File(context.cacheDir, "updates/${update.apkName}")
            if (!UpdateInstaller.canInstall(context)) {
                // Not allowed (the default): Install asks for Android's setting and installs nothing.
                model.install()
                waitFor(60_000) { model.state.value.status is UpdateStatus.NeedsInstallPermission }
                assertEquals(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    UpdateInstaller.installPermissionIntent(context).action,
                )
                // Returning without allowing it keeps the verified file and explains the choice.
                model.installPermissionReturned()
                val kept = model.state.value.status as UpdateStatus.Ready
                assertTrue(kept.note!!.contains("permission"))
                assertTrue(apk.isFile)
            }
            // The verified file goes into a real PackageInstaller session; Android answers with its
            // own approval screen through the unexported receiver. It is never approved here.
            UpdateInstaller.commit(context, apk, update.apkSha256)
            waitFor(90_000) {
                model.state.value.status.let {
                    it is UpdateStatus.AwaitingApproval || it is UpdateStatus.Failed
                }
            }
            val awaiting = model.state.value.status
            assertTrue("$awaiting", awaiting is UpdateStatus.AwaitingApproval)
            val confirmation = (awaiting as UpdateStatus.AwaitingApproval).confirmation
            assertTrue(
                "${confirmation.action}",
                confirmation.action.orEmpty().startsWith("android.content.pm.action.CONFIRM"),
            )
            model.approvalShown()
            assertTrue(model.state.value.status is UpdateStatus.Installing)
            // Android's answer arrives through the unexported receiver; a cancel keeps the file.
            UpdateInstallReceiver()
                .onReceive(
                    context,
                    android.content.Intent(UpdateInstaller.ACTION_STATUS)
                        .putExtra(
                            android.content.pm.PackageInstaller.EXTRA_STATUS,
                            android.content.pm.PackageInstaller.STATUS_FAILURE_ABORTED,
                        ),
                )
            assertTrue(model.state.value.status is UpdateStatus.Ready)
            assertTrue(apk.isFile)
        } finally {
            AppUpdateCoordinator.replaceForTesting(null)
        }
    }

    @Test
    fun androidInstallerOutcomesExplainWhatHappenedToTheInstalledApp() {
        val pi = android.content.pm.PackageInstaller::class.java
        fun status(name: String) = pi.getField(name).getInt(null)
        assertSame(UpdateInstaller.Outcome.Cancelled, UpdateInstaller.outcome(status("STATUS_FAILURE_ABORTED"), null))
        assertSame(UpdateInstaller.Outcome.Installed, UpdateInstaller.outcome(status("STATUS_SUCCESS"), null))
        val conflict = UpdateInstaller.outcome(status("STATUS_FAILURE_CONFLICT"), null) as UpdateInstaller.Outcome.Failed
        assertFalse(conflict.retryWithSameFile)
        assertTrue(conflict.message.contains("unchanged"))
        val storage = UpdateInstaller.outcome(status("STATUS_FAILURE_STORAGE"), null) as UpdateInstaller.Outcome.Failed
        assertTrue(storage.retryWithSameFile)
        // Approval without Android's own screen is impossible; it is reported, never bypassed.
        assertTrue(UpdateInstaller.outcome(status("STATUS_PENDING_USER_ACTION"), null) is UpdateInstaller.Outcome.Failed)
        val approval = android.content.Intent("android.content.pm.action.CONFIRM_INSTALL")
        assertEquals(
            UpdateInstaller.Outcome.NeedsApproval(approval),
            UpdateInstaller.outcome(status("STATUS_PENDING_USER_ACTION"), approval),
        )
    }
}
