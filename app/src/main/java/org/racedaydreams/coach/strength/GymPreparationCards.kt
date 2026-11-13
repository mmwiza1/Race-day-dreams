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
    GymRestControls(
        prefs = prefs,
        timer = timer,
        prescribedRestSeconds = slot.target()?.restSeconds(),
        enabled = enabled,
        notificationExplanation =
            "Remaining time uses the device's monotonic clock. Android may delay notifications while locked or saving battery. Rest never blocks your next set. Alerts stay quiet during run recording; Android notification and audio settings also apply.",
        onAction = model::restAction,
        onPreferencesChange = model::restPreferences,
        onNotificationsChange = { value ->
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
        },
    )
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
