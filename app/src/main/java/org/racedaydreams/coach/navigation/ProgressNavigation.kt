package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*
import org.racedaydreams.health.HealthConnectGateway

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.progressGraph(state: CoachUiState, model: CoachViewModel, nav: NavHostController) {
    composable("progress") { ProgressDashboard(state){nav.navigate(it)} }

    listOf("pbs","trends","weight").forEach { section -> composable("progress/$section") { ProgressScreen(state,model,{nav.navigate("activity/${Uri.encode(it)}")},{nav.navigate("shoes")},{nav.navigate("strength")},section) } }

    composable("shoes") { ShoesScreen(state, model) }
}
