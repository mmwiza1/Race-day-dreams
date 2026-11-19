package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained evidence model; missing values remain unknown and snapshots immutable. */
data class RunSplit(
    private val number: Int,
    private val distanceMetres: Double,
    private val duration: Duration?,
    private val averageHeartRate: Double?,
    private val elevationChangeMetres: Double?,
    private val cadence: Double?,
    private val partial: Boolean,
) {
    init {
        require(number >= 1 && distanceMetres.isFinite() && distanceMetres > 0) {
            "A positive split number and distance are required."
        }
        if (duration == null) throw NullPointerException("Split duration")
        require(!duration.isZero() && !duration.isNegative()) { "Split duration must be positive." }
        ActivityChecks.positive(averageHeartRate, "Split heart rate")
        ActivityChecks.nonNegative(cadence, "Split cadence")
        require(elevationChangeMetres == null || elevationChangeMetres.isFinite()) {
            "Elevation change must be finite."
        }
    }

    fun number(): Int = number

    fun distanceMetres(): Double = distanceMetres

    fun duration(): Duration = duration!!

    fun averageHeartRate(): Double? = averageHeartRate

    fun elevationChangeMetres(): Double? = elevationChangeMetres

    fun cadence(): Double? = cadence

    fun partial(): Boolean = partial

    fun secondsPerKilometre(): Double =
        ActivityChecks.seconds(duration()) / (distanceMetres / 1000.0)

    override fun toString(): String =
        "RunSplit[number=$number, distanceMetres=$distanceMetres, duration=$duration, averageHeartRate=$averageHeartRate, elevationChangeMetres=$elevationChangeMetres, cadence=$cadence, partial=$partial]"
}
