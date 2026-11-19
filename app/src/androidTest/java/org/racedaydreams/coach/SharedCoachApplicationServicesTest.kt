package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.ExerciseExclusionRepository
import org.racedaydreams.health.*
import org.racedaydreams.storage.*

/** Actual shared root, real SQL and original snapshot; no main-thread DB or competing runner. */
@RunWith(AndroidJUnit4::class)
class SharedCoachApplicationServicesTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)

    private class Fixture(
        val root: SharedCoachApplicationServices<String>,
        val db: SqlDatabase,
        val calls: AtomicInteger,
        val reconciles: AtomicInteger,
        val healthReads: AtomicInteger,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val calls = AtomicInteger()
            val reconciles = AtomicInteger()
            val healthReads = AtomicInteger()
            val db = AndroidSqlConnection {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "Root storage must run on IO."
                }
                calls.incrementAndGet()
                raw
            }
            val identities = AtomicInteger()
            val next = { "root-event-${identities.incrementAndGet()}" }
            val repositories =
                CoachRepositories(
                    SqliteActivities(db) { now },
                    SqliteResults(db),
                    SqliteShoes(db),
                    SqliteBaselines(db) { now },
                    SqliteSettings(db),
                    SqlitePlannedWorkouts(db),
                    SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint),
                    SqliteStrengthProgramming(db, { now }, { today }),
                )
            val gateway =
                object : HealthGateway {
                    override fun availability() = HealthAvailability.UNAVAILABLE

                    override suspend fun grantedPermissions(): Set<String> {
                        healthReads.incrementAndGet()
                        error("Unavailable health must not claim grants.")
                    }

                    override suspend fun sync(store: HealthImportRepository): HealthSyncResult =
                        error("No automatic health import in root construction.")
                }
            val snapshot =
                object : CoachSnapshotPlatform {
                    override fun importWindow(): Pair<Instant, Instant>? = null

                    override fun reconcileReminders() {
                        reconciles.incrementAndGet()
                    }

                    override fun reminderPreferences() = ReminderSettings.defaults("Europe/London")

                    override fun healthAvailability() = gateway.availability()

                    override suspend fun grantedHealthPermissions() = gateway.grantedPermissions()
                }
            val documents =
                object : SettingsPlatform<String> {
                    override val restoredMessage = "Unused selected-document boundary"

                    override fun exportBackup(file: String): Unit = error("No document selected.")

                    override fun restoreBackup(file: String): Unit = error("No restore approved.")

                    override fun reset(): Unit = error("No reset approved.")

                    override fun saveReminderPreferences(preferences: ReminderSettings): Unit =
                        error("No reminder edit requested.")
                }
            val gym =
                SharedGymProgrammeOperations(
                    {
                        object : GymProgrammeActionClock {
                            override fun instant() = now

                            override fun today() = this@SharedCoachApplicationServicesTest.today
                        }
                    },
                    next,
                )
            val platform =
                CoachPlatformBindings(
                    snapshot,
                    gateway,
                    emptySet(),
                    { error("No body access requested.") },
                    documents,
                    gym,
                    { date ->
                        val at = date.atTime(12, 0).atZone(ZoneId.of("Europe/London"))
                        at.toInstant() to at.offset
                    },
                    { date, time, duration ->
                        val start =
                            date.atTime(LocalTime.parse(time)).atZone(ZoneId.of("Europe/London"))
                        val end = start.plus(duration)
                        RunningEvidenceCoordinator.Timing(
                            start.toInstant(),
                            end.toInstant(),
                            start.offset,
                            end.offset,
                        )
                    },
                    { it.toString() },
                    { false },
                )
            val root = AtomicReference<SharedCoachApplicationServices<String>>()
            instrumentation.runOnMainSync {
                root.set(
                    SharedCoachApplicationServices(
                        db,
                        scope,
                        { null },
                        repositories,
                        platform,
                        { now },
                        { today },
                        next,
                    )
                )
            }
            assertEquals(
                "Constructing the real root must not open its borrowed database.",
                0,
                calls.get(),
            )
            block(Fixture(root.get(), db, calls, reconciles, healthReads))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun execute(f: Fixture, action: () -> Unit): CoachUiState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20_000) { f.root.operations.state.first { !it.busy } } }
        instrumentation.waitForIdleSync()
        return f.root.operations.current
    }

    @Test
    fun rootIsLazyAndRefreshUsesRealSnapshotWithoutInventingTrainingOrHealthAccess() =
        fixture { f ->
            val state = execute(f) { f.root.operations.refresh() }
            assertNull(state.error)
            assertFalse(state.loading)
            assertTrue(f.calls.get() > 0)
            assertEquals(1, f.reconciles.get())
            assertNull(state.profile)
            assertNull(state.currentPlan)
            assertNull(state.primaryFocus)
            assertTrue(state.activities.isEmpty())
            assertTrue(state.weights.isEmpty())
            assertTrue(state.strengthSessions.isEmpty())
            assertEquals(HealthAvailability.UNAVAILABLE, state.healthAvailability)
            assertEquals(0, f.healthReads.get())
        }

    @Test
    fun strengthOnlyCrossFeatureCommandsShareOneRunnerAndPublishBeforeSuccessCallback() =
        fixture { f ->
            val captured = AtomicReference<CoachUiState>()
            val complete = CountDownLatch(1)
            instrumentation.runOnMainSync {
                f.root.progress.saveWeight(87.0, today.minusDays(1))
                f.root.settings.primaryFocus(PrimaryFocus.STRENGTH) {
                    captured.set(f.root.operations.current)
                    complete.countDown()
                }
            }
            assertTrue("Both serial commands must finish.", complete.await(20, TimeUnit.SECONDS))
            val state = captured.get()
            assertNull(state.error)
            assertEquals(PrimaryFocus.STRENGTH, state.primaryFocus)
            assertNull(state.profile)
            assertNull(state.currentPlan)
            val weight = state.weights.single()
            assertEquals(87.0, weight.kilograms(), 0.0)
            assertEquals("manual", weight.provider())
            assertEquals(weight.id(), weight.sourceId())
            assertEquals(Instant.parse("2026-11-16T12:00:00Z"), weight.at())
            assertTrue(state.activities.isEmpty())
            assertTrue(state.strengthSessions.isEmpty())
        }

    @Test
    fun failedFeatureCommandRetainsCoherentEvidenceAndOtherFeatureCanContinue() = fixture { f ->
        assertNull(execute(f) { f.root.settings.primaryFocus(PrimaryFocus.STRENGTH) }.error)
        val exercise = f.root.operations.current.exercises.first().id()
        assertNull(
            execute(f) {
                    f.root.strength.exerciseExclusion(
                        exercise,
                        ExerciseExclusionRepository.Reason.PREFERENCE,
                    )
                }
                .error
        )
        val before = f.root.operations.current.exerciseExclusions
        f.db.execute(
            "CREATE TRIGGER reject_mass_unit BEFORE INSERT ON settings WHEN NEW.setting_key='mass.unit' BEGIN SELECT RAISE(ABORT,'Rejected setting'); END"
        )
        val rejected = execute(f) { f.root.settings.setting("mass.unit", "lb") }
        assertNotNull(rejected.error)
        assertEquals(PrimaryFocus.STRENGTH, rejected.primaryFocus)
        assertEquals(before, rejected.exerciseExclusions)
        val continued = execute(f) { f.root.progress.saveWeight(70.0, today.minusDays(1)) }
        assertNull(continued.error)
        assertEquals(before, continued.exerciseExclusions)
        assertEquals(70.0, continued.weights.single().kilograms(), 0.0)
        assertEquals(PrimaryFocus.STRENGTH, continued.primaryFocus)
        assertTrue(continued.activities.isEmpty())
        assertNull(continued.currentPlan)
    }
}
