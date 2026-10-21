package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SplitAnalysis
import org.racedaydreams.domain.activity.SplitCalculator
import org.racedaydreams.domain.baseline.AssessmentAnalyzer

@Composable
fun ActivityHistoryScreen(state: CoachUiState, open: (String) -> Unit) {
    val groups = remember(state.activities) { state.activities.sortedByDescending { it.start() }.groupBy(::displayActivityDate) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            CoachHeading("Activity calendar")
            Text("Recorded runs · list view", Modifier.padding(top = 8.dp))
            Text("Your week can change. Recorded activities are retained independently of planned workouts.", Modifier.padding(top = 8.dp))
        }
        if (groups.isEmpty()) item { Text("No completed runs have been added. Connect Health Connect or enter an optional completed assessment to begin your history.") }
        groups.forEach { (date, activities) ->
            item(key = "date:$date") { Text(date, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
            items(activities, key = { it.id() }) { activity ->
                Card(onClick = { open(activity.id()) }, Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (activity.source().provider() == "assessment") "Assessment run" else "Recorded run", style = MaterialTheme.typography.titleMedium)
                        Text(activity.distanceMetres()?.let { formatDistance(it, state) } ?: "Distance unavailable")
                        Text("Elapsed: ${PaceCalculator.formatTime(activity.elapsedTime())}")
                        Text("Source: ${activity.source().provider()}${activity.source().originPackage()?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}")
                        Text("Completed · tap for details")
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityDetailScreen(state: CoachUiState, model: CoachViewModel, id: String?) {
    val activity = state.activities.firstOrNull { it.id() == id }
    if (activity == null) { CoachPage { CoachHeading("Activity unavailable"); Text("This activity is no longer in local history. A source may have updated or removed it.") }; return }
    var imperial by rememberSaveable(activity.id()) { mutableStateOf(state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) }
    val splitUnits = if (imperial) UnitSystem.IMPERIAL else UnitSystem.METRIC
    val analysis by produceState<SplitAnalysis?>(null, activity, splitUnits) {
        value = withContext(Dispatchers.Default) { SplitCalculator.calculate(activity, splitUnits) }
    }
    var notes by rememberSaveable(activity.id(), activity.notes()) { mutableStateOf(activity.notes() ?: "") }
    var rpe by rememberSaveable(activity.id(), activity.rpe()) { mutableStateOf(activity.rpe()?.let(::decimal) ?: "") }
    var error by rememberSaveable(activity.id()) { mutableStateOf<String?>(null) }
    var chooseShoe by rememberSaveable { mutableStateOf(false) }
    val assessment = state.assessments.firstOrNull { it.activityId() == activity.id() }
    val assessmentSummary = remember(activity, assessment, state.baseline) {
        assessment?.let { AssessmentAnalyzer.analyze(it, activity, state.baseline) }
    }
    CoachPage {
        CoachHeading(if (assessment != null) "Assessment run" else "Run detail")
        Text(activity.start().atOffset(activity.startOffset() ?: ZoneOffset.UTC).toString())
        if (activity.startOffset() == null) Text("The source did not provide a time-zone offset; this time is shown explicitly in UTC.")
        CoachCard {
            activity.distanceMetres()?.let { Text(formatDistance(it, state), style = MaterialTheme.typography.headlineMedium) }
                ?: Text("Distance unavailable")
            Text("Elapsed time: ${PaceCalculator.formatTime(activity.elapsedTime())}")
            activity.movingTime()?.let { Text("Moving time: ${PaceCalculator.formatTime(it)}") }
            activity.distanceMetres()?.takeIf { it > 0 }?.let { metres ->
                val unitMetres = if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) 1609.344 else 1000.0
                Text("Calculated elapsed average pace: ${formatPaceSeconds(activity.elapsedTime().toMillis() / 1000.0 / (metres / unitMetres))}/${if (unitMetres > 1000) "mi" else "km"}")
            }
            activity.averageHeartRate()?.let { Text("Average recorded HR: ${decimal(it)} bpm") }
            activity.maxHeartRate()?.let { Text("Maximum recorded HR: ${decimal(it)} bpm") }
            activity.elevationGainMetres()?.let { Text("Recorded elevation gain: ${decimal(it)} m") }
            activity.cadence()?.let { Text("Recorded cadence: ${decimal(it)} steps/min") }
            activity.rpe()?.let { Text("Manually entered effort: ${decimal(it)}/10") }
            Text("Source: ${activity.source().provider()}")
            activity.source().originPackage()?.takeIf { it.isNotBlank() }?.let { Text("Origin app: $it") }
            Text("Source record: ${activity.source().recordId()}")
            Text("Values absent from the source remain unavailable. Elapsed time is not presented as moving time.")
        }
        assessmentSummary?.let { summary ->
            CoachCard {
                Text("What this assessment tells us", style = MaterialTheme.typography.titleMedium)
                Text("Mode: ${assessment!!.assessment().mode().name.lowercase().replace('_', ' ')}")
                Text("Baseline confidence: ${summary.confidence().name.lowercase()}")
                summary.findings().forEach { Text(it) }
            }
        }
        CoachHeading("Distance splits")
        CheckRow("Show mile splits instead of kilometre splits", imperial) { imperial = it }
        val splitAnalysis = analysis
        if (splitAnalysis == null) Text("Analysing retained samples…")
        else {
            if (splitAnalysis.splits().isEmpty()) Text("Reliable distance/time samples are unavailable. Splits have not been invented from average pace.")
            splitAnalysis.reasons().forEach { Text(it) }
            splitAnalysis.splits().forEach { split ->
                CoachCard {
                    Text("Split ${split.number()}${if (split.partial()) " · partial" else ""}", style = MaterialTheme.typography.titleMedium)
                    val length = if (imperial) split.distanceMetres() / 1609.344 else split.distanceMetres() / 1000.0
                    val pace = split.secondsPerKilometre() * if (imperial) 1.609344 else 1.0
                    Text("${decimal(length)} ${if (imperial) "mi" else "km"} · ${PaceCalculator.formatTime(split.duration())} · ${formatPaceSeconds(pace)}/${if (imperial) "mi" else "km"}")
                    split.averageHeartRate()?.let { Text("Available-sample HR: ${decimal(it)} bpm") }
                    split.elevationChangeMetres()?.let { Text("Elevation change: ${decimal(it)} m") }
                    split.cadence()?.let { Text("Cadence: ${decimal(it)} steps/min") }
                }
            }
        }
        CoachHeading("Recorded laps / intervals")
        if (activity.laps().isEmpty()) Text("No actual laps were supplied. Planned repetitions are never substituted for completed laps.")
        activity.laps().forEachIndexed { index, lap ->
            CoachCard {
                Text(lap.label()?.takeIf { it.isNotBlank() } ?: "Lap ${index + 1}", style = MaterialTheme.typography.titleMedium)
                Text("Elapsed: ${PaceCalculator.formatTime(Duration.between(lap.start(), lap.end()))}")
                lap.distanceMetres()?.let { Text(formatDistance(it, state)) }
                lap.plannedSegmentId()?.let { Text("Linked planned segment: $it") }
            }
        }
        val planned = state.plannedWorkouts.firstOrNull { it.id() == activity.plannedWorkoutId() }
        CoachHeading("Planned and actual")
        if (planned == null) Text(if (activity.plannedWorkoutId() == null) "No planned workout is linked to this activity."
            else "The linked planned workout is not currently available. The actual result remains retained.")
        else CoachCard {
            Text("Planned: ${planned.category().name.replace('_', ' ').lowercase()}")
            planned.distanceKm()?.let { Text("Planned distance: ${formatDistance(it * 1000, state)}") }
            planned.duration()?.let { Text("Planned duration: ${PaceCalculator.formatTime(it)}") }
            activity.distanceMetres()?.let { actual ->
                Text("Actual distance: ${formatDistance(actual, state)}")
                planned.distanceKm()?.let { target -> Text("Distance difference: ${decimal(actual / 1000 - target)} km") }
            }
            Text("The planned prescription is retained separately from this completion.")
        }
        val results = state.results.filter { it.activityId() == activity.id() }
        if (results.isNotEmpty()) CoachCard {
            Text("Linked performance results", style = MaterialTheme.typography.titleMedium)
            results.forEach { Text("${it.distance().label()}: ${PaceCalculator.formatTime(it.duration())} · ${it.source().name.lowercase().replace('_', ' ')}") }
            Text("Personal-best progression is compared with your other retained results in Progress.")
        }
        CoachHeading("Shoe and your notes")
        val shoe = state.shoes.firstOrNull { it.id() == activity.shoeId() }
        Text(shoe?.let { "Shoe: ${it.brand()} ${it.model()}${if (it.retired()) " · retired" else ""}" } ?: "No shoe assigned")
        OutlinedButton(onClick = { chooseShoe = true }, enabled = !state.busy) { Text("Change shoe assignment") }
        CoachField("Your effort / RPE, 0–10 (optional)", rpe, { rpe = it })
        CoachField("Your notes", notes, { notes = it }, singleLine = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val effort = optionalNumber(rpe)
                require(effort == null || effort in 0.0..10.0) { "RPE must be between 0 and 10." }
                error = null
                model.saveNotes(activity.id(), notes, effort)
            } catch (failure: IllegalArgumentException) { error = failure.message }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save notes and effort") }
    }
    if (chooseShoe) AlertDialog(onDismissRequest = { chooseShoe = false },
        title = { Text("Assign a shoe") },
        text = { Column {
            TextButton(onClick = { chooseShoe = false; model.assignShoe(activity.id(), null) }) { Text("No shoe") }
            state.shoes.forEach { shoe ->
                TextButton(onClick = { chooseShoe = false; model.assignShoe(activity.id(), shoe.id()) }) {
                    Text("${shoe.brand()} ${shoe.model()}${if (shoe.retired()) " · retired" else ""}")
                }
            }
            if (state.shoes.isEmpty()) Text("Add shoes from Progress or Settings first.")
        } },
        confirmButton = { TextButton(onClick = { chooseShoe = false }) { Text("Close") } })
}
