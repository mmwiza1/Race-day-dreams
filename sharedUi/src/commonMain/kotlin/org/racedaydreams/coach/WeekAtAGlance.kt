package org.racedaydreams.coach

import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/**
 * Builds the days for Home's strip from existing records only: the running plan, the gym schedule
 * and recorded sessions. Home looks forward (today and the next six calendar days, today first);
 * Monday–Sunday weekly figures stay in Plan, Calendar and Progress. Completion uses the same rules
 * as the calendar (a run linked to its planned workout; a gym link to a scheduled session). Nothing
 * is inferred: a day with no plan and no record is a rest day.
 */
object WeekAtAGlance {
    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val months =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "Nov", for chart month labels. */
    fun monthShort(date: LocalDate): String = months[date.getMonthValue() - 1]

    private val monthNames =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        )

    fun weekStart(today: LocalDate): LocalDate =
        today.minusDays((today.getDayOfWeek().getValue() - 1).toLong())

    fun weekday(date: LocalDate): String = weekdays[date.getDayOfWeek().getValue() - 1]

    /** "Sun 22 Nov", for the strip's range line and the selected day's heading. */
    fun dayLabel(date: LocalDate): String =
        "${weekday(date)} ${date.getDayOfMonth()} ${months[date.getMonthValue() - 1]}"

    /** "Sunday 22 November", for screen readers. */
    fun spokenDay(date: LocalDate): String =
        "${spokenWeekdays[date.getDayOfWeek().getValue() - 1]} ${date.getDayOfMonth()} " +
            monthNames[date.getMonthValue() - 1]

    private val spokenWeekdays =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /** "Sun 22 Nov – Sat 28 Nov"; years are added when the days cross into a new year. */
    fun range(days: List<WeekDay>): String {
        if (days.isEmpty()) return ""
        val first = days.first().date
        val last = days.last().date
        return if (first.getYear() == last.getYear()) "${dayLabel(first)} – ${dayLabel(last)}"
        else "${dayLabel(first)} ${first.getYear()} – ${dayLabel(last)} ${last.getYear()}"
    }

    /** Today and the following six calendar days, today always first. */
    fun nextSevenDays(
        state: CoachUiState,
        today: LocalDate,
        dateOf: ((Instant) -> LocalDate)?,
        workoutName: (WorkoutCategory) -> String,
    ): List<WeekDay> = build(state, today, today, dateOf, workoutName)

    private fun build(
        state: CoachUiState,
        start: LocalDate,
        today: LocalDate,
        dateOf: ((Instant) -> LocalDate)?,
        workoutName: (WorkoutCategory) -> String,
    ): List<WeekDay> {
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
            WeekDay(day, weekday(day), sessions)
        }
    }

    /**
     * What is still ahead in the shown days, e.g. "3 runs planned" or "4 planned: 3 runs, 1
     * strength". Sessions already done today are not counted as upcoming. Null when nothing is
     * shown.
     */
    fun summary(days: List<WeekDay>): String? {
        val sessions = days.flatMap { it.sessions }
        if (sessions.isEmpty()) return null
        val planned = sessions.filter { it.mark == SessionMark.PLANNED }
        val runs = planned.count { it.discipline == Discipline.RUNNING }
        val strength = planned.count { it.discipline == Discipline.STRENGTH }
        return when {
            planned.isEmpty() -> "Nothing else planned"
            strength == 0 -> "${plural(runs, "run")} planned"
            runs == 0 ->
                "$strength strength ${if (strength == 1) "session" else "sessions"} planned"
            else -> "${planned.size} planned: ${plural(runs, "run")}, $strength strength"
        }
    }
}
