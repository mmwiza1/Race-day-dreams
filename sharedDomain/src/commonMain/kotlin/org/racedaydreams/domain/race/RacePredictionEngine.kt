package org.racedaydreams.domain.race

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.coaching.performanceDecimal
import org.racedaydreams.domain.time.*

/** Original conservative product heuristic, not a validated individual race model. */
object RacePredictionEngine {
    data class Prediction(
        private val lowSeconds: Long?,
        private val highSeconds: Long?,
        private val confidence: String?,
        private val reasons: List<String>?,
    ) {
        fun lowSeconds(): Long? = lowSeconds

        fun highSeconds(): Long? = highSeconds

        fun confidence(): String? = confidence

        fun reasons(): List<String>? = reasons

        override fun toString(): String =
            "Prediction[lowSeconds=$lowSeconds, highSeconds=$highSeconds, confidence=$confidence, reasons=$reasons]"
    }

    @JvmStatic
    @JvmOverloads
    fun predict(
        metres: Double,
        results: List<RecordedResult>,
        runs: List<ActivityRecord>,
        today: LocalDate,
        /** Shows the reason distances in miles; the evidence itself is unchanged (audit B-04). */
        imperial: Boolean = false,
    ): Prediction {
        require(metres.isFinite() && metres >= 1000 && metres <= 100000) { "Invalid race distance" }
        val reasons = mutableListOf<String>()
        val estimates = mutableListOf<Double>()
        val sources = mutableSetOf<String>()
        var authoritative = 0
        for (r in
            results.sortedWith(
                compareByDescending<RecordedResult> { it.date().toEpochDay() }.thenBy { it.id() }
            )) {
            val days = today.toEpochDay() - r.date().toEpochDay()
            if (days < 0 || days > 120 || r.distance().metres() < min(5000.0, metres * 0.4))
                continue
            val key = r.activityId() ?: "${r.date()}:${r.distance().code()}:${r.duration()}"
            if (!sources.add(key)) continue
            if (r.source() == ResultSource.ROLLING && r.distance().metres() < metres * 0.5) continue
            estimates.add(
                r.duration().toMillis() / 1000.0 *
                    (metres / r.distance().metres()).pow(1.07) *
                    (1 + days / 120.0 * 0.02)
            )
            if (r.source() != ResultSource.ROLLING) authoritative++
            reasons.add(
                "${r.distance().label()} result from $days days ago (${raceSourceText(r.source())})."
            )
            if (estimates.size == 8) break
        }
        var total = 0.0
        var longest = 0.0
        val weeks = mutableSetOf<LocalDate>()
        // Each physical run counts once, however many sources reported it (audit B-02).
        for (a in PhysicalRunSessions.counted(runs)) {
            val date = ActivityDates.localDate(a)
            if (
                date.toEpochDay() > today.toEpochDay() ||
                    date.toEpochDay() < today.minusDays(27).toEpochDay() ||
                    a.distanceMetres() == null
            )
                continue
            val distance = a.distanceMetres()!!
            total += distance
            longest = max(longest, distance)
            weeks.add(date.minusDays((date.getDayOfWeek().getValue() - 1).toLong()))
        }
        val unitMetres = if (imperial) 1609.344 else 1000.0
        val unit = if (imperial) "mi" else "km"
        reasons.add(
            "Recent weekly average ${performanceDecimal(total / 4 / unitMetres)} $unit; longest ${performanceDecimal(longest / unitMetres)} $unit. Endurance coverage is separate from short-distance speed."
        )
        val endurance =
            longest >= metres * (if (metres >= 40000) 0.6 else if (metres >= 20000) 0.6 else 0.5) &&
                total / 4 >= metres * (if (metres >= 40000) 0.7 else 0.6)
        if (estimates.size < 2 || metres >= 20000 && !endurance)
            return Prediction(null, null, "Insufficient evidence", coachingReadOnlyList(reasons))
        estimates.sort()
        val median = estimates[estimates.size / 2]
        val spread = (estimates.last() - estimates.first()) / median
        var strong =
            estimates.size >= 3 &&
                authoritative >= 2 &&
                weeks.size >= 4 &&
                endurance &&
                spread < 0.08
        var width = if (strong) 0.035 else if (endurance) 0.065 else 0.12
        val penalty = if (endurance) 1.0 else 1.05
        if (spread > 0.1) {
            width = max(width, min(0.25, spread))
            strong = false
            reasons.add("Conflicting performances widen the range.")
        }
        reasons.add(
            "Distance scaling, ageing and range widths are conservative product heuristics. Terrain, weather and race-day response remain unknown. Your goal is unchanged."
        )
        return Prediction(
            floor(median * penalty * (1 - width) / 60).toLong() * 60,
            ceil(median * penalty * (1 + width) / 60).toLong() * 60,
            if (strong) "Strong" else if (endurance) "Moderate" else "Limited",
            coachingReadOnlyList(reasons),
        )
    }
}

/** JVM keeps its original platform-default source-label casing; native UI uses English labels. */
internal expect fun raceSourceText(source: ResultSource): String
