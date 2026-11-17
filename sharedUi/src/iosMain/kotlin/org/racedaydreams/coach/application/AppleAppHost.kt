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
import org.racedaydreams.coach.strength.SharedStrengthAnalyticsScreen
import org.racedaydreams.coach.theme.SharedRaceDayTheme
import org.racedaydreams.domain.presentation.AthleteLanguage
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.settings.PrimaryFocus
import org.racedaydreams.domain.strength.StrengthMeasurementPolicy
import org.racedaydreams.health.body.BodyMetric
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.recording.AppleRecordingClock
import org.racedaydreams.storage.LocalBackupOperations.ReviewedBackup
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
            var restoring by remember { mutableStateOf(false) }
            var restoreError by remember { mutableStateOf<String?>(null) }
            val hostScope = rememberCoroutineScope()
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
                if (restoring) {
                    CoachPage { Text("Restoring app data…") }
                } else if (active != null) {
                    AppleAppContent(
                        active,
                        controller,
                        onRestore = { reviewed ->
                            restoring = true
                            restoreError = null
                            hostScope.launch {
                                try {
                                    active.services.operations.serialiseDraftWrite {
                                        active.backupDocuments.restoreReviewed(reviewed)
                                    }
                                    session = null
                                    active.close()
                                    attempt++
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    val message = failure.message ?: "Could not restore the backup."
                                    if (session == null) startupError = message
                                    else restoreError = message
                                } finally {
                                    runCatching { reviewed.close() }
                                    restoring = false
                                }
                            }
                        },
                        restoreError = restoreError,
                    ) { bodyOnly ->
                        AppleHealthControls(
                            active,
                            preferences,
                            workoutKey,
                            weightKey,
                            heightKey,
                            metrics,
                            bodyOnly,
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
    HISTORY,
    PLAN,
    WEEKLY,
    BASELINE,
    BODY,
    GYM,
    PROGRAMMES,
    EXERCISES,
    STRENGTH_HISTORY,
    STRENGTH_ANALYTICS,
    ACHIEVEMENTS,
    RUNNING_TRENDS,
    STRENGTH_RECORD,
    ACTIVITY,
    SETTINGS,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleAppContent(
    session: AppleCoachSession<Unit>,
    controller: UIViewController,
    onRestore: (ReviewedBackup) -> Unit,
    restoreError: String?,
    health: @Composable (Boolean) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    var page by remember { mutableStateOf(ApplePage.START) }
    var calendarWorkout by remember { mutableStateOf<String?>(null) }
    var calendarStrength by remember { mutableStateOf<String?>(null) }
    var recordingWorkout by remember { mutableStateOf<String?>(null) }
    var activityId by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        restoreError?.let { Text(it, Modifier.padding(horizontal = 12.dp)) }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            TextButton(onClick = { page = ApplePage.START }) { Text("Today") }
            TextButton(
                onClick = {
                    recordingWorkout = null
                    page = ApplePage.RECORD
                }
            ) {
                Text("Record")
            }
            TextButton(onClick = { page = ApplePage.PLAN }) { Text("Plan") }
            TextButton(onClick = { page = ApplePage.GYM }) { Text("Gym") }
            TextButton(
                onClick = {
                    calendarWorkout = null
                    calendarStrength = null
                    page = ApplePage.HISTORY
                }
            ) {
                Text("Calendar")
            }
            TextButton(onClick = { page = ApplePage.SETTINGS }) { Text("Settings") }
        }
        Box(Modifier.weight(1f)) {
            when (page) {
                ApplePage.SETTINGS ->
                    AppleSettingsScreen(session, controller, onRestore) {
                        CoachButton("Body measurements and protein") { page = ApplePage.BODY }
                        health(false)
                    }
                ApplePage.BODY ->
                    AppleBodyMeasurements(
                        session,
                        back = {
                            session.services.operations.refresh()
                            page = ApplePage.START
                        },
                        healthControls = { health(true) },
                    )
                ApplePage.RECORD ->
                    AppleRecordingScreen(session, recordingWorkout) {
                        activityId = it
                        page = ApplePage.ACTIVITY
                    }
                ApplePage.HISTORY ->
                    AppleCalendar(
                        session,
                        calendarWorkout,
                        calendarStrength,
                        startPlanned = {
                            recordingWorkout = it
                            page = ApplePage.RECORD
                        },
                    ) {
                        activityId = it
                        page = ApplePage.ACTIVITY
                    }
                ApplePage.PLAN ->
                    AppleRunningPlan(
                        session,
                        openWorkout = {
                            calendarWorkout = it
                            calendarStrength = null
                            page = ApplePage.HISTORY
                        },
                        openStrength = {
                            calendarWorkout = null
                            calendarStrength = it.toString()
                            page = ApplePage.HISTORY
                        },
                    )
                ApplePage.WEEKLY -> AppleWeeklyReview(session, state) { page = ApplePage.PLAN }
                ApplePage.BASELINE -> AppleBaselineSummary(state)
                ApplePage.GYM ->
                    Column {
                        TextButton(onClick = { page = ApplePage.PROGRAMMES }) {
                            Text("Strength programmes")
                        }
                        FlowRow {
                            TextButton(onClick = { page = ApplePage.EXERCISES }) {
                                Text("Exercises & technique")
                            }
                            TextButton(onClick = { page = ApplePage.STRENGTH_ANALYTICS }) {
                                Text("Strength trends")
                            }
                            TextButton(onClick = { page = ApplePage.ACHIEVEMENTS }) {
                                Text("Recorded milestones")
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            AppleGymScreen(session) { page = ApplePage.START }
                        }
                    }
                ApplePage.PROGRAMMES -> AppleStrengthProgrammes(session) { page = ApplePage.GYM }
                ApplePage.EXERCISES -> AppleExerciseLibrary(session) { page = ApplePage.GYM }
                ApplePage.STRENGTH_HISTORY ->
                    AppleHistoryScreen(session, strengthOnly = true) {
                        activityId = it
                        page = ApplePage.ACTIVITY
                    }
                ApplePage.STRENGTH_ANALYTICS ->
                    SharedStrengthAnalyticsScreen(
                        state.strengthSessions,
                        { page = ApplePage.GYM },
                        AppleRecordingClock.now(),
                        AppleCalendarDates.today(),
                        AppleCalendarDates::label,
                        { it.toString().removeSuffix(".0") },
                        StrengthMeasurementPolicy::duration,
                        AthleteLanguage.WORKING_SETS,
                    )
                ApplePage.RUNNING_TRENDS -> AppleRunningTrends(state) { page = ApplePage.START }
                ApplePage.ACHIEVEMENTS ->
                    AppleAchievements(
                        session,
                        back = { page = ApplePage.START },
                        openRun = {
                            activityId = it
                            page = ApplePage.ACTIVITY
                        },
                        openGym = {
                            activityId = it
                            page = ApplePage.STRENGTH_RECORD
                        },
                    )
                ApplePage.STRENGTH_RECORD ->
                    activityId?.let {
                        AppleStrengthRecord(state, it) { page = ApplePage.ACHIEVEMENTS }
                    }
                ApplePage.ACTIVITY -> activityId?.let { AppleRunDetails(session, it) }
                ApplePage.START ->
                    if (state.loading) {
                        CoachPage { Text("Loading local training…") }
                    } else if (!state.busy && state.primaryFocus == null) {
                        SharedStartingPath(
                            record = {
                                session.services.settings.startingPath("record") {
                                    page = ApplePage.RECORD
                                }
                            },
                            race = {
                                session.services.settings.startingPath("race") {
                                    page = ApplePage.PLAN
                                }
                            },
                            strength = {
                                session.services.settings.primaryFocus(PrimaryFocus.STRENGTH) {
                                    page = ApplePage.GYM
                                }
                            },
                            hybrid = {
                                session.services.settings.startingPath("hybrid") {
                                    page = ApplePage.PLAN
                                }
                            },
                        )
                    } else if (
                        state.primaryFocus == PrimaryFocus.STRENGTH && state.strengthToday != null
                    ) {
                        val today = AppleCalendarDates.today()
                        SharedStrengthTodayScreen(
                            requireNotNull(state.strengthToday),
                            today,
                            AppleCalendarDates.label(today),
                            state.authoredGymProgramme != null || state.gymProgramme != null,
                            state.gymProgramme?.settings()?.minutes(),
                            open = { route ->
                                page =
                                    when {
                                        route == "gym" -> ApplePage.PROGRAMMES
                                        route == "exercises" || route == "strength/pbs" ->
                                            ApplePage.EXERCISES
                                        route.startsWith("history") -> ApplePage.STRENGTH_HISTORY
                                        else -> ApplePage.GYM
                                    }
                            },
                            openSession = {
                                calendarWorkout = null
                                calendarStrength = today.toString()
                                page = ApplePage.HISTORY
                            },
                            gymNow = { page = ApplePage.GYM },
                        )
                    } else if (state.primaryFocus == PrimaryFocus.STRENGTH) {
                        CoachPage { Text(state.error ?: "Loading strength training…") }
                    } else {
                        SharedHomeScreen(
                            state = state,
                            presentation = appleHomePresentation(state),
                            strengthContent = { Text("Loading strength training…") },
                            weekly = { page = ApplePage.WEEKLY },
                            baseline = { page = ApplePage.BASELINE },
                            assessment = { page = ApplePage.BASELINE },
                            history = {
                                calendarWorkout = null
                                calendarStrength = null
                                page = ApplePage.HISTORY
                            },
                            plan = { page = ApplePage.PLAN },
                            workout = {
                                calendarWorkout = it
                                calendarStrength = null
                                page = ApplePage.HISTORY
                            },
                            record = {
                                recordingWorkout = null
                                page = ApplePage.RECORD
                            },
                            quick = { route ->
                                when {
                                    route.startsWith("execute/") -> {
                                        val version = state.currentPlan
                                        val prefix = "execute/${version?.id()}/"
                                        if (version != null && route.startsWith(prefix)) {
                                            recordingWorkout = route.removePrefix(prefix)
                                            page = ApplePage.RECORD
                                        }
                                    }
                                    route.startsWith("activity/") -> {
                                        activityId = route.removePrefix("activity/")
                                        page = ApplePage.ACTIVITY
                                    }
                                    route.startsWith("gym/session/") -> {
                                        val id = route.removePrefix("gym/session/")
                                        val date =
                                            state.authoredGymProgramme
                                                ?.workouts()
                                                ?.firstOrNull { it.id() == id }
                                                ?.date()
                                                ?: state.gymProgramme
                                                    ?.sessions()
                                                    ?.firstOrNull { it.id() == id }
                                                    ?.date()
                                        calendarWorkout = null
                                        calendarStrength = date?.toString()
                                        page = ApplePage.HISTORY
                                    }
                                    route == "progress/trends" -> page = ApplePage.RUNNING_TRENDS
                                    route == "achievements" -> page = ApplePage.ACHIEVEMENTS
                                    route == "progress/weight" -> page = ApplePage.BODY
                                    route == "gym-now" || route.startsWith("strength") ->
                                        page = ApplePage.GYM
                                }
                            },
                            race = { page = ApplePage.PLAN },
                        )
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
    bodyOnly: Boolean,
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
        if (!bodyOnly) {
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
            enabled = !pending && ((!bodyOnly && workouts) || weight || height),
            onClick = {
                pending = true
                scope.launch {
                    try {
                        if (!bodyOnly && workouts) session.health.requestWorkoutReadAccess()
                        val selected = metrics()
                        if (selected.isNotEmpty()) session.health.requestBodyReadAccess(selected)
                        if (bodyOnly) session.body.sync() else session.services.health.syncHealth()
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
