package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import org.racedaydreams.domain.presentation.CalendarData
import java.time.temporal.ChronoUnit
import java.time.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*

@Composable
fun TrainingPlanScreen(state: CoachUiState, model: CoachViewModel, open: (String) -> Unit, editGoal: () -> Unit, strength: (LocalDate) -> Unit = {}, routines: () -> Unit = {}, routine: (String, String?) -> Unit = { _, _ -> }) =
    SharedTrainingPlanScreen(
        state, AndroidCoachCalendarDates,
        { start -> "${start.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.UK))} – ${start.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu", java.util.Locale.UK))}" },
        { formatDistance(it, state) },
        { model.runningPlan.generatePlan() },
        { proposal, choice -> model.runningPlan.decide(proposal, choice) },
        open, editGoal, strength, routine,
        { id, related, action -> WarmupSupportLink(id, related, action) },
        { MobilitySupportCard(routines) },
    )

@Composable
fun WorkoutCard(state: CoachUiState, workout: PlannedWorkout, completed: Boolean, open: () -> Unit) = SemanticCard(if(completed)Tile.Progress else Tile.Planned) {
    Text("${friendlyDate(workout.date())} · ${if (completed) "Completed" else if (workout.date() < LocalDate.now()) "Workout not completed" else "Planned"}")
    Text(workoutName(workout.category()), style = MaterialTheme.typography.titleMedium)
    Text(workout.distanceKm()?.let { formatDistance(it * 1000, state) } ?: workout.duration()?.let { "${it.toMinutes()} min" } ?: "Prescription in detail")
    TextButton(onClick = open, modifier = Modifier.heightIn(min = 48.dp)) { Text("View workout and purpose") }
}

@Composable
fun PlannedWorkoutScreen(state: CoachUiState, model: CoachViewModel, id: String?, openActivity: (String) -> Unit, startRun: (String,String) -> Unit = {_,_->}, routine:(String,String?)->Unit={_,_->}) {
    val version = state.currentPlan
    val workout = version?.plan()?.workouts()?.firstOrNull { it.id() == id }
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val paceFactor = if (imperial) 1.609344 else 1.0
    var recording by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        if (workout == null) { CoachHeading("Workout unavailable"); Text("The current programme no longer contains this workout. Completed prescriptions remain linked to their preserved version.") }
        else {
            CoachHeading(workoutName(workout.category()))
            Text("${friendlyDate(workout.date())} · plan version ${version.version()}")
            Text(workout.distanceKm()?.let { formatDistance(it * 1000, state) } ?: workout.duration()?.let { "${it.toMinutes()} min" } ?: "Runner-owned race")
            val briefing=org.racedaydreams.domain.coaching.WorkoutContext.briefing(workout,state.plannedWorkouts,state.activities,state.strengthSessions,state.weeklyReview)
            CoachCard { Text("Today’s focus",style=MaterialTheme.typography.titleMedium);Text(requireNotNull(briefing.title()));WhyDetails(requireNotNull(briefing.observations()).joinToString("\n"),"Before you run") }
            if(workout.date()>=LocalDate.now()&&state.activities.none{it.plannedWorkoutId()==workout.id()})RecoveryContextSection(state,model,if(workout.category()==WorkoutCategory.RACE)null else workout.id())
            CoachButton("Start with Live Coach"){startRun(version.id(),workout.id())}
            val warm=org.racedaydreams.domain.support.RunningRoutines.warmup(workout.category(),version.goalSnapshot().raceGoal().distance().kilometres())
            WarmupSupportLink(warm,workout.id()){routine(warm,workout.id())}
            TextButton(onClick={routine(if(workout.category()==WorkoutCategory.LONG_RUN||org.racedaydreams.domain.coaching.AdaptiveCoachingEngine.isQuality(workout.category())||workout.category()==WorkoutCategory.RACE)"cool-demanding"else"cool-easy",workout.id())}){Text("View optional cool-down")}
            Text(org.racedaydreams.domain.presentation.AthleteLanguage.runningPurpose(workout.category()))
            CoachHeading("Workout structure")
            workout.segments().forEachIndexed { index, segment -> CoachCard {
                Text("${index + 1}. ${segment.kind().name.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()}}", style = MaterialTheme.typography.titleMedium)
                segment.distanceKm()?.let { Text(formatDistance(it * 1000, state)) }; segment.duration()?.let { Text("${it.toMinutes()} min") }
                segment.rpe()?.let { Text("How hard it should feel: ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.effortDescription(it)}.");WhyDetails("RPE (rating of perceived effort): ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.number(it)}/10. This is the effort target saved in this plan.","Exact effort target") }
                val minimumPace = segment.minimumSecondsPerKm()
                val maximumPace = segment.maximumSecondsPerKm()
                if (minimumPace != null && maximumPace != null) Text("Pace: ${formatPaceSeconds(minimumPace * paceFactor)}–${formatPaceSeconds(maximumPace * paceFactor)}/${if (imperial) "mi" else "km"}; effort takes precedence")
                Text(segment.guidance() ?: "Adjust to conditions and how you feel.")
            } }
            CoachHeading("Why this workout?")
            var moving by rememberSaveable { mutableStateOf(false) }
            var moveDate by rememberSaveable { mutableStateOf(workout.date().toString()) }
            if(workout.category()!=WorkoutCategory.RACE && state.runResponses.none{it.plannedWorkoutId()==workout.id()}) {
                TextButton(onClick={moving=!moving}){Text("Move or skip this session")}
                if(moving){DateInput("Move to",moveDate,{moveDate=it});val proposed=runCatching{LocalDate.parse(moveDate)}.getOrNull();org.racedaydreams.domain.coaching.WorkoutContext.moveWarnings(workout,proposed,state.plannedWorkouts,state.strengthSessions,state.gymProgramme).forEach{Text(it)};if(proposed!=null&&state.profile?.preferences()?.runningDays()?.contains(proposed.dayOfWeek)==false)Text("Outside your normal running days. This one-off move will not change your regular routine.");CoachButton("Move this workout only",{model.runningPlan.changeWorkout(workout.id(),LocalDate.parse(moveDate))});OutlinedButton(onClick={model.runningPlan.changeWorkout(workout.id())}){Text("Skip this session")}}
            }
            WhyDetails(workout.note() ?: "No explanation was recorded.","Why this workout?")
            Text("Goal: ${version.goalSnapshot().raceGoal().distance().label()} · ${version.goalSnapshot().raceGoal().mode().label()}")
            Text("You can run a different workout, take it easy or rest. Record what actually happened; your original prescription is retained. No session is blocked by a recommendation.")
            val actual = state.activities.firstOrNull { it.plannedWorkoutId() == workout.id() }
            if (actual != null) CoachButton("View completed run and feedback") { openActivity(actual.id()) }
            else if (workout.date() <= LocalDate.now()) {
                CoachButton(if (recording) "Hide completion form" else "Record actual run") { recording = !recording }
                if (recording) CompletionForm(state, model, version, workout, openActivity)
                var linking by rememberSaveable{mutableStateOf(false)}
                OutlinedButton(onClick={linking=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Link completed activity")}
                if(linking)AlertDialog(onDismissRequest={linking=false},title={Text("Choose recorded run")},text={androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=320.dp)){val candidates=state.activities.filter{a->a.plannedWorkoutId()==null}.sortedByDescending{it.start()}.take(100);if(candidates.isEmpty())item{Text("No unlinked activities. Import or record a run first.")};items(candidates.size){i->val a=candidates[i];TextButton(onClick={model.runningEvidence.linkRun(a.id(),version.id(),workout.id());linking=false;openActivity(a.id())}){Text("${displayActivityDate(a)} · ${a.distanceMetres()?.let{formatDistance(it,state)}?:"Distance unavailable"}")}}}},confirmButton={TextButton(onClick={linking=false}){Text("Cancel")}})

            } else Text("Record completion after the workout takes place. Phone GPS recording is available from Home → Record a run.")
        }
    }
}

@Composable
private fun CompletionForm(state: CoachUiState, model: CoachViewModel, version: PlanVersion, workout: PlannedWorkout, done: (String) -> Unit) {
    var km by rememberSaveable { mutableStateOf("") }; var duration by rememberSaveable { mutableStateOf("") }
    var rpe by rememberSaveable { mutableStateOf("") }; var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var time by rememberSaveable { mutableStateOf("08:00") }; var notes by rememberSaveable { mutableStateOf("") }; var error by rememberSaveable { mutableStateOf<String?>(null) }
    CoachCard {
        Text("Actual workout — manually entered")
        CoachInput(if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) "Actual distance (miles)" else "Actual distance (km)", km, { km=it }, true)
        CoachInput("Actual duration (mm:ss or h:mm:ss)", duration, { duration=it })
        CoachInput("Start date (YYYY-MM-DD)", date, { date=it })
        CoachInput("Start time (HH:mm, local)", time, { time=it })
        CoachInput("RPE 0–10 (optional)", rpe, { rpe=it }, true)
        CoachInput("Notes (optional)", notes, { notes=it })
        error?.let { Text(it) }
        Button(enabled = !state.busy, onClick = {
            try { model.runningEvidence.completeRun(version.id(), workout.id(), km.toDouble() * (if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) 1.609344 else 1.0), PaceCalculator.parseTime(duration), LocalDate.parse(date), LocalTime.parse(time).toString(),
                rpe.takeIf { it.isNotBlank() }?.toDouble(), notes, done); error=null }
            catch (e: Exception) { error=e.message ?: "Check actual values." }
        }, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Save actual run") }
    }
}

@Composable
fun CoachInput(label: String, value: String, change: (String) -> Unit, decimal: Boolean = false) {
    if(label.contains("YYYY-MM-DD")){DateInput(label.substringBefore(" ("),value,change);return}
    CoachTextInput(label, value, change, decimal)
}
