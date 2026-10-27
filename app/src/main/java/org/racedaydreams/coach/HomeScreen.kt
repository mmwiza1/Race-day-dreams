package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeScreen(state: CoachUiState, weekly: () -> Unit, baseline: () -> Unit, assessment: () -> Unit, history: () -> Unit, plan: () -> Unit, workout: (String) -> Unit, record: () -> Unit = {}, quick:(String)->Unit={}, race:()->Unit={}) = CoachPage {
    CoachHeading("Today")
    Text(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM",java.util.Locale.UK)))
    val today=state.currentPlan?.plan()?.workouts()?.firstOrNull{it.date()==java.time.LocalDate.now()}
    val completed=today!=null&&(state.runResponses.any{it.plannedWorkoutId()==today.id()}||state.activities.any{it.plannedWorkoutId()==today.id()})
    SemanticCard(Tile.Planned){
        Text(if(today==null)"Your day, your choice"else workoutName(today.category()),style=MaterialTheme.typography.headlineMedium)
        if(today!=null){Text(today.distanceKm()?.let{"${decimal(it)} km"}?:today.duration()?.let{"${it.toMinutes()} min"}?:"Run by effort");Text(if(completed)"Completed training is retained. No catch-up mileage."else "Your planned session")
            today.segments().firstOrNull{it.kind()==org.racedaydreams.domain.WorkoutSegment.Kind.RUN}?.let{segment->segment.rpe()?.let{Text("Target effort ~${decimal(it)}/10")};if(segment.minimumSecondsPerKm()!=null&&segment.maximumSecondsPerKm()!=null)Text("${formatPaceSeconds(segment.minimumSecondsPerKm())}–${formatPaceSeconds(segment.maximumSecondsPerKm())}/km")}
            if(!completed)CoachButton("Start with Live Coach"){quick("execute/${Uri.encode(state.currentPlan!!.id())}/${Uri.encode(today.id())}")}
            TextButton(onClick={workout(today.id())}){Text(if(completed)"View completed workout"else"View workout details")}
        }else{Text("No running session is planned today. Rest or record independently.");state.gymProgramme?.sessions()?.firstOrNull{it.date()==java.time.LocalDate.now()}?.let{gym->Text(gym.title());CoachButton("View strength session"){quick("gym/session/${Uri.encode(gym.id())}")}}}
    }
    SemanticCard(Tile.Planned){Text("Run now",style=MaterialTheme.typography.titleLarge);Text("Not on the plan? Record a spontaneous run anytime.");CoachButton("Run now",record)}
    val summary=org.racedaydreams.domain.presentation.HomeCoachingSummary.from(state.weeklyReview)
    SemanticCard(Tile.Progress){Text("How am I doing?",style=MaterialTheme.typography.titleMedium);Text(summary.title(),style=MaterialTheme.typography.headlineSmall);summary.observations().take(3).forEach{Text(it)};Text("Plan recommendation: ${summary.recommendation()}");TextButton(onClick=weekly){Text("Weekly coaching review")}}
    val hard=org.racedaydreams.domain.strength.StrengthAnalytics.hardLowerBodyDates(state.strengthSessions).contains(java.time.LocalDate.now().minusDays(1))
    if(hard||state.proposals.isNotEmpty())SemanticCard(Tile.Insight){Text("Anything I should know?",style=MaterialTheme.typography.titleMedium);Text(if(hard)"Hard lower-body strength was recorded yesterday. Today's demanding running may feel harder; consider effort and your own response."else"A coaching recommendation is ready. Your programme has not been changed.");TextButton(onClick=weekly){Text("Review choices")}}
    state.profile?.raceGoal()?.takeIf{it.raceDate()!=null}?.let{goal->SemanticCard(Tile.Race){Text(goal.raceName().ifBlank{goal.distance().label()},style=MaterialTheme.typography.titleLarge);Text("${java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),goal.raceDate())} days · ${friendlyDate(goal.raceDate())}");TextButton(onClick=race){Text("Race preparation")}}}
    if(state.profile?.preferences()?.strategy()==org.racedaydreams.domain.TrainingStrategy.PERFORMANCE&&!state.performanceEligible)SemanticCard(Tile.Warning){Text("Current evidence does not support extra performance-focused workload. Your saved goal and routine have not been changed.")}
    state.profile?.raceGoal()?.takeIf{it.raceDate()==null}?.let{goal->SemanticCard(Tile.Race){Text("Current goal",style=MaterialTheme.typography.titleMedium);Text(goal.distance().label(),style=MaterialTheme.typography.titleLarge);Text(goal.mode().label())}}
    CoachHeading("Quick actions")
    CoachButton("Log strength"){quick("strength/log")}
    OutlinedButton(onClick={quick("progress/weight")},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Add weight")}
    OutlinedButton(onClick=plan,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("View plan")}
    TextButton(onClick=history){Text("View activity history")}
    TextButton(onClick=baseline){Text("Review running baseline")}
    TextButton(onClick=weekly){Text("Not feeling 100%?")}
}
