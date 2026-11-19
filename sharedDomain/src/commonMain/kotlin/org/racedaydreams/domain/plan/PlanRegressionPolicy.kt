package org.racedaydreams.domain.plan

/** Existing explicit fixture acceptance budgets, not universal training prescriptions. */
data class PlanRegressionPolicy(
    private val maximumWeeklyIncreaseKm: Double,
    private val maximumWeeklyMultiplier: Double,
    private val maximumLongRunIncreaseKm: Double,
    private val maximumSingleRunFraction: Double,
    private val maximumHardSessionsPerWeek: Int,
    private val minimumDaysBetweenHardSessions: Int,
    private val daysAfterHardLowerBody: Int,
    private val maximumTaperWeekFraction: Double,
) {
    init {
        positive(maximumWeeklyIncreaseKm, "weekly increase")
        positive(maximumWeeklyMultiplier, "weekly multiplier")
        positive(maximumLongRunIncreaseKm, "long-run increase")
        positive(maximumSingleRunFraction, "single-run fraction")
        positive(maximumTaperWeekFraction, "taper fraction")
        require(
            !(maximumWeeklyMultiplier < 1 ||
                maximumSingleRunFraction > 1 ||
                maximumTaperWeekFraction > 1 ||
                maximumHardSessionsPerWeek < 0 ||
                minimumDaysBetweenHardSessions < 1 ||
                daysAfterHardLowerBody < 0)
        ) {
            "Invalid regression acceptance budget"
        }
    }

    fun maximumWeeklyIncreaseKm(): Double = maximumWeeklyIncreaseKm

    fun maximumWeeklyMultiplier(): Double = maximumWeeklyMultiplier

    fun maximumLongRunIncreaseKm(): Double = maximumLongRunIncreaseKm

    fun maximumSingleRunFraction(): Double = maximumSingleRunFraction

    fun maximumHardSessionsPerWeek(): Int = maximumHardSessionsPerWeek

    fun minimumDaysBetweenHardSessions(): Int = minimumDaysBetweenHardSessions

    fun daysAfterHardLowerBody(): Int = daysAfterHardLowerBody

    fun maximumTaperWeekFraction(): Double = maximumTaperWeekFraction

    override fun toString(): String =
        "PlanRegressionPolicy[maximumWeeklyIncreaseKm=$maximumWeeklyIncreaseKm, maximumWeeklyMultiplier=$maximumWeeklyMultiplier, maximumLongRunIncreaseKm=$maximumLongRunIncreaseKm, maximumSingleRunFraction=$maximumSingleRunFraction, maximumHardSessionsPerWeek=$maximumHardSessionsPerWeek, minimumDaysBetweenHardSessions=$minimumDaysBetweenHardSessions, daysAfterHardLowerBody=$daysAfterHardLowerBody, maximumTaperWeekFraction=$maximumTaperWeekFraction]"

    private fun positive(value: Double, field: String) {
        require(value.isFinite() && value > 0) { "Invalid $field" }
    }
}
