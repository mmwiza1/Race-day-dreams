package org.racedaydreams.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityAnalytics
import org.racedaydreams.domain.activity.MilestoneEngine
import org.racedaydreams.domain.activity.PerformanceDistance
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.PersonalBestEntry
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.RecordedWeek
import org.racedaydreams.domain.activity.ResultSource
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeMetricsCalculator

/** Presentation of retained evidence. Blank metrics are not substituted with predicted values. */
@Composable
fun ProgressScreen(state: CoachUiState, model: CoachViewModel, onActivity: (String) -> Unit,
                   onShoes: () -> Unit, onStrength: () -> Unit = {}) {
    val weeks = remember(state.activities) {
        ActivityAnalytics.weekly(state.activities, LocalDate.now(), ZoneId.systemDefault(), 12)
    }
    val current = remember(state.results) { PersonalBestCalculator.current(state.results).associateBy { it.result().distance().code() } }
    val history = remember(state.results) { PersonalBestCalculator.history(state.results) }
    val milestones = remember(state.activities, state.results, state.milestonesEnabled) {
        MilestoneEngine.calculate(state.milestonesEnabled, state.activities, state.results)
    }
    var expandedDistance by rememberSaveable { mutableStateOf<String?>(null) }
    var enteringResult by rememberSaveable { mutableStateOf(false) }
    var showAllEntered by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        if (state.strengthSessions.isNotEmpty()) {
            CoachHeading("Strength progress")
            Text("${state.strengthSessions.size} recorded strength sessions; exercise PB progression is separate from running performance.")
            CoachButton("View Strength progress", onStrength)
        } else TextButton(onClick=onStrength, modifier=Modifier.heightIn(min=48.dp)) { Text("Optional strength training") }

        CoachHeading("Progress from your recorded running")
        WhyDetails("These are summaries of retained records, not a claim that unrecorded training did not happen. No readiness score or race prediction is generated.","About these trends")
        WeeklyDistanceCard(weeks, state)
        WeightSection(state, model)
        val now = java.time.Instant.now()
        val load = remember(state.activities, state.strengthSessions) {
            org.racedaydreams.domain.coaching.TrainingLoadEvidence.window(state.activities, state.strengthSessions, now.minus(java.time.Duration.ofDays(7)), now)
        }
        CoachCard {
            Text("Recent session-RPE context", style = MaterialTheme.typography.titleMedium)
            Text(load.runningLoad()?.let { "Running: ${kotlin.math.round(it).toInt()} min·RPE from ${load.runsWithRpe()}/${load.runsObserved()} runs in the last seven days" } ?: "Running load: unavailable — no RPE recorded in this window")
            Text(load.strengthLoad()?.let { "Strength: ${kotlin.math.round(it).toInt()} min·RPE from ${load.strengthWithRpe()}/${load.strengthObserved()} sessions" } ?: "Strength load: unavailable — no RPE recorded in this window")
            WhyDetails("Duration × reported RPE. Missing effort is excluded, not filled with zero. Running and strength loads are separate context signals, not injury predictions.","How is workload calculated?")
        }
        CoachHeading("Personal bests")
        Text("Results retain their date and source. Official chip times are entered by you; they are not independently verified. Rolling efforts require reliable continuous samples.")
        Button(onClick = { enteringResult = !enteringResult }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (enteringResult) "Close result form" else "Add a race or manual result")
        }
        if (enteringResult) ResultEditor(state, model) { enteringResult = false }
        PerformanceDistance.CORE.forEach { distance ->
            val best = current[distance.code()]
            CoachCard {
                Text(distance.label(), style = MaterialTheme.typography.titleLarge)
                if (best == null) Text("No supported result recorded yet.")
                else {
                    ResultEvidence(best, state, onActivity)
                    TextButton(onClick = { expandedDistance = if (expandedDistance == distance.code()) null else distance.code() }) {
                        Text(if (expandedDistance == distance.code()) "Hide PB progression" else "Show PB progression")
                    }
                    if (expandedDistance == distance.code()) {
                        history.filter { it.result().distance().code() == distance.code() }.forEach { past ->
                            HorizontalDivider()
                            Text("${friendlyDate(past.result().date())} · ${PaceCalculator.formatTime(past.result().duration())} · ${resultSource(past.result())}")
                            past.improvement()?.let { Text("Improvement: ${PaceCalculator.formatTime(it)}") }
                            LinkedActivity(past.result(), state, onActivity)
                        }
                    }
                }
            }
        }
        val manualResults = state.results.filter { it.source() != ResultSource.ROLLING }
        if (manualResults.isNotEmpty()) {
            CoachHeading("Your entered results")
            Text("Every result is retained, including efforts that were not a personal best.")
            val entered = manualResults.sortedByDescending { it.date() }
            (if (showAllEntered) entered else entered.take(20)).forEach { result ->
                CoachCard {
                    Text("${result.distance().label()} · ${PaceCalculator.formatTime(result.duration())}")
                    Text("${friendlyDate(result.date())} · ${resultSource(result)}")
                    result.eventName()?.takeIf { it.isNotBlank() }?.let { Text(it) }
                    LinkedActivity(result, state, onActivity)
                    TextButton(onClick = { model.deleteResult(result.id()) }, enabled = !state.busy) { Text("Remove entered result") }
                }
            }
            if (entered.size > 20) TextButton(onClick = { showAllEntered = !showAllEntered }) {
                Text(if (showAllEntered) "Show recent entered results" else "Show all ${entered.size} entered results")
            }
        }
        CoachHeading("Shoes and mileage")
        CoachButton("View and manage shoes", onShoes)
        CoachCard {
            Text("Optional milestones", style = MaterialTheme.typography.titleMedium)
            CheckRow("Show restrained recorded milestones", state.milestonesEnabled, model::setMilestonesEnabled)
            Text("No streaks or milestone notifications. A first recorded distance refers to this app’s history, not your lifetime running.")
            if (state.milestonesEnabled) {
                if (milestones.isEmpty()) Text("No qualifying milestones in the retained evidence yet.")
                milestones.forEach { milestone ->
                    Text("${milestone.label()} · ${friendlyDate(milestone.date())}")
                    Text(milestone.explanation(), style = MaterialTheme.typography.bodySmall)
                    milestone.activityId()?.takeIf { id -> state.activities.any { it.id() == id } }?.let { id ->
                        TextButton(onClick = { onActivity(id) }) { Text("View milestone activity") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyDistanceCard(weeks: List<RecordedWeek>, state: CoachUiState) = CoachCard {
    Text("Weekly recorded distance", style = MaterialTheme.typography.titleLarge)
    Text("Twelve weeks · grouped in the device time zone. This week is still in progress.")
    val largest = weeks.mapNotNull { it.distanceMetres() }.maxOrNull()?.takeIf { it > 0 }
    weeks.forEach { week ->
        val label = when {
            week.activityCount() == 0 -> "No recorded runs"
            week.distanceMetres() == null -> "Distance unavailable for ${week.activityCount()} recorded runs"
            else -> formatDistance(week.distanceMetres(), state) +
                if (week.missingDistanceCount() > 0) " known distance; ${week.missingDistanceCount()} other runs have no distance" else " recorded"
        }
        Text("${shortDate(week.weekStart())} · $label", style = MaterialTheme.typography.bodyMedium)
        val fraction = if (largest != null && week.distanceMetres() != null)
            (week.distanceMetres() / largest).toFloat().coerceIn(0f, 1f) else 0f
        // The complete equivalent is the text above. Bars do not add colour-only information.
        Box(Modifier.fillMaxWidth().height(8.dp).clearAndSetSemantics { }
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
        }
        if (week.activityCount() > 0) Text("${week.activityCount()} runs across ${week.runningDays()} recorded running days" +
            (week.longestRunMetres()?.let { " · longest ${formatDistance(it, state)}" } ?: ""), style = MaterialTheme.typography.bodySmall)
    }
    val latest = weeks.lastOrNull()
    val previous = weeks.takeLast(5).dropLast(1)
    if (latest != null && latest.activityCount() > 0 && latest.missingDistanceCount() == 0 &&
        previous.size == 4 && previous.all { it.activityCount() > 0 && it.missingDistanceCount() == 0 && it.distanceMetres() != null }) {
        val average = previous.sumOf { it.distanceMetres() } / 4
        if (average > 0) {
            val percent = (latest.distanceMetres() / average - 1) * 100
            Text("This week so far: ${if (percent >= 0) "+" else ""}${decimal(percent)}% recorded distance compared with the previous four observed weeks. This is not a training-load or readiness score.")
        }
    }
}

@Composable
private fun ResultEvidence(entry: PersonalBestEntry, state: CoachUiState, onActivity: (String) -> Unit) {
    val units = state.profile?.preferences()?.units() ?: UnitSystem.METRIC
    Text(PaceCalculator.formatTime(entry.result().duration()), style = MaterialTheme.typography.headlineMedium)
    Text("Pace: ${entry.pace(units)}")
    Text("${friendlyDate(entry.result().date())} · ${resultSource(entry.result())}")
    entry.result().eventName()?.takeIf { it.isNotBlank() }?.let { Text(it) }
    entry.previousBest()?.let { Text("Previous PB: ${PaceCalculator.formatTime(it.duration())} on ${friendlyDate(it.date())}") }
        ?: Text("First supported result at this distance.")
    entry.improvement()?.let { Text("Improvement: ${PaceCalculator.formatTime(it)}") }
    LinkedActivity(entry.result(), state, onActivity)
}

@Composable
private fun LinkedActivity(result: RecordedResult, state: CoachUiState, open: (String) -> Unit) {
    val linked = result.activityId() ?: return
    if (state.activities.any { it.id() == linked }) TextButton(onClick = { open(linked) }) { Text("View linked activity") }
    else Text("Linked activity is not currently available on this device.", style = MaterialTheme.typography.bodySmall)
}

private fun resultSource(result: RecordedResult): String = when (result.source()) {
    ResultSource.OFFICIAL_CHIP -> "Official chip time (entered)"
    ResultSource.MANUAL -> "Manually entered result"
    ResultSource.ROLLING -> "Calculated rolling effort"
}

@Composable
private fun ResultEditor(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    var code by rememberSaveable { mutableStateOf(PerformanceDistance.FIVE_K.code()) }
    var time by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var event by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var official by rememberSaveable { mutableStateOf(false) }
    var activityId by rememberSaveable { mutableStateOf<String?>(null) }
    var chooseActivity by rememberSaveable { mutableStateOf(false) }
    var activitySearch by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachCard {
        Text("Record a result", style = MaterialTheme.typography.titleLarge)
        PerformanceDistance.CORE.forEach { distance ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                RadioButton(code == distance.code(), { code = distance.code() })
                TextButton(onClick = { code = distance.code() }) { Text(distance.label()) }
            }
        }
        CoachField("Finishing time (MM:SS or H:MM:SS)", time, { time = it })
        CoachField("Result date (YYYY-MM-DD)", date, { date = it })
        CoachField("Race or event name (optional)", event, { event = it })
        CheckRow("This is my official chip time", official) { official = it }
        Text("Otherwise this is a manually entered result. Both are kept independently of GPS-recorded totals.")
        Text(activityId?.let { "Linked activity: ${state.activities.firstOrNull { a -> a.id() == it }?.let(::displayActivityDate) ?: it}" } ?: "No linked activity")
        TextButton(onClick = { chooseActivity = !chooseActivity }) { Text(if (chooseActivity) "Hide activity choices" else "Choose optional linked activity") }
        if (chooseActivity) {
            CoachField("Find activity date (e.g. 19 October, optional)", activitySearch, { activitySearch = it })
            TextButton(onClick = { activityId = null; chooseActivity = false }) { Text("No linked activity") }
            val matches = state.activities.filter { activitySearch.isBlank() || displayActivityDate(it).contains(activitySearch.trim()) }
            matches.take(100).forEach { activity ->
                TextButton(onClick = { activityId = activity.id(); chooseActivity = false }) {
                    Text("${displayActivityDate(activity)} · ${activity.distanceMetres()?.let { formatDistance(it, state) } ?: "distance unavailable"}")
                }
            }
            if (matches.size > 100) Text("Only the most recent 100 matches are shown. Enter a date to find older activities. Linking an activity is optional.")
            if (matches.isEmpty()) Text("No retained activities match that date.")
        }
        CoachField("Result notes (optional)", note, { note = it }, singleLine = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val day = LocalDate.parse(date.trim())
                require(!day.isAfter(LocalDate.now())) { "A completed race result cannot be in the future." }
                val distance = PerformanceDistance.CORE.first { it.code() == code }
                val result = RecordedResult(UUID.randomUUID().toString(), distance, PaceCalculator.parseTime(time), day,
                    event.trim().takeIf { it.isNotEmpty() }, if (official) ResultSource.OFFICIAL_CHIP else ResultSource.MANUAL,
                    activityId, note.trim().takeIf { it.isNotEmpty() })
                error = null
                model.saveResult(result, onDone)
            } catch (failure: Exception) { error = failure.message ?: "Check the result date and finishing time." }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save result") }
    }
}

@Composable
fun ShoesScreen(state: CoachUiState, model: CoachViewModel) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val metrics = remember(state.shoes, state.activities) {
        state.shoes.associate { shoe -> shoe.id() to ShoeMetricsCalculator.calculate(shoe, state.activities, Clock.systemDefaultZone()) }
    }
    CoachPage {
        CoachHeading("Your shoes")
        Text("Mileage is recalculated from the current run assignments. Import updates and later corrections therefore change the recorded total.")
        Button(onClick = { editingId = null; showEditor = true }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Add shoes") }
        if (showEditor) ShoeEditor(state.shoes.firstOrNull { it.id() == editingId }, state, model) { showEditor = false }
        if (state.shoes.isEmpty()) Text("No shoes have been added. You can assign shoes to runs after adding them here.")
        state.shoes.forEach { shoe ->
            val summary = metrics.getValue(shoe.id())
            CoachCard {
                Text("${shoe.brand()} ${shoe.model()}".trim(), style = MaterialTheme.typography.titleLarge)
                Text(when { shoe.retired() -> "Retired"; shoe.defaultShoe() -> "Default for newly added runs"; else -> "Active" })
                Text("Recorded distance: ${formatDistance(summary.recordedDistanceMetres(), state)}")
                summary.totalDistanceMetres()?.let { Text("Known mileage including entered starting mileage: ${formatDistance(it, state)}") }
                    ?: Text("Earlier mileage was not supplied; recorded distance is a known lower bound, not lifetime shoe mileage.")
                Text("${summary.runCount()} assigned runs · ${summary.runsWithKnownDistance()} with known distance")
                if (summary.runsWithKnownDistance() < summary.runCount()) Text("Some assigned runs have no distance. Mileage totals include known distances only.")
                summary.averageRunDistanceMetres()?.let { Text("Average known run distance: ${formatDistance(it, state)}") }
                summary.longestRunMetres()?.let { Text("Longest assigned run: ${formatDistance(it, state)}") }
                summary.ageDays()?.let { Text("Age since entered purchase date: $it days") }
                summary.firstRecordedRun()?.let { Text("First recorded run: ${friendlyDate(it.atZone(ZoneId.systemDefault()).toLocalDate())} (device time zone)") }
                if (summary.firstRecordedRun() == null) Text("No first recorded run yet.")
                Text(summary.wearGuidance())
                shoe.notes()?.takeIf { it.isNotBlank() }?.let { Text(it) }
                OutlinedButton(onClick = { editingId = shoe.id(); showEditor = true }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Edit shoes") }
                Text("Assign or change these shoes from an individual run’s detail page.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ShoeEditor(shoe: Shoe?, state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    val id = shoe?.id() ?: "new"
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val metresPerUnit = if (imperial) 1609.344 else 1000.0
    var brand by rememberSaveable(id) { mutableStateOf(shoe?.brand() ?: "") }
    var name by rememberSaveable(id) { mutableStateOf(shoe?.model() ?: "") }
    var date by rememberSaveable(id) { mutableStateOf(shoe?.purchaseDate()?.toString() ?: "") }
    var starting by rememberSaveable(id) { mutableStateOf(shoe?.startingDistanceMetres()?.let { decimal(it / metresPerUnit) } ?: "") }
    var retired by rememberSaveable(id) { mutableStateOf(shoe?.retired() ?: false) }
    var default by rememberSaveable(id) { mutableStateOf(shoe?.defaultShoe() ?: false) }
    var notes by rememberSaveable(id) { mutableStateOf(shoe?.notes() ?: "") }
    var error by rememberSaveable(id) { mutableStateOf<String?>(null) }
    CoachCard {
        Text(if (shoe == null) "Add shoes" else "Edit shoes", style = MaterialTheme.typography.titleLarge)
        CoachField("Brand (optional)", brand, { brand = it })
        CoachField("Model", name, { name = it })
        CoachField("Purchase date (YYYY-MM-DD, optional)", date, { date = it })
        CoachField("Starting mileage (${if (imperial) "mi" else "km"}, optional)", starting, { starting = it })
        Text("Leave starting mileage blank if it is unknown. Enter 0 only if no earlier running mileage needs to be included.")
        CheckRow("Retired", retired) { retired = it; if (it) default = false }
        if (!retired) CheckRow("Use as default for new runs", default) { default = it }
        CoachField("Shoe notes (optional)", notes, { notes = it }, singleLine = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val purchase = date.trim().takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
                require(purchase == null || !purchase.isAfter(LocalDate.now())) { "Purchase date cannot be in the future." }
                val mileage = optionalNumber(starting)?.times(metresPerUnit)
                val result = Shoe(shoe?.id() ?: UUID.randomUUID().toString(), brand.trim(), name.trim(), purchase,
                    mileage, retired, notes.trim().takeIf { it.isNotEmpty() }, default && !retired)
                error = null
                model.saveShoe(result, onDone)
            } catch (failure: Exception) { error = failure.message ?: "Check the shoe details." }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save shoes") }
        TextButton(onClick = onDone) { Text("Cancel shoe changes") }
    }
}
