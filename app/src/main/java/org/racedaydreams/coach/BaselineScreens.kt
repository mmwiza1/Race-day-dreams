package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.baseline.AssessmentAnalyzer
import org.racedaydreams.domain.baseline.ManualBaselineInput
import org.racedaydreams.domain.baseline.RunnerBaseline
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthConnectGateway
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale

@Composable
fun BaselineScreen(state: CoachUiState, model: CoachViewModel, historyAvailable: Boolean,
                   connect: (Boolean, Boolean) -> Unit, providerUpdate: () -> Unit,
                   onManual: () -> Unit, onAssessment: () -> Unit, onContinue: () -> Unit) = CoachPage {
    CoachHeading("Start with the running you already do")
    WhyDetails("Compatible apps can share existing runs through Health Connect. Your data stays on this device.","How does this work?")
    HealthConnectionCard(state, model, historyAvailable, connect, providerUpdate)
    val baseline = state.baseline
    SharedBaselineSummaryCard(
        baseline,
        { formatDistance(it, state) },
        ::decimal,
        PaceCalculator::formatTime,
        { shortDate(it.atOffset(ZoneOffset.UTC).toLocalDate()) },
    )
    WhyDetails(baseline.explanations().joinToString("\n"),"About your baseline")
    if (state.eligibilityReasons.isNotEmpty()) CoachCard {
        Text("Performance-focused training", style = MaterialTheme.typography.titleMedium)
        WhyDetails(state.eligibilityReasons.joinToString("\n"),"Why this status?")
    }
    Button(onClick = onContinue, enabled = !state.busy,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(if (state.profile == null) "Looks right / continue to my goals" else "Confirm baseline")
    }
    OutlinedButton(onManual, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Fill gaps or correct this baseline") }
    OutlinedButton(onAssessment, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Optional baseline assessment") }
    Text("Your future goal, race date and availability are still your decisions. Recorded history cannot tell us your intentions.")
}

@Composable
fun HealthConnectionCard(state: CoachUiState, model: CoachViewModel, historyAvailable: Boolean,
                         connect: (Boolean, Boolean) -> Unit, providerUpdate: () -> Unit) {
    var heartRate by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    CoachCard {
        Text("Optional Health Connect", style = MaterialTheme.typography.titleMedium)
        when (state.healthAvailability) {
            HealthAvailability.UNAVAILABLE -> Text("Health Connect is unavailable on this device. Optional manual details and assessment results remain available; an account is never required.")
            HealthAvailability.UPDATE_REQUIRED -> {
                Text("Health Connect needs installation or an update. You can continue without it.")
                OutlinedButton(providerUpdate, enabled = !state.busy) { Text("Open Health Connect update") }
            }
            HealthAvailability.AVAILABLE -> {
                Text("Exercise sessions and distance are used for your running history and baseline. Missing or declined permissions do not block the app.")
                CheckRow("Also request heart rate for available run averages", heartRate) { heartRate = it }
                if (historyAvailable) CheckRow("Request up to 12 weeks of existing history", history) { history = it }
                Text("Without extended-history access, Android may make only recent history available. We show the actual accessible window.")
                if (state.healthPermissions.containsAll(HealthConnectGateway.BASELINE_PERMISSIONS)) {
                    Button(onClick = model::syncHealth, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Refresh recorded runs") }
                    if (heartRate || history) OutlinedButton(onClick = { connect(heartRate, history) }, enabled = !state.busy) { Text("Request selected optional access") }
                } else Button(onClick = { connect(heartRate, history) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Connect Health Connect") }
            }
        }
    }
}

@Composable
fun ManualBaselineScreen(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) =
    SharedManualBaselineScreen(state, ::decimal, ::formatPaceSeconds, { model.baseline.saveManualBaseline(it, onDone) }, onDone)

@Composable
fun AssessmentScreen(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    val defaultStart = remember { LocalDateTime.now().minusHours(1) }
    SharedAssessmentScreen(
        state,
        defaultStart.toLocalDate().toString(),
        defaultStart.toLocalTime().withSecond(0).withNano(0).toString(),
        dateField = { value, change -> CoachField("Completed run date (YYYY-MM-DD)", value, change) },
        save = { input ->
            model.baseline.saveAssessment(
                input.distance, input.duration, LocalDate.parse(input.date), LocalTime.parse(input.time),
                input.bestEffort, input.rpe, input.notes, onDone,
            )
        },
        onDone = onDone,
        saveError = state.error,
    )
}

@Composable
fun CoachField(label: String, value: String, update: (String) -> Unit, supporting: String? = null, singleLine: Boolean = true) {
    if(label.contains("YYYY-MM-DD") && !label.startsWith("Find")){DateInput(label.substringBefore(" ("),value,update,optional=label.contains("optional"));return}
    OutlinedTextField(value, update, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        supportingText = if (supporting == null) null else ({ Text(supporting) }), singleLine = singleLine)
}

@Composable
fun CheckRow(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Checkbox(checked, changed)
        TextButton(onClick = { changed(!checked) }, modifier = Modifier.weight(1f)) { Text(label) }
    }
}

fun optionalNumber(text: String): Double? {
    if (text.isBlank()) return null
    val value = text.trim().toDoubleOrNull() ?: throw IllegalArgumentException("Enter a number, or leave the field blank.")
    require(value.isFinite()) { "Values must be finite numbers." }
    return value
}
fun optionalDuration(text: String): Duration? = text.trim().takeIf(String::isNotEmpty)?.let(PaceCalculator::parseTime)
fun decimal(value: Double): String = String.format(Locale.ROOT, "%.1f", value)
fun formatPaceSeconds(seconds: Double): String { val rounded = kotlin.math.round(seconds).toLong(); return String.format(Locale.ROOT, "%d:%02d", rounded / 60, rounded % 60) }
fun formatDistance(metres: Double, state: CoachUiState): String = if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL)
    "${decimal(metres / 1609.344)} mi" else "${decimal(metres / 1000)} km"
