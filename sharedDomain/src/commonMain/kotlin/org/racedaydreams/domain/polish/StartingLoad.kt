package org.racedaydreams.domain.polish

import kotlin.jvm.JvmStatic
import kotlin.math.floor
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.*

/** Reference only, never an actual set or automatic progression. */
object StartingLoad {
    data class Reference(
        private val lowKg: Double,
        private val highKg: Double,
        private val reason: String?,
    ) {
        fun lowKg(): Double = lowKg

        fun highKg(): Double = highKg

        fun reason(): String? = reason

        override fun toString(): String = "Reference[lowKg=$lowKg, highKg=$highKg, reason=$reason]"
    }

    @JvmStatic
    fun suggest(
        p: GymProgramme.Prescription,
        history: List<StrengthSession>,
        now: Instant,
    ): Reference? {
        if (p.minimumReps() < 1) return null
        val eligible =
            history
                .filter {
                    !it.startedAt().isAfter(now) &&
                        !it.startedAt()
                            .isBefore(
                                Instants.ofEpochSecond(
                                    now.getEpochSecond() - 28L * 24 * 60 * 60,
                                    now.getNano().toLong(),
                                )
                            )
                }
                .sortedByDescending { it.startedAt() }
        for (session in eligible) {
            val loads = mutableListOf<Double>()
            for (exercise in session.exercises()) if (
                exercise.exercise().id() == p.exerciseId() &&
                    exercise.exercise().mode() == StrengthExercise.Mode.WEIGHTED
            )
                for (set in exercise.sets()) {
                    val effort = set.coachingRpe()
                    if (
                        !set.warmUp() &&
                            set.kilograms() > 0 &&
                            set.reps() >= p.minimumReps() &&
                            set.reps() <= p.maximumReps() + 2 &&
                            effort != null &&
                            effort >= 4 &&
                            effort <= p.maximumRpe()
                    )
                        loads.add(set.kilograms())
                }
            if (loads.size >= 2) {
                val proven = loads.min()
                return Reference(
                    floor(proven * .95 * 2) / 2,
                    proven,
                    "Optional reference from ${loads.size} recent exact-exercise working sets at recorded effort. Keep another manageable load if preferred.",
                )
            }
        }
        return null
    }
}
