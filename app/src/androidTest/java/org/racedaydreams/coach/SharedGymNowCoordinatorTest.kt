package org.racedaydreams.coach

import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
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

/** Actual shared operations and real SQL; clocks/alerts are explicitly controlled platform effects. */
@RunWith(AndroidJUnit4::class)
class SharedGymNowCoordinatorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val now = Instant.parse("2026-11-17T12:00:00.123456789Z")
    private class Retained : GymNowRetainedState {
        val values = mutableMapOf<String, Any?>()
        override fun get(key: String): Any? = values[key]
        override fun set(key: String, value: Any?) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
        override fun strings(key: String): List<String>? =
            (values[key] as? List<*>)?.map { it as String }
    }
    private inner class Fixture(val raw: SQLiteDatabase, val scope: CoroutineScope) {
        val calls = AtomicInteger()
        val db = AndroidSqlConnection {
            check(Looper.myLooper() != Looper.getMainLooper()) { "Gym SQL must stay off Main." }
            calls.incrementAndGet()
            raw
        }
        val millis = AtomicLong(1000)
        val epoch = AtomicReference("original-boot")
        val ids = AtomicInteger()
        val retained = Retained()
        val gym = SqliteGymExecutions(db, { now }, PortableLegacyStrengthFingerprint)
        var loseSaveAcknowledgement = false
        val writes = object : GymExecutionRepository by gym {
            override fun save(snapshot: GymExecution, expectedRevision: Long): GymExecutionRepository.Stored {
                val saved = gym.save(snapshot, expectedRevision)
                if (loseSaveAcknowledgement) {
                    loseSaveAcknowledgement = false
                    throw IllegalStateException("Saved, but acknowledgement interrupted.")
                }
                return saved
            }
        }
        val alerts = mutableListOf<GymRestTimer?>()
        lateinit var model: SharedGymNowCoordinator
        fun construct(): SharedGymNowCoordinator {
            val result = AtomicReference<SharedGymNowCoordinator>()
            instrumentation.runOnMainSync {
                result.set(SharedGymNowCoordinator(
                    db, writes, SqliteGymAssociations(db) { now },
                    SqliteStrengthProgramming(db, { now }, { java.time.LocalDate.of(2026, 11, 17) }),
                    SqliteCoachingRepository(db, { now }, { "coaching-${ids.incrementAndGet()}" }, PortableLegacyStrengthFingerprint),
                    { null }, { _, _ -> null }, gym::favourites, gym::recentActual, gym::finishedActualIds,
                    retained, scope,
                    GymWorkoutSessionCoordinator.TickSource { GymExecution.Tick("process", millis.get(), now) },
                    { GymRestTimer.Clock(epoch.get(), millis.get()) },
                    { timer, _ -> alerts.add(timer) }, { now }, { ZoneOffset.ofHours(14) },
                    { "gym-event-${ids.incrementAndGet()}" },
                ))
            }
            return result.get()
        }
    }
    private fun fixture(block: (Fixture) -> Unit) {
        val raw = SQLiteDatabase.create(null)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        try {
            createAndroidSchemaFixture(raw)
            val f = Fixture(raw, scope)
            f.model = f.construct()
            assertEquals(0, f.calls.get())
            block(f)
        } finally {
            instrumentation.runOnMainSync { scope.cancel() }
            raw.close()
        }
    }
    private fun command(f: Fixture, action: () -> Unit): GymNowState {
        instrumentation.runOnMainSync { action() }
        runBlocking { withTimeout(20000) { f.model.state.first { !it.busy && !it.loading } } }
        instrumentation.waitForIdleSync()
        return f.model.state.value
    }
    @Test fun lostAcknowledgementRetainsFormAndExactRetryDoesNotDuplicateActual() = fixture { f ->
        command(f) { f.model.refresh() }
        var ready = false
        val started = command(f) { f.model.startFree { ready = true } }
        assertTrue(ready)
        assertEquals(ZoneOffset.ofHours(14), started.view!!.observed()!!.offset())
        val exercise = ExerciseCatalog.exercises().first { it.id() == "bench-press" }
        command(f) { f.model.addExercise(exercise) }
        command(f) { f.model.updateForm { it.copy(kilograms = "invalid", repetitions = "6") } }
        val invalid = command(f) { f.model.saveSet() }
        assertNotNull(invalid.error)
        assertEquals("invalid", invalid.form.kilograms)
        assertTrue(invalid.view!!.observed()!!.actualSets().isEmpty())
        command(f) { f.model.updateForm { it.copy(kilograms = "80", repetitions = "6", effort = "7") } }
        val entered = f.model.state.value.form
        f.loseSaveAcknowledgement = true
        val pending = command(f) { f.model.saveSet() }
        assertNotNull(pending.view!!.pending())
        assertEquals(entered, pending.form)
        assertEquals(1, f.gym.load(pending.view!!.observed()!!.id())!!.execution().actualSets().size)
        val saved = command(f) { f.model.retry() }
        assertNull(saved.view!!.pending())
        assertEquals(1, saved.view!!.observed()!!.actualSets().size)
        assertEquals("", saved.form.kilograms)
        assertNotEquals(entered.id, saved.form.id)
        assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
    }
    @Test fun builderSupersetRetainsIdentityAndTimerEpochDoesNotInventElapsedRest() = fixture { f ->
        command(f) { f.model.refresh() }
        val exercises = ExerciseCatalog.exercises().filter { it.id() in setOf("bench-press", "dumbbell-row") }
        assertEquals(2, exercises.size)
        command(f) { f.model.builderTitle("Original grouped workout") }
        for (exercise in exercises) command(f) {
            assertTrue(f.model.builderPutSlot(null, exercise, GymSlotInput(sets = "2", minimumReps = "6", maximumReps = "8", restSeconds = "90")))
        }
        val id = f.model.state.value.builder.id
        val first = f.model.state.value.builder.slots.first().id()
        command(f) { f.model.builderPair(first) }
        val grouped = f.model.state.value.builder.slots
        assertNotNull(grouped.first().supersetGroup())
        assertEquals(grouped[0].supersetGroup(), grouped[1].supersetGroup())
        command(f) { f.model.saveBuilderFavourite() }
        f.model = f.construct()
        assertEquals(id, f.model.state.value.builder.id)
        assertEquals(grouped, f.model.state.value.builder.slots)
        command(f) { f.model.startBuilder {} }
        val running = command(f) { f.model.restAction("start") }
        assertEquals(90000L, running.restTimer!!.remainingMillis())
        f.millis.addAndGet(30000)
        val paused = command(f) { f.model.restAction("pause") }
        assertEquals(60000L, paused.restTimer!!.remainingMillis())
        command(f) { f.model.restAction("resume") }
        f.epoch.set("new-boot")
        val unknown = command(f) { f.model.refresh() }
        assertEquals(GymRestTimer.Phase.INTERRUPTED, unknown.restTimer!!.phase())
        assertTrue(unknown.view!!.observed()!!.actualSets().isEmpty())
        assertTrue(f.db.query("SELECT * FROM strength_sessions").isEmpty())
        assertTrue(f.alerts.isNotEmpty())
    }
}
