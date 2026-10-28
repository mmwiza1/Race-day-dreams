package org.racedaydreams.coach
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.time.*
import org.racedaydreams.domain.health.*

/** One presentation of existing raw workload plus optional personal health context. */
@Composable fun RecoveryContextSection(state:CoachUiState,model:CoachViewModel,workoutId:String?){
    val context=LocalContext.current
    val recovery by produceState<RecoveryCoaching.Context?>(null,state.activities,state.strengthSessions,state.weeklyReview){
        value=withContext(Dispatchers.IO){
            val today=LocalDate.now();val from=today.minusDays(6);val prior=from.minusDays(28)
            val recent=state.activities.filter{it.start().atZone(ZoneId.systemDefault()).toLocalDate()>=from}
            val base=state.activities.filter{it.start().atZone(ZoneId.systemDefault()).toLocalDate() in prior..from.minusDays(1)}
            val high=base.size>=6&&recent.all{it.distanceMetres()!=null}&&base.all{it.distanceMetres()!=null}&&recent.sumOf{it.distanceMetres()?:0.0}>base.sumOf{it.distanceMetres()?:0.0}/4*1.25
            val hard=recent.count{(it.rpe()?:0.0)>=8}>=3
            val lower=state.strengthSessions.any{it.startedAt().isAfter(Instant.now().minus(Duration.ofHours(36)))&&org.racedaydreams.domain.strength.StrengthAnalytics.context(it).lowerBody()==org.racedaydreams.domain.strength.StrengthAnalytics.Involvement.HIGH}
            RecoveryCoaching.evaluate(HealthIntelligence.evaluate(org.racedaydreams.data.HealthIntelligenceStore(context).recent(today),today),high,hard,lower)
        }
    }
    val r=recovery?:return
    if(r.reviewSuggested())SemanticCard(Tile.Insight){
        Text("Consider an easier option",style=MaterialTheme.typography.titleMedium)
        Text("Several signals support reviewing today's demand. This is training context, not a diagnosis.")
        WhyDetails(r.evidence().joinToString("\n"),"Why?")
        Text("Keep the plan, run by effort, move or rest. Your goal stays unchanged.")
        if(workoutId!=null){var confirm by remember{mutableStateOf(false)};TextButton(onClick={confirm=true}){Text("Preview easy alternative")};if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Replace only this session with easy running?")},text={Text("The same distance/time ceiling is retained at conversational effort, RPE about 3. No extra mileage is added. Other sessions, your goal and recurring days stay unchanged. The original version is retained.")},confirmButton={TextButton(onClick={confirm=false;model.runningPlan.chooseEasy(workoutId,r.evidence().joinToString(" "))}){Text("Choose easy")}},dismissButton={TextButton(onClick={confirm=false}){Text("Keep original")}})}
    }
}
