package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic
import kotlin.math.*
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Durations

/** Original conservative evidence matching. Similarity never implies a physiological outcome. */
object ComparableRuns {
    data class Match(
        private val activity: ActivityRecord?,
        private val confidence: String?,
        private val reasons: List<String>?,
        private val relevance: Double,
    ) {
        fun activity(): ActivityRecord? = activity

        fun confidence(): String? = confidence

        fun reasons(): List<String>? = reasons

        fun relevance(): Double = relevance

        override fun toString(): String =
            "Match[activity=$activity, confidence=$confidence, reasons=$reasons, relevance=$relevance]"
    }

    @JvmStatic
    fun find(
        current: ActivityRecord,
        history: List<ActivityRecord>,
        prescriptions: List<PlannedWorkout>,
    ): List<Match> {
        val types = mutableMapOf<String, WorkoutCategory>()
        for (p in prescriptions) types[p.id()] = p.category()
        val out = mutableListOf<Match>()
        val currentDistance = current.distanceMetres()
        if (currentDistance == null || currentDistance < 500 || !plausible(current))
            return Immutable.list()
        val purpose = types[current.plannedWorkoutId()]
        for (prior in history) {
            val priorDistance = prior.distanceMetres()
            if (
                !plausible(prior) ||
                    prior.id() == current.id() ||
                    !prior.start().isBefore(current.start()) ||
                    priorDistance == null
            )
                continue
            val delta = abs(priorDistance - currentDistance) / currentDistance
            if (delta > .12) continue
            val durationDelta =
                abs(prior.elapsedTime().toMillis() - current.elapsedTime().toMillis()) /
                    current.elapsedTime().toMillis().toDouble()
            if (durationDelta > .35) continue
            val other = types[prior.plannedWorkoutId()]
            if (purpose != null && other != null && purpose != other) continue
            val currentElevation = current.elevationGainMetres()
            val priorElevation = prior.elevationGainMetres()
            if (
                currentElevation != null &&
                    priorElevation != null &&
                    abs(currentElevation - priorElevation) > max(40.0, currentDistance * .015)
            )
                continue
            // Original Duration.toDays uses integral seconds, truncated towards zero.
            val age = Durations.between(prior.start(), current.start()).getSeconds() / 86400
            if (age > 365) continue
            val reasons =
                mutableListOf(
                    "Distance within ${roundedNanos(delta * 100)}%",
                    "Similar elapsed duration",
                )
            var score = 50 - delta * 100 - durationDelta * 25 - min(age, 180L) / 18.0
            val known = purpose != null && purpose == other
            if (known) {
                score += 25
                reasons.add("Same planned workout type")
            }
            if (currentElevation != null && priorElevation != null) {
                score += 5
                reasons.add("Similar recorded elevation gain")
            }
            if (!known && (delta > .05 || durationDelta > .15)) continue
            out.add(
                Match(
                    prior,
                    if (known && delta <= .05) "Strong" else "Moderate",
                    Immutable.copyList(reasons),
                    score,
                )
            )
        }
        out.sortWith(compareByDescending<Match> { it.relevance() }.thenBy { it.activity()!!.id() })
        return Immutable.copyList(out)
    }

    private fun plausible(a: ActivityRecord): Boolean {
        val distance = a.distanceMetres() ?: return false
        if (distance <= 0) return false
        val pace = a.elapsedTime().toMillis() / distance
        return pace >= 100 && pace <= 3600
    }

    @JvmStatic
    fun interpretation(current: ActivityRecord, prior: ActivityRecord): String {
        val distance = current.distanceMetres()
        val priorDistance = prior.distanceMetres()
        if (distance == null || priorDistance == null) return "Distance evidence is unavailable."
        val pace = current.elapsedTime().toMillis() / distance
        val old = prior.elapsedTime().toMillis() / priorDistance
        var speed =
            if (abs(pace - old) < .6) "Similar average pace"
            else if (pace < old) "Faster average pace" else "Slower average pace"
        val currentEffort = current.rpe()
        val priorEffort = prior.rpe()
        if (currentEffort != null && priorEffort != null) {
            val effort = currentEffort - priorEffort
            speed +=
                if (effort >= 1) " with higher reported effort"
                else if (effort <= -1) " with lower reported effort"
                else " with similar reported effort"
        } else speed += "; reported effort is incomplete"
        return "$speed. This comparison alone does not establish a fitness change."
    }
}
