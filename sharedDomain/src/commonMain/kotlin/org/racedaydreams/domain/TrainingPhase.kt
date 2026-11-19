package org.racedaydreams.domain

import org.racedaydreams.domain.time.LocalDate

/** Original inclusive phase bounds; no new allocation or coaching rules. */
data class TrainingPhase(
    private val kind: Kind?,
    private val startDate: LocalDate?,
    private val endDate: LocalDate?,
    private val explanation: String?,
) {
    enum class Kind {
        FOUNDATION,
        ENDURANCE_DEVELOPMENT,
        RACE_SPECIFIC,
        PEAK,
        TAPER,
        RACE,
    }

    init {
        if (kind == null) throw NullPointerException("kind")
        if (startDate == null) throw NullPointerException("startDate")
        if (endDate == null) throw NullPointerException("endDate")
        require(endDate.toEpochDay() >= startDate.toEpochDay()) { "Phase end precedes start" }
    }

    fun kind(): Kind = kind!!

    fun startDate(): LocalDate = startDate!!

    fun endDate(): LocalDate = endDate!!

    fun explanation(): String? = explanation

    override fun toString(): String =
        "TrainingPhase[kind=$kind, startDate=$startDate, endDate=$endDate, explanation=$explanation]"
}
