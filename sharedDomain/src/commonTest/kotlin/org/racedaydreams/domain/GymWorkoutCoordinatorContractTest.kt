package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

class GymWorkoutCoordinatorContractTest {
    private val now = Instants.ofEpochSecond(1793102400)
    private var time = 0L
    private var clockCalls = 0
    private var epoch = "boot"

    private fun coordinator(repo: Store) =
        GymWorkoutSessionCoordinator(repo) {
            clockCalls++
            GymExecution.Tick(epoch, time, now)
        }

    private fun start(c: GymWorkoutSessionCoordinator) =
        c.start("workout", GymExecution.Origin.FREE, null, null, Offsets.ofTotalSeconds(0))

    private fun add(c: GymWorkoutSessionCoordinator) =
        c.addExercises(
            listOf(
                GymWorkoutDefinition.Slot(
                    "a",
                    ExerciseCatalog.definition("bench-press")!!.exercise(),
                    null,
                    null,
                )
            )
        )

    private fun actual(id: String) = StrengthSession.SetEntry(id, 5, 80.0, 7.0, false)

    private class Store : GymExecutionRepository {
        var stored: GymExecutionRepository.Stored? = null
        var before = false
        var after = false
        var wrong = false
        var writes = 0
        var reads = 0
        var duringWrite: (() -> Unit)? = null

        private fun write(snapshot: GymExecution, expected: Long): GymExecutionRepository.Stored? {
            writes++
            duringWrite?.invoke()
            if (before) {
                before = false
                throw IllegalStateException("before commit")
            }
            if (stored?.execution() != snapshot) {
                check((stored?.revision() ?: 0L) == expected)
                stored = GymExecutionRepository.Stored(snapshot, expected + 1)
            }
            if (after) {
                after = false
                throw IllegalStateException("lost acknowledgement")
            }
            return if (wrong) null else stored
        }

        override fun start(initial: GymExecution) = write(initial, 0)

        override fun load(id: String): GymExecutionRepository.Stored? {
            reads++
            return stored?.takeIf { it.execution().id() == id }
        }

        override fun save(snapshot: GymExecution, expectedRevision: Long) =
            write(snapshot, expectedRevision)

        override fun finish(finished: GymExecution, expectedRevision: Long) =
            write(finished, expectedRevision)

        override fun unfinished(): List<GymExecutionRepository.Summary> = emptyList()

        override fun saveWorkout(
            workout: GymWorkoutDefinition,
            source: GymExecution.SourceReference?,
        ) = error("not used")

        override fun workout(id: String, revision: Int): GymWorkoutDefinition? = null

        override fun preferences(): List<GymExecutionRepository.Preference> = emptyList()

        override fun savePreference(preference: GymExecutionRepository.Preference) =
            error("not used")
    }

    @Test
    fun failedStartCannotBePresentedAsAcceptedAndRetriesUseExactClock() {
        for (afterCommit in listOf(false, true)) {
            clockCalls = 0
            val repo = Store().apply { if (afterCommit) after = true else before = true }
            val c = coordinator(repo)
            assertNull(c.observe().observed())
            assertEquals(0, clockCalls)
            assertFailsWith<IllegalStateException> { start(c) }
            val pending = c.view().pending()!!
            assertEquals(GymWorkoutSessionCoordinator.Operation.START, pending.operation())
            assertNull(c.view().accepted())
            assertNull(c.observe().observed())
            assertEquals(1, clockCalls)
            assertFailsWith<IllegalStateException> { c.discard() }
            time = 99999
            assertEquals(pending.snapshot(), c.retryPending().accepted()!!.execution())
            assertEquals(1, clockCalls)
            assertEquals(1L, repo.stored!!.revision())
            assertNull(c.view().pending())
        }
    }

    @Test
    fun failedSetAndFinishKeepAcceptedEvidenceUntilExactAcknowledgement() {
        val repo = Store()
        val c = coordinator(repo)
        start(c)
        add(c)
        val accepted = c.view().accepted()
        time = 1000
        repo.after = true
        assertFailsWith<IllegalStateException> { c.saveSet("a", actual("set")) }
        assertEquals(accepted, c.view().accepted())
        assertTrue(c.observe().observed()!!.actualSets().isEmpty())
        val pendingSet = c.view().pending()!!.snapshot()
        val calls = clockCalls
        time = 9000
        c.retryPending()
        assertEquals(calls, clockCalls)
        assertEquals(pendingSet, c.view().observed())
        assertEquals(1, repo.stored!!.execution().actualSets().size)
        repo.after = true
        time = 10000
        assertFailsWith<IllegalStateException> { c.finish(null, null, false) }
        assertNull(c.view().observed()!!.completedSession())
        val pendingFinish = c.view().pending()!!.snapshot()
        assertNotNull(pendingFinish.completedSession())
        val callsBeforeRetry = clockCalls
        time = 999999
        c.retryPending()
        assertEquals(callsBeforeRetry, clockCalls)
        assertEquals(pendingFinish, c.view().accepted()!!.execution())
        assertEquals(
            1,
            repo.stored!!.execution().completedSession()!!.exercises().single().sets().size,
        )
    }

    @Test
    fun newCoordinatorDoesNotRecoverUntilAskedAndDowntimeIsUnknown() {
        val repo = Store()
        val first = coordinator(repo)
        start(first)
        add(first)
        time = 1000
        first.saveSet("a", actual("set"))
        val calls = clockCalls
        epoch = "new"
        time = 100000
        val second = coordinator(repo)
        assertNull(second.observe().observed())
        assertEquals(calls, clockCalls)
        assertEquals(0, repo.reads)
        val recovered = second.recoverExplicitly("workout").accepted()!!.execution()
        assertEquals(GymExecution.Status.RECOVERY_REQUIRED, recovered.status())
        assertEquals(1000L, recovered.activeMillis())
        assertFailsWith<IllegalArgumentException> { second.saveSet("a", actual("blocked")) }
        second.resume()
        time += 1000
        assertEquals(
            2000L,
            second.finish(null, null, false).accepted()!!.execution().activeMillis(),
        )
    }

    @Test
    fun invalidAcknowledgementsRetainPendingAndMonitorIsReentrantAfterFailure() {
        val repo = Store()
        val c = coordinator(repo)
        // A platform repository may inspect the accepted view while holding the same operation
        // lock.
        repo.duringWrite = { assertNotNull(c.view().pending()) }
        repo.wrong = true
        assertFailsWith<IllegalStateException> { start(c) }
        assertNotNull(c.view().pending())
        assertNull(c.view().accepted())
        assertFailsWith<IllegalStateException> { c.checkpoint() }
        repo.wrong = false
        assertNotNull(c.retryPending().accepted())
        assertNull(c.view().pending())
        assertFailsWith<IllegalStateException> { c.retryPending() }
        assertEquals(GymExecution.Status.DISCARDED, c.discard().accepted()!!.execution().status())
    }
}
