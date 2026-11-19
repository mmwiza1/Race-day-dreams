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
@Composable
fun ExerciseDetail(
    state: CoachUiState,
    model: CoachViewModel,
    id: String?,
    customMetadata: CustomStrengthExercise? = null,
    source: (String) -> Unit = {},
) = SharedExerciseDetail(
    state = state,
    id = id,
    onExerciseChoice = { exercise, favourite, archived ->
        model.strength.exerciseChoice(exercise, favourite, archived)
    },
    onExclusion = { exercise, reason -> model.exerciseExclusion(exercise, reason) },
    onSaveExercise = { model.strength.saveExercise(it) },
    onDeleteHistorical = { model.strength.deleteHistorical(it) },
    source = source,
    formatDate = ::friendlyDate,
    formatNumber = ::decimal,
    displaySet = StrengthMeasurements::display,
    technique = { TechniqueSection(it) },
    customMetadata = customMetadata,
)
