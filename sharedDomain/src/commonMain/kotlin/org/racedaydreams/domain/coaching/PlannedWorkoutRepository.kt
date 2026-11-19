package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.PlannedWorkout

/** Revisioned reminder mirror; actual recorded activity stays separate. */
interface PlannedWorkoutRepository {
    fun plannedWorkouts(): List<PlannedWorkout>

    fun plannedWorkoutRevision(id: String): Long

    fun savePlannedWorkout(workout: PlannedWorkout)

    fun deletePlannedWorkout(id: String)
}
