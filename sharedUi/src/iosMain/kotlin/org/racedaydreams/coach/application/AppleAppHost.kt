package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import org.racedaydreams.coach.*
import org.racedaydreams.coach.theme.SharedRaceDayTheme
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.health.body.BodyMetric
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.recording.AppleRecordingClock
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController

/** Swift owns the view controller. Compose owns the app session across screen changes. */
fun raceDayViewController(databasePath: String, bundleIdentifier: String): UIViewController {
    val preferences = NSUserDefaults.standardUserDefaults
    val workoutKey = "healthkit.workouts"
    val weightKey = "healthkit.weight"
    val heightKey = "healthkit.height"
    val metrics = {
        buildSet {
            if (preferences.boolForKey(weightKey)) add(BodyMetric.WEIGHT_KILOGRAMS)
            if (preferences.boolForKey(heightKey)) add(BodyMetric.HEIGHT_METRES)
        }
    }
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        SharedRaceDayTheme {
            var session by remember { mutableStateOf<AppleCoachSession<Unit>?>(null) }
            var startupError by remember { mutableStateOf<String?>(null) }
            var attempt by remember { mutableIntStateOf(0) }
            LaunchedEffect(attempt) {
                try {
                    session =
                        AppleCoachSession.open(
                            path = databasePath,
                            bundleIdentifier = bundleIdentifier,
                            readWindow = {
                                val now = AppleRecordingClock.now()
                                BodyReadWindow(
                                    Instant.fromEpochSeconds(
                                        now.getEpochSecond() - 84 * 86_400L,
                                        now.getNano(),
                                    ),
                                    Instant.fromEpochSeconds(now.getEpochSecond(), now.getNano()),
                                )
                            },
                            selectedWorkouts = { preferences.boolForKey(workoutKey) },
                            selectedBodyMetrics = metrics,
                            savedDraft = { null },
                            documents = {
                                object : SettingsPlatform<Unit> {
                                    override val restoredMessage = ""

                                    override fun exportBackup(file: Unit) =
                                        error("Use the system backup picker.")

                                    override fun restoreBackup(file: Unit) =
                                        error("Restore is unavailable.")

                                    override fun reset() = error("Reset is unavailable.")

                                    override fun saveReminderPreferences(
                                        preferences: ReminderSettings
                                    ) = error("Use the app reminder settings.")
                                }
                            },
                            shortDate = {
                                "${it.getDayOfMonth().toString().padStart(2, '0')}/${it.getMonthValue().toString().padStart(2, '0')}"
                            },
                        )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    startupError = failure.message ?: "Could not open local training data."
                }
            }
            val active = session
            Surface(Modifier.fillMaxSize()) {
                if (active != null) {
                    AppleAppContent(active, controller) {
                        AppleHealthControls(
                            active,
                            preferences,
                            workoutKey,
                            weightKey,
                            heightKey,
                            metrics,
                        )
                    }
                } else {
                    CoachPage {
                        CoachHeading("Race Day Dreams")
                        Text(startupError ?: "Opening local training data…")
                        if (startupError != null)
                            CoachButton("Try again") {
                                startupError = null
                                attempt++
                            }
                    }
                }
            }
        }
    }
    return controller
}

private enum class ApplePage {
    START,
    RECORD,
    SETTINGS,
}

@Composable
private fun AppleAppContent(
    session: AppleCoachSession<Unit>,
    controller: UIViewController,
    health: @Composable () -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    var page by remember { mutableStateOf(ApplePage.START) }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            TextButton(onClick = { page = ApplePage.START }) { Text("Today") }
            TextButton(onClick = { page = ApplePage.RECORD }) { Text("Record") }
            TextButton(onClick = { page = ApplePage.SETTINGS }) { Text("Settings") }
        }
        Box(Modifier.weight(1f)) {
            when (page) {
                ApplePage.SETTINGS -> AppleSettingsScreen(session, controller, health)
                ApplePage.RECORD ->
                    AppleRecordingScreen(session) {
                        saved = true
                        page = ApplePage.START
                    }
                ApplePage.START ->
                    if (!state.busy && state.primaryFocus == null) {
                        SharedStartingPath(
                            record = {
                                session.services.settings.startingPath("record") {
                                    page = ApplePage.RECORD
                                }
                            },
                            race = { session.services.settings.startingPath("race") },
                            strength = {
                                session.services.settings.primaryFocus(PrimaryFocus.STRENGTH)
                            },
                            hybrid = { session.services.settings.startingPath("hybrid") },
                        )
                    } else {
                        CoachPage {
                            CoachHeading("Today")
                            if (saved) Text("Run saved.")
                            state.error?.let { Text(it) }
                            CoachButton("Record a run") { page = ApplePage.RECORD }
                            CoachButton("Settings") { page = ApplePage.SETTINGS }
                        }
                    }
            }
        }
    }
}

@Composable
private fun AppleHealthControls(
    session: AppleCoachSession<Unit>,
    preferences: NSUserDefaults,
    workoutKey: String,
    weightKey: String,
    heightKey: String,
    metrics: () -> Set<BodyMetric>,
) {
    val scope = rememberCoroutineScope()
    var workouts by remember { mutableStateOf(preferences.boolForKey(workoutKey)) }
    var weight by remember { mutableStateOf(preferences.boolForKey(weightKey)) }
    var height by remember { mutableStateOf(preferences.boolForKey(heightKey)) }
    var pending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    CoachCard {
        Text("Apple Health", style = MaterialTheme.typography.titleLarge)
        Text("Choose what to read. Training and manual entry work without Apple Health.")
        Row {
            Checkbox(
                workouts,
                {
                    workouts = it
                    preferences.setBool(it, workoutKey)
                },
            )
            Text("Running workouts")
        }
        Row {
            Checkbox(
                weight,
                {
                    weight = it
                    preferences.setBool(it, weightKey)
                },
            )
            Text("Weight")
        }
        Row {
            Checkbox(
                height,
                {
                    height = it
                    preferences.setBool(it, heightKey)
                },
            )
            Text("Height")
        }
        Button(
            enabled = !pending && (workouts || weight || height),
            onClick = {
                pending = true
                scope.launch {
                    try {
                        if (workouts) session.health.requestWorkoutReadAccess()
                        val selected = metrics()
                        if (selected.isNotEmpty()) session.health.requestBodyReadAccess(selected)
                        session.services.health.syncHealth()
                        message =
                            "Apple Health shares only records you allow. A finished permission request does not confirm access."
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        message = failure.message ?: "Could not request Apple Health access."
                    } finally {
                        pending = false
                    }
                }
            },
        ) {
            Text("Request access and import")
        }
        message?.let { Text(it) }
    }
}
