@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.WorkoutHistoryQuery

@Composable fun WorkoutHistoryScreen(state:CoachUiState,openRun:(String)->Unit,openStrength:()->Unit){
    var kind by rememberSaveable{mutableStateOf("ALL")};var query by rememberSaveable{mutableStateOf("")};var type by rememberSaveable{mutableStateOf("")};var from by rememberSaveable{mutableStateOf("")};var through by rememberSaveable{mutableStateOf("")};var more by rememberSaveable{mutableIntStateOf(20)}
    val prescriptions=remember(state.planVersions){state.planVersions.flatMap{it.plan().workouts()}.distinctBy{it.id()}}
    val page=remember(state.activities,state.strengthSessions,prescriptions,kind,query,type,from,through,more){runCatching{WorkoutHistoryQuery.page(state.activities,state.strengthSessions,prescriptions,WorkoutHistoryQuery.Kind.valueOf(kind),type.takeIf{it.isNotBlank()}?.let(WorkoutCategory::valueOf),query,from.takeIf{it.isNotBlank()}?.let(LocalDate::parse),through.takeIf{it.isNotBlank()}?.let(LocalDate::parse),0,100)}.getOrNull()}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        CoachHeading("Workout history")
        FlowRow{WorkoutHistoryQuery.Kind.values().forEach{k->FilterChip(selected=kind==k.name,onClick={kind=k.name;more=20},label={Text(k.name.lowercase().replaceFirstChar{it.uppercase()})})}}
        var filters by rememberSaveable{mutableStateOf(false)};TextButton(onClick={filters=!filters}){Text("Search and date filters")}
        if(filters){CoachInput("Strength exercise or alias",query,{query=it;more=20});DateInput("From (optional)",from,{from=it;more=20});DateInput("Through (optional)",through,{through=it;more=20});var types by remember{mutableStateOf(false)};Box{OutlinedButton(onClick={types=true}){Text(type.takeIf{it.isNotBlank()}?.let{workoutName(WorkoutCategory.valueOf(it))}?:"All run types")};DropdownMenu(expanded=types,onDismissRequest={types=false}){DropdownMenuItem(text={Text("All run types")},onClick={type="";types=false});WorkoutCategory.values().forEach{t->DropdownMenuItem(text={Text(workoutName(t))},onClick={type=t.name;types=false;more=20})}}};TextButton(onClick={query="";type="";from="";through=""}){Text("Clear filters")}}
        if(page==null)Text("Check the selected date range.")else{Text("${page.total()} matching workouts");if(page.total()==0)Text("No workouts match. Try a wider range, or record a workout.");LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(page.entries().take(more).size,key={page.entries()[it].kind().name+page.entries()[it].id()}){i->val e=page.entries()[i];Card(onClick={if(e.run()!=null)openRun(e.id())else openStrength()},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Column(Modifier.padding(12.dp)){Text(friendlyDate(e.date()));Text(e.label().replaceFirstChar{it.uppercase()});e.run()?.distanceMetres()?.let{Text(formatDistance(it,state))};e.strength()?.let{Text("${it.exercises().size} exercises · ${it.duration().toMinutes()} min")}}}};if(page.entries().size>more)item{TextButton(onClick={more+=20}){Text("Load more")}};if(page.hasMore())item{Text("Showing the newest 100 matches. Narrow the date range to browse older workouts.")}}}
    }
}
