package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Original UK dates, Android lifecycle and back handling for the shared production screen. */
@Composable
fun GymNowScreen(
    model: GymNowViewModel,
    onWorkout: () -> Unit,
    onBuild: () -> Unit,
    onBack: () -> Unit,
    plannedDate: LocalDate? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedGymNowScreen(
        model.shared,
        onWorkout,
        onBuild,
        onBack,
        state,
        LocalDate.now(),
        { it.format(GYM_DAY) },
        { it.atZone(ZoneId.systemDefault()).format(GYM_DAY) },
        ::gymTargetDescription,
        plannedDate = plannedDate,
        backHandler = { enabled, action -> BackHandler(enabled = enabled, onBack = action) },
        // The Android shell's app bar Back leaves Gym Now, so the overview shows no second Back.
        appBarBack = true,
    )
}

private val GYM_DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
