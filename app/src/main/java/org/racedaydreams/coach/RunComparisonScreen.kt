package org.racedaydreams.coach
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.PaceCalculator

@Composable fun RunComparisonSection(state:CoachUiState,current:ActivityRecord){
    var expanded by rememberSaveable(current.id()){mutableStateOf(false)};var choose by rememberSaveable(current.id()){mutableStateOf(false)}
    val prescriptions=remember(state.planVersions){state.planVersions.flatMap{it.plan().workouts()}.distinctBy{it.id()}}
    val matches=remember(current,state.activities,prescriptions){ComparableRuns.find(current,state.activities,prescriptions)}
    var selected by rememberSaveable(current.id()){mutableStateOf<String?>(null)}
    val prior=state.activities.firstOrNull{it.id()==selected}?:matches.firstOrNull()?.activity()
    OutlinedButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Compare this run")}
    if(expanded){
        if(prior==null)Text("No sufficiently comparable previous run found.")
        else CoachCard{
            Text("Run comparison",style=MaterialTheme.typography.titleLarge)
            Text("Current · ${displayActivityDate(current)}");ComparisonMetrics(current,state)
            HorizontalDivider();Text("Previous · ${displayActivityDate(prior)}");ComparisonMetrics(prior,state)
            Text(ComparableRuns.interpretation(current,prior))
            val match=matches.firstOrNull{it.activity().id()==prior.id()}
            WhyDetails(match?.let{"${it.confidence()} match: ${it.reasons().joinToString("; ")}. Route matching is not assumed."}?:"You chose this comparison manually. Similarity has not been established.","Why these runs were compared")
        }
        TextButton(onClick={choose=true}){Text("Choose another run")}
    }
    if(choose)AlertDialog(onDismissRequest={choose=false},title={Text("Choose previous run")},text={androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=350.dp)){val rows=state.activities.filter{it.id()!=current.id()&&it.start()<current.start()}.sortedByDescending{it.start()};items(rows.size,key={rows[it].id()}){i->TextButton(onClick={selected=rows[i].id();choose=false}){Text("${displayActivityDate(rows[i])} · ${rows[i].distanceMetres()?.let{formatDistance(it,state)}?:"Distance unavailable"}")}}}},confirmButton={TextButton(onClick={choose=false}){Text("Cancel")}})
}
@Composable private fun ComparisonMetrics(a:ActivityRecord,s:CoachUiState){
    Text("${a.distanceMetres()?.let{formatDistance(it,s)}?:"Distance unavailable"} · ${PaceCalculator.formatTime(a.elapsedTime())}")
    if(a.distanceMetres()!=null&&a.distanceMetres()>0)Text("${formatPaceSeconds(a.elapsedTime().seconds/(a.distanceMetres()/1000)*(if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)1.609344 else 1.0))}/${if(s.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL)"mi"else "km"} average")
    Text(a.averageHeartRate()?.let{"${decimal(it)} bpm average"}?:"Heart rate unavailable")
    Text(a.rpe()?.let{"RPE ${decimal(it)}"}?:"RPE not recorded")
    val durability=LongRunDurability.analyse(a);Text(durability.explanation())
}
