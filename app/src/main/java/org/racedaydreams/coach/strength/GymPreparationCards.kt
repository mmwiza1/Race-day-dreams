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
import androidx.compose.ui.platform.LocalContext
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
    val suggestion =
        remember(slot, state.recent, state.view?.observed()?.actualSets()) {
            model.warmupSuggestion()
        }
    GymWarmupGuidanceCard(
        slotId = slot.id(),
        exerciseId = slot.exercise().id(),
        suggestion = suggestion,
        enabled = enabled,
        formatKilograms = { String.format(Locale.UK, "%.1f", it) },
        onPrepare = model::prepareWarmup,
    )
}
