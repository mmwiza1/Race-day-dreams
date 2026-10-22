package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.coaching.*

@Composable
fun RunFeedbackSection(state: CoachUiState, model: CoachViewModel, activity: ActivityRecord) {
    val response=state.runResponses.firstOrNull{it.activityId()==activity.id()}
    val version=state.planVersions.firstOrNull { it.id()==response?.planVersionId() }
    val planned=version?.plan()?.workouts()?.firstOrNull { it.id()==response?.plannedWorkoutId() }
    val feedback by produceState<RunFeedback?>(null,activity,planned,response) {
        value=withContext(Dispatchers.Default){RunScoreEngine.analyse(planned,activity,response)}
    }
    CoachHeading(if(planned==null) "Run Analysis" else "Run Score and feedback")
    feedback?.let { result -> CoachCard {
        Text(result.score()?.let { "$it/10 — ${result.label()}" } ?: result.label(),style=MaterialTheme.typography.headlineSmall)
        Text("RPE describes how hard this felt. Run Score describes execution of the intended purpose; neither is a health or fitness-improvement score.")
        result.components().forEach { c ->
            Text("${c.name()}: ${c.fraction()?.let { "${kotlin.math.round(it*100).toInt()}% of this component" } ?: "Not available"}",style=MaterialTheme.typography.titleSmall)
            Text(c.explanation())
        }
        if(result.wentWell().isNotEmpty()){Text("What went well",style=MaterialTheme.typography.titleMedium);result.wentWell().forEach{Text(it)}}
        if(result.differed().isNotEmpty()){Text("What differed from the plan",style=MaterialTheme.typography.titleMedium);result.differed().forEach{Text(it)}}
        Text("What we learned",style=MaterialTheme.typography.titleMedium);Text(result.learned())
        Text("Next step",style=MaterialTheme.typography.titleMedium);Text(result.nextStep())
        Text("Deterministic product heuristics, rule version ${RunScoreEngine.RULE_VERSION}; not scientifically validated weights. Missing evidence is excluded.")
    } }
    if (planned != null) {
        if (state.proposals.isEmpty()) Text("No programme adjustment is currently proposed. This run remains useful evidence for future reviews.")
        else {
            Text("Review the rest of your week. These proposals use the underlying run and strength evidence, not your rounded Run Score.")
            state.proposals.forEach { proposal -> AdjustmentCard(proposal, state.busy) { model.decide(proposal, it) } }
        }
    }
    if(planned!=null){CoachCard {
        Text("Preserved planned workout · version ${version.version()}",style=MaterialTheme.typography.titleMedium)
        Text("${planned.date()} · ${planned.category().name.replace('_',' ')}")
        planned.distanceKm()?.let {Text("Planned distance: ${formatDistance(it * 1000, state)}")};planned.duration()?.let {Text("Planned duration: ${it.toMinutes()} min")}
        Text(planned.note() ?: "No recorded explanation.")
        Text("Actual results and your response remain separate from this prescription. Fitness improvement requires more evidence than completing one session.")
    }} else {
        var showLink by rememberSaveable { mutableStateOf(false) }
        if(state.currentPlan!=null){
            TextButton(onClick={showLink=!showLink},modifier=Modifier.heightIn(min=48.dp)){Text("Link to a planned workout…")}
            if(showLink) state.currentPlan.plan().workouts().filter { it.date()<=java.time.LocalDate.now() && state.activities.none { a->a.plannedWorkoutId()==it.id() } }.takeLast(14).forEach { w ->
                OutlinedButton(onClick={model.linkRun(activity.id(),state.currentPlan.id(),w.id());showLink=false},enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){
                    Text("${w.date()} · ${w.category().name.replace('_',' ')} · ${w.distanceKm()?.let { formatDistance(it * 1000, state) } ?: "timed"}")
                }
            }
        }
    }
}
