package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.recording.RunMetricsEngine
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.PaceCalculator
import java.time.Duration

/** Glanceable primary controls. All state is supplied by the service, never timed here. */
@Composable fun ActiveRunPanel(s:RunMetricsEngine.Status,w:WorkoutExecutionEngine.State?,hr:Double?,cue:String?,race:String?,notice:String?,imperial:Boolean,send:(String)->Unit){
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text(if(s.paused())"PAUSED"else w?.let{if(it.complete())"Workout steps finished"else "Step ${it.index()+1} of ${it.total()}"}?:"Recording",style=MaterialTheme.typography.headlineMedium)
    if(w!=null&&!w.complete()){
        Text(w.current()?.kind()?.name?.lowercase()?.replace('_',' ')?:"Run",style=MaterialTheme.typography.titleLarge)
        Text(w.metresRemaining()?.let{if(imperial)"${decimal(it/1609.344)} mi remaining"else"${it.toInt()} m remaining"}?:w.millisRemaining()?.let{"${it/1000} sec remaining"}?:"Continue by effort",style=MaterialTheme.typography.headlineLarge)
    }
    Text(s.secondsPerKm()?.let{"${formatPaceSeconds(it*if(imperial)1.609344 else 1.0)} /${if(imperial)"mi"else"km"}"}?:"Pace unavailable",style=MaterialTheme.typography.displaySmall)
    w?.current()?.let{step->if(step.fastPace()!=null&&step.slowPace()!=null)Text("Target ${formatPaceSeconds(step.fastPace()*if(imperial)1.609344 else 1.0)}–${formatPaceSeconds(step.slowPace()*if(imperial)1.609344 else 1.0)}/${if(imperial)"mi"else"km"}")else step.rpe()?.let{Text("Effort approximately ${it.toInt()}/10")}}
    w?.current()?.let { step -> if(step.fastPace()!=null&&step.slowPace()!=null) PaceRange(step.fastPace(),step.slowPace(),if(s.paused())null else s.secondsPerKm(),imperial) }
    Text(if(imperial)"${decimal(s.metres()/1609.344)} mi"else"${decimal(s.metres()/1000)} km",style=MaterialTheme.typography.headlineMedium)
    Text("Elapsed ${PaceCalculator.formatTime(Duration.ofMillis(s.elapsedMillis()))} · Active ${PaceCalculator.formatTime(Duration.ofMillis(s.movingMillis()))}")
    hr?.let{Text("Heart rate ${it.toInt()} bpm")}
    Text(s.gps())
    notice?.let{Text(it)}
    var details by remember{mutableStateOf(false)}
    TextButton(onClick={details=!details},modifier=Modifier.heightIn(min=48.dp)){Text(if(details)"Hide details"else"Workout details / controls")}
    if(details){
        val dream by org.racedaydreams.recording.RunRecordingService.dreamRoute.collectAsState()
        dream?.let{route->var show by remember{mutableStateOf(false)};TextButton(onClick={show=!show}){Text(if(show)"Hide Dream progress"else"Dream Route progress")};if(show){Text("Equivalent distance only · not your physical location");DreamDrawing(org.racedaydreams.domain.support.DreamRoutes.forId(route),0.0,kotlin.math.min(42195.0,s.metres()))}}
        w?.current()?.let{Text(it.label())};w?.next()?.let{Text("Next: ${it.label()}")};cue?.let{Text(it)};race?.let{Text(it)};if(w!=null&&!w.complete()&&!w.early())OutlinedButton(onClick={send(if(w.current()?.metres()==null&&w.current()?.millis()==null)"COMPLETE_OPEN"else"SKIP_STEP")},modifier=Modifier.heightIn(min=56.dp)){Text(if(w.current()?.metres()==null&&w.current()?.millis()==null)"Complete open step"else"Skip current step")};Text("You can finish early. Unobserved distance and incomplete steps are never filled in.")}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Button(onClick={send(if(s.paused())"RESUME"else"PAUSE")},modifier=Modifier.weight(1f).heightIn(min=64.dp)){Text(if(s.paused())"Resume"else"Pause")}
        OutlinedButton(onClick={send("FINISH")},modifier=Modifier.weight(1f).heightIn(min=64.dp)){Text("Finish & save")}
    }
    }
}
