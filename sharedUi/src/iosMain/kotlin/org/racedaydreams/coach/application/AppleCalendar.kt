package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.*
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.reminders.KotlinxReminderCalendar

internal object AppleCalendarDates : CoachCalendarDates {
    override fun today(): LocalDate =
        KotlinxReminderCalendar.at(AppleRecordingClock.now(), TimeZone.currentSystemDefault().id)
            .date

    override fun parse(value: String): LocalDate =
        kotlinx.datetime.LocalDate.parse(value).let {
            CalendarDates.of(it.year, it.monthNumber, it.dayOfMonth)
        }

    override fun shiftMonth(date: LocalDate, months: Long): LocalDate {
        val shifted = date.getYear().toLong() * 12 + date.getMonthValue() - 1 + months
        val start = CalendarDates.of(shifted.floorDiv(12L).toInt(), shifted.mod(12L).toInt() + 1, 1)
        val next =
            if (start.getMonthValue() == 12) CalendarDates.of(start.getYear() + 1, 1, 1)
            else CalendarDates.of(start.getYear(), start.getMonthValue() + 1, 1)
        return start.plusDays(
            (date.getDayOfMonth() - 1L).coerceAtMost(next.toEpochDay() - start.toEpochDay() - 1)
        )
    }

    override fun monthStart(date: LocalDate): LocalDate =
        CalendarDates.of(date.getYear(), date.getMonthValue(), 1)

    override fun weekStart(date: LocalDate): LocalDate =
        date.minusDays(date.getDayOfWeek().getValue() - 1L)

    override fun label(date: LocalDate): String =
        "${date.getDayOfMonth()} ${monthName(date).take(3)} ${date.getYear()}"

    override fun monthLabel(date: LocalDate): String = "${monthName(date)} ${date.getYear()}"

    private fun monthName(date: LocalDate): String = months[date.getMonthValue() - 1]

    private val months =
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
}

@Composable
internal fun AppleCalendar(
    session: AppleCoachSession<*>,
    initialWorkout: String?,
    initialStrength: String?,
    startPlanned: (String) -> Unit,
    activity: (String) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var plannedId by remember(initialWorkout) { mutableStateOf(initialWorkout) }
    var strengthDate by remember(initialStrength) { mutableStateOf(initialStrength) }
    val runId = plannedId
    val gymDate = strengthDate
    when {
        runId != null ->
            CoachPage {
                CoachButton("Back to calendar") { plannedId = null }
                val workout =
                    (state.currentPlan?.plan()?.workouts() ?: state.plannedWorkouts).firstOrNull {
                        it.id() == runId
                    }
                if (workout == null) Text("This planned workout is unavailable.")
                else {
                    CoachHeading(CalendarData.label(workout.category()))
                    Text(AppleCalendarDates.label(workout.date()))
                    workout.distanceKm()?.let { Text(distanceLabel(it * 1_000, imperial)) }
                    workout.duration()?.let { Text("${it.getSeconds() / 60} min planned") }
                    val completed =
                        state.runResponses.any { it.plannedWorkoutId() == runId } ||
                            state.activities.any { it.plannedWorkoutId() == runId }
                    if (state.currentPlan != null && !completed) {
                        CoachButton("Start with Live Coach") { startPlanned(runId) }
                    }
                    workout.segments().forEach { segment ->
                        CoachCard {
                            Text(
                                segment.kind().name.lowercase().replace('_', ' ').replaceFirstChar {
                                    it.uppercase()
                                }
                            )
                            segment.duration()?.let { Text("${it.getSeconds() / 60} min") }
                            segment.distanceKm()?.let { Text(distanceLabel(it * 1_000, imperial)) }
                            segment.rpe()?.let {
                                Text(
                                    "Aim for a ${StrengthPrescriptionText.effortDescription(it)} effort."
                                )
                            }
                        }
                    }
                }
            }
        gymDate != null ->
            CoachPage {
                CoachButton("Back to calendar") { strengthDate = null }
                val date = AppleCalendarDates.parse(gymDate)
                CoachHeading("Planned strength")
                Text(AppleCalendarDates.label(date))
                val authored = state.authoredGymProgramme
                if (authored != null) {
                    authored
                        .workouts()
                        .filter { it.date() == date }
                        .forEach { workout ->
                            CoachCard {
                                Text(workout.definition().title())
                                workout.definition().slots().forEach { Text(it.exercise().name()) }
                            }
                        }
                } else {
                    state.gymProgramme
                        ?.sessions()
                        ?.filter { it.date() == date }
                        .orEmpty()
                        .forEach { workout ->
                            CoachCard {
                                Text(workout.title())
                                workout.exercises().forEach { exercise ->
                                    Text(
                                        state.exercises
                                            .firstOrNull { it.id() == exercise.exerciseId() }
                                            ?.name() ?: "Exercise unavailable"
                                    )
                                }
                            }
                        }
                }
            }
        else ->
            SharedActivityCalendarScreen(
                state = state,
                onViewChange = { session.services.settings.setting("calendar.view", it) },
                workout = { plannedId = it },
                strength = { strengthDate = it },
                activity = activity,
                dates = AppleCalendarDates,
                distanceLabel = { distanceLabel(it, imperial) },
                strengthAction = "View workout",
                history = { AppleHistoryScreen(session, activity) },
            )
    }
}
