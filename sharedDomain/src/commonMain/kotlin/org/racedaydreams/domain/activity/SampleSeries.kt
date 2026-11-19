package org.racedaydreams.domain.activity

import kotlin.jvm.JvmField
import kotlin.math.*

/** Original conservative quality gate. Never interpolates across invalid intervals. */
internal class SampleSeries(activity: ActivityRecord) {
    @JvmField val samples = activity.samples()
    @JvmField val segments = mutableListOf<Segment>()
    @JvmField val reasons = mutableListOf<String>()
    @JvmField var quality: SampleQuality? = null
    @JvmField var totalsDisagree = false
    private var secondsIndex: DoubleArray? = null
    private var heartRateTotals: DoubleArray? = null
    private var cadenceTotals: DoubleArray? = null
    private var heartRateCounts: IntArray? = null
    private var cadenceCounts: IntArray? = null

    data class Segment(private val first: Int, private val last: Int) {
        fun first(): Int = first

        fun last(): Int = last
    }

    data class Point(private val seconds: Double, private val elevation: Double?) {
        fun seconds(): Double = seconds

        fun elevation(): Double? = elevation
    }

    init {
        initialize(activity)
    }

    private fun initialize(activity: ActivityRecord) {
        if (samples.size < 2) {
            quality = SampleQuality.UNAVAILABLE
            reasons.add(
                "Continuous distance/time samples are unavailable; whole-activity totals cannot establish rolling efforts or distance splits."
            )
            return
        }
        if (samples.size > 500000) {
            reject("The activity contains too many samples to analyse safely.")
            return
        }
        val elapsed = ActivityChecks.seconds(activity.elapsedTime())
        if (ActivityChecks.seconds(samples.last().elapsed()) > elapsed + .001) {
            reject("Distance samples extend beyond the recorded activity duration.")
            return
        }
        quality = SampleQuality.QUALIFIED
        var first = 0
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val seconds = ActivityChecks.seconds(current.elapsed().minus(previous.elapsed()))
            val distance = current.distanceMetres() - previous.distanceMetres()
            if (seconds <= 0 || distance < 0) {
                reject("Samples have unordered timestamps or decreasing cumulative distance.")
                return
            }
            if (distance / seconds > MAX_SPEED_METRES_PER_SECOND) {
                reject(
                    "An implausible distance jump exceeds the running sample quality limit (12 m/s); no automatic PB is awarded."
                )
                return
            }
            if (seconds > MAX_GAP_SECONDS || current.discontinuity()) {
                if (index - 1 > first) segments.add(Segment(first, index - 1))
                first = index
                quality = SampleQuality.UNCERTAIN
            }
        }
        if (samples.size - 1 > first) segments.add(Segment(first, samples.size - 1))
        if (quality == SampleQuality.UNCERTAIN)
            reasons.add(
                "Pause/discontinuity markers or gaps over 30 seconds split the samples into continuous sections. No split or effort crosses them."
            )
        val lastDistance = samples.last().distanceMetres()
        val total = activity.distanceMetres()
        if (total != null && abs(lastDistance - total) > max(100.0, total * .05)) {
            totalsDisagree = true
            quality = SampleQuality.UNCERTAIN
            reasons.add(
                "Sample distance and the recorded activity total differ materially; calculated efforts remain uncertain."
            )
        }
        indexMetrics()
    }

    private fun reject(reason: String) {
        quality = SampleQuality.REJECTED
        segments.clear()
        reasons.clear()
        reasons.add(reason)
    }

    fun usable(): Boolean =
        quality != SampleQuality.REJECTED && quality != SampleQuality.UNAVAILABLE

    /** Start uses the latest observation at a distance plateau; end uses the earliest. */
    fun at(segment: Segment, distance: Double, start: Boolean): Point {
        var low = segment.first()
        var high = segment.last()
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (samples[middle].distanceMetres() < distance) low = middle + 1 else high = middle
        }
        var index = low
        var right = samples[index]
        if (abs(right.distanceMetres() - distance) < .000001) {
            if (start) {
                while (
                    index < segment.last() &&
                        abs(samples[index + 1].distanceMetres() - distance) < .000001
                ) index++
                right = samples[index]
            }
            return Point(ActivityChecks.seconds(right.elapsed()), right.elevationMetres())
        }
        val left = samples[index - 1]
        val fraction =
            (distance - left.distanceMetres()) / (right.distanceMetres() - left.distanceMetres())
        val seconds =
            ActivityChecks.seconds(left.elapsed()) +
                fraction * ActivityChecks.seconds(right.elapsed().minus(left.elapsed()))
        val le = left.elevationMetres()
        val re = right.elevationMetres()
        return Point(seconds, if (le != null && re != null) le + fraction * (re - le) else null)
    }

    fun sampleMean(startSeconds: Double, endSeconds: Double, heartRate: Boolean): Double? {
        val first = lowerTime(startSeconds, false)
        val last = lowerTime(endSeconds, true)
        val totals = (if (heartRate) heartRateTotals else cadenceTotals)!!
        val counts = (if (heartRate) heartRateCounts else cadenceCounts)!!
        val count = counts[last] - counts[first]
        return if (count == 0) null else (totals[last] - totals[first]) / count
    }

    private fun indexMetrics() {
        val size = samples.size
        val seconds = DoubleArray(size)
        val hrTotals = DoubleArray(size + 1)
        val cdTotals = DoubleArray(size + 1)
        val hrCounts = IntArray(size + 1)
        val cdCounts = IntArray(size + 1)
        secondsIndex = seconds
        heartRateTotals = hrTotals
        cadenceTotals = cdTotals
        heartRateCounts = hrCounts
        cadenceCounts = cdCounts
        for (index in 0 until size) {
            val sample = samples[index]
            seconds[index] = ActivityChecks.seconds(sample.elapsed())
            hrTotals[index + 1] = hrTotals[index]
            cdTotals[index + 1] = cdTotals[index]
            hrCounts[index + 1] = hrCounts[index]
            cdCounts[index + 1] = cdCounts[index]
            sample.heartRate()?.let {
                hrTotals[index + 1] += it
                hrCounts[index + 1]++
            }
            sample.cadence()?.let {
                cdTotals[index + 1] += it
                cdCounts[index + 1]++
            }
        }
    }

    private fun lowerTime(seconds: Double, inclusive: Boolean): Int {
        val values = secondsIndex!!
        var low = 0
        var high = values.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (values[middle] < seconds || inclusive && values[middle] <= seconds) low = middle + 1
            else high = middle
        }
        return low
    }

    companion object {
        const val MAX_GAP_SECONDS = 30.0
        const val MAX_SPEED_METRES_PER_SECOND = 12.0
    }
}
