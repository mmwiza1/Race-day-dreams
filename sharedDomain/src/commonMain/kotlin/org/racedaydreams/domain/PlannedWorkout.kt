package org.racedaydreams.domain

import org.racedaydreams.domain.time.Duration
import org.racedaydreams.domain.time.LocalDate

/** Frozen prescription, kept separate from actual evidence even after an athlete override. */
data class PlannedWorkout(
    private var id: String?,
    private val date: LocalDate?,
    private val category: WorkoutCategory?,
    private val distanceKm: Double?,
    private val duration: Duration?,
    private var segments: List<WorkoutSegment>?,
    private val note: String?,
) {
    init {
        id = DomainChecks.id(id, "workout id")
        if (date == null) throw NullPointerException("date")
        if (category == null) throw NullPointerException("category")
        DomainChecks.nonNegative(distanceKm, "distance")
        DomainChecks.nonNegative(duration, "duration")
        segments = DomainChecks.immutableList(segments)
    }

    fun id(): String = id!!

    fun date(): LocalDate = date!!

    fun category(): WorkoutCategory = category!!

    fun distanceKm(): Double? = distanceKm

    fun duration(): Duration? = duration

    fun segments(): List<WorkoutSegment> = segments!!

    fun note(): String? = note

    override fun toString(): String =
        "PlannedWorkout[id=$id, date=$date, category=$category, distanceKm=$distanceKm, duration=$duration, segments=$segments, note=$note]"
}
