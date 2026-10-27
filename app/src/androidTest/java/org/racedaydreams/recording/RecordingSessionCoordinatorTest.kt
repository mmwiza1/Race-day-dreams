package org.racedaydreams.recording

import android.os.Looper
import android.test.AndroidTestCase
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.domain.recording.RunMetricsEngine

class RecordingSessionCoordinatorTest : AndroidTestCase() {
    private val wall = Instant.parse("2026-10-01T00:00:00Z")
    private val session get() = RecordingStore.Session("session", "ACTIVE", wall, 1000, 1000)

    private class Repository : RecordingRepository {
        val attempts = mutableListOf<RecordingBatch>()
        var appendAction: (RecordingBatch) -> Unit = {}
        var retained: RecordingStore.Session? = null
        var finished: Triple<RecordingStore.Session, Long?, Boolean>? = null
        var finishOnMain: Boolean? = null

        override fun append(batch: RecordingBatch) {
            attempts.add(batch)
            appendAction(batch)
        }

        override fun unfinished() = retained

        override fun finish(session: RecordingStore.Session, movingMillis: Long?, recovered: Boolean): String {
            finishOnMain = Looper.myLooper() == Looper.getMainLooper()
            finished = Triple(session, movingMillis, recovered)
            return "rdd:${session.id}"
        }
    }

    fun testFailedStagingLeavesBothCursorsAtLastCommittedBatch() = runBlocking {
        val repository = Repository()
        val clock = AtomicLong(2000)
        val recording = RecordingSessionCoordinator(session, repository, Dispatchers.IO, clock::get)
        withContext(Dispatchers.Main) {
            addPoint(recording, 1000)
            addPoint(recording, 2000)
        }
        repository.appendAction = { error("Storage temporarily unavailable") }
        try {
            withContext(Dispatchers.Main) { recording.flush() }
            fail("Failed batch acknowledged")
        } catch (_: IllegalStateException) {
            // The next successful flush must include all unacknowledged evidence.
        }
        repository.appendAction = {}
        withContext(Dispatchers.Main) {
            addPoint(recording, 3000)
            clock.set(3000)
            recording.flush()
            addPoint(recording, 4000)
            clock.set(4000)
            recording.flush()
        }
        assertEquals(3, repository.attempts.size)
        val failed = repository.attempts[0]
        val retry = repository.attempts[1]
        val next = repository.attempts[2]
        assertEquals(0, failed.fromPoint)
        assertEquals(0, retry.fromPoint)
        assertEquals(0, retry.fromEvent)
        assertEquals(3, retry.points.size)
        assertEquals(failed.events, retry.events)
        assertEquals(3, next.fromPoint)
        assertEquals(retry.events.size, next.fromEvent)
        assertEquals(listOf(4000L), next.points.map { it.fix().monotonicMillis() })
    }

    fun testInFlightBatchIsImmutableAndConcurrentFlushWaitsForItsCommit() = runBlocking {
        val repository = Repository()
        val clock = AtomicLong(2000)
        val source = JSONObject().put("label", "preserved")
        val snapshotThreads = mutableListOf<Boolean>()
        val recording = RecordingSessionCoordinator(
            session, repository, Dispatchers.IO, clock::get,
            { snapshotThreads.add(Looper.myLooper() == Looper.getMainLooper()); source },
        )
        withContext(Dispatchers.Main) {
            addPoint(recording, 1000)
            addPoint(recording, 2000)
        }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        repository.appendAction = {
            assertFalse("Disk append must not run on Main", Looper.myLooper() == Looper.getMainLooper())
            if (it.fromPoint == 0) {
                entered.countDown()
                assertTrue("Test did not release the staged append", release.await(10, TimeUnit.SECONDS))
            }
        }
        val first = launch(Dispatchers.Main) { recording.flush() }
        var second: kotlinx.coroutines.Job? = null
        try {
            assertTrue("First append never started", entered.await(10, TimeUnit.SECONDS))
            withContext(Dispatchers.Main) {
                addPoint(recording, 3000)
                clock.set(3000)
                source.put("label", "next batch")
            }
            val secondStarted = CompletableDeferred<Unit>()
            second = launch(Dispatchers.Main) {
                secondStarted.complete(Unit)
                recording.flush()
            }
            secondStarted.await()
            assertEquals(1, repository.attempts.size)
            assertEquals(2, repository.attempts.first().points.size)
            assertEquals("preserved", repository.attempts.first().executionContext!!.getString("label"))
        } finally {
            release.countDown()
            first.join()
            second?.join()
        }
        assertEquals(2, repository.attempts.size)
        val next = repository.attempts.last()
        assertEquals(2, next.fromPoint)
        assertEquals(listOf(3000L), next.points.map { it.fix().monotonicMillis() })
        assertEquals("next batch", next.executionContext!!.getString("label"))
        assertEquals(listOf(true, true), snapshotThreads)
    }

    fun testPausedBatchAndFinalizationKeepElapsedAndActiveSemantics() = runBlocking {
        val repository = Repository()
        val clock = AtomicLong(62000)
        val clockOnMain = mutableListOf<Boolean>()
        val recording = RecordingSessionCoordinator(session, repository, Dispatchers.IO, {
            clockOnMain.add(Looper.myLooper() == Looper.getMainLooper())
            clock.get()
        })
        withContext(Dispatchers.Main) {
            addPoint(recording, 1000)
            addPoint(recording, 2000)
            recording.engine.pause(3000, wall.plusSeconds(2))
            recording.flush()
        }
        val paused = repository.attempts.single()
        assertEquals("PAUSED", paused.state)
        assertEquals(62000L, paused.lastMonotonic)
        assertEquals(61000L, recording.engine.status(62000).elapsedMillis())
        assertEquals(2000L, recording.engine.status(62000).movingMillis())
        repository.retained = session.copy(state = "PAUSED", last = 62000)
        assertEquals("rdd:session", withContext(Dispatchers.Main) { recording.finish() })
        assertEquals(Triple(repository.retained, 2000L, false), repository.finished)
        assertEquals(listOf(true, true), clockOnMain)
        assertEquals(false, repository.finishOnMain)
    }

    fun testMissingStageCannotBeFinalizedAndDoesNotClearEngineEvidence() = runBlocking {
        val repository = Repository()
        val recording = RecordingSessionCoordinator(session, repository, Dispatchers.IO, { 2000 })
        withContext(Dispatchers.Main) {
            addPoint(recording, 1000)
            addPoint(recording, 2000)
        }
        try {
            withContext(Dispatchers.Main) { recording.finish() }
            fail("Missing stage finalized")
        } catch (_: IllegalStateException) {
            assertNull(repository.finished)
            assertEquals(2, recording.engine.pointCount())
        }
    }

    private fun addPoint(recording: RecordingSessionCoordinator, now: Long) {
        recording.engine.accept(
            RunMetricsEngine.Fix(wall.plusMillis(now - 1000), now, 0.0,
                (now - 1000) * .00000003, 3.0, null, null, 3.0)
        )
    }
}
