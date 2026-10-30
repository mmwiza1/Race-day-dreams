package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.racedaydreams.coach.*
import org.racedaydreams.coach.strength.*

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.strengthGraph(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
    gymModel: () -> GymNowViewModel = {
        error("A gym session owner is required for this destination.")
    },
    programmeModel: () -> StrengthProgrammeViewModel = {
        error("A programme authoring owner is required.")
    },
) {
    composable("gym-programme-builder") {
        StrengthProgrammeBuilderScreen(
            programmeModel(),
            onBack = { nav.popBackStack() },
            onActivated = {
                model.refresh()
                if (!nav.popBackStack("gym", false)) nav.navigate("gym") { launchSingleTop = true }
            },
        )
    }
    composable("gym-now") {
        GymNowScreen(
            gymModel(),
            onWorkout = { nav.navigate("gym-workout") { launchSingleTop = true } },
            onBuild = { nav.navigate("gym-workout-builder") },
            onBack = {
                model.refresh()
                nav.popBackStack()
            },
        )
    }

    composable("gym-workout") {
        GymWorkoutScreen(gymModel()) {
            model.refresh()
            if (!nav.popBackStack("gym-now", false)) nav.navigate("gym-now")
        }
    }

    composable("gym-workout-builder") {
        GymWorkoutBuilderScreen(
            gymModel(),
            onWorkout = { nav.navigate("gym-workout") },
            onBack = { nav.popBackStack() },
        )
    }

    composable(
        "strength?date={date}",
        arguments =
            listOf(
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
    ) { destination ->
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
        GymProgrammeScreen(readState(), model, author = { nav.navigate("gym-programme-builder") }) {
            nav.navigate("gym/session/${Uri.encode(it)}")
        }
    }

    composable("gym/session/{id}") { destination ->
        GymSessionScreen(
            readState(),
            model,
            destination.arguments?.getString("id"),
            log = { nav.navigate("strength?date=$it") },
            start = { nav.navigate("gym-now") },
        )
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
