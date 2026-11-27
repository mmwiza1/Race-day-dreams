package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.domain.WorkoutCategory
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.time.LocalDate

/** Real locale/zone/route presentation, supplied by the platform without changing stored data. */
class HomePresentation(
    val currentDate: () -> LocalDate,
    val dateLabel: () -> String,
    val friendlyDate: (LocalDate) -> String,
    val shortDate: (LocalDate) -> String,
    val workoutName: (WorkoutCategory) -> String,
    val decimal: (Double) -> String,
    val paceSeconds: (Double) -> String,
    val activityDate: (ActivityRecord) -> String,
    val distance: (Double) -> String,
    val encodeRouteComponent: (String) -> String,
    /** The local calendar date of an instant; lets the week strip place unplanned sessions. */
    val dateOf: ((org.racedaydreams.domain.time.Instant) -> LocalDate)? = null,
)

/**
 * The eyebrow above the race name on Home's countdown card (audit B-03). Before the race it is
 * preparation, on the day it is the race itself, and afterwards it invites the result instead of
 * counting down.
 */
internal fun homeRaceCountdownLabel(days: Long): String =
    when {
        days > 0 -> "Race preparation"
        days == 0L -> "Race day"
        else -> "Race complete · add your result"
    }

/**
 * Actual existing Home hierarchy, routes and evidence; platform effects stay required callbacks.
 */
@Composable
fun SharedHomeScreen(
    state: CoachUiState,
    presentation: HomePresentation,
    strengthContent: @Composable () -> Unit,
    weekly: () -> Unit,
    baseline: () -> Unit,
    assessment: () -> Unit,
    history: () -> Unit,
    plan: () -> Unit,
    workout: (String) -> Unit,
    record: () -> Unit,
    quick: (String) -> Unit,
    race: () -> Unit,
    hasRoute: (String) -> Boolean = { true },
) {
    if (state.primaryFocus == org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH) {
        strengthContent()
        return
    }
    RunningTodayScreen(
        state,
        presentation,
        weekly,
        baseline,
        assessment,
        history,
        plan,
        workout,
        record,
        quick,
        race,
        hasRoute,
    )
}

@Composable
private fun RunningTodayScreen(
    state: CoachUiState,
    presentation: HomePresentation,
    weekly: () -> Unit,
    baseline: () -> Unit,
    @Suppress("UNUSED_PARAMETER") assessment: () -> Unit,
    history: () -> Unit,
    @Suppress("UNUSED_PARAMETER") plan: () -> Unit,
    workout: (String) -> Unit,
    record: () -> Unit,
    quick: (String) -> Unit,
    race: () -> Unit,
    hasRoute: (String) -> Boolean,
) = CoachPage {
    val currentDate = presentation.currentDate()
    TodayHeader(presentation.dateLabel())
    val today = state.currentPlan?.plan()?.workouts()?.firstOrNull { it.date() == currentDate }
    val completed =
        today != null &&
            (state.runResponses.any { it.plannedWorkoutId() == today.id() } ||
                state.activities.any { it.plannedWorkoutId() == today.id() })
    val upcoming =
        state.currentPlan
            ?.plan()
            ?.workouts()
            .orEmpty()
            .filter { it.date().toEpochDay() > currentDate.toEpochDay() }
            .sortedBy { it.date().toEpochDay() }
            .take(2)
    val hybrid = state.primaryFocus == org.racedaydreams.domain.settings.PrimaryFocus.HYBRID
    // Forward-looking: today and the next six days. Weekly Monday–Sunday figures live elsewhere.
    val week =
        remember(state, currentDate) {
            WeekAtAGlance.nextSevenDays(
                state,
                currentDate,
                presentation.dateOf,
                presentation.workoutName,
            )
        }
    val gymToday = state.gymProgramme?.sessions()?.firstOrNull { it.date() == currentDate }
    val authoredToday =
        state.authoredGymProgramme?.workouts()?.firstOrNull { it.date() == currentDate }

    // 1. Today: one hero with the day's training, its one primary action and, on hybrid days,
    // today's strength nested inside so both disciplines read as one day.
    HeroSurface(
        Discipline.RUNNING,
        Glyph.RUN,
        when {
            today == null -> "No run planned"
            completed -> "Done today"
            else -> "Today's run"
        },
        Modifier.testTag("today-hero"),
    ) {
        if (today != null) {
            HeroTitle(presentation.workoutName(today.category()))
            val run =
                today.segments().firstOrNull {
                    it.kind() == org.racedaydreams.domain.WorkoutSegment.Kind.RUN
                }
            val minimumPace = run?.minimumSecondsPerKm()
            val maximumPace = run?.maximumSecondsPerKm()
            // Stored in km and s/km; shown in the user's distance unit (B-04).
            val imperial = DisplayUnits.imperial(state)
            val unit = DisplayUnits.distanceUnit(imperial)
            StatRow(
                listOfNotNull(
                    today.distanceKm()?.let {
                        Stat(
                            "Distance",
                            presentation.decimal(DisplayUnits.distance(it, imperial)),
                            unit,
                        )
                    }
                        ?: today.duration()?.let {
                            Stat("Duration", "${it.getSeconds() / 60}", "min")
                        }
                        ?: Stat("Target", "By effort"),
                    run?.rpe()
                        ?.takeIf { it.isFinite() && it >= 1.0 && it <= 10.0 }
                        ?.let {
                            Stat(
                                "Effort",
                                org.racedaydreams.domain.presentation.StrengthPrescriptionText
                                    .effortDescription(it)
                                    .replaceFirstChar { c -> c.uppercaseChar() },
                            )
                        },
                    if (minimumPace != null && maximumPace != null)
                        Stat(
                            "Pace",
                            "${presentation.paceSeconds(DisplayUnits.pace(minimumPace, imperial))}–${presentation.paceSeconds(DisplayUnits.pace(maximumPace, imperial))}",
                            "/$unit",
                            unitGap = false,
                        )
                    else null,
                )
            )
            if (completed)
                Text(
                    "Completed training is retained. No catch-up mileage.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            if (!completed && hasRoute("execute"))
                HeroPrimaryAction(Discipline.RUNNING, "Start with Live Coach") {
                    quick(
                        "execute/${presentation.encodeRouteComponent(state.currentPlan!!.id())}/${presentation.encodeRouteComponent(today.id())}"
                    )
                }
            HeroSecondaryActions(
                listOf(
                    (if (completed) "View completed workout" else "View workout details") to
                        {
                            workout(today.id())
                        }
                )
            )
        } else {
            HeroTitle("Your day, your choice")
            Text(
                "Rest, or start a run or workout below.",
                style = MaterialTheme.typography.bodyLarge,
            )
            upcoming.firstOrNull()?.let { next ->
                HeroSecondaryActions(
                    listOf(
                        "Next run · ${presentation.workoutName(next.category())} · ${presentation.shortDate(next.date())}" to
                            {
                                workout(next.id())
                            }
                    )
                )
            }
            gymToday?.let { gym ->
                CompanionSession(
                    Discipline.STRENGTH,
                    Glyph.DUMBBELL,
                    "Today's strength",
                    gym.title(),
                    null,
                ) {
                    CoachButton("View strength session") {
                        quick("gym/session/${presentation.encodeRouteComponent(gym.id())}")
                    }
                }
            }
        }

        // Hybrid days: today's strength sits inside the day, secondary to the run.
        if (today != null && hybrid)
            gymToday?.let { gym ->
                CompanionSession(
                    Discipline.STRENGTH,
                    Glyph.DUMBBELL,
                    "Today's strength",
                    gym.title(),
                    "${gym.exercises().size.let { if (it == 1) "1 exercise" else "$it exercises" }} · up to ${state.gymProgramme!!.settings().minutes()} min",
                ) {
                    OutlinedButton(
                        onClick = { quick("strength?date=${gym.date()}") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Log today's strength workout")
                    }
                    TextButton(
                        onClick = {
                            quick("gym/session/${presentation.encodeRouteComponent(gym.id())}")
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("View strength session")
                    }
                }
            }
        if (today == null || hybrid)
            authoredToday?.let { gym ->
                CompanionSession(
                    Discipline.STRENGTH,
                    Glyph.DUMBBELL,
                    "Today's strength",
                    gym.definition().title(),
                    "${gym.definition().slots().size.let { if (it == 1) "1 exercise" else "$it exercises" }} · about ${gym.expectedMinutes()} min",
                ) {
                    if (today == null) CoachButton("Start strength workout") { quick("gym-now") }
                    else
                        OutlinedButton(
                            onClick = { quick("gym-now") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text("Start strength workout")
                        }
                    TextButton(
                        onClick = {
                            quick("gym/session/${presentation.encodeRouteComponent(gym.id())}")
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("View strength session")
                    }
                }
            }
    }

    // 2. The days ahead: the rolling strip, then the next sessions in detail.
    if (week.any { it.sessions.isNotEmpty() }) {
        SectionLabel("Your next 7 days", Glyph.CALENDAR, ChartHue.BLUE, WeekAtAGlance.summary(week))
        WeekStrip(week, currentDate)
    }
    if (upcoming.isNotEmpty())
        ExpressiveSection(
            "Coming up",
            Glyph.RUN,
            ChartHue.BLUE,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            upcoming.forEach { next ->
                ScheduleRow(
                    WeekAtAGlance.weekdays[next.date().getDayOfWeek().getValue() - 1],
                    next.date().getDayOfMonth(),
                    Discipline.RUNNING,
                    presentation.workoutName(next.category()),
                    presentation.shortDate(next.date()),
                    // The platform distance formatter honours the user's km/mi setting (B-04).
                    next.distanceKm()?.let { presentation.distance(it * 1000) }
                        ?: next.duration()?.let { "${it.getSeconds() / 60} min" },
                ) {
                    workout(next.id())
                }
            }
        }

    // 3. The race the plan builds towards: a milestone, high enough to motivate.
    state.profile?.raceGoal()?.let { goal ->
        val raceDate = goal.raceDate()
        if (raceDate != null) {
            val days = raceDate.toEpochDay() - currentDate.toEpochDay()
            CountdownCard(
                days,
                homeRaceCountdownLabel(days),
                goal.raceName().ifBlank { goal.distance().label() },
                presentation.friendlyDate(raceDate),
                race,
            )
        } else
            Panel {
                Text("Current goal", style = MaterialTheme.typography.labelLarge)
                Text(goal.distance().label(), style = MaterialTheme.typography.titleLarge)
                Text(goal.mode().label(), style = MaterialTheme.typography.bodyMedium)
            }
    }

    // 4. Start something unplanned: always one tap away, never another primary button.
    Text(
        "Start something else",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
    QuickStartTiles(
        buildList {
            add(QuickStart("Run now", Glyph.RUN, Discipline.RUNNING, record))
            if (hasRoute("gym-now"))
                add(QuickStart("Gym now", Glyph.DUMBBELL, Discipline.STRENGTH) { quick("gym-now") })
        }
    )

    // 5. What do I need to know? Only shown when there is something to know.
    val hard =
        org.racedaydreams.domain.strength.StrengthAnalytics.hardLowerBodyDates(
                state.strengthSessions
            )
            .contains(currentDate.minusDays(1))
    if (hard || state.proposals.isNotEmpty())
        SemanticCard(Tile.Insight) {
            Text("Anything I should know?", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hard)
                    "Hard lower-body strength was recorded yesterday. Today's demanding running may feel harder; consider effort and your own response."
                else "A coaching recommendation is ready. Your programme has not been changed."
            )
            TextButton(onClick = weekly) { Text("Review choices") }
        }
    if (
        state.profile?.preferences()?.strategy() ==
            org.racedaydreams.domain.TrainingStrategy.PERFORMANCE && !state.performanceEligible
    )
        SemanticCard(Tile.Warning) {
            Text(
                "Current evidence does not support extra performance-focused workload. Your saved goal and routine have not been changed."
            )
        }

    val summary = org.racedaydreams.domain.presentation.HomeCoachingSummary.from(state.weeklyReview)
    SharedHomeCoachingCard(summary, weekly)

    val recent = state.activities.maxByOrNull { it.start() }
    // Secondary destinations in two purpose groups instead of one long list; still one tap each.
    ActionGroup("Your training", glyph = Glyph.TREND, hue = ChartHue.BLUE) {
        recent?.let {
            ActionRow(
                "Last run",
                presentation.activityDate(it),
                it.distanceMetres()?.let { metres -> presentation.distance(metres) },
            ) {
                quick("activity/${presentation.encodeRouteComponent(it.id())}")
            }
        }
        if (recent != null && hasRoute("progress/trends"))
            ActionRow("Training trends", "Distance, pace and time by week") {
                quick("progress/trends")
            }
        if (recent != null && hasRoute("achievements"))
            ActionRow("Recorded milestones") { quick("achievements") }
        ActionRow("View activity history", null, null, onClick = history)
    }
    ActionGroup("Log and adjust", glyph = Glyph.LIST, hue = ChartHue.TEAL) {
        ActionRow("Log strength", "Record a gym session you did") { quick("strength/log") }
        if (hasRoute("progress/weight")) ActionRow("Add weight") { quick("progress/weight") }
        ActionRow("Not feeling 100%?", "Adjust this week safely", null, onClick = weekly)
        ActionRow("Review running baseline", null, null, onClick = baseline)
    }
}

@Composable
internal fun TodayHeader(dateLabel: String) {
    // The date leads as a quiet eyebrow; "Today" is the large, bold page title.
    Column(
        Modifier.padding(top = 4.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            dateLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Today",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
    }
}
