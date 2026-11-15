package org.racedaydreams.domain.portability

import org.racedaydreams.domain.PlannedWorkout

/** Original transport value shares the exact prescription and revision; never an actual workout. */
data class StructuredWorkoutDelivery(
    private val workout: PlannedWorkout?,
    private val revision: Long,
) {
    init {
        if (workout == null) throw NullPointerException("shared prescription")
        require(revision >= 1) { "A prescription revision is required." }
    }

    fun workout(): PlannedWorkout = workout!!

    fun revision(): Long = revision

    override fun toString(): String =
        "StructuredWorkoutDelivery[workout=$workout, revision=$revision]"
}
