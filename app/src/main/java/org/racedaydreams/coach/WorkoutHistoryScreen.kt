@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import org.racedaydreams.data.WorkoutHistoryStore
import java.time.LocalDate
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.WorkoutHistoryQuery

@Composable fun WorkoutHistoryScreen(state:CoachUiState,openRun:(String)->Unit,openStrength:(String)->Unit,initialKind:String="ALL"){
    var kind by rememberSaveable{mutableStateOf(initialKind)};var query by rememberSaveable{mutableStateOf("")};var type by rememberSaveable{mutableStateOf("")};var from by rememberSaveable{mutableStateOf("")};var through by rememberSaveable{mutableStateOf("")};var more by rememberSaveable{mutableIntStateOf(20)}
    val prescriptions=remember(state.planVersions){state.planVersions.flatMap{it.plan().workouts()}.distinctBy{it.id()}}
    val context=LocalContext.current
    var rows by remember{mutableStateOf<List<WorkoutHistoryStore.Row>>(emptyList())}
    var next by remember{mutableStateOf<WorkoutHistoryStore.Cursor?>(null)}
    var loading by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var loadedKey by remember{mutableStateOf<List<Any?>?>(null)}
    val queryKey=listOf(kind,query,type,from,through,state.activities,state.strengthSessions,state.planVersions)
    LaunchedEffect(queryKey,more) {
        val append=loadedKey==queryKey
        if(!append){rows=emptyList();next=null;loadedKey=queryKey}
        loading=true;error=null
        try {
            val result=withContext(Dispatchers.IO){WorkoutHistoryStore(context).page(WorkoutHistoryQuery.Kind.valueOf(kind),type.takeIf{it.isNotBlank()}?.let(WorkoutCategory::valueOf),query,from.takeIf{it.isNotBlank()}?.let(LocalDate::parse),through.takeIf{it.isNotBlank()}?.let(LocalDate::parse),prescriptions,if(append)next else null)}
            rows=if(append)rows+result.rows else result.rows;next=result.next
        } catch(cancelled:CancellationException){throw cancelled}
          catch(_:Exception){error="History could not be loaded. Check the date range and try again; your records are retained."}
        finally{loading=false}
    }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        CoachHeading("Workout history")
        FlowRow{WorkoutHistoryQuery.Kind.values().forEach{k->FilterChip(selected=kind==k.name,onClick={kind=k.name;more=20},label={Text(k.name.lowercase().replaceFirstChar{it.uppercase()})})}}
        var filters by rememberSaveable{mutableStateOf(false)};TextButton(onClick={filters=!filters}){Text("Search and date filters")}
        if(filters){CoachInput("Strength exercise or alias",query,{query=it;more=20});DateInput("From (optional)",from,{from=it;more=20});DateInput("Through (optional)",through,{through=it;more=20});var types by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={types=true}){Text(type.takeIf{it.isNotBlank()}?.let{workoutName(WorkoutCategory.valueOf(it))}?:"All run types")};DropdownMenu(expanded=types,onDismissRequest={types=false}){DropdownMenuItem(text={Text("All run types")},onClick={type="";types=false});WorkoutCategory.values().forEach{t->DropdownMenuItem(text={Text(workoutName(t))},onClick={type=t.name;types=false;more=20})}}};TextButton(onClick={query="";type="";from="";through=""}){Text("Clear filters")}}
        error?.let{Text(it);TextButton(onClick={more++}){Text("Try again")}}
        Text("${rows.size} workouts loaded")
        if(rows.isEmpty()&&!loading&&error==null)Text("No workouts match. Try a wider range, or record a workout.")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(rows.size,key={rows[it].kind.name+rows[it].id}) { i -> val e=rows[i]
                Card(onClick={if(e.kind==WorkoutHistoryQuery.Kind.RUNNING)openRun(e.id)else openStrength(e.id)},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)) {
                    Column(Modifier.padding(16.dp)){Text(friendlyDate(e.date));Text(e.label.replaceFirstChar{it.uppercase()});e.distanceMetres?.let{Text(formatDistance(it,state))};e.minutes?.let{Text("${e.exerciseCount} exercises · $it min")}}
                }
            }
            if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            else if(next!=null)item{TextButton(onClick={more++},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Load more")}}
        }
    }
}
