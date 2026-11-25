package org.racedaydreams.domain.coaching

import kotlin.math.pow
import org.racedaydreams.domain.GoalMode
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.RaceGoal
import org.racedaydreams.domain.TrainingPlan
import org.racedaydreams.domain.TrainingPreferences
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.baseline.RunnerBaseline

/**
 * Honest race-readiness check for a generated plan (training Piece 1).
 *
 * Several pieces of evidence must agree before a plan is rated Supported:
 * - endurance: the longest run, repeated long-run exposure and the peak weekly distance;
 * - time to build up before race day;
 * - running days per week;
 * - prescribed distances (run/walk sessions show time on feet, not continuous running);
 * - for a target time, a recent race result that supports it.
 *
 * Each factor can only lower the rating, and each lowering factor is explained in plain words. The
 * plan itself is never changed or made more aggressive. Reference figures describe what plans for a
 * distance usually build to; they are not safety thresholds, and even Supported is a guide, not a
 * guarantee of race-day readiness or of a finish time.
 */
object PlanFeasibility {
    enum class Level {
        /** The plan follows the usual build-up for the distance on every factor assessed. */
        SUPPORTED,
        /** The plan helps, but one or more factors fall short of the usual build-up. */
        LIMITED,
        /** The plan cannot reasonably prepare the runner to cover this distance on race day. */
        NOT_SUPPORTED,
    }

    class Assessment(
        private val level: Level,
        private val headline: String,
        private val reasons: List<String>,
        private val alternatives: List<String>,
    ) {
        fun level(): Level = level

        fun headline(): String = headline

        fun reasons(): List<String> = reasons

        fun alternatives(): List<String> = alternatives

        /** Always shown with the rating: it is a guide, never a guarantee. */
        fun caveat(): String = CAVEAT

        override fun toString(): String =
            "Assessment[level=$level, headline=$headline, reasons=$reasons, alternatives=$alternatives]"
    }

    const val CAVEAT =
        "This is a general guide based on your plan, not a guarantee of race-day readiness or of a finish time. Your plan won't speed up to catch up, and this isn't medical advice."

    /** Reference values per distance, drawn from published novice plans and the plan reviews. */
    private class Reference(
        /** Longest planned run (km) at which the plan reaches the usual range. */
        val supportedKm: Double,
        /** Below this longest run (km) the plan cannot reasonably prepare for the distance. */
        val minimumKm: Double,
        /** Peak planned week (km) usually reached for the distance. */
        val supportedWeekKm: Double,
        /** Run/walk minutes below which the plan cannot reasonably prepare for the distance. */
        val minimumMinutes: Long,
        /** Usual peak long-run range quoted to the runner. */
        val usualLongRun: String,
        /** Shortest build usually recommended for a runner new to the distance. */
        val usualWeeks: Int,
    )

    private fun reference(distance: RaceDistance): Reference? =
        when (distance) {
            RaceDistance.FIVE_K -> Reference(4.0, 0.0, 12.0, 0, "about 4–8 km", 6)
            RaceDistance.TEN_K -> Reference(8.0, 5.0, 20.0, 20, "about 8–14 km", 8)
            RaceDistance.HALF_MARATHON -> Reference(15.0, 10.0, 28.0, 60, "about 16–19 km", 12)
            RaceDistance.MARATHON -> Reference(26.0, 20.0, 40.0, 120, "about 26–32 km", 16)
            else -> null
        }

    /**
     * Assesses [plan] for [goal]. [baseline] supplies recent race results for a target time;
     * without one a target time cannot be judged. Returns null when there is no race (general
     * fitness).
     */
    @kotlin.jvm.JvmStatic
    @kotlin.jvm.JvmOverloads
    fun assess(
        plan: TrainingPlan,
        goal: RaceGoal,
        preferences: TrainingPreferences?,
        baseline: RunnerBaseline? = null,
    ): Assessment? {
        val race = goal.raceDate() ?: return null
        val ref = reference(goal.distance()) ?: return null
        val label = goal.distance().label()
        val training = plan.workouts().filter { it.category() != WorkoutCategory.RACE }
        val distances = training.mapNotNull { it.distanceKm() }
        val timeOnly = distances.isEmpty()
        val longestKm = distances.maxOrNull() ?: 0.0
        val longestMinutes =
            training.mapNotNull { it.duration()?.getSeconds()?.div(60) }.maxOrNull() ?: 0
        val weeks = ((race.toEpochDay() - plan.startDate().toEpochDay()) / 7).toInt() + 1
        val runs = preferences?.runsPerWeek()
        val longer = goal.distance().kilometres() > 10

        // Every factor below can only lower the rating.
        var level = Level.SUPPORTED
        val reasons = mutableListOf<String>()
        fun lower(to: Level, reason: String) {
            if (to.ordinal > level.ordinal) level = to
            reasons += reason
        }

        if (timeOnly) {
            // Run/walk minutes show time on your feet, not the ability to run continuously.
            lower(
                if (longestMinutes < ref.minimumMinutes) Level.NOT_SUPPORTED else Level.LIMITED,
                "We don't have enough recent running history to plan distances, so every session is run/walk of up to $longestMinutes minutes. That builds the habit, but doesn't yet show you can run the whole $label.",
            )
        } else {
            val peakWeek =
                training
                    .filter { it.distanceKm() != null }
                    .groupBy { (it.date().toEpochDay() - plan.startDate().toEpochDay()) / 7 }
                    .values
                    .maxOfOrNull { week -> week.sumOf { it.distanceKm()!! } } ?: 0.0
            // A single long run is not enough: the usual build repeats long runs near the peak.
            val longRuns = distances.count { it >= ref.supportedKm * .85 }
            if (longestKm < ref.minimumKm)
                lower(
                    Level.NOT_SUPPORTED,
                    "Your longest run in this plan is ${oneDecimal(longestKm)} km. Plans for a $label usually build to ${ref.usualLongRun}.",
                )
            else if (longestKm < ref.supportedKm)
                lower(
                    Level.LIMITED,
                    "Your longest run in this plan is ${oneDecimal(longestKm)} km. Plans for a $label usually build to ${ref.usualLongRun}.",
                )
            else if (longRuns < 2)
                lower(
                    Level.LIMITED,
                    "Your plan reaches ${oneDecimal(longestKm)} km only once. Plans for a $label usually repeat longer runs a few times before race day.",
                )
            if (peakWeek < ref.supportedWeekKm)
                lower(
                    Level.LIMITED,
                    "Your busiest week is ${oneDecimal(peakWeek)} km. Plans for a $label usually reach about ${ref.supportedWeekKm.toInt()} km a week or more.",
                )
        }
        if (weeks < ref.usualWeeks)
            lower(
                Level.LIMITED,
                "There ${if (weeks == 1) "is 1 week" else "are $weeks weeks"} until race day. Plans for a $label usually allow at least ${ref.usualWeeks} weeks to build up gradually.",
            )
        if (runs != null && runs <= 2 && longer)
            lower(
                Level.LIMITED,
                "Running $runs days a week limits how much endurance your plan can build.",
            )

        // Anything above lowers the rating because of preparation; a target time is judged apart.
        val preparation = level
        val preparationShort = preparation != Level.SUPPORTED
        // A target time needs its own evidence: a finish-ready plan doesn't make a target
        // realistic.
        var targetUnsupported = false
        val target = goal.targetTime()
        if (goal.mode() == GoalMode.TARGET_TIME && target != null) {
            val predicted = predictedSeconds(baseline, goal.distance().kilometres())
            val targetSeconds = target.getSeconds().toDouble()
            if (predicted == null) {
                targetUnsupported = true
                // The finish statement must agree with the preparation rating above (audit R-01):
                // only a Supported preparation may say the plan prepares the runner to finish.
                lower(
                    Level.LIMITED,
                    "There's no recent race time to show your target time is realistic. " +
                        when (preparation) {
                            Level.SUPPORTED ->
                                "Your plan prepares you to finish; the target is up to you."
                            Level.LIMITED ->
                                "Your preparation is also limited, so focus on being ready to finish before aiming for a time."
                            Level.NOT_SUPPORTED ->
                                "Your plan can't yet prepare you to finish, so a target time isn't realistic for this race."
                        },
                )
            } else if (targetSeconds < predicted * .97) {
                targetUnsupported = true
                lower(
                    Level.LIMITED,
                    "Your recent race times suggest about ${clock(predicted)} for a $label, so your target of ${clock(targetSeconds)} is ambitious.",
                )
            }
        }

        val alternatives = mutableListOf<String>()
        if (timeOnly)
            alternatives +=
                "Add your recent running (weekly distance, longest run) so your plan can include distances."
        if (preparationShort)
            alternatives += "Choose a later race to give yourself more time to build up."
        if (level == Level.NOT_SUPPORTED || timeOnly)
            shorterDistance(goal.distance())?.let {
                alternatives += "Build towards a ${it.label()} first, then aim for the $label."
            }
        if (preparationShort && runs != null && runs < 4)
            alternatives += "Add a running day if your week allows."
        if (targetUnsupported)
            alternatives +=
                "Run a shorter race first to see where your fitness is, or switch to a completion goal."
        if (timeOnly && goal.distance() == RaceDistance.FIVE_K)
            alternatives += "Run/walk the race. It's a perfectly good way to finish a 5K."
        if (level == Level.NOT_SUPPORTED && longer)
            alternatives +=
                "If you keep this race, plan to run/walk it, and get advice from a qualified coach or health professional."

        val headline =
            when (level) {
                Level.SUPPORTED -> "Your plan follows the usual build-up for a $label."
                Level.LIMITED ->
                    if (targetUnsupported && !preparationShort)
                        "Your plan prepares you to finish a $label, but your target time isn't backed by recent results."
                    else "Your plan gives limited preparation for a $label."
                Level.NOT_SUPPORTED -> "Your plan can't prepare you to run a $label by race day."
            }
        return Assessment(level, headline, reasons.toList(), alternatives.toList())
    }

    /** Riegel-style estimate from the closest recent result; a guide, not a prediction. */
    private fun predictedSeconds(baseline: RunnerBaseline?, km: Double): Double? {
        if (baseline == null) return null
        val results =
            listOfNotNull(
                baseline.recent5k()?.let { 5.0 to it.getSeconds().toDouble() },
                baseline.recent10k()?.let { 10.0 to it.getSeconds().toDouble() },
                baseline.recentHalf()?.let { 21.0975 to it.getSeconds().toDouble() },
            )
        val closest =
            results.minByOrNull { kotlin.math.abs(kotlin.math.ln(it.first / km)) } ?: return null
        return closest.second * (km / closest.first).pow(1.06)
    }

    private fun shorterDistance(distance: RaceDistance): RaceDistance? =
        when (distance) {
            RaceDistance.MARATHON -> RaceDistance.HALF_MARATHON
            RaceDistance.HALF_MARATHON -> RaceDistance.TEN_K
            RaceDistance.TEN_K -> RaceDistance.FIVE_K
            else -> null
        }

    private fun oneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10).toLong()
        return "${tenths / 10}.${tenths % 10}"
    }

    private fun clock(seconds: Double): String {
        val total = kotlin.math.round(seconds).toLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
        else "$m:${s.toString().padStart(2, '0')}"
    }
}
