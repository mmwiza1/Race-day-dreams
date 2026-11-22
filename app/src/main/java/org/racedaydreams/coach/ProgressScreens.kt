package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.MilestoneEngine
import org.racedaydreams.domain.activity.PerformanceDistance
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.PersonalBestEntry
import org.racedaydreams.domain.activity.RecordedResult
import org.racedaydreams.domain.activity.ResultSource

/** Presentation of retained evidence. Blank metrics are not substituted with predicted values. */
@Composable
fun ProgressScreen(
    state: CoachUiState,
    model: CoachViewModel,
    onActivity: (String) -> Unit,
    onShoes: () -> Unit,
    onStrength: () -> Unit = {},
    section: String = "pbs",
) {
    val today = LocalDate.now()
    val displayZone = ZoneId.systemDefault()
    val current =
        remember(state.results) {
            PersonalBestCalculator.current(state.results).associateBy {
                it.result().distance().code()
            }
        }
    val history = remember(state.results) { PersonalBestCalculator.history(state.results) }
    val milestones =
        remember(state.activities, state.results, state.milestonesEnabled) {
            MilestoneEngine.calculate(state.milestonesEnabled, state.activities, state.results)
        }
    var expandedDistance by rememberSaveable { mutableStateOf<String?>(null) }
    var enteringResult by rememberSaveable { mutableStateOf(false) }
    var showAllEntered by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        if (section == "trends") {
            SharedTrainingTrends(
                state.activities,
                today,
                { it.atZone(displayZone).toLocalDate() },
                state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
                { formatDistance(it, state) },
                ::decimal,
                ::formatPaceSeconds,
                ::shortDate,
                ::friendlyDate,
                onActivity,
                null,
            )
            run {
                val insights =
                    remember(state.activities) {
                        org.racedaydreams.domain.polish.TrainingInsights.from(
                            state.activities,
                            java.time.Instant.now(),
                        )
                    }
                if (insights.isNotEmpty()) {
                    WhyDetails(
                        insights.joinToString("\n\n") { "${it.title()} ${it.evidence()}" },
                        "Longer-term recorded insights",
                    )
                }
                WhyDetails(
                    "These are summaries of retained records, not a claim that unrecorded training did not happen. No readiness score or race prediction is generated.",
                    "About these trends",
                )
            }

            val now = java.time.Instant.now()
            val load =
                remember(state.activities, state.strengthSessions) {
                    org.racedaydreams.domain.coaching.TrainingLoadEvidence.window(
                        state.activities,
                        state.strengthSessions,
                        now.minus(java.time.Duration.ofDays(7)),
                        now,
                    )
                }
            WhyDetails(
                (load.runningLoad()?.let {
                    "Running: ${kotlin.math.round(it).toInt()} min·RPE from ${load.runsWithRpe()}/${load.runsObserved()} runs in the last seven days"
                } ?: "Running load: unavailable — no RPE recorded in this window") +
                    "\n\n" +
                    (load.strengthLoad()?.let {
                        "Strength: ${kotlin.math.round(it).toInt()} min·RPE from ${load.strengthWithRpe()}/${load.strengthObserved()} sessions"
                    } ?: "Strength load: unavailable — no RPE recorded in this window") +
                    "\n\nDuration × reported perceived effort (RPE). Missing effort is excluded, not filled with zero. Running and strength loads are separate context signals, not injury predictions.",
                "Recent session-RPE context",
            )
        }
        if (section == "weight") WeightSection(state, model)
        if (section == "pbs") {
            CoachHeading("Running PBs")
            WhyDetails(
                "Results retain their date and source. Official chip times are entered by you, not independently verified. Rolling efforts require reliable continuous samples.",
                "How PBs are calculated",
            )
            Button(
                onClick = { enteringResult = !enteringResult },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(if (enteringResult) "Close result form" else "Add a race or manual result")
            }
            if (enteringResult) ResultEditor(state, model) { enteringResult = false }
            var showAllDistances by rememberSaveable { mutableStateOf(false) }
            if (current.isEmpty())
                Text(
                    "No running PBs yet. Add a race or manual result, or continue training; supported PBs appear when sufficient evidence exists."
                )
            TextButton(onClick = { showAllDistances = !showAllDistances }) {
                Text(if (showAllDistances) "Show recorded distances" else "View all distances")
            }
            PerformanceDistance.CORE.filter { showAllDistances || current.containsKey(it.code()) }
                .forEach { distance ->
                    val best = current[distance.code()]
                    CoachCard {
                        Text(distance.label(), style = MaterialTheme.typography.titleLarge)
                        if (best == null) Text("No supported result recorded yet.")
                        else {
                            PbCelebration(
                                state,
                                model,
                                "running:" + best.result().id(),
                                best.improvement()?.let {
                                    "${PaceCalculator.formatTime(it)} faster than the previous retained PB"
                                },
                            )
                            ResultEvidence(best, state, onActivity)
                            val ranked =
                                remember(state.results, distance.code()) {
                                    rankedRunningResults(state.results, distance.code())
                                }
                            ranked
                                .filter { it.id() != best.result().id() }
                                .forEachIndexed { index, result ->
                                    HorizontalDivider()
                                    RankedRunningResult(result, index + 2, state, onActivity)
                                }
                            TextButton(
                                onClick = {
                                    expandedDistance =
                                        if (expandedDistance == distance.code()) null
                                        else distance.code()
                                }
                            ) {
                                Text(
                                    if (expandedDistance == distance.code()) "Hide PB progression"
                                    else "Show PB progression"
                                )
                            }
                            if (expandedDistance == distance.code()) {
                                history
                                    .filter { it.result().distance().code() == distance.code() }
                                    .forEach { past ->
                                        HorizontalDivider()
                                        Text(
                                            "${friendlyDate(past.result().date())} · ${PaceCalculator.formatTime(past.result().duration())} · ${resultSource(past.result())}"
                                        )
                                        past.improvement()?.let {
                                            Text("Improvement: ${PaceCalculator.formatTime(it)}")
                                        }
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
                        Text(
                            "${result.distance().label()} · ${PaceCalculator.formatTime(result.duration())}"
                        )
                        Text("${friendlyDate(result.date())} · ${resultSource(result)}")
                        result.eventName()?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        LinkedActivity(result, state, onActivity)
                        TextButton(
                            onClick = { model.runningEvidence.deleteResult(result.id()) },
                            enabled = !state.busy,
                        ) {
                            Text("Remove entered result")
                        }
                    }
                }
                if (entered.size > 20)
                    TextButton(onClick = { showAllEntered = !showAllEntered }) {
                        Text(
                            if (showAllEntered) "Show recent entered results"
                            else "Show all ${entered.size} entered results"
                        )
                    }
            }
        }
        if (section == "trends") {
            CoachHeading("Shoes and mileage")
            CoachButton("View and manage shoes", onShoes)
            CoachCard {
                Text("Optional milestones", style = MaterialTheme.typography.titleMedium)
                CheckRow(
                    "Show restrained recorded milestones",
                    state.milestonesEnabled,
                    model::setMilestonesEnabled,
                )
                Text(
                    "No streaks or milestone notifications. A first recorded distance refers to this app’s history, not your lifetime running."
                )
                if (state.milestonesEnabled) {
                    if (milestones.isEmpty())
                        Text("No qualifying milestones in the retained evidence yet.")
                    milestones.forEach { milestone ->
                        Text(
                            "${milestone.label()} · ${milestone.date()?.let(::friendlyDate) ?: "Date unavailable"}"
                        )
                        milestone.explanation()?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        milestone
                            .activityId()
                            ?.takeIf { id -> state.activities.any { it.id() == id } }
                            ?.let { id ->
                                TextButton(onClick = { onActivity(id) }) {
                                    Text("View milestone activity")
                                }
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultEvidence(
    entry: PersonalBestEntry,
    state: CoachUiState,
    onActivity: (String) -> Unit,
) {
    RankedRunningResult(entry.result(), 1, state, onActivity)
    entry.previousBest()?.let {
        Text(
            "Previous PB: ${PaceCalculator.formatTime(it.duration())} on ${friendlyDate(it.date())}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
        ?: Text(
            "First supported result at this distance.",
            style = MaterialTheme.typography.bodySmall,
        )
    entry.improvement()?.let {
        Text(
            "Improvement: ${PaceCalculator.formatTime(it)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RankedRunningResult(
    result: RecordedResult,
    rank: Int,
    state: CoachUiState,
    onActivity: (String) -> Unit,
) {
    SharedRankedRunningResult(
        result,
        rank,
        state.profile?.preferences()?.units() ?: UnitSystem.METRIC,
        ::friendlyDate,
    ) {
        LinkedActivity(result, state, onActivity)
    }
}

@Composable
private fun LinkedActivity(result: RecordedResult, state: CoachUiState, open: (String) -> Unit) {
    val linked = result.activityId() ?: return
    if (state.activities.any { it.id() == linked })
        TextButton(onClick = { open(linked) }) { Text("View linked activity") }
    else
        Text(
            "Linked activity is not currently available on this device.",
            style = MaterialTheme.typography.bodySmall,
        )
}

private fun resultSource(result: RecordedResult): String = sharedResultSource(result)

@Composable
private fun ResultEditor(state: CoachUiState, model: CoachViewModel, onDone: () -> Unit) {
    org.racedaydreams.coach.activity.SharedResultEditor(
        state = state,
        today = LocalDate.now(),
        parseDate = LocalDate::parse,
        resultDateField = { value, change ->
            CoachField("Result date (YYYY-MM-DD)", value, change)
        },
        describeActivity = ::displayActivityDate,
        formatDistance = { formatDistance(it, state) },
        newId = { UUID.randomUUID().toString() },
        saveResult = { model.runningEvidence.saveResult(it, onDone) },
    )
}

@Composable
fun ShoesScreen(state: CoachUiState, model: CoachViewModel) {
    org.racedaydreams.coach.activity.SharedShoesScreen(
        state = state,
        today = LocalDate.now(),
        now = java.time.Instant::now,
        newId = { UUID.randomUUID().toString() },
        parseDate = LocalDate::parse,
        formatDecimal = ::decimal,
        formatDistance = { formatDistance(it, state) },
        firstRunDate = { friendlyDate(it.atZone(ZoneId.systemDefault()).toLocalDate()) },
        purchaseDateField = { value, change ->
            CoachField("Purchase date (YYYY-MM-DD, optional)", value, change)
        },
        saveShoe = model.runningEvidence::saveShoe,
    )
}
