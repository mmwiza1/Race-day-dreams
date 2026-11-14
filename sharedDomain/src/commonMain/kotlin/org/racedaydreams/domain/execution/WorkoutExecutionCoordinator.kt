package org.racedaydreams.domain.execution

import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.recording.recordingSnapshot

/** Session-local orchestration over the one executor; no clock, sensors, persistence or plan. */
class WorkoutExecutionCoordinator(steps: List<WorkoutExecutionEngine.Step>) {
    class Update(
        state: WorkoutExecutionEngine.State?,
        newEvents: List<WorkoutExecutionEngine.Event>,
    ) {
        private val state = state
        private val newEvents = recordingSnapshot(newEvents)

        fun state(): WorkoutExecutionEngine.State? = state

        fun newEvents(): List<WorkoutExecutionEngine.Event> = newEvents

        override fun equals(other: Any?): Boolean =
            other is Update && state == other.state && newEvents == other.newEvents

        override fun hashCode(): Int = 31 * (state?.hashCode() ?: 0) + newEvents.hashCode()

        override fun toString(): String = "Update[state=$state, newEvents=$newEvents]"
    }

    private val execution = WorkoutExecutionEngine(steps)
    private var publishedEvents = 0

    /** Returns each execution event once while retaining its complete snapshot. */
    fun update(metrics: RunMetricsEngine.Status): Update {
        val input =
            WorkoutExecutionEngine.Input(
                metrics.metres(),
                metrics.movingMillis(),
                metrics.elapsedMillis(),
                metrics.secondsPerKm(),
                metrics.hasReliablePace(),
                metrics.paused(),
            )
        val state = execution.update(input)
        val events = execution.events()
        val fresh = ArrayList(events.subList(publishedEvents, events.size))
        publishedEvents = events.size
        return Update(state, fresh)
    }

    fun completeOpen() = execution.completeOpen()

    fun skip() = execution.skip()

    fun finishEarly() = execution.finishEarly()

    fun events(): List<WorkoutExecutionEngine.Event> = execution.events()

    fun steps(): List<WorkoutExecutionEngine.Step> = execution.steps()
}
