package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    week: List<WeekDay> = emptyList(),
) {
    CoachPage {
        TodayHeader(dateLabel)
        if (week.any { it.sessions.isNotEmpty() }) {
            SectionTitle("Your next 7 days", WeekAtAGlance.summary(week))
            WeekStrip(week, today)
        }
        // One hero answers what, how long and how to start; logging after the fact is secondary.
        HeroCard(Discipline.STRENGTH) {
            DisciplineLabel("Your strength training")
            val session = summary.today()
            val authored = summary.authoredToday()
            if (authored != null) {
                Text(authored.definition().title(), style = MaterialTheme.typography.headlineLarge)
                StatRow(
                    listOf(
                        Stat("Exercises", "${authored.definition().slots().size}"),
                        Stat("About", "${authored.expectedMinutes()}", "min"),
                    )
                )
                ExercisePreview(authored.definition().slots().map { it.exercise().name() })
                if (summary.todayCompleted())
                    Text("Actual workout recorded and explicitly linked to this session.")
                else if (gymNow != null) CoachButton("Start planned workout", gymNow)
                TextButton(onClick = { openSession(authored.id()) }) {
                    Text("View workout details")
                }
            } else if (session == null) {
                Text("Your day, your choice", style = MaterialTheme.typography.headlineLarge)
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
                Text(session.title(), style = MaterialTheme.typography.headlineLarge)
                StatRow(
                    listOf(
                        Stat("Exercises", "${session.exercises().size}"),
                        Stat("Up to", "${requireNotNull(legacyMinutes)}", "min"),
                    )
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

        // This week as figures rather than sentences; each label names exactly what is counted.
        SectionTitle("Your week so far")
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().testTag("strength-week-figures"),
        ) {
            StatRow(
                buildList {
                    if (summary.plannedThisWeek() > 0)
                        add(
                            Stat(
                                "Programmed sessions done",
                                "${summary.completedThisWeek()} / ${summary.plannedThisWeek()}",
                            )
                        )
                    add(Stat("Workouts recorded", "${summary.actualWorkoutsThisWeek()}"))
                    if (summary.recentPbExercises() > 0)
                        add(Stat("Exercises with a PB, 28 days", "${summary.recentPbExercises()}"))
                },
                Modifier.padding(16.dp),
            )
        }

        ActionGroup("More") {
            ActionRow("PBs & progression", "Recorded bests and history") { open("strength/pbs") }
            ActionRow("Exercises & technique", "Search, favourite and learn") { open("exercises") }
            ActionRow("Strength workout history") { open("history?kind=STRENGTH") }
        }
    }
}

/** The first three exercises as a numbered list; the count states the rest. */
@Composable
private fun ExercisePreview(names: List<String>) {
    if (names.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.take(3).forEachIndexed { index, name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(24.dp).clearAndSetSemantics {},
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (names.size > 3)
            Text(
                "+ ${names.size - 3} more",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 36.dp),
            )
    }
}
