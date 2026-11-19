package org.racedaydreams.domain.reminders

import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.time.*

/** Original versioned prescription/notification evidence; never actual workout completion. */
data class ReminderPlanEntry(
    private val workout: PlannedWorkout?,
    private val revision: Long,
    private val status: Status?,
) {
    enum class Status {
        PLANNED,
        COMPLETED,
        CANCELLED,
        SKIPPED,
    }

    init {
        if (workout == null) throw NullPointerException("workout")
        if (status == null) throw NullPointerException("workout status")
        require(revision >= 1) { "A workout revision must be positive." }
    }

    fun workout(): PlannedWorkout = workout!!

    fun revision(): Long = revision

    fun status(): Status = status!!

    override fun toString(): String =
        "ReminderPlanEntry[workout=$workout, revision=$revision, status=$status]"
}
