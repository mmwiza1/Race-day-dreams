package org.racedaydreams.domain.health

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.coaching.coachingReadOnlyList

/** Corroborated context only: no diagnosis, readiness number or automatic plan mutation. */
object RecoveryCoaching {
    @JvmStatic
    fun easyAlternative(w: PlannedWorkout, reason: String?): PlannedWorkout {
        require(w.category() != WorkoutCategory.RACE) {
            "Your race goal is changed only through goal setup."
        }
        val segment =
            WorkoutSegment(
                WorkoutSegment.Kind.RUN,
                w.distanceKm(),
                w.duration(),
                null,
                null,
                null,
                3.0,
                "Conversational effort; stop or shorten if needed. No catch-up mileage.",
            )
        return PlannedWorkout(
            w.id(),
            w.date(),
            WorkoutCategory.EASY_RUN,
            w.distanceKm(),
            w.duration(),
            Immutable.list(segment),
            "Runner chose an easy alternative. $reason",
        )
    }

    /** Freshness/corroboration remain caller-owned, exactly as the original interpretation. */
    @JvmStatic
    fun adversePersonalTrend(trend: HealthIntelligence.Trend): Boolean =
        trend.metric() == HealthIntelligence.Metric.RESTING_HR &&
            trend.interpretation()!!.equals("Above recent personal baseline") ||
            (trend.metric() == HealthIntelligence.Metric.SLEEP_MINUTES ||
                trend.metric() == HealthIntelligence.Metric.HRV_RMSSD) &&
                trend.interpretation()!!.equals("Below recent personal baseline")

    data class Context(
        private val evidence: List<String>?,
        private val options: List<String>?,
        private val reviewSuggested: Boolean,
    ) {
        fun evidence(): List<String>? = evidence

        fun options(): List<String>? = options

        fun reviewSuggested(): Boolean = reviewSuggested

        override fun toString(): String =
            "Context[evidence=$evidence, options=$options, reviewSuggested=$reviewSuggested]"
    }

    @JvmStatic
    fun evaluate(
        trends: List<HealthIntelligence.Trend>,
        highRunningLoad: Boolean,
        repeatedHighRpe: Boolean,
        hardLowerBody: Boolean,
    ): Context {
        val reasons = mutableListOf<String>()
        var sleep = false
        var hrv = false
        var rhr = false
        for (t in trends) {
            if (
                t.recentDays() < 4 ||
                    t.referenceDays() < 14 ||
                    t.recent() == null ||
                    t.baseline() == null
            )
                continue
            val low = t.interpretation()!!.equals("Below recent personal baseline")
            val high = t.interpretation()!!.equals("Above recent personal baseline")
            if (t.metric() == HealthIntelligence.Metric.SLEEP_MINUTES && low) {
                sleep = true
                reasons.add("Sleep duration is below your recent personal range.")
            }
            if (t.metric() == HealthIntelligence.Metric.HRV_RMSSD && low) {
                hrv = true
                reasons.add("Comparable-source HRV is below your recent range.")
            }
            if (t.metric() == HealthIntelligence.Metric.RESTING_HR && high) {
                rhr = true
                reasons.add("Resting heart rate is above your recent baseline.")
            }
            if (t.metric() == HealthIntelligence.Metric.STEPS && high)
                reasons.add(
                    "General activity has been higher than usual; it is not converted into running stress."
                )
        }
        if (highRunningLoad) reasons.add("Recent running load is above your usual workload.")
        if (repeatedHighRpe) reasons.add("Several recent sessions had high reported effort.")
        if (hardLowerBody) reasons.add("Hard lower-body strength was recorded recently.")
        val suggest =
            sleep && rhr && highRunningLoad ||
                hrv && repeatedHighRpe ||
                sleep && repeatedHighRpe && hardLowerBody
        if (reasons.isEmpty())
            reasons.add(
                "No corroborated recovery concern is available. Missing signals remain unknown."
            )
        // Preserve the original fixed-size options list; choices never apply themselves.
        return Context(
            coachingReadOnlyList(reasons),
            if (suggest)
                arrayOf(
                        "Keep planned workout",
                        "Run by effort",
                        "Choose easy instead",
                        "Move workout",
                        "Rest",
                    )
                    .asList()
            else Immutable.list("Keep planned workout"),
            suggest,
        )
    }
}
