package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.min
import org.racedaydreams.domain.TrainingPreferences
import org.racedaydreams.domain.time.LocalDate

/** Original exhaustive scoring; hard availability is checked before soft placement costs. */
object ConstraintScheduler {
    data class Schedule(
        private val days: List<LocalDate>?,
        private val longRun: LocalDate?,
        private val quality: LocalDate?,
        private val cost: Int,
        private val explanation: String?,
    ) {
        fun days(): List<LocalDate>? = days

        fun longRun(): LocalDate? = longRun

        fun quality(): LocalDate? = quality

        fun cost(): Int = cost

        fun explanation(): String? = explanation

        override fun toString(): String =
            "Schedule[days=$days, longRun=$longRun, quality=$quality, cost=$cost, explanation=$explanation]"
    }

    @JvmStatic
    fun select(
        from: LocalDate,
        to: LocalDate,
        maximum: Int,
        preferences: TrainingPreferences,
        unavailable: Set<LocalDate>,
        hardLowerBody: Set<LocalDate>,
        wantQuality: Boolean,
        raceDate: LocalDate?,
    ): Schedule {
        val candidates = mutableListOf<LocalDate>()
        var date = from
        while (date.toEpochDay() <= to.toEpochDay()) {
            if (
                date.getDayOfWeek() in preferences.runningDays() &&
                    date !in unavailable &&
                    (raceDate == null || date.toEpochDay() < raceDate.minusDays(1).toEpochDay())
            )
                candidates.add(date)
            date = date.plusDays(1)
        }
        var winner =
            Schedule(
                Immutable.list(),
                null,
                null,
                0,
                "No available training days in this part of the week.",
            )
        val count = min(maximum, candidates.size)
        var bestCost = Int.MAX_VALUE
        for (mask in 1 until (1 shl candidates.size)) {
            if (mask.countOneBits() != count) continue
            val days = mutableListOf<LocalDate>()
            for (i in candidates.indices) if (mask and (1 shl i) != 0) days.add(candidates[i])
            for (longDay in days) {
                val qualities = mutableListOf<LocalDate?>(null)
                if (wantQuality)
                    for (d in days) if (abs(d.toEpochDay() - longDay.toEpochDay()) >= 2)
                        qualities.add(d)
                for (quality in qualities) {
                    var cost = 0
                    if (
                        preferences.longRunDay() != null &&
                            longDay.getDayOfWeek() != preferences.longRunDay()
                    )
                        cost += 20
                    if (wantQuality && quality == null) cost += 80
                    for (i in 1 until days.size) if (days[i - 1].plusDays(1) == days[i]) cost += 2
                    for (d in days) {
                        val demanding = d == longDay || d == quality
                        if (demanding && d.minusDays(1) in hardLowerBody) cost += 120
                        if (demanding && d in hardLowerBody) cost += 90
                        if (demanding && d.minusDays(1).getDayOfWeek() in preferences.gymDays())
                            cost += 4
                    }
                    if (cost < bestCost) {
                        bestCost = cost
                        winner =
                            Schedule(
                                Immutable.copyList(days),
                                longDay,
                                quality,
                                cost,
                                "Selected from feasible available days; score $cost (lower is preferred). " +
                                    "Long-run preference, recovery spacing and recorded lower-body work affect placement. " +
                                    "Gym availability alone is a small preference, not evidence of a hard session.",
                            )
                    }
                }
            }
        }
        return winner
    }
}
