package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*
import org.racedaydreams.health.HealthConnectGateway

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.strengthGraph(state: CoachUiState, model: CoachViewModel, nav: NavHostController) {
    composable("strength?date={date}") { destination -> val date=destination.arguments?.getString("date");if(date!=null)StrengthScreen(state,model,date) else StrengthDashboard(state){nav.navigate(it)} }

    composable("strength/log") { StrengthScreen(state,model) }

    composable("strength/pbs") { StrengthScreen(state,model,section="pbs") }

    composable("strength/source/{id}") { d -> StrengthScreen(state,model,section="history",sourceId=d.arguments?.getString("id")) }

    composable("strength/history") { StrengthScreen(state,model,section="history") }

    composable("gym") { GymProgrammeScreen(state,model){nav.navigate("gym/session/${Uri.encode(it)}")} }

    composable("gym/session/{id}") { destination -> GymSessionScreen(state,model,destination.arguments?.getString("id")){nav.navigate("strength?date=$it")} }

    composable("exercises") { ExerciseBrowser(state,model){nav.navigate("exercise/${Uri.encode(it)}")} }

    composable("exercise/{id}") { destination -> ExerciseDetail(state,model,destination.arguments?.getString("id")){nav.navigate("strength/source/${Uri.encode(it)}")} }
}
