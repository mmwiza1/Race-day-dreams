package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
        TodayHeader(dateLabel)
        // One card answers what, how long and how to start; logging after the fact is secondary.
        SemanticCard(Tile.Strength) {
            Text("Your strength training", style = MaterialTheme.typography.labelLarge)
            val session = summary.today()
            val authored = summary.authoredToday()
            if (authored != null) {
                Text(authored.definition().title(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${authored.definition().slots().size} exercises · about ${authored.expectedMinutes()} min"
                )
                ExercisePreview(authored.definition().slots().map { it.exercise().name() })
                if (summary.todayCompleted())
                    Text("Actual workout recorded and explicitly linked to this session.")
                else if (gymNow != null) CoachButton("Start planned workout", gymNow)
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
                if (!hasProgramme) CoachButton("Create a gym programme") { open("gym") }
                else
                    OutlinedButton(
                        onClick = { open("gym") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("View gym programme")
                    }
            } else {
                Text(session.title(), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${session.exercises().size} exercises · up to ${requireNotNull(legacyMinutes)} min"
                )
                ExercisePreview(
                    session.exercises().map {
                        ExerciseCatalog.definition(it.exerciseId())?.exercise()?.name()
                            ?: "Retained exercise"
                    }
                )
                if (summary.todayCompleted()) {
                    Text("Actual workout recorded and explicitly linked to this session.")
                } else {
                    if (gymNow != null) CoachButton("Start planned workout", gymNow)
                    OutlinedButton(
                        onClick = { open("strength?date=$today") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Log today's workout")
                    }
                }
                TextButton(onClick = { openSession(session.id()) }) { Text("View workout details") }
            }
        }

        // Unplanned training is named as such, so it cannot be mistaken for today's session.
        Text(
            "Unplanned training",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        QuickStartRow(
            listOf(
                if (gymNow != null) "Start unplanned workout" to gymNow
                else "Log a spontaneous workout" to { open("strength/log") }
            )
        )
        Text(
            "Saved separately from your programme unless you choose to link it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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

        SemanticCard(Tile.Progress) {
            Text("This week", style = MaterialTheme.typography.titleMedium)
            if (summary.plannedThisWeek() > 0) {
                Text(
                    "${summary.completedThisWeek()} / ${summary.plannedThisWeek()} programmed sessions completed",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                "${summary.actualWorkoutsThisWeek()} actual strength ${if (summary.actualWorkoutsThisWeek() == 1) "workout" else "workouts"} recorded"
            )
            if (summary.recentPbExercises() > 0)
                Text(
                    "PB evidence in ${summary.recentPbExercises()} ${if (summary.recentPbExercises() == 1) "exercise" else "exercises"} in the past 28 days"
                )
        }

        ActionGroup("More") {
            ActionRow("PBs & progression", "Recorded bests and history") { open("strength/pbs") }
            ActionRow("Exercises & technique", "Search, favourite and learn") { open("exercises") }
            ActionRow("Strength workout history") { open("history?kind=STRENGTH") }
        }
    }
}

/** The first three exercises on one line keep the card short; the count states the rest. */
@Composable
private fun ExercisePreview(names: List<String>) {
    if (names.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        names.take(3).forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        if (names.size > 3)
            Text(
                "+ ${names.size - 3} more",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}
