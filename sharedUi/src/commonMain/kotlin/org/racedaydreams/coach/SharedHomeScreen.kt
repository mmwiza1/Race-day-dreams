package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
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

    // 1. What am I doing today, and how do I start? One card, one primary action.
    SemanticCard(Tile.Planned) {
        Text(
            when {
                today == null -> "Nothing planned"
                completed -> "Done today"
                else -> "Today's run"
            },
            style = MaterialTheme.typography.labelLarge,
        )
        if (today != null) {
            Text(
                presentation.workoutName(today.category()),
                style = MaterialTheme.typography.headlineMedium,
            )
            val run =
                today.segments().firstOrNull {
                    it.kind() == org.racedaydreams.domain.WorkoutSegment.Kind.RUN
                }
            val minimumPace = run?.minimumSecondsPerKm()
            val maximumPace = run?.maximumSecondsPerKm()
            MetaLine(
                listOfNotNull(
                    today.distanceKm()?.let { "${presentation.decimal(it)} km" }
                        ?: today.duration()?.let { "${it.getSeconds() / 60} min" }
                        ?: "Run by effort",
                    run?.rpe()
                        ?.takeIf { it.isFinite() && it >= 1.0 && it <= 10.0 }
                        ?.let {
                            org.racedaydreams.domain.presentation.StrengthPrescriptionText
                                .effortDescription(it)
                                .replaceFirstChar { c -> c.uppercaseChar() } + " effort"
                        },
                    if (minimumPace != null && maximumPace != null)
                        "${presentation.paceSeconds(minimumPace)}–${presentation.paceSeconds(maximumPace)}/km"
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
            Text("Your day, your choice", style = MaterialTheme.typography.headlineMedium)
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
        val colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        val content: @Composable ColumnScope.() -> Unit = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (raceDate != null) "Race preparation" else "Current goal",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        if (raceDate != null) goal.raceName().ifBlank { goal.distance().label() }
                        else goal.distance().label(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        if (raceDate != null)
                            "${raceDate.toEpochDay() - currentDate.toEpochDay()} days · ${presentation.friendlyDate(raceDate)}"
                        else goal.mode().label(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (raceDate != null)
                    Text(
                        "›",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
            }
        }
        if (raceDate != null)
            Card(
                onClick = race,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = colors,
                content = content,
            )
        else
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = colors,
                content = content,
            )
    }

    val summary = org.racedaydreams.domain.presentation.HomeCoachingSummary.from(state.weeklyReview)
    SharedHomeCoachingCard(summary, weekly)

    if (upcoming.isNotEmpty())
        ActionGroup("Coming up") {
            upcoming.forEach { next ->
                ActionRow(
                    presentation.workoutName(next.category()),
                    presentation.shortDate(next.date()),
                ) {
                    workout(next.id())
                }
            }
        }

    val recent = state.activities.maxByOrNull { it.start() }
    ActionGroup("More") {
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
        ActionRow("Log strength", "Record a gym session you did") { quick("strength/log") }
        if (hasRoute("progress/weight")) ActionRow("Add weight") { quick("progress/weight") }
        ActionRow("Review running baseline", null, null, onClick = baseline)
        ActionRow("Not feeling 100%?", "Adjust this week safely", null, onClick = weekly)
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
