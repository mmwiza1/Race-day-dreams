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
fun TrainingPlanScreen(state: CoachUiState, model: CoachViewModel, open: (String) -> Unit, editGoal: () -> Unit) {
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
            Text("Missing or stale workload evidence starts with short conversational run/walk sessions. A short race horizon never triggers a catch-up build.")
            CoachButton("Generate my programme") { model.generatePlan() }
        } else {
            val plan = version.plan()
            val startWeek = plan.startDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val selectedWeek = currentWeek.plusWeeks(week.toLong()).coerceAtLeast(startWeek)
            Text("${version.goalSnapshot().raceGoal().distance().label()} · version ${version.version()}")
            Text("${plan.startDate()} – ${plan.endDate()}")
            Text(version.reason())
            if (state.profile != version.goalSnapshot()) Text("Your saved goal or availability has changed. This programme still uses its original snapshot. Review and explicitly rebuild when ready.")
            state.proposals.forEach { proposal -> CoachCard {
                Text("Recommendation", style = MaterialTheme.typography.titleMedium)
                Text(proposal.reason())
                Text(proposal.impact())
                Text(proposal.confidence())
                proposal.options().forEach { option -> OutlinedButton(onClick = { model.decide(proposal, option.id()) }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(option.label()) } }
            } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !overview, onClick = { overview = false }, label = { Text("Week") })
                FilterChip(selected = overview, onClick = { overview = true }, label = { Text("Programme") })
            }
            if (overview) {
                Text("Distant weeks are projections. The next few weeks are planned but adaptable; current-week sessions are concrete prescriptions.")
                plan.phases().forEach { phase -> CoachCard {
                    Text(phase.kind().name.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
                    Text("${phase.startDate()} – ${phase.endDate()}")
                    Text(phase.explanation())
                } }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { week-- }, enabled = selectedWeek > startWeek) { Text("Previous") }
                    TextButton(onClick = { week = 0 }) { Text("Current week") }
                    TextButton(onClick = { week++ }, enabled = selectedWeek.plusWeeks(1) <= plan.endDate()) { Text("Next") }
                }
                Text("Week of $selectedWeek")
                val workouts = plan.workouts().filter { it.date() >= selectedWeek && it.date() < selectedWeek.plusWeeks(1) }
                if (workouts.isEmpty()) Text("No workouts scheduled in this week.")
                workouts.forEach { workout -> WorkoutCard(workout, state.activities.any { it.plannedWorkoutId() == workout.id() }) { open(workout.id()) } }
                state.strengthSessions.filter { it.startedAt().atOffset(it.offset()).toLocalDate() in selectedWeek..selectedWeek.plusDays(6) }.forEach {
                    Text("Strength · ${it.startedAt().atOffset(it.offset()).toLocalDate()} · ${it.duration().toMinutes()} min · recorded")
                }
                if (state.profile?.preferences()?.gymDays()?.isNotEmpty() == true) Text("Preferred gym days: ${state.profile.preferences().gymDays().joinToString()}. Run + gym on the same day is allowed; these preferences are not invented workouts.")
            }
            Text("Evidence used", style = MaterialTheme.typography.titleMedium)
            Text(version.evidenceSnapshot())
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
fun WorkoutCard(workout: PlannedWorkout, completed: Boolean, open: () -> Unit) = CoachCard {
    Text("${workout.date()} · ${if (completed) "Completed" else if (workout.date() < LocalDate.now()) "Workout not completed" else "Planned"}")
    Text(workout.category().name.replace('_', ' '), style = MaterialTheme.typography.titleMedium)
    Text(workout.distanceKm()?.let { "$it km" } ?: workout.duration()?.let { "${it.toMinutes()} min" } ?: "Prescription in detail")
    TextButton(onClick = open, modifier = Modifier.heightIn(min = 48.dp)) { Text("View workout and purpose") }
}

@Composable
fun PlannedWorkoutScreen(state: CoachUiState, model: CoachViewModel, id: String?, openActivity: (String) -> Unit) {
    val version = state.currentPlan
    val workout = version?.plan()?.workouts()?.firstOrNull { it.id() == id }
    var recording by rememberSaveable { mutableStateOf(false) }
    CoachPage {
        if (workout == null) { CoachHeading("Workout unavailable"); Text("The current programme no longer contains this workout. Completed prescriptions remain linked to their preserved version.") }
        else {
            CoachHeading(workout.category().name.replace('_', ' '))
            Text("${workout.date()} · plan version ${version.version()}")
            Text(workout.distanceKm()?.let { "$it km" } ?: workout.duration()?.let { "${it.toMinutes()} min" } ?: "Runner-owned race")
            CoachHeading("Workout structure")
            workout.segments().forEachIndexed { index, segment -> CoachCard {
                Text("${index + 1}. ${segment.kind().name.replace('_',' ')}", style = MaterialTheme.typography.titleMedium)
                segment.distanceKm()?.let { Text("$it km") }; segment.duration()?.let { Text("${it.toMinutes()} min") }
                segment.rpe()?.let { Text("Intended RPE: approximately ${it.toInt()}/10") }
                if (segment.minimumSecondsPerKm() != null && segment.maximumSecondsPerKm() != null) Text("Pace: ${formatPace(segment.minimumSecondsPerKm())}–${formatPace(segment.maximumSecondsPerKm())}/km; effort takes precedence")
                Text(segment.guidance() ?: "Adjust to conditions and how you feel.")
            } }
            CoachHeading("Why this workout?")
            Text(workout.note() ?: "No explanation was recorded.")
            Text("Goal: ${version.goalSnapshot().raceGoal().distance().label()} · ${version.goalSnapshot().raceGoal().mode().label()}")
            Text("You can run a different workout, take it easy or rest. Record what actually happened; your original prescription is retained. No session is blocked by a recommendation.")
            val actual = state.activities.firstOrNull { it.plannedWorkoutId() == workout.id() }
            if (actual != null) CoachButton("View completed run and feedback") { openActivity(actual.id()) }
            else if (workout.date() <= LocalDate.now()) {
                CoachButton(if (recording) "Hide completion form" else "Record actual run") { recording = !recording }
                if (recording) CompletionForm(state, model, version, workout, openActivity)
                Text("Already imported a run? Open it in Calendar and link it to this prescription instead of creating a duplicate.")
            } else Text("Record completion after the workout takes place. Activity recording with GPS is not included in this test build.")
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
        CoachInput("Actual distance (km)", km, { km=it }, true)
        CoachInput("Actual duration (mm:ss or h:mm:ss)", duration, { duration=it })
        CoachInput("Start date (YYYY-MM-DD)", date, { date=it })
        CoachInput("Start time (HH:mm, local)", time, { time=it })
        CoachInput("RPE 0–10 (optional)", rpe, { rpe=it }, true)
        CoachInput("Notes (optional)", notes, { notes=it })
        error?.let { Text(it) }
        Button(enabled = !state.busy, onClick = {
            try { model.completeRun(version.id(), workout.id(), km.toDouble(), PaceCalculator.parseTime(duration), LocalDate.parse(date), LocalTime.parse(time),
                rpe.takeIf { it.isNotBlank() }?.toDouble(), notes, done); error=null }
            catch (e: Exception) { error=e.message ?: "Check actual values." }
        }, modifier = Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Save actual run") }
    }
}

@Composable
fun CoachInput(label: String, value: String, change: (String) -> Unit, decimal: Boolean = false) {
    OutlinedTextField(value, change, label={Text(label)}, modifier=Modifier.fillMaxWidth(),
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Text), singleLine=true)
}
private fun formatPace(seconds: Double): String { val total=kotlin.math.round(seconds).toInt();return "%d:%02d".format(total/60,total%60) }
