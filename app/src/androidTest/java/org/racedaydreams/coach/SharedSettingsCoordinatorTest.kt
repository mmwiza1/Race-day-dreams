package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.reminders.QuietWindow
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.storage.*

/** Common commands use real settings storage; native document/alarm ownership stays explicit. */
@RunWith(AndroidJUnit4::class)
class SharedSettingsCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private class Platform : SettingsPlatform<String> {
        override val restoredMessage = "Backup restored by the selected platform."
        val files = mutableListOf<String>()
        var failRestore = false
        var resets = 0
        var preferences: ReminderSettings? = null

        override fun exportBackup(file: String) {
            files.add("export:$file")
        }

        override fun restoreBackup(file: String) {
            files.add("restore:$file")
            if (failRestore) throw IllegalArgumentException("Backup rejected before replacement.")
        }

        override fun reset() {
            resets++
        }

        override fun saveReminderPreferences(preferences: ReminderSettings) {
            this.preferences = preferences
        }
    }

    private class Fixture(
        val service: SettingsCoordinator<String>,
        val runner: CoachOperationRunner,
        val settings: SqliteSettings,
        val focus: SqlitePrimaryFocus,
        val platform: Platform,
        val recording: AtomicBoolean,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val db = AndroidSqlConnection { raw }
            val settings = SqliteSettings(db)
            val focus = SqlitePrimaryFocus(settings)
            val platform = Platform()
            val recording = AtomicBoolean(false)
            val runner = CoachOperationRunner(scope, { CoachUiState(loading = false) })
            val service = SettingsCoordinator(runner, settings, focus, platform, recording::get)
            block(Fixture(service, runner, settings, focus, platform, recording))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun execute(f: Fixture, action: () -> Unit): CoachUiState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20_000) { f.runner.state.first { !it.busy } } }
        instrumentation.waitForIdleSync()
        return f.runner.current
    }

    @Test
    fun focusAndApprovedSettingsPersistWithoutAllowingArbitrarySettingWrites() = fixture { f ->
        assertNull(execute(f) { f.service.startingPath("hybrid") }.error)
        assertEquals("hybrid", f.settings.getSetting("starting.path"))
        assertEquals(PrimaryFocus.HYBRID, f.focus.load())
        assertNull(execute(f) { f.service.primaryFocus(PrimaryFocus.STRENGTH) }.error)
        assertEquals(PrimaryFocus.STRENGTH, f.focus.load())
        assertNull(execute(f) { f.service.setting("calendar.view", "week") }.error)
        assertEquals("week", f.settings.getSetting("calendar.view"))
        assertNotNull(execute(f) { f.service.setting("baseline.reviewed", "true") }.error)
        assertNull(f.settings.getSetting("baseline.reviewed"))
        assertNotNull(execute(f) { f.service.startingPath("unsupported") }.error)
        assertEquals("hybrid", f.settings.getSetting("starting.path"))
        assertEquals(PrimaryFocus.STRENGTH, f.focus.load())
    }

    @Test
    fun documentCommandsKeepSelectedIdentityAndFailureDoesNotReportRestoreSuccess() = fixture { f ->
        var successes = 0
        var failures = 0
        assertNull(execute(f) { f.service.exportBackup("chosen-export") }.error)
        val restored =
            execute(f) {
                f.service.restoreBackup("chosen-restore", { successes++ }, { failures++ })
            }
        assertNull(restored.error)
        assertEquals(f.platform.restoredMessage, restored.message)
        assertEquals(1, successes)
        assertEquals(0, failures)
        f.platform.failRestore = true
        val rejected =
            execute(f) {
                f.service.restoreBackup("invalid-backup", { successes++ }, { failures++ })
            }
        assertEquals("Backup rejected before replacement.", rejected.error)
        assertEquals(1, successes)
        assertEquals(1, failures)
        assertEquals(
            listOf("export:chosen-export", "restore:chosen-restore", "restore:invalid-backup"),
            f.platform.files,
        )
    }

    @Test
    fun activeRecordingResetReviewAndExactReminderSettingsRetainExistingBoundaries() =
        fixture { f ->
            f.recording.set(true)
            assertFalse(f.service.canReset())
            assertEquals(
                "Finish the active recording before deleting app data.",
                f.runner.current.error,
            )
            assertEquals(0, f.platform.resets)
            f.recording.set(false)
            assertTrue(f.service.canReset())
            var complete = false
            assertNull(
                execute(f) {
                        f.service.reset({ complete = true }, { fail("Unexpected reset failure") })
                    }
                    .error
            )
            assertTrue(complete)
            assertEquals(1, f.platform.resets)
            val preferences =
                ReminderSettings(
                    ReminderSettings.Mode.LONG_HARD_ONLY,
                    19 * 3600 * 1000000000L + 123,
                    QuietWindow(22 * 3600 * 1000000000L, 7 * 3600 * 1000000000L),
                    "Europe/London",
                )
            assertNull(execute(f) { f.service.saveReminderPreferences(preferences) }.error)
            assertEquals(preferences, f.platform.preferences)
            assertNull(execute(f) { f.service.setMilestonesEnabled(false) }.error)
            assertEquals("false", f.settings.getSetting("milestones.enabled"))
        }
}
