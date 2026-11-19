package org.racedaydreams.domain.reminders

import org.racedaydreams.domain.time.*

/** Original versioned prescription/notification evidence; never actual workout completion. */
data class EveningReminder(
    private val workoutId: String?,
    private val revision: Long,
    private val workoutDate: LocalDate?,
    private val triggerAt: Instant?,
    private val title: String?,
    private val message: String?,
) {
    init {
        if (workoutId == null) throw NullPointerException("workout id")
        if (workoutDate == null) throw NullPointerException("workout date")
        if (triggerAt == null) throw NullPointerException("trigger time")
        if (title == null) throw NullPointerException("notification title")
        if (message == null) throw NullPointerException("notification message")
        require(workoutId.trim { it <= ' ' }.isNotEmpty() && revision >= 1) {
            "Invalid reminder identity."
        }
    }

    fun workoutId(): String = workoutId!!

    fun revision(): Long = revision

    fun workoutDate(): LocalDate = workoutDate!!

    fun triggerAt(): Instant = triggerAt!!

    fun title(): String = title!!

    fun message(): String = message!!

    override fun toString(): String =
        "EveningReminder[workoutId=$workoutId, revision=$revision, workoutDate=$workoutDate, triggerAt=$triggerAt, title=$title, message=$message]"
}
