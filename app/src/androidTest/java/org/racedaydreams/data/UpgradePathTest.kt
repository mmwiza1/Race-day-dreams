package org.racedaydreams.data

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import java.time.*
import java.util.EnumSet
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.climbing.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.SqliteClimbing
import org.racedaydreams.update.ApkVerifier
import org.racedaydreams.update.InstalledApp
import org.racedaydreams.domain.update.AppUpdates
import org.racedaydreams.domain.update.AvailableUpdate

/**
 * The real in-place upgrade path, driven by scripts/verify-android-upgrade.sh on an emulator:
 *
 * 1. `-e upgradePhase seed` on the old build writes synthetic user data and reports a SHA-256
 *    fingerprint of every database table plus device-local preferences.
 * 2. The script installs a newer build signed with the same key over it (`adb install -r`).
 * 3. `-e upgradePhase verify` checks the fingerprint is unchanged, the data reads back, and that
 *    the in-app verifier accepts a newer same-key APK and rejects a differently signed one.
 *
 * Without arguments (the normal suite) it only checks the fingerprint is stable and complete.
 */
@RunWith(AndroidJUnit4::class)
class UpgradePathTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val args = InstrumentationRegistry.getArguments()

    @Test
    fun seedRealUserDataForTheUpgrade() {
        if (args.getString("upgradePhase") != "seed") {
            assertEquals(fingerprint(), fingerprint())
            return
        }
        seed()
        instrumentation.sendStatus(0, Bundle().apply { putString("fingerprint", fingerprint()) })
    }

    @Test
    fun userDataSurvivesTheUpdate() {
        if (args.getString("upgradePhase") != "verify") {
            assertTrue(tables().contains("climbing_sessions"))
            return
        }
        assertEquals(args.getString("expectedFingerprint"), fingerprint())
        // Read back through the real repositories, not only as bytes.
        val climb = SqliteClimbing(AndroidSqlConnection.forApplication(context)).sessions().single()
        assertEquals(ClimbingGrade(GradeSystem.FONTAINEBLEAU, "6C+"), climb.problems.first().grade)
        assertEquals(GradeSystem.FONTAINEBLEAU, SqliteClimbing(AndroidSqlConnection.forApplication(context)).gradeSystem())
        assertEquals("Upgrade test shoe", FeatureStore(context).shoes().single { it.id() == "upgrade-shoe" }.brand())
        assertNotNull(SqliteUserPreferencesRepository(context).load())
        assertEquals(
            args.getString("installedVersionCode")?.toLong(),
            InstalledApp.read(context).versionCode,
        )
        args.getString("nextApk")?.let { verifyRealArtifacts(it, args.getString("foreignApk")!!) }
    }

    /** The in-app verifier on real APKs built by the script with real, different keys. */
    private fun verifyRealArtifacts(nextPath: String, foreignPath: String) {
        val installed = InstalledApp.read(context)
        assertTrue("The upgrade-test build embeds its own release fingerprint", installed.updateCapable)
        fun pull(path: String, name: String): File {
            val target = File(context.cacheDir, name)
            instrumentation.uiAutomation.executeShellCommand("cat $path").use { pfd ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }
            return target
        }
        fun announce(apk: File, code: Long, certificate: String) =
            AvailableUpdate(
                versionName = "0.99.0-test1",
                versionCode = code,
                minSdk = 28,
                apkName = "Race-Day-Dreams-0.99.0-test1.apk",
                apkUrl = AppUpdates.RELEASE_DOWNLOAD_PREFIX + "Race-Day-Dreams-0.99.0-test1.apk",
                apkBytes = apk.length(),
                apkSha256 = ApkVerifier.sha256(apk),
                certificateSha256 = certificate,
                commit = "0".repeat(40),
                notes = "",
            )
        val next = pull(nextPath, "upgrade-next.apk")
        assertEquals(
            ApkVerifier.Result.Verified,
            ApkVerifier.verify(context, next, announce(next, installed.versionCode + 1, installed.releaseCertificateSha256), installed),
        )
        val foreign = pull(foreignPath, "upgrade-foreign.apk")
        val rejected =
            ApkVerifier.verify(context, foreign, announce(foreign, installed.versionCode + 2, installed.releaseCertificateSha256), installed)
        assertTrue("$rejected", rejected is ApkVerifier.Result.Rejected)
        assertTrue((rejected as ApkVerifier.Result.Rejected).reason.contains("different key"))
        next.delete()
        foreign.delete()
    }

    private fun seed() {
        val today = LocalDate.of(2026, 11, 18)
        SqliteUserPreferencesRepository(context)
            .save(
                UserProfile(
                    RaceGoal(RaceDistance.HALF_MARATHON, "Upgrade Half", today.plusWeeks(12), GoalMode.TARGET_TIME, Duration.ofSeconds(7199)),
                    TrainingPreferences(
                        TrainingStrategy.BALANCED,
                        EnumSet.allOf(DayOfWeek::class.java),
                        4,
                        DayOfWeek.SUNDAY,
                        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        ScheduleType.FLEXIBLE,
                        UnitSystem.METRIC,
                    ),
                    false,
                )
            )
        val store = FeatureStore(context)
        store.putSetting("baseline.reviewed", "true")
        store.saveManualBaseline(ManualBaselineInput(25.0, 4.0, 12.0, Duration.ofMinutes(27), null, null, 390.0, 8))
        store.saveShoe(Shoe("upgrade-shoe", "Upgrade test shoe", "Daily trainer", today.minusDays(80), 120000.0, false, "Synthetic upgrade fixture", true))
        val evidence = DevelopmentStore(context)
        for (i in 0..3) {
            val at = today.minusWeeks(i.toLong()).atTime(12, 0).atOffset(ZoneOffset.UTC)
            evidence.saveWeight(
                org.racedaydreams.domain.body.BodyWeight("upgrade-weight-$i", 72.0 + i * .2, at.toInstant(), at.offset, "manual", "upgrade-weight-$i", null)
            )
        }
        evidence.saveHistorical(
            HistoricalStrengthRecord(
                "upgrade-historical",
                StrengthExercise.starters().first(),
                80.0,
                5,
                null,
                HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                "Synthetic upgrade fixture",
            )
        )
        val climbing = SqliteClimbing(AndroidSqlConnection.forApplication(context))
        climbing.saveNew(
            ClimbingSession(
                "upgrade-climb",
                today.atTime(18, 0).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                5400,
                "North wall",
                "Synthetic upgrade fixture",
                listOf(
                    ClimbingProblem("p1", ClimbingGrade(GradeSystem.FONTAINEBLEAU, "6C+"), 1, ClimbOutcome.FLASH),
                    ClimbingProblem("p2", ClimbingGrade(GradeSystem.V_SCALE, "V5"), 4, ClimbOutcome.SEND),
                    ClimbingProblem("p3", null, 2, ClimbOutcome.NOT_SENT),
                ),
            )
        )
        climbing.gradeSystem(GradeSystem.FONTAINEBLEAU)
        context
            .getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
            .edit()
            .putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, false)
            .commit()
    }

    private fun tables(): List<String> =
        ProfileDatabase.get(context).readableDatabase.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT IN ('android_metadata','sqlite_sequence') ORDER BY name",
                null,
            )
            .use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }

    /** Every row of every table, the schema version and the appearance preference. */
    private fun fingerprint(): String {
        val db = ProfileDatabase.get(context).readableDatabase
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("user_version=${db.version}\n".toByteArray())
        for (table in tables()) {
            val rows = mutableListOf<String>()
            db.rawQuery("SELECT * FROM \"$table\"", null).use { c ->
                while (c.moveToNext())
                    rows +=
                        (0 until c.columnCount).joinToString("\u0001") { i ->
                            when (c.getType(i)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> "∅"
                                android.database.Cursor.FIELD_TYPE_BLOB -> ApkVerifier.hex(c.getBlob(i))
                                else -> c.getString(i)
                            }
                        }
            }
            digest.update("$table:${rows.size}\n".toByteArray())
            rows.sorted().forEach { digest.update((it + "\n").toByteArray()) }
        }
        val appearance =
            context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0).all.toSortedMap().toString()
        digest.update(appearance.toByteArray())
        return ApkVerifier.hex(digest.digest())
    }
}
