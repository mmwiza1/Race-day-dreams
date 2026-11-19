package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.time.*

/** Completed user-entered assessment, optionally linked to an actual recorded activity. */
data class AssessmentResult(
    private val id: String?,
    private val assessment: BaselineAssessment?,
    private val duration: Duration?,
    private val rpe: Double?,
    private val completedAt: Instant?,
    private val activityId: String?,
) {
    init {
        require(id != null && id.trim { it <= ' ' }.isNotEmpty()) { "Assessment ID is required." }
        if (assessment == null) throw NullPointerException("assessment")
        if (duration == null) throw NullPointerException("duration")
        if (completedAt == null) throw NullPointerException("completedAt")
        require(!duration.isNegative() && !duration.isZero() && duration <= Durations.ofHours(48)) {
            "Enter a positive assessment time up to 48 hours."
        }
        require(rpe == null || rpe.isFinite() && rpe >= 0 && rpe <= 10) {
            "Assessment RPE must be between 0 and 10."
        }
    }

    fun id(): String = id!!

    fun assessment(): BaselineAssessment = assessment!!

    fun duration(): Duration = duration!!

    fun rpe(): Double? = rpe

    fun completedAt(): Instant = completedAt!!

    fun activityId(): String? = activityId

    override fun toString(): String =
        "AssessmentResult[id=$id, assessment=$assessment, duration=$duration, rpe=$rpe, completedAt=$completedAt, activityId=$activityId]"
}
