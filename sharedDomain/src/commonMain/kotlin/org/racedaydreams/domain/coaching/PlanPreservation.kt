package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.PlannedWorkout
import org.racedaydreams.domain.TrainingPlan
import org.racedaydreams.domain.time.LocalDate

/** Same-day rebuilds cannot relabel an already completed prescription with new targets. */
object PlanPreservation {
    @JvmStatic
    fun retainCompleted(
        candidate: TrainingPlan,
        completed: Map<String, PlannedWorkout?>,
    ): TrainingPlan {
        val workouts = mutableListOf<PlannedWorkout>()
        val dates = mutableSetOf<LocalDate>()
        for (proposed in candidate.workouts()) {
            // getOrDefault deliberately distinguishes an absent key from a raw null mapping.
            val preserved =
                if (completed.containsKey(proposed.id())) completed[proposed.id()]!! else proposed
            require(preserved.id() == proposed.id() && dates.add(preserved.date())) {
                "Completed prescriptions conflict with the rebuilt schedule. Keep the existing programme and review availability."
            }
            workouts.add(preserved)
        }
        workouts.sortBy { it.date().toEpochDay() }
        return TrainingPlan(
            candidate.id(),
            candidate.startDate(),
            candidate.endDate(),
            candidate.phases(),
            workouts,
        )
    }
}
