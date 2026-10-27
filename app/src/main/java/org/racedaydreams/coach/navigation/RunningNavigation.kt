package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*

/** Feature-owned destinations retain the existing flat routes and back stack. */
fun NavGraphBuilder.runningGraph(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
    runSnapshot: () -> String?,
    startSpontaneous: (String?) -> Unit,
    connect: (Boolean, Boolean) -> Unit,
    providerUpdate: () -> Unit,
) {
    composable("run-now") { RunNowScreen { snapshot -> startSpontaneous(snapshot) } }

    composable("spontaneous-run") {
        RecordingScreen(
            model,
            customSnapshot = runSnapshot(),
            startWhenPermitted = true,
            onSaved = { nav.navigate("activity/${Uri.encode(it)}") },
        )
    }

    composable("record-run") {
        RecordingScreen(model, onSaved = { nav.navigate("activity/${Uri.encode(it)}") })
    }

    composable("execute/{version}/{workout}") { d ->
        RecordingScreen(
            model,
            d.arguments?.getString("version"),
            d.arguments?.getString("workout"),
            onSaved = { nav.navigate("activity/${Uri.encode(it)}") },
        )
    }

    composable("race") {
        RaceScreen(
            readState(),
            model,
            { v, w, h -> nav.navigate("race-run/${Uri.encode(v)}/${Uri.encode(w)}/$h") },
            { nav.navigate("progress/pbs") },
            { r -> nav.navigate("routine/$r") },
        )
    }

    composable("race-run/{version}/{workout}/{hilly}") { d ->
        RecordingScreen(
            model,
            d.arguments?.getString("version"),
            d.arguments?.getString("workout"),
            d.arguments?.getString("hilly") == "true",
            onSaved = { nav.navigate("activity/${Uri.encode(it)}") },
        )
    }

    composable("baseline") {
        BaselineScreen(
            readState(),
            model,
            model.health.historyPermissionAvailable(),
            connect,
            providerUpdate,
            onManual = { nav.navigate("manual") },
            onAssessment = { nav.navigate("assessment") },
            onContinue = {
                model.reviewBaseline {
                    nav.navigate(if (readState().profile == null) "goals" else "home") {
                        launchSingleTop = true
                    }
                }
            },
        )
    }

    composable("manual") { ManualBaselineScreen(readState(), model) { nav.popBackStack() } }

    composable("assessment") {
        AssessmentScreen(readState(), model) { nav.navigate("calendar") { launchSingleTop = true } }
    }

    composable("goals") {
        GoalForm(readState(), model) {
            model.cancelGoalEditing()
            if (readState().profile == null) nav.navigate("baseline")
            else nav.navigate("home") { launchSingleTop = true }
        }
    }

    composable("plan") {
        TrainingPlanScreen(
            readState(),
            model,
            { nav.navigate("workout/${Uri.encode(it)}") },
            { nav.navigate("goals") },
            {
                val gym = readState().gymProgramme?.sessions()?.firstOrNull { s -> s.date() == it }
                nav.navigate(
                    gym?.let { "gym/session/${Uri.encode(it.id())}" } ?: "strength?date=$it"
                )
            },
            { nav.navigate("routines") },
            { id, related ->
                nav.navigate("routine/${Uri.encode(id)}?related=${Uri.encode(related?:"")}")
            },
        )
    }

    composable("workout/{id}") { destination ->
        PlannedWorkoutScreen(
            readState(),
            model,
            destination.arguments?.getString("id"),
            { nav.navigate("activity/${Uri.encode(it)}") },
            { v, w -> nav.navigate("execute/${Uri.encode(v)}/${Uri.encode(w)}") },
            { r, w -> nav.navigate("routine/$r?related=${Uri.encode(w?:"")}") },
        )
    }

    composable("history?kind={kind}") { d ->
        WorkoutHistoryScreen(
            readState(),
            { nav.navigate("activity/${Uri.encode(it)}") },
            { nav.navigate("strength/source/${Uri.encode(it)}") },
            d.arguments?.getString("kind") ?: "ALL",
        )
    }

    composable("calendar") {
        ActivityCalendarScreen(
            readState(),
            model,
            { nav.navigate("workout/${Uri.encode(it)}") },
            { date ->
                val gym =
                    readState().gymProgramme?.sessions()?.firstOrNull { s -> s.date().toString() == date }
                nav.navigate(
                    gym?.let { "gym/session/${Uri.encode(it.id())}" } ?: "strength?date=$date"
                )
            },
            activity = { nav.navigate("activity/${Uri.encode(it)}") },
            allStrength = { nav.navigate("history?kind=STRENGTH") },
        )
    }

    composable("activity/{id}") { destination ->
        ActivityDetailScreen(readState(), model, destination.arguments?.getString("id"))
    }
}
