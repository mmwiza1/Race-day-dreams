package org.racedaydreams.domain.portability

import org.racedaydreams.domain.time.Instant

/** Original provider-neutral coordinates; optional sensor values are never zero-filled. */
data class TrackPoint(
    private val time: Instant?,
    private val latitude: Double,
    private val longitude: Double,
    private val altitudeMetres: Double?,
    private val accuracyMetres: Double?,
    private val heartRate: Double?,
    private val cadence: Double?,
) {
    init {
        if (time == null) throw NullPointerException("point time")
        require(
            latitude.isFinite() &&
                latitude >= -90 &&
                latitude <= 90 &&
                longitude.isFinite() &&
                longitude >= -180 &&
                longitude <= 180
        ) {
            "Invalid geographic coordinates."
        }
        finite(altitudeMetres, false)
        finite(accuracyMetres, true)
        finite(heartRate, true)
        finite(cadence, true)
    }

    fun time(): Instant = time!!

    fun latitude(): Double = latitude

    fun longitude(): Double = longitude

    fun altitudeMetres(): Double? = altitudeMetres

    fun accuracyMetres(): Double? = accuracyMetres

    fun heartRate(): Double? = heartRate

    fun cadence(): Double? = cadence

    override fun toString(): String =
        "TrackPoint[time=$time, latitude=$latitude, longitude=$longitude, altitudeMetres=$altitudeMetres, accuracyMetres=$accuracyMetres, heartRate=$heartRate, cadence=$cadence]"

    private fun finite(value: Double?, nonNegative: Boolean) {
        require(value == null || value.isFinite() && (!nonNegative || value >= 0)) {
            "Invalid optional point measurement."
        }
    }
}
