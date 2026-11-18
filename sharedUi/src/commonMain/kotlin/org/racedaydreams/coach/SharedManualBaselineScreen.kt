package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.baseline.ManualBaselineInput

@Composable
fun SharedManualBaselineScreen(
    state: CoachUiState,
    decimal: (Double) -> String,
    formatPaceSeconds: (Double) -> String,
    save: (ManualBaselineInput) -> Unit,
    onDone: () -> Unit,
) {
    val baseline = state.baseline
    val saved = state.manualBaseline
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val kmPerUnit = if (imperial) 1.609344 else 1.0
    val unit = if (imperial) "mi" else "km"
    var correct by rememberSaveable { mutableStateOf(false) }
    var weekly by rememberSaveable {
        mutableStateOf(saved.weeklyDistanceKm()?.let { decimal(it / kmPerUnit) } ?: "")
    }
    var frequency by rememberSaveable { mutableStateOf(saved.runsPerWeek()?.let(decimal) ?: "") }
    var longest by rememberSaveable {
        mutableStateOf(saved.longestRunKm()?.let { decimal(it / kmPerUnit) } ?: "")
    }
    var five by rememberSaveable {
        mutableStateOf(saved.recent5k()?.let(PaceCalculator::formatTime) ?: "")
    }
    var ten by rememberSaveable {
        mutableStateOf(saved.recent10k()?.let(PaceCalculator::formatTime) ?: "")
    }
    var half by rememberSaveable {
        mutableStateOf(saved.recentHalf()?.let(PaceCalculator::formatTime) ?: "")
    }
    var easy by rememberSaveable {
        mutableStateOf(
            saved.easyPaceSecondsPerKm()?.let { formatPaceSeconds(it * kmPerUnit) } ?: ""
        )
    }
    var weeks by rememberSaveable { mutableStateOf(saved.consistentWeeks()?.toString() ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachPage {
        CoachHeading("Optional current-running details")
        Text(
            "We only ask for gaps by default. You can correct recorded values if they do not represent your current running. These entries are marked manual, rather than measured."
        )
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Checkbox(correct, { correct = it })
            TextButton(onClick = { correct = !correct }) {
                Text("Correct values already available")
            }
        }
        if (correct || baseline.weeklyDistanceKm() == null || saved.weeklyDistanceKm() != null)
            BaselineField(
                "Current weekly distance ($unit)",
                weekly,
                { weekly = it },
                baseline.weeklyDistanceKm()?.let { "Recorded: ${decimal(it / kmPerUnit)} $unit" },
            )
        if (correct || baseline.runsPerWeek() == null || saved.runsPerWeek() != null)
            BaselineField(
                "Current usual runs/week",
                frequency,
                { frequency = it },
                baseline.runsPerWeek()?.let { "Recorded: ${decimal(it)}" },
            )
        if (correct || baseline.longestRunKm() == null || saved.longestRunKm() != null)
            BaselineField(
                "Longest recent run ($unit)",
                longest,
                { longest = it },
                baseline.longestRunKm()?.let { "Recorded: ${decimal(it / kmPerUnit)} $unit" },
            )
        if (correct || baseline.recent5k() == null || saved.recent5k() != null)
            BaselineField("Recent 5K result (MM:SS or H:MM:SS)", five, { five = it })
        if (correct || baseline.recent10k() == null || saved.recent10k() != null)
            BaselineField("Recent 10K result", ten, { ten = it })
        if (correct || baseline.recentHalf() == null || saved.recentHalf() != null)
            BaselineField("Recent half-marathon result", half, { half = it })
        if (
            correct ||
                baseline.easyPaceSecondsPerKm() == null ||
                saved.easyPaceSecondsPerKm() != null
        )
            BaselineField("Approximate easy pace (MM:SS/$unit)", easy, { easy = it })
        if (correct || baseline.consistentWeeks() == null || saved.consistentWeeks() != null)
            BaselineField("Weeks running consistently", weeks, { weeks = it })
        Text(
            "Every field is optional. Blank fields leave recorded evidence in place; use Reset manual corrections to remove previous manual entries."
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    val input =
                        ManualBaselineInput(
                            baselineNumber(weekly)?.times(kmPerUnit),
                            baselineNumber(frequency),
                            baselineNumber(longest)?.times(kmPerUnit),
                            baselineDuration(five),
                            baselineDuration(ten),
                            baselineDuration(half),
                            baselineDuration(easy)?.getSeconds()?.toDouble()?.div(kmPerUnit),
                            weeks.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                                ?: if (weeks.isBlank()) null
                                else
                                    throw IllegalArgumentException(
                                        "Enter whole weeks, or leave this field blank."
                                    ),
                        )
                    error = null
                    save(input)
                } catch (failure: IllegalArgumentException) {
                    error = failure.message
                }
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save optional baseline")
        }
        OutlinedButton(onClick = { save(ManualBaselineInput.empty()) }, enabled = !state.busy) {
            Text("Reset manual corrections")
        }
        TextButton(onDone) { Text("Continue without changes") }
    }
}

@Composable
private fun BaselineField(
    label: String,
    value: String,
    change: (String) -> Unit,
    supporting: String? = null,
) {
    OutlinedTextField(
        value,
        change,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        supportingText = supporting?.let { text -> { Text(text) } },
        singleLine = true,
    )
}

private fun baselineNumber(value: String): Double? {
    if (value.isBlank()) return null
    val parsed = value.trim().toDoubleOrNull()
    require(parsed != null && parsed.isFinite()) {
        "Enter a finite number, or leave the field blank."
    }
    return parsed
}

private fun baselineDuration(value: String) =
    value.trim().takeIf(String::isNotEmpty)?.let(PaceCalculator::parseTime)
