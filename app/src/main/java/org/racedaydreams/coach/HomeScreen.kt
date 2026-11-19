package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Original Android formatting/navigation bindings; the actual Home UI is common. */
@Composable
internal fun HomeScreen(
    state: CoachUiState,
    weekly: () -> Unit,
    baseline: () -> Unit,
    assessment: () -> Unit,
    history: () -> Unit,
    plan: () -> Unit,
    workout: (String) -> Unit,
    record: () -> Unit = {},
    quick: (String) -> Unit = {},
    race: () -> Unit = {},
) =
    SharedHomeScreen(
        state,
        androidHomePresentation(state),
        { StrengthTodayScreen(state, quick, gymNow = { quick("gym-now") }) },
        weekly,
        baseline,
        assessment,
        history,
        plan,
        workout,
        record,
        quick,
        race,
    )

internal fun androidHomePresentation(state: CoachUiState) =
    HomePresentation(
        LocalDate::now,
        { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)) },
        ::friendlyDate,
        ::shortDate,
        ::workoutName,
        ::decimal,
        ::formatPaceSeconds,
        ::displayActivityDate,
        { formatDistance(it, state) },
        Uri::encode,
    )
