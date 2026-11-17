package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.activity.SplitAnalysis
import org.racedaydreams.domain.activity.SplitCalculator
import org.racedaydreams.domain.time.*

@Composable
fun AppleHistoryScreen(
    session: AppleCoachSession<*>,
    strengthOnly: Boolean = false,
    open: (String) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var showStrength by remember { mutableStateOf(strengthOnly) }
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
    var splits by remember(id, imperial) { mutableStateOf<SplitAnalysis?>(null) }
    var route by remember(id) { mutableStateOf<PreparedLiveRoute?>(null) }
    var detailRemoved by remember(id) { mutableStateOf(false) }
    var showRoute by remember(id) { mutableStateOf(false) }
    var showSplits by remember(id) { mutableStateOf(false) }
    var loading by remember(id) { mutableStateOf(true) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, imperial) {
        try {
            val (run, analysis) =
                withContext(applicationIoDispatcher()) {
                    val run = session.activities.activityDetail(id)
                    run to
                        run?.let {
                            SplitCalculator.calculate(it, if (imperial) 1609.344 else 1000.0)
                        }
                }
            activity = run
            splits = analysis
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }
    var routeFailed by remember(id) { mutableStateOf(false) }
    var routeLoading by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, showRoute) {
        if (!showRoute) return@LaunchedEffect
        routeLoading = true
        try {
            val (prepared, removed) =
                withContext(applicationIoDispatcher()) {
                    val saved = session.activities.recordedRoute(id)
                    saved.points
                        .takeIf { it.isNotEmpty() }
                        ?.let { PreparedLiveRoute.fromPoints(it) } to saved.detailRemoved
                }
            route = prepared
            detailRemoved = removed
            routeFailed = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            routeFailed = true
        } finally {
            routeLoading = false
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
                    TextButton(onClick = { showRoute = !showRoute }) {
                        Text(if (showRoute) "Hide route" else "View recorded route")
                    }
                    if (showRoute) {
                        when {
                            routeLoading -> Text("Loading recorded route…")
                            detailRemoved ->
                                Text(
                                    "Detailed route data was removed. The run summary is retained."
                                )
                            routeFailed -> Text("The recorded route could not be loaded.")
                            else -> SharedLiveRecordedRoute(route, unavailable = route == null)
                        }
                    }
                    TextButton(onClick = { showSplits = !showSplits }) {
                        Text(if (showSplits) "Hide splits" else "View splits")
                    }
                    if (showSplits) {
                        val analysis = splits
                        if (analysis?.splits().isNullOrEmpty())
                            Text("Qualified split data is unavailable.")
                        analysis?.reasons()?.forEach { Text(it) }
                        analysis?.splits()?.forEach { split ->
                            Text(
                                "${split.number()} · ${distanceLabel(split.distanceMetres(), imperial)} · ${PaceCalculator.formatTime(split.duration())}"
                            )
                        }
                    }
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

internal fun distanceLabel(metres: Double, miles: Boolean): String {
    val hundredths = (metres / (if (miles) 1609.344 else 1000.0) * 100).roundToLong()
    val fraction = (hundredths % 100).toString().padStart(2, '0')
    return "${hundredths / 100}.$fraction ${if (miles) "mi" else "km"}"
}
