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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.R
import org.racedaydreams.domain.reminders.ReminderPreferences

@Composable
fun SettingsScreen(
    state: CoachUiState,
    model: CoachViewModel,
    historyAvailable: Boolean,
    connect: (Boolean, Boolean) -> Unit,
    providerUpdate: () -> Unit,
    providerSettings: () -> Unit,
    editGoals: () -> Unit,
    reviewBaseline: () -> Unit,
    shoes: () -> Unit,
    export: () -> Unit,
    restore: () -> Unit,
    reset: () -> Unit,
    weightPermissions: () -> Unit = {},
    healthContext: () -> Unit = {},
    storage: (() -> Unit)? = null,
) = CoachPage {
    CoachHeading("Settings")
    Text(
        "Everything is grouped below. Open a section to change it.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val context = LocalContext.current
    var showLicence by rememberSaveable { mutableStateOf(false) }
    var noticeAsset by rememberSaveable { mutableStateOf<String?>(null) }
    AppUpdatesSettings(card = false)

    ExpandableSection(
        "Training",
        "Primary focus, race goal, baseline, shoes and session choices",
        "settings-training",
        lastOpened = model.settingsSection,
    ) {
        SharedPrimaryFocusSettings(state) { model.settings.primaryFocus(it) }
        ActionGroup(null) {
            ActionRow("Edit race goal and availability", "Goal, race date and weekly routine") {
                editGoals()
            }
            ActionRow("Review running baseline", onClick = reviewBaseline)
            ActionRow("Shoes and mileage", onClick = shoes)
            ActionRow(
                "Personal health context",
                "Optional context for coaching",
                onClick = healthContext,
            )
        }
        SchedulePreferences()
        DreamPreferences()
    }

    ExpandableSection(
        "Health and connected data",
        "Health Connect access, activity source and weight",
        "settings-health",
        lastOpened = model.settingsSection,
    ) {
        HealthConnectionCard(state, model, historyAvailable, connect, providerUpdate)
        CoachCard {
            Text("Activity data source", style = MaterialTheme.typography.titleMedium)
            Text("Health Connect · optional. Manual entry works independently.")
            TextButton(providerSettings) { Text("Manage Health Connect permissions") }
            TextButton(
                onClick = weightPermissions,
                enabled =
                    state.healthAvailability ==
                        org.racedaydreams.health.HealthAvailability.AVAILABLE,
            ) {
                Text("Allow optional Health Connect weight")
            }
            WhyDetails(
                "Revoking access stops future reads. Existing locally imported records remain until you delete local data. Optional native-run totals are written only through your explicit Record a run → Write action. The app never deletes Health Connect records.\n\n" +
                    "Optional weight observations support your personal trend and any general protein guidance you choose to enable. Body measurements are not required for training.",
                "What access means",
            )
        }
    }

    ExpandableSection(
        "Reminders",
        "Evening-before preparation for planned runs",
        "settings-reminders",
        lastOpened = model.settingsSection,
    ) {
        ReminderSettings(state, model)
    }

    ExpandableSection(
        "Appearance and updates",
        "Colours and app updates",
        "settings-app",
        lastOpened = model.settingsSection,
    ) {
        AppearanceSettings()
        org.racedaydreams.coach.theme.DisciplineColourSettings(
            state.disciplineColours,
            state.busy,
            model.settings::colour,
        )
        AppUpdatesSettings(effects = false)
    }

    ExpandableSection(
        "Data and storage",
        "Backup, restore, recording storage and diagnostics",
        "settings-data",
        lastOpened = model.settingsSection,
    ) {
        SharedBackupSettings(state.busy, export, restore)
        storage?.let {
            OutlinedButton(it, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Storage & recording detail")
            }
        }
        DiagnosticExport(state)
        OutlinedButton(
            reset,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Delete local app data")
        }
    }

    ExpandableSection(
        "Privacy, safety and licences",
        "How your data is kept, fitness guidance and open-source notices",
        "settings-about",
        lastOpened = model.settingsSection,
    ) {
        CoachCard {
            Text("Privacy", style = MaterialTheme.typography.titleLarge)
            Text(
                "No account, advertising, subscriptions, analytics or backend. Health access is optional. Records stay in this app’s private on-device storage; automatic Android backup and device transfer are excluded. Export is an explicit choice. The only network access is the optional update check, which sends no training data."
            )
            Text(
                "The local database relies on Android’s application storage and device protection. Exported backups are not encrypted by the app; store them somewhere you trust."
            )
        }
        CoachCard {
            Text("Fitness guidance", style = MaterialTheme.typography.titleLarge)
            Text("The app recommends. The runner decides. The plan adapts.")
            Text(
                "This app provides fitness guidance, not medical care. Recommendations and estimates are not guarantees and do not promise to prevent injury."
            )
            Text(
                "If you experience chest pain, fainting, severe difficulty breathing or serious injury symptoms, stop exercise and seek appropriate medical advice."
            )
        }
        CoachCard {
            Text("About and licences", style = MaterialTheme.typography.titleLarge)
            Text("Race Day Dreams · free and open source · original code under the MIT License.")
            TextButton(onClick = { showLicence = true }) { Text("Read app licence") }
            WhyDetails("Third-party licences and route sources") {
                Text(
                    "Kotlin, kotlinx.coroutines, AndroidX Compose / Material 3, Activity, Navigation, Lifecycle and Health Connect are used under Apache License 2.0. Their source, versions and required notices are recorded in THIRD_PARTY.md."
                )
                Text(
                    "No commercial training schedules or proprietary assets are included. NHS programme structure is not implemented in this version; NHS source and reuse terms must be verified before adding it."
                )
                listOf(
                        "third_party.md" to "Third-party inventory and source notices",
                        "apache-2.0.txt" to "Apache License 2.0",
                        "kotlin-LICENSE.txt" to "Kotlin licence",
                        "coroutines-LICENSE.txt" to "Coroutines licence",
                        "protobuf-LICENSE.txt" to "Protocol Buffers licence",
                        "licences/berlin-2014-MIT.txt" to "Berlin route source licence",
                        "licences/tokyo-2026-CC0.txt" to "Tokyo route source licence",
                        "licences/london-historical-MIT.txt" to "London route source licence",
                        "licences/new-york-2026-NOTICE.txt" to "New York route source attribution",
                        "licences/paris-2026-NOTICE.txt" to
                            "Paris2026 annual route source attribution",
                        "licences/ODbL-1.0.txt" to "Open Database Licence",
                        "licences/dream-basemaps-NOTICE.txt" to "Offline city context attribution",
                        "transitive-NOTICES.txt" to "Dependency notices",
                    )
                    .forEach { (asset, label) ->
                        TextButton(onClick = { noticeAsset = asset }) { Text(label) }
                    }
            }
        }
    }
    if (showLicence)
        AlertDialog(
            onDismissRequest = { showLicence = false },
            title = { Text("MIT License") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    item { Text(context.getString(R.string.licence_mit)) }
                }
            },
            confirmButton = { TextButton(onClick = { showLicence = false }) { Text("Close") } },
        )
    noticeAsset?.let { asset ->
        val contents by
            produceState<String?>(null, asset) {
                value =
                    withContext(Dispatchers.IO) {
                        try {
                            context.assets.open(asset).bufferedReader().use { it.readText() }
                        } catch (_: Exception) {
                            "The bundled notice could not be opened. The source inventory is also included in the project repository."
                        }
                    }
            }
        AlertDialog(
            onDismissRequest = { noticeAsset = null },
            title = { Text("Open-source notices") },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    item {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(contents ?: "Loading bundled notice…")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { noticeAsset = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun ReminderSettings(state: CoachUiState, model: CoachViewModel) {
    val context = LocalContext.current
    val saved = ReminderPreferences.fromShared(state.reminderPreferences)
    var mode by rememberSaveable(saved) { mutableStateOf(saved.mode().name) }
    var time by rememberSaveable(saved) { mutableStateOf(saved.eveningTime().toString()) }
    var quiet by rememberSaveable(saved) { mutableStateOf(saved.quietHours() != null) }
    var quietStart by
        rememberSaveable(saved) {
            mutableStateOf(saved.quietHours()?.start()?.toString() ?: "22:00")
        }
    var quietEnd by
        rememberSaveable(saved) { mutableStateOf(saved.quietHours()?.end()?.toString() ?: "07:00") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionPending by rememberSaveable { mutableStateOf(false) }
    fun preferences(): ReminderPreferences =
        ReminderPreferences(
            ReminderPreferences.Mode.valueOf(mode),
            LocalTime.parse(time.trim()),
            if (quiet)
                ReminderPreferences.QuietHours(
                    LocalTime.parse(quietStart.trim()),
                    LocalTime.parse(quietEnd.trim()),
                )
            else null,
            ZoneId.systemDefault(),
        )
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (permissionPending) {
                permissionPending = false
                try {
                    val chosen = preferences()
                    if (granted) model.saveReminderPreferences(chosen)
                    else {
                        model.saveReminderPreferences(
                            ReminderPreferences(
                                ReminderPreferences.Mode.DISABLED,
                                chosen.eveningTime(),
                                chosen.quietHours(),
                                chosen.zone(),
                            )
                        )
                        error =
                            "Notification access was declined. Reminders remain disabled; every other feature is available."
                    }
                } catch (_: Exception) {
                    error = "Check the reminder time and quiet hours before trying again."
                }
            }
        }
    CoachCard {
        Text("Evening-before preparation", style = MaterialTheme.typography.titleLarge)
        Text(
            "Optional local reminders for actual scheduled runs. Moving, replacing or cancelling a workout updates the reminder; delivery checks the current workout again. Android may delay inexact alarms."
        )
        listOf(
                ReminderPreferences.Mode.DISABLED to "Disabled",
                ReminderPreferences.Mode.ALL to "Every planned run",
                ReminderPreferences.Mode.LONG_HARD_ONLY to "Long or hard workouts only",
            )
            .forEach { (value, label) ->
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
        if (state.plannedWorkouts.isEmpty())
            Text(
                "There are no scheduled workouts yet. These settings will apply when actual planned runs are available; enabling reminders does not create a training plan."
            )
        Text(
            "Preparation messages use general advice about normal food, hydration, sleep and kit. They do not prescribe fluid volumes, supplements or universal carb-loading rules."
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    val chosen = preferences()
                    error = null
                    if (
                        chosen.mode() != ReminderPreferences.Mode.DISABLED &&
                            Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionPending = true
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else model.saveReminderPreferences(chosen)
                } catch (_: Exception) {
                    error =
                        "Enter valid times in HH:MM format. Quiet hours must start and end at different times."
                }
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save reminder choices")
        }
        TextButton(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }
        ) {
            Text("Open Android notification settings")
        }
    }
}

/** Uses a local display preference; changes neither athlete data nor portable backup formats. */
@Composable
private fun AppearanceSettings() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(org.racedaydreams.coach.theme.APPEARANCE_PREFERENCES, 0)
    }
    var enabled by remember {
        mutableStateOf(prefs.getBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, true))
    }
    CoachCard {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Text("Follows your device's light or dark theme.")
        if (Build.VERSION.SDK_INT >= 31) {
            CheckRow("Use device colours", enabled) {
                enabled = it
                prefs.edit().putBoolean(org.racedaydreams.coach.theme.DYNAMIC_COLOUR, it).apply()
            }
            Text(
                "Turn off to use Race Day Dreams blue. This display choice stays on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else Text("Race Day Dreams blue · device colours are available on Android 12 and later.")
    }
}
