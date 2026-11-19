package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.baseline.AssessmentAnalyzer
import org.racedaydreams.domain.time.Duration

data class CompletedAssessmentInput(
    val distance: Int,
    val duration: Duration,
    val date: String,
    val time: String,
    val bestEffort: Boolean,
    val rpe: Double?,
    val notes: String,
)

@Composable
fun SharedAssessmentScreen(
    state: CoachUiState,
    defaultDate: String,
    defaultTime: String,
    dateField: @Composable (String, (String) -> Unit) -> Unit,
    save: (CompletedAssessmentInput) -> Unit,
    onDone: () -> Unit,
    saveError: String? = null,
) {
    val recommendation = remember(state.baseline) { AssessmentAnalyzer.recommend(state.baseline) }
    var distance by rememberSaveable { mutableIntStateOf(recommendation.distanceKm()) }
    var bestEffort by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableStateOf(defaultDate) }
    var startTime by rememberSaveable { mutableStateOf(defaultTime) }
    var time by rememberSaveable { mutableStateOf("") }
    var rpe by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachPage {
        CoachHeading("Optional baseline assessment")
        recommendation.reasons().forEach { Text(it) }
        Text(
            "Choose a distance and effort that fit your experience. You may stop, shorten it, or skip it. This screen records an assessment you have already completed; it does not record GPS."
        )
        Text("Assessment distance", style = MaterialTheme.typography.titleMedium)
        (1..5).forEach { value ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                RadioButton(selected = distance == value, onClick = { distance = value })
                TextButton(onClick = { distance = value }) { Text("${value}K") }
            }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Checkbox(bestEffort, { bestEffort = it })
            TextButton(onClick = { bestEffort = !bestEffort }, modifier = Modifier.weight(1f)) {
                Text("Best effort (optional; controlled effort is the default)")
            }
        }
        Text(
            if (bestEffort)
                "Avoid an unnecessary maximal effort when new or returning to running. One result is not proof of longer-distance endurance."
            else
                "Controlled assessment: use a sustainable effort. The entered pace will not automatically become your easy-running pace."
        )
        dateField(date) { date = it }
        CoachTextInput("Start time (HH:MM, device local time)", startTime, { startTime = it })
        CoachTextInput("Total elapsed time (MM:SS or H:MM:SS)", time, { time = it })
        CoachTextInput("Effort / RPE, 0–10 (optional)", rpe, { rpe = it })
        OutlinedTextField(
            notes,
            { notes = it },
            label = { Text("Context or notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    val duration = PaceCalculator.parseTime(time)
                    val effort = rpe.trim().toDoubleOrNull()
                    require(rpe.isBlank() || effort != null && effort in 0.0..10.0) {
                        "RPE must be between 0 and 10."
                    }
                    error = null
                    save(
                        CompletedAssessmentInput(
                            distance,
                            duration,
                            date.trim(),
                            startTime.trim(),
                            bestEffort,
                            effort,
                            notes,
                        )
                    )
                } catch (failure: Exception) {
                    error = failure.message ?: "Check the assessment values."
                }
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save completed assessment")
        }
        Text(
            "No splits, HR, cadence or elevation are invented. When reliable imported samples exist, they can provide additional analysis."
        )
        Text(
            "If you experience chest pain, fainting, severe difficulty breathing or serious injury symptoms, stop exercise and seek appropriate medical advice."
        )
        TextButton(onDone) { Text("Skip assessment") }
    }
}
