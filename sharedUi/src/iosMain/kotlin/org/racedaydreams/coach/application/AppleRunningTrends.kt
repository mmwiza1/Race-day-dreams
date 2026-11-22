package org.racedaydreams.coach.application

import androidx.compose.runtime.*
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.*
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.reminders.KotlinxReminderCalendar

/** The shared chart-first Training trends screen, with the iOS navigation around it. */
@Composable
internal fun AppleRunningTrends(
    state: CoachUiState,
    results: () -> Unit,
    routines: () -> Unit,
    back: () -> Unit,
    openRun: (String) -> Unit,
) {
    val today = AppleCalendarDates.today()
    val zone = TimeZone.currentSystemDefault().id
    val presentation = appleHomePresentation(state)
    CoachPage {
        CoachButton("Back", back)
        SharedTrainingTrends(
            state.activities,
            today,
            { KotlinxReminderCalendar.at(it, zone).date },
            state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
            presentation.distance,
            presentation.decimal,
            presentation.paceSeconds,
            presentation.shortDate,
            presentation.friendlyDate,
            openRun,
            null,
        )
        CoachButton("Recorded race results", results)
        CoachButton("Warm-ups and mobility", routines)
    }
}
