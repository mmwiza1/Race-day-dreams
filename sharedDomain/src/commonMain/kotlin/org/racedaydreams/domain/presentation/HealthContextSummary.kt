package org.racedaydreams.domain.presentation

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.activity.roundedNanos
import org.racedaydreams.domain.health.HealthIntelligence

/** Display-only translation; personal evidence gates remain in HealthIntelligence. */
data class HealthContextSummary(
    private val title: String?,
    private val value: String?,
    private val context: String?,
) {
    fun title(): String? = title

    fun value(): String? = value

    fun context(): String? = context

    override fun toString(): String =
        "HealthContextSummary[title=$title, value=$value, context=$context]"

    companion object {
        @JvmStatic
        fun of(t: HealthIntelligence.Trend): HealthContextSummary {
            val title =
                when (t.metric()!!) {
                    HealthIntelligence.Metric.SLEEP_MINUTES -> "Sleep"
                    HealthIntelligence.Metric.RESTING_HR -> "Resting heart rate"
                    HealthIntelligence.Metric.HRV_RMSSD -> "Heart-rate variability"
                    HealthIntelligence.Metric.STEPS -> "General activity"
                }
            val recent = t.recent()
            val value =
                if (recent == null) "Not available"
                else
                    when (t.metric()!!) {
                        HealthIntelligence.Metric.SLEEP_MINUTES -> {
                            val minutes = roundedNanos(recent)
                            "${minutes/60}h ${minutes%60}m"
                        }
                        HealthIntelligence.Metric.RESTING_HR -> "${roundedNanos(recent)} bpm"
                        HealthIntelligence.Metric.HRV_RMSSD -> "${healthWhole(recent)} ms"
                        HealthIntelligence.Metric.STEPS ->
                            "${healthGrouped(roundedNanos(recent))} steps/day"
                    }
            val context =
                if (t.recentDays() < 4 || t.referenceDays() < 14 || t.baseline() == null)
                    "Not enough comparable readings"
                else
                    when {
                        t.interpretation()!!.startsWith("Within") -> "Around your usual range"
                        t.interpretation()!!.startsWith("Above") -> "Above your usual range"
                        t.interpretation()!!.startsWith("Below") -> "Below your usual range"
                        else -> "Not enough comparable readings"
                    }
            return HealthContextSummary(title, value, context)
        }
    }
}

internal expect fun healthWhole(value: Double): String

/** UK grouping of the exact rounded long; never uses a lossy floating-point round trip. */
internal fun healthGrouped(value: Long): String {
    val text = value.toString()
    val unsigned = text.removePrefix("-")
    return (if (value < 0) "-" else "") +
        unsigned.reversed().chunked(3).joinToString(",").reversed()
}

/** Canonical decimal half-up, retaining negative zero and non-finite raw DTO display behaviour. */
internal fun healthWholeDecimal(value: Double): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
    val negative = value.toBits() < 0
    val text = expandPrescriptionDecimal(kotlin.math.abs(value).toString())
    var whole = text.substringBefore('.').toCharArray()
    if (text.substringAfter('.', "0").first() >= '5') {
        var i = whole.lastIndex
        while (i >= 0 && whole[i] == '9') {
            whole[i] = '0'
            i--
        }
        if (i < 0) return (if (negative) "-" else "") + "1" + whole.concatToString()
        whole[i]++
    }
    return (if (negative) "-" else "") + whole.concatToString()
}
