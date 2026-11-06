package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Duration
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine

/** Glanceable primary controls. All state is supplied by the service, never timed here. */
@Composable
fun ActiveRunPanel(
    s: RunMetricsEngine.Status,
    w: WorkoutExecutionEngine.State?,
    hr: Double?,
    cue: String?,
    race: String?,
    notice: String?,
    imperial: Boolean,
    send: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (s.paused()) "PAUSED"
                else
                    w?.let {
                        if (it.complete()) "Workout steps finished"
                        else "Step ${it.index()+1} of ${it.total()}"
                    } ?: "Recording",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (w != null && !w.complete()) {
                Text(
                    w.current()?.kind()?.name?.lowercase()?.replace('_', ' ') ?: "Run",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    w.metresRemaining()?.let {
                        if (imperial) "${decimal(it/1609.344)} mi remaining"
                        else "${it.toInt()} m remaining"
                    }
                        ?: w.millisRemaining()?.let { "${it/1000} sec remaining" }
                        ?: "Continue by effort",
                    style = MaterialTheme.typography.headlineLarge,
                )
            }
            GuidanceCard {
                RunMetric(
                    "Distance",
                    if (imperial) "${decimal(s.metres()/1609.344)} mi"
                    else "${decimal(s.metres()/1000)} km",
                    prominent = true,
                )
                RunMetric(
                    "Elapsed time",
                    PaceCalculator.formatTime(Duration.ofMillis(s.elapsedMillis())),
                )
                HorizontalDivider()
                RunMetric(
                    "Current pace",
                    s.secondsPerKm()
                        ?.takeIf { s.hasReliablePace() }
                        ?.let {
                            "${formatPaceSeconds(it*if(imperial)1.609344 else 1.0)} /${if(imperial)"mi"else"km"}"
                        } ?: "Pace unavailable",
                )
                if (s.metres() > 0 && s.movingMillis() > 0)
                    RunMetric(
                        "Active average pace",
                        "${formatPaceSeconds(s.movingMillis()/1000.0/(s.metres()/if(imperial)1609.344 else 1000.0))} /${if(imperial)"mi"else"km"}",
                    )
                Text(
                    "Active ${PaceCalculator.formatTime(Duration.ofMillis(s.movingMillis()))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            w?.current()?.let { step ->
                if (step.fastPace() != null && step.slowPace() != null)
                    Text(
                        "Target ${formatPaceSeconds(step.fastPace()*if(imperial)1.609344 else 1.0)}–${formatPaceSeconds(step.slowPace()*if(imperial)1.609344 else 1.0)}/${if(imperial)"mi"else"km"}"
                    )
                else
                    step.rpe()?.let {
                        Text(
                            "Aim for a perceived effort of ${decimal(it)}/10. Use how hard running feels to guide you."
                        )
                    }
            }
            w?.current()?.let { step ->
                if (step.fastPace() != null && step.slowPace() != null)
                    PaceRange(
                        step.fastPace(),
                        step.slowPace(),
                        if (s.hasReliablePace()) s.secondsPerKm() else null,
                        imperial,
                    )
            }
            hr?.let { RunMetric("Heart rate", "${it.toInt()} bpm") }
            Text(
                s.gps(),
                modifier =
                    Modifier.semantics {
                        // Ordinary quality fluctuations stay readable without repeated speech.
                        if (!s.paused() && s.gps().contains("temporarily lost"))
                            liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                    },
            )
            if (s.gps() == "GPS signal weak")
                Text("Your pace may be temporarily inaccurate. Continue by effort.")
            notice?.let { Text(it) }
            var details by remember { mutableStateOf(false) }
            TextButton(
                onClick = { details = !details },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (details) "Hide details" else "Workout details / controls")
            }
            if (details) {
                val dream by
                    org.racedaydreams.recording.RunRecordingService.dreamRoute.collectAsState()
                dream?.let { route ->
                    var show by remember { mutableStateOf(false) }
                    TextButton(onClick = { show = !show }) {
                        Text(if (show) "Hide Dream progress" else "Dream Route progress")
                    }
                    if (show) {
                        Text("Equivalent distance only · not your physical location")
                        DreamDrawing(
                            org.racedaydreams.domain.support.DreamRoutes.forId(route),
                            0.0,
                            kotlin.math.min(42195.0, s.metres()),
                        )
                    }
                }
                w?.current()?.let { Text(it.label()) }
                w?.next()?.let { Text("Next: ${it.label()}") }
                cue?.let { Text(it) }
                race?.let { Text(it) }
                if (w != null && !w.complete() && !w.early())
                    OutlinedButton(
                        onClick = {
                            send(
                                if (w.current()?.metres() == null && w.current()?.millis() == null)
                                    "COMPLETE_OPEN"
                                else "SKIP_STEP"
                            )
                        },
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) {
                        Text(
                            if (w.current()?.metres() == null && w.current()?.millis() == null)
                                "Complete open step"
                            else "Skip current step"
                        )
                    }
                Text(
                    "You can finish early. Unobserved distance and incomplete steps are never filled in."
                )
            }
        }
        var confirmFinish by rememberSaveable { mutableStateOf(false) }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2,
        ) {
            Button(
                onClick = { send(if (s.paused()) "RESUME" else "PAUSE") },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) {
                Text(if (s.paused()) "Resume" else "Pause")
            }
            OutlinedButton(
                onClick = { confirmFinish = true },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
            ) {
                Text("Finish & save")
            }
        }
        if (confirmFinish)
            AlertDialog(
                onDismissRequest = { confirmFinish = false },
                title = { Text("Finish this run?") },
                text = {
                    Text(
                        "Save the distance and time recorded so far. You can keep recording instead."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmFinish = false
                            send("FINISH")
                        }
                    ) {
                        Text("Save recorded run")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmFinish = false }) { Text("Keep recording") }
                },
            )
    }
}

@Composable
private fun RunMetric(label: String, value: String, prominent: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(
            value,
            style =
                if (prominent) MaterialTheme.typography.displaySmall
                else MaterialTheme.typography.headlineMedium,
        )
    }
}
