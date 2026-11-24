package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlin.math.roundToLong
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.reminders.KotlinxReminderCalendar

internal fun appleHomePresentation(state: CoachUiState): HomePresentation {
    val miles = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    return HomePresentation(
        currentDate = AppleCalendarDates::today,
        dateLabel = { AppleCalendarDates.label(AppleCalendarDates.today()) },
        friendlyDate = AppleCalendarDates::label,
        shortDate = AppleCalendarDates::label,
        workoutName = CalendarData::label,
        decimal = ::homeNumber,
        paceSeconds = { PaceCalculator.formatTime(Durations.ofSeconds(it.roundToLong())) },
        activityDate = {
            AppleCalendarDates.label(
                KotlinxReminderCalendar.at(it.start(), TimeZone.currentSystemDefault().id).date
            )
        },
        distance = { distanceLabel(it, miles) },
        // These route strings are dispatched directly in Compose, with no URL parsing.
        encodeRouteComponent = { it },
        dateOf = { KotlinxReminderCalendar.at(it, TimeZone.currentSystemDefault().id).date },
    )
}

@Composable
internal fun AppleWeeklyReview(
    session: AppleCoachSession<*>,
    state: CoachUiState,
    plan: () -> Unit,
) = CoachPage {
    ScreenHeader(Discipline.RUNNING, Glyph.CALENDAR, "Coaching", "Weekly review")
    val review = state.weeklyReview
    if (review == null)
        Text("A weekly review appears with a running programme and available training evidence.")
    else {
        val presentation = appleHomePresentation(state)
        SharedWeeklyReviewContent(
            review,
            state.currentPlan?.plan(),
            state.weeklyDecision,
            state.busy,
            plan,
            { session.services.runningPlan.weeklyDecision(it) },
            presentation.friendlyDate,
            presentation.shortDate,
            presentation.decimal,
            { workout ->
                val task =
                    workout.distanceKm()?.let { presentation.distance(it * 1_000) }
                        ?: workout.duration()?.let { "${it.getSeconds() / 60} min" }
                listOfNotNull(presentation.workoutName(workout.category()), task)
                    .joinToString(" · ")
            },
            DisplayUnits.imperial(state),
        )
    }
    state.error?.let { Text(it) }
}

@Composable
internal fun AppleBaselineSummary(state: CoachUiState, manual: () -> Unit, assessment: () -> Unit) =
    CoachPage {
        CoachHeading("Running baseline")
        val presentation = appleHomePresentation(state)
        SharedBaselineSummaryCard(
            state.baseline,
            presentation.distance,
            presentation.decimal,
            PaceCalculator::formatTime,
            { AppleCalendarDates.label(KotlinxReminderCalendar.at(it, "UTC").date) },
        )
        CoachButton("Fill gaps or correct this baseline", manual)
        CoachButton("Optional baseline assessment", assessment)
    }

private fun homeNumber(value: Double): String {
    val tenths = (value * 10).roundToLong()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}
