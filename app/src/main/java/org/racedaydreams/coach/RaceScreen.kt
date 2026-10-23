package org.racedaydreams.coach

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.*
import org.racedaydreams.domain.race.*
import org.racedaydreams.domain.PaceCalculator
import java.time.*

@Composable fun RaceScreen(state:CoachUiState,model:CoachViewModel,start:(String,String,Boolean)->Unit,results:()->Unit,routine:(String)->Unit)=CoachPage {
    val goal=state.profile?.raceGoal()
    if(goal==null||goal.raceDate()==null){CoachHeading("Race Day");Text("Add a dated race goal in Settings to prepare for an event. Recording works without a race.");return@CoachPage}
    val metres=goal.distance().kilometres()*1000
    val prediction by produceState<RacePredictionEngine.Prediction?>(null,goal,state.results,state.activities){value=withContext(Dispatchers.Default){RacePredictionEngine.predict(metres,state.results,state.activities,LocalDate.now())}}
    var hilly by rememberSaveable{mutableStateOf(false)}
    CoachHeading(goal.raceName().ifBlank{goal.distance().label()})
    val days=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),goal.raceDate())
    Text(if(days>0)"$days days to race day"else if(days==0L)"Race day"else "Race date was ${-days} days ago")
    Text(friendlyDate(goal.raceDate()))
    SemanticCard(Tile.Race) {Text("Your goal",style=MaterialTheme.typography.titleMedium);Text(goal.targetTime()?.let{PaceCalculator.formatTime(it)}?:goal.mode().label(),style=MaterialTheme.typography.headlineMedium);goal.targetTime()?.let{Text("Target average ${formatPaceSeconds(it.seconds/(metres/1000))}/km")}}
    SemanticCard(Tile.Progress) {Text("Estimated capability",style=MaterialTheme.typography.titleMedium);prediction?.let{p->Text(if(p.lowSeconds()==null)"Not enough comparable evidence yet"else "${PaceCalculator.formatTime(Duration.ofSeconds(p.lowSeconds()))}–${PaceCalculator.formatTime(Duration.ofSeconds(p.highSeconds()))}");Text("Confidence: ${p.confidence()}");WhyDetails(p.reasons().joinToString("\n"),"Why this range?")};Text("Your chosen target stays unchanged. This range is not a guarantee.")}
    CheckRow("Hilly course / uncertain elevation — use effort guidance",hilly){hilly=it}
    val stages=remember(goal,hilly){RaceStrategyEngine.create(metres,goal.targetTime()?.seconds,hilly)}
    CoachHeading("Your pacing strategy")
    stages.forEach{stage->SemanticCard(Tile.Neutral){Text("${decimal(stage.from()/1000)}–${decimal(stage.to()/1000)} km");if(stage.fastPace()!=null)Text("${formatPaceSeconds(stage.fastPace())}–${formatPaceSeconds(stage.slowPace())}/km");Text(stage.guidance())}}
    val race=state.currentPlan?.plan()?.workouts()?.firstOrNull{it.category()==org.racedaydreams.domain.WorkoutCategory.RACE}
    if(race!=null&&state.currentPlan?.goalSnapshot()?.raceGoal()==goal)CoachButton("Prepare / start race"){start(state.currentPlan!!.id(),race.id(),hilly)}else Text("Create or explicitly rebuild the programme for this goal before linking Race Day. Free recording remains available from Home; no goal is changed automatically.")
    TextButton(onClick={routine(org.racedaydreams.domain.support.RunningRoutines.warmup(org.racedaydreams.domain.WorkoutCategory.RACE,goal.distance().kilometres()))}){Text("Race warm-up")}
    if(days in 0L..7L){
        RaceWeekChecklist(goal)
        SemanticCard(Tile.Planned){Text("Remaining taper sessions",style=MaterialTheme.typography.titleMedium);state.currentPlan?.plan()?.workouts()?.filter{it.date()>=LocalDate.now()&&it.date()<goal.raceDate()}?.forEach{Text("${friendlyDate(it.date())} · ${workoutName(it.category())}")}}
    }
    if(days in 0L..7L)SemanticCard(Tile.Insight){Text("Race-week preparation",style=MaterialTheme.typography.titleMedium);Text(if(days>3)"The taper prioritises arriving recovered. Avoid adding missed mileage."else "Prepare familiar kit, eat normally, hydrate appropriately and check your travel plan. Avoid last-minute experiments.")}
    WhyDetails("Shoes · socks · number · timing chip if used · phone · headphones if allowed · familiar fuel/hydration plan · weather-appropriate kit · travel arrangements. Adapt this checklist to your event.","Race-morning checklist")
    TextButton(onClick=results){Text("Add or view official result")}
}
