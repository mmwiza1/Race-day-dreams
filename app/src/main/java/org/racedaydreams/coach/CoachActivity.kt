package org.racedaydreams.coach

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.racedaydreams.coach.navigation.*
import org.racedaydreams.health.HealthConnectGateway

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
            MaterialTheme(
                colorScheme =
                    if (dark)
                        darkColorScheme(
                            primary = Color(0xFF9ACAFF),
                            background = Color(0xFF101923),
                            surface = Color(0xFF15202C),
                            onSurface = Color(0xFFE8F0FA),
                            onBackground = Color(0xFFE8F0FA),
                        )
                    else
                        lightColorScheme(
                            primary = Color(0xFF086AD8),
                            background = Color(0xFFF6F9FD),
                            surface = Color(0xFFFCFDFF),
                            onSurface = Color(0xFF14243B),
                            onBackground = Color(0xFF14243B),
                        )
            ) {
                Surface(Modifier.fillMaxSize()) { CoachApp(model) }
            }
        }
    }

    override fun onStop() {
        model.flushDraft()
        super.onStop()
    }

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
    var runSnapshot by rememberSaveable { mutableStateOf<String?>(null) }
    var resetDialog by rememberSaveable { mutableStateOf(false) }
    var restoreUri by rememberSaveable { mutableStateOf<String?>(null) }
    val permissions =
        rememberLauncherForActivityResult(
            model.health.permissionContract(),
            model::permissionResult,
        )
    val export =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) {
            if (it != null) model.exportBackup(it)
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            restoreUri = it?.toString()
        }
    val connect: (Boolean, Boolean) -> Unit = { heartRate, history ->
        val requested = HealthConnectGateway.BASELINE_PERMISSIONS.toMutableSet()
        if (heartRate) requested.addAll(HealthConnectGateway.OPTIONAL_PERMISSIONS)
        if (history && model.health.historyPermissionAvailable())
            requested.add(HealthConnectGateway.HISTORY_PERMISSION)
        permissions.launch(requested)
    }
    val providerUpdate: () -> Unit = {
        try {
            context.startActivity(model.health.providerUpdateIntent())
        } catch (_: ActivityNotFoundException) {
            model.refresh()
        }
    }
    val providerSettings: () -> Unit = {
        try {
            context.startActivity(model.health.settingsIntent())
        } catch (_: ActivityNotFoundException) {
            model.refresh()
        }
    }

    LaunchedEffect(state.message, state.error) {
        (state.error ?: state.message)?.let {
            snackbars.showSnackbar(it)
            model.dismissMessage()
        }
    }
    LaunchedEffect(state.savedProfileRevision) {
        if (state.savedProfileRevision > 0 && state.profile != null) {
            nav.navigate("home") {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
            model.acknowledgeProfileSaved(state.savedProfileRevision)
        }
    }
    LaunchedEffect(state.profile, state.loading) {
        if (!state.loading && state.profile == null && route == "plan") {
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
            org.racedaydreams.recording.RunRecordingService.running.value -> "record-run"
            state.profile != null -> "home"
            state.startingPath == "record" -> "home"
            state.baselineReviewed -> "goals"
            else -> "welcome"
        }
    }

    val tabs =
        listOf(
            "home" to "Home",
            "plan" to "Plan",
            "calendar" to "Calendar",
            "progress" to "Progress",
            "strength" to "Strength",
        )
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f)
                            "RDD"
                        else "Race Day Dreams",
                        maxLines = 1,
                        modifier = Modifier.semantics { contentDescription = "Race Day Dreams" },
                    )
                },
                navigationIcon = {
                    if (
                        route != null &&
                            route !in tabs.map { it.first } &&
                            route != "strength?date={date}" &&
                            route != "goals"
                    ) {
                        TextButton(onClick = { if (!nav.popBackStack()) nav.navigate("home") }) {
                            Text("Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { nav.navigate("settings") }) { Text("⚙ Settings") }
                },
            )
        },
        bottomBar = {
            if (route in (tabs.map { it.first } + "strength?date={date}")) {
                NavigationBar {
                    tabs.forEach { (destination, label) ->
                        val largeFont =
                            androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f
                        val navLabel: (@Composable () -> Unit)? =
                            if (largeFont) null
                            else ({ Text(label, maxLines = 1, softWrap = false) })
                        NavigationBarItem(
                            modifier =
                                if (largeFont) Modifier.semantics { contentDescription = label }
                                else Modifier,
                            selected =
                                route == destination ||
                                    destination == "strength" &&
                                        route?.startsWith("strength") == true,
                            icon = {
                                Icon(
                                    androidx.compose.ui.res.painterResource(
                                        when (destination) {
                                            "home" -> org.racedaydreams.R.drawable.ic_nav_home
                                            "plan" -> org.racedaydreams.R.drawable.ic_nav_plan
                                            "calendar" ->
                                                org.racedaydreams.R.drawable.ic_nav_calendar
                                            "progress" ->
                                                org.racedaydreams.R.drawable.ic_nav_progress
                                            else -> org.racedaydreams.R.drawable.ic_nav_strength
                                        }
                                    ),
                                    contentDescription = null,
                                )
                            },
                            label = navLabel,
                            onClick = {
                                nav.navigate(destination) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) },
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
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = Modifier.weight(1f),
            ) {
                homeGraph({ state }, model, nav)
                runningGraph(
                    { state },
                    model,
                    nav,
                    { runSnapshot },
                    startSpontaneous = { snapshot ->
                        runSnapshot = snapshot
                        nav.navigate("spontaneous-run")
                    },
                    connect = connect,
                    providerUpdate = providerUpdate,
                )
                strengthGraph({ state }, model, nav)
                progressGraph({ state }, model, nav)
                settingsGraph(
                    { state },
                    model,
                    nav,
                    connect,
                    providerUpdate,
                    providerSettings,
                    exportBackup = { export.launch("race-day-dreams-backup.json") },
                    restoreBackup = {
                        restore.launch(
                            arrayOf("application/json", "text/plain", "application/octet-stream")
                        )
                    },
                    requestReset = { resetDialog = true },
                    requestWeightPermission = {
                        permissions.launch(setOf(HealthConnectGateway.WEIGHT_PERMISSION))
                    },
                )
            }
        }
    }
    if (resetDialog)
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Delete local app data?") },
            text = {
                Text(
                    "This deletes your local profile, goals, activities, results, shoes and draft. It does not delete records held by Health Connect. This cannot be undone without a backup."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetDialog = false
                        model.reset()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Cancel") } },
        )
    if (restoreUri != null)
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Replace local app data from this backup?") },
            text = {
                Text(
                    "The file is validated before changes are made. A successful restore replaces this app’s current local data. Health Connect records can be imported again. Export your current data first if you want to keep it."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = restoreUri
                        restoreUri = null
                        if (uri != null) model.restoreBackup(android.net.Uri.parse(uri))
                    }
                ) {
                    Text("Validate and replace")
                }
            },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } },
        )
}
