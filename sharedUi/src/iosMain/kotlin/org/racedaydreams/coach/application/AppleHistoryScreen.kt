package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.time.*

@Composable
fun AppleHistoryScreen(session: AppleCoachSession<*>, open: (String) -> Unit) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var showStrength by remember { mutableStateOf(false) }
    if (showStrength) {
        CoachPage {
            CoachHeading("Strength history")
            CoachButton("Back to history") { showStrength = false }
            state.strengthSessions
                .sortedByDescending { it.startedAt() }
                .forEach { workout ->
                    CoachCard {
                        Text(dateLabel(workout.startedAt(), workout.offset()))
                        workout.exercises().forEach { Text(it.exercise().name()) }
                    }
                }
        }
        return
    }
    SharedActivityHistoryScreen(
        state = state,
        open = open,
        allStrength = { showStrength = true },
        loadSummaryPage = { time, id, limit ->
            withContext(applicationIoDispatcher()) {
                session.activities.activitySummaryPage(time, id, limit)
            }
        },
        activityDate = { dateLabel(it.start(), it.startOffset()) },
        strengthDate = ::dateLabel,
        distance = { distanceLabel(it, imperial) },
        elapsedPace = { activity, miles ->
            activity
                .distanceMetres()
                ?.takeIf { it > 0 }
                ?.let { metres ->
                    val seconds =
                        activity.elapsedTime().getSeconds() +
                            activity.elapsedTime().getNano() / 1_000_000_000.0
                    val unitMetres = if (miles) 1609.344 else 1000.0
                    val pace =
                        PaceCalculator.formatTime(
                            Durations.ofSeconds((seconds * unitMetres / metres).roundToLong())
                        )
                    "$pace/${if (miles) "mi" else "km"}"
                }
        },
        emptyMessage =
            "No completed runs yet. Record a run or import selected workouts from Apple Health.",
    )
}

@Composable
fun AppleRunDetails(session: AppleCoachSession<*>, id: String) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var activity by remember(id) { mutableStateOf<ActivityRecord?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        try {
            activity =
                withContext(applicationIoDispatcher()) { session.activities.activityDetail(id) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }
    CoachPage {
        CoachHeading("Recorded run")
        val run = activity
        when {
            loading -> Text("Loading run…")
            failed -> Text("Could not load the run. Your records are retained.")
            run == null -> Text("This run is unavailable.")
            else ->
                CoachCard {
                    Text(dateLabel(run.start(), run.startOffset()))
                    Text(
                        run.distanceMetres()?.let { distanceLabel(it, imperial) }
                            ?: "Distance unavailable"
                    )
                    Text("Elapsed: ${PaceCalculator.formatTime(run.elapsedTime())}")
                    run.movingTime()?.let { Text("Active: ${PaceCalculator.formatTime(it)}") }
                    Text("Source: ${ActivitySources.friendly(run.source())}")
                    run.notes()?.takeIf { it.isNotBlank() }?.let { Text(it) }
                }
        }
    }
}

private fun dateLabel(instant: Instant, offset: ZoneOffset?): String {
    val date = CalendarDates.atOffset(instant, offset ?: Offsets.ofTotalSeconds(0))
    val day = date.getDayOfMonth().toString().padStart(2, '0')
    val month = date.getMonthValue().toString().padStart(2, '0')
    return "$day/$month/${date.getYear()}"
}

private fun distanceLabel(metres: Double, miles: Boolean): String {
    val hundredths = (metres / (if (miles) 1609.344 else 1000.0) * 100).roundToLong()
    val fraction = (hundredths % 100).toString().padStart(2, '0')
    return "${hundredths / 100}.$fraction ${if (miles) "mi" else "km"}"
}
