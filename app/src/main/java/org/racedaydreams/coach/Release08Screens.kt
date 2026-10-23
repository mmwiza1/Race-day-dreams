package org.racedaydreams.coach
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.json.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.support.DreamRoutes
import java.time.*

@Composable fun SpontaneousAssociation(state:CoachUiState,model:CoachViewModel,activity:ActivityRecord){
    if(activity.source().provider()!="rdd")return
    val context=LocalContext.current
    val origin by produceState<JSONObject?>(null,activity.id()){value=withContext(Dispatchers.IO){Release08Store(context).get("origin",activity.source().recordId())}}
    val mode=origin?.optString("mode")?:return
    Text("Recording origin: ${when(mode){"free"->"Spontaneous free run";"quick"->"Quick target run";"custom"->"Custom structured workout";"repeat"->"Repeated workout";"race"->"Race";else->"Planned programme run"}}")
    if(mode in listOf("workout","race")||state.runResponses.any{it.activityId()==activity.id()})return
    var selected by remember{mutableStateOf<org.racedaydreams.domain.PlannedWorkout?>(null)}
    val date=activity.start().atZone(ZoneId.systemDefault()).toLocalDate();val version=state.currentPlan
    val candidates=version?.plan()?.workouts()?.filter{it.date()<=LocalDate.now()&&kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(date,it.date()))<=2&&state.runResponses.none{r->r.plannedWorkoutId()==it.id()}&&state.activities.none{a->a.plannedWorkoutId()==it.id()}}?:emptyList()
    val tomorrow=version?.plan()?.workouts()?.firstOrNull{it.date()==date.plusDays(1)&&it.date()>=LocalDate.now()&&org.racedaydreams.domain.strength.GymProgrammeGenerator.demanding(it.category())}
    if(tomorrow!=null&&org.racedaydreams.domain.polish.TrainingInsights.extraRunDemand(activity))SemanticCard(Tile.Insight){Text("Review tomorrow's demand",style=MaterialTheme.typography.titleMedium);Text("You recorded an extra run before ${workoutName(tomorrow.category())}. It may feel more demanding. Your plan is unchanged.");var preview by remember{mutableStateOf(false)};TextButton(onClick={preview=true}){Text("Preview easy alternative")};if(preview)AlertDialog(onDismissRequest={preview=false},title={Text("Choose easy instead?")},text={Text("Only tomorrow's session changes to conversational effort. Its current distance/time ceiling is retained; no extra mileage is added. The original plan version is preserved.")},confirmButton={TextButton(onClick={model.chooseEasy(tomorrow.id(),"Runner chose an easy alternative after an extra recorded run");preview=false}){Text("Choose easy")}},dismissButton={TextButton(onClick={preview=false}){Text("Keep original")}})}
    if(candidates.isNotEmpty())SemanticCard(Tile.Insight){Text("Extra run or planned training?",style=MaterialTheme.typography.titleMedium);Text("Kept as an extra run unless you explicitly link it. The original prescription and actual GPS remain unchanged.");candidates.forEach{w->TextButton(onClick={selected=w}){Text("Count toward ${workoutName(w.category())} · ${friendlyDate(w.date())}")}}}
    selected?.let{w->AlertDialog(onDismissRequest={selected=null},title={Text("Link this actual run?")},text={Text("This will compare your run with the preserved ${friendlyDate(w.date())} prescription. It does not rewrite GPS or change the programme.")},confirmButton={TextButton(onClick={model.linkRun(activity.id(),version!!.id(),w.id());selected=null}){Text("Confirm link")}},dismissButton={TextButton(onClick={selected=null}){Text("Keep as extra run")}})}
}

@Composable fun DreamPreferences(){
    val context=LocalContext.current;val scope=rememberCoroutineScope();var loaded by remember{mutableStateOf(false)};var enabled by remember{mutableStateOf(true)};var cities by remember{mutableStateOf<Set<String>>(emptySet())};var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){val p=withContext(Dispatchers.IO){Release08Store(context).dreamPreferences()};enabled=p.first;cities=p.second;loaded=true}
    SemanticCard(Tile.Race){Text("Dream Routes",style=MaterialTheme.typography.titleLarge);if(loaded){CheckRow("Dream Routes",enabled){enabled=it};DreamRoutes.routes().forEach{r->CheckRow(r.name(),r.id() in cities){checked->cities=if(checked)cities+r.id()else cities-r.id()}};Text("Future assignments only. Existing run assignments never reroll. No city selected uses standard activity presentation.");CoachButton("Save Dream Route choices"){scope.launch{try{withContext(Dispatchers.IO){Release08Store(context).save("config","dream",JSONObject().put("version",1).put("type","dream").put("enabled",enabled).put("cities",JSONArray(cities.sorted())))};error="Choices saved."}catch(_:Exception){error="Choices could not be saved."}}};error?.let{Text(it)}}}
}

/** Deliberate allow-list; no routes, health values, notes, identifiers or history. */
@Composable fun DiagnosticExport(state:CoachUiState){
    val context=LocalContext.current;val scope=rememberCoroutineScope();var message by remember{mutableStateOf<String?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)scope.launch{message=try{withContext(Dispatchers.IO){val report=DiagnosticReport.build(context,state.healthAvailability.name,state.healthPermissions);context.contentResolver.openOutputStream(uri,"wt")!!.use{it.write(report.toString(2).toByteArray())}};"Diagnostic report exported."}catch(_:Exception){"Diagnostic export failed; local data is unchanged."}}}
    SemanticCard(Tile.Neutral){Text("Diagnostic report",style=MaterialTheme.typography.titleLarge);Text("App/schema versions, Android/device model and permission states only. Excludes GPS, health measurements, weight, notes and workout history. Inspect the file before sharing.");CoachButton("Export diagnostic report"){export.launch("race-day-dreams-diagnostic.json")};message?.let{Text(it)}}
}

@Composable fun StartingPath(model:CoachViewModel,choose:(String)->Unit)=CoachPage {
    CoachHeading("Make today yours")
    Text("Choose where to start. Every feature remains available later.")
    SemanticCard(Tile.Planned){Text("Run independently",style=MaterialTheme.typography.titleLarge);Text("No training programme or health access required.");CoachButton("Record my runs"){model.startingPath("record");choose("home")}}
    SemanticCard(Tile.Race){Text("A race goal",style=MaterialTheme.typography.titleLarge);CoachButton("Train for a race"){model.startingPath("race");choose("baseline")}}
    SemanticCard(Tile.Strength){Text("Integrated training",style=MaterialTheme.typography.titleLarge);Text("Begin with your running goal; strength stays optional.");CoachButton("Running + strength"){model.startingPath("hybrid");choose("baseline")}}
}

@Composable fun RaceWeekChecklist(goal:org.racedaydreams.domain.RaceGoal){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val key="${goal.raceDate()}:${goal.raceName()}";var values by remember(key){mutableStateOf<Map<String,Boolean>>(emptyMap())}
    val labels=listOf("Shoes & socks","Race number / timing chip","Phone charged","Familiar fuel / hydration plan","Kit & travel arrangements")
    LaunchedEffect(key){values=withContext(Dispatchers.IO){labels.associateWith{Release08Store(context).get("checklist","$key:$it")?.optBoolean("checked")?:false}}}
    SemanticCard(Tile.Race){Text("Race morning checklist",style=MaterialTheme.typography.titleLarge);labels.forEach{label->CheckRow(label,values[label]==true){checked->scope.launch{withContext(Dispatchers.IO){Release08Store(context).save("checklist","$key:$label",JSONObject().put("version",1).put("label",label).put("checked",checked))};values=values+(label to checked)}}};Text("Use the items relevant to your race. No extra training is required.")}
}

@Composable fun SchedulePreferences(){
    val context=LocalContext.current;val scope=rememberCoroutineScope();var minutes by remember{mutableStateOf("60")};var distribution by remember{mutableStateOf("BALANCED")};var unavailable by remember{mutableStateOf<List<String>>(emptyList())};var date by remember{mutableStateOf(LocalDate.now().toString())};var message by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){val j=withContext(Dispatchers.IO){Release08Store(context).get("config","schedule")};j?.let{minutes=it.getInt("maximumMinutes").toString();distribution=it.getString("distribution");val a=it.getJSONArray("unavailable");unavailable=(0 until a.length()).map{a.getString(it)}}}
    SemanticCard(Tile.Planned){Text("Session preferences",style=MaterialTheme.typography.titleLarge);Text("Normal run days, preferred long run and gym days remain in Edit goals. These preferences apply only when you explicitly generate/rebuild a programme.");CoachInput("Usual session budget (minutes)",minutes,{minutes=it});listOf("FEWER_LONGER" to "Fewer, longer sessions","BALANCED" to "Balanced","MORE_SHORTER" to "More, shorter sessions").forEach{(value,label)->FilterChip(selected=distribution==value,onClick={distribution=value},label={Text(label)})};DateInput("One-off unavailable date",date,{date=it});TextButton(onClick={if(date !in unavailable)unavailable=unavailable+date}){Text("Add unavailable date")};unavailable.forEach{d->TextButton(onClick={unavailable=unavailable-d}){Text("${friendlyDate(LocalDate.parse(d))} · Remove")}};Text("Race day ignores normal training weekdays, but an explicit unavailable race date is a genuine conflict. Distance-only sessions cannot promise an exact duration without suitable pace evidence; the plan explains this trade-off.");CoachButton("Save future planning preferences"){scope.launch{message=try{withContext(Dispatchers.IO){Release08Store(context).save("config","schedule",JSONObject().put("version",1).put("type","schedule").put("maximumMinutes",minutes.toInt()).put("distribution",distribution).put("unavailable",JSONArray(unavailable)))};"Saved. Existing programme is unchanged; review and rebuild explicitly from Plan."}catch(_:Exception){"Choose 15–360 minutes and valid dates."}}};message?.let{Text(it)}}
}
