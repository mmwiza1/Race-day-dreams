package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.*
import org.racedaydreams.coach.activity.SharedRunNotes
import org.racedaydreams.coach.documents.AppleBackupDocuments
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.ActivitySources
import org.racedaydreams.domain.activity.SplitAnalysis
import org.racedaydreams.domain.activity.SplitCalculator
import org.racedaydreams.domain.time.*
import org.racedaydreams.reminders.KotlinxReminderCalendar
import platform.UIKit.UIViewController

@Composable
fun AppleHistoryScreen(
    session: AppleCoachSession<*>,
    strengthOnly: Boolean = false,
    open: (String) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var showStrength by remember { mutableStateOf(strengthOnly) }
    if (showStrength) {
        CoachPage {
            CoachHeading("Strength history")
            CoachButton("Back to history") { showStrength = false }
            state.strengthSessions
                .sortedByDescending { it.startedAt() }
                .forEach { workout ->
                    CoachCard {
                        Text(dateLabel(workout.startedAt(), workout.offset()))
                        workout.exercises().forEach { Text(it.exercise().name()) }
                    }
                }
        }
        return
    }
    SharedActivityHistoryScreen(
        state = state,
        open = open,
        allStrength = { showStrength = true },
        loadSummaryPage = { time, id, limit ->
            withContext(applicationIoDispatcher()) {
                session.activities.activitySummaryPage(time, id, limit)
            }
        },
        activityDate = { dateLabel(it) },
        strengthDate = ::dateLabel,
        distance = { distanceLabel(it, imperial) },
        elapsedPace = { activity, miles ->
            activity
                .distanceMetres()
                ?.takeIf { it > 0 }
                ?.let { metres ->
                    val seconds =
                        activity.elapsedTime().getSeconds() +
                            activity.elapsedTime().getNano() / 1_000_000_000.0
                    val unitMetres = if (miles) 1609.344 else 1000.0
                    val pace =
                        PaceCalculator.formatTime(
                            Durations.ofSeconds((seconds * unitMetres / metres).roundToLong())
                        )
                    "$pace/${if (miles) "mi" else "km"}"
                }
        },
        emptyMessage =
            "No completed runs yet. Record a run or import selected workouts from Apple Health.",
    )
}

@Composable
fun AppleRunDetails(session: AppleCoachSession<*>, id: String, presenter: UIViewController) {
    val scope = rememberCoroutineScope()
    var exporting by remember(id) { mutableStateOf(false) }
    var exportStatus by remember(id) { mutableStateOf<String?>(null) }
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var activity by remember(id) { mutableStateOf<ActivityRecord?>(null) }
    var splits by remember(id, imperial) { mutableStateOf<SplitAnalysis?>(null) }
    var route by remember(id) { mutableStateOf<PreparedLiveRoute?>(null) }
    var splitDetailRemoved by remember(id) { mutableStateOf(false) }
    var detailRemoved by remember(id) { mutableStateOf(false) }
    var editingShoes by remember(id) { mutableStateOf(false) }
    var showRoute by remember(id) { mutableStateOf(false) }
    var showSplits by remember(id) { mutableStateOf(false) }
    var loading by remember(id) { mutableStateOf(true) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, imperial) {
        try {
            val (detail, analysis) =
                withContext(applicationIoDispatcher()) {
                    val detail = session.activities.recordedRunDetail(id)
                    val analysis =
                        detail
                            ?.takeUnless { it.detailRemoved }
                            ?.let {
                                SplitCalculator.calculate(
                                    it.activity,
                                    if (imperial) 1609.344 else 1000.0,
                                )
                            }
                    detail to analysis
                }
            activity = detail?.activity
            splits = analysis
            splitDetailRemoved = detail?.detailRemoved == true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }
    var routeFailed by remember(id) { mutableStateOf(false) }
    var routeLoading by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, showRoute) {
        if (!showRoute) return@LaunchedEffect
        routeLoading = true
        try {
            val (prepared, removed) =
                withContext(applicationIoDispatcher()) {
                    val saved = session.activities.recordedRoute(id)
                    saved.points
                        .takeIf { it.isNotEmpty() }
                        ?.let { PreparedLiveRoute.fromPoints(it) } to saved.detailRemoved
                }
            route = prepared
            detailRemoved = removed
            routeFailed = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            routeFailed = true
        } finally {
            routeLoading = false
        }
    }
    CoachPage {
        CoachHeading("Recorded run")
        val run = activity
        when {
            loading -> Text("Loading run…")
            failed -> Text("Could not load the run. Your records are retained.")
            run == null -> Text("This run is unavailable.")
            else ->
                // The shared summary, heart-rate and route sections carry their own labelled
                // panels.
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(dateLabel(run))
                    SharedRecordedRunSummary(
                        run,
                        { distanceLabel(it, imperial) },
                        sharedRecordedElapsedPace(run, imperial) { seconds ->
                            val rounded = kotlin.math.round(seconds).toLong()
                            "${rounded / 60}:${(rounded % 60).toString().padStart(2, '0')}"
                        },
                    )
                    SharedRecordedHeartRateSummary(run) { it.toString().removeSuffix(".0") }
                    AppleSavedDreamRoute(session, id)
                    if (run.source().provider() == "rdd") {
                        val mode by
                            produceState<String?>(null, id) {
                                value =
                                    withContext(applicationIoDispatcher()) {
                                        runCatching {
                                                session.runningSupport.recordingMode(
                                                    run.source().recordId()
                                                )
                                            }
                                            .getOrNull()
                                    }
                            }
                        mode?.let {
                            SharedSpontaneousAssociation(
                                state,
                                run,
                                it,
                                KotlinxReminderCalendar.at(
                                        run.start(),
                                        TimeZone.currentSystemDefault().id,
                                    )
                                    .date,
                                AppleCalendarDates.today(),
                                AppleCalendarDates::label,
                                session.services.runningPlan::chooseEasy,
                                { activity, version, workout ->
                                    session.services.runningEvidence.linkRun(
                                        activity,
                                        version,
                                        workout,
                                    )
                                },
                            )
                        }
                    }
                    Text("Source: ${ActivitySources.friendly(run.source())}")
                    val current = state.activities.firstOrNull { it.id() == id } ?: run
                    val shoeId = current.shoeId()
                    val shoe = state.shoes.firstOrNull { it.id() == shoeId }
                    Text(
                        shoe?.let { "Shoes: ${"${it.brand()} ${it.model()}".trim()}" }
                            ?: "No shoes assigned"
                    )
                    if (state.shoes.isNotEmpty()) {
                        TextButton(
                            onClick = { editingShoes = !editingShoes },
                            enabled = !state.busy,
                        ) {
                            Text(if (editingShoes) "Close shoe choices" else "Assign shoes")
                        }
                        if (editingShoes) {
                            TextButton(
                                onClick = {
                                    session.services.runningEvidence.assignShoe(id, null)
                                    editingShoes = false
                                },
                                enabled = !state.busy,
                            ) {
                                Text("No shoes")
                            }
                            state.shoes.forEach { choice ->
                                TextButton(
                                    onClick = {
                                        session.services.runningEvidence.assignShoe(id, choice.id())
                                        editingShoes = false
                                    },
                                    enabled = !state.busy,
                                ) {
                                    Text(
                                        "${choice.brand()} ${choice.model()}".trim() +
                                            if (choice.retired()) " (retired)" else ""
                                    )
                                }
                            }
                        }
                    }
                    if (shoe != null) {
                        val noted = id in state.shoeDiscomfort
                        Row(
                            Modifier.heightIn(min = 48.dp)
                                .toggleable(
                                    value = noted,
                                    enabled = !state.busy,
                                    role = Role.Checkbox,
                                    onValueChange = {
                                        session.services.runningEvidence.shoeDiscomfort(id, it)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = noted, onCheckedChange = null, enabled = !state.busy)
                            Text("I noted discomfort while wearing this shoe (no cause inferred)")
                        }
                    }
                    SharedRunNotes(current, state.busy) { notes, effort ->
                        session.services.runningEvidence.saveNotes(id, notes, effort)
                    }
                    (state.error ?: state.message)?.let { Text(it) }
                    TextButton(onClick = { showRoute = !showRoute }) {
                        Text(if (showRoute) "Hide route" else "View recorded route")
                    }
                    if (showRoute) {
                        when {
                            routeLoading -> Text("Loading recorded route…")
                            detailRemoved ->
                                Text(
                                    "Detailed route data was removed. The run summary is retained."
                                )
                            routeFailed -> Text("The recorded route could not be loaded.")
                            else -> SharedLiveRecordedRoute(route, unavailable = route == null)
                        }
                    }
                    if (
                        showRoute &&
                            !routeLoading &&
                            !routeFailed &&
                            !detailRemoved &&
                            (route?.xy?.size ?: 0) >= 2
                    ) {
                        TextButton(
                            enabled = !exporting,
                            onClick = {
                                exporting = true
                                exportStatus = null
                                scope.launch {
                                    try {
                                        session.backupDocuments.exportRecordedRoute(
                                            id,
                                            session.activities,
                                            presenter,
                                        ) {
                                            exporting = false
                                            exportStatus =
                                                when (it) {
                                                    AppleBackupDocuments.Outcome.SAVED ->
                                                        "GPX file saved."
                                                    AppleBackupDocuments.Outcome.CANCELLED ->
                                                        "GPX export cancelled."
                                                }
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (failure: Exception) {
                                        exporting = false
                                        exportStatus =
                                            failure.message ?: "Could not export the route."
                                    }
                                }
                            },
                        ) {
                            Text("Export recorded route (GPX)")
                        }
                    }
                    exportStatus?.let { Text(it) }
                    TextButton(onClick = { showSplits = !showSplits }) {
                        Text(if (showSplits) "Hide splits" else "View splits")
                    }
                    if (showSplits) {
                        val analysis = splits
                        if (splitDetailRemoved) {
                            Text("Detailed recording data was removed. Splits are unavailable.")
                        } else if (analysis?.splits().isNullOrEmpty()) {
                            Text("Qualified split data is unavailable.")
                        }
                        analysis?.reasons()?.forEach { Text(it) }
                        analysis?.splits()?.forEach { split ->
                            Text(
                                "${split.number()} · ${distanceLabel(split.distanceMetres(), imperial)} · ${PaceCalculator.formatTime(split.duration())}"
                            )
                        }
                    }
                }
        }
    }
}

/** Runs use the shared dating rule: the source's offset, else the device zone (audit B-01). */
private fun dateLabel(run: ActivityRecord): String =
    dateText(org.racedaydreams.domain.activity.ActivityDates.localDate(run))

private fun dateLabel(instant: Instant, offset: ZoneOffset?): String =
    dateText(org.racedaydreams.domain.activity.ActivityDates.localDate(instant, offset))

private fun dateText(date: LocalDate): String {
    val day = date.getDayOfMonth().toString().padStart(2, '0')
    val month = date.getMonthValue().toString().padStart(2, '0')
    return "$day/$month/${date.getYear()}"
}

internal fun distanceLabel(metres: Double, miles: Boolean): String {
    val hundredths = (metres / (if (miles) 1609.344 else 1000.0) * 100).roundToLong()
    val fraction = (hundredths % 100).toString().padStart(2, '0')
    return "${hundredths / 100}.$fraction ${if (miles) "mi" else "km"}"
}
