package org.racedaydreams.coach.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedResultEditor(
    state: CoachUiState,
    today: LocalDate,
    parseDate: (String) -> LocalDate,
    resultDateField: @Composable (String, (String) -> Unit) -> Unit,
    describeActivity: (ActivityRecord) -> String,
    formatDistance: (Double) -> String,
    newId: () -> String,
    saveResult: (RecordedResult) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf(PerformanceDistance.FIVE_K.code()) }
    var time by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today.toString()) }
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
        CoachTextInput("Finishing time (MM:SS or H:MM:SS)", time, { time = it })
        resultDateField(date) { date = it }
        CoachTextInput("Race or event name (optional)", event, { event = it })
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Checkbox(official, { official = it })
            TextButton(onClick = { official = !official }) { Text("This is my official chip time") }
        }
        Text(
            "Otherwise this is a manually entered result. Both are kept independently of GPS-recorded totals."
        )
        Text(
            activityId?.let {
                "Linked activity: ${state.activities.firstOrNull { a -> a.id() == it }?.let(describeActivity) ?: it}"
            } ?: "No linked activity"
        )
        TextButton(onClick = { chooseActivity = !chooseActivity }) {
            Text(if (chooseActivity) "Hide activity choices" else "Choose optional linked activity")
        }
        if (chooseActivity) {
            CoachTextInput(
                "Find activity date (e.g. 19 October, optional)",
                activitySearch,
                { activitySearch = it },
            )
            TextButton(
                onClick = {
                    activityId = null
                    chooseActivity = false
                }
            ) {
                Text("No linked activity")
            }
            val matches =
                state.activities.filter {
                    activitySearch.isBlank() || describeActivity(it).contains(activitySearch.trim())
                }
            matches.take(100).forEach { activity ->
                TextButton(
                    onClick = {
                        activityId = activity.id()
                        chooseActivity = false
                    }
                ) {
                    Text(
                        "${describeActivity(activity)} · ${activity.distanceMetres()?.let { formatDistance(it) } ?: "distance unavailable"}"
                    )
                }
            }
            if (matches.size > 100)
                Text(
                    "Only the most recent 100 matches are shown. Enter a date to find older activities. Linking an activity is optional."
                )
            if (matches.isEmpty()) Text("No retained activities match that date.")
        }
        OutlinedTextField(
            note,
            { note = it },
            label = { Text("Result notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    val day = parseDate(date.trim())
                    require(day.toEpochDay() <= today.toEpochDay()) {
                        "A completed race result cannot be in the future."
                    }
                    val distance = PerformanceDistance.CORE.first { it.code() == code }
                    val result =
                        RecordedResult(
                            newId(),
                            distance,
                            PaceCalculator.parseTime(time),
                            day,
                            event.trim().takeIf { it.isNotEmpty() },
                            if (official) ResultSource.OFFICIAL_CHIP else ResultSource.MANUAL,
                            activityId,
                            note.trim().takeIf { it.isNotEmpty() },
                        )
                    error = null
                    saveResult(result)
                } catch (failure: Exception) {
                    error = failure.message ?: "Check the result date and finishing time."
                }
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save result")
        }
    }
}
