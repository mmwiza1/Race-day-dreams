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
import org.racedaydreams.domain.strength.*
import org.racedaydreams.storage.*

/**
 * Real generation/revision/progression operations, typed SQL, original engine and no fake actuals.
 */
@RunWith(AndroidJUnit4::class)
class SharedStrengthGenerationCoordinatorTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val now = Instant.parse("2026-11-17T18:00:00Z")
    private val today = LocalDate.of(2026, 11, 17)

    private class Fixture(
        val model: SharedStrengthGenerationCoordinator,
        val drafts: SqliteConcreteStrengthProgrammes,
        val programmes: SqliteStrengthProgramming,
        val db: SqlDatabase,
        val retained: AtomicReference<String?>,
    )

    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val calls = AtomicInteger()
            val db = AndroidSqlConnection {
                check(Looper.myLooper() != Looper.getMainLooper()) {
                    "Generation SQL must stay off Main."
                }
                calls.incrementAndGet()
                raw
            }
            val sequence = AtomicInteger()
            val next = { "generation-event-${sequence.incrementAndGet()}" }
            val programmes = SqliteStrengthProgramming(db, { now }, { today })
            val drafts = SqliteConcreteStrengthProgrammes(db, { now }, { today })
            val coaching =
                SqliteCoachingRepository(db, { now }, next, PortableLegacyStrengthFingerprint)
            val gym = SqliteGymExecutions(db, { now }, PortableLegacyStrengthFingerprint)
            val retained = AtomicReference<String?>(null)
            val value = AtomicReference<SharedStrengthGenerationCoordinator>()
            instrumentation.runOnMainSync {
                value.set(
                    SharedStrengthGenerationCoordinator(
                        db,
                        drafts,
                        programmes,
                        coaching,
                        gym,
                        gym::recentActual,
                        scope,
                        retained::get,
                        {
                            check(Looper.myLooper() == Looper.getMainLooper()) {
                                "Retained ID must publish on Main."
                            }
                            retained.set(it)
                        },
                        { now },
                        { today },
                        next,
                    )
                )
            }
            assertEquals(0, calls.get())
            block(Fixture(value.get(), drafts, programmes, db, retained))
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }

    private fun command(f: Fixture, action: () -> Unit): StrengthGenerationState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.busy && !it.loading } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }

    private fun generate(f: Fixture): ConcreteStrengthProgrammeDraft {
        val settings =
            StrengthProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.STRENGTH_FIRST,
                GymProgramme.Experience.BEGINNER,
                setOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                2,
                45,
                emptySet(),
            )
        val state =
            command(f) {
                f.model.generate(
                    "Original generated programme",
                    today,
                    4,
                    settings,
                    "full-body-2",
                    false,
                    {},
                )
            }
        assertNull(state.error)
        return state.draft!!
    }

    @Test
    fun standaloneStrengthGenerationIsDurableWithoutRunningProfileOrAutomaticActivation() =
        fixture { f ->
            val empty = command(f) { f.model.reload() }
            assertNull(empty.current)
            val draft = generate(f)
            assertEquals(8, draft.programme().workouts().size)
            assertEquals(draft, f.drafts.get(draft.id()))
            assertEquals(draft.id(), f.retained.get())
            assertNull(f.programmes.currentReference())
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
            assertNull(SqliteAthleteProfile(f.db).load())
            command(f) { f.model.close() }
            assertNull(f.retained.get())
            val callback = AtomicReference<String>()
            val opened =
                command(f) {
                    f.model.open(draft.id()) {
                        assertFalse(f.model.state.value.busy)
                        assertEquals(f.model.state.value.draft!!.id(), f.retained.get())
                        callback.set(f.retained.get())
                    }
                }
            assertEquals(draft, opened.draft)
            assertEquals(draft.id(), callback.get())
            assertNull(f.programmes.currentReference())
        }

    @Test
    fun futureTargetEditsRequireFreshActivationAndRetainPreviousPrescriptionWithoutActuals() =
        fixture { f ->
            val initial = generate(f)
            val accepted = command(f) { f.model.activate(initial, "accept-original", {}) }
            assertNull(accepted.error)
            val original = f.programmes.programme(initial.programme().id(), 1)!!
            val next = command(f) { f.model.prepareFuture({}) }.draft!!
            assertEquals(1, next.programme().parentRevision())
            assertEquals(2, next.programme().revision())
            assertEquals(1, f.programmes.currentReference()!!.revision())
            val workout =
                next.programme().workouts().first {
                    it.definition().slots().any { slot -> slot.exercise().measurement().usesReps() }
                }
            val slot = workout.definition().slots().first { it.exercise().measurement().usesReps() }
            val edited =
                command(f) {
                    f.model.putSlot(
                        workout.id(),
                        slot.id(),
                        slot.exercise(),
                        GymSlotInput(
                            sets = "2",
                            minimumReps = "6",
                            maximumReps = "10",
                            minimumEffort = "5",
                            maximumEffort = "7",
                        ),
                        {},
                    )
                }
            assertNull(edited.error)
            assertEquals(original, f.programmes.programme(original.id(), 1))
            assertEquals(1, f.programmes.currentReference()!!.revision())
            val stale =
                command(f) {
                    f.model.activate(next, "stale-accept") {
                        fail("Changed draft must not activate")
                    }
                }
            assertNotNull(stale.error)
            assertEquals(1, f.programmes.currentReference()!!.revision())
            val final = command(f) { f.model.activate(edited.draft!!, "accept-edited", {}) }
            assertNull(final.error)
            assertEquals(2, f.programmes.currentReference()!!.revision())
            assertEquals(original, f.programmes.programme(original.id(), 1))
            assertEquals(edited.draft!!.programme(), f.programmes.programme(original.id(), 2))
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        }

    @Test
    fun insufficientWorkingEvidenceCannotSaveAnOptionalProgressionOrRewriteTargets() =
        fixture { f ->
            val draft = generate(f)
            val workout = draft.programme().workouts().first()
            val slot = workout.definition().slots().first()
            val reviewed = command(f) { f.model.reviewProgression(workout.id(), slot.id()) }
            assertNull(reviewed.error)
            assertNull(reviewed.progressionReview!!.advice.proposedTarget())
            assertTrue(reviewed.progressionReview!!.advice.supportingSessionIds().isEmpty())
            val rejected = command(f) { f.model.saveProgressionProposal() }
            assertNotNull(rejected.error)
            assertEquals(draft, rejected.draft)
            assertEquals(draft, f.drafts.get(draft.id()))
            assertNull(f.programmes.currentReference())
            assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        }
}
