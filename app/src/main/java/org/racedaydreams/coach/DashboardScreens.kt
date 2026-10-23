@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.UnitSystem

@Composable private fun DestinationCard(title:String,summary:String,action:()->Unit){Card(onClick=action,colors=CardDefaults.cardColors(containerColor=tileColour(if(title in listOf("Log workout","Gym programme","Exercises"))Tile.Strength else Tile.Progress)),modifier=Modifier.fillMaxWidth().heightIn(min=80.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(title,style=MaterialTheme.typography.titleLarge);Text(summary,style=MaterialTheme.typography.bodyMedium)}}}
@Composable fun StrengthDashboard(state:CoachUiState,open:(String)->Unit)=CoachPage{
    CoachHeading("Strength")
    DestinationCard("Log workout","Record what you actually did"){open("strength/log")}
    DestinationCard("Gym programme","Optional programming alongside your running"){open("gym")}
    DestinationCard("PBs & progression","Historical and workout records together"){open("strength/pbs")}
    DestinationCard("Workout history","${state.strengthSessions.size} recorded sessions"){open("history?kind=STRENGTH")}
    DestinationCard("Exercises","Search, favourite and manage your exercises"){open("exercises")}
    state.strengthSessions.maxByOrNull{it.startedAt()}?.let{CoachCard{Text("Most recent workout",style=MaterialTheme.typography.titleSmall);Text(friendlyDate(it.startedAt().atOffset(it.offset()).toLocalDate()));Text("${it.exercises().size} exercises · ${it.duration().toMinutes()} min")}}
}
@Composable fun ProgressDashboard(state:CoachUiState,open:(String)->Unit)=CoachPage{
    CoachHeading("Progress")
    DestinationCard("Running PBs","Your results and progression"){open("progress/pbs")}
    DestinationCard("Training trends","Running volume and workload context"){open("progress/trends")}
    DestinationCard("Body weight","Recorded measurements, without judgement"){open("progress/weight")}
    DestinationCard("Weekly coaching review","Evidence, recommendations and your decisions"){open("weekly")}
    state.weights.maxByOrNull{it.at()}?.let{CoachCard{Text("Latest weight",style=MaterialTheme.typography.titleSmall);Text("${decimal(state.massUnit.fromKilograms(it.kilograms()))} ${state.massUnit.name.lowercase()}")}}
}
fun exerciseMatches(state:CoachUiState,query:String):List<StrengthExercise>{
    val recent=state.strengthSessions.sortedByDescending{it.startedAt()}.flatMap{it.exercises().map{it.exercise().id()}}.distinct()
    return ExerciseSearch.search(state.exercises,query,state.exerciseChoices.filter{it.favourite()}.map{it.exerciseId()}.toSet(),recent,state.exerciseChoices.filter{it.archived()}.map{it.exerciseId()}.toSet())
}
@Composable fun ExerciseBrowser(state:CoachUiState,model:CoachViewModel,open:(String)->Unit){
    var query by rememberSaveable{mutableStateOf("")};var custom by rememberSaveable{mutableStateOf(false)};var name by rememberSaveable{mutableStateOf("")};var profile by rememberSaveable{mutableStateOf("LOAD_REPS")};var region by rememberSaveable{mutableStateOf("FULL")};var showHidden by rememberSaveable{mutableStateOf(false)}
    val rows=remember(state.exercises,state.exerciseChoices,state.strengthSessions,query,showHidden){exerciseMatches(if(showHidden)state.copy(exerciseChoices=state.exerciseChoices.map{org.racedaydreams.data.StrengthUserStore.Choice(it.exerciseId(),it.favourite(),false)})else state,query)}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        CoachHeading("Exercises")
        CoachInput("Search exercises or aliases",query,{query=it})
        Row{TextButton(onClick={custom=true}){Text("Add custom exercise")};FilterChip(selected=showHidden,onClick={showHidden=!showHidden},label={Text("Show archived")})}
        Text("${rows.size} exercises · favourites and recent choices appear first")
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(rows.size,key={rows[it].id()}){i->val e=rows[i];Card(onClick={open(e.id())},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp)){Column(Modifier.padding(12.dp)){Text(e.name(),style=MaterialTheme.typography.titleMedium);Text(ExerciseCatalog.definition(e.id())?.category()?:"Custom exercise")}}}}
    }
    if(custom)AlertDialog(onDismissRequest={custom=false},title={Text("Custom exercise")},text={Column(Modifier.verticalScroll(rememberScrollState())){CoachInput("Exercise name",name,{name=it});Text("Measurement");StrengthExercise.MeasurementProfile.values().forEach{p->FilterChip(selected=profile==p.name,onClick={profile=p.name},label={Text(p.name.lowercase().replace('_',' '))})};Text("Training region");Row{StrengthExercise.Region.values().forEach{v->FilterChip(selected=region==v.name,onClick={region=v.name},label={Text(v.name.lowercase())})}}}},confirmButton={TextButton(enabled=name.isNotBlank(),onClick={val p=StrengthExercise.MeasurementProfile.valueOf(profile);model.saveExercise(StrengthExercise(java.util.UUID.randomUUID().toString(),name.trim(),StrengthExercise.Region.valueOf(region),if(p.usesLoad())StrengthExercise.Mode.WEIGHTED else StrengthExercise.Mode.BODYWEIGHT,false,p));custom=false}){Text("Save")}},dismissButton={TextButton(onClick={custom=false}){Text("Cancel")}})
}
@Composable fun ExerciseDetail(state:CoachUiState,model:CoachViewModel,id:String?,source:(String)->Unit = {}){
    val e=state.exercises.firstOrNull{it.id()==id}?:return
    var section by rememberSaveable(id){mutableStateOf("Overview")};var renamed by rememberSaveable(id){mutableStateOf(e.name())}
    val choice=state.exerciseChoices.firstOrNull{it.exerciseId()==id}
    val sessions=remember(state.strengthSessions,id){state.strengthSessions.filter{s->s.exercises().any{it.exercise().id()==id}}.sortedByDescending{it.startedAt()}}
    CoachPage{
        Row(Modifier.fillMaxWidth()){Text(e.name(),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));IconButton(onClick={model.exerciseChoice(e.id(),choice?.favourite()!=true,choice?.archived()==true)}){Text(if(choice?.favourite()==true)"★"else"☆",modifier=Modifier.semantics{contentDescription=(if(choice?.favourite()==true)"Remove ${e.name()} from favourites"else"Add ${e.name()} to favourites")})}}
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Overview","How to do it","History","PBs").forEach{tab->FilterChip(selected=section==tab,onClick={section=tab},label={Text(tab)})}}
        when(section){
            "Overview"->{CoachCard{Text(ExerciseCatalog.definition(e.id())?.category()?:"Custom exercise");Text("Equipment: ${ExerciseCatalog.definition(e.id())?.equipment()?:"User defined"}");Text("Measurement: ${e.measurement().name.lowercase().replace('_',' ')}")};CoachButton(if(choice?.favourite()==true)"Unfavourite"else "Favourite"){model.exerciseChoice(e.id(),choice?.favourite()!=true,choice?.archived()==true)};TextButton(onClick={model.exerciseChoice(e.id(),choice?.favourite()==true,choice?.archived()!=true)}){Text(if(choice?.archived()==true)"Restore to My Exercises"else if(ExerciseCatalog.definition(e.id())==null)"Archive exercise"else "Hide from My Exercises")};Text("${sessions.size} recorded workouts");if(ExerciseCatalog.definition(e.id())==null){CoachInput("Custom exercise name",renamed,{renamed=it});CoachButton("Save exercise name"){model.saveExercise(StrengthExercise(e.id(),renamed.trim(),e.region(),e.mode(),e.estimateOneRepMax(),e.measurement()))}}}
            "How to do it"->TechniqueSection(e.id())
            "History"->{if(sessions.isEmpty())Text("No workouts for this exercise yet. Log a workout to start its history.");var limit by rememberSaveable{mutableIntStateOf(20)};sessions.take(limit).forEach{s->CoachCard{Text(friendlyDate(s.startedAt().atOffset(s.offset()).toLocalDate()));s.exercises().filter{it.exercise().id()==id}.forEach{it.sets().forEach{set->Text(StrengthMeasurements.display(set,state.massUnit,state.profile?.preferences()?.units()?:UnitSystem.METRIC))}}}};if(sessions.size>limit)TextButton(onClick={limit+=20}){Text("Load more")}}
            "PBs"->{val records=remember(state.strengthSessions,state.historicalStrength,id){StrengthProgression.timeline(state.strengthSessions,state.historicalStrength).filter{it.exerciseId()==id}};if(records.isEmpty())Text("No PB evidence yet. Record working sets or add a historical record from PBs & progression.");records.forEach{r->CoachCard{Text(r.category(),style=MaterialTheme.typography.titleMedium);Text(if(r.category().startsWith("Duration"))StrengthMeasurements.duration(r.value().toLong())else if(r.category().startsWith("Distance"))"${decimal(r.value())} m"else if(r.category()=="Estimated 1RM")"Estimated 1RM: ~${kotlin.math.round(state.massUnit.fromKilograms(r.value())).toInt()} ${state.massUnit.name.lowercase()}"else if(r.category()=="Bodyweight reps")"${r.reps()} reps"else "${decimal(state.massUnit.fromKilograms(r.kilograms()))} ${state.massUnit.name.lowercase()} × ${r.reps()}");Text(r.provenance());Text(r.date()?.let{(if(r.precision()==HistoricalStrengthRecord.DatePrecision.APPROXIMATE)"Around "else "")+friendlyDate(it)}?:"Date unknown · outside chronology");if(r.sessionId()!=null)TextButton(onClick={source(r.sessionId())}){Text("Edit source workout")};if(r.sessionId()==null)TextButton(onClick={model.deleteHistorical(r.id())}){Text("Delete historical PB")}}}}
        }
    }
}
