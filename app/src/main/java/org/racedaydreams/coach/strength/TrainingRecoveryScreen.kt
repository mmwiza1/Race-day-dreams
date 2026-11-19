package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.coach.*

@Composable
fun TrainingRecoveryScreen(
    model: TrainingRecoveryViewModel,
    onBack: () -> Unit,
    onReview: (String) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    TrainingRecoveryContent(
        state,
        onBack,
        model::refresh,
        model::keep,
        { model.prepareLighterWeek(onReview) },
        { id, date -> model.prepareMove(id, date, onReview) },
    )
}

@Composable
fun TrainingRecoveryContent(
    state: TrainingRecoveryState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onKeep: () -> Unit,
    onLighterWeek: () -> Unit,
    onMove: (String, java.time.LocalDate) -> Unit,
) {
    val date = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK) }
    SharedTrainingRecoveryContent(
        state,
        onBack,
        onRefresh,
        onKeep,
        onLighterWeek,
        onMove,
        dateLabel = { date.format(it) },
        instantLabel = { date.format(it.atZone(java.time.ZoneId.systemDefault())) },
    )
}
