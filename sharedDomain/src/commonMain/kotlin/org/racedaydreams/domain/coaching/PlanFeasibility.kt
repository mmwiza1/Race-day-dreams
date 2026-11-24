package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.GoalMode
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.RaceGoal
import org.racedaydreams.domain.TrainingPlan
import org.racedaydreams.domain.TrainingPreferences
import org.racedaydreams.domain.WorkoutCategory

/**
 * Honest race-readiness check for a generated plan (training Piece 1).
 *
 * It compares what the plan actually prescribes before race day with what published plans for the
 * distance usually build to, and explains limits from missing history, few running days or a short
 * build. It never changes the plan and never forces faster progression: the reference figures
 * describe common practice for a distance, not a safety threshold or a promise of a finish.
 */
object PlanFeasibility {
    enum class Level {
        /** The plan reaches the usual long-run range for the distance. */
        SUPPORTED,
        /** The plan helps, but falls short of what plans for this distance usually build to. */
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

        override fun toString(): String =
            "Assessment[level=$level, headline=$headline, reasons=$reasons, alternatives=$alternatives]"
    }

    /** Reference values per distance, drawn from published novice plans and the plan reviews. */
    private class Reference(
        /** Longest planned run (km) at which the plan reaches the usual range. */
        val supportedKm: Double,
        /** Below this longest run (km) the plan cannot reasonably prepare for the distance. */
        val minimumKm: Double,
        /** Run/walk minutes that reach the usual range when no distance can be prescribed. */
        val supportedMinutes: Long,
        /** Below these run/walk minutes the plan cannot reasonably prepare for the distance. */
        val minimumMinutes: Long,
        /** Usual peak long-run range quoted to the runner. */
        val usualLongRun: String,
        /** Shortest build usually recommended for a runner new to the distance. */
        val usualWeeks: Int,
    )

    private fun reference(distance: RaceDistance): Reference? =
        when (distance) {
            RaceDistance.FIVE_K -> Reference(4.0, 0.0, 20, 0, "about 4–8 km", 6)
            RaceDistance.TEN_K -> Reference(8.0, 5.0, 60, 20, "about 8–14 km", 8)
            RaceDistance.HALF_MARATHON -> Reference(15.0, 10.0, 120, 60, "about 16–19 km", 12)
            RaceDistance.MARATHON -> Reference(26.0, 20.0, 180, 120, "about 26–32 km", 16)
            else -> null
        }

    /**
     * Assesses [plan] for [goal]. Returns null when there is no race to prepare for (general
     * fitness or no race date).
     */
    @kotlin.jvm.JvmStatic
    fun assess(plan: TrainingPlan, goal: RaceGoal, preferences: TrainingPreferences?): Assessment? {
        val race = goal.raceDate() ?: return null
        val ref = reference(goal.distance()) ?: return null
        val training = plan.workouts().filter { it.category() != WorkoutCategory.RACE }
        val longestKm = training.mapNotNull { it.distanceKm() }.maxOrNull()
        val longestMinutes =
            training.mapNotNull { it.duration()?.getSeconds()?.div(60) }.maxOrNull()
        val timeOnly = longestKm == null
        val label = goal.distance().label()
        val weeks = ((race.toEpochDay() - plan.startDate().toEpochDay()) / 7).toInt() + 1
        val runs = preferences?.runsPerWeek()

        val level =
            if (timeOnly) {
                val minutes = longestMinutes ?: 0
                if (minutes >= ref.supportedMinutes) Level.SUPPORTED
                else if (minutes >= ref.minimumMinutes) Level.LIMITED else Level.NOT_SUPPORTED
            } else {
                val km = longestKm ?: 0.0
                if (km >= ref.supportedKm) Level.SUPPORTED
                else if (km >= ref.minimumKm) Level.LIMITED else Level.NOT_SUPPORTED
            }

        val reasons = mutableListOf<String>()
        if (timeOnly)
            reasons +=
                "We don't have enough recent running history to prescribe distances, so every session is run/walk of up to ${longestMinutes ?: 0} minutes."
        else
            reasons +=
                "The longest run in this plan is ${oneDecimal(longestKm!!)} km. Plans for a $label usually build to ${ref.usualLongRun}."
        if (weeks < ref.usualWeeks)
            reasons +=
                "There ${if (weeks == 1) "is 1 week" else "are $weeks weeks"} until race day. Plans for a $label usually allow at least ${ref.usualWeeks} weeks to build up gradually."
        if (runs != null && runs <= 2 && goal.distance().kilometres() > 10)
            reasons +=
                "$runs running days a week limit how much endurance the plan can build safely."
        if (goal.mode() == GoalMode.TARGET_TIME && level != Level.SUPPORTED)
            reasons += "Your target time isn't changed, but this preparation makes it ambitious."

        val alternatives = mutableListOf<String>()
        if (level != Level.SUPPORTED) {
            if (timeOnly)
                alternatives +=
                    "Add your recent running (weekly distance, longest run) so the plan can prescribe distances."
            alternatives += "Choose a later race to give yourself more time to build up."
            shorterDistance(goal.distance())?.let {
                alternatives += "Build towards a ${it.label()} first, then aim for the $label."
            }
            if (runs != null && runs < 4) alternatives += "Add a running day if your week allows."
            if (goal.mode() == GoalMode.TARGET_TIME)
                alternatives += "Switch to a completion goal for this race."
            if (
                level == Level.NOT_SUPPORTED &&
                    (goal.distance() == RaceDistance.HALF_MARATHON ||
                        goal.distance() == RaceDistance.MARATHON)
            )
                alternatives +=
                    "If you keep this race, plan to run/walk it, and get advice from a qualified coach or health professional."
        }

        val headline =
            when (level) {
                Level.SUPPORTED -> "This plan builds towards the usual preparation for a $label."
                Level.LIMITED -> "This plan gives limited preparation for a $label."
                Level.NOT_SUPPORTED -> "This plan can't prepare you to run a $label by race day."
            }
        return Assessment(level, headline, reasons.toList(), alternatives.toList())
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
}
