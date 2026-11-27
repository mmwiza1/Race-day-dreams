package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.recording.RunMetricsEngine

/**
 * Glanceable primary controls. All state and platform content are supplied by the authoritative
 * recorder, never timed here.
 */
@Composable
fun SharedActiveRunPanel(
    s: RunMetricsEngine.Status,
    w: WorkoutExecutionEngine.State?,
    hr: Double?,
    cue: String?,
    race: String?,
    notice: String?,
    imperial: Boolean,
    send: (String) -> Unit,
    formatTime: (Long) -> String,
    formatPace: (Double) -> String,
    formatDecimal: (Double) -> String,
    formatDistance: (Double) -> String,
    routeContent: @Composable () -> Unit,
    dreamContent: @Composable () -> Unit,
    diagnostics: (@Composable () -> Unit)? = null,
) {
    val gps = s.gps() ?: "GPS status unavailable"
    var routeView by rememberSaveable { mutableStateOf(false) }
    val largeText = LocalDensity.current.fontScale >= 1.5f
    Column(
        Modifier.fillMaxSize()
            .testTag("live-running-panel")
            .semantics { paneTitle = "Live run" }
            .padding(horizontal = CoachSpace.Inset, vertical = CoachSpace.Related),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Live run",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color =
                        if (s.paused()) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                    contentColor =
                        if (s.paused()) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        if (s.paused()) "PAUSED" else "Recording",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            val pace =
                s.secondsPerKm()
                    ?.takeIf { s.hasReliablePace() }
                    ?.let {
                        "${formatPace(it*if(imperial)1.609344 else 1.0)} /${if(imperial)"mi"else"km"}"
                    } ?: "Pace unavailable"
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    gps,
                    style = MaterialTheme.typography.labelLarge,
                    modifier =
                        Modifier.semantics {
                            if (!s.paused() && gps.contains("temporarily lost"))
                                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                        },
                )
                if (gps == "GPS signal weak")
                    Text(
                        "Your pace may be temporarily inaccurate. Continue by effort.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                notice?.let {
                    Text(
                        it,
                        modifier =
                            Modifier.semantics {
                                liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                            },
                    )
                }
            }
            if (!routeView) LivePrimaryMetrics(s, pace, largeText, formatTime, formatDistance)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (largeText && maxWidth < 360.dp) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(false to "Statistics", true to "Route").forEach { (route, label) ->
                            CoachFilterChip(
                                selected = routeView == route,
                                onClick = { routeView = route },
                                label = { Text(label) },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag(
                                            if (route) "live-route-tab" else "live-statistics-tab"
                                        ),
                            )
                        }
                    }
                } else {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !routeView,
                            onClick = { routeView = false },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier =
                                Modifier.weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("live-statistics-tab"),
                        ) {
                            Text("Statistics")
                        }
                        SegmentedButton(
                            selected = routeView,
                            onClick = { routeView = true },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier =
                                Modifier.weight(1f).heightIn(min = 48.dp).testTag("live-route-tab"),
                        ) {
                            Text("Route")
                        }
                    }
                }
            }
            if (routeView) {
                routeContent()
                LivePrimaryMetrics(s, pace, largeText, formatTime, formatDistance)
            }
            if (w?.complete() == true)
                Text("Workout steps finished", style = MaterialTheme.typography.titleMedium)
            else if (w != null) LiveWorkoutProgress(w, imperial, formatDecimal)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
                maxItemsInEachRow = if (largeText) 1 else 2,
            ) {
                LiveMetricCard("Active time", formatTime(s.movingMillis()), Modifier.weight(1f))
                if (s.metres() > 0 && s.movingMillis() > 0)
                    LiveMetricCard(
                        "Active average pace",
                        "${formatPace(s.movingMillis()/1000.0/(s.metres()/if(imperial)1609.344 else 1000.0))} /${if(imperial)"mi"else"km"}",
                        Modifier.weight(1f),
                    )
                hr?.let { LiveMetricCard("Heart rate", "${it.toInt()} bpm", Modifier.weight(1f)) }
            }
            w?.current()?.let { step ->
                val fast = step.fastPace()
                val slow = step.slowPace()
                if (fast != null && slow != null)
                    Text(
                        "Target ${formatPace(fast*if(imperial)1.609344 else 1.0)}–${formatPace(slow*if(imperial)1.609344 else 1.0)}/${if(imperial)"mi"else"km"}"
                    )
                else
                    step.rpe()?.let {
                        Text(
                            "Aim for a ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.effortDescription(it)} effort. Use how hard running feels to guide you."
                        )
                    }
            }
            w?.current()?.let { step ->
                val fast = step.fastPace()
                val slow = step.slowPace()
                if (fast != null && slow != null)
                    SharedPaceRange(
                        fast,
                        slow,
                        if (s.hasReliablePace()) s.secondsPerKm() else null,
                        imperial,
                        formatPace,
                    )
            }
            ExpandableSection(
                "Workout details / controls",
                "Workout steps, pace and coaching",
                "live-workout-details",
            ) {
                Text(
                    "Current pace reflects recent reliable GPS movement. Average pace summarises the run using the timing shown. Weak GPS can make current pace unavailable; no missing movement is invented."
                )
                w?.current()?.rpe()?.let {
                    Text(
                        "RPE (rating of perceived effort): ${formatDecimal(it)}/10. This is how hard the planned section should feel."
                    )
                }
                dreamContent()
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
            ExpandableSection(
                "GPS troubleshooting",
                "Review recording and signal information",
                "live-gps-troubleshooting",
            ) {
                Text("If the signal drops, keep recording and use your effort to guide your pace.")
                if (diagnostics != null) diagnostics()
                else Text("Find recording reports in Settings under Data and storage.")
            }
        }
        var confirmFinish by rememberSaveable { mutableStateOf(false) }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = if (largeText) 1 else 2,
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
                text = { Text("Save the distance and time recorded so far.") },
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
private fun LivePrimaryMetrics(
    status: RunMetricsEngine.Status,
    pace: String,
    largeText: Boolean,
    formatTime: (Long) -> String,
    formatDistance: (Double) -> String,
) {
    LiveDistance(formatDistance(status.metres()))
    val elapsed = formatTime(status.elapsedMillis())
    if (largeText) {
        // Give each metric the whole row, including long elapsed values at 200% text.
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Related),
        ) {
            LiveMetricCard("Elapsed time", elapsed, Modifier.fillMaxWidth().testTag("live-elapsed"))
            LiveMetricCard("Current pace", pace, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
        ) {
            LiveMetricCard("Elapsed time", elapsed, Modifier.weight(1f).testTag("live-elapsed"))
            LiveMetricCard("Current pace", pace, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LiveDistance(distance: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(CoachSpace.Inset).semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Distance", style = MaterialTheme.typography.labelLarge)
            Text(
                distance,
                modifier = Modifier.fillMaxWidth(),
                style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
            )
        }
    }
}

@Composable
private fun LiveWorkoutProgress(
    w: WorkoutExecutionEngine.State,
    imperial: Boolean,
    formatDecimal: (Double) -> String,
) {
    val step = w.current() ?: return
    val metres = step.metres()
    val metresRemaining = w.metresRemaining()
    val millis = step.millis()
    val millisRemaining = w.millisRemaining()
    val progress =
        when {
            metres != null && metresRemaining != null -> 1.0 - metresRemaining / metres
            millis != null && millisRemaining != null -> 1.0 - millisRemaining.toDouble() / millis
            else -> null
        }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().testTag("live-workout-progress"),
    ) {
        Column(
            Modifier.padding(CoachSpace.Inset),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Step ${w.index()+1} of ${w.total()} · ${step.kind().name.lowercase().replace('_', ' ')}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                w.metresRemaining()?.let {
                    if (imperial) "${formatDecimal(it/1609.344)} mi remaining"
                    else "${it.toInt()} m remaining"
                }
                    ?: w.millisRemaining()?.let { "${it/1000} sec remaining" }
                    ?: "Continue by effort",
                style = MaterialTheme.typography.titleLarge,
            )
            progress?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .12f),
                )
            }
        }
    }
}

@Composable
private fun LiveMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(CoachSpace.Related).semantics(
                mergeDescendants = true
            ) {},
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                modifier = Modifier.fillMaxWidth(),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum",
                    ),
            )
        }
    }
}
