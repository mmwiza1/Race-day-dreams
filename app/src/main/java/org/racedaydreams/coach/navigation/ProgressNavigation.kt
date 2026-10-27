package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.progressGraph(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
) {
    composable("progress") { ProgressDashboard(readState()) { nav.navigate(it) } }

    listOf("pbs", "trends", "weight").forEach { section ->
        composable("progress/$section") {
            ProgressScreen(
                readState(),
                model,
                { nav.navigate("activity/${Uri.encode(it)}") },
                { nav.navigate("shoes") },
                { nav.navigate("strength") },
                section,
            )
        }
    }

    composable("shoes") { ShoesScreen(readState(), model) }
}
