package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*

@Composable
fun TrainingPlanScreen(
    state: CoachUiState,
    model: CoachViewModel,
    open: (String) -> Unit,
    editGoal: () -> Unit,
    strength: (LocalDate) -> Unit = {},
    routines: () -> Unit = {},
    routine: (String, String?) -> Unit = { _, _ -> },
) =
    SharedTrainingPlanScreen(
        state,
        AndroidCoachCalendarDates,
        { start ->
            "${start.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK))} – ${start.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu", java.util.Locale.UK))}"
        },
        { formatDistance(it, state) },
        { model.runningPlan.generatePlan() },
        { proposal, choice -> model.runningPlan.decide(proposal, choice) },
        open,
        editGoal,
        strength,
        routine,
        { id, related, action -> WarmupSupportLink(id, related, action) },
        { MobilitySupportCard(routines) },
    )

@Composable
fun WorkoutCard(
    state: CoachUiState,
    workout: PlannedWorkout,
    completed: Boolean,
    open: () -> Unit,
) =
    SharedWorkoutCard(
        state,
        workout,
        completed,
        androidHomePresentation(state),
        AndroidCoachCalendarDates,
        open,
    )

@Composable
fun PlannedWorkoutScreen(
    state: CoachUiState,
    model: CoachViewModel,
    id: String?,
    openActivity: (String) -> Unit,
    startRun: (String, String) -> Unit = { _, _ -> },
    routine: (String, String?) -> Unit = { _, _ -> },
) =
    SharedPlannedWorkoutScreen(
        state,
        model.runningPlan,
        model.runningEvidence,
        id,
        androidHomePresentation(state),
        AndroidCoachCalendarDates,
        { LocalTime.parse(it).toString() },
        openActivity,
        startRun,
        routine,
        { label, value, change -> DateInput(label, value, change) },
        { workoutId -> RecoveryContextSection(state, model, workoutId) },
        { id, related, open -> WarmupSupportLink(id, related, open) },
        { label, value, change, decimal -> CoachInput(label, value, change, decimal) },
    )

@Composable
fun CoachInput(label: String, value: String, change: (String) -> Unit, decimal: Boolean = false) {
    if (label.contains("YYYY-MM-DD")) {
        DateInput(label.substringBefore(" ("), value, change)
        return
    }
    CoachTextInput(label, value, change, decimal)
}
