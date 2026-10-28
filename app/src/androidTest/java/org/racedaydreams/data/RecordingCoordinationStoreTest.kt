package org.racedaydreams.data

import android.test.AndroidTestCase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.domain.WorkoutSegment
import org.racedaydreams.domain.execution.WorkoutExecutionCoordinator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.recording.RecordingBatch
import org.racedaydreams.recording.RecordingRepository
import org.racedaydreams.recording.RecordingSessionCoordinator
import org.racedaydreams.recording.SqliteRecordingRepository

/** Real staging/finalisation/backup around the service's extracted execution projection. */
class RecordingCoordinationStoreTest : AndroidTestCase() {
    override fun setUp() {
        super.setUp()
        SqliteUserPreferencesRepository(context).clear()
    }

    override fun tearDown() {
        SqliteUserPreferencesRepository(context).clear()
        super.tearDown()
    }

    fun testPausedPartialExecutionRetainsOriginalStepEvidenceThroughCompleteBackup() = runBlocking {
        val steps =
            listOf(
                step("Easy warm-up", WorkoutSegment.Kind.WARM_UP, 100.0),
                step("Controlled work", WorkoutSegment.Kind.RUN, 100.0),
                step("Cool-down", WorkoutSegment.Kind.COOL_DOWN, 100.0),
            )
        val rows = JSONArray()
        steps.forEach { rows.put(SupportStore.step(it)) }
        val contextSnapshot =
            JSONObject().put("version", 1).put("mode", "custom").put("steps", rows)
        val wall = Instant.parse("2026-10-01T00:00:00Z")
        val execution = WorkoutExecutionCoordinator(steps)
        val store = RecordingStore(context)
        store.start("coordination", wall, 1000, contextSnapshot)
        val clock = AtomicLong(1000)
        val sqlite = SqliteRecordingRepository(store)
        var loseFirstAcknowledgement = true
        val repository =
            object : RecordingRepository by sqlite {
                override fun append(batch: RecordingBatch) {
                    sqlite.append(batch)
                    if (loseFirstAcknowledgement) {
                        loseFirstAcknowledgement = false
                        error("Committed append acknowledgement interrupted")
                    }
                }
            }
        val recording =
            RecordingSessionCoordinator(
                store.unfinished()!!,
                repository,
                Dispatchers.IO,
                clock::get,
                { contextSnapshot },
                { execution.events() },
            )
        val metrics = recording.engine

        fun publish(now: Long) {
            execution.update(metrics.status(now)).newEvents().forEach {
                metrics.sensorEvent(
                    it.type(),
                    it.step().toDouble(),
                    now,
                    wall.plusMillis(now - 1000),
                )
            }
        }

        fun snapshot(): JSONObject {
            val events = JSONArray()
            execution.events().forEach {
                events.put(
                    JSONObject()
                        .put("type", it.type())
                        .put("step", it.step())
                        .put("metres", it.metres())
                        .put("active", it.activeMillis())
                )
            }
            return JSONObject(contextSnapshot.toString()).put("events", events)
        }

        for (second in 0..100) {
            val now = 1000 + second * 1000L
            val at = wall.plusSeconds(second.toLong())
            if (second == 35) metrics.pause(now, at)
            if (second == 65) metrics.resume(now, at)
            metrics.accept(
                RunMetricsEngine.Fix(at, now, 0.0, second * .00003, 3.0, null, null, 3.0)
            )
            publish(now)
            if (second % 5 == 0) {
                clock.set(now)
                try {
                    recording.flush()
                } catch (failure: IllegalStateException) {
                    if (second != 0) throw failure
                }
            }
        }
        execution.finishEarly()
        publish(101000)
        metrics.event("FINISH", 101000, wall.plusSeconds(100))
        clock.set(101000)
        recording.flush()
        assertFalse(
            "Lost acknowledgement must follow a real committed append",
            loseFirstAcknowledgement,
        )
        val expectedEvents = snapshot().getJSONArray("events").toString()
        assertEquals("WORKOUT_FINISHED_EARLY", execution.events().last().type())
        val id = recording.finish()
        val activity = FeatureStore(context).activityDetail(id)
        assertEquals(100000L, activity.elapsedTime().toMillis())
        assertEquals(70000L, activity.movingTime()!!.toMillis())
        assertEquals(metrics.pointCount(), activity.samples().size)
        assertTrue(activity.distanceMetres()!! < 300)
        assertTrue(
            "Custom actual must not silently complete a plan",
            CoachingStore(context).responses().isEmpty(),
        )
        val retained = SupportStore(context).get("execution", "coordination")!!
        assertEquals(expectedEvents, retained.getJSONArray("events").toString())
        assertEquals(id, retained.getString("activityId"))
        assertFalse(retained.getBoolean("interrupted"))

        val complete = ByteArrayOutputStream()
        CompleteBackup(context).exportTo(complete)
        SqliteUserPreferencesRepository(context).clear()
        CompleteBackup(context).restoreFrom(ByteArrayInputStream(complete.toByteArray()))
        assertEquals(activity, FeatureStore(context).activityDetail(id))
        assertEquals(
            expectedEvents,
            SupportStore(context)
                .get("execution", "coordination")!!
                .getJSONArray("events")
                .toString(),
        )
        assertEquals(metrics.pointCount(), RecordingStore(context).points("coordination").size)
    }

    private fun step(name: String, kind: WorkoutSegment.Kind, metres: Double) =
        WorkoutExecutionEngine.Step(name, kind, metres, null, null, null, null)
}
