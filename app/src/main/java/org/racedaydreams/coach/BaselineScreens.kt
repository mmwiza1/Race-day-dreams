package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    CoachCard {
        Text("Does this look right?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (baseline.activityCount() > 0) Text("Found ${baseline.activityCount()} recorded runs in the available history window.")
        else Text("No running history is available yet. You can continue without it.")
        WhyDetails("Window: ${shortDate(baseline.windowStart().atOffset(ZoneOffset.UTC).toLocalDate())} to ${shortDate(baseline.windowEnd().atOffset(ZoneOffset.UTC).toLocalDate())} (UTC boundaries)","History window")
        BaselineValue("Average recorded weekly distance", baseline.weeklyDistanceKm()?.let { formatDistance(it * 1000, state) }, RunnerBaseline.WEEKLY_DISTANCE, baseline)
        BaselineValue("Usual recorded runs/week", baseline.runsPerWeek()?.let { decimal(it) }, RunnerBaseline.RUNS_PER_WEEK, baseline)
        BaselineValue("Longest recent run", baseline.longestRunKm()?.let { formatDistance(it * 1000, state) }, RunnerBaseline.LONGEST_RUN, baseline)
        BaselineValue("Typical elapsed run duration", baseline.typicalRunMinutes()?.let { "${decimal(it)} min" }, RunnerBaseline.TYPICAL_DURATION, baseline)
        BaselineValue("Recent 5K result", baseline.recent5k()?.let(PaceCalculator::formatTime), RunnerBaseline.RECENT_5K, baseline)
        BaselineValue("Recent 10K result", baseline.recent10k()?.let(PaceCalculator::formatTime), RunnerBaseline.RECENT_10K, baseline)
        BaselineValue("Recent half-marathon result", baseline.recentHalf()?.let(PaceCalculator::formatTime), RunnerBaseline.RECENT_HALF, baseline)
        Text("Baseline confidence: ${baseline.confidence().name.lowercase().replaceFirstChar { it.uppercase() }}")
        WhyDetails("Confidence describes the amount, recency and quality of evidence. It is not a probability of race success.","About confidence")
    }
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
private fun BaselineValue(label: String, value: String?, key: String, baseline: RunnerBaseline) {
    if (value != null) {
        val origin = baseline.provenance()[key]?.name?.lowercase()?.replace('_', ' ')
        Text("$label: $value${origin?.let { " · $it" } ?: ""}")
    }
}

@Composable
fun ManualBaselineScreen(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    val baseline = state.baseline
    val saved = state.manualBaseline
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val kmPerUnit = if (imperial) 1.609344 else 1.0
    val unit = if (imperial) "mi" else "km"
    var correct by rememberSaveable { mutableStateOf(false) }
    var weekly by rememberSaveable { mutableStateOf(saved.weeklyDistanceKm()?.let { decimal(it / kmPerUnit) } ?: "") }
    var frequency by rememberSaveable { mutableStateOf(saved.runsPerWeek()?.let(::decimal) ?: "") }
    var longest by rememberSaveable { mutableStateOf(saved.longestRunKm()?.let { decimal(it / kmPerUnit) } ?: "") }
    var five by rememberSaveable { mutableStateOf(saved.recent5k()?.let(PaceCalculator::formatTime) ?: "") }
    var ten by rememberSaveable { mutableStateOf(saved.recent10k()?.let(PaceCalculator::formatTime) ?: "") }
    var half by rememberSaveable { mutableStateOf(saved.recentHalf()?.let(PaceCalculator::formatTime) ?: "") }
    var easy by rememberSaveable { mutableStateOf(saved.easyPaceSecondsPerKm()?.let { formatPaceSeconds(it * kmPerUnit) } ?: "") }
    var weeks by rememberSaveable { mutableStateOf(saved.consistentWeeks()?.toString() ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachPage {
        CoachHeading("Optional current-running details")
        Text("We only ask for gaps by default. You can correct recorded values if they do not represent your current running. These entries are marked manual, rather than measured.")
        CheckRow("Correct values already available", correct) { correct = it }
        if (correct || baseline.weeklyDistanceKm() == null || saved.weeklyDistanceKm() != null)
            CoachField("Current weekly distance ($unit)", weekly, { weekly = it }, baseline.weeklyDistanceKm()?.let { "Recorded: ${decimal(it / kmPerUnit)} $unit" })
        if (correct || baseline.runsPerWeek() == null || saved.runsPerWeek() != null)
            CoachField("Current usual runs/week", frequency, { frequency = it }, baseline.runsPerWeek()?.let { "Recorded: ${decimal(it)}" })
        if (correct || baseline.longestRunKm() == null || saved.longestRunKm() != null)
            CoachField("Longest recent run ($unit)", longest, { longest = it }, baseline.longestRunKm()?.let { "Recorded: ${decimal(it / kmPerUnit)} $unit" })
        if (correct || baseline.recent5k() == null || saved.recent5k() != null) CoachField("Recent 5K result (MM:SS or H:MM:SS)", five, { five = it })
        if (correct || baseline.recent10k() == null || saved.recent10k() != null) CoachField("Recent 10K result", ten, { ten = it })
        if (correct || baseline.recentHalf() == null || saved.recentHalf() != null) CoachField("Recent half-marathon result", half, { half = it })
        if (correct || baseline.easyPaceSecondsPerKm() == null || saved.easyPaceSecondsPerKm() != null) CoachField("Approximate easy pace (MM:SS/$unit)", easy, { easy = it })
        if (correct || baseline.consistentWeeks() == null || saved.consistentWeeks() != null) CoachField("Weeks running consistently", weeks, { weeks = it })
        Text("Every field is optional. Blank fields leave recorded evidence in place; use Reset manual corrections to remove previous manual entries.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val input = ManualBaselineInput(
                    optionalNumber(weekly)?.times(kmPerUnit), optionalNumber(frequency), optionalNumber(longest)?.times(kmPerUnit),
                    optionalDuration(five), optionalDuration(ten), optionalDuration(half),
                    optionalDuration(easy)?.seconds?.toDouble()?.div(kmPerUnit),
                    weeks.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        ?: if (weeks.isBlank()) null else throw IllegalArgumentException("Enter whole weeks, or leave this field blank.")
                )
                error = null
                model.baseline.saveManualBaseline(input, onDone)
            } catch (failure: IllegalArgumentException) { error = failure.message }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save optional baseline") }
        OutlinedButton(onClick = { model.baseline.saveManualBaseline(ManualBaselineInput.empty(), onDone) }, enabled = !state.busy) { Text("Reset manual corrections") }
        TextButton(onDone) { Text("Continue without changes") }
    }
}

@Composable
fun AssessmentScreen(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    val recommendation = remember(state.baseline) { AssessmentAnalyzer.recommend(state.baseline) }
    var distance by rememberSaveable { mutableIntStateOf(recommendation.distanceKm()) }
    var bestEffort by rememberSaveable { mutableStateOf(false) }
    val defaultStart = remember { LocalDateTime.now().minusHours(1) }
    var date by rememberSaveable { mutableStateOf(defaultStart.toLocalDate().toString()) }
    var startTime by rememberSaveable { mutableStateOf(defaultStart.toLocalTime().withSecond(0).withNano(0).toString()) }
    var time by rememberSaveable { mutableStateOf("") }
    var rpe by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachPage {
        CoachHeading("Optional baseline assessment")
        recommendation.reasons().forEach { Text(it) }
        Text("Choose a distance and effort that fit your experience. You may stop, shorten it, or skip it. This screen records an assessment you have already completed; it does not record GPS.")
        Text("Assessment distance", style = MaterialTheme.typography.titleMedium)
        (1..5).forEach { value ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                RadioButton(selected = distance == value, onClick = { distance = value })
                TextButton(onClick = { distance = value }) { Text("${value}K") }
            }
        }
        CheckRow("Best effort (optional; controlled effort is the default)", bestEffort) { bestEffort = it }
        Text(if (bestEffort) "Avoid an unnecessary maximal effort when new or returning to running. One result is not proof of longer-distance endurance."
            else "Controlled assessment: use a sustainable effort. The entered pace will not automatically become your easy-running pace.")
        CoachField("Completed run date (YYYY-MM-DD)", date, { date = it })
        CoachField("Start time (HH:MM, device local time)", startTime, { startTime = it })
        CoachField("Total elapsed time (MM:SS or H:MM:SS)", time, { time = it })
        CoachField("Effort / RPE, 0–10 (optional)", rpe, { rpe = it })
        CoachField("Context or notes (optional)", notes, { notes = it }, singleLine = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val duration = PaceCalculator.parseTime(time)
                val parsedDate = LocalDate.parse(date.trim())
                val parsedTime = LocalTime.parse(startTime.trim())
                val effort = optionalNumber(rpe)
                require(effort == null || effort in 0.0..10.0) { "RPE must be between 0 and 10." }
                error = null
                model.baseline.saveAssessment(distance, duration, parsedDate, parsedTime, bestEffort, effort, notes, onDone)
            } catch (failure: Exception) { error = failure.message ?: "Check the assessment values." }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save completed assessment") }
        Text("No splits, HR, cadence or elevation are invented. When reliable imported samples exist, they can provide additional analysis.")
        Text("If you experience chest pain, fainting, severe difficulty breathing or serious injury symptoms, stop exercise and seek appropriate medical advice.")
        TextButton(onDone) { Text("Skip assessment") }
    }
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
