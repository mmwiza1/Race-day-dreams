package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmOverloads
import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class RunExecution
@JvmOverloads
constructor(
    private val activityId: String?,
    private val plannedWorkoutId: String?,
    private val planVersionId: String?,
    private val completedReps: Int?,
    private val recoveriesCompleted: Boolean?,
    private var responseNotes: String?,
    private var steps: List<StepEvidence>? = emptyList(),
) {
    data class StepEvidence(
        private val index: Int,
        private val kind: WorkoutSegment.Kind?,
        private val metres: Double,
        private val activeMillis: Long,
        private val fastPace: Double?,
        private val slowPace: Double?,
        private val completed: Boolean,
        private val reliablePace: Boolean,
    ) {
        init {
            require(
                index in 0..499 &&
                    kind != null &&
                    metres.isFinite() &&
                    metres >= 0 &&
                    activeMillis >= 0 &&
                    (fastPace == null || fastPace.isFinite() && fastPace > 0) &&
                    (slowPace == null || slowPace.isFinite() && slowPace > 0) &&
                    (fastPace == null || slowPace == null || fastPace <= slowPace)
            ) {
                "Invalid step execution evidence"
            }
        }

        fun index(): Int = index

        fun kind(): WorkoutSegment.Kind = kind!!

        fun metres(): Double = metres

        fun activeMillis(): Long = activeMillis

        fun fastPace(): Double? = fastPace

        fun slowPace(): Double? = slowPace

        fun completed(): Boolean = completed

        fun reliablePace(): Boolean = reliablePace

        override fun toString(): String =
            "StepEvidence[index=$index, kind=$kind, metres=$metres, activeMillis=$activeMillis, fastPace=$fastPace, slowPace=$slowPace, completed=$completed, reliablePace=$reliablePace]"
    }

    init {
        steps = Immutable.copyList(steps ?: throw NullPointerException())
        val indices = mutableSetOf<Int>()
        for (step in steps!!) require(indices.add(step.index())) { "Duplicate step evidence" }
        require(steps!!.isEmpty() || plannedWorkoutId != null) {
            "Structured evidence needs a preserved prescription"
        }
        require(steps!!.size <= 500) { "Too many execution steps" }
        require(activityId != null && activityId.trim { it <= ' ' }.isNotEmpty()) {
            "Activity ID is required."
        }
        require((plannedWorkoutId == null) == (planVersionId == null)) {
            "A planned link needs its preserved plan version."
        }
        require(completedReps == null || completedReps in 0..1000) { "Check completed reps." }
        responseNotes = responseNotes ?: ""
        require(responseNotes!!.length <= 10000) { "Response notes are too long." }
    }

    fun activityId(): String = activityId!!

    fun plannedWorkoutId(): String? = plannedWorkoutId

    fun planVersionId(): String? = planVersionId

    fun completedReps(): Int? = completedReps

    fun recoveriesCompleted(): Boolean? = recoveriesCompleted

    fun responseNotes(): String = responseNotes!!

    fun steps(): List<StepEvidence> = steps!!

    override fun toString(): String =
        "RunExecution[activityId=$activityId, plannedWorkoutId=$plannedWorkoutId, planVersionId=$planVersionId, completedReps=$completedReps, recoveriesCompleted=$recoveriesCompleted, responseNotes=$responseNotes, steps=$steps]"
}
