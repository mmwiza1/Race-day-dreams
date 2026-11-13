package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class ActivityLap(
    private var id: String?,
    private val start: Instant?,
    private val end: Instant?,
    private val distanceMetres: Double?,
    private val label: String?,
    private val plannedSegmentId: String?,
) {
    init {
        id = ActivityChecks.required(id, "Lap ID")
        if (start == null) throw NullPointerException("Lap start")
        if (end == null) throw NullPointerException("Lap end")
        require(end.isAfter(start)) { "Lap end must follow start." }
        ActivityChecks.nonNegative(distanceMetres, "Lap distance")
    }

    fun id(): String = id!!

    fun start(): Instant = start!!

    fun end(): Instant = end!!

    fun distanceMetres(): Double? = distanceMetres

    fun label(): String? = label

    fun plannedSegmentId(): String? = plannedSegmentId

    override fun toString(): String =
        "ActivityLap[id=$id, start=$start, end=$end, distanceMetres=$distanceMetres, label=$label, plannedSegmentId=$plannedSegmentId]"
}
