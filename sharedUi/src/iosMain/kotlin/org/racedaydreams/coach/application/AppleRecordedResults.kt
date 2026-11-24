package org.racedaydreams.coach.application

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import org.racedaydreams.coach.*
import org.racedaydreams.coach.activity.SharedResultEditor
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.PerformanceDistance
import org.racedaydreams.domain.activity.PersonalBestCalculator
import org.racedaydreams.domain.activity.ResultSource
import platform.Foundation.NSUUID

@Composable
internal fun AppleRecordedResults(
    session: AppleCoachSession<*>,
    back: () -> Unit,
    openActivity: (String) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    var showAllEntered by rememberSaveable { mutableStateOf(false) }
    var removingId by rememberSaveable { mutableStateOf<String?>(null) }
    val entered =
        remember(state.results) {
            state.results
                .filter { it.source() != ResultSource.ROLLING }
                .sortedByDescending { it.date().toEpochDay() }
        }
    val pendingRemoval = entered.firstOrNull { it.id() == removingId }
    if (pendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { removingId = null },
            title = { Text("Remove entered result?") },
            text = {
                Text(
                    "Remove the ${pendingRemoval.distance().label()} result from ${AppleCalendarDates.label(pendingRemoval.date())}? Any linked run stays in your history."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        removingId = null
                        session.services.runningEvidence.deleteResult(pendingRemoval.id())
                    },
                    enabled = !state.busy,
                ) {
                    Text("Remove result")
                }
            },
            dismissButton = { TextButton(onClick = { removingId = null }) { Text("Cancel") } },
        )
    }
    val units = state.profile?.preferences()?.units() ?: UnitSystem.METRIC
    val imperial = units == UnitSystem.IMPERIAL
    val history = remember(state.results) { PersonalBestCalculator.history(state.results) }
    CoachPage {
        CoachButton("Back", back)
        CoachHeading("Recorded race results")
        TextButton(onClick = { editing = !editing }, enabled = !state.busy) {
            Text(if (editing) "Cancel result entry" else "Record a result")
        }
        if (editing) {
            SharedResultEditor(
                state = state,
                today = AppleCalendarDates.today(),
                parseDate = AppleCalendarDates::parse,
                resultDateField = { value, change ->
                    CoachTextInput("Result date (YYYY-MM-DD)", value, change)
                },
                describeActivity = {
                    AppleCalendarDates.label(
                        org.racedaydreams.domain.activity.ActivityDates.localDate(it)
                    )
                },
                formatDistance = { distanceLabel(it, imperial) },
                newId = { NSUUID().UUIDString },
                saveResult = { session.services.runningEvidence.saveResult(it) { editing = false } },
            )
        }
        (state.error ?: state.message)?.let { Text(it) }
        CoachHeading("Running personal bests")
        if (history.isEmpty()) Text("No supported running personal bests yet.")
        PerformanceDistance.CORE.forEach { distance ->
            val ranked =
                remember(state.results, distance) {
                    sharedRankedRunningResults(state.results, distance.code())
                }
            if (ranked.isNotEmpty()) {
                var expanded by rememberSaveable(distance.code()) { mutableStateOf(false) }
                CoachCard {
                    Text(distance.label(), style = MaterialTheme.typography.titleLarge)
                    ranked.forEachIndexed { index, result ->
                        if (index > 0) HorizontalDivider()
                        SharedRankedRunningResult(
                            result,
                            index + 1,
                            units,
                            AppleCalendarDates::label,
                        ) {
                            result.activityId()?.let { id ->
                                if (state.activities.any { it.id() == id }) {
                                    TextButton(onClick = { openActivity(id) }) {
                                        Text("View linked run")
                                    }
                                } else Text("Linked run is unavailable on this device.")
                            }
                        }
                    }
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "Hide PB progression" else "Show PB progression")
                    }
                    if (expanded) {
                        history
                            .filter { it.result().distance().code() == distance.code() }
                            .forEach { entry ->
                                val result = entry.result()
                                Text(
                                    "${AppleCalendarDates.label(result.date())} · ${PaceCalculator.formatTime(result.duration())} · ${sharedResultSource(result)}"
                                )
                                entry.improvement()?.let {
                                    Text("Improvement: ${PaceCalculator.formatTime(it)}")
                                }
                            }
                    }
                }
            }
        }
        CoachHeading("Entered race results")
        if (entered.isEmpty()) Text("No race results have been entered.")
        (if (showAllEntered) entered else entered.take(20)).forEach { result ->
            CoachCard {
                Text(
                    "${result.distance().label()} · ${PaceCalculator.formatTime(result.duration())}"
                )
                Text(AppleCalendarDates.label(result.date()))
                result.eventName()?.let { Text(it) }
                result.note()?.let { Text(it) }
                Text(sharedResultSource(result))
                result.activityId()?.let { id ->
                    if (state.activities.any { it.id() == id }) {
                        TextButton(onClick = { openActivity(id) }) { Text("View linked run") }
                    } else Text("Linked run is unavailable on this device.")
                }
                TextButton(onClick = { removingId = result.id() }, enabled = !state.busy) {
                    Text("Remove entered result")
                }
            }
        }
        if (entered.size > 20) {
            TextButton(onClick = { showAllEntered = !showAllEntered }) {
                Text(
                    if (showAllEntered) "Show recent entered results"
                    else "Show all ${entered.size} entered results"
                )
            }
        }
    }
}
