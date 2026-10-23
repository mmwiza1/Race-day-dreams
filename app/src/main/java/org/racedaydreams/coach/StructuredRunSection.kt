package org.racedaydreams.coach
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.time.*
import org.json.JSONObject
import org.racedaydreams.data.SupportStore
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.activity.*

@Composable fun StructuredRunSection(state:CoachUiState,model:CoachViewModel,activity:ActivityRecord){
    val context=LocalContext.current
    val snapshot by produceState<JSONObject?>(null,activity.id()){value=withContext(Dispatchers.IO){SupportStore(context).get("execution",activity.source().recordId())}}
    val j=snapshot?:return
    val targets=remember(j){SupportStore.steps(j)}
    val events=j.optJSONArray("events")?:org.json.JSONArray()
    CoachCard {
        Text(if(j.getString("mode")=="race")"Race execution"else"Structured execution",style=MaterialTheme.typography.titleLarge)
        Text("Compared with the exact preserved prescription; later plan edits do not change this record.")
        if(j.optBoolean("interrupted"))Text("Recovered partial recording. Unobserved running and completion are not assumed.")
        targets.forEachIndexed{index,step->
            var start:JSONObject?=null;var end:JSONObject?=null
            for(i in 0 until events.length()){val e=events.getJSONObject(i);if(e.getInt("step")==index){if(e.getString("type")=="STEP_STARTED")start=e;if(e.getString("type") in listOf("STEP_COMPLETED","STEP_SKIPPED","WORKOUT_FINISHED_EARLY"))end=e}}
            val a=start;val b=end
            Text("${index+1}. ${step.label()}",style=MaterialTheme.typography.titleMedium)
            Text(when(b?.optString("type")){"STEP_COMPLETED"->"Completed";"STEP_SKIPPED"->"Skipped by runner";else->"Not completed in retained evidence"})
            if(a!=null&&b!=null){val dt=b.getLong("active")-a.getLong("active");val dm=b.getDouble("metres")-a.getDouble("metres");Text("${PaceCalculator.formatTime(Duration.ofMillis(dt))} active · ${decimal(dm)} m")
                if(dm>0&&activity.samples().isNotEmpty()&&activity.samples().drop(1).none{it.discontinuity()}){
                    val pace=dt/dm;Text("Step average ${formatPaceSeconds(pace)}/km")
                    if(step.fastPace()!=null&&step.slowPace()!=null)Text(if(pace in step.fastPace()..step.slowPace())"Within intended pace range"else"Outside intended pace range; conditions and effort matter")
                }else Text("Step pace needs uninterrupted detailed distance evidence.")
            }
        }
        if(j.getString("mode")=="race"){
            val official=state.results.firstOrNull{it.activityId()==activity.id()&&it.source()==ResultSource.OFFICIAL_CHIP}
            val target=if(j.has("targetSeconds")&&!j.isNull("targetSeconds"))j.getLong("targetSeconds")else null
            target?.let{Text("Preserved goal: ${PaceCalculator.formatTime(Duration.ofSeconds(it))}")}
            if(j.has("predictionLow")&&!j.isNull("predictionLow"))Text("Pre-race estimate: ${PaceCalculator.formatTime(Duration.ofSeconds(j.getLong("predictionLow")))}–${PaceCalculator.formatTime(Duration.ofSeconds(j.getLong("predictionHigh")))}")
            official?.let{result->Text("Official result: ${PaceCalculator.formatTime(result.duration())} · ${result.distance().label()}");if(target!=null)Text(if(result.duration().seconds<=target)"You achieved your goal by ${PaceCalculator.formatTime(Duration.ofSeconds(target-result.duration().seconds))}."else"Official result was ${PaceCalculator.formatTime(Duration.ofSeconds(result.duration().seconds-target))} beyond the selected target. Your GPS evidence is unchanged.")}
            var time by rememberSaveable{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)}
            if(official==null){OutlinedTextField(value=time,onValueChange={time=it},label={Text("Official chip time (h:mm:ss or m:ss)")});TextButton(onClick={runCatching{val distance=PerformanceDistance.CORE.first{ kotlin.math.abs(it.metres()-j.getDouble("goalMetres"))<1 };model.saveResult(RecordedResult(java.util.UUID.randomUUID().toString(),distance,PaceCalculator.parseTime(time),activity.start().atOffset(activity.startOffset()?:ZoneOffset.UTC).toLocalDate(),j.optString("raceName"),ResultSource.OFFICIAL_CHIP,activity.id(),"Official result entered separately from GPS evidence."))}.onFailure{error="Check the official result time."}}){Text("Save separate official result")};error?.let{Text(it)}}
        }
    }
}
