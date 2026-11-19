package org.racedaydreams.domain

import org.racedaydreams.domain.time.*

/** Original evidence/choice contract, shared without changing validation or history. */
data class RaceReadiness(
    private val score: Int,
    private val assessedAt: Instant?,
    private var indicators: List<Indicator>?,
) {

    data class Indicator(
        private var category: String?,
        private val score: Int,
        private var explanation: String?,
    ) {
        init {
            category = DomainChecks.id(category, "readiness category")
            checkScore(score)
            explanation = DomainChecks.id(explanation, "indicator explanation")
        }

        fun category(): String = category!!

        fun score(): Int = score

        fun explanation(): String = explanation!!

        override fun toString(): String =
            "Indicator[category=$category, score=$score, explanation=$explanation]"
    }

    companion object {
        private fun checkScore(value: Int) {
            require(value in 0..100) { "Score must be 0 to 100" }
        }
    }

    init {

        checkScore(score)
        if (assessedAt == null) throw NullPointerException("assessedAt")
        indicators = DomainChecks.immutableList(indicators)
    }

    fun score(): Int = score

    fun assessedAt(): Instant = assessedAt!!

    fun indicators(): List<Indicator> = indicators!!

    override fun toString(): String =
        "RaceReadiness[score=$score, assessedAt=$assessedAt, indicators=$indicators]"
}
