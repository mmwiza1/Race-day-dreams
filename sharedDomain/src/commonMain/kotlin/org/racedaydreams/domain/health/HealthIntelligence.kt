package org.racedaydreams.domain.health

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max
import org.racedaydreams.domain.coaching.coachingReadOnlyList
import org.racedaydreams.domain.coaching.coachingSum
import org.racedaydreams.domain.time.*

/** Original source-specific personal context, never a medical or readiness score. */
object HealthIntelligence {
    enum class Metric {
        SLEEP_MINUTES,
        RESTING_HR,
        HRV_RMSSD,
        STEPS,
    }

    data class Observation(
        private val id: String?,
        private val metric: Metric?,
        private val date: LocalDate?,
        private val start: Instant?,
        private val end: Instant?,
        private val value: Double,
        private val source: String?,
        private val importedAt: Instant?,
    ) {
        init {
            if (
                id == null ||
                    metric == null ||
                    date == null ||
                    start == null ||
                    end == null ||
                    importedAt == null
            )
                throw NullPointerException()
            require(
                id.trim { it <= ' ' }.isNotEmpty() &&
                    source != null &&
                    source.trim { it <= ' ' }.isNotEmpty() &&
                    !end.isBefore(start) &&
                    value.isFinite() &&
                    value >= 0
            ) {
                "Invalid health observation"
            }
            require(
                !((metric == Metric.RESTING_HR || metric == Metric.HRV_RMSSD) && value == 0.0)
            ) {
                "Missing is not zero"
            }
        }

        fun id(): String = id!!

        fun metric(): Metric = metric!!

        fun date(): LocalDate = date!!

        fun start(): Instant = start!!

        fun end(): Instant = end!!

        fun value(): Double = value

        fun source(): String = source!!

        fun importedAt(): Instant = importedAt!!

        override fun toString(): String =
            "Observation[id=$id, metric=$metric, date=$date, start=$start, end=$end, value=$value, source=$source, importedAt=$importedAt]"
    }

    data class Trend(
        private val metric: Metric?,
        private val source: String?,
        private val recent: Double?,
        private val baseline: Double?,
        private val recentDays: Int,
        private val referenceDays: Int,
        private val interpretation: String?,
        private val limitation: String?,
    ) {
        fun metric(): Metric? = metric

        fun source(): String? = source

        fun recent(): Double? = recent

        fun baseline(): Double? = baseline

        fun recentDays(): Int = recentDays

        fun referenceDays(): Int = referenceDays

        fun interpretation(): String? = interpretation

        fun limitation(): String? = limitation

        override fun toString(): String =
            "Trend[metric=$metric, source=$source, recent=$recent, baseline=$baseline, recentDays=$recentDays, referenceDays=$referenceDays, interpretation=$interpretation, limitation=$limitation]"
    }

    /** Latest source wins; reference excludes recent seven days and never combines sources. */
    @JvmStatic
    fun evaluate(input: List<Observation>, today: LocalDate): List<Trend> {
        val out = mutableListOf<Trend>()
        for (metric in Metric.entries) {
            val rows =
                input
                    .filter {
                        it.metric() == metric &&
                            it.date().toEpochDay() <= today.toEpochDay() &&
                            it.date().toEpochDay() >= today.minusDays(34).toEpochDay()
                    }
                    .sortedWith(
                        compareByDescending<Observation> { it.start() }.thenBy { it.source() }
                    )
            if (rows.isEmpty()) {
                out.add(
                    Trend(
                        metric,
                        "Unavailable",
                        null,
                        null,
                        0,
                        0,
                        "Insufficient evidence",
                        "Optional data is missing.",
                    )
                )
                continue
            }
            val source = rows.first().source()
            val days = mutableMapOf<LocalDate, Observation>()
            for (row in rows) if (row.source() == source) {
                if (metric == Metric.SLEEP_MINUTES) {
                    val old = days[row.date()]
                    if (old == null || row.value() > old.value()) days[row.date()] = row
                } else if (!days.containsKey(row.date())) days[row.date()] = row
            }
            val recent = mutableListOf<Double>()
            val base = mutableListOf<Double>()
            for ((date, row) in days.entries.sortedBy { it.key.toEpochDay() }) if (
                date.toEpochDay() >= today.minusDays(6).toEpochDay()
            )
                recent.add(row.value())
            else base.add(row.value())
            val r = average(recent)
            val b = average(base)
            var state = "Insufficient evidence"
            if (recent.size >= 4 && base.size >= 14 && b != null && b > 0) {
                val spread = average(base.map { abs(it - b) }) ?: 0.0
                val threshold =
                    max(
                        spread * 1.5,
                        b *
                            (if (metric == Metric.HRV_RMSSD) 0.15
                            else if (metric == Metric.STEPS) 0.25 else 0.08),
                    )
                state =
                    if (abs(r!! - b) <= threshold) "Within recent personal range"
                    else if (r > b) "Above recent personal baseline"
                    else "Below recent personal baseline"
            }
            out.add(
                Trend(
                    metric,
                    source,
                    r,
                    b,
                    recent.size,
                    base.size,
                    state,
                    "7-day trend / preceding 28-day reference; at least 4 and 14 measured days required. Source changes are not combined. Supporting context, not diagnosis.",
                )
            )
        }
        return coachingReadOnlyList(out)
    }

    private fun average(values: List<Double>): Double? =
        if (values.isEmpty()) null else coachingSum(values) / values.size.toDouble()
}
