package org.racedaydreams.domain.baseline

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.*

/** Optional runner answers: null remains unknown and no reference becomes recorded evidence. */
data class ManualBaselineInput(
    private val weeklyDistanceKm: Double?,
    private val runsPerWeek: Double?,
    private val longestRunKm: Double?,
    private val recent5k: Duration?,
    private val recent10k: Duration?,
    private val recentHalf: Duration?,
    private val easyPaceSecondsPerKm: Double?,
    private val consistentWeeks: Int?,
) {
    init {
        nonNegative(weeklyDistanceKm, "Weekly distance")
        nonNegative(runsPerWeek, "Runs per week")
        require(runsPerWeek == null || runsPerWeek <= 21) { "Check the number of runs per week." }
        nonNegative(longestRunKm, "Longest recent run")
        positiveTime(recent5k)
        positiveTime(recent10k)
        positiveTime(recentHalf)
        require(
            easyPaceSecondsPerKm == null ||
                easyPaceSecondsPerKm.isFinite() && easyPaceSecondsPerKm > 0
        ) {
            "Easy pace must be a positive time per kilometre."
        }
        require(consistentWeeks == null || consistentWeeks >= 0) {
            "Consistent weeks cannot be negative."
        }
    }

    fun weeklyDistanceKm(): Double? = weeklyDistanceKm

    fun runsPerWeek(): Double? = runsPerWeek

    fun longestRunKm(): Double? = longestRunKm

    fun recent5k(): Duration? = recent5k

    fun recent10k(): Duration? = recent10k

    fun recentHalf(): Duration? = recentHalf

    fun easyPaceSecondsPerKm(): Double? = easyPaceSecondsPerKm

    fun consistentWeeks(): Int? = consistentWeeks

    fun hasAnyValue(): Boolean =
        weeklyDistanceKm != null ||
            runsPerWeek != null ||
            longestRunKm != null ||
            recent5k != null ||
            recent10k != null ||
            recentHalf != null ||
            easyPaceSecondsPerKm != null ||
            consistentWeeks != null

    override fun toString(): String =
        "ManualBaselineInput[weeklyDistanceKm=$weeklyDistanceKm, runsPerWeek=$runsPerWeek, longestRunKm=$longestRunKm, recent5k=$recent5k, recent10k=$recent10k, recentHalf=$recentHalf, easyPaceSecondsPerKm=$easyPaceSecondsPerKm, consistentWeeks=$consistentWeeks]"

    companion object {
        @JvmStatic
        fun empty(): ManualBaselineInput =
            ManualBaselineInput(null, null, null, null, null, null, null, null)

        @JvmStatic
        fun nonNegative(value: Double?, label: String) {
            require(value == null || value.isFinite() && value >= 0) {
                "$label cannot be negative or non-finite."
            }
        }

        @JvmStatic
        fun positiveTime(value: Duration?) {
            require(
                value == null ||
                    !value.isZero() &&
                        !value.isNegative() &&
                        value.getNano() == 0 &&
                        value <= Durations.ofHours(48)
            ) {
                "Recent performance times must be positive whole seconds, up to 48 hours."
            }
        }
    }
}
