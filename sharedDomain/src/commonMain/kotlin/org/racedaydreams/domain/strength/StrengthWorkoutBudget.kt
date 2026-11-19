package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic
import kotlin.math.ceil
import kotlin.math.floor

/** Original transparent planning estimate, not measured exercise speed or actual elapsed time. */
object StrengthWorkoutBudget {
    @JvmStatic
    fun exerciseMinutes(target: GymWorkoutDefinition.Target?): Int {
        require(target != null) { "An open exercise has no calculable time budget." }
        val metres = target.metres()
        val working =
            target.seconds()
                ?: if (metres != null) {
                    val value = metres * 2
                    val whole = floor(value)
                    whole.toLong() + if (value - whole >= 0.5) 1 else 0
                } else target.maximumReps()!!.toLong() * 4
        val rest = target.restSeconds()?.toLong() ?: 60L
        return ceil((target.sets() * working + maxOf(0, target.sets() - 1) * rest + 60) / 60.0)
            .toInt()
    }

    @JvmStatic
    fun workoutMinutes(slots: List<GymWorkoutDefinition.Slot>): Int =
        4 + slots.sumOf { exerciseMinutes(it.target()) }

    @JvmStatic
    fun explanation(): String =
        "Planning estimate: four minutes to prepare, four seconds per repetition or two seconds per metre, prescribed duration, rest between sets (one minute when unspecified), and one minute per exercise change. Actual time varies."
}
