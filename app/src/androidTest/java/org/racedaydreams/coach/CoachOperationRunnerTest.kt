package org.racedaydreams.coach

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.coach.application.CoachOperationRunner

/** Feature commands must share one serialised publication boundary across UI lifecycles. */
@RunWith(AndroidJUnit4::class)
class CoachOperationRunnerTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun stopScope() {
        instrumentation.runOnMainSync { scope.cancel() }
    }

    @Test
    fun concurrentFeatureCommandsSerialiseAndCallbacksSeeTheirPublishedSnapshotOnMain() {
        val retained = AtomicInteger(0)
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondStarted = AtomicBoolean(false)
        val callbacksOnMain = AtomicBoolean(true)
        val snapshots = Collections.synchronizedList(mutableListOf<String>())
        val completed = CountDownLatch(2)
        val runner =
            CoachOperationRunner(
                scope,
                {
                    withContext(Dispatchers.IO) {
                        CoachUiState(loading = false, calendarView = retained.get().toString())
                    }
                },
            )
        fun observe() {
            callbacksOnMain.compareAndSet(true, Looper.myLooper() == Looper.getMainLooper())
            snapshots.add(runner.current.calendarView)
            completed.countDown()
        }

        instrumentation.runOnMainSync {
            runner.submit(onSuccess = ::observe) {
                firstStarted.complete(Unit)
                releaseFirst.await()
                retained.incrementAndGet()
            }
            runner.submit(onSuccess = ::observe) {
                secondStarted.set(true)
                retained.incrementAndGet()
            }
        }
        runBlocking { withTimeout(20_000) { firstStarted.await() } }
        assertFalse(
            "A second feature must not enter a command while the first holds its boundary",
            secondStarted.get(),
        )
        assertEquals(0, retained.get())
        releaseFirst.complete(Unit)
        assertTrue("Both feature callbacks must arrive", completed.await(20, TimeUnit.SECONDS))
        assertTrue(callbacksOnMain.get())
        assertEquals(listOf("1", "2"), snapshots.toList())
        assertEquals("2", runner.current.calendarView)
        assertFalse(runner.current.busy)
        assertNull(runner.current.error)
    }

    @Test
    fun failedFeatureCommandPreservesObservationsAndInputThenAllowsAnotherCommand() {
        val retained = AtomicInteger(0)
        val failed = CountDownLatch(1)
        val succeeded = CountDownLatch(1)
        val successWasCalled = AtomicBoolean(false)
        val failureOnMain = AtomicBoolean(false)
        val runner =
            CoachOperationRunner(
                scope,
                {
                    CoachUiState(
                        loading = false,
                        calendarView = if (retained.get() == 0) "Week" else "Month",
                        draft = "Unfinished input",
                    )
                },
            )
        instrumentation.runOnMainSync { runner.refresh() }
        runBlocking { withTimeout(20_000) { runner.state.first { !it.loading && !it.busy } } }

        instrumentation.runOnMainSync {
            runner.submit(
                onSuccess = { successWasCalled.set(true) },
                onFailure = {
                    failureOnMain.set(Looper.myLooper() == Looper.getMainLooper())
                    failed.countDown()
                },
            ) {
                throw IllegalArgumentException(
                    "The selected prescription changed. Review it again."
                )
            }
        }
        assertTrue(failed.await(20, TimeUnit.SECONDS))
        runBlocking { withTimeout(20_000) { runner.state.first { it.error != null } } }
        assertFalse(successWasCalled.get())
        assertTrue(failureOnMain.get())
        assertEquals("Week", runner.current.calendarView)
        assertEquals("Unfinished input", runner.current.draft)
        assertEquals("The selected prescription changed. Review it again.", runner.current.error)
        assertFalse(runner.current.busy)

        instrumentation.runOnMainSync {
            runner.submit(onSuccess = { succeeded.countDown() }) { retained.incrementAndGet() }
        }
        assertTrue(succeeded.await(20, TimeUnit.SECONDS))
        assertEquals("Month", runner.current.calendarView)
        assertNull(runner.current.error)
    }

    @Test
    fun failedSnapshotRefreshDoesNotReplaceRetainedEvidenceOrCallSuccess() {
        val failed = CountDownLatch(1)
        val readFails = AtomicBoolean(false)
        val successWasCalled = AtomicBoolean(false)
        val runner =
            CoachOperationRunner(
                scope,
                {
                    if (readFails.get()) throw IllegalStateException("Synthetic unreadable store")
                    CoachUiState(
                        loading = false,
                        calendarView = "Week",
                        draft = "Retain this input",
                    )
                },
            )
        instrumentation.runOnMainSync { runner.refresh() }
        runBlocking { withTimeout(20_000) { runner.state.first { !it.loading && !it.busy } } }
        readFails.set(true)
        instrumentation.runOnMainSync {
            runner.submit(
                onSuccess = { successWasCalled.set(true) },
                onFailure = { failed.countDown() },
            ) {}
        }
        assertTrue(failed.await(20, TimeUnit.SECONDS))
        runBlocking { withTimeout(20_000) { runner.state.first { it.error != null } } }
        assertFalse(successWasCalled.get())
        assertEquals("Week", runner.current.calendarView)
        assertEquals("Retain this input", runner.current.draft)
        assertFalse(runner.current.busy)
        assertTrue(runner.current.error!!.contains("left in place"))
    }

    /** Reads the current data on IO, counting reads; [gate] can hold a read in progress. */
    private class CountingReads {
        val data = AtomicInteger(0)
        val reads = AtomicInteger(0)
        @Volatile var gate: CompletableDeferred<Unit>? = null
        val reading = CompletableDeferred<Unit>()

        suspend fun read(): CoachUiState {
            reads.incrementAndGet()
            // What this read observed; a change after this point needs another read.
            val observed = data.get()
            reading.complete(Unit)
            gate?.await()
            return withContext(Dispatchers.IO) {
                CoachUiState(loading = false, calendarView = observed.toString())
            }
        }
    }

    private fun CoachOperationRunner.settle(reads: CountingReads, expected: Int) {
        runBlocking {
            withTimeout(20_000) { state.first { !it.busy && reads.reads.get() >= expected } }
        }
    }

    @Test
    fun overlappingResumeRefreshesShareOneQueuedReadThatSeesEveryEarlierChange() {
        val reads = CountingReads()
        val runner = CoachOperationRunner(scope, reads::read)
        val release = CompletableDeferred<Unit>()
        val holding = CompletableDeferred<Unit>()
        instrumentation.runOnMainSync {
            // A feature command holds the boundary, as a save or import does.
            runner.submit {
                holding.complete(Unit)
                release.await()
                reads.data.incrementAndGet()
            }
        }
        runBlocking { withTimeout(20_000) { holding.await() } }
        instrumentation.runOnMainSync {
            // Resume, a permission result and screen entry ask together, around more changes.
            runner.refresh(merge = true)
            reads.data.incrementAndGet()
            runner.refresh(merge = true)
            runner.refresh(merge = true)
            reads.data.incrementAndGet()
            runner.refresh(merge = true)
        }
        release.complete(Unit)
        runner.settle(reads, 2)
        Thread.sleep(500)
        // One read for the command and one for all four refreshes, instead of five.
        assertEquals(2, reads.reads.get())
        assertEquals("3", runner.current.calendarView)
    }

    @Test
    fun aChangeDuringAnInProgressRefreshAlwaysCausesAnotherRead() {
        val reads = CountingReads()
        val runner = CoachOperationRunner(scope, reads::read)
        val gate = CompletableDeferred<Unit>()
        reads.gate = gate
        instrumentation.runOnMainSync { runner.refresh(merge = true) }
        runBlocking { withTimeout(20_000) { reads.reading.await() } }
        // The first refresh is already reading; a run is then saved and the app asks again.
        reads.data.incrementAndGet()
        instrumentation.runOnMainSync { runner.refresh(merge = true) }
        reads.gate = null
        gate.complete(Unit)
        // The in-progress read saw the old data; the later request reads again and sees the run.
        runner.settle(reads, 2)
        runBlocking { withTimeout(20_000) { runner.state.first { it.calendarView == "1" } } }
        assertEquals(2, reads.reads.get())
    }

    @Test
    fun refreshesThatImportOrAreNotMergeableAreNeverDropped() {
        val reads = CountingReads()
        val runner = CoachOperationRunner(scope, reads::read)
        val imports = AtomicInteger(0)
        val release = CompletableDeferred<Unit>()
        val holding = CompletableDeferred<Unit>()
        instrumentation.runOnMainSync {
            runner.submit {
                holding.complete(Unit)
                release.await()
            }
        }
        runBlocking { withTimeout(20_000) { holding.await() } }
        instrumentation.runOnMainSync {
            runner.refresh(merge = true)
            runner.refresh({ imports.incrementAndGet() })
            runner.refresh()
            runner.refresh(merge = true)
        }
        release.complete(Unit)
        runner.settle(reads, 4)
        Thread.sleep(500)
        assertEquals(1, imports.get())
        // The command, the merged pair, the import refresh and the plain refresh.
        assertEquals(4, reads.reads.get())
    }
}
