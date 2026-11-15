package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class RecoveryMetrics(
    private val sleep: Duration?,
    private val restingHeartRate: Double?,
    private val fatigue: Double?,
    private val soreness: Double?,
    private val notes: String?,
) {
    init {

        DomainChecks.nonNegative(sleep, "sleep")
        DomainChecks.nonNegative(restingHeartRate, "resting heart rate")
        DomainChecks.rpe(fatigue)
        DomainChecks.rpe(soreness)
    }

    fun sleep(): Duration? = sleep

    fun restingHeartRate(): Double? = restingHeartRate

    fun fatigue(): Double? = fatigue

    fun soreness(): Double? = soreness

    fun notes(): String? = notes

    override fun toString(): String =
        "RecoveryMetrics[sleep=$sleep, restingHeartRate=$restingHeartRate, fatigue=$fatigue, soreness=$soreness, notes=$notes]"
}
