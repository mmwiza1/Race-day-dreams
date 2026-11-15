package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class RacePrediction(
    private val distance: RaceDistance?,
    private val lowerFinishTime: Duration?,
    private val upperFinishTime: Duration?,
    private val assessedAt: Instant?,
    private var evidence: List<String>?,
    private var limitingFactor: String?,
) {
    init {

        if (distance == null) throw NullPointerException("distance")
        if (lowerFinishTime == null) throw NullPointerException("lowerFinishTime")
        if (upperFinishTime == null) throw NullPointerException("upperFinishTime")
        if (assessedAt == null) throw NullPointerException("assessedAt")
        require(
            !lowerFinishTime.isNegative() &&
                !lowerFinishTime.isZero() &&
                upperFinishTime >= lowerFinishTime
        ) {
            "Prediction must be a positive ordered range"
        }
        evidence = DomainChecks.immutableList(evidence)
        limitingFactor = DomainChecks.id(limitingFactor, "limiting factor explanation")
    }

    fun distance(): RaceDistance = distance!!

    fun lowerFinishTime(): Duration = lowerFinishTime!!

    fun upperFinishTime(): Duration = upperFinishTime!!

    fun assessedAt(): Instant = assessedAt!!

    fun evidence(): List<String> = evidence!!

    fun limitingFactor(): String = limitingFactor!!

    override fun toString(): String =
        "RacePrediction[distance=$distance, lowerFinishTime=$lowerFinishTime, upperFinishTime=$upperFinishTime, assessedAt=$assessedAt, evidence=$evidence, limitingFactor=$limitingFactor]"
}
