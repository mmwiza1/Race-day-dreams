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
    requestBodyPermissions: () -> Unit = {},
) {
    composable("progress/body") {
        val body: org.racedaydreams.coach.body.BodyMeasurementsViewModel =
            androidx.lifecycle.viewmodel.compose.viewModel()
        org.racedaydreams.coach.body.BodyMeasurementsScreen(
            body,
            onBack = { nav.popBackStack() },
            onPermissions = requestBodyPermissions,
        )
    }
    composable("achievements") {
        val milestones: org.racedaydreams.coach.strength.AchievementsViewModel =
            androidx.lifecycle.viewmodel.compose.viewModel()
        org.racedaydreams.coach.strength.AchievementsScreen(
            milestones,
            onSource = { evidence ->
                nav.navigate(
                    if (
                        evidence.source() ==
                            org.racedaydreams.domain.achievements.TrainingMilestones.Source.RUN
                    )
                        "activity/${Uri.encode(evidence.recordId())}"
                    else "strength/source/${Uri.encode(evidence.recordId())}"
                )
            },
        ) {
            nav.popBackStack()
        }
    }

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
