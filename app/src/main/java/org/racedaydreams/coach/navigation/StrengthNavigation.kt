package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.racedaydreams.coach.*

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.strengthGraph(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
) {
    composable("strength?date={date}", arguments = listOf(navArgument("date") {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    })) { destination ->
        val date = destination.arguments?.getString("date")
        if (date != null) StrengthScreen(readState(), model, date)
        else StrengthDashboard(readState()) { nav.navigate(it) }
    }

    composable("strength/log") { StrengthScreen(readState(), model) }

    composable("strength/pbs") { StrengthScreen(readState(), model, section = "pbs") }

    composable("strength/source/{id}") { d ->
        StrengthScreen(
            readState(),
            model,
            section = "history",
            sourceId = d.arguments?.getString("id"),
        )
    }

    composable("strength/history") { StrengthScreen(readState(), model, section = "history") }

    composable("gym") {
        GymProgrammeScreen(readState(), model) { nav.navigate("gym/session/${Uri.encode(it)}") }
    }

    composable("gym/session/{id}") { destination ->
        GymSessionScreen(readState(), model, destination.arguments?.getString("id")) {
            nav.navigate("strength?date=$it")
        }
    }

    composable("exercises") {
        ExerciseBrowser(readState(), model) { nav.navigate("exercise/${Uri.encode(it)}") }
    }

    composable("exercise/{id}") { destination ->
        ExerciseDetail(readState(), model, destination.arguments?.getString("id")) {
            nav.navigate("strength/source/${Uri.encode(it)}")
        }
    }
}
