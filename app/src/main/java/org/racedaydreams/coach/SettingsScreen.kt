package org.racedaydreams.coach

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.ZoneId
import org.racedaydreams.R
import org.racedaydreams.domain.reminders.ReminderPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(state: CoachUiState, model: CoachViewModel, historyAvailable: Boolean,
                   connect: (Boolean, Boolean) -> Unit, providerUpdate: () -> Unit, providerSettings: () -> Unit,
                   editGoals: () -> Unit, reviewBaseline: () -> Unit, shoes: () -> Unit,
                   export: () -> Unit, restore: () -> Unit, reset: () -> Unit, weightPermissions: () -> Unit = {}, healthContext: () -> Unit = {}) = CoachPage {
    SchedulePreferences()
    DreamPreferences()
    DiagnosticExport(state)
    val context = LocalContext.current
    var showLicence by rememberSaveable { mutableStateOf(false) }
    var noticeAsset by rememberSaveable { mutableStateOf<String?>(null) }
    CoachHeading("Settings")
    CoachButton("Personal health context",healthContext)
    CoachButton("Edit race goal and availability", editGoals)
    OutlinedButton(reviewBaseline, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Review running baseline") }
    OutlinedButton(shoes, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Shoes and mileage") }
    HealthConnectionCard(state, model, historyAvailable, connect, providerUpdate)
    TextButton(providerSettings) { Text("Manage Health Connect permissions") }
    Text("Revoking access stops future reads. Existing locally imported records remain until you delete local data. Optional native-run totals are written only through your explicit Record a run → Write action. The app never deletes Health Connect records.")
    CoachCard {
        Text("Activity data source",style=MaterialTheme.typography.titleMedium)
        Text("Health Connect")
        Text("Optional health access. Manual entry works independently.")
        TextButton(onClick=weightPermissions,enabled=state.healthAvailability==org.racedaydreams.health.HealthAvailability.AVAILABLE){Text("Allow optional Health Connect weight")}
        Text("Weight is used only for your personal trend. No weight-based coaching judgement.")
    }
    ReminderSettings(state, model)
    CoachCard {
        Text("Local backup and restore", style = MaterialTheme.typography.titleLarge)
        Text("A backup preserves your app-specific goals, manual entries, shoes, notes and results in an inspectable JSON file. Also use Record a run → Back up raw recordings to preserve GPS coordinates, sensor observations and unfinished runs in a checksummed archive. Raw Health Connect records are omitted; they can be imported again. Your chosen file provider controls where the file is stored.")
        Button(export, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Export local backup") }
        OutlinedButton(restore, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Choose backup to restore") }
        Text("Restore validates the file before replacing local app data. Merge restore is not currently supported.")
    }
    CoachCard {
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        Text("No account, advertising, subscriptions, analytics or backend. Health access is optional. Records stay in this app’s private on-device storage; automatic Android backup and device transfer are excluded. Export is an explicit choice.")
        Text("The local database relies on Android’s application storage and device protection. Exported backups are not encrypted by the app; store them somewhere you trust.")
    }
    CoachCard {
        Text("Fitness guidance", style = MaterialTheme.typography.titleLarge)
        Text("The app recommends. The runner decides. The plan adapts.")
        Text("This app provides fitness guidance, not medical care. Recommendations and estimates are not guarantees and do not promise to prevent injury.")
        Text("If you experience chest pain, fainting, severe difficulty breathing or serious injury symptoms, stop exercise and seek appropriate medical advice.")
    }
    CoachCard {
        Text("About and licences", style = MaterialTheme.typography.titleLarge)
        Text("Race Day Dreams · free and open source · original code under the MIT License.")
        Text("Kotlin, kotlinx.coroutines, AndroidX Compose / Material 3, Activity, Navigation, Lifecycle and Health Connect are used under Apache License 2.0. Their source, versions and required notices are recorded in THIRD_PARTY.md.")
        Text("No commercial training schedules or proprietary assets are included. NHS programme structure is not implemented in this version; NHS source and reuse terms must be verified before adding it.")
        TextButton(onClick = { showLicence = true }) { Text("Read app licence") }
        TextButton(onClick = { noticeAsset = "third_party.md" }) { Text("Third-party inventory and source notices") }
        TextButton(onClick = { noticeAsset = "apache-2.0.txt" }) { Text("Apache License 2.0") }
        TextButton(onClick = { noticeAsset = "kotlin-LICENSE.txt" }) { Text("Kotlin licence") }
        TextButton(onClick = { noticeAsset = "coroutines-LICENSE.txt" }) { Text("Coroutines licence") }
        TextButton(onClick = { noticeAsset = "protobuf-LICENSE.txt" }) { Text("Protocol Buffers licence") }
        TextButton(onClick = { noticeAsset = "licences/berlin-2014-MIT.txt" }) { Text("Berlin route source licence") }
        TextButton(onClick = { noticeAsset = "licences/tokyo-2026-CC0.txt" }) { Text("Tokyo route source licence") }
        TextButton(onClick = { noticeAsset = "transitive-NOTICES.txt" }) { Text("Dependency notices") }
    }
    OutlinedButton(reset, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Delete local app data") }
    if (showLicence) AlertDialog(onDismissRequest = { showLicence = false }, title = { Text("MIT License") },
        text = { androidx.compose.foundation.lazy.LazyColumn { item { Text(context.getString(R.string.licence_mit)) } } },
        confirmButton = { TextButton(onClick = { showLicence = false }) { Text("Close") } })
    noticeAsset?.let { asset ->
        val contents by produceState<String?>(null, asset) {
            value = withContext(Dispatchers.IO) {
                try { context.assets.open(asset).bufferedReader().use { it.readText() } }
                catch (_: Exception) { "The bundled notice could not be opened. The source inventory is also included in the project repository." }
            }
        }
        AlertDialog(onDismissRequest = { noticeAsset = null }, title = { Text("Open-source notices") },
            text = { androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item { androidx.compose.foundation.text.selection.SelectionContainer { Text(contents ?: "Loading bundled notice…") } }
            } },
            confirmButton = { TextButton(onClick = { noticeAsset = null }) { Text("Close") } })
    }
}

@Composable
private fun ReminderSettings(state: CoachUiState, model: CoachViewModel) {
    val context = LocalContext.current
    val saved = state.reminderPreferences
    var mode by rememberSaveable(saved) { mutableStateOf(saved.mode().name) }
    var time by rememberSaveable(saved) { mutableStateOf(saved.eveningTime().toString()) }
    var quiet by rememberSaveable(saved) { mutableStateOf(saved.quietHours() != null) }
    var quietStart by rememberSaveable(saved) { mutableStateOf(saved.quietHours()?.start()?.toString() ?: "22:00") }
    var quietEnd by rememberSaveable(saved) { mutableStateOf(saved.quietHours()?.end()?.toString() ?: "07:00") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionPending by rememberSaveable { mutableStateOf(false) }
    fun preferences(): ReminderPreferences = ReminderPreferences(ReminderPreferences.Mode.valueOf(mode),
        LocalTime.parse(time.trim()), if (quiet) ReminderPreferences.QuietHours(LocalTime.parse(quietStart.trim()), LocalTime.parse(quietEnd.trim())) else null,
        ZoneId.systemDefault())
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (permissionPending) {
            permissionPending = false
            try {
                val chosen = preferences()
                if (granted) model.saveReminderPreferences(chosen)
                else {
                    model.saveReminderPreferences(ReminderPreferences(ReminderPreferences.Mode.DISABLED, chosen.eveningTime(), chosen.quietHours(), chosen.zone()))
                    error = "Notification access was declined. Reminders remain disabled; every other feature is available."
                }
            } catch (_: Exception) { error = "Check the reminder time and quiet hours before trying again." }
        }
    }
    CoachCard {
        Text("Evening-before preparation", style = MaterialTheme.typography.titleLarge)
        Text("Optional local reminders for actual scheduled runs. Moving, replacing or cancelling a workout updates the reminder; delivery checks the current workout again. Android may delay inexact alarms.")
        listOf(ReminderPreferences.Mode.DISABLED to "Disabled", ReminderPreferences.Mode.ALL to "Every planned run",
            ReminderPreferences.Mode.LONG_HARD_ONLY to "Long or hard workouts only").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                RadioButton(selected = mode == value.name, onClick = { mode = value.name })
                TextButton(onClick = { mode = value.name }) { Text(label) }
            }
        }
        CoachField("Evening time (HH:MM, device local time)", time, { time = it })
        CheckRow("Respect quiet hours", quiet) { quiet = it }
        if (quiet) {
            CoachField("Quiet hours begin (HH:MM)", quietStart, { quietStart = it })
            CoachField("Quiet hours end (HH:MM)", quietEnd, { quietEnd = it })
        }
        if (state.plannedWorkouts.isEmpty()) Text("There are no scheduled workouts yet. These settings will apply when actual planned runs are available; enabling reminders does not create a training plan.")
        Text("Preparation messages use general advice about normal food, hydration, sleep and kit. They do not prescribe fluid volumes, supplements or universal carb-loading rules.")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try {
                val chosen = preferences()
                error = null
                if (chosen.mode() != ReminderPreferences.Mode.DISABLED && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionPending = true
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else model.saveReminderPreferences(chosen)
            } catch (_: Exception) { error = "Enter valid times in HH:MM format. Quiet hours must start and end at different times." }
        }, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save reminder choices") }
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }) { Text("Open Android notification settings") }
    }
}
