package org.racedaydreams.coach

import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Builds the seven days of the current week (Monday first) for the week strip from existing records
 * only: the running plan, the gym schedule and recorded sessions. Completion uses the same rules as
 * the calendar (a run linked to its planned workout; a gym link to a scheduled session). Nothing is
 * inferred: a day with no plan and no record is simply empty.
 */
object WeekAtAGlance {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun weekStart(today: LocalDate): LocalDate =
        today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())

    fun build(
        state: CoachUiState,
        today: LocalDate,
        dateOf: ((Instant) -> LocalDate)?,
        workoutName: (WorkoutCategory) -> String,
    ): List<WeekDay> {
        val start = weekStart(today)
        val plan = state.currentPlan?.plan()?.workouts().orEmpty()
        val planIds = plan.map { it.id() }.toSet()
        val completedRuns =
            (state.activities.mapNotNull { it.plannedWorkoutId() } +
                    state.runResponses.mapNotNull { it.plannedWorkoutId() })
                .toSet()
        val gym = programmeSchedule(state)
        return (0 until 7).map { offset ->
            val day = start.plusDays(offset.toLong())
            val past = day.toEpochDay() < today.toEpochDay()
            val sessions = mutableListOf<DaySession>()
            for (workout in plan.filter { it.date() == day }) {
                sessions +=
                    DaySession(
                        Discipline.RUNNING,
                        when {
                            workout.id() in completedRuns -> SessionMark.DONE
                            past -> SessionMark.MISSED
                            else -> SessionMark.PLANNED
                        },
                        workoutName(workout.category()),
                    )
            }
            if (dateOf != null)
                state.activities
                    .filter {
                        dateOf(it.start()) == day &&
                            it.plannedWorkoutId().let { id -> id == null || id !in planIds }
                    }
                    .forEach { sessions += DaySession(Discipline.RUNNING, SessionMark.DONE, "Run") }
            var linkedStrength = 0
            gym?.let { (scheduled, completed) ->
                for ((id, date) in scheduled.distinctBy { it.first }) {
                    if (date != day) continue
                    val done = id in completed
                    if (done) linkedStrength++
                    sessions +=
                        DaySession(
                            Discipline.STRENGTH,
                            when {
                                done -> SessionMark.DONE
                                past -> SessionMark.MISSED
                                else -> SessionMark.PLANNED
                            },
                            "Strength session",
                        )
                }
            }
            if (dateOf != null) {
                val recorded = state.strengthSessions.count { dateOf(it.startedAt()) == day }
                repeat((recorded - linkedStrength).coerceAtLeast(0)) {
                    sessions +=
                        DaySession(Discipline.STRENGTH, SessionMark.DONE, "Strength workout")
                }
            }
            WeekDay(day, weekdays[offset], sessions)
        }
    }

    /**
     * "2 of 4 sessions done" for the planned sessions of the week, or null with nothing planned.
     */
    fun summary(days: List<WeekDay>): String? {
        val planned = days.flatMap { it.sessions }
        if (planned.isEmpty()) return null
        val done = planned.count { it.mark == SessionMark.DONE }
        return "$done of ${planned.size} done"
    }
}
