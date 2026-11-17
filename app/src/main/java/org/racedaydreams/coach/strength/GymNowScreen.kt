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
        { enabled, action -> BackHandler(enabled = enabled, onBack = action) },
    )
}

private val GYM_DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)
