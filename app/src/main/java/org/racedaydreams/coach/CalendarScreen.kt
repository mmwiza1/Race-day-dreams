package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import org.racedaydreams.domain.presentation.CalendarData

fun calendarEntries(
    state: CoachUiState,
    date: LocalDate,
    today: LocalDate = LocalDate.now(),
): List<CalendarData.Entry> = sharedCalendarEntries(state, date, today)

/** Original system-zone calendar and UK labels; domain dates/history are unchanged. */
internal object AndroidCoachCalendarDates : CoachCalendarDates {
    override fun today() = LocalDate.now()

    override fun parse(value: String) = LocalDate.parse(value)

    override fun shiftMonth(date: LocalDate, months: Long) = date.plusMonths(months)

    override fun monthStart(date: LocalDate) = date.withDayOfMonth(1)

    override fun weekStart(date: LocalDate) =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    override fun label(date: LocalDate) = friendlyDate(date)

    override fun monthLabel(date: LocalDate) =
        date.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK))
}

@Composable
fun ActivityCalendarScreen(
    state: CoachUiState,
    model: CoachViewModel,
    workout: (String) -> Unit,
    strength: (String) -> Unit,
    activity: (String) -> Unit,
    allStrength: () -> Unit = {},
) =
    SharedActivityCalendarScreen(
        state = state,
        onViewChange = { model.settings.setting("calendar.view", it) },
        workout = workout,
        strength = strength,
        activity = activity,
        dates = AndroidCoachCalendarDates,
        distanceLabel = { formatDistance(it, state) },
        history = { ActivityHistoryScreen(state, activity, allStrength) },
    )
