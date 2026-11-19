package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class CompletedWorkout(
    private var id: String?,
    private val plannedWorkoutId: String?,
    private val completedAt: Instant?,
    private val distanceKm: Double?,
    private val duration: Duration?,
    private val averageHeartRate: Double?,
    private val rpe: Double?,
    private val note: String?,
) {
    init {

        id = DomainChecks.id(id, "completed workout id")
        if (plannedWorkoutId != null) DomainChecks.id(plannedWorkoutId, "planned workout id")
        if (completedAt == null) throw NullPointerException("completedAt")
        DomainChecks.nonNegative(distanceKm, "distance")
        DomainChecks.nonNegative(duration, "duration")
        DomainChecks.nonNegative(averageHeartRate, "heart rate")
        DomainChecks.rpe(rpe)
    }

    fun id(): String = id!!

    fun plannedWorkoutId(): String? = plannedWorkoutId

    fun completedAt(): Instant = completedAt!!

    fun distanceKm(): Double? = distanceKm

    fun duration(): Duration? = duration

    fun averageHeartRate(): Double? = averageHeartRate

    fun rpe(): Double? = rpe

    fun note(): String? = note

    override fun toString(): String =
        "CompletedWorkout[id=$id, plannedWorkoutId=$plannedWorkoutId, completedAt=$completedAt, distanceKm=$distanceKm, duration=$duration, averageHeartRate=$averageHeartRate, rpe=$rpe, note=$note]"
}
