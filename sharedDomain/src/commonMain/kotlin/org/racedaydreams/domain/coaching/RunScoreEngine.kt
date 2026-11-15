package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min
import org.racedaydreams.domain.*
import org.racedaydreams.domain.activity.*

/** Original execution heuristics, never a fitness, health or scientifically validated score. */
object RunScoreEngine {
    const val RULE_VERSION = 3

    @JvmStatic
    fun analyse(
        planned: PlannedWorkout?,
        actual: ActivityRecord?,
        response: RunExecution?,
    ): RunFeedback {
        if (planned == null)
            return RunFeedback(
                null,
                "Run Analysis",
                Immutable.list(),
                Immutable.list(),
                Immutable.list(),
                "There was no planned workout to compare this run against. Recorded distance, duration, RPE and splits remain useful evidence.",
                "One completed run does not establish a fitness change.",
            )
        require(response == null || response.plannedWorkoutId() == planned.id()) {
            "The response must refer to this prescription."
        }
        val record = actual ?: throw NullPointerException()
        val components = mutableListOf<RunFeedback.Component>()
        val good = mutableListOf<String>()
        val differed = mutableListOf<String>()
        val plannedKm = planned.distanceKm()
        val actualMetres = record.distanceMetres()
        val plannedDuration = planned.duration()
        val ratio =
            if (plannedKm != null && plannedKm > 0 && actualMetres != null)
                actualMetres / (plannedKm * 1000)
            else if (plannedDuration != null && !plannedDuration.isZero())
                record.elapsedTime().toMillis() / plannedDuration.toMillis().toDouble()
            else null
        val completion = ratio?.let { max(0.0, min(1.0, it) - max(0.0, it - 1.15) * .5) }
        components.add(
            RunFeedback.Component(
                "Completion",
                completion,
                if (ratio == null) "Comparable distance or duration is unavailable."
                else
                    "Completed ${roundedNanos(ratio * 100)}% of the prescription; extra distance earns no extra credit.",
            )
        )
        if (ratio != null) {
            if (ratio >= .95 && ratio <= 1.15)
                good.add("You completed approximately the prescribed amount.")
            else
                differed.add(
                    if (ratio < .95)
                        "You completed less than prescribed. That is useful evidence, not a failure."
                    else "This run exceeded its planned amount; more is not automatically better."
                )
        }
        val controlled =
            planned.category() in
                listOf(
                    WorkoutCategory.RECOVERY_RUN,
                    WorkoutCategory.EASY_RUN,
                    WorkoutCategory.ZONE_2_RUN,
                    WorkoutCategory.LONG_RUN,
                )
        val upper =
            if (planned.category() == WorkoutCategory.RECOVERY_RUN) 3.0
            else if (controlled) 4.0
            else if (
                planned.category() == WorkoutCategory.RACE ||
                    planned.category() == WorkoutCategory.TIME_TRIAL
            )
                10.0
            else 8.0
        val lower = if (controlled) 0.0 else 5.0
        val actualRpe = record.rpe()
        val effort =
            actualRpe?.let { max(0.0, 1 - max(0.0, it - upper) * .2 - max(0.0, lower - it) * .12) }
        components.add(
            RunFeedback.Component(
                "Effort match",
                effort,
                if (actualRpe == null) "RPE was not recorded. Heart rate is not required."
                else
                    "Recorded RPE $actualRpe; intended ${if (controlled) "controlled conversational effort" else "controlled quality work"}. RPE and Run Score measure different things.",
            )
        )
        if (effort != null) {
            if (effort >= .9) good.add("Your reported effort matched the session purpose.")
            else differed.add("Your reported effort differed from the intended intensity.")
        }
        var paceMatch: Double? = null
        if (planned.segments().size == 1 && actualMetres != null && actualMetres > 0) {
            val segment = planned.segments().first()
            val fast = segment.minimumSecondsPerKm()
            val slow = segment.maximumSecondsPerKm()
            if (fast != null && slow != null) {
                val pace = record.elapsedTime().toMillis() / actualMetres
                val outside = max(0.0, max(fast - pace, pace - slow))
                paceMatch = max(0.0, 1 - outside / max(1.0, slow) * 4)
            }
        }
        var structuredPace = false
        if (response != null && response.steps().isNotEmpty()) {
            var sumAdherence = 0.0
            var measuredSteps = 0
            for (e in response.steps()) {
                val fast = e.fastPace()
                val slow = e.slowPace()
                if (
                    e.kind() == WorkoutSegment.Kind.RUN &&
                        e.completed() &&
                        e.reliablePace() &&
                        e.metres() > 0 &&
                        e.activeMillis() > 0 &&
                        fast != null &&
                        slow != null
                ) {
                    val actualPace = e.activeMillis() / e.metres()
                    val outside = max(0.0, max(fast - actualPace, actualPace - slow))
                    sumAdherence += max(0.0, 1 - outside / slow * 4)
                    measuredSteps++
                }
            }
            if (measuredSteps > 0) {
                paceMatch = sumAdherence / measuredSteps
                structuredPace = true
            }
        }
        components.add(
            RunFeedback.Component(
                "Target pace",
                paceMatch,
                if (paceMatch == null)
                    "No reliable whole-run pace comparison is available; warm-ups and recoveries are not treated as quality reps."
                else if (structuredPace)
                    "Compared individual completed run segments with their preserved ranges, excluding recoveries and discontinuous GPS evidence."
                else "Compared with the prescribed range, without rewarding extra speed.",
            )
        )
        var stability: Double? = null
        val splits = SplitCalculator.calculate(record, 1000.0)
        if (splits.splits().size >= 3 && planned.category() != WorkoutCategory.INTERVALS) {
            val full = splits.splits().filter { !it.partial() }
            if (full.size >= 3) {
                val first = full.first().duration().toMillis() / full.first().distanceMetres()
                val last = full.last().duration().toMillis() / full.last().distanceMetres()
                val fade = (last - first) / first
                stability =
                    if (planned.category() == WorkoutCategory.PROGRESSION_RUN) {
                        if (last < first * .97) 1.0 else .4
                    } else max(0.0, 1 - max(0.0, fade - .08) * 3)
            }
        }
        components.add(
            RunFeedback.Component(
                "Pacing / objective",
                stability,
                if (stability == null)
                    "Reliable continuous splits are unavailable; pacing stability is not inferred."
                else if (planned.category() == WorkoutCategory.PROGRESSION_RUN)
                    "Compared early and late complete splits for a genuine progression."
                else
                    "Compared early and late complete splits. Terrain, conditions and context can affect this heuristic.",
            )
        )
        if (stability != null) {
            if (planned.category() == WorkoutCategory.PROGRESSION_RUN) {
                if (stability >= .9)
                    good.add(
                        "Your later complete splits were faster than your early splits, supporting the progression objective."
                    )
                else differed.add("Your complete splits did not show the intended progression.")
            } else if (stability >= .9)
                good.add("Retained complete splits did not show substantial late slowing.")
            else
                differed.add(
                    "Your later complete splits were slower. Terrain, conditions and effort context can affect this observation."
                )
        }
        var reps: Double? = null
        val completedReps = response?.completedReps()
        if (
            planned.category() == WorkoutCategory.INTERVALS &&
                response != null &&
                completedReps != null
        ) {
            val prescribed = planned.segments().count { it.kind() == WorkoutSegment.Kind.RUN }
            if (prescribed > 0) reps = min(1.0, completedReps / prescribed.toDouble())
        }
        if (planned.category() == WorkoutCategory.INTERVALS) {
            components.add(
                RunFeedback.Component(
                    "Reps completed",
                    reps,
                    if (reps == null)
                        "Rep completion was not recorded; total distance does not prove reps were completed."
                    else if (response!!.steps().isEmpty())
                        "Uses runner-reported completed reps against the prescribed main sets."
                    else "Uses recorder execution events against the preserved main sets.",
                )
            )
            val recoveries = response?.recoveriesCompleted()
            if (recoveries != null)
                components.add(
                    RunFeedback.Component(
                        "Recoveries",
                        if (recoveries) 1.0 else .4,
                        "Uses runner-reported recovery completion.",
                    )
                )
        }
        var sum = 0.0
        var weight = 0.0
        var known = 0
        for (c in components) {
            val fraction = c.fraction()
            if (fraction != null) {
                val w =
                    when (c.name()) {
                        "Completion" -> 3.0
                        "Effort match" -> 4.0
                        else -> 2.0
                    }
                sum += fraction * w
                weight += w
                known++
            }
        }
        var score: Int? =
            if (known < 2) null else max(1, min(10, roundedNanos(sum / weight * 10).toInt()))
        if (
            score != null &&
                (planned.category() == WorkoutCategory.PROGRESSION_RUN &&
                    stability != null &&
                    stability < .6 ||
                    planned.category() == WorkoutCategory.INTERVALS && reps != null && reps < .8)
        )
            score = min(score, 7)
        return RunFeedback(
            score,
            if (score == null) "Execution evidence incomplete"
            else if (score >= 8) "Good execution" else "Against today's objective",
            components,
            good,
            differed,
            (if (controlled && effort != null && effort < .8)
                "This aerobic session became more demanding than intended. "
            else "This records how the run met its purpose. ") +
                "Completion alone does not prove fitness improved. Missing components are excluded, not scored as zero.",
            if (differed.isEmpty())
                "No change is needed from this run alone. Keep the next session aligned with its purpose."
            else
                "Review the raw evidence and the rest of your week. Any meaningful plan adjustment remains your choice.",
        )
    }
}
