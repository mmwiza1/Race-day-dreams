package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedSpontaneousAssociation(
    state: CoachUiState,
    activity: ActivityRecord,
    mode: String,
    date: LocalDate,
    today: LocalDate,
    dateLabel: (LocalDate) -> String,
    chooseEasy: (String, String) -> Unit,
    linkRun: (String, String, String) -> Unit,
) {
    val versionId = state.currentPlan?.id()
    Text(
        "Recording origin: ${when(mode){"free"->"Spontaneous free run"
"quick"->"Quick target run"
"custom"->"Custom structured workout"
"repeat"->"Repeated workout"
"race"->"Race"
else->"Planned programme run"}}"
    )
    if (
        mode in listOf("workout", "race") ||
            state.runResponses.any { it.activityId() == activity.id() }
    )
        return
    var selected by
        remember(activity.id(), versionId) {
            mutableStateOf<org.racedaydreams.domain.PlannedWorkout?>(null)
        }
    val version = state.currentPlan
    val candidates =
        version?.plan()?.workouts()?.filter {
            it.date().toEpochDay() <= today.toEpochDay() &&
                kotlin.math.abs(it.date().toEpochDay() - date.toEpochDay()) <= 2 &&
                state.runResponses.none { r -> r.plannedWorkoutId() == it.id() } &&
                state.activities.none { a -> a.plannedWorkoutId() == it.id() }
        } ?: emptyList()
    val tomorrow =
        version?.plan()?.workouts()?.firstOrNull {
            it.date() == date.plusDays(1) &&
                it.date().toEpochDay() >= today.toEpochDay() &&
                org.racedaydreams.domain.strength.GymProgrammeGenerator.demanding(it.category())
        }
    if (
        tomorrow != null &&
            org.racedaydreams.domain.polish.TrainingInsights.extraRunDemand(activity)
    )
        SemanticCard(Tile.Insight) {
            Text("Review tomorrow's demand", style = MaterialTheme.typography.titleMedium)
            Text(
                "You recorded an extra run before ${CalendarData.label(tomorrow.category())}. It may feel more demanding. Your plan is unchanged."
            )
            var preview by remember { mutableStateOf(false) }
            TextButton(onClick = { preview = true }) { Text("Preview easy alternative") }
            if (preview)
                AlertDialog(
                    onDismissRequest = { preview = false },
                    title = { Text("Choose easy instead?") },
                    text = {
                        Text(
                            "Only tomorrow's session changes to conversational effort. Its current distance/time ceiling is retained; no extra mileage is added. The original plan version is preserved."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                chooseEasy(
                                    tomorrow.id(),
                                    "Runner chose an easy alternative after an extra recorded run",
                                )
                                preview = false
                            }
                        ) {
                            Text("Choose easy")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { preview = false }) { Text("Keep original") }
                    },
                )
        }
    if (candidates.isNotEmpty())
        SemanticCard(Tile.Insight) {
            Text("Extra run or planned training?", style = MaterialTheme.typography.titleMedium)
            Text(
                "Kept as an extra run unless you explicitly link it. The original prescription and actual GPS remain unchanged."
            )
            candidates.forEach { w ->
                TextButton(onClick = { selected = w }) {
                    Text(
                        "Count toward ${CalendarData.label(w.category())} · ${dateLabel(w.date())}"
                    )
                }
            }
        }
    selected?.let { w ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Link this actual run?") },
            text = {
                Text(
                    "This will compare your run with the preserved ${dateLabel(w.date())} prescription. It does not rewrite GPS or change the programme."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        linkRun(activity.id(), version!!.id(), w.id())
                        selected = null
                    }
                ) {
                    Text("Confirm link")
                }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("Keep as extra run") }
            },
        )
    }
}
