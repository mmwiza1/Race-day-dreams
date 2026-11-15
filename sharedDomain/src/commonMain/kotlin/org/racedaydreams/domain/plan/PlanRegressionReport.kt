package org.racedaydreams.domain.plan

import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.time.LocalDate

/** Original engineering diagnostics. Missing prescription distance remains unknown. */
data class PlanRegressionReport(private var issues: List<Issue>, private var weeks: List<Week>) {
    enum class Severity {
        ERROR,
        UNVERIFIABLE,
    }

    data class Issue(
        private val severity: Severity?,
        private val code: String?,
        private val date: LocalDate?,
        private val explanation: String?,
    ) {
        fun severity(): Severity? = severity

        fun code(): String? = code

        fun date(): LocalDate? = date

        fun explanation(): String? = explanation

        override fun toString(): String =
            "Issue[severity=$severity, code=$code, date=$date, explanation=$explanation]"
    }

    data class Week(
        private val start: LocalDate?,
        private val complete: Boolean,
        private val trainingRuns: Int,
        private val hardSessions: Int,
        private val trainingDistanceKm: Double?,
        private val knownDistanceKm: Double,
        private val missingDistancePrescriptions: Int,
        private val longestRunKm: Double?,
    ) {
        fun start(): LocalDate? = start

        fun complete(): Boolean = complete

        fun trainingRuns(): Int = trainingRuns

        fun hardSessions(): Int = hardSessions

        fun trainingDistanceKm(): Double? = trainingDistanceKm

        fun knownDistanceKm(): Double = knownDistanceKm

        fun missingDistancePrescriptions(): Int = missingDistancePrescriptions

        fun longestRunKm(): Double? = longestRunKm

        override fun toString(): String =
            "Week[start=$start, complete=$complete, trainingRuns=$trainingRuns, hardSessions=$hardSessions, trainingDistanceKm=$trainingDistanceKm, knownDistanceKm=$knownDistanceKm, missingDistancePrescriptions=$missingDistancePrescriptions, longestRunKm=$longestRunKm]"
    }

    init {
        issues = coachingReadOnlyList(issues)
        weeks = coachingReadOnlyList(weeks)
    }

    fun issues(): List<Issue> = issues

    fun weeks(): List<Week> = weeks

    fun passed(): Boolean = issues.none { it.severity() == Severity.ERROR }

    fun fullyVerified(): Boolean = issues.isEmpty()

    fun has(code: String?): Boolean = issues.any { it.code()!!.equals(code) }

    override fun toString(): String = "PlanRegressionReport[issues=$issues, weeks=$weeks]"
}
