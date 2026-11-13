@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.*

@Composable
internal fun GymRestCard(
    state: GymNowState,
    model: GymNowViewModel,
    slot: GymWorkoutDefinition.Slot,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val prefs = state.restPreferences
    val timer = state.restTimer
    var details by rememberSaveable { mutableStateOf(false) }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            model.restPreferences(
                model.state.value.restPreferences.let {
                    GymRestTimer.Preferences(
                        it.defaultSeconds(),
                        it.suggestAfterSet(),
                        granted,
                        it.vibration(),
                        it.audio(),
                    )
                }
            )
        }
    SemanticCard(Tile.Progress) {
        Text(
            "Optional rest timer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val seconds = GymRestTimer.suggestedSeconds(slot, prefs)
        if (prefs.suggestAfterSet())
            Text(
                if (seconds > 0)
                    "Rest for ${restDuration(seconds.toLong())} before your next set, or continue when ready."
                else "No timed rest is prescribed. Continue when ready."
            )
        if (timer != null) {
            Text(
                when (timer.phase()) {
                    GymRestTimer.Phase.RUNNING ->
                        "${restDuration(timer.secondsRemaining())} remaining"
                    GymRestTimer.Phase.PAUSED ->
                        "Paused · ${restDuration(timer.secondsRemaining())} remaining"
                    GymRestTimer.Phase.COMPLETE -> "Rest time complete — continue when ready"
                    GymRestTimer.Phase.SKIPPED -> "Rest skipped"
                    GymRestTimer.Phase.INTERRUPTED ->
                        "Timer interrupted. Reset it or explicitly resume the retained remaining time."
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (timer.phase() == GymRestTimer.Phase.RUNNING)
                    OutlinedButton(onClick = { model.restAction("pause") }, enabled = enabled) {
                        Text("Pause rest")
                    }
                if (
                    timer.phase() in
                        listOf(GymRestTimer.Phase.PAUSED, GymRestTimer.Phase.INTERRUPTED)
                )
                    OutlinedButton(onClick = { model.restAction("resume") }, enabled = enabled) {
                        Text("Resume rest")
                    }
                OutlinedButton(onClick = { model.restAction("reset") }, enabled = enabled) {
                    Text("Reset rest")
                }
                OutlinedButton(onClick = { model.restAction("+30") }, enabled = enabled) {
                    Text("Add 30 seconds")
                }
                OutlinedButton(onClick = { model.restAction("-30") }, enabled = enabled) {
                    Text("Remove 30 seconds")
                }
                TextButton(onClick = { model.restAction("skip") }, enabled = enabled) {
                    Text("Skip rest")
                }
            }
        }
        if (
            timer == null ||
                timer.phase() in listOf(GymRestTimer.Phase.COMPLETE, GymRestTimer.Phase.SKIPPED)
        )
            Button(onClick = { model.restAction("start") }, enabled = enabled) {
                Text("Start rest timer")
            }
        TextButton(
            onClick = { details = !details },
            modifier =
                Modifier.heightIn(min = 48.dp).semantics {
                    stateDescription = if (details) "Expanded" else "Collapsed"
                },
        ) {
            Text(if (details) "Timer settings ▴" else "Timer settings ▾")
        }
        if (details) {
            Text("Default rest when no target is prescribed")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(0, 60, 90, 120, 180, 300).forEach { value ->
                    CoachFilterChip(
                        selected = prefs.defaultSeconds() == value,
                        enabled = enabled,
                        onClick = {
                            model.restPreferences(
                                GymRestTimer.Preferences(
                                    value,
                                    prefs.suggestAfterSet(),
                                    prefs.notifications(),
                                    prefs.vibration(),
                                    prefs.audio(),
                                )
                            )
                        },
                        label = { Text(restDuration(value.toLong())) },
                    )
                }
            }
            RestToggle("Show rest suggestions", prefs.suggestAfterSet(), enabled) {
                model.restPreferences(
                    GymRestTimer.Preferences(
                        prefs.defaultSeconds(),
                        it,
                        prefs.notifications(),
                        prefs.vibration(),
                        prefs.audio(),
                    )
                )
            }
            RestToggle("Optional completion notifications", prefs.notifications(), enabled) { value
                ->
                if (
                    value &&
                        Build.VERSION.SDK_INT >= 33 &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                )
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else
                    model.restPreferences(
                        GymRestTimer.Preferences(
                            prefs.defaultSeconds(),
                            prefs.suggestAfterSet(),
                            value,
                            prefs.vibration(),
                            prefs.audio(),
                        )
                    )
            }
            RestToggle(
                "Vibrate at rest completion",
                prefs.vibration(),
                enabled && prefs.notifications(),
            ) {
                model.restPreferences(
                    GymRestTimer.Preferences(
                        prefs.defaultSeconds(),
                        prefs.suggestAfterSet(),
                        prefs.notifications(),
                        it,
                        prefs.audio(),
                    )
                )
            }
            RestToggle("Play a completion sound", prefs.audio(), enabled && prefs.notifications()) {
                model.restPreferences(
                    GymRestTimer.Preferences(
                        prefs.defaultSeconds(),
                        prefs.suggestAfterSet(),
                        prefs.notifications(),
                        prefs.vibration(),
                        it,
                    )
                )
            }
            Text(
                "Remaining time uses the device's monotonic clock. Android may delay notifications while locked or saving battery. Rest never blocks your next set. Alerts stay quiet during run recording; Android notification and audio settings also apply."
            )
        }
    }
}

@Composable
private fun RestToggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    change: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Checkbox(
            checked = checked,
            onCheckedChange = change,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label },
        )
        Text(label, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
    }
}

internal fun restDuration(seconds: Long): String =
    when {
        seconds == 0L -> "0 seconds"
        seconds < 60 -> "$seconds seconds"
        seconds % 60 == 0L -> "${seconds / 60} ${if (seconds == 60L) "minute" else "minutes"}"
        else -> "${seconds / 60} min ${seconds % 60} sec"
    }

@Composable
internal fun GymWarmupCard(
    state: GymNowState,
    model: GymNowViewModel,
    slot: GymWorkoutDefinition.Slot,
    enabled: Boolean,
) {
    var open by rememberSaveable(slot.id(), slot.exercise().id()) { mutableStateOf(false) }
    var evidence by rememberSaveable(slot.id(), slot.exercise().id()) { mutableStateOf(false) }
    val suggestion =
        remember(slot, state.recent, state.view?.observed()?.actualSets()) {
            model.warmupSuggestion()
        }
    SemanticCard(Tile.Insight) {
        TextButton(
            onClick = { open = !open },
            modifier =
                Modifier.heightIn(min = 48.dp).semantics {
                    stateDescription = if (open) "Expanded" else "Collapsed"
                },
        ) {
            Text(if (open) "Optional warm-up ▴" else "Optional warm-up ▾")
        }
        if (open && suggestion != null) {
            Text(
                "Warm-up sets are optional practice before your main sets. Use a comfortable movement and record only what you actually do."
            )
            Text(suggestion.instruction())
            Text("These are practice ideas, not recorded sets. Review and edit them before saving.")
            suggestion.ramps().forEach { ramp ->
                OutlinedButton(onClick = { model.prepareWarmup(ramp) }, enabled = enabled) {
                    Text(
                        "Review ${String.format(Locale.UK,"%.1f",ramp.kilograms())} kg × ${ramp.repetitions()} practice repetitions"
                    )
                }
            }
            TextButton(onClick = { model.prepareWarmup(null) }, enabled = enabled) {
                Text("Enter my own warm-up")
            }
            TextButton(
                onClick = { evidence = !evidence },
                modifier =
                    Modifier.heightIn(min = 48.dp).semantics {
                        stateDescription = if (evidence) "Expanded" else "Collapsed"
                    },
            ) {
                Text(if (evidence) "Hide preparation evidence" else "Why this preparation?")
            }
            if (evidence) {
                Text(suggestion.why(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Warm-ups do not count as working sets, PBs or progression evidence. Suggested loads are not completed lifts or working-weight prescriptions."
                )
            }
            TextButton(onClick = { open = false }) { Text("Skip preparation guidance") }
        }
    }
}
