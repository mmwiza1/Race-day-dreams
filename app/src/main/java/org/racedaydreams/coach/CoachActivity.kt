package org.racedaydreams.coach

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.racedaydreams.domain.GoalMode
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.ui.OnboardingView
import java.time.ZoneOffset

/** Modern shell; the established goal form remains an AndroidView during incremental migration. */
class CoachActivity : ComponentActivity() {
    private lateinit var model: CoachViewModel
    private var hasResumed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        model = ViewModelProvider(this)[CoachViewModel::class.java]
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF93D4BF))
                else lightColorScheme(primary = Color(0xFF245D50))) {
                Surface(Modifier.fillMaxSize()) { CoachApp(model) }
            }
        }
    }

    override fun onStop() { model.flushDraft(); super.onStop() }

    override fun onResume() {
        super.onResume()
        // Recheck permissions and data after returning from Android settings or a document picker.
        if (hasResumed) model.refresh()
        hasResumed = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachApp(model: CoachViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbars = remember { SnackbarHostState() }
    val context = LocalContext.current
    var resetDialog by rememberSaveable { mutableStateOf(false) }
    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }
    val permissions = rememberLauncherForActivityResult(model.health.permissionContract(), model::permissionResult)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        if (it != null) model.exportBackup(it)
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        restoreUri = it?.toString()
    }
    val connect: (Boolean, Boolean) -> Unit = { heartRate, history ->
        val requested = HealthConnectGateway.BASELINE_PERMISSIONS.toMutableSet()
        if (heartRate) requested.addAll(HealthConnectGateway.OPTIONAL_PERMISSIONS)
        if (history && model.health.historyPermissionAvailable()) requested.add(HealthConnectGateway.HISTORY_PERMISSION)
        permissions.launch(requested)
    }
    val providerUpdate: () -> Unit = {
        try { context.startActivity(model.health.providerUpdateIntent()) }
        catch (_: ActivityNotFoundException) { model.refresh() }
    }
    val providerSettings: () -> Unit = {
        try { context.startActivity(model.health.settingsIntent()) }
        catch (_: ActivityNotFoundException) { model.refresh() }
    }

    LaunchedEffect(state.message, state.error) {
        (state.error ?: state.message)?.let { snackbars.showSnackbar(it); model.dismissMessage() }
    }
    LaunchedEffect(state.savedProfileRevision) {
        if (state.savedProfileRevision > 0 && state.profile != null) {
            nav.navigate("home") {
                popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true
            }
            model.acknowledgeProfileSaved(state.savedProfileRevision)
        }
    }
    LaunchedEffect(state.profile, state.loading) {
        if (!state.loading && state.profile == null && route in listOf("home", "plan", "calendar", "progress", "settings", "shoes")) {
            nav.navigate("baseline") { popUpTo(nav.graph.id) { inclusive = true } }
        }
    }

    if (state.loading) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Text("Loading your local running information…", Modifier.padding(top = 16.dp))
        }
        return
    }

    // Profile/review changes navigate explicitly. Rebuilding this graph while a runner is
    // typing would recreate the retained goal form and can discard the current interaction.
    val startDestination = rememberSaveable {
        when {
            state.profile != null -> "home"
            state.baselineReviewed -> "goals"
            else -> "baseline"
        }
    }

    val tabs = listOf("home" to "Home", "plan" to "Plan", "calendar" to "Calendar", "progress" to "Progress", "strength" to "Strength")
    Scaffold(
        topBar = { TopAppBar(title = { Text("Race Day Dreams") }, navigationIcon = {
            if (route != null && route !in tabs.map { it.first } && route != "goals") {
                TextButton(onClick = { if (!nav.popBackStack()) nav.navigate("home") }) { Text("Back") }
            }
        }, actions = { TextButton(onClick={nav.navigate("settings")}) { Text("⚙ Settings") } }) },
        bottomBar = {
            if (state.profile != null && route in (tabs.map { it.first } + "strength?date={date}")) {
                NavigationBar {
                    tabs.forEach { (destination, label) ->
                        NavigationBarItem(selected = route == destination || destination == "strength" && route?.startsWith("strength") == true,
                            icon = { Text(label.take(1), Modifier.clearAndSetSemantics { }) },
                            label = { Text(label) }, onClick = {
                                nav.navigate(destination) { popUpTo("home"); launchSingleTop = true }
                            })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.error != null && state.profile == null && state.activities.isEmpty()) {
                Text(state.error!!, Modifier.padding(16.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = { model.refresh() }) { Text("Try again") }
                    TextButton(onClick = { resetDialog = true }) { Text("Delete local data") }
                }
            }
            NavHost(navController = nav, startDestination = startDestination, modifier = Modifier.weight(1f)) {
                composable("weekly") { WeeklyReviewScreen(state,model) }
                composable("home") { if(state.weeklyReview!=null) {}
                    HomeScreen(state, { nav.navigate("weekly") }, { nav.navigate("baseline") }, { nav.navigate("assessment") }, { nav.navigate("calendar") }, { nav.navigate("plan") }, { nav.navigate("workout/${Uri.encode(it)}") }) }
                composable("baseline") {
                    BaselineScreen(state, model, model.health.historyPermissionAvailable(), connect, providerUpdate,
                        onManual = { nav.navigate("manual") }, onAssessment = { nav.navigate("assessment") },
                        onContinue = {
                            model.reviewBaseline {
                                nav.navigate(if (state.profile == null) "goals" else "home") { launchSingleTop = true }
                            }
                        })
                }
                composable("manual") { ManualBaselineScreen(state, model) { nav.popBackStack() } }
                composable("assessment") { AssessmentScreen(state, model) { nav.navigate("calendar") { launchSingleTop = true } } }
                composable("goals") { GoalForm(state, model) {
                    model.cancelGoalEditing()
                    if (state.profile == null) nav.navigate("baseline") else nav.navigate("home") { launchSingleTop = true }
                } }
                composable("plan") { TrainingPlanScreen(state, model, { nav.navigate("workout/${Uri.encode(it)}") }, { nav.navigate("goals") }) }
                composable("workout/{id}") { destination -> PlannedWorkoutScreen(state, model, destination.arguments?.getString("id")) { nav.navigate("activity/${Uri.encode(it)}") } }
                composable("strength?date={date}") { destination -> StrengthScreen(state, model, destination.arguments?.getString("date")) }
                composable("calendar") { ActivityCalendarScreen(state, model, { nav.navigate("workout/${Uri.encode(it)}") }, { nav.navigate("strength?date=$it") }) { nav.navigate("activity/${Uri.encode(it)}") } }
                composable("activity/{id}") { destination ->
                    ActivityDetailScreen(state, model, destination.arguments?.getString("id"))
                }
                composable("progress") { ProgressScreen(state, model, { nav.navigate("activity/${Uri.encode(it)}") }, { nav.navigate("shoes") }, { nav.navigate("strength") }) }
                composable("shoes") { ShoesScreen(state, model) }
                composable("settings") { SettingsScreen(state, model, model.health.historyPermissionAvailable(), connect,
                    providerUpdate, providerSettings,
                    { nav.navigate("goals") }, { nav.navigate("baseline") }, { nav.navigate("shoes") },
                    { export.launch("race-day-dreams-backup.json") }, { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    { resetDialog = true }, { permissions.launch(setOf(HealthConnectGateway.WEIGHT_PERMISSION)) }) }
            }
        }
    }
    if (resetDialog) AlertDialog(onDismissRequest = { resetDialog = false },
        title = { Text("Delete local app data?") },
        text = { Text("This deletes your local profile, goals, activities, results, shoes and draft. It does not delete records held by Health Connect. This cannot be undone without a backup.") },
        confirmButton = { TextButton(onClick = { resetDialog = false; model.reset() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Cancel") } })
    if (restoreUri != null) AlertDialog(onDismissRequest = { restoreUri = null },
        title = { Text("Replace local app data from this backup?") },
        text = { Text("The file is validated before changes are made. A successful restore replaces this app’s current local data. Health Connect records can be imported again. Export your current data first if you want to keep it.") },
        confirmButton = { TextButton(onClick = { val uri = restoreUri; restoreUri = null; if (uri != null) model.restoreBackup(android.net.Uri.parse(uri)) }) { Text("Validate and replace") } },
        dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } })
}

@Composable
private fun GoalForm(state: CoachUiState, model: CoachViewModel, onCancel: () -> Unit) {
    var form by remember { mutableStateOf<OnboardingView?>(null) }
    BackHandler { if (form?.navigateBack() != true) onCancel() }
    AndroidView(modifier = Modifier.fillMaxSize().imePadding(), factory = { context ->
        OnboardingView(context, state.draft, state.profile, state.performanceEligible, object : OnboardingView.Listener {
            override fun onSave(profile: org.racedaydreams.domain.UserProfile) { model.saveProfile(profile) }
            override fun onDraftChanged(json: String) { model.saveDraft(json) }
            override fun onCancel() { onCancel() }
        }).also { form = it; it.tag = false }
    }, update = { view ->
        if (view.tag != state.busy) { view.setSaving(state.busy); view.tag = state.busy }
    })
}

@Composable
private fun HomeScreen(state: CoachUiState, weekly: () -> Unit, baseline: () -> Unit, assessment: () -> Unit, history: () -> Unit, plan: () -> Unit, workout: (String) -> Unit) = CoachPage {
    CoachHeading("Your running, your way")
    if(state.weeklyReview!=null) OutlinedButton(onClick=weekly,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Weekly coaching review")}
    state.currentPlan?.let { version ->
        val today = version.plan().workouts().firstOrNull { it.date() == java.time.LocalDate.now() }
        CoachHeading("Today")
        if (today == null) Text("No run is scheduled today. You can rest, run independently or review the programme.")
        else WorkoutCard(state, today, state.activities.any { it.plannedWorkoutId() == today.id() }) { workout(today.id()) }
        if (state.proposals.isNotEmpty()) Text("A coaching recommendation is ready to review. Your programme has not been changed.")
    }
    CoachButton(if (state.currentPlan == null) "Create training programme" else "View training programme", plan)
    state.profile?.let { profile ->
        CoachCard {
            Text(profile.raceGoal().distance().label(), style = MaterialTheme.typography.headlineSmall)
            if (profile.raceGoal().raceName().isNotBlank()) Text(profile.raceGoal().raceName())
            profile.raceGoal().raceDate()?.let { Text("Race date: ${friendlyDate(it)}") }
            Text("Goal: ${profile.raceGoal().mode().label()}")
            if (profile.raceGoal().mode() == GoalMode.TARGET_TIME) {
                Text("Target time: ${PaceCalculator.formatTime(profile.raceGoal().targetTime())}")
                Text("Target pace: ${PaceCalculator.targetPace(profile.raceGoal().targetTime(), profile.raceGoal().distance(), profile.preferences().units())}")
            }
            Text("${profile.preferences().runsPerWeek()} preferred runs/week · ${profile.preferences().strategy().label()}")
        }
    }
    Text("Your baseline confidence: ${state.baseline.confidence().name.lowercase().replaceFirstChar { it.uppercase() }}")
    WhyDetails(state.baseline.explanations().joinToString("\n"), "About your baseline")
    if (state.profile?.preferences()?.strategy() == org.racedaydreams.domain.TrainingStrategy.PERFORMANCE && !state.performanceEligible) {
        Text("Current evidence does not support extra performance-focused workload. Your saved goal and routine have not been changed.")
    }
    CoachButton("Review running baseline", baseline)
    CoachButton("View activity history", history)
    OutlinedButton(onClick = assessment, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Optional assessment result") }
}

@Composable
fun CoachPage(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable fun CoachHeading(text: String) { Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() }) }
@Composable fun CoachButton(text: String, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) } }
@Composable fun CoachCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}

fun displayActivityDate(activity: org.racedaydreams.domain.activity.ActivityRecord): String =
    activity.start().atOffset(activity.startOffset() ?: ZoneOffset.UTC).toLocalDate().let(::friendlyDate)
