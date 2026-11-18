package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import org.racedaydreams.coach.*
import org.racedaydreams.coach.activity.SharedResultEditor
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ResultSource
import org.racedaydreams.domain.time.CalendarDates
import org.racedaydreams.domain.time.Offsets
import platform.Foundation.NSUUID

@Composable
internal fun AppleRecordedResults(
    session: AppleCoachSession<*>,
    back: () -> Unit,
    openActivity: (String) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
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
                        CalendarDates.atOffset(
                            it.start(),
                            it.startOffset() ?: Offsets.ofTotalSeconds(0),
                        )
                    )
                },
                formatDistance = { distanceLabel(it, imperial) },
                newId = { NSUUID().UUIDString },
                saveResult = { session.services.runningEvidence.saveResult(it) { editing = false } },
            )
        }
        (state.error ?: state.message)?.let { Text(it) }
        if (state.results.isEmpty()) Text("No race results have been saved.")
        state.results
            .sortedByDescending { it.date().toEpochDay() }
            .forEach { result ->
                CoachCard {
                    Text(
                        "${result.distance().label()} · ${PaceCalculator.formatTime(result.duration())}"
                    )
                    Text(AppleCalendarDates.label(result.date()))
                    result.eventName()?.let { Text(it) }
                    result.note()?.let { Text(it) }
                    Text(
                        when (result.source()) {
                            ResultSource.OFFICIAL_CHIP -> "Official chip time (entered)"
                            ResultSource.MANUAL -> "Manually entered result"
                            ResultSource.ROLLING -> "Calculated rolling effort"
                        }
                    )
                    result.activityId()?.let { id ->
                        TextButton(onClick = { openActivity(id) }) { Text("View linked run") }
                    }
                }
            }
    }
}
