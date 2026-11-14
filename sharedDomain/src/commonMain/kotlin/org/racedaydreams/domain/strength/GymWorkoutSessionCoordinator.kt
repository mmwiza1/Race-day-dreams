package org.racedaydreams.domain.strength

import org.racedaydreams.domain.time.ZoneOffset

/**
 * Serialized operations around the one execution engine and typed durability. Call from an IO
 * executor: the platform's reentrant monitor covers the entire repository operation. Recreating UI
 * must reuse this coordinator, never implicitly start or recover a workout.
 */
class GymWorkoutSessionCoordinator(repository: GymExecutionRepository?, ticks: TickSource?) {
    fun interface TickSource {
        fun now(): GymExecution.Tick
    }

    enum class Operation {
        START,
        SAVE,
        FINISH,
    }

    /** Exact immutable retry input, including the clock observation before attempted IO. */
    data class Pending(
        private val operation: Operation?,
        private val snapshot: GymExecution?,
        private val expectedRevision: Long,
    ) {
        init {
            if (operation == null) throw NullPointerException("pending operation")
            if (snapshot == null) throw NullPointerException("pending snapshot")
            require(
                if (operation == Operation.START) expectedRevision == 0L
                else expectedRevision >= 1 && expectedRevision != Long.MAX_VALUE
            ) {
                "Invalid pending gym revision."
            }
        }

        fun operation(): Operation = operation!!

        fun snapshot(): GymExecution = snapshot!!

        fun expectedRevision(): Long = expectedRevision

        override fun toString(): String =
            "Pending[operation=$operation, snapshot=$snapshot, expectedRevision=$expectedRevision]"
    }

    /** Accepted means acknowledged durable evidence. Pending is never displayed as completed. */
    data class View(
        private val accepted: GymExecutionRepository.Stored?,
        private val observed: GymExecution?,
        private val pending: Pending?,
    ) {
        fun accepted(): GymExecutionRepository.Stored? = accepted

        fun observed(): GymExecution? = observed

        fun pending(): Pending? = pending

        override fun toString(): String =
            "View[accepted=$accepted, observed=$observed, pending=$pending]"
    }

    private val repository = repository ?: throw NullPointerException("gym repository")
    private val ticks = ticks ?: throw NullPointerException("gym clock")
    private val monitor = GymExecutionMonitor()
    private var engine: GymExecutionEngine? = null
    private var accepted: GymExecutionRepository.Stored? = null
    private var pending: Pending? = null

    fun start(
        id: String?,
        origin: GymExecution.Origin?,
        prescription: GymWorkoutDefinition?,
        source: GymExecution.SourceReference?,
        offset: ZoneOffset?,
    ): View =
        monitor.locked {
            requireNoPending()
            check(engine?.let { unfinished(it.snapshot()) } != true) {
                "Finish or discard the current gym workout first."
            }
            engine = GymExecutionEngine.start(id, origin, prescription, source, offset, ticks.now())
            accepted = null
            persist(Operation.START, engine!!.snapshot(), 0)
        }

    /** Staged evidence is read only after an explicit athlete choice. No bridging downtime. */
    fun recoverExplicitly(id: String): View =
        monitor.locked {
            requireNoPending()
            val current = engine
            check(
                !(current != null &&
                    unfinished(current.snapshot()) &&
                    current.snapshot().id() != id)
            ) {
                "Finish or discard the current gym workout first."
            }
            val stored =
                repository.load(id)
                    ?: throw IllegalArgumentException("The unfinished gym workout is missing.")
            val recovered = GymExecutionEngine.recover(stored.execution(), ticks.now())
            accepted = stored
            engine = recovered
            persist(Operation.SAVE, engine!!.snapshot(), stored.revision())
        }

    fun saveSet(slotId: String?, actual: StrengthSession.SetEntry?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.saveSet(slotId, actual, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun selectExercise(slotId: String?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.selectExercise(slotId, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun skipExercise(slotId: String?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.skipExercise(slotId, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun breakSuperset(slotId: String?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.breakSuperset(slotId, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun addExercises(slots: List<GymWorkoutDefinition.Slot>?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.addExercises(slots, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun replaceExercise(slotId: String?, replacement: StrengthExercise?): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.SAVE,
                engine!!.replaceExercise(slotId, replacement, ticks.now()),
                accepted!!.revision(),
            )
        }

    fun pause(): View =
        monitor.locked {
            requireReady()
            persist(Operation.SAVE, engine!!.pause(ticks.now()), accepted!!.revision())
        }

    fun resume(): View =
        monitor.locked {
            requireReady()
            persist(Operation.SAVE, engine!!.resume(ticks.now()), accepted!!.revision())
        }

    fun finish(sessionRpe: Double?, notes: String?, finishEarly: Boolean): View =
        monitor.locked {
            requireReady()
            persist(
                Operation.FINISH,
                engine!!.finish(ticks.now(), sessionRpe, notes, finishEarly),
                accepted!!.revision(),
            )
        }

    /** Failed pending writes must be retried before discard: they may already have committed. */
    fun discard(): View =
        monitor.locked {
            requireReady()
            persist(Operation.SAVE, engine!!.discard(ticks.now()), accepted!!.revision())
        }

    /** Explicit lifecycle checkpoint, never disk IO for each display frame. */
    fun checkpoint(): View =
        monitor.locked {
            requireReady()
            persist(Operation.SAVE, engine!!.observe(ticks.now()), accepted!!.revision())
        }

    fun observe(): View =
        monitor.locked {
            val current = engine
            if (pending == null && current != null && unfinished(current.snapshot()))
                current.observe(ticks.now())
            view()
        }

    /** No IO/clock observation, including when inspecting a failed write. */
    fun view(): View =
        monitor.locked {
            View(
                accepted,
                if (pending != null) accepted?.execution() else engine?.snapshot(),
                pending,
            )
        }

    /** Retry the exact operation without another clock observation, set mutation or event. */
    fun retryPending(): View =
        monitor.locked {
            val attempt =
                pending ?: throw IllegalStateException("There is no pending gym write to retry.")
            val stored =
                when (attempt.operation()) {
                    Operation.START -> repository.start(attempt.snapshot())
                    Operation.SAVE ->
                        repository.save(attempt.snapshot(), attempt.expectedRevision())
                    Operation.FINISH ->
                        repository.finish(attempt.snapshot(), attempt.expectedRevision())
                }
            val validRevision =
                stored != null &&
                    (if (attempt.operation() == Operation.START) stored.revision() == 1L
                    else
                        stored.revision() == attempt.expectedRevision() ||
                            stored.revision() == attempt.expectedRevision() + 1)
            check(validRevision && stored!!.execution() == attempt.snapshot()) {
                "The gym write acknowledgement does not match its evidence."
            }
            accepted = stored
            pending = null
            view()
        }

    private fun persist(
        operation: Operation,
        snapshot: GymExecution,
        expectedRevision: Long,
    ): View {
        pending = Pending(operation, snapshot, expectedRevision)
        return retryPending()
    }

    private fun requireNoPending() {
        check(pending == null) { "Retry the pending gym write before another action." }
    }

    private fun requireReady() {
        requireNoPending()
        check(engine != null && accepted != null) {
            "Start or explicitly recover a gym workout first."
        }
        check(accepted!!.revision() != Long.MAX_VALUE) {
            "The saved gym revision cannot advance further."
        }
    }

    private fun unfinished(execution: GymExecution): Boolean =
        execution.status() != GymExecution.Status.FINISHED &&
            execution.status() != GymExecution.Status.DISCARDED
}

/** Same reentrant serialization boundary on JVM and Apple; no second execution engine. */
internal expect class GymExecutionMonitor() {
    fun <T> locked(action: () -> T): T
}
