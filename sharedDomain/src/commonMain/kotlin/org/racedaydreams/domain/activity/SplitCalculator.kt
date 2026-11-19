package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.UnitSystem

/** Splits require retained samples; aggregate time/distance cannot establish their evidence. */
object SplitCalculator {
    @JvmStatic
    fun calculate(activity: ActivityRecord?, units: UnitSystem?): SplitAnalysis =
        calculate(activity, if (units == UnitSystem.IMPERIAL) 1609.344 else 1000.0)

    @JvmStatic
    fun calculate(activity: ActivityRecord?, splitMetres: Double): SplitAnalysis {
        if (activity == null) throw NullPointerException("Activity")
        require(splitMetres.isFinite() && splitMetres > 0) { "Split distance must be positive." }
        val series = SampleSeries(activity)
        val result = mutableListOf<RunSplit>()
        val reasons = ArrayList(series.reasons)
        if (!series.usable()) return SplitAnalysis(result, series.quality, reasons)
        val last = series.samples.last().distanceMetres()
        if (last / splitMetres > 10000)
            return SplitAnalysis(
                result,
                SampleQuality.REJECTED,
                listOf("The sample range is too large for a sensible split list."),
            )
        var segmentIndex = 0
        val count = ceil(last / splitMetres - .000000001).toInt()
        var missing = false
        for (number in 1..count) {
            val from = (number - 1) * splitMetres
            val to = min(number * splitMetres, last)
            if (to - from < .001) continue
            while (
                segmentIndex < series.segments.size &&
                    series.samples[series.segments[segmentIndex].last()].distanceMetres() <
                        to - .000001
            ) segmentIndex++
            if (segmentIndex == series.segments.size) {
                missing = true
                continue
            }
            val segment = series.segments[segmentIndex]
            if (series.samples[segment.first()].distanceMetres() > from + .000001) {
                missing = true
                continue
            }
            // Fixed splits share the first observed crossing; final stationary time is included.
            val start = series.at(segment, from, false)
            val end = series.at(segment, to, to == last)
            val startSeconds = start.seconds()
            if (end.seconds() <= startSeconds) {
                missing = true
                continue
            }
            val startElevation = start.elevation()
            val endElevation = end.elevation()
            val elevation =
                if (startElevation == null || endElevation == null) null
                else endElevation - startElevation
            result.add(
                RunSplit(
                    number,
                    to - from,
                    ActivityChecks.duration(end.seconds() - startSeconds),
                    series.sampleMean(startSeconds, end.seconds(), true),
                    elevation,
                    series.sampleMean(startSeconds, end.seconds(), false),
                    to - from < splitMetres - .001,
                )
            )
        }
        if (missing)
            reasons.add(
                "Some distance splits could not be derived because their full interval was not continuously recorded."
            )
        reasons.add(
            "Splits use observed elapsed time, including stationary time within continuously recorded sections. Boundaries are interpolated between nearby recorded points. Heart-rate/cadence values are means of available samples, not invented complete recordings."
        )
        return SplitAnalysis(
            result,
            if (missing) SampleQuality.UNCERTAIN else series.quality,
            reasons,
        )
    }
}
