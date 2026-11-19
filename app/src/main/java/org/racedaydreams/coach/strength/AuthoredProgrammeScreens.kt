package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import org.racedaydreams.coach.friendlyDate
import org.racedaydreams.domain.strength.StrengthProgramme

/** Original UK date and exact target labels for the actual shared authored-programme screens. */
@Composable
internal fun AuthoredProgrammeOverview(
    programme: StrengthProgramme,
    open: (String) -> Unit,
    author: (() -> Unit)? = null,
) = SharedAuthoredProgrammeOverview(programme, open, ::friendlyDate, author)

@Composable
internal fun AuthoredProgrammeWorkout(
    programme: StrengthProgramme,
    id: String?,
    start: (() -> Unit)?,
) = SharedAuthoredProgrammeWorkout(programme, id, start, ::friendlyDate, ::gymTargetDescription)
