package org.racedaydreams.domain

import org.racedaydreams.domain.time.Duration

/** Nullable prescriptions are unavailable guidance; no heart rate is required or invented. */
data class WorkoutSegment(
    private val kind: Kind?,
    private val distanceKm: Double?,
    private val duration: Duration?,
    private val minimumSecondsPerKm: Double?,
    private val maximumSecondsPerKm: Double?,
    private val heartRateZone: Int?,
    private val rpe: Double?,
    private val guidance: String?,
) {
    enum class Kind {
        WARM_UP,
        RUN,
        WALK,
        RECOVERY,
        COOL_DOWN,
    }

    init {
        if (kind == null) throw NullPointerException("kind")
        DomainChecks.nonNegative(distanceKm, "distance")
        DomainChecks.nonNegative(duration, "duration")
        DomainChecks.nonNegative(minimumSecondsPerKm, "minimum pace")
        DomainChecks.nonNegative(maximumSecondsPerKm, "maximum pace")
        require(
            minimumSecondsPerKm == null ||
                maximumSecondsPerKm == null ||
                minimumSecondsPerKm <= maximumSecondsPerKm
        ) {
            "Pace range must be ordered"
        }
        require(heartRateZone == null || heartRateZone in 1..5) { "HR zone must be 1 to 5" }
        DomainChecks.rpe(rpe)
    }

    fun kind(): Kind = kind!!

    fun distanceKm(): Double? = distanceKm

    fun duration(): Duration? = duration

    fun minimumSecondsPerKm(): Double? = minimumSecondsPerKm

    fun maximumSecondsPerKm(): Double? = maximumSecondsPerKm

    fun heartRateZone(): Int? = heartRateZone

    fun rpe(): Double? = rpe

    fun guidance(): String? = guidance

    override fun toString(): String =
        "WorkoutSegment[kind=$kind, distanceKm=$distanceKm, duration=$duration, minimumSecondsPerKm=$minimumSecondsPerKm, maximumSecondsPerKm=$maximumSecondsPerKm, heartRateZone=$heartRateZone, rpe=$rpe, guidance=$guidance]"
}
