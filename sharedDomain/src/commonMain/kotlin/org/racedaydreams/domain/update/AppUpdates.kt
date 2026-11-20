package org.racedaydreams.domain.update

/**
 * Rules for Android in-place updates from the official Race Day Dreams GitHub release.
 *
 * Pure logic with no platform or network types, so it is shared, deterministic and testable. The
 * Android host fetches the manifest, downloads and verifies the APK and asks Android to install it;
 * nothing here performs network access, and iOS never uses it.
 */
object AppUpdates {
    /** The only place an update manifest or APK may come from. */
    const val RELEASE_DOWNLOAD_PREFIX =
        "https://github.com/mmwiza1/Race-day-dreams/releases/download/development/"

    /** Rolling manifest for the latest verified, consistently signed development build. */
    const val MANIFEST_URL = RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-latest-update.json"

    const val PACKAGE_NAME = "org.racedaydreams"

    /** At most one automatic check per day, and only when the app is opened. */
    const val AUTOMATIC_INTERVAL_SECONDS = 24L * 3600

    /** "Remind me later" hides the prompt for this long; Settings still shows the update. */
    const val SNOOZE_SECONDS = 3L * 24 * 3600

    const val MIN_APK_BYTES = 1_000_000L
    const val MAX_APK_BYTES = 300_000_000L
    const val MAX_NOTES_CHARS = 4_000

    private val HEX_64 = Regex("[0-9a-f]{64}")
    private val COMMIT = Regex("[0-9a-f]{40}")
    private val VERSION_NAME = Regex("\\d+\\.\\d+\\.\\d+(-test\\d+)?")
    private val APK_NAME = Regex("Race-Day-Dreams-\\d+\\.\\d+\\.\\d+(-test\\d+)?\\.apk")

    /** Why an available build cannot be installed over this installation. */
    enum class Incompatibility {
        /** Signed with a different certificate; Android would refuse an in-place update. */
        SIGNING,
        /** Requires a newer Android version than this device runs. */
        ANDROID_VERSION,
    }

    /** Result of comparing the installed app with the published release. */
    sealed class Decision {
        object UpToDate : Decision()

        /** [prompt] is false when the person skipped this version or postponed reminders. */
        data class Available(val update: AvailableUpdate, val prompt: Boolean) : Decision()

        data class Incompatible(val update: AvailableUpdate, val reason: Incompatibility) :
            Decision()
    }

    /**
     * Compares strictly by Android versionCode, never by the version name, because Android itself
     * only installs an update whose versionCode is higher than the installed one.
     */
    fun evaluate(
        installedVersionCode: Long,
        installedCertificateSha256: String,
        deviceSdk: Int,
        update: AvailableUpdate,
        preferences: UpdatePreferences,
        nowEpochSecond: Long,
    ): Decision {
        if (update.versionCode <= installedVersionCode) return Decision.UpToDate
        if (update.minSdk > deviceSdk)
            return Decision.Incompatible(update, Incompatibility.ANDROID_VERSION)
        if (update.certificateSha256 != installedCertificateSha256.lowercase())
            return Decision.Incompatible(update, Incompatibility.SIGNING)
        val skipped = preferences.skippedVersionCode == update.versionCode
        val snoozed =
            preferences.snoozedVersionCode == update.versionCode &&
                (preferences.snoozedUntilEpochSecond ?: 0) > nowEpochSecond
        return Decision.Available(update, prompt = !skipped && !snoozed)
    }

    /** Automatic checks are opt-out, run at app start at most daily, and never mid-workout. */
    fun shouldCheckAutomatically(
        preferences: UpdatePreferences,
        nowEpochSecond: Long,
        activeWork: Boolean,
    ): Boolean {
        if (!preferences.automaticChecks || activeWork) return false
        val last = preferences.lastCheckEpochSecond ?: return true
        // A clock moved backwards must not suppress checks indefinitely.
        return nowEpochSecond < last || nowEpochSecond - last >= AUTOMATIC_INTERVAL_SECONDS
    }

    /** One notification per version, only when the person opted in and has not dismissed it. */
    fun shouldNotify(preferences: UpdatePreferences, decision: Decision): Boolean =
        decision is Decision.Available &&
            decision.prompt &&
            preferences.notifications &&
            preferences.notifiedVersionCode != decision.update.versionCode

    fun validSha256(value: String): Boolean = HEX_64.matches(value)

    internal fun validCommit(value: String) = COMMIT.matches(value)

    internal fun validVersionName(value: String) = VERSION_NAME.matches(value)

    internal fun validApkName(value: String) = APK_NAME.matches(value)
}

/**
 * A verified build announced by the release manifest. Construction rejects anything that is not an
 * official, versioned, checksummed APK from the Race Day Dreams release.
 */
data class AvailableUpdate(
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val apkName: String,
    val apkUrl: String,
    val apkBytes: Long,
    val apkSha256: String,
    val certificateSha256: String,
    val commit: String,
    val notes: String,
) {
    init {
        require(AppUpdates.validVersionName(versionName)) { "Unexpected version name." }
        require(versionCode in 1..2_100_000_000L) { "Unexpected version code." }
        require(minSdk in 1..1_000) { "Unexpected minimum Android version." }
        require(AppUpdates.validApkName(apkName)) { "Unexpected APK name." }
        // The versioned file, never the rolling alias, so its checksum cannot change underneath.
        require(apkUrl == AppUpdates.RELEASE_DOWNLOAD_PREFIX + apkName) {
            "The update does not come from the official Race Day Dreams release."
        }
        require(apkBytes in AppUpdates.MIN_APK_BYTES..AppUpdates.MAX_APK_BYTES) {
            "Unexpected update size."
        }
        require(AppUpdates.validSha256(apkSha256)) { "Missing update checksum." }
        require(AppUpdates.validSha256(certificateSha256)) { "Missing signing certificate." }
        require(AppUpdates.validCommit(commit)) { "Missing source commit." }
        require(notes.length <= AppUpdates.MAX_NOTES_CHARS) { "Release notes are too long." }
    }
}

/** Device-local choices; never part of backups, because they describe this installation only. */
data class UpdatePreferences(
    val automaticChecks: Boolean = true,
    val notifications: Boolean = true,
    val skippedVersionCode: Long? = null,
    val snoozedVersionCode: Long? = null,
    val snoozedUntilEpochSecond: Long? = null,
    val notifiedVersionCode: Long? = null,
    val lastCheckEpochSecond: Long? = null,
) {
    fun skip(versionCode: Long) = copy(skippedVersionCode = versionCode)

    fun snooze(versionCode: Long, nowEpochSecond: Long) =
        copy(
            snoozedVersionCode = versionCode,
            snoozedUntilEpochSecond = nowEpochSecond + AppUpdates.SNOOZE_SECONDS,
        )

    fun notified(versionCode: Long) = copy(notifiedVersionCode = versionCode)

    fun checked(nowEpochSecond: Long) = copy(lastCheckEpochSecond = nowEpochSecond)
}
