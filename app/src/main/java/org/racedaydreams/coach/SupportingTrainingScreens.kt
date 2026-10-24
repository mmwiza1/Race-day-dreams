package org.racedaydreams.coach

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.domain.support.*
import org.racedaydreams.data.SupportStore

@Composable fun RoutineLibrary(open:(String)->Unit)=CoachPage {
    CoachHeading("Movement support")
    Text("Optional short routines. Two to four short mobility sessions can be useful when they fit your week. There is no compliance target, load or missed-mobility warning.")
    RunningRoutines.all().forEach{r->SemanticCard(Tile.Planned){Text(r.title(),style=MaterialTheme.typography.titleMedium);Text("${r.minutes()} min · ${r.purpose()}");CoachButton("Start ${r.title()}"){open(r.id())}}}
    Text("General movement guidance. Keep movements comfortable, stop if they cause pain and seek qualified advice when unsure. This is not injury treatment or a recovery guarantee.")
}

@Composable fun RoutineScreen(id:String?,related:String?,done:()->Unit){
    val routine=remember(id){runCatching{RunningRoutines.get(id?:"")}.getOrNull()}
    if(routine==null){CoachPage{Text("Routine unavailable")};return}
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var index by rememberSaveable(id){mutableIntStateOf(0)};var saving by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    val movement=routine.movements()[index]
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        CoachHeading(routine.title())
        LinearProgressIndicator(progress={ (index+1f)/routine.movements().size },modifier=Modifier.fillMaxWidth().height(6.dp))
        Text("${index+1} of ${routine.movements().size}",style=MaterialTheme.typography.labelLarge)
        val art=routineArtwork(movement.id())
        if(art!=null)IllustratedMovement(art)else Text("Visual guide unavailable. Follow the written guidance or choose a familiar movement.")
        Text(movement.title(),style=MaterialTheme.typography.headlineLarge)
        Surface(color=guidanceBlue(),shape=MaterialTheme.shapes.large){Text(movement.dose(),Modifier.padding(horizontal=14.dp,vertical=8.dp),style=MaterialTheme.typography.titleMedium)}
        Text(movement.instructions(),style=MaterialTheme.typography.bodyLarge)
        GuidanceNote("Tip",movement.cue())
        GuidanceNote("Common mistake",movement.mistake(),caution=true)
        WhyDetails("Keep movements comfortable and stop if painful. General guidance, not individual assessment or treatment.","About this routine")

        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            OutlinedButton(onClick={index--},enabled=index>0,modifier=Modifier.weight(1f).heightIn(min=56.dp)){Text("Back")}
            Button(onClick={if(index+1<routine.movements().size)index++ else{saving=true;scope.launch{try{withContext(Dispatchers.IO){SupportStore(context).completeRoutine(routine.id(),related)};done()}catch(_:Exception){error="Completion could not be saved. Your training plan is unchanged."}finally{saving=false}}}},enabled=!saving,modifier=Modifier.weight(1f).heightIn(min=56.dp)){Text(if(index+1<routine.movements().size)"Next"else"Finish routine")}
        };error?.let{Text(it)}
    }
}

@Composable fun DreamRouteSection(activity:org.racedaydreams.domain.activity.ActivityRecord){
    val metres=activity.distanceMetres()?:return;if(metres<=0)return
    val context=LocalContext.current
    val assignment by produceState<DreamRoutes.Assignment?>(null,activity.id()){value=withContext(Dispatchers.IO){SupportStore(context).dream(activity.id(),metres)}}
    val a=assignment?:return;val route=DreamRoutes.forId(a.routeId())
    val progress=remember(a){org.racedaydreams.domain.presentation.DreamRouteProgress.of(a)}
    GuidanceCard(MaterialTheme.colorScheme.surface) {
        Text("Dream Route · ${route.name()}",style=MaterialTheme.typography.titleLarge)
        Text(String.format(java.util.Locale.UK,"You covered the equivalent of %.2f km of a marathon course today.",a.coveredMetres()/1000))
        DreamDrawing(route,a.startMetres(),a.startMetres()+progress.highlightedMetres())
        Text("● Section start · ◉ Section finish",style=MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
            Text("${progress.percentage()}% of a marathon",style=MaterialTheme.typography.titleLarge)
        }
        LinearProgressIndicator(progress={progress.fraction().toFloat()},modifier=Modifier.fillMaxWidth().height(10.dp))
        Text(progress.distanceLabel(),style=MaterialTheme.typography.labelLarge)
        if(progress.extraMetres()>0)Text("One full marathon equivalent plus ${decimal(progress.extraMetres()/1000)} km. Your recorded run is unchanged.")
        DreamRoutes.attribution(route).takeIf{it.isNotEmpty()}?.let{Text(it,style=MaterialTheme.typography.labelSmall)}
        WhyDetails("${DreamRoutes.provenance(route)} Your stable equivalent assignment is saved for this activity; it does not claim you ran in ${route.name()}.","About this route ⓘ")
    }

}

@Composable fun MobilitySupportCard(open:()->Unit){
    val context=LocalContext.current
    val count by produceState(0){value=withContext(Dispatchers.IO){SupportStore(context).routineCount(java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant(),mobilityOnly=true)}}
    GuidanceCard(guidanceMint()) {
        Text("Mobility this week",style=MaterialTheme.typography.titleMedium)
        Text("$count optional sessions · 2–4 short sessions suggested")
        Text("Next: Hips & glutes · 8 min",style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick=open){Text("View routines")}
    }

}
@Composable fun WarmupSupportLink(id:String,related:String,open:()->Unit){
    val context=LocalContext.current
    val completed by produceState(false,id,related){value=withContext(Dispatchers.IO){SupportStore(context).routineCount(java.time.Instant.EPOCH,id,related)>0}}
    TextButton(onClick=open){Text(if(completed)"✓ Warm-up"else"Warm-up · ${RunningRoutines.get(id).minutes()} min")}
}
