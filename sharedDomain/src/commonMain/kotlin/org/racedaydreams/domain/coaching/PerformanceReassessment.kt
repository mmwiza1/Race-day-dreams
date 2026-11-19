package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.*
import org.racedaydreams.domain.time.*

/** Original conservative product heuristics. No goal or easy-pace mutation. */
object PerformanceReassessment {
    enum class Confidence {
        STRONG,
        MODERATE,
        LIMITED,
    }

    data class Evidence(
        private val identity: String?,
        private val metres: Double,
        private val duration: Duration?,
        private val date: LocalDate?,
        private val source: String?,
        private val authority: Double,
    ) {
        fun identity(): String? = identity

        fun metres(): Double = metres

        fun duration(): Duration? = duration

        fun date(): LocalDate? = date

        fun source(): String? = source

        fun authority(): Double = authority

        override fun toString(): String =
            "Evidence[identity=$identity, metres=$metres, duration=$duration, date=$date, source=$source, authority=$authority]"
    }

    data class Review(
        private val propose: Boolean,
        private val confidence: Confidence?,
        private val fasterFraction: Double,
        private val supportingMetres: Double,
        private val reasons: List<String>?,
    ) {
        fun propose(): Boolean = propose

        fun confidence(): Confidence? = confidence

        fun fasterFraction(): Double = fasterFraction

        fun supportingMetres(): Double = supportingMetres

        fun reasons(): List<String>? = reasons

        override fun toString(): String =
            "Review[propose=$propose, confidence=$confidence, fasterFraction=$fasterFraction, supportingMetres=$supportingMetres, reasons=$reasons]"
    }

    @JvmStatic
    fun review(
        results: List<RecordedResult>,
        assessments: List<AssessmentResult>,
        end: LocalDate,
        baseline: BaselineConfidence,
        completion: Double,
        controlledResponses: Int,
        effortConcern: Boolean,
    ): Review {
        val evidence = mutableListOf<Evidence>()
        for (r in results) evidence.add(
            Evidence(
                r.activityId() ?: "day:${r.date()}",
                r.distance().metres(),
                r.duration(),
                r.date(),
                if (r.source() == ResultSource.OFFICIAL_CHIP) "Official race"
                else if (r.source() == ResultSource.ROLLING) "Rolling best effort"
                else "Manual result",
                if (r.source() == ResultSource.OFFICIAL_CHIP) 3.0
                else if (r.source() == ResultSource.ROLLING) .5 else 2.0,
            )
        )
        for (a in assessments) if (a.assessment().mode() == AssessmentMode.BEST_EFFORT) {
            val date = CalendarDates.atOffset(a.completedAt(), Offsets.ofTotalSeconds(0))
            evidence.add(
                Evidence(
                    a.activityId() ?: "day:$date",
                    a.assessment().distanceKm() * 1000.0,
                    a.duration(),
                    date,
                    "Deliberate assessment",
                    3.0,
                )
            )
        }
        val improvements = HashMap<String, Double>()
        val strengths = HashMap<String, Double>()
        val winners = HashMap<String, Evidence>()
        val reasons = mutableListOf<String>()
        for (r in evidence) {
            val date = requireNotNull(r.date())
            if (
                coachingBefore(date, end.minusDays(28)) ||
                    !coachingBefore(date, end) ||
                    r.metres() < 1000
            )
                continue
            val old =
                evidence
                    .filter { p ->
                        abs(p.metres() - r.metres()) < 1 &&
                            coachingBefore(requireNotNull(p.date()), date) &&
                            !coachingBefore(p.date()!!, coachingSixMonthsBefore(end)) &&
                            p.authority() >= 2
                    }
                    .minWithOrNull(compareBy { requireNotNull(it.duration()) }) ?: continue
            val gain = 1 - r.duration()!!.toMillis() / old.duration()!!.toMillis().toDouble()
            if (gain < .02) continue
            val weight =
                r.authority() *
                    (if (r.metres() < 3000) .65 else 1.0) *
                    (if (coachingBefore(date, end.minusDays(14))) .75 else 1.0)
            val identity = requireNotNull(r.identity())
            val previous = improvements[identity] ?: 0.0
            if (
                gain > previous ||
                    abs(gain - previous) < 1e-9 && weight > (strengths[identity] ?: 0.0)
            ) {
                improvements[identity] = gain
                strengths[identity] = weight
                winners[identity] = r
            }
        }
        var gain = 0.0
        for (value in improvements.values) gain = max(gain, value)
        val authority = coachingSum(strengths.values)
        val corroborated = completion >= .8 && controlledResponses >= 3 && !effortConcern
        val credible = authority >= 1.5 && (gain >= .05 || improvements.size >= 2 && gain >= .025)
        val confidence =
            if (!corroborated || !credible || baseline == BaselineConfidence.LOW) Confidence.LIMITED
            else if (authority >= 3 && baseline == BaselineConfidence.STRONG) Confidence.STRONG
            else Confidence.MODERATE
        val propose = confidence != Confidence.LIMITED
        var distance = 0.0
        for (value in winners.values) distance = max(distance, value.metres())
        val fraction = if (propose) min(if (distance < 3000) .015 else .03, gain * .4) else 0.0
        val described = mutableSetOf<String>()
        val sorted =
            winners.values.sortedWith(
                compareBy<Evidence> { it.date()!!.toEpochDay() }.thenBy { it.identity() }
            )
        for (e in sorted) {
            val identity = requireNotNull(e.identity())
            if (described.add(identity))
                reasons.add(
                    "${e.source()} over ${performanceDecimal(e.metres() / 1000)} km: approximately ${performanceDecimal(improvements.getValue(identity) * 100)}% faster than prior comparable evidence."
                )
        }
        reasons.add(
            "${improvements.size} independent recent comparable performance improvements; same-activity efforts count once."
        )
        reasons.add(
            "Source, distance and recency affect confidence: official results and deliberate best-effort assessments carry more weight than incidental rolling efforts. Manual results are not independently verified."
        )
        reasons.add(
            "Baseline confidence: ${baseline.name.lowercase()}. Completion: ${roundedNanos(completion * 100)}%; controlled reported responses: $controlledResponses."
        )
        if (effortConcern) reasons.add("Recent effort concerns argue against faster ranges.")
        reasons.add(
            if (propose)
                "Offer a bounded quality-pace review only. Easy/recovery effort, mileage and race goal stay unchanged."
            else "Keep current training ranges; more corroborating evidence is needed."
        )
        return Review(propose, confidence, fraction, distance, coachingReadOnlyList(reasons))
    }
}

/** Original raw records permit null entries; factory lists still contain only actual findings. */
internal fun <T> coachingReadOnlyList(values: Collection<T>): List<T> {
    val rows = ArrayList(values)
    return object : AbstractList<T>() {
        override val size: Int
            get() = rows.size

        override fun get(index: Int): T = rows[index]
    }
}
