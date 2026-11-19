package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Original two linear sweeps per distance: inspect both sample-boundary starts and ends. */
object BestEffortCalculator {
    @JvmStatic
    fun calculate(activity: ActivityRecord?): BestEffortAnalysis =
        calculate(activity, PerformanceDistance.CORE)

    @JvmStatic
    fun calculate(
        activity: ActivityRecord?,
        distances: List<PerformanceDistance>?,
    ): BestEffortAnalysis {
        if (activity == null) throw NullPointerException("Activity")
        if (distances == null) throw NullPointerException("Distances")
        val series = SampleSeries(activity)
        val efforts = mutableListOf<BestEffort>()
        if (!series.usable()) return BestEffortAnalysis(efforts, series.quality, series.reasons)
        for (rawDistance: PerformanceDistance? in distances) {
            val distance = rawDistance ?: throw NullPointerException("Distance")
            var best: Candidate? = null
            for (segment in series.segments) {
                var end = segment.first()
                for (start in segment.first()..segment.last()) {
                    val target = series.samples[start].distanceMetres() + distance.metres()
                    while (
                        end <= segment.last() &&
                            series.samples[end].distanceMetres() < target - .000001
                    ) end++
                    if (end > segment.last()) break
                    val finish = interpolate(series.samples, end, target)
                    best =
                        better(
                            best,
                            ActivityChecks.seconds(series.samples[start].elapsed()),
                            finish,
                            end - start + 1,
                        )
                }
                var start = segment.first()
                for (endIndex in segment.first()..segment.last()) {
                    val target = series.samples[endIndex].distanceMetres() - distance.metres()
                    if (target < series.samples[segment.first()].distanceMetres() - .000001)
                        continue
                    while (
                        start < endIndex &&
                            series.samples[start + 1].distanceMetres() <= target + .000001
                    ) start++
                    val begin =
                        if (abs(series.samples[start].distanceMetres() - target) < .000001)
                            ActivityChecks.seconds(series.samples[start].elapsed())
                        else interpolate(series.samples, start + 1, target)
                    best =
                        better(
                            best,
                            begin,
                            ActivityChecks.seconds(series.samples[endIndex].elapsed()),
                            endIndex - start + 1,
                        )
                }
            }
            val candidate = best ?: continue
            val reasons = mutableListOf<String>()
            var quality = SampleQuality.QUALIFIED
            if (candidate.samples < 5) {
                quality = SampleQuality.UNCERTAIN
                reasons.add(
                    "Fewer than five distance points cover this effort; it is not awarded as an automatic PB."
                )
            }
            if (series.totalsDisagree) {
                quality = SampleQuality.UNCERTAIN
                reasons.add(
                    "Sample distance and total distance disagree; this candidate is not awarded as an automatic PB."
                )
            }
            reasons.add(
                "Derived from a continuous recorded interval with boundaries interpolated between nearby samples; not an official race result."
            )
            val begin = ActivityChecks.duration(candidate.start)
            val finish = ActivityChecks.duration(candidate.end)
            efforts.add(BestEffort(distance, finish.minus(begin), begin, finish, quality, reasons))
        }
        val reasons = ArrayList(series.reasons)
        if (efforts.isEmpty())
            reasons.add(
                "No continuously recorded interval was long enough for the requested distances."
            )
        return BestEffortAnalysis(efforts, series.quality, reasons)
    }

    /** Stable import identities; no duplicate PB evidence or rewriting activity actuals. */
    @JvmStatic
    fun qualifiedResults(activity: ActivityRecord?): List<RecordedResult> {
        val results = mutableListOf<RecordedResult>()
        for (effort in calculate(activity).efforts()) {
            if (effort.quality() != SampleQuality.QUALIFIED) continue
            val record = activity!!
            results.add(
                RecordedResult(
                    record.id() + ":" + effort.distance().code(),
                    effort.distance(),
                    effort.duration(),
                    CalendarDates.atOffset(
                        record.start(),
                        record.startOffset() ?: Offsets.ofTotalSeconds(0),
                    ),
                    null,
                    ResultSource.ROLLING,
                    record.id(),
                    "Calculated rolling effort from retained continuous samples.",
                )
            )
        }
        return Immutable.copyList(results)
    }

    private data class Candidate(val start: Double, val end: Double, val samples: Int)

    private fun better(previous: Candidate?, start: Double, end: Double, samples: Int): Candidate? {
        if (end <= start) return previous
        return if (
            previous == null ||
                end - start < previous.end - previous.start - .000000001 ||
                abs(end - start - (previous.end - previous.start)) <= .000000001 &&
                    start < previous.start
        )
            Candidate(start, end, samples)
        else previous
    }

    private fun interpolate(samples: List<DistanceSample>, end: Int, target: Double): Double {
        val right = samples[end]
        if (abs(right.distanceMetres() - target) < .000001)
            return ActivityChecks.seconds(right.elapsed())
        val left = samples[end - 1]
        val fraction =
            (target - left.distanceMetres()) / (right.distanceMetres() - left.distanceMetres())
        return ActivityChecks.seconds(left.elapsed()) +
            fraction * ActivityChecks.seconds(right.elapsed().minus(left.elapsed()))
    }
}
