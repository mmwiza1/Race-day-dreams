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
    SharedRecoveryContextCard(r, workoutId?.let { id -> { reason -> model.runningPlan.chooseEasy(id,reason) } })
}
