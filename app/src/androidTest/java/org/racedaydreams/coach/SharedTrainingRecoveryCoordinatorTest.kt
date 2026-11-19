package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.strength.*
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.createAndroidSchemaFixture
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.recovery.TrainingRecovery
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

/**
 * Actual SQL and original recovery policy; the optional-provider fixture supplies no observations.
 */
@RunWith(AndroidJUnit4::class)
class SharedTrainingRecoveryCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00Z")

    private class Fixture(
        val model: SharedTrainingRecoveryCoordinator,
        val db: SqlDatabase,
        val programmes: SqliteStrengthProgramming,
        val drafts: SqliteConcreteStrengthProgrammes,
        val coaching: SqliteCoachingRepository,
        val calls: AtomicInteger,
        val today: LocalDate,
    )

    private fun fixture(offset: ZoneOffset = ZoneOffset.UTC, block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val today = now.atOffset(offset).toLocalDate()
        try {
            createAndroidSchemaFixture(raw)
            val calls = AtomicInteger()
            val db = AndroidSqlConnection {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "Recovery SQL must stay off Main."
                }
                calls.incrementAndGet()
                raw
            }
            val ids = AtomicInteger()
            val next = { "recovery-event-${ids.incrementAndGet()}" }
            val programmes = SqliteStrengthProgramming(db, { now }, { today })
            val drafts = SqliteConcreteStrengthProgrammes(db, { now }, { today })
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val gym = SqliteGymExecutions(db, { now }, PortableLegacyStrengthFingerprint)
            val activities = SqliteActivities(db) { now }
            val focus = SqlitePrimaryFocus(SqliteSettings(db))
            val reference = AtomicReference<SharedTrainingRecoveryCoordinator>()
            instrumentation.runOnMainSync {
                reference.set(
                    SharedTrainingRecoveryCoordinator(
                        programmes,
                        drafts,
                        gym,
                        coaching,
                        activities,
                        { date ->
                            check(Looper.myLooper() != Looper.getMainLooper())
                            assertEquals(today, date)
                            emptyList()
                        },
                        focus::load,
                        scope,
                        { now },
                        { offset },
                        next,
                    )
                )
            }
            assertEquals(0, calls.get())
            block(Fixture(reference.get(), db, programmes, drafts, coaching, calls, today))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun command(f: Fixture, action: () -> Unit): TrainingRecoveryState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.busy } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }

    @Test
    fun missingEvidenceUsesActualLocalDateAndKeepCannotInventOrWriteADeload() =
        fixture(ZoneOffset.ofHours(14)) { f ->
            val state = command(f) { f.model.refresh() }
            assertNull(state.error)
            assertEquals(LocalDate.of(2026, 11, 18), state.review!!.date())
            assertFalse(state.review!!.lighterWeek())
            assertFalse(state.canReviewLighterWeek)
            assertNull(state.programme)
            assertTrue(state.conflicts.isEmpty())
            val reads = f.calls.get()
            instrumentation.runOnMainSync { f.model.keep() }
            assertEquals(reads, f.calls.get())
            assertEquals("Your current programme is unchanged.", f.model.state.value.notice)
            val rejected =
                command(f) {
                    f.model.prepareLighterWeek { fail("Missing evidence cannot open a deload") }
                }
            assertNotNull(rejected.error)
            assertNull(f.programmes.currentReference())
            assertTrue(f.drafts.list(20, 0).isEmpty())
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun hybridMoveOnlySavesPreviewAndChangedProgrammingRejectsAStaleReview() = fixture { f ->
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.BEGINNER,
                DayOfWeek.values().toSet(),
                2,
                45,
                emptySet(),
            )
        val original =
            StrengthProgrammeGenerator()
                .generate(
                    StrengthProgrammeGenerator.Request(
                        "recovery-original",
                        now,
                        "Existing gym programme",
                        settings,
                        f.today,
                        4,
                        "full-body-2",
                        PrimaryFocus.STRENGTH,
                        emptySet(),
                        null,
                        emptySet(),
                        emptySet(),
                        false,
                    )
                )
        f.programmes.activate(original, null, "accept-original", emptyList())
        val workout = original.workouts().first { TrainingRecovery.meaningfulLower(it) }
        val profile =
            UserProfile(
                RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
                TrainingPreferences(
                    TrainingStrategy.BALANCED,
                    DayOfWeek.values().toSet(),
                    3,
                    DayOfWeek.SUNDAY,
                    emptySet(),
                    ScheduleType.FLEXIBLE,
                    UnitSystem.METRIC,
                ),
                false,
            )
        SqliteAthleteProfile(f.db).save(profile)
        val run =
            PlannedWorkout(
                "demanding-run",
                workout.date(),
                WorkoutCategory.INTERVALS,
                5.0,
                Duration.ofMinutes(30),
                emptyList(),
                "Explicit running prescription",
            )
        val running =
            PlanVersion(
                "running-original",
                "running",
                1,
                now,
                "Athlete choice",
                profile,
                "Retained running evidence",
                TrainingPlan("running", f.today, f.today.plusWeeks(4), emptyList(), listOf(run)),
            )
        f.coaching.saveCurrent(running)
        SqlitePrimaryFocus(SqliteSettings(f.db)).save(PrimaryFocus.HYBRID)
        val state = command(f) { f.model.refresh() }
        assertNull(state.error)
        assertTrue(state.conflicts.any { it.workoutId() == workout.id() })
        val date = state.alternatives[workout.id()]!!.first()
        val opened = AtomicReference<String>()
        val proposed =
            command(f) {
                f.model.prepareMove(workout.id(), date) { id ->
                    check(Looper.myLooper() == Looper.getMainLooper())
                    assertFalse(f.model.state.value.busy)
                    opened.set(id)
                }
            }
        assertNull(proposed.error)
        val saved = f.drafts.get(opened.get())!!
        assertEquals(date, saved.programme().workouts().first { it.id() == workout.id() }.date())
        assertEquals(original, f.programmes.programme(original.id(), 1))
        assertEquals(1, f.programmes.currentReference()!!.revision())
        assertEquals(running, f.coaching.current())
        assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        val drafts = f.drafts.list(20, 0)
        f.programmes.stop(f.programmes.currentReference()!!, "athlete-stopped-elsewhere")
        val rejected =
            command(f) {
                f.model.prepareMove(workout.id(), date) {
                    fail("Changed review cannot save another proposal")
                }
            }
        assertNotNull(rejected.error)
        assertEquals(drafts, f.drafts.list(20, 0))
        assertEquals(saved, f.drafts.get(saved.id()))
        assertEquals(original, f.programmes.programme(original.id(), 1))
        assertEquals(running, f.coaching.current())
    }
}
