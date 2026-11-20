package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.update.*
import org.racedaydreams.domain.update.AppUpdates.Decision
import org.racedaydreams.domain.update.AppUpdates.Incompatibility

class AppUpdatesTest {
    private val cert = "a".repeat(64)
    private val otherCert = "b".repeat(64)
    private val now = 1_800_000_000L

    private fun update(
        code: Long = 23,
        name: String = "0.15.0-test1",
        minSdk: Int = 28,
        certificate: String = cert,
    ) =
        AvailableUpdate(
            versionName = name,
            versionCode = code,
            minSdk = minSdk,
            apkName = "Race-Day-Dreams-$name.apk",
            apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-$name.apk",
            apkBytes = 59_000_000,
            apkSha256 = "c".repeat(64),
            certificateSha256 = certificate,
            commit = "d".repeat(40),
            notes = "• Bouldering\n• Updates",
        )

    private fun evaluate(
        update: AvailableUpdate,
        installed: Long = 22,
        installedCert: String = cert,
        sdk: Int = 35,
        preferences: UpdatePreferences = UpdatePreferences(),
    ) = AppUpdates.evaluate(installed, installedCert, sdk, update, preferences, now)

    @Test
    fun onlyAHigherVersionCodeIsAnUpdateWhateverTheName() {
        assertEquals(Decision.UpToDate, evaluate(update(code = 22)))
        assertEquals(Decision.UpToDate, evaluate(update(code = 21, name = "9.9.9-test1")))
        val newer = evaluate(update(code = 23, name = "0.0.1-test1"))
        assertTrue(newer is Decision.Available && newer.prompt)
    }

    @Test
    fun aDifferentSigningCertificateIsNeverOfferedAsAnInPlaceUpdate() {
        assertEquals(
            Decision.Incompatible(update(), Incompatibility.SIGNING),
            evaluate(update(), installedCert = otherCert),
        )
        // Certificate digests from Android may be upper case; comparison is exact on the bytes.
        assertTrue(evaluate(update(), installedCert = cert.uppercase()) is Decision.Available)
    }

    @Test
    fun anUpdateForANewerAndroidVersionIsExplainedNotOffered() {
        assertEquals(
            Decision.Incompatible(update(minSdk = 36), Incompatibility.ANDROID_VERSION),
            evaluate(update(minSdk = 36), sdk = 35),
        )
    }

    @Test
    fun skippingAVersionStopsPromptsForThatVersionOnly() {
        val skipped = UpdatePreferences().skip(23)
        assertEquals(
            Decision.Available(update(), prompt = false),
            evaluate(update(), preferences = skipped),
        )
        assertEquals(
            Decision.Available(update(code = 24, name = "0.15.1-test1"), prompt = true),
            evaluate(update(code = 24, name = "0.15.1-test1"), preferences = skipped),
        )
    }

    @Test
    fun remindMeLaterLastsThreeDaysForThatVersion() {
        val later = UpdatePreferences().snooze(23, now)
        assertFalse((evaluate(update(), preferences = later) as Decision.Available).prompt)
        val expired =
            AppUpdates.evaluate(22, cert, 35, update(), later, now + AppUpdates.SNOOZE_SECONDS)
        assertTrue((expired as Decision.Available).prompt)
        val newer = evaluate(update(code = 24, name = "0.15.1-test1"), preferences = later)
        assertTrue((newer as Decision.Available).prompt)
    }

    @Test
    fun automaticChecksAreDailyAtMostAndNeverDuringActiveWork() {
        val never = UpdatePreferences()
        assertTrue(AppUpdates.shouldCheckAutomatically(never, now, activeWork = false))
        assertFalse(AppUpdates.shouldCheckAutomatically(never, now, activeWork = true))
        val recent = never.checked(now - 3600)
        assertFalse(AppUpdates.shouldCheckAutomatically(recent, now, activeWork = false))
        val dayOld = never.checked(now - AppUpdates.AUTOMATIC_INTERVAL_SECONDS)
        assertTrue(AppUpdates.shouldCheckAutomatically(dayOld, now, activeWork = false))
        val clockMovedBack = never.checked(now + 3600)
        assertTrue(AppUpdates.shouldCheckAutomatically(clockMovedBack, now, activeWork = false))
        val off = never.copy(automaticChecks = false)
        assertFalse(AppUpdates.shouldCheckAutomatically(off, now, activeWork = false))
    }

    @Test
    fun aVersionIsNotifiedOnceAndOnlyWhenEnabledAndNotDismissed() {
        val available = evaluate(update())
        assertTrue(AppUpdates.shouldNotify(UpdatePreferences(), available))
        assertFalse(AppUpdates.shouldNotify(UpdatePreferences().notified(23), available))
        assertFalse(AppUpdates.shouldNotify(UpdatePreferences(notifications = false), available))
        val skipped = UpdatePreferences().skip(23)
        assertFalse(AppUpdates.shouldNotify(skipped, evaluate(update(), preferences = skipped)))
        assertFalse(AppUpdates.shouldNotify(UpdatePreferences(), evaluate(update(code = 22))))
        assertFalse(
            AppUpdates.shouldNotify(
                UpdatePreferences(),
                evaluate(update(), installedCert = otherCert),
            )
        )
    }

    @Test
    fun onlyVersionedApksFromTheOfficialReleaseAreAccepted() {
        fun rejects(block: () -> Unit) = assertFailsWith<IllegalArgumentException> { block() }
        val good = update()
        rejects { good.copy(apkUrl = "https://example.com/Race-Day-Dreams-0.15.0-test1.apk") }
        rejects {
            good.copy(
                apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-latest-test.apk"
            )
        }
        rejects { good.copy(apkName = "Race-Day-Dreams-latest-test.apk") }
        rejects { good.copy(apkUrl = good.apkUrl.replace("https://", "http://")) }
        rejects { good.copy(apkSha256 = "C".repeat(64)) }
        rejects { good.copy(certificateSha256 = "short") }
        rejects { good.copy(apkBytes = 10) }
        rejects { good.copy(versionCode = 0) }
        rejects { good.copy(commit = "main") }
        rejects { good.copy(notes = "x".repeat(AppUpdates.MAX_NOTES_CHARS + 1)) }
        rejects { good.copy(versionName = "latest") }
    }
}
