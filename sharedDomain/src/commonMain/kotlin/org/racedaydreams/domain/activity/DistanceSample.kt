package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class DistanceSample(
    private val elapsed: Duration?,
    private val distanceMetres: Double,
    private val heartRate: Double?,
    private val cadence: Double?,
    private val elevationMetres: Double?,
    private val discontinuity: Boolean,
) {
    init {
        if (elapsed == null) throw NullPointerException("Sample elapsed time")
        ActivityChecks.duration(elapsed, "Sample time")
        ActivityChecks.nonNegative(distanceMetres, "Sample distance")
        ActivityChecks.positive(heartRate, "Heart rate")
        ActivityChecks.nonNegative(cadence, "Cadence")
        require(elevationMetres == null || elevationMetres.isFinite()) {
            "Elevation must be finite."
        }
    }

    fun elapsed(): Duration = elapsed!!

    fun distanceMetres(): Double = distanceMetres

    fun heartRate(): Double? = heartRate

    fun cadence(): Double? = cadence

    fun elevationMetres(): Double? = elevationMetres

    fun discontinuity(): Boolean = discontinuity

    override fun toString(): String =
        "DistanceSample[elapsed=$elapsed, distanceMetres=$distanceMetres, heartRate=$heartRate, cadence=$cadence, elevationMetres=$elevationMetres, discontinuity=$discontinuity]"
}
