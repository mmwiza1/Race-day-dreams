package org.racedaydreams.domain.baseline

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.activity.*

/** Original assessment interpretation with explicit limits, not a single-run fitness oracle. */
object AssessmentAnalyzer {
    @JvmStatic
    fun recommend(baseline: RunnerBaseline): AssessmentRecommendation {
        val reasons = mutableListOf<String>()
        val unnecessary = baseline.confidence() == BaselineConfidence.STRONG
        val frequency = baseline.runsPerWeek()
        val longest = baseline.longestRunKm()
        val consistent = baseline.consistentWeeks()
        val distance: Int
        if (unnecessary) {
            distance = 5
            reasons.add(
                "Your recent running history already supports a useful baseline. An additional assessment is optional, not required."
            )
        } else if (
            frequency == null ||
                frequency < 1 ||
                longest == null ||
                longest < 2 ||
                consistent != null && consistent < 2
        ) {
            distance = 1
            reasons.add(
                "For a new or returning routine, a short controlled 1K provides a starting point without asking for an unnecessary maximal 5K."
            )
        } else if (longest < 3 || frequency < 2) {
            distance = 2
            reasons.add("A controlled 2K fits the limited endurance evidence currently available.")
        } else if (longest >= 6 && frequency >= 3 && consistent != null && consistent >= 6) {
            distance = 5
            reasons.add(
                "Your established routine supports offering a 5K assessment if you want one."
            )
        } else {
            distance = 3
            reasons.add(
                "A controlled 3K can fill gaps in an occasional running history without requiring a maximal 5K."
            )
        }
        reasons.add(
            "You may choose any distance from 1K to 5K, choose controlled effort or best effort, or skip the assessment."
        )
        return AssessmentRecommendation(distance, unnecessary, reasons)
    }

    @JvmStatic
    fun analyze(
        result: AssessmentResult,
        activity: ActivityRecord?,
        history: RunnerBaseline,
    ): AssessmentSummary {
        val findings = mutableListOf<String>()
        val seconds = result.duration().getSeconds() + result.duration().getNano() / 1_000_000_000.0
        val averagePace = seconds / result.assessment().distanceKm()
        var first: Double? = null
        var last: Double? = null
        var fade: Double? = null
        var hr: Double? = null
        var cadence: Double? = null
        if (activity != null) {
            require(result.activityId() != null && result.activityId() == activity.id()) {
                "Assessment activity link does not match."
            }
            val metres = activity.distanceMetres()
            val comparable =
                metres != null &&
                    abs(metres - result.assessment().distanceKm() * 1000.0) <=
                        max(50.0, result.assessment().distanceKm() * 20.0)
            if (comparable) {
                hr = activity.averageHeartRate()
                cadence = activity.cadence()
            }
            val analysis = SplitCalculator.calculate(activity, 1000.0)
            val full = mutableListOf<RunSplit>()
            if (comparable && analysis.quality() == SampleQuality.QUALIFIED)
                for (split in analysis.splits()) if (!split.partial()) full.add(split)
            if (full.isNotEmpty() && full.size >= result.assessment().distanceKm()) {
                first = full.first().secondsPerKilometre()
                last = full.last().secondsPerKilometre()
                if (full.size >= 2) {
                    fade = (last - first) / first * 100
                    val variation = (maximum(full) - minimum(full)) / average(full)
                    if (variation <= 0.10)
                        findings.add("Your recorded kilometre paces stayed relatively stable.")
                    else
                        findings.add(
                            "Your recorded kilometre paces varied; terrain, pauses and effort may all contribute."
                        )
                    if (last < first * 0.98)
                        findings.add("The final recorded kilometre was faster than the first.")
                    else if (last > first * 1.05)
                        findings.add(
                            "The final recorded kilometre was slower than the first. One result cannot establish the cause."
                        )
                }
            } else
                findings.add(
                    "Reliable kilometre samples are unavailable, so pace fade and finishing splits have not been invented."
                )
        } else
            findings.add(
                "Only the entered distance, time and optional RPE are known. Splits, HR, cadence and elevation remain unavailable."
            )
        if (result.assessment().mode() == AssessmentMode.CONTROLLED)
            findings.add(
                "This was a controlled assessment, not a claim of maximal race capability."
            )
        else
            findings.add(
                "This was recorded as a best effort, but one short result is not proof of longer-distance endurance."
            )
        if (result.rpe() != null)
            findings.add(
                "Your effort rating is manually entered and kept separate from measured sensor data."
            )
        val weight = 1.0 / (1.0 + history.activityCount() / 3.0)
        findings.add(
            "This assessment is one piece of evidence. Its relative weight decreases as completed running history accumulates."
        )
        findings.add(
            "Training pace prescriptions and race predictions are pending; average assessment pace is not automatically labelled easy pace."
        )
        val confidence =
            when (history.confidence()) {
                BaselineConfidence.STRONG -> BaselineConfidence.STRONG
                BaselineConfidence.MODERATE -> BaselineConfidence.MODERATE
                else -> BaselineConfidence.LOW
            }
        return AssessmentSummary(
            averagePace,
            first,
            last,
            fade,
            hr,
            cadence,
            confidence,
            weight,
            findings,
        )
    }

    private fun maximum(splits: List<RunSplit>): Double {
        var value = 0.0
        for (split in splits) value = max(value, split.secondsPerKilometre())
        return value
    }

    private fun minimum(splits: List<RunSplit>): Double {
        var value = Double.MAX_VALUE
        for (split in splits) value = min(value, split.secondsPerKilometre())
        return value
    }

    private fun average(splits: List<RunSplit>): Double {
        var value = 0.0
        for (split in splits) value += split.secondsPerKilometre()
        return value / splits.size
    }
}
