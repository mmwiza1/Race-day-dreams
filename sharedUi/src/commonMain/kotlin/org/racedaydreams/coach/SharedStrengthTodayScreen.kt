package org.racedaydreams.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.time.LocalDate

/** Existing Strength Today UI; platform supplies time, prepared evidence and navigation. */
@Composable
fun SharedStrengthTodayScreen(
    summary: StrengthTodaySummary,
    today: LocalDate,
    dateLabel: String,
    hasProgramme: Boolean,
    legacyMinutes: Int?,
    open: (String) -> Unit,
    openSession: (String) -> Unit,
    gymNow: (() -> Unit)? = null,
) {
    CoachPage {
        CoachHeading("Today")
        Text(dateLabel)
        SemanticCard(Tile.Strength) {
            Text("Your strength training", style = MaterialTheme.typography.titleMedium)
            val session = summary.today()
            val authored = summary.authoredToday()
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
                TextButton(onClick = { openSession(authored.id()) }) {
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
                    "${session.exercises().size} exercises · up to ${requireNotNull(legacyMinutes)} min"
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
                TextButton(onClick = { openSession(session.id()) }) { Text("View workout details") }
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
                advice.reason()?.let { Text(it) }
                TextButton(onClick = { open("gym") }) { Text("Review programme and choices") }
            }
        }
        TextButton(onClick = { open("exercises") }) { Text("Exercises & technique") }
        TextButton(onClick = { open("history?kind=STRENGTH") }) { Text("Strength workout history") }
    }
}
