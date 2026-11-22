package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    if (week.any { it.sessions.isNotEmpty() }) {
        SectionTitle("Your next 7 days", WeekAtAGlance.summary(week))
        WeekStrip(week, currentDate)
    }

    // 1. What am I doing today, and how do I start? One hero, one primary action, three figures.
    HeroCard(Discipline.RUNNING) {
        DisciplineLabel(
            when {
                today == null -> "Nothing planned"
                completed -> "Done today"
                else -> "Today's run"
            }
        )
        if (today != null) {
            Text(
                presentation.workoutName(today.category()),
                style = MaterialTheme.typography.headlineLarge,
            )
            val run =
                today.segments().firstOrNull {
                    it.kind() == org.racedaydreams.domain.WorkoutSegment.Kind.RUN
                }
            val minimumPace = run?.minimumSecondsPerKm()
            val maximumPace = run?.maximumSecondsPerKm()
            StatRow(
                listOfNotNull(
                    today.distanceKm()?.let { Stat("Distance", presentation.decimal(it), "km") }
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
                            "${presentation.paceSeconds(minimumPace)}–${presentation.paceSeconds(maximumPace)}",
                            "/km",
                            unitGap = false,
                        )
                    else null,
                )
            )
            if (completed) Text("Completed training is retained. No catch-up mileage.")
            if (!completed && hasRoute("execute"))
                CoachButton("Start with Live Coach") {
                    quick(
                        "execute/${presentation.encodeRouteComponent(state.currentPlan!!.id())}/${presentation.encodeRouteComponent(today.id())}"
                    )
                }
            TextButton(onClick = { workout(today.id()) }) {
                Text(if (completed) "View completed workout" else "View workout details")
            }
        } else {
            Text("Your day, your choice", style = MaterialTheme.typography.headlineLarge)
            Text("Rest, or start a run or workout below.")
            upcoming.firstOrNull()?.let { next ->
                Text(
                    "Next: ${presentation.workoutName(next.category())} · ${presentation.shortDate(next.date())}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            state.gymProgramme
                ?.sessions()
                ?.firstOrNull { it.date() == currentDate }
                ?.let { gym ->
                    Text(gym.title(), style = MaterialTheme.typography.titleMedium)
                    CoachButton("View strength session") {
                        quick("gym/session/${presentation.encodeRouteComponent(gym.id())}")
                    }
                }
        }
    }

    // Hybrid days: today's strength session is secondary to the run, so its actions are tonal.
    if (today != null && hybrid) {
        state.gymProgramme
            ?.sessions()
            ?.firstOrNull { it.date() == currentDate }
            ?.let { gym ->
                SemanticCard(Tile.Strength) {
                    Text("Today's strength", style = MaterialTheme.typography.labelLarge)
                    Text(gym.title(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${gym.exercises().size} exercises · up to ${state.gymProgramme!!.settings().minutes()} min"
                    )
                    OutlinedButton(
                        onClick = { quick("strength?date=${gym.date()}") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Log today's strength workout")
                    }
                    TextButton(
                        onClick = {
                            quick("gym/session/${presentation.encodeRouteComponent(gym.id())}")
                        }
                    ) {
                        Text("View strength session")
                    }
                }
            }
    }
    if (today == null || hybrid) {
        state.authoredGymProgramme
            ?.workouts()
            ?.firstOrNull { it.date() == currentDate }
            ?.let { gym ->
                SemanticCard(Tile.Strength) {
                    Text("Today's strength", style = MaterialTheme.typography.labelLarge)
                    Text(gym.definition().title(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${gym.definition().slots().size} exercises · about ${gym.expectedMinutes()} min"
                    )
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
                        }
                    ) {
                        Text("View strength session")
                    }
                }
            }
    }

    // The next sessions follow today's directly, so the week ahead is visible without scrolling.
    if (upcoming.isNotEmpty())
        ActionGroup("Coming up", emphasised = true) {
            upcoming.forEach { next ->
                ScheduleRow(
                    WeekAtAGlance.weekdays[next.date().getDayOfWeek().getValue() - 1],
                    next.date().getDayOfMonth(),
                    Discipline.RUNNING,
                    presentation.workoutName(next.category()),
                    presentation.shortDate(next.date()),
                    next.distanceKm()?.let { "${presentation.decimal(it)} km" }
                        ?: next.duration()?.let { "${it.getSeconds() / 60} min" },
                ) {
                    workout(next.id())
                }
            }
        }

    // 2. Start something unplanned: always one row away, never another primary button.
    Text(
        "Start something else",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    QuickStartRow(
        buildList {
            add("Run now" to record)
            if (hasRoute("gym-now")) add("Gym now" to { quick("gym-now") })
        }
    )

    // 3. What do I need to know? Only shown when there is something to know.
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

    state.profile?.raceGoal()?.let { goal ->
        val raceDate = goal.raceDate()
        if (raceDate != null)
            CountdownCard(
                raceDate.toEpochDay() - currentDate.toEpochDay(),
                "Race preparation",
                goal.raceName().ifBlank { goal.distance().label() },
                presentation.friendlyDate(raceDate),
                race,
            )
        else
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Current goal", style = MaterialTheme.typography.labelLarge)
                    Text(goal.distance().label(), style = MaterialTheme.typography.titleLarge)
                    Text(goal.mode().label(), style = MaterialTheme.typography.bodyMedium)
                }
            }
    }

    val summary = org.racedaydreams.domain.presentation.HomeCoachingSummary.from(state.weeklyReview)
    SharedHomeCoachingCard(summary, weekly)

    val recent = state.activities.maxByOrNull { it.start() }
    // Secondary destinations in two purpose groups instead of one long list; still one tap each.
    ActionGroup("Your training") {
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
    ActionGroup("Log and adjust") {
        ActionRow("Log strength", "Record a gym session you did") { quick("strength/log") }
        if (hasRoute("progress/weight")) ActionRow("Add weight") { quick("progress/weight") }
        ActionRow("Not feeling 100%?", "Adjust this week safely", null, onClick = weekly)
        ActionRow("Review running baseline", null, null, onClick = baseline)
    }
}

@Composable
internal fun TodayHeader(dateLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CoachHeading("Today")
        Text(
            dateLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
