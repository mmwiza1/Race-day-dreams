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
import androidx.compose.ui.text.font.FontWeight
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
        // One hero answers what, how long and how to start; logging after the fact is secondary.
        HeroSurface(
            Discipline.STRENGTH,
            Glyph.DUMBBELL,
            "Your strength training",
            Modifier.testTag("today-hero"),
        ) {
            val session = summary.today()
            val authored = summary.authoredToday()
            if (authored != null) {
                HeroTitle(authored.definition().title())
                StatRow(
                    listOf(
                        Stat("Exercises", "${authored.definition().slots().size}"),
                        Stat("About", "${authored.expectedMinutes()}", "min"),
                    )
                )
                ExercisePreview(authored.definition().slots().map { it.exercise().name() })
                if (summary.todayCompleted())
                    Text("Actual workout recorded and explicitly linked to this session.")
                else if (gymNow != null)
                    HeroPrimaryAction(Discipline.STRENGTH, "Start planned workout", gymNow)
                HeroSecondaryActions(
                    listOf("View workout details" to { openSession(authored.id()) })
                )
            } else if (session == null) {
                HeroTitle("Your day, your choice")
                Text(
                    if (!hasProgramme)
                        "Build a programme or log a workout independently. No race goal is needed."
                    else "No gym session is planned today. Rest or train independently."
                )
                if (!hasProgramme)
                    HeroPrimaryAction(Discipline.STRENGTH, "Create a gym programme") { open("gym") }
                else HeroSecondaryActions(listOf("View gym programme" to { open("gym") }))
            } else {
                HeroTitle(session.title())
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
                    if (gymNow != null)
                        HeroPrimaryAction(Discipline.STRENGTH, "Start planned workout", gymNow)
                }
                HeroSecondaryActions(
                    buildList {
                        if (!summary.todayCompleted())
                            add("Log today's workout" to { open("strength?date=$today") })
                        add("View workout details" to { openSession(session.id()) })
                    }
                )
            }
        }

        if (week.any { it.sessions.isNotEmpty() }) {
            SectionLabel(
                "Your next 7 days",
                Glyph.CALENDAR,
                ChartHue.PLUM,
                WeekAtAGlance.summary(week),
            )
            WeekStrip(week, today)
        }

        Text(
            "Unplanned training",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
        )
        QuickStartTiles(
            listOf(
                if (gymNow != null)
                    QuickStart(
                        "Start unplanned workout",
                        Glyph.DUMBBELL,
                        Discipline.STRENGTH,
                        gymNow,
                    )
                else
                    QuickStart("Log a spontaneous workout", Glyph.LIST, Discipline.STRENGTH) {
                        open("strength/log")
                    }
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
        SectionLabel("Your week so far", Glyph.VOLUME, ChartHue.PLUM)
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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

        ActionGroup("More", glyph = Glyph.LIST, hue = ChartHue.PLUM) {
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
