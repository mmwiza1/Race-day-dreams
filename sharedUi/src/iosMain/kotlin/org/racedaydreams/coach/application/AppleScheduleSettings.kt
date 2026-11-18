package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.CoachButton
import org.racedaydreams.coach.CoachFilterChip
import org.racedaydreams.coach.CoachTextInput
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.storage.SqliteRunningSupportArchives

@Composable
internal fun AppleScheduleSettings(support: SqliteRunningSupportArchives) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Hide session preferences" else "Session preferences")
    }
    if (expanded) AppleScheduleEditor(support)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleScheduleEditor(support: SqliteRunningSupportArchives) {
    val scope = rememberCoroutineScope()
    var minutes by remember { mutableStateOf("60") }
    var distribution by remember { mutableStateOf("BALANCED") }
    var unavailable by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
    var date by remember { mutableStateOf(AppleCalendarDates.today().toString()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(support, attempt) {
        notice = null
        try {
            val saved = withContext(applicationIoDispatcher()) { support.schedule() }
            if (saved != null) {
                minutes = saved.maximumMinutes.toString()
                distribution = saved.distribution
                unavailable = saved.unavailable
            }
            loaded = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            notice = "Could not load session preferences."
        }
    }
    if (!loaded) {
        if (notice == null) Text("Loading session preferences…")
        else {
            Text(requireNotNull(notice))
            TextButton(onClick = { attempt++ }) { Text("Retry session preferences") }
        }
        return
    }
    Text(
        "These preferences apply when you generate or rebuild a plan. Saving these preferences leaves your current programme unchanged."
    )
    CoachTextInput("Usual session budget (minutes)", minutes, { minutes = it }, decimal = true)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
                "FEWER_LONGER" to "Fewer, longer sessions",
                "BALANCED" to "Balanced",
                "MORE_SHORTER" to "More, shorter sessions",
            )
            .forEach { (value, label) ->
                CoachFilterChip(
                    distribution == value,
                    { distribution = value },
                    label = { Text(label) },
                )
            }
    }
    CoachTextInput("Unavailable date (YYYY-MM-DD)", date, { date = it })
    TextButton(
        onClick = {
            try {
                unavailable = unavailable + AppleCalendarDates.parse(date.trim())
                notice = null
            } catch (_: IllegalArgumentException) {
                notice = "Enter a valid unavailable date."
            }
        }
    ) {
        Text("Add unavailable date")
    }
    unavailable
        .sortedBy { it.toEpochDay() }
        .forEach { day ->
            TextButton(onClick = { unavailable = unavailable - day }) {
                Text("Remove ${AppleCalendarDates.label(day)}")
            }
        }
    CoachButton("Save future planning preferences") {
        if (!busy) {
            val budget = minutes.trim().toIntOrNull()
            if (budget == null || budget !in 15..360) {
                notice = "Enter a session budget of 15 to 360 minutes."
            } else {
                val selected =
                    SqliteRunningSupportArchives.SchedulePreferences(
                        budget,
                        distribution,
                        unavailable,
                    )
                busy = true
                notice = null
                scope.launch {
                    try {
                        withContext(applicationIoDispatcher()) { support.saveSchedule(selected) }
                        notice = "Saved. Review and rebuild explicitly from Plan."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        notice = "Could not save session preferences."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }
    notice?.let { Text(it) }
}
