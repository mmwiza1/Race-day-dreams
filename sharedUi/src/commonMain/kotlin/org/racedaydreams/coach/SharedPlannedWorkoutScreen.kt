package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.time.*

/**
 * Existing workout UI and manual-entry flow. Actual coordinators and OS presentation remain
 * required bindings.
 */
@Composable
fun SharedWorkoutCard(
    state: CoachUiState,
    workout: PlannedWorkout,
    completed: Boolean,
    presentation: HomePresentation,
    dates: CoachCalendarDates,
    open: () -> Unit,
) =
    SemanticCard(if (completed) Tile.Progress else Tile.Planned) {
        Text(
            "${dates.label(workout.date())} · ${if (completed) "Completed" else if (workout.date().toEpochDay() < dates.today().toEpochDay()) "Workout not completed" else "Planned"}"
        )
        Text(
            presentation.workoutName(workout.category()),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            workout.distanceKm()?.let { presentation.distance(it * 1000) }
                ?: workout.duration()?.let { "${it.getSeconds() / 60} min" }
                ?: "Prescription in detail"
        )
        TextButton(onClick = open, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("View workout and purpose")
        }
    }

@Composable
fun SharedPlannedWorkoutScreen(
    state: CoachUiState,
    runningPlan: org.racedaydreams.coach.application.RunningPlanCoordinator,
    runningEvidence: org.racedaydreams.coach.application.RunningEvidenceCoordinator,
    id: String?,
    presentation: HomePresentation,
    dates: CoachCalendarDates,
    canonicalTime: (String) -> String,
    openActivity: (String) -> Unit,
    startRun: (String, String) -> Unit,
    routine: (String, String?) -> Unit,
    dateInput: @Composable (String, String, (String) -> Unit) -> Unit,
    recovery: @Composable (String?) -> Unit,
    warmup: @Composable (String, String, () -> Unit) -> Unit,
    input: @Composable (String, String, (String) -> Unit, Boolean) -> Unit,
) {
    val version = state.currentPlan
    val workout = version?.plan()?.workouts()?.firstOrNull { it.id() == id }
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val paceFactor = if (imperial) 1.609344 else 1.0
    var recording by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        if (workout == null) {
            CoachHeading("Workout unavailable")
            Text(
                "The current programme no longer contains this workout. Completed prescriptions remain linked to their preserved version."
            )
        } else {
            CoachHeading(presentation.workoutName(workout.category()))
            Text("${dates.label(workout.date())} · plan version ${version.version()}")
            Text(
                workout.distanceKm()?.let { presentation.distance(it * 1000) }
                    ?: workout.duration()?.let { "${it.getSeconds() / 60} min" }
                    ?: "Runner-owned race"
            )
            if (
                workout.distanceKm() != null &&
                    workout.segments().any {
                        it.kind() == WorkoutSegment.Kind.RECOVERY &&
                            it.distanceKm() == null &&
                            it.duration() != null
                    }
            ) {
                Text("The total includes an estimate for timed recoveries. Recover by time.")
            }
            val briefing =
                org.racedaydreams.domain.coaching.WorkoutContext.briefing(
                    workout,
                    state.plannedWorkouts,
                    state.activities,
                    state.strengthSessions,
                    state.weeklyReview,
                )
            CoachCard {
                Text("Today’s focus", style = MaterialTheme.typography.titleMedium)
                Text(requireNotNull(briefing.title()))
                WhyDetails(
                    requireNotNull(briefing.observations()).joinToString("\n"),
                    "Before you run",
                )
            }
            if (
                workout.date().toEpochDay() >= dates.today().toEpochDay() &&
                    state.activities.none { it.plannedWorkoutId() == workout.id() }
            )
                recovery(if (workout.category() == WorkoutCategory.RACE) null else workout.id())
            CoachButton("Start with Live Coach") { startRun(version.id(), workout.id()) }
            val warm =
                org.racedaydreams.domain.support.RunningRoutines.warmup(
                    workout.category(),
                    version.goalSnapshot().raceGoal().distance().kilometres(),
                )
            warmup(warm, workout.id()) { routine(warm, workout.id()) }
            TextButton(
                onClick = {
                    routine(
                        if (
                            workout.category() == WorkoutCategory.LONG_RUN ||
                                org.racedaydreams.domain.coaching.AdaptiveCoaching.isQuality(
                                    workout.category()
                                ) ||
                                workout.category() == WorkoutCategory.RACE
                        )
                            "cool-demanding"
                        else "cool-easy",
                        workout.id(),
                    )
                }
            ) {
                Text("View optional cool-down")
            }
            Text(
                org.racedaydreams.domain.presentation.AthleteLanguage.runningPurpose(
                    workout.category()
                )
            )
            CoachHeading("Workout structure")
            workout.segments().forEachIndexed { index, segment ->
                CoachCard {
                    Text(
                        "${index + 1}. ${segment.kind().name.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()}}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    segment.distanceKm()?.let { Text(presentation.distance(it * 1000)) }
                    segment.duration()?.let {
                        val seconds = it.getSeconds()
                        val minutes = seconds / 60
                        val remainder = seconds % 60
                        Text(
                            when {
                                minutes == 0L -> "$seconds sec"
                                remainder == 0L -> "$minutes min"
                                else -> "$minutes min $remainder sec"
                            }
                        )
                    }
                    segment.rpe()?.let {
                        Text(
                            "How hard it should feel: ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.effortDescription(it)}."
                        )
                        WhyDetails(
                            "RPE (rating of perceived effort): ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.number(it)}/10. This is the effort target saved in this plan.",
                            "Exact effort target",
                        )
                    }
                    val minimumPace = segment.minimumSecondsPerKm()
                    val maximumPace = segment.maximumSecondsPerKm()
                    if (minimumPace != null && maximumPace != null)
                        Text(
                            "Pace: ${presentation.paceSeconds(minimumPace * paceFactor)}–${presentation.paceSeconds(maximumPace * paceFactor)}/${if (imperial) "mi" else "km"}; effort takes precedence"
                        )
                    Text(segment.guidance() ?: "Adjust to conditions and how you feel.")
                }
            }
            CoachHeading("Why this workout?")
            var moving by rememberSaveable { mutableStateOf(false) }
            var moveDate by rememberSaveable { mutableStateOf(workout.date().toString()) }
            if (
                workout.category() != WorkoutCategory.RACE &&
                    state.runResponses.none { it.plannedWorkoutId() == workout.id() }
            ) {
                TextButton(onClick = { moving = !moving }) { Text("Move or skip this session") }
                if (moving) {
                    dateInput("Move to", moveDate, { moveDate = it })
                    val proposed = runCatching { dates.parse(moveDate) }.getOrNull()
                    org.racedaydreams.domain.coaching.WorkoutContext.moveWarnings(
                            workout,
                            proposed,
                            state.plannedWorkouts,
                            state.strengthSessions,
                            state.gymProgramme,
                        )
                        .forEach { Text(it) }
                    if (
                        proposed != null &&
                            state.profile
                                ?.preferences()
                                ?.runningDays()
                                ?.contains(proposed.getDayOfWeek()) == false
                    )
                        Text(
                            "Outside your normal running days. This one-off move will not change your regular routine."
                        )
                    CoachButton(
                        "Move this workout only",
                        { runningPlan.changeWorkout(workout.id(), dates.parse(moveDate)) },
                    )
                    OutlinedButton(onClick = { runningPlan.changeWorkout(workout.id()) }) {
                        Text("Skip this session")
                    }
                }
            }
            WhyDetails(workout.note() ?: "No explanation was recorded.", "Why this workout?")
            Text(
                "Goal: ${version.goalSnapshot().raceGoal().distance().label()} · ${version.goalSnapshot().raceGoal().mode().label()}"
            )
            Text(
                "You can run a different workout, take it easy or rest. Record what actually happened; your original prescription is retained. No session is blocked by a recommendation."
            )
            val actual = state.activities.firstOrNull { it.plannedWorkoutId() == workout.id() }
            if (actual != null)
                CoachButton("View completed run and feedback") { openActivity(actual.id()) }
            else if (workout.date().toEpochDay() <= dates.today().toEpochDay()) {
                CoachButton(if (recording) "Hide completion form" else "Record actual run") {
                    recording = !recording
                }
                if (recording)
                    SharedCompletionForm(
                        state,
                        runningEvidence,
                        dates,
                        canonicalTime,
                        input,
                        version,
                        workout,
                        openActivity,
                    )
                var linking by rememberSaveable { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { linking = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Link completed activity")
                }
                if (linking)
                    AlertDialog(
                        onDismissRequest = { linking = false },
                        title = { Text("Choose recorded run") },
                        text = {
                            androidx.compose.foundation.lazy.LazyColumn(
                                Modifier.heightIn(max = 320.dp)
                            ) {
                                val candidates =
                                    state.activities
                                        .filter { a -> a.plannedWorkoutId() == null }
                                        .sortedByDescending { it.start() }
                                        .take(100)
                                if (candidates.isEmpty())
                                    item {
                                        Text(
                                            "No unlinked activities. Import or record a run first."
                                        )
                                    }
                                items(candidates.size) { i ->
                                    val a = candidates[i]
                                    TextButton(
                                        onClick = {
                                            runningEvidence.linkRun(
                                                a.id(),
                                                version.id(),
                                                workout.id(),
                                            )
                                            linking = false
                                            openActivity(a.id())
                                        }
                                    ) {
                                        Text(
                                            "${presentation.activityDate(a)} · ${a.distanceMetres()?.let{presentation.distance(it)}?:"Distance unavailable"}"
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { linking = false }) { Text("Cancel") }
                        },
                    )
            } else
                Text(
                    "Record completion after the workout takes place. Phone GPS recording is available from Home → Record a run."
                )
        }
    }
}

@Composable
private fun SharedCompletionForm(
    state: CoachUiState,
    runningEvidence: org.racedaydreams.coach.application.RunningEvidenceCoordinator,
    dates: CoachCalendarDates,
    canonicalTime: (String) -> String,
    input: @Composable (String, String, (String) -> Unit, Boolean) -> Unit,
    version: PlanVersion,
    workout: PlannedWorkout,
    done: (String) -> Unit,
) {
    var km by rememberSaveable { mutableStateOf("") }
    var duration by rememberSaveable { mutableStateOf("") }
    var rpe by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(dates.today().toString()) }
    var time by rememberSaveable { mutableStateOf("08:00") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachCard {
        Text("Actual workout — manually entered")
        input(
            if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL)
                "Actual distance (miles)"
            else "Actual distance (km)",
            km,
            { km = it },
            true,
        )
        input("Actual duration (mm:ss or h:mm:ss)", duration, { duration = it }, false)
        input("Start date (YYYY-MM-DD)", date, { date = it }, false)
        input("Start time (HH:mm, local)", time, { time = it }, false)
        input("RPE 0–10 (optional)", rpe, { rpe = it }, true)
        input("Notes (optional)", notes, { notes = it }, false)
        error?.let { Text(it) }
        Button(
            enabled = !state.busy,
            onClick = {
                try {
                    runningEvidence.completeRun(
                        version.id(),
                        workout.id(),
                        km.toDouble() *
                            (if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL)
                                1.609344
                            else 1.0),
                        PaceCalculator.parseTime(duration),
                        dates.parse(date),
                        canonicalTime(time),
                        rpe.takeIf { it.isNotBlank() }?.toDouble(),
                        notes,
                        done,
                    )
                    error = null
                } catch (e: Exception) {
                    error = e.message ?: "Check actual values."
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save actual run")
        }
    }
}
