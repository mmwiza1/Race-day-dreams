package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.*

/** Evidence-backed suggestions, never an automatic load increment or plan mutation. */
object StrengthProgressionAdvisor {
    enum class Action {
        GATHER_EVIDENCE,
        HOLD,
        CONSIDER_INCREASE,
        REDUCE_OR_SIMPLIFY,
    }

    // The original public record does not validate or defensively copy raw constructor inputs.
    data class Advice(
        private val action: Action?,
        private val reason: String?,
        private val previousKilograms: Double?,
        private val suggestedKilograms: Double?,
        private val options: List<String>?,
    ) {
        fun action(): Action? = action

        fun reason(): String? = reason

        fun previousKilograms(): Double? = previousKilograms

        fun suggestedKilograms(): Double? = suggestedKilograms

        fun options(): List<String>? = options

        override fun toString(): String =
            "Advice[action=$action, reason=$reason, previousKilograms=$previousKilograms, suggestedKilograms=$suggestedKilograms, options=$options]"
    }

    @JvmStatic
    fun review(
        prescription: GymProgramme.Prescription,
        history: List<StrengthSession>,
        asOf: Instant,
    ): Advice {
        val earliest =
            Instants.ofEpochSecond(
                asOf.getEpochSecond() - 28L * 24 * 60 * 60,
                asOf.getNano().toLong(),
            )
        val recent =
            history.filter { !it.startedAt().isBefore(earliest) && !it.startedAt().isAfter(asOf) }
        val anyWorkingEvidence =
            history.any { s ->
                s.exercises().any { e ->
                    e.exercise().id() == prescription.exerciseId() && e.sets().any { !it.warmUp() }
                }
            }
        val result = review(prescription, recent)
        if (result.action() == Action.GATHER_EVIDENCE && anyWorkingEvidence)
            return advice(
                Action.HOLD,
                "Recent comparable strength evidence is needed before increasing the load. Older or future-dated sessions do not count as recent evidence.",
                null,
                null,
            )
        return result
    }

    @JvmStatic
    fun review(prescription: GymProgramme.Prescription, history: List<StrengthSession>): Advice {
        val sessions =
            history.sortedWith(
                compareByDescending<StrengthSession> { it.startedAt() }.thenBy { it.id() }
            )
        val observations = mutableListOf<List<StrengthSession.SetEntry>>()
        for (session in sessions) {
            val sets = mutableListOf<StrengthSession.SetEntry>()
            for (block in session.exercises()) if (
                block.exercise().id() == prescription.exerciseId()
            )
                for (set in block.sets()) if (!set.warmUp()) sets.add(set)
            if (sets.isNotEmpty()) {
                observations.add(sets)
                if (observations.size == 2) break
            }
        }
        if (observations.isEmpty())
            return advice(
                Action.GATHER_EVIDENCE,
                "Record working sets first. No load has been inferred.",
                null,
                null,
            )
        val latest = observations[0]
        val load = latest[0].kilograms()
        if (latest.size < prescription.sets())
            return advice(
                Action.HOLD,
                "The latest session had fewer working sets than prescribed. Hold the load and review how the session felt.",
                load,
                null,
            )
        if (
            latest.any { s ->
                s.coachingRpe()?.let { it > prescription.maximumRpe() + .5 } == true ||
                    s.reps() > 0 && s.reps() < prescription.minimumReps()
            }
        )
            return advice(
                Action.REDUCE_OR_SIMPLIFY,
                "Recent working sets were harder or shorter than the intended range. Consider less load or fewer sets.",
                load,
                null,
            )
        if (observations.size < 2)
            return advice(
                Action.HOLD,
                "One completed session is useful evidence; repeat before considering progression.",
                load,
                null,
            )
        if (prescription.minimumReps() == 0)
            return advice(
                Action.HOLD,
                "Time and distance progression needs repeated task-specific evidence; retain the current prescription.",
                load,
                null,
            )
        val top =
            observations.all { sets ->
                sets.size >= prescription.sets() &&
                    sets.all { s ->
                        s.reps() >= prescription.maximumReps() &&
                            s.coachingRpe()?.let { it <= prescription.maximumRpe() } == true &&
                            s.kilograms() == load
                    }
            }
        if (top && load > 0 && roundedIncrease(load) > load && roundedIncrease(load) <= 1500)
            return advice(
                Action.CONSIDER_INCREASE,
                "Two recorded sessions reached the top of the rep range at the intended effort and the same load. A small increase is an option, not a requirement.",
                load,
                roundedIncrease(load),
            )
        return advice(
            Action.HOLD,
            "Current evidence supports repeating the load; missing effort is not assumed to be easy.",
            load,
            null,
        )
    }

    // Math.round's positive half-up rule, preserving the original multiply order. Set loads are
    // validated finite and <=1500 kg, far below Long saturation or precision limits here.
    private fun roundedIncrease(load: Double): Double = floor(load * 1.025 * 2 + .5).toLong() / 2.0

    private fun advice(action: Action, reason: String, prior: Double?, suggested: Double?): Advice =
        Advice(
            action,
            reason,
            prior,
            suggested,
            if (action == Action.CONSIDER_INCREASE)
                Immutable.list("Keep current load", "Try a small increase")
            else Immutable.list("Keep current prescription", "Use a lighter or simpler option"),
        )
}
