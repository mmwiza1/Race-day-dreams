package org.racedaydreams.domain.strength

import kotlin.jvm.JvmStatic

/** Original bounded Epley estimate. This is never a measured lift or a progression decision. */
object StrengthRecordPolicy {
    @JvmStatic
    fun estimatedOneRepMax(exercise: StrengthExercise?, set: StrengthSession.SetEntry?): Double? {
        val actual = set ?: throw NullPointerException()
        if (actual.reps() < 2) return null
        val definition = exercise ?: throw NullPointerException()
        if (
            !definition.estimateOneRepMax() ||
                definition.mode() != StrengthExercise.Mode.WEIGHTED ||
                actual.warmUp() ||
                actual.reps() > 10 ||
                actual.kilograms() <= 0
        )
            return null
        return actual.kilograms() * (1 + actual.reps() / 30.0)
    }
}
