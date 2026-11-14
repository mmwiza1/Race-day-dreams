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
    DestinationCard("Gym now","Start a planned or spontaneous workout, with each actual set saved locally"){open("gym-now")}
    DestinationCard("Log workout","Record what you actually did"){open("strength/log")}
    DestinationCard("Gym programme","Programming for your strength goals, independently or with running"){open("gym")}
    DestinationCard("Strength trends","Working sets, primary muscle areas and recorded exercise frequency"){open("strength/analytics")}
    DestinationCard("PBs & progression","Historical and workout records together"){open("strength/pbs")}
    DestinationCard("Recorded milestones","Local recognition from genuine training, with no streaks or penalties"){open("achievements")}
    DestinationCard("Workout history","${state.strengthSessions.size} recorded sessions"){open("history?kind=STRENGTH")}
    DestinationCard("Exercises","Search, favourite and manage your exercises"){open("exercises")}
    DestinationCard("Recovery & training balance","Optional guidance from recent training; review changes before activation"){open("strength/recovery")}
    state.strengthSessions.maxByOrNull{it.startedAt()}?.let{CoachCard{Text("Most recent workout",style=MaterialTheme.typography.titleSmall);Text(friendlyDate(it.startedAt().atOffset(it.offset()).toLocalDate()));Text("${it.exercises().size} exercises · ${it.duration().toMinutes()} min")}}
}
@Composable fun ProgressDashboard(state:CoachUiState,open:(String)->Unit)=CoachPage{
    CoachHeading("Progress")
    val week = remember(state.activities) {
        org.racedaydreams.domain.activity.ActivityAnalytics.weekly(
            state.activities, java.time.LocalDate.now(), java.time.ZoneId.systemDefault(), 1,
        ).lastOrNull()
    }
    SemanticCard(Tile.Progress) {
        Text("Your recorded week", style = MaterialTheme.typography.titleMedium)
        week?.let { recorded ->
            Text("${shortDate(requireNotNull(recorded.weekStart()))} onwards · this week is in progress", style = MaterialTheme.typography.bodyMedium)
            if (recorded.activityCount() == 0) {
                Text("No runs recorded this week yet", style = MaterialTheme.typography.headlineSmall)
                Text("Your history will build as you record or import runs.")
            } else {
                Text(recorded.distanceMetres()?.let { formatDistance(it, state) } ?: "Distance unavailable", style = MaterialTheme.typography.displaySmall)
                Text("${recorded.activityCount()} recorded ${if (recorded.activityCount() == 1) "run" else "runs"}")
                if (recorded.missingDistanceCount() > 0) Text("Distance is missing from ${recorded.missingDistanceCount()} recorded runs; only known distance is shown.")
            }
        }
    }
    DestinationCard("Running PBs","Ranked retained results, dates and recorded pace"){open("progress/pbs")}
    DestinationCard("Training trends","Distance, pace, time and recorded heart rate"){open("progress/trends")}
    DestinationCard("Recorded milestones","Running and strength evidence, with optional in-app celebrations"){open("achievements")}
    DestinationCard("Body weight","Recorded measurements, without judgement"){open("progress/weight")}
    DestinationCard("Weekly coaching review","Evidence, recommendations and your decisions"){open("weekly")}
    DestinationCard("Body & protein guidance","Optional source-labelled measurements and general adult nutrition guidance"){open("progress/body")}
    state.weights.maxByOrNull{it.at()}?.let{CoachCard{Text("Latest weight",style=MaterialTheme.typography.titleSmall);Text("${decimal(state.massUnit.fromKilograms(it.kilograms()))} ${state.massUnit.name.lowercase()}")}}
}
fun exerciseMatches(state:CoachUiState,query:String):List<StrengthExercise>{
    val recent=state.strengthSessions.sortedByDescending{it.startedAt()}.flatMap{it.exercises().map{it.exercise().id()}}.distinct()
    return ExerciseSearch.search(state.exercises,query,state.exerciseChoices.filter{it.favourite()}.map{it.exerciseId()}.toSet(),recent,state.exerciseChoices.filter{it.archived()}.map{it.exerciseId()}.toSet()).filter{candidate->query.isNotBlank()||state.exerciseExclusions.none{it.exerciseId()==candidate.id()}}
}
@Composable
fun ExerciseBrowser(
    state: CoachUiState,
    model: CoachViewModel,
    createClassifiedCustom: (() -> Unit)? = null,
    customMetadata: Map<String, org.racedaydreams.domain.strength.CustomStrengthExercise> = emptyMap(),
    open: (String) -> Unit,
) {
    org.racedaydreams.coach.strength.ExerciseBrowseScreen(
        state, { model.strength.saveExercise(it) }, open,
        { model.exerciseExclusion(it, null) }, createClassifiedCustom, customMetadata,
    )
}
@Composable fun ExerciseDetail(state:CoachUiState,model:CoachViewModel,id:String?,customMetadata:org.racedaydreams.domain.strength.CustomStrengthExercise?=null,source:(String)->Unit = {}){
    val e=state.exercises.firstOrNull{it.id()==id}?:return
    var section by rememberSaveable(id){mutableStateOf("Overview")};var renamed by rememberSaveable(id){mutableStateOf(e.name())}
    val choice=state.exerciseChoices.firstOrNull{it.exerciseId()==id}
    val sessions=remember(state.strengthSessions,id){state.strengthSessions.filter{s->s.exercises().any{it.exercise().id()==id}}.sortedByDescending{it.startedAt()}}
    CoachPage{
        Row(Modifier.fillMaxWidth()){Text(e.name(),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.weight(1f));IconButton(onClick={model.strength.exerciseChoice(e.id(),choice?.favourite()!=true,choice?.archived()==true)}){Text(if(choice?.favourite()==true)"★"else"☆",modifier=Modifier.semantics{contentDescription=(if(choice?.favourite()==true)"Remove ${e.name()} from favourites"else"Add ${e.name()} to favourites")})}}
        CoachSectionTabs(listOf("Overview", "How to do it", "History", "PBs"), section) { section = it }
        if(section=="Overview") org.racedaydreams.coach.strength.ExerciseExclusionControl(e.name(),state.exerciseExclusions.any{it.exerciseId()==e.id()},state.busy){model.exerciseExclusion(e.id(),it)}
        when(section){
            "Overview"->{CoachCard{Text(ExerciseCatalog.definition(e.id())?.category()?:"Custom exercise");Text("Equipment: ${ExerciseCatalog.definition(e.id())?.equipment()?:customMetadata?.let{org.racedaydreams.coach.strength.customEquipmentDescription(it)}?:"User defined"}");Text(org.racedaydreams.domain.presentation.AthleteLanguage.measurement(e));WhyDetails(org.racedaydreams.domain.presentation.AthleteLanguage.GYM_TERMS,"Sets and repetitions explained");customMetadata?.let { org.racedaydreams.coach.strength.CustomExerciseMetadataSummary(it) }};CoachButton(if(choice?.favourite()==true)"Unfavourite"else "Favourite"){model.strength.exerciseChoice(e.id(),choice?.favourite()!=true,choice?.archived()==true)};TextButton(onClick={model.strength.exerciseChoice(e.id(),choice?.favourite()==true,choice?.archived()!=true)}){Text(if(choice?.archived()==true)"Restore to My Exercises"else if(ExerciseCatalog.definition(e.id())==null)"Archive exercise"else "Hide from My Exercises")};Text("${sessions.size} recorded workouts");if(ExerciseCatalog.definition(e.id())==null){CoachInput("Custom exercise name",renamed,{renamed=it});CoachButton("Save exercise name"){model.strength.saveExercise(StrengthExercise(e.id(),renamed.trim(),e.region(),e.mode(),e.estimateOneRepMax(),e.measurement()))}}}
            "How to do it"->TechniqueSection(e.id())
            "History"->{if(sessions.isEmpty())Text("No workouts for this exercise yet. Log a workout to start its history.");var limit by rememberSaveable{mutableIntStateOf(20)};sessions.take(limit).forEach{s->CoachCard{Text(friendlyDate(s.startedAt().atOffset(s.offset()).toLocalDate()));s.exercises().filter{it.exercise().id()==id}.forEach{it.sets().forEach{set->Text(StrengthMeasurements.display(set,state.massUnit,state.profile?.preferences()?.units()?:UnitSystem.METRIC))}}}};if(sessions.size>limit)TextButton(onClick={limit+=20}){Text("Load more")}}
            "PBs"->{val records=remember(state.strengthSessions,state.historicalStrength,id){StrengthProgression.timeline(state.strengthSessions,state.historicalStrength).filter{it.exerciseId()==id}};if(records.isEmpty())Text("No personal best recorded yet. Record your main sets or add a previous result from PBs & progression.");records.forEach{r->CoachCard{Text(org.racedaydreams.domain.presentation.AthleteLanguage.recordLabel(r.category()),style=MaterialTheme.typography.titleMedium);Text(if(requireNotNull(r.category()).startsWith("Duration"))StrengthMeasurements.duration(r.value().toLong())else if(requireNotNull(r.category()).startsWith("Distance"))"${decimal(r.value())} m"else if(r.category()=="Estimated 1RM")"Estimated maximum for one repetition: ~${kotlin.math.round(state.massUnit.fromKilograms(r.value())).toInt()} ${state.massUnit.name.lowercase()}"else if(r.category()=="Bodyweight reps")"${r.reps()} reps"else "${decimal(state.massUnit.fromKilograms(r.kilograms()))} ${state.massUnit.name.lowercase()} × ${r.reps()}");Text(requireNotNull(r.provenance()));Text(r.date()?.let{(if(r.precision()==HistoricalStrengthRecord.DatePrecision.APPROXIMATE)"Around "else "")+friendlyDate(it)}?:"Date unknown · outside chronology");if(r.sessionId()!=null)TextButton(onClick={source(requireNotNull(r.sessionId()))}){Text("Edit source workout")};if(r.sessionId()==null)TextButton(onClick={model.strength.deleteHistorical(requireNotNull(r.id()))}){Text("Delete historical PB")}}}}
        }
    }
}
