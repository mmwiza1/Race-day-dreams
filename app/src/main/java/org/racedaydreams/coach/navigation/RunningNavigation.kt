package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import org.racedaydreams.coach.*
import org.racedaydreams.domain.settings.PrimaryFocus

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
        val state = readState()
        if (state.primaryFocus == PrimaryFocus.STRENGTH) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    GymProgrammeScreen(state, model) {
                        nav.navigate("gym/session/${Uri.encode(it)}")
                    }
                }
                if (state.profile != null) {
                    TextButton(
                        onClick = { nav.navigate("running-plan") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("View running programme")
                    }
                }
            }
        } else RunningPlanDestination(readState, model, nav)
    }

    // An explicit secondary destination preserves Back to the athlete's Strength-first Plan.
    composable("running-plan") { RunningPlanDestination(readState, model, nav) }

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
                    readState().gymProgramme?.sessions()?.firstOrNull { s ->
                        s.date().toString() == date
                    }
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

@Composable
private fun RunningPlanDestination(
    readState: () -> CoachUiState,
    model: CoachViewModel,
    nav: NavHostController,
) {
    TrainingPlanScreen(
        readState(),
        model,
        { nav.navigate("workout/${Uri.encode(it)}") },
        { nav.navigate("goals") },
        {
            val gym = readState().gymProgramme?.sessions()?.firstOrNull { s -> s.date() == it }
            nav.navigate(gym?.let { "gym/session/${Uri.encode(it.id())}" } ?: "strength?date=$it")
        },
        { nav.navigate("routines") },
        { id, related ->
            nav.navigate("routine/${Uri.encode(id)}?related=${Uri.encode(related?:"")}")
        },
    )
}
