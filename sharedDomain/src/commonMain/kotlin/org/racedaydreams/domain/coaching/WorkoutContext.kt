package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.*

/** Original briefing and one-off previews. Recurring availability never changes here. */
object WorkoutContext {
    data class Briefing(private val title: String?, private val observations: List<String>?) {
        fun title(): String? = title

        fun observations(): List<String>? = observations

        override fun toString(): String = "Briefing[title=$title, observations=$observations]"
    }

    @JvmStatic
    fun briefing(
        workout: PlannedWorkout,
        plan: List<PlannedWorkout>,
        actual: List<ActivityRecord>,
        strength: List<StrengthSession>,
        review: WeeklyCoaching.Review?,
    ): Briefing {
        val reasons = mutableListOf<String>()
        val hardGym = workout.date().minusDays(1) in StrengthAnalytics.hardLowerBodyDates(strength)
        if (hardGym)
            reasons.add(
                "Hard lower-body strength was recorded yesterday. Start conservatively and adjust if your legs feel unusually fatigued."
            )
        val similar =
            plan
                .filter {
                    it.category() == workout.category() && !coachingAfter(it.date(), workout.date())
                }
                .map { it.id() }
                .toSet()
        val recent =
            actual
                .filter {
                    it.plannedWorkoutId() in similar &&
                        coachingBefore(coachingDate(it), workout.date())
                }
                .sortedByDescending { it.start() }
        val high = recent.take(3).count { it.rpe()?.let { rpe -> rpe >= 8 } == true }
        if (high >= 2)
            reasons.add(
                "Recent similar sessions had high reported effort. Keep the opening work controlled."
            )
        if (review?.channels()?.any { it.state() == WeeklyCoaching.State.NEEDS_ATTENTION } == true)
            reasons.add(
                "Your weekly review has evidence worth checking before this session. Recommendations remain optional."
            )
        if (reasons.isEmpty())
            reasons.add(
                if (recent.isEmpty())
                    "We are still gathering similar-session evidence. Begin at the intended effort and adjust to conditions."
                else
                    "Recent comparable sessions are available. Keep the opening work controlled; completion alone does not prove fitness improved."
            )
        return Briefing(
            if (hardGym || high >= 2) "Take care with today's intensity"
            else if (recent.isEmpty()) "Start with the planned effort"
            else "Keep today's purpose in focus",
            Immutable.copyList(reasons),
        )
    }

    @JvmStatic
    fun moveWarnings(
        workout: PlannedWorkout,
        proposed: LocalDate?,
        plan: List<PlannedWorkout>,
        strength: List<StrengthSession>,
    ): List<String> = moveWarnings(workout, proposed, plan, strength, null)

    @JvmStatic
    fun moveWarnings(
        workout: PlannedWorkout,
        proposed: LocalDate?,
        plan: List<PlannedWorkout>,
        strength: List<StrengthSession>,
        gym: GymProgramme?,
    ): List<String> {
        val out = mutableListOf<String>()
        if (proposed == null) return Immutable.list("Choose a valid date.")
        if (
            proposed.minusDays(1) in StrengthAnalytics.hardLowerBodyDates(strength) &&
                GymProgrammeGenerator.demanding(workout.category())
        )
            out.add(
                "This places a demanding run immediately after recorded hard lower-body strength."
            )
        for (other in plan) if (
            other.id() != workout.id() &&
                GymProgrammeGenerator.demanding(other.category()) &&
                GymProgrammeGenerator.demanding(workout.category()) &&
                abs(proposed.toEpochDay() - other.date().toEpochDay()) == 1L
        )
            out.add("A demanding running session is on the neighbouring day.")
        if (gym != null && GymProgrammeGenerator.demanding(workout.category()))
            for (session in gym.sessions()) if (
                (session.date() == proposed || session.date() == proposed.minusDays(1)) &&
                    session.exercises().any {
                        ExerciseCatalog.definition(it.exerciseId())?.exercise()?.region() ==
                            StrengthExercise.Region.LOWER
                    }
            )
                out.add(
                    "A programmed lower-body strength session neighbours this date. It has not necessarily been completed; consider the trade-off before moving."
                )
        if (out.isEmpty())
            out.add(
                "No neighbouring conflict was found in the available evidence. This is not medical readiness advice."
            )
        out.add(
            "Only this workout will move. Your normal training days and other workouts stay unchanged."
        )
        return Immutable.copyList(out)
    }
}
