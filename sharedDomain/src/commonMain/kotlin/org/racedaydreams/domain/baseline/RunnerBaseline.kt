package org.racedaydreams.domain.baseline

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/** Original retained evidence snapshot. Missing values remain unknown. */
data class RunnerBaseline
@JvmOverloads
constructor(
    private val weeklyDistanceKm: Double?,
    private val runsPerWeek: Double?,
    private val longestRunKm: Double?,
    private val typicalRunMinutes: Double?,
    private val easyPaceSecondsPerKm: Double?,
    private val consistentWeeks: Int?,
    private val recent5k: Duration?,
    private val recent10k: Duration?,
    private val recentHalf: Duration?,
    private val activityCount: Int,
    private val weeksWithRuns: Int,
    private val windowStart: Instant?,
    private val windowEnd: Instant?,
    private val lastActivityAt: Instant?,
    private val confidence: BaselineConfidence?,
    private var provenance: Map<String, BaselineProvenance>?,
    private var explanations: List<String>?,
    private var weeklyHistory: List<WeeklyRunningSummary>?,
    private var gaps: List<RecordedTrainingGap>?,
    private val manualRecordedAt: Instant? = null,
) {
    init {
        if (windowStart == null) throw NullPointerException("windowStart")
        if (windowEnd == null) throw NullPointerException("windowEnd")
        require(windowEnd.isAfter(windowStart)) { "Baseline window must have positive length." }
        require(activityCount >= 0 && weeksWithRuns >= 0) { "Evidence counts cannot be negative." }
        ManualBaselineInput.nonNegative(weeklyDistanceKm, "Weekly distance")
        ManualBaselineInput.nonNegative(runsPerWeek, "Runs per week")
        ManualBaselineInput.nonNegative(longestRunKm, "Longest run")
        ManualBaselineInput.nonNegative(typicalRunMinutes, "Typical duration")
        ManualBaselineInput.nonNegative(easyPaceSecondsPerKm, "Easy pace")
        require(consistentWeeks == null || consistentWeeks >= 0) {
            "Consistent weeks cannot be negative."
        }
        measuredTime(recent5k)
        measuredTime(recent10k)
        measuredTime(recentHalf)
        if (confidence == null) throw NullPointerException("confidence")
        provenance = baselineMapSnapshot(provenance ?: throw NullPointerException("provenance"))
        explanations = strictList(explanations)
        weeklyHistory = strictList(weeklyHistory)
        gaps = strictList(gaps)
    }

    fun weeklyDistanceKm(): Double? = weeklyDistanceKm

    fun runsPerWeek(): Double? = runsPerWeek

    fun longestRunKm(): Double? = longestRunKm

    fun typicalRunMinutes(): Double? = typicalRunMinutes

    fun easyPaceSecondsPerKm(): Double? = easyPaceSecondsPerKm

    fun consistentWeeks(): Int? = consistentWeeks

    fun recent5k(): Duration? = recent5k

    fun recent10k(): Duration? = recent10k

    fun recentHalf(): Duration? = recentHalf

    fun activityCount(): Int = activityCount

    fun weeksWithRuns(): Int = weeksWithRuns

    fun windowStart(): Instant = windowStart!!

    fun windowEnd(): Instant = windowEnd!!

    fun lastActivityAt(): Instant? = lastActivityAt

    fun confidence(): BaselineConfidence = confidence!!

    fun provenance(): Map<String, BaselineProvenance> = provenance!!

    fun explanations(): List<String> = explanations!!

    fun weeklyHistory(): List<WeeklyRunningSummary> = weeklyHistory!!

    fun gaps(): List<RecordedTrainingGap> = gaps!!

    fun manualRecordedAt(): Instant? = manualRecordedAt

    override fun toString(): String =
        "RunnerBaseline[weeklyDistanceKm=$weeklyDistanceKm, runsPerWeek=$runsPerWeek, longestRunKm=$longestRunKm, typicalRunMinutes=$typicalRunMinutes, easyPaceSecondsPerKm=$easyPaceSecondsPerKm, consistentWeeks=$consistentWeeks, recent5k=$recent5k, recent10k=$recent10k, recentHalf=$recentHalf, activityCount=$activityCount, weeksWithRuns=$weeksWithRuns, windowStart=$windowStart, windowEnd=$windowEnd, lastActivityAt=$lastActivityAt, confidence=$confidence, provenance=$provenance, explanations=$explanations, weeklyHistory=$weeklyHistory, gaps=$gaps, manualRecordedAt=$manualRecordedAt]"

    companion object {
        const val WEEKLY_DISTANCE = "weeklyDistanceKm"
        const val RUNS_PER_WEEK = "runsPerWeek"
        const val LONGEST_RUN = "longestRunKm"
        const val TYPICAL_DURATION = "typicalRunMinutes"
        const val EASY_PACE = "easyPaceSecondsPerKm"
        const val CONSISTENT_WEEKS = "consistentWeeks"
        const val RECENT_5K = "recent5k"
        const val RECENT_10K = "recent10k"
        const val RECENT_HALF = "recentHalf"

        @JvmStatic
        fun empty(from: Instant?, to: Instant?): RunnerBaseline =
            RunnerBaseline(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                from,
                to,
                null,
                BaselineConfidence.LOW,
                emptyMap(),
                listOf(
                    "No running history is available. You can continue with optional manual details or an assessment."
                ),
                emptyList(),
                emptyList(),
            )

        private fun measuredTime(duration: Duration?) {
            require(
                duration == null ||
                    !duration.isNegative() &&
                        !duration.isZero() &&
                        duration <= Durations.ofHours(48)
            ) {
                "Performance duration must be positive and no more than 48 hours."
            }
        }

        private fun <T> strictList(values: List<T>?): List<T> {
            val rows = values ?: throw NullPointerException("values")
            for (value in rows) if (value == null)
                throw NullPointerException("List values cannot be null.")
            return baselineListSnapshot(rows)
        }
    }
}
