package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import java.time.LocalDate

@Composable
fun RaceScreen(
    state: CoachUiState,
    start: (String, String, Boolean) -> Unit,
    results: () -> Unit,
    routine: (String) -> Unit,
) =
    SharedRaceDay(
        state,
        LocalDate.now(),
        ::friendlyDate,
        ::decimal,
        ::formatPaceSeconds,
        start,
        results,
        routine,
        checklist = { RaceWeekChecklist(it) },
    )
