@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.polish.CustomWorkout
import org.racedaydreams.domain.execution.WorkoutExecutionEngine.Step
import org.racedaydreams.domain.WorkoutSegment
import org.json.*

@Composable fun RunNowScreen(start:(String?)->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var section by rememberSaveable{mutableStateOf("Overview")}
    BackHandler(enabled=section!="Overview"){section="Overview"}
    var previous by remember{mutableStateOf<List<CustomWorkout>>(emptyList())};var saved by remember{mutableStateOf<List<CustomWorkout>>(emptyList())};var title by rememberSaveable{mutableStateOf("My workout")}
    var encoded by rememberSaveable{mutableStateOf("[]")};var message by remember{mutableStateOf<String?>(null)}
    var repeatGroupEdit by rememberSaveable{mutableIntStateOf(-1)};var editGroup by rememberSaveable{mutableIntStateOf(-1)};var editStep by rememberSaveable{mutableIntStateOf(0)};var showEditor by rememberSaveable{mutableStateOf(false)}
    var kind by rememberSaveable{mutableStateOf("RUN")};var target by rememberSaveable{mutableStateOf("Distance")};var amount by rememberSaveable{mutableStateOf("800")};var intensity by rememberSaveable{mutableStateOf("None")};var fast by rememberSaveable{mutableStateOf("4:40")};var slow by rememberSaveable{mutableStateOf("4:50")};var rpe by rememberSaveable{mutableStateOf("7")};var repeat by rememberSaveable{mutableStateOf("6")};var quick by rememberSaveable{mutableStateOf("5")};var duration by rememberSaveable{mutableStateOf("30")}
    val groups=remember(encoded){val a=JSONArray(encoded);(0 until a.length()).map{val j=a.getJSONObject(it);CustomWorkout.Group(j.getInt("repeats"),SupportStore.steps(j))}}
    fun setGroups(g:List<CustomWorkout.Group>){val a=JSONArray();g.forEach{group->a.put(JSONObject().put("repeats",group.repeats()).put("steps",JSONArray().also{b->group.steps().forEach{b.put(SupportStore.step(it))}}))};encoded=a.toString()}
    fun build()=CustomWorkout(java.util.UUID.randomUUID().toString(),title.trim(),groups)
    fun begin(w:CustomWorkout,mode:String){start(Release08Store.snapshot(w,mode).toString())}
    LaunchedEffect(Unit){saved=withContext(Dispatchers.IO){Release08Store(context).workouts()};previous=withContext(Dispatchers.IO){Release08Store(context).previous()}}
    CoachPage{
        CoachHeading(if(section=="Overview")"Run now"else section)
        if(section!="Overview")TextButton(onClick={section="Overview"}){Text("Back to Run Now")}
        if(section=="Overview"){
        SemanticCard(Tile.Planned){Text("No programme required",style=MaterialTheme.typography.titleLarge);Text("Record a spontaneous run. It stays separate from planned training unless you choose to link it afterwards.");CoachButton("Free run"){start(null)}}
        SemanticCard(Tile.Neutral){Text("A simple distance or time target");CoachButton("Quick run"){section="Quick run"}}
        SemanticCard(Tile.Progress){Text("Build steps, recoveries and repeats");CoachButton("Custom workout"){section="Custom workout"}}
        SemanticCard(Tile.Achievement){CoachButton("Favourite workouts"){section="Favourite workouts"}}
        SemanticCard(Tile.Planned){CoachButton("Repeat previous workout"){section="Repeat previous workout"}}
        }
        if(section=="Quick run"){
        CoachHeading("Choose a target")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(3,5,10).forEach{km->OutlinedButton(onClick={begin(CustomWorkout("quick","$km km quick run",listOf(CustomWorkout.Group(1,listOf(Step("$km km · run by comfortable effort",WorkoutSegment.Kind.RUN,km*1000.0,null,null,null,null))))),"quick")}){Text("$km km")}}}
        CoachInput("Custom distance (km)",quick,{quick=it});TextButton(onClick={runCatching{val m=quick.toDouble()*1000;require(m in 100.0..100000.0);begin(CustomWorkout("quick","Quick distance",listOf(CustomWorkout.Group(1,listOf(Step("Distance target",WorkoutSegment.Kind.RUN,m,null,null,null,null))))),"quick")}.onFailure{message="Enter 0.1–100 km."}}){Text("Start distance target")}
        FlowRow{listOf(20,30,45,60).forEach{min->TextButton(onClick={duration=min.toString()}){Text("$min min")}}}
        CoachInput("Duration (minutes)",duration,{duration=it});TextButton(onClick={runCatching{val ms=(duration.toDouble()*60000).toLong();require(ms in 60000..21600000);begin(CustomWorkout("quick","Quick duration",listOf(CustomWorkout.Group(1,listOf(Step("Duration target",WorkoutSegment.Kind.RUN,null,ms,null,null,null))))),"quick")}.onFailure{message="Enter 1–360 minutes."}}){Text("Start duration target")}
        }
        if(section=="Custom workout"){
        CoachInput("Workout name",title,{title=it})
        groups.forEachIndexed{g,group->SemanticCard(Tile.Planned){Text(if(group.repeats()>1)"Repeat ${group.repeats()} times"else"Step ${g+1}",style=MaterialTheme.typography.titleMedium);group.steps().forEachIndexed{s,step->Text("${step.label()} · ${step.metres()?.let{"${decimal(it)} m"}?:step.millis()?.let{"${it/60000.0} min"}?:"Open · advance manually"}");TextButton(onClick={editGroup=g;editStep=s;kind=step.kind().name;target=if(step.metres()!=null)"Distance"else if(step.millis()!=null)"Time"else"Open";amount=(step.metres()?:step.millis()?.div(60000.0)?:0).toString();intensity=if(step.fastPace()!=null)"Pace"else if(step.rpe()!=null)"RPE"else"None";fast=step.fastPace()?.let(::formatPaceSeconds)?:"4:40";slow=step.slowPace()?.let(::formatPaceSeconds)?:"4:50";rpe=(step.rpe()?:7).toString();showEditor=true}){Text("Edit step ${s+1}")}}
            FlowRow{if(group.steps().size>1||group.repeats()>1)TextButton(onClick={repeat=group.repeats().toString();repeatGroupEdit=g}){Text("Edit repeats")};TextButton(enabled=g>0,onClick={val list=groups.toMutableList();java.util.Collections.swap(list,g,g-1);setGroups(list)}){Text("Move up")};TextButton(enabled=g<groups.lastIndex,onClick={val list=groups.toMutableList();java.util.Collections.swap(list,g,g+1);setGroups(list)}){Text("Move down")};TextButton(onClick={setGroups(groups.toMutableList().also{it.add(g+1,group)})}){Text("Duplicate")};TextButton(onClick={setGroups(groups.filterIndexed{i,_->i!=g})}){Text("Delete")}}}}
        OutlinedButton(onClick={editGroup=-1;kind="RUN";target="Distance";amount="800";intensity="None";showEditor=true}){Text("Add step")}
        CoachInput("Repeat count",repeat,{repeat=it});TextButton(enabled=groups.size>=2,onClick={runCatching{val count=repeat.toInt();val last=groups.takeLast(2);require(last.all{it.repeats()==1});setGroups(groups.dropLast(2)+CustomWorkout.Group(count,last.flatMap{it.steps()}))}.onFailure{message="Choose 1–100 repeats; last two groups must be single steps."}}){Text("Group last two steps as repeats")}
        if(groups.isNotEmpty()){val totals=runCatching{build().totals()}.getOrNull();totals?.let{Text("Preview: ${groups.sumOf{it.repeats()*it.steps().size}} steps · ${decimal(it.knownMetres()/1000)} known km · ${it.knownMillis()/60000} known min. Mixed/open targets do not imply an exact total duration.")};CoachButton("Start custom workout"){runCatching{begin(build(),"custom")}.onFailure{message=it.message}};OutlinedButton(onClick={runCatching{build()}.onSuccess{w->scope.launch{withContext(Dispatchers.IO){Release08Store(context).save(w)};saved=saved+w;message="Saved to favourite workouts."}}.onFailure{message=it.message}}){Text("Save favourite workout")}}
        }
        if(section=="Repeat previous workout"){if(previous.isEmpty()){Text("No completed structured recording yet. Build a custom workout or use Live Coach to retain a prescription for later.");TextButton(onClick={section="Custom workout"}){Text("Create custom workout")}};if(previous.isNotEmpty()){CoachHeading("Recent structured runs");previous.take(10).forEach{w->TextButton(onClick={begin(w,"repeat")}){Text("Repeat ${w.title()} · ${w.flattened().size} steps")}}}
        }
        if(section=="Favourite workouts"){
        if(saved.isEmpty())Text("Save a custom workout to repeat its exact structure later.")
        saved.forEach{w->SemanticCard(Tile.Progress){Text(w.title(),style=MaterialTheme.typography.titleMedium);Text("${w.flattened().size} steps");TextButton(onClick={begin(w,"repeat")}){Text("Repeat workout")};TextButton(onClick={title=w.title();setGroups(w.groups());section="Custom workout"}){Text("Use in editor")};TextButton(onClick={scope.launch{withContext(Dispatchers.IO){Release08Store(context).removeWorkout(w.id())};saved=saved.filter{it.id()!=w.id()}}}){Text("Remove favourite")}}}
        if(saved.size>=20)TextButton(onClick={scope.launch{val more=withContext(Dispatchers.IO){Release08Store(context).workouts(saved.size)};saved=(saved+more).distinctBy{it.id()};if(more.isEmpty())message="All saved workouts loaded."}}){Text("Load more saved workouts")}
        }
        message?.let{Text(it)}
    }
    if(repeatGroupEdit>=0)AlertDialog(onDismissRequest={repeatGroupEdit=-1},title={Text("Repeat group")},text={CoachInput("Repeat count (1–100)",repeat,{repeat=it})},confirmButton={TextButton(onClick={runCatching{val list=groups.toMutableList();list[repeatGroupEdit]=CustomWorkout.Group(repeat.toInt(),list[repeatGroupEdit].steps());val candidate=CustomWorkout("preview",title.ifBlank{"Workout"},list);setGroups(candidate.groups());repeatGroupEdit=-1}.onFailure{message="Choose 1–100 repeats, at most 500 executed steps."}}){Text("Save repeats")}},dismissButton={TextButton(onClick={repeatGroupEdit=-1}){Text("Cancel")}})
    if(showEditor)AlertDialog(onDismissRequest={showEditor=false},title={Text("Workout step")},text={Column(Modifier.heightIn(max=450.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
        FlowRow{listOf("WARM_UP","RUN","RECOVERY","COOL_DOWN").forEach{k->FilterChip(selected=kind==k,onClick={kind=k},label={Text(k.lowercase().replace('_',' '))})}}
        FlowRow{listOf("Distance","Time","Open").forEach{v->FilterChip(selected=target==v,onClick={target=v},label={Text(v)})}}
        if(target!="Open")CoachInput(if(target=="Distance")"Metres"else"Minutes",amount,{amount=it})
        FlowRow{listOf("None","Pace","RPE").forEach{v->FilterChip(selected=intensity==v,onClick={intensity=v},label={Text(v)})}}
        if(intensity=="Pace"){CoachInput("Fast end (m:ss/km)",fast,{fast=it});CoachInput("Slow end (m:ss/km)",slow,{slow=it})};if(intensity=="RPE")CoachInput("Target RPE",rpe,{rpe=it})
        message?.let{Text(it)}
    }},confirmButton={TextButton(onClick={runCatching{fun pace(s:String):Double{val p=s.split(":");require(p.size==2&&p[1].toInt() in 0..59);return p[0].toDouble()*60+p[1].toDouble()};val step=Step(kind.lowercase().replace('_',' '),WorkoutSegment.Kind.valueOf(kind),if(target=="Distance")amount.toDouble()else null,if(target=="Time")(amount.toDouble()*60000).toLong()else null,if(intensity=="Pace")pace(fast)else null,if(intensity=="Pace")pace(slow)else null,if(intensity=="RPE")rpe.toDouble()else null);val list=groups.toMutableList();if(editGroup<0)list.add(CustomWorkout.Group(1,listOf(step)))else{val g=list[editGroup];list[editGroup]=CustomWorkout.Group(g.repeats(),g.steps().toMutableList().also{it[editStep]=step})};setGroups(list);showEditor=false;message=null}.onFailure{message="Check target, pace range and RPE (1–10)."}}){Text("Save step")}},dismissButton={TextButton(onClick={showEditor=false}){Text("Cancel")}})
}
