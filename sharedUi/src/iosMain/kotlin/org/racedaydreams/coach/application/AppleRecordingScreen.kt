package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.recording.PlannedRecordingExecution

/** Native command bindings for the existing active run panel and retained run recovery. */
@Composable
fun AppleRecordingScreen(
    session: AppleCoachSession<*>,
    plannedId: String? = null,
    raceVersion: String? = null,
    hilly: Boolean = false,
    openSaved: (String) -> Unit,
) {
    val recorder = session.recording
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val status by recorder.status.collectAsState()
    val monitors by recorder.monitors.collectAsState()
    val heartRate by recorder.heartRate.collectAsState()
    var monitorId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(recorder) { onDispose { recorder.stopHeartRateScan() } }
    val cue by recorder.cue.collectAsState()
    val raceContext by recorder.raceContext.collectAsState()
    val workout by recorder.workout.collectAsState()
    val authorized by recorder.permission.collectAsState()
    val retained by recorder.recovery.collectAsState()
    val storageError by recorder.error.collectAsState()
    val scope = rememberCoroutineScope()
    var spokenCues by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }

    fun command(action: suspend () -> Unit) {
        if (working) return
        working = true
        error = null
        scope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Could not complete the recording action."
            } finally {
                working = false
            }
        }
    }

    fun saved(id: String) {
        session.services.operations.refresh()
        openSaved(id)
    }

    val live = status
    val recovery = retained
    if (live != null) {
        SharedActiveRunPanel(
            s = live,
            w = workout,
            hr = heartRate,
            cue = cue,
            race = raceContext,
            notice = error ?: storageError,
            imperial = imperial,
            send = { action ->
                when (action) {
                    "PAUSE" -> command { recorder.pause() }
                    "RESUME" -> command { recorder.resume() }
                    "FINISH" -> command { saved(recorder.finish()) }
                    "COMPLETE_OPEN" -> command { recorder.changeWorkoutStep(skip = false) }
                    "SKIP_STEP" -> command { recorder.changeWorkoutStep(skip = true) }
                    else -> Unit
                }
            },
            formatTime = ::recordingTime,
            formatPace = { recordingTime((it * 1_000).toLong()) },
            formatDecimal = ::recordingDecimal,
            formatDistance = {
                if (imperial) "${recordingDecimal(it / 1_609.344)} mi"
                else "${recordingDecimal(it / 1_000)} km"
            },
            routeContent = {
                var route by remember { mutableStateOf<PreparedLiveRoute?>(null) }
                // The route tab alone prepares geometry. Copy on the recorder owner thread.
                LaunchedEffect(live.elapsedMillis() / 2_000) {
                    val points = recorder.routePoints()
                    route =
                        withContext(applicationIoDispatcher()) {
                            PreparedLiveRoute.fromPoints(points)
                        }
                }
                SharedLiveRecordedRoute(route, unavailable = false)
            },
            dreamContent = {},
        )
        return
    }

    CoachPage {
        CoachHeading("Record a run")
        if (recovery != null) {
            CoachCard {
                Text("Interrupted recording")
                Text("Retained elapsed time: ${recordingTime(recovery.elapsedMillis)}")
                Text("Active time: ${recordingTime(recovery.activeMillis)}")
                Text(
                    "${recovery.pointCount} saved GPS observations. Time after the last save is unavailable."
                )
                if (!working && recovery.pointCount >= 2) {
                    CoachButton("Save retained run") {
                        command { saved(recorder.saveInterrupted(recovery.session)) }
                    }
                }
                TextButton(onClick = { confirmDiscard = true }, enabled = !working) {
                    Text("Discard interrupted recording")
                }
            }
            if (confirmDiscard) {
                AlertDialog(
                    onDismissRequest = { confirmDiscard = false },
                    title = { Text("Discard interrupted recording?") },
                    text = { Text("The saved details of this unfinished run will be removed.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                confirmDiscard = false
                                command {
                                    if (!recorder.discardInterrupted(recovery.session)) {
                                        error =
                                            "The interrupted recording changed. Review it again."
                                    }
                                }
                            }
                        ) {
                            Text("Discard")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDiscard = false }) {
                            Text("Keep recording details")
                        }
                    },
                )
            }
        } else if (!authorized) {
            Text("Allow location access to record your outdoor route.")
            CoachButton("Allow location access") { recorder.requestLocationPermission() }
        } else if (!working) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = spokenCues, onCheckedChange = { spokenCues = it })
                Text("Speak coaching cues")
            }
            TextButton(
                onClick = {
                    monitorId = null
                    recorder.scanHeartRateMonitors()
                }
            ) {
                Text("Find a Bluetooth heart-rate monitor")
            }
            if (monitorId != null) {
                TextButton(onClick = { monitorId = null }) { Text("Record without a monitor") }
            }
            monitors.forEach { (id, name) ->
                TextButton(
                    onClick = {
                        monitorId = id
                        recorder.stopHeartRateScan()
                    }
                ) {
                    Text(if (monitorId == id) "$name · selected" else name)
                }
            }
            CoachButton(if (plannedId == null) "Start recording" else "Start planned workout") {
                command {
                    val planned =
                        plannedId?.let {
                            val version = requireNotNull(state.currentPlan) { "No current plan." }
                            require(raceVersion == null || raceVersion == version.id()) {
                                "The plan changed. Open Race Day again."
                            }
                            PlannedRecordingExecution(version, it, hilly)
                        }
                    recorder.start(planned, imperial, spokenCues, monitorId, hilly)
                }
            }
        }
        error?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}

private fun recordingTime(millis: Long): String =
    PaceCalculator.formatTime(Durations.ofMillis(millis))

private fun recordingDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}
