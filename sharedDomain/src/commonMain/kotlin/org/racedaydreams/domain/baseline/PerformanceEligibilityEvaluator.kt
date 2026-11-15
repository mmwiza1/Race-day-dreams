package org.racedaydreams.domain.baseline

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/** Original conservative selection heuristics, not medical assurance or a permanent flag. */
object PerformanceEligibilityEvaluator {
    @JvmStatic
    fun evaluate(baseline: RunnerBaseline, now: Instant): EligibilityDecision =
        evaluate(baseline, now, baseline.manualRecordedAt())

    @JvmStatic
    fun evaluate(
        baseline: RunnerBaseline,
        now: Instant,
        manualReviewedAt: Instant?,
    ): EligibilityDecision {
        val reasons = mutableListOf<String>()
        val manual =
            isManual(baseline, RunnerBaseline.WEEKLY_DISTANCE) ||
                isManual(baseline, RunnerBaseline.RUNS_PER_WEEK) ||
                isManual(baseline, RunnerBaseline.LONGEST_RUN) ||
                isManual(baseline, RunnerBaseline.CONSISTENT_WEEKS)
        val lastActivity = baseline.lastActivityAt()
        val recentRecords =
            lastActivity != null &&
                !lastActivity.isAfter(now) &&
                Durations.between(lastActivity, now) <= Durations.ofHours(14 * 24)
        val recentManual =
            manualReviewedAt != null &&
                !manualReviewedAt.isAfter(now) &&
                Durations.between(manualReviewedAt, now) <= Durations.ofHours(14 * 24)
        val entirelyManual =
            isManual(baseline, RunnerBaseline.WEEKLY_DISTANCE) &&
                isManual(baseline, RunnerBaseline.RUNS_PER_WEEK) &&
                isManual(baseline, RunnerBaseline.LONGEST_RUN) &&
                isManual(baseline, RunnerBaseline.CONSISTENT_WEEKS)
        val current =
            if (manual) recentManual && (entirelyManual || recentRecords) else recentRecords
        if (!current)
            reasons.add(
                if (manual)
                    "Review your current running details before choosing Performance; manual review or the supporting recorded evidence is missing or more than two weeks old."
                else
                    "Recent running evidence is missing or more than two weeks old. Performance remains available for reassessment as history builds."
            )
        val distance = baseline.weeklyDistanceKm()
        if (distance == null || distance < 25)
            reasons.add(
                "Performance currently asks for evidence of roughly 25 km or more per observed week."
            )
        val frequency = baseline.runsPerWeek()
        if (frequency == null || frequency < 3)
            reasons.add(
                "Performance currently asks for an established routine of at least three runs per week."
            )
        val longest = baseline.longestRunKm()
        if (longest == null || longest < 8)
            reasons.add("A recent endurance run of around 8 km or more has not yet been supported.")
        val consistent = baseline.consistentWeeks()
        if (consistent == null || consistent < 6)
            reasons.add(
                "At least six weeks of a consistent routine would give a stronger basis for extra intensity."
            )
        val eligible = reasons.isEmpty()
        if (eligible)
            reasons.add(
                "Your recent workload, routine and endurance evidence support offering Performance. This is an adjustable training preference, not a medical assurance."
            )
        if (manual)
            reasons.add(
                "This decision includes your manually entered history. Imported or completed workouts will refine it over time."
            )
        reasons.add(
            "These thresholds are transparent product heuristics, not universal training rules. Other strategies and runner choices remain available."
        )
        var evidenceAt = if (manual) manualReviewedAt else lastActivity
        if (manual && !entirelyManual) {
            evidenceAt =
                if (evidenceAt == null || lastActivity == null) null
                else if (lastActivity.isBefore(evidenceAt)) lastActivity else evidenceAt
        }
        var deadline =
            if (evidenceAt == null) now
            else
                Instants.ofEpochSecond(
                    checkedAdd(evidenceAt.getEpochSecond(), 14 * 86400L),
                    evidenceAt.getNano().toLong(),
                )
        if (deadline.isBefore(now)) deadline = now
        return EligibilityDecision(eligible, reasons, deadline)
    }

    private fun isManual(baseline: RunnerBaseline, key: String): Boolean =
        baseline.provenance()[key] == BaselineProvenance.MANUAL
}
