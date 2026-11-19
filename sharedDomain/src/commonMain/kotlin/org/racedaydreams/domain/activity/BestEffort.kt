package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.Duration

/**
 * Candidate calculated from samples. Only QUALIFIED candidates may become automatic PB evidence.
 */
data class BestEffort(
    private val distance: PerformanceDistance?,
    private val duration: Duration?,
    private val startElapsed: Duration?,
    private val endElapsed: Duration?,
    private val quality: SampleQuality?,
    private var reasons: List<String>?,
) {
    init {
        if (distance == null) throw NullPointerException("Effort distance")
        if (duration == null) throw NullPointerException("Effort duration")
        if (startElapsed == null) throw NullPointerException("Effort start")
        if (endElapsed == null) throw NullPointerException("Effort end")
        if (quality == null) throw NullPointerException("Effort quality")
        require(
            !duration.isZero() &&
                !duration.isNegative() &&
                !startElapsed.isNegative() &&
                endElapsed.minus(startElapsed) == duration
        ) {
            "Effort times must be positive and consistent."
        }
        reasons = ActivityChecks.copy(reasons)
    }

    fun distance(): PerformanceDistance = distance!!

    fun duration(): Duration = duration!!

    fun startElapsed(): Duration = startElapsed!!

    fun endElapsed(): Duration = endElapsed!!

    fun quality(): SampleQuality = quality!!

    fun reasons(): List<String> = reasons!!

    override fun toString(): String =
        "BestEffort[distance=$distance, duration=$duration, startElapsed=$startElapsed, endElapsed=$endElapsed, quality=$quality, reasons=$reasons]"
}
