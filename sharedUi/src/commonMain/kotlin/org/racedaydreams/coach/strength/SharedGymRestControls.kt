@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.strength.GymRestTimer

/** Existing production controls. Permissions and alert delivery stay with the platform caller. */
@Composable
fun GymRestControls(
    prefs: GymRestTimer.Preferences,
    timer: GymRestTimer?,
    prescribedRestSeconds: Int?,
    enabled: Boolean,
    notificationExplanation: String,
    onAction: (String) -> Unit,
    onPreferencesChange: (GymRestTimer.Preferences) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
) {
    var details by rememberSaveable { mutableStateOf(false) }
    SemanticCard(Tile.Progress) {
        Text(
            "Optional rest timer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        val seconds = GymRestTimer.suggestedSeconds(prescribedRestSeconds, prefs)
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
                    OutlinedButton(onClick = { onAction("pause") }, enabled = enabled) {
                        Text("Pause rest")
                    }
                if (
                    timer.phase() in
                        listOf(GymRestTimer.Phase.PAUSED, GymRestTimer.Phase.INTERRUPTED)
                )
                    OutlinedButton(onClick = { onAction("resume") }, enabled = enabled) {
                        Text("Resume rest")
                    }
                OutlinedButton(onClick = { onAction("reset") }, enabled = enabled) {
                    Text("Reset rest")
                }
                OutlinedButton(onClick = { onAction("+30") }, enabled = enabled) {
                    Text("Add 30 seconds")
                }
                OutlinedButton(onClick = { onAction("-30") }, enabled = enabled) {
                    Text("Remove 30 seconds")
                }
                TextButton(onClick = { onAction("skip") }, enabled = enabled) { Text("Skip rest") }
            }
        }
        if (
            timer == null ||
                timer.phase() in listOf(GymRestTimer.Phase.COMPLETE, GymRestTimer.Phase.SKIPPED)
        )
            Button(onClick = { onAction("start") }, enabled = enabled) { Text("Start rest timer") }
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
                            onPreferencesChange(
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
                onPreferencesChange(
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
                onNotificationsChange(value)
            }
            RestToggle(
                "Vibrate at rest completion",
                prefs.vibration(),
                enabled && prefs.notifications(),
            ) {
                onPreferencesChange(
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
                onPreferencesChange(
                    GymRestTimer.Preferences(
                        prefs.defaultSeconds(),
                        prefs.suggestAfterSet(),
                        prefs.notifications(),
                        prefs.vibration(),
                        it,
                    )
                )
            }
            Text(notificationExplanation)
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

private fun restDuration(seconds: Long): String =
    when {
        seconds == 0L -> "0 seconds"
        seconds < 60 -> "$seconds seconds"
        seconds % 60 == 0L -> "${seconds / 60} ${if (seconds == 60L) "minute" else "minutes"}"
        else -> "${seconds / 60} min ${seconds % 60} sec"
    }
