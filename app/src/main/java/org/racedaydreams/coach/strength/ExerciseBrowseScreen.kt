package org.racedaydreams.coach.strength

import androidx.compose.runtime.Composable
import java.util.UUID
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.coach.ExerciseLibraryThumbnail
import org.racedaydreams.domain.strength.CustomStrengthExercise
import org.racedaydreams.domain.strength.StrengthExercise

/** Existing Android entry point; UI/filter logic is shared, bitmap IO and identity stay native. */
@Composable
fun ExerciseBrowseScreen(
    state: CoachUiState,
    onCreateCustom: (StrengthExercise) -> Unit,
    open: (String) -> Unit,
    onRestore: ((String) -> Unit)? = null,
    onCreateClassifiedCustom: (() -> Unit)? = null,
    customMetadata: Map<String, CustomStrengthExercise> = emptyMap(),
) =
    SharedExerciseBrowseScreen(
        state = state,
        onCreateCustom = onCreateCustom,
        open = open,
        thumbnail = { ExerciseLibraryThumbnail(it) },
        nextExerciseId = { UUID.randomUUID().toString() },
        onRestore = onRestore,
        onCreateClassifiedCustom = onCreateClassifiedCustom,
        customMetadata = customMetadata,
    )
