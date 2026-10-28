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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TrainingPlanScreen(state: CoachUiState, model: CoachViewModel, open: (String) -> Unit, editGoal: () -> Unit, strength: (LocalDate) -> Unit = {},routines:()->Unit={}, routine:(String,String?)->Unit={_,_->}) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var week by rememberSaveable { mutableIntStateOf(0) }
    val version = state.currentPlan
    val today = LocalDate.now()
    CoachPage {
        if (version == null) {
            CoachHeading("Plan")
            GuidanceCard { Text("Your next chapter", style=MaterialTheme.typography.titleLarge)
                Text("Create a programme from your goal and availability, or record runs without a plan.")
                CoachButton("Generate my programme") { model.runningPlan.generatePlan() }
            }
            WhyDetails("Missing or stale workload evidence starts with short conversational run/walk sessions. A short race horizon never triggers a catch-up build.")
            CoachButton("Edit goal and routine", editGoal)
        } else {
            val plan = version.plan()
            val startWeek = plan.startDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val currentWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val baseWeek = currentWeek.coerceIn(startWeek, plan.endDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            val selectedWeek = baseWeek.plusWeeks(week.toLong()).coerceIn(startWeek, plan.endDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            val totalWeeks = ChronoUnit.WEEKS.between(startWeek, plan.endDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))+1
            val weekNumber = ChronoUnit.WEEKS.between(startWeek,selectedWeek)+1
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                CoachHeading("Plan")
                Text("${version.goalSnapshot().raceGoal().distance().label()} · Week $weekNumber of $totalWeeks", style=MaterialTheme.typography.titleMedium)
            }
            val largeFont=androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.15f
            if(largeFont)Text("This week", style=MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                if(!largeFont)Text("This week",modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                TextButton(onClick={week--},enabled=selectedWeek>startWeek,modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)) { Text("‹", modifier=Modifier.semantics { contentDescription="Previous week" }) }
                Text("${selectedWeek.format(java.time.format.DateTimeFormatter.ofPattern("d MMM",java.util.Locale.UK))} – ${selectedWeek.plusDays(6).format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu",java.util.Locale.UK))}",modifier=if(largeFont)Modifier.weight(1f)else Modifier,textAlign=androidx.compose.ui.text.style.TextAlign.Center,style=MaterialTheme.typography.labelSmall)
                TextButton(onClick={week++},enabled=selectedWeek.plusWeeks(1)<=plan.endDate(),modifier=Modifier.sizeIn(minWidth=48.dp,minHeight=48.dp)) { Text("›", modifier=Modifier.semantics { contentDescription="Next week" }) }
            }
            if(week!=0)TextButton(onClick={week=0}) { Text("Current week") }
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            repeat(7) { offset ->
                val day=selectedWeek.plusDays(offset.toLong())
                val entries=calendarEntries(state,day)
                val runs=plan.workouts().filter{it.date()==day}
                // Calendar remains the authority for explicit rest, moved and skipped states.
                GuidanceCard(if(entries.any{it.category() in listOf(CalendarData.Category.EASY,CalendarData.Category.QUALITY,CalendarData.Category.LONG,CalendarData.Category.RACE)}) guidanceBlue() else MaterialTheme.colorScheme.surface,padding=12.dp,spacing=0.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.widthIn(min=36.dp)) {
                            Text(day.dayOfWeek.name.take(3),style=MaterialTheme.typography.labelSmall)
                            Text("${day.dayOfMonth}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            if(entries.isEmpty()) { Text("No session planned",style=MaterialTheme.typography.titleMedium); Text("Your day, your choice",style=MaterialTheme.typography.bodySmall) }
                            entries.forEach { entry ->
                                val workout=runs.firstOrNull{it.id()==entry.id()}
                                if(workout!=null) {
                                    TextButton(onClick={open(workout.id())},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).semantics{contentDescription="View workout and purpose";stateDescription=entry.state().name.lowercase().replace('_',' ')},shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
                                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                            Text(entry.label()+(if(entry.state()==CalendarData.State.COMPLETED)" ✓"else""),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurface)
                                            Text(workout.distanceKm()?.let{formatDistance(it*1000,state)}?:workout.duration()?.let{"${it.toMinutes()} min"}?:"Run by effort",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("›",style=MaterialTheme.typography.titleLarge)
                                    }
                                    val warm=org.racedaydreams.domain.support.RunningRoutines.warmup(workout.category(),version.goalSnapshot().raceGoal().distance().kilometres())
                                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        WarmupSupportLink(warm,workout.id()){routine(warm,workout.id())}
                                        if(workout.category()==WorkoutCategory.LONG_RUN||AdaptiveCoachingEngine.isQuality(workout.category())||workout.category()==WorkoutCategory.RACE)TextButton(onClick={routine("cool-demanding",workout.id())},contentPadding=PaddingValues(0.dp)){Text("Cool-down · 6 min",style=MaterialTheme.typography.labelMedium)}
                                    }
                                } else when {
                                    entry.category()==CalendarData.Category.REST -> {Text("Rest Day",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text("Recover and prepare for your next session.",style=MaterialTheme.typography.bodySmall)}
                                    entry.category()==CalendarData.Category.STRENGTH -> TextButton(onClick={strength(day)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {Column(Modifier.weight(1f)){Text(entry.label()+(if(entry.state()==CalendarData.State.COMPLETED)" ✓"else""),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.onSurface);Text(if(entry.state()==CalendarData.State.COMPLETED)"Recorded · View strength"else"View strength workout",style=MaterialTheme.typography.bodySmall)};Text("›")}
                                    else -> {Text(entry.label(),style=MaterialTheme.typography.titleMedium);Text(entry.state().name.lowercase().replace('_',' '),style=MaterialTheme.typography.bodySmall)}
                                }

                            }
                        }
                    }
                }
            }
            }
            MobilitySupportCard(routines)
            state.proposals.forEach { proposal -> AdjustmentCard(proposal, state.busy) { model.runningPlan.decide(proposal, it) } }
            TextButton(onClick={details=!details}) { Text(if(details)"Hide programme details"else"Programme details") }
            if(details) {
                Text("${friendlyDate(plan.startDate())} – ${friendlyDate(plan.endDate())}")
                Text("Immutable plan version ${version.version()} · ${state.planVersions.size} preserved versions")
                WhyDetails(version.reason(),"Why this plan version?")
                WhyDetails(version.evidenceSnapshot(),"Evidence used for this plan")
                if(state.profile!=version.goalSnapshot())Text("Your saved goal or availability has changed. This programme retains its original snapshot until you explicitly rebuild.")
                plan.phases().forEach { phase -> CoachCard {
                    Text(phase.kind().name.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()},style=MaterialTheme.typography.titleMedium)
                    Text("${friendlyDate(phase.startDate())} – ${friendlyDate(phase.endDate())}")
                    WhyDetails(phase.explanation())
                } }
                OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text("Rebuild remaining programme…") }
                CoachButton("Edit goal and routine", editGoal)
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Rebuild your remaining programme?")},
        text={Text("Use your current goal, availability and evidence from today onward. Future sessions may change. Previous prescriptions remain in history. No catch-up mileage is added.")},
        confirmButton={TextButton(onClick={confirm=false;model.runningPlan.generatePlan()}){Text("Rebuild")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Keep current plan")}})
}

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
            CoachCard { Text("Today’s focus",style=MaterialTheme.typography.titleMedium);Text(briefing.title());WhyDetails(briefing.observations().joinToString("\n"),"Before you run") }
            if(workout.date()>=LocalDate.now()&&state.activities.none{it.plannedWorkoutId()==workout.id()})RecoveryContextSection(state,model,if(workout.category()==WorkoutCategory.RACE)null else workout.id())
            CoachButton("Start with Live Coach"){startRun(version.id(),workout.id())}
            val warm=org.racedaydreams.domain.support.RunningRoutines.warmup(workout.category(),version.goalSnapshot().raceGoal().distance().kilometres())
            WarmupSupportLink(warm,workout.id()){routine(warm,workout.id())}
            TextButton(onClick={routine(if(workout.category()==WorkoutCategory.LONG_RUN||org.racedaydreams.domain.coaching.AdaptiveCoachingEngine.isQuality(workout.category())||workout.category()==WorkoutCategory.RACE)"cool-demanding"else"cool-easy",workout.id())}){Text("View optional cool-down")}
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
            try { model.runningEvidence.completeRun(version.id(), workout.id(), km.toDouble() * (if (state.profile?.preferences()?.units() == UnitSystem.IMPERIAL) 1.609344 else 1.0), PaceCalculator.parseTime(duration), LocalDate.parse(date), LocalTime.parse(time),
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
fun AdjustmentCard(proposal: AdjustmentProposal, busy: Boolean, decide: (String) -> Unit) = SemanticCard(Tile.Insight) {
    Text("Plan adjustment suggested", style = MaterialTheme.typography.titleMedium)
    Text(proposal.reason())
    Text(proposal.impact())
    Text(proposal.confidence())
    proposal.options().forEach { option ->
        OutlinedButton(onClick = { decide(option.id()) }, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(option.label()) }
    }
}
