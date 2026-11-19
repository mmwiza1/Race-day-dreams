package org.racedaydreams.domain

import org.racedaydreams.domain.time.LocalDate

/** Immutable original plan snapshot; future edits cannot mutate earlier prescriptions. */
data class TrainingPlan(
    private var id: String?,
    private val startDate: LocalDate?,
    private val endDate: LocalDate?,
    private var phases: List<TrainingPhase>?,
    private var workouts: List<PlannedWorkout>?,
) {
    init {
        id = DomainChecks.id(id, "plan id")
        if (startDate == null) throw NullPointerException("startDate")
        if (endDate == null) throw NullPointerException("endDate")
        require(endDate.toEpochDay() >= startDate.toEpochDay()) { "Plan end precedes start" }
        phases = DomainChecks.immutableList(phases)
        workouts = DomainChecks.immutableList(workouts)
        for (phase in phases!!) require(
            phase.startDate().toEpochDay() >= startDate.toEpochDay() &&
                phase.endDate().toEpochDay() <= endDate.toEpochDay()
        ) {
            "Phase falls outside plan dates"
        }
        for (workout in workouts!!) require(
            workout.date().toEpochDay() >= startDate.toEpochDay() &&
                workout.date().toEpochDay() <= endDate.toEpochDay()
        ) {
            "Workout falls outside plan dates"
        }
    }

    fun id(): String = id!!

    fun startDate(): LocalDate = startDate!!

    fun endDate(): LocalDate = endDate!!

    fun phases(): List<TrainingPhase> = phases!!

    fun workouts(): List<PlannedWorkout> = workouts!!

    override fun toString(): String =
        "TrainingPlan[id=$id, startDate=$startDate, endDate=$endDate, phases=$phases, workouts=$workouts]"
}
