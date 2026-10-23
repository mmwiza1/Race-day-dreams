package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.*

@Composable
fun TrainingPlanScreen(state: CoachUiState, model: CoachViewModel, open: (String) -> Unit, editGoal: () -> Unit, strength: (LocalDate) -> Unit = {}) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    var overview by rememberSaveable { mutableStateOf(false) }
    var week by rememberSaveable { mutableIntStateOf(0) }
    val version = state.currentPlan
    val today = LocalDate.now()
    CoachPage {
        CoachHeading("Your training programme")
        Text("The app recommends. The runner decides. The plan adapts.")
        if (version == null) {
            Text("Create an original programme from your saved goal, running evidence and availability. No training has been prescribed yet.")
            Text("Starting conservatively when evidence is limited.")
            WhyDetails("Missing or stale workload evidence starts with short conversational run/walk sessions. A short race horizon never triggers a catch-up build.")
            CoachButton("Generate my programme") { model.generatePlan() }
        } else {
            val plan = version.plan()
            val startWeek = plan.startDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val selectedWeek = currentWeek.plusWeeks(week.toLong()).coerceAtLeast(startWeek)
            Text("${version.goalSnapshot().raceGoal().distance().label()} · version ${version.version()}")
            Text("${friendlyDate(plan.startDate())} – ${friendlyDate(plan.endDate())}")
            WhyDetails(version.reason(),"Why this plan version?")
            if (state.profile != version.goalSnapshot()) Text("Your saved goal or availability has changed. This programme still uses its original snapshot. Review and explicitly rebuild when ready.")
            state.proposals.forEach { proposal -> AdjustmentCard(proposal, state.busy) { model.decide(proposal, it) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !overview, onClick = { overview = false }, label = { Text("Week") })
                FilterChip(selected = overview, onClick = { overview = true }, label = { Text("Programme") })
            }
            if (overview) {
                WhyDetails("Distant weeks are projections. The next few weeks are planned but adaptable; current-week sessions are concrete prescriptions.","How certain is the programme?")
                plan.phases().forEach { phase -> CoachCard {
                    Text(phase.kind().name.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()}, style = MaterialTheme.typography.titleMedium)
                    Text("${friendlyDate(phase.startDate())} – ${friendlyDate(phase.endDate())}")
                    WhyDetails(phase.explanation())
                } }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { week-- }, enabled = selectedWeek > startWeek) { Text("Previous") }
                    TextButton(onClick = { week = 0 }) { Text("Current week") }
                    TextButton(onClick = { week++ }, enabled = selectedWeek.plusWeeks(1) <= plan.endDate()) { Text("Next") }
                }
                Text("Week of ${friendlyDate(selectedWeek)}")
                val workouts = plan.workouts().filter { it.date() >= selectedWeek && it.date() < selectedWeek.plusWeeks(1) }
                if (workouts.isEmpty()) Text("No workouts scheduled in this week.")
                workouts.forEach { workout -> WorkoutCard(state, workout, state.activities.any { it.plannedWorkoutId() == workout.id() }) { open(workout.id()) } }
                state.strengthSessions.filter { it.startedAt().atOffset(it.offset()).toLocalDate() in selectedWeek..selectedWeek.plusDays(6) }.forEach {
                    Text("Strength · ${friendlyDate(it.startedAt().atOffset(it.offset()).toLocalDate())} · ${it.duration().toMinutes()} min · recorded")
                }
                repeat(7) { offset -> val day=selectedWeek.plusDays(offset.toLong())
                    val programmed=state.gymProgramme?.sessions()?.firstOrNull{it.date()==day}
                    if(programmed!=null)OutlinedButton(onClick={strength(day)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("${programmed.title()} · ${shortDate(day)} · View strength workout")}
                    else if(state.gymProgramme==null&&state.profile?.preferences()?.gymDays()?.contains(day.dayOfWeek)==true) OutlinedButton(onClick={strength(day)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Strength routine · ${shortDate(day)} · Log workout")}
                }
            }
            Text("Evidence used", style = MaterialTheme.typography.titleMedium)
            WhyDetails(version.evidenceSnapshot(),"Evidence used for this plan")
            Text("${state.planVersions.size} preserved plan versions. Earlier prescriptions remain available when linked to a completed run.")
            OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Rebuild remaining programme…") }
        }
        CoachButton("Edit goal and routine", editGoal)
        Text("Fitness guidance, not medical care. Stop exercise and seek appropriate medical advice for chest pain, fainting or severe difficulty breathing.")
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Rebuild your remaining programme?") },
        text = { Text("Use your current goal, availability and evidence from today onward. This can change future sessions substantially. Previous versions and completed prescriptions remain in history. Missed mileage will not be added to catch up.") },
        confirmButton = { TextButton(onClick = { confirm = false; model.generatePlan() }) { Text("Rebuild") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep current plan") } })
}

@Composable
fun WorkoutCard(state: CoachUiState, workout: PlannedWorkout, completed: Boolean, open: () -> Unit) = CoachCard {
    Text("${friendlyDate(workout.date())} · ${if (completed) "Completed" else if (workout.date() < LocalDate.now()) "Workout not completed" else "Planned"}")
    Text(workoutName(workout.category()), style = MaterialTheme.typography.titleMedium)
    Text(workout.distanceKm()?.let { formatDistance(it * 1000, state) } ?: workout.duration()?.let { "${it.toMinutes()} min" } ?: "Prescription in detail")
    TextButton(onClick = open, modifier = Modifier.heightIn(min = 48.dp)) { Text("View workout and purpose") }
}

@Composable
fun PlannedWorkoutScreen(state: CoachUiState, model: CoachViewModel, id: String?, openActivity: (String) -> Unit) {
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
            CoachCard { Text("Today’s focus",style=MaterialTheme.typography.titleMedium);Text(briefing.title());WhyDetails(briefing.observations().joinToString("\n"),"Before you run") }
            CoachHeading("Workout structure")
            workout.segments().forEachIndexed { index, segment -> CoachCard {
                Text("${index + 1}. ${segment.kind().name.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()}}", style = MaterialTheme.typography.titleMedium)
                segment.distanceKm()?.let { Text(formatDistance(it * 1000, state)) }; segment.duration()?.let { Text("${it.toMinutes()} min") }
                segment.rpe()?.let { Text("Intended RPE: approximately ${it.toInt()}/10") }
                if (segment.minimumSecondsPerKm() != null && segment.maximumSecondsPerKm() != null) Text("Pace: ${formatPaceSeconds(segment.minimumSecondsPerKm() * paceFactor)}–${formatPaceSeconds(segment.maximumSecondsPerKm() * paceFactor)}/${if (imperial) "mi" else "km"}; effort takes precedence")
                Text(segment.guidance() ?: "Adjust to conditions and how you feel.")
            } }
            CoachHeading("Why this workout?")
            var moving by rememberSaveable { mutableStateOf(false) }
            var moveDate by rememberSaveable { mutableStateOf(workout.date().toString()) }
            if(workout.category()!=WorkoutCategory.RACE && state.runResponses.none{it.plannedWorkoutId()==workout.id()}) {
                TextButton(onClick={moving=!moving}){Text("Move or skip this session")}
                if(moving){DateInput("Move to",moveDate,{moveDate=it});val proposed=runCatching{LocalDate.parse(moveDate)}.getOrNull();org.racedaydreams.domain.coaching.WorkoutContext.moveWarnings(workout,proposed,state.plannedWorkouts,state.strengthSessions,state.gymProgramme).forEach{Text(it)};if(proposed!=null&&state.profile?.preferences()?.runningDays()?.contains(proposed.dayOfWeek)==false)Text("Outside your normal running days. This one-off move will not change your regular routine.");CoachButton("Move this workout only",{model.changeWorkout(workout.id(),LocalDate.parse(moveDate))});OutlinedButton(onClick={model.changeWorkout(workout.id())}){Text("Skip this session")}}
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
                if(linking)AlertDialog(onDismissRequest={linking=false},title={Text("Choose recorded run")},text={androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=320.dp)){val candidates=state.activities.filter{a->a.plannedWorkoutId()==null}.sortedByDescending{it.start()}.take(100);if(candidates.isEmpty())item{Text("No unlinked activities. Import or record a run first.")};items(candidates.size){i->val a=candidates[i];TextButton(onClick={model.linkRun(a.id(),version.id(),workout.id());linking=false;openActivity(a.id())}){Text("${displayActivityDate(a)} · ${a.distanceMetres()?.let{formatDistance(it,state)}?:"Distance unavailable"}")}}}},confirmButton={TextButton(onClick={linking=false}){Text("Cancel")}})

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
            try { model.completeRun(version.id(), workout.id(), km.toDouble() * (if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) 1.609344 else 1.0), PaceCalculator.parseTime(duration), LocalDate.parse(date), LocalTime.parse(time),
                rpe.takeIf { it.isNotBlank() }?.toDouble(), notes, done); error=null }
            catch (e: Exception) { error=e.message ?: "Check actual values." }
        }, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Save actual run") }
    }
}

@Composable
fun CoachInput(label: String, value: String, change: (String) -> Unit, decimal: Boolean = false) {
    if(label.contains("YYYY-MM-DD")){DateInput(label.substringBefore(" ("),value,change);return}
    OutlinedTextField(value, change, label={Text(label)}, modifier=Modifier.fillMaxWidth(),
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Text), singleLine=true)
}

@Composable
fun AdjustmentCard(proposal: AdjustmentProposal, busy: Boolean, decide: (String) -> Unit) = CoachCard {
    Text("Plan adjustment suggested", style = MaterialTheme.typography.titleMedium)
    Text(proposal.reason())
    Text(proposal.impact())
    Text(proposal.confidence())
    proposal.options().forEach { option ->
        OutlinedButton(onClick = { decide(option.id()) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(option.label()) }
    }
}
