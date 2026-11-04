package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.strength.ExerciseCatalog

/** Strength-only Today. Every action is supplied by the feature navigation graph. */
@Composable
internal fun StrengthTodayScreen(
    state: CoachUiState,
    open: (String) -> Unit,
    gymNow: (() -> Unit)? = null,
) {
    val today = LocalDate.now()
    // Production snapshots prepare this projection on IO, including PB/history analysis.
    // The fallback also supports isolated previews with explicit, in-memory observations.
    val summary =
        state.strengthToday
            ?: remember(
                state.gymProgramme,
                state.authoredGymProgramme,
                state.strengthSessions,
                state.gymLinks,
                today,
            ) {
                val links =
                    state.gymLinks.values.map {
                        StrengthTodaySummary.Association(
                            it.actualSessionId(),
                            it.versionKey(),
                            it.prescribedSessionId(),
                        )
                    }
                state.authoredGymProgramme?.let {
                    StrengthTodaySummary.fromAuthored(
                        it,
                        state.strengthSessions,
                        links,
                        today,
                        Instant.now(),
                    )
                }
                    ?: StrengthTodaySummary.from(
                        state.gymProgramme,
                        state.strengthSessions,
                        links,
                        today,
                        Instant.now(),
                    )
            }
    CoachPage {
        CoachHeading("Today")
        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)))
        SemanticCard(Tile.Strength) {
            Text("Your strength training", style = MaterialTheme.typography.titleMedium)
            val session = summary.today()
            val authored = summary.authoredToday()
            val hasProgramme = state.gymProgramme != null || state.authoredGymProgramme != null
            if (authored != null) {
                Text(authored.definition().title(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${authored.definition().slots().size} exercises · about ${authored.expectedMinutes()} min"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    authored.definition().slots().take(4).forEach { Text(it.exercise().name()) }
                    if (authored.definition().slots().size > 4)
                        Text("+ ${authored.definition().slots().size - 4} more")
                }
                if (summary.todayCompleted())
                    Text("Actual workout recorded and explicitly linked to this session.")
                else if (gymNow != null) CoachButton("Start workout", gymNow)
                TextButton(onClick = { open("gym/session/${Uri.encode(authored.id())}") }) {
                    Text("View workout details")
                }
            } else if (session == null) {
                Text("Your day, your choice", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (!hasProgramme)
                        "Build a programme or log a workout independently. No race goal is needed."
                    else "No gym session is planned today. Rest or train independently."
                )
                CoachButton(if (!hasProgramme) "Create a gym programme" else "View gym programme") {
                    open("gym")
                }
            } else {
                Text(session.title(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${session.exercises().size} exercises · up to ${state.gymProgramme!!.settings().minutes()} min"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    session.exercises().take(4).forEach {
                        Text(
                            ExerciseCatalog.definition(it.exerciseId())?.exercise()?.name()
                                ?: "Retained exercise"
                        )
                    }
                    if (session.exercises().size > 4) Text("+ ${session.exercises().size - 4} more")
                }
                if (summary.todayCompleted()) {
                    Text("Actual workout recorded and explicitly linked to this session.")
                } else {
                    if (gymNow != null) CoachButton("Start workout", gymNow)
                    CoachButton("Log today's workout") { open("strength?date=$today") }
                }
                TextButton(onClick = { open("gym/session/${Uri.encode(session.id())}") }) {
                    Text("View workout details")
                }
            }
        }
        SemanticCard(Tile.Strength) {
            Text(
                if (gymNow != null) "Gym now" else "Train independently",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Spontaneous training stays separate from your programme unless you choose to associate it."
            )
            CoachButton(if (gymNow != null) "Gym now" else "Log a spontaneous workout") {
                gymNow?.invoke() ?: open("strength/log")
            }
        }
        SemanticCard(Tile.Progress) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            if (summary.plannedThisWeek() > 0) {
                Text(
                    "${summary.completedThisWeek()} / ${summary.plannedThisWeek()} programmed sessions completed",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Text(
                "${summary.actualWorkoutsThisWeek()} actual strength ${if (summary.actualWorkoutsThisWeek() == 1) "workout" else "workouts"} recorded"
            )
            if (summary.recentPbExercises() > 0)
                Text(
                    "PB evidence in ${summary.recentPbExercises()} ${if (summary.recentPbExercises() == 1) "exercise" else "exercises"} in the past 28 days"
                )
            TextButton(onClick = { open("strength/pbs") }) { Text("PBs & progression") }
        }
        summary.recommendation()?.let { advice ->
            SemanticCard(Tile.Insight) {
                Text("Coach", style = MaterialTheme.typography.titleMedium)
                Text(
                    ExerciseCatalog.definition(advice.exerciseId())?.exercise()?.name()
                        ?: "Your recorded exercise",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(advice.reason())
                TextButton(onClick = { open("gym") }) { Text("Review programme and choices") }
            }
        }
        TextButton(onClick = { open("exercises") }) { Text("Exercises & technique") }
        TextButton(onClick = { open("history?kind=STRENGTH") }) { Text("Strength workout history") }
    }
}
