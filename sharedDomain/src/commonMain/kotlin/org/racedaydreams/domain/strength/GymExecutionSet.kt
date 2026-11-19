package org.racedaydreams.domain.strength

import org.racedaydreams.domain.coaching.Immutable

/** One entered actual, frozen with the exercise definition used for that set. */
data class GymExecutionSet(
    private val slotId: String?,
    private val exercise: StrengthExercise?,
    private val actual: StrengthSession.SetEntry?,
) {
    init {
        GymWorkoutDefinition.requireId(slotId, "exercise slot")
        if (exercise == null) throw NullPointerException("exercise")
        if (actual == null) throw NullPointerException("actual set")
        StrengthSession.ExerciseSets(exercise, Immutable.list(actual))
    }

    fun slotId(): String = slotId!!

    fun exercise(): StrengthExercise = exercise!!

    fun actual(): StrengthSession.SetEntry = actual!!

    override fun toString(): String =
        "GymExecutionSet[slotId=$slotId, exercise=$exercise, actual=$actual]"
}
