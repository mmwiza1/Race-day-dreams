package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeScreen(
    state: CoachUiState,
    weekly: () -> Unit,
    baseline: () -> Unit,
    assessment: () -> Unit,
    history: () -> Unit,
    plan: () -> Unit,
    workout: (String) -> Unit,
    record: () -> Unit = {},
    quick: (String) -> Unit = {},
    race: () -> Unit = {},
) {
    if (state.primaryFocus == org.racedaydreams.domain.settings.PrimaryFocus.STRENGTH) {
        StrengthTodayScreen(state, quick, gymNow = { quick("gym-now") })
        return
    }
    RunningTodayScreen(
        state,
        weekly,
        baseline,
        assessment,
        history,
        plan,
        workout,
        record,
        quick,
        race,
    )
}

@Composable
private fun RunningTodayScreen(
    state: CoachUiState,
    weekly: () -> Unit,
    baseline: () -> Unit,
    assessment: () -> Unit,
    history: () -> Unit,
    plan: () -> Unit,
    workout: (String) -> Unit,
    record: () -> Unit,
    quick: (String) -> Unit,
    race: () -> Unit,
) = CoachPage {
    CoachHeading("Today")
    Text(
        java.time.LocalDate.now()
            .format(
                java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.UK)
            )
    )
    val today =
        state.currentPlan?.plan()?.workouts()?.firstOrNull {
            it.date() == java.time.LocalDate.now()
        }
    val completed =
        today != null &&
            (state.runResponses.any { it.plannedWorkoutId() == today.id() } ||
                state.activities.any { it.plannedWorkoutId() == today.id() })
    SemanticCard(Tile.Planned) {
        Text(
            if (today == null) "Your day, your choice" else workoutName(today.category()),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (today != null) {
            Text(
                today.distanceKm()?.let { "${decimal(it)} km" }
                    ?: today.duration()?.let { "${it.toMinutes()} min" }
                    ?: "Run by effort"
            )
            Text(
                if (completed) "Completed training is retained. No catch-up mileage."
                else "Your planned session"
            )
            today
                .segments()
                .firstOrNull { it.kind() == org.racedaydreams.domain.WorkoutSegment.Kind.RUN }
                ?.let { segment ->
                    segment.rpe()?.let {
                        Text(
                            if (completed)
                                "Planned effort: ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.effortDescription(it)}."
                            else
                                "Aim for a ${org.racedaydreams.domain.presentation.StrengthPrescriptionText.effortDescription(it)} effort."
                        )
                    }
                    val minimumPace = segment.minimumSecondsPerKm()
                    val maximumPace = segment.maximumSecondsPerKm()
                    if (minimumPace != null && maximumPace != null)
                        Text(
                            "${formatPaceSeconds(minimumPace)}–${formatPaceSeconds(maximumPace)}/km"
                        )
                }
            if (!completed)
                CoachButton("Start with Live Coach") {
                    quick(
                        "execute/${Uri.encode(state.currentPlan!!.id())}/${Uri.encode(today.id())}"
                    )
                }
            TextButton(onClick = { workout(today.id()) }) {
                Text(if (completed) "View completed workout" else "View workout details")
            }
        } else {
            Text("No running session is planned today. Rest or record independently.")
            state.gymProgramme
                ?.sessions()
                ?.firstOrNull { it.date() == java.time.LocalDate.now() }
                ?.let { gym ->
                    Text(gym.title())
                    CoachButton("View strength session") {
                        quick("gym/session/${Uri.encode(gym.id())}")
                    }
                }
        }
    }
    if (
        today != null && state.primaryFocus == org.racedaydreams.domain.settings.PrimaryFocus.HYBRID
    ) {
        state.gymProgramme
            ?.sessions()
            ?.firstOrNull { it.date() == java.time.LocalDate.now() }
            ?.let { gym ->
                SemanticCard(Tile.Strength) {
                    Text("Today's strength", style = MaterialTheme.typography.titleMedium)
                    Text(gym.title(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${gym.exercises().size} exercises · up to ${state.gymProgramme!!.settings().minutes()} min"
                    )
                    TextButton(onClick = { quick("gym/session/${Uri.encode(gym.id())}") }) {
                        Text("View strength session")
                    }
                    OutlinedButton(
                        onClick = { quick("strength?date=${gym.date()}") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("Log today's strength workout")
                    }
                }
            }
    }
    if (
        today == null || state.primaryFocus == org.racedaydreams.domain.settings.PrimaryFocus.HYBRID
    ) {
        state.authoredGymProgramme
            ?.workouts()
            ?.firstOrNull { it.date() == java.time.LocalDate.now() }
            ?.let { gym ->
                SemanticCard(Tile.Strength) {
                    Text("Today's strength", style = MaterialTheme.typography.titleMedium)
                    Text(gym.definition().title(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${gym.definition().slots().size} exercises · about ${gym.expectedMinutes()} min"
                    )
                    TextButton(onClick = { quick("gym/session/${Uri.encode(gym.id())}") }) {
                        Text("View strength session")
                    }
                    CoachButton("Start strength workout") { quick("gym-now") }
                }
            }
    }
    state.profile
        ?.raceGoal()
        ?.takeIf { it.raceDate() != null }
        ?.let { goal ->
            val raceDate = goal.raceDate() ?: return@let
            SemanticCard(Tile.Race) {
                Text(
                    goal.raceName().ifBlank { goal.distance().label() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(),raceDate)} days · ${friendlyDate(raceDate)}"
                )
                TextButton(onClick = race) { Text("Race preparation") }
            }
        }
    state.profile
        ?.raceGoal()
        ?.takeIf { it.raceDate() == null }
        ?.let { goal ->
            SemanticCard(Tile.Race) {
                Text("Current goal", style = MaterialTheme.typography.titleMedium)
                Text(goal.distance().label(), style = MaterialTheme.typography.titleLarge)
                Text(goal.mode().label())
            }
        }
    val upcoming =
        state.currentPlan
            ?.plan()
            ?.workouts()
            .orEmpty()
            .filter { it.date().isAfter(java.time.LocalDate.now()) }
            .sortedBy { it.date() }
            .take(2)
    if (upcoming.isNotEmpty()) {
        CoachHeading("Coming up")
        upcoming.forEach { next ->
            Card(
                onClick = { workout(next.id()) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            workoutName(next.category()),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingContent = { Text(shortDate(next.date())) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                    trailingContent = { Text("›", modifier = Modifier.clearAndSetSemantics {}) },
                )
            }
        }
    }
    CoachCard {
        Text("Run now", style = MaterialTheme.typography.titleLarge)
        Text("Not on the plan? Record a spontaneous run anytime.")
        OutlinedButton(record, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Run now") }
    }
    val summary = org.racedaydreams.domain.presentation.HomeCoachingSummary.from(state.weeklyReview)
    SemanticCard(Tile.Progress) {
        Text("How am I doing?", style = MaterialTheme.typography.titleMedium)
        Text(summary.title(), style = MaterialTheme.typography.headlineSmall)
        summary.observations().take(3).forEach { Text(it) }
        Text("Plan recommendation: ${summary.recommendation()}")
        TextButton(onClick = weekly) { Text("Weekly coaching review") }
    }
    val hard =
        org.racedaydreams.domain.strength.StrengthAnalytics.hardLowerBodyDates(
                state.strengthSessions
            )
            .contains(java.time.LocalDate.now().minusDays(1))
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
    if (state.activities.isNotEmpty()) {
        CoachHeading("Recent progress")
        CoachCard {
            state.activities
                .maxByOrNull { it.start() }
                ?.let { recent ->
                    Text("Most recent run", style = MaterialTheme.typography.titleMedium)
                    Text(displayActivityDate(recent), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        recent.distanceMetres()?.let { formatDistance(it, state) }
                            ?: "Distance unavailable",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TextButton(onClick = { quick("activity/${Uri.encode(recent.id())}") }) {
                        Text("View recorded run")
                    }
                }
            TextButton(onClick = { quick("progress/trends") }) { Text("Training trends") }
            TextButton(onClick = { quick("achievements") }) { Text("Recorded milestones") }
        }
    }
    CoachHeading("Quick actions")
    CoachButton("Log strength") { quick("strength/log") }
    OutlinedButton(
        onClick = { quick("progress/weight") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text("Add weight")
    }
    OutlinedButton(onClick = plan, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("View plan")
    }
    TextButton(onClick = history) { Text("View activity history") }
    TextButton(onClick = baseline) { Text("Review running baseline") }
    TextButton(onClick = weekly) { Text("Not feeling 100%?") }
}
