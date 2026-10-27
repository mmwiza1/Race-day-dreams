package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*
import org.racedaydreams.health.HealthConnectGateway

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.homeGraph(state: CoachUiState, model: CoachViewModel, nav: NavHostController) {
    composable("welcome") { StartingPath(model){nav.navigate(it){popUpTo(nav.graph.id){inclusive=true}}} }

    composable("routines") { RoutineLibrary {nav.navigate("routine/$it")} }

    composable("routine/{id}?related={related}") { d -> RoutineScreen(d.arguments?.getString("id"),d.arguments?.getString("related")){nav.popBackStack()} }

    composable("weekly") { WeeklyReviewScreen(state,model,{nav.navigate("goals")}) }

    composable("home") { HomeScreen(state, { nav.navigate("weekly") }, { nav.navigate("baseline") }, { nav.navigate("assessment") }, { nav.navigate("history") }, { nav.navigate("plan") }, { nav.navigate("workout/${Uri.encode(it)}") }, record = {nav.navigate("run-now")},quick={nav.navigate(it)}, race={nav.navigate("race")}) }
}
