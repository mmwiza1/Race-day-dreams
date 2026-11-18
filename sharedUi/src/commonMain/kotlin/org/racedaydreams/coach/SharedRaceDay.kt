package org.racedaydreams.coach

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.RaceGoal
import org.racedaydreams.domain.presentation.CalendarData
import org.racedaydreams.domain.race.RacePredictionEngine
import org.racedaydreams.domain.race.RaceStrategyEngine
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedRaceDay(
    state: CoachUiState,
    today: LocalDate,
    dateLabel: (LocalDate) -> String,
    decimal: (Double) -> String,
    formatPaceSeconds: (Double) -> String,
    start: (String, String, Boolean) -> Unit,
    results: () -> Unit,
    routine: (String) -> Unit,
    checklist: @Composable (RaceGoal) -> Unit = {},
) = CoachPage {
    val goal = state.profile?.raceGoal()
    val raceDate = goal?.raceDate()
    if (goal == null || raceDate == null) {
        CoachHeading("Race Day")
        Text(
            "Add a dated race goal in Settings to prepare for an event. Recording works without a race."
        )
        return@CoachPage
    }
    val metres = goal.distance().kilometres() * 1000
    val prediction by
        produceState<RacePredictionEngine.Prediction?>(
            null,
            goal,
            state.results,
            state.activities,
        ) {
            value =
                withContext(Dispatchers.Default) {
                    RacePredictionEngine.predict(metres, state.results, state.activities, today)
                }
        }
    var hilly by rememberSaveable { mutableStateOf(false) }
    CoachHeading(goal.raceName().ifBlank { goal.distance().label() })
    val days = raceDate.toEpochDay() - today.toEpochDay()
    Text(
        if (days > 0) "$days days to race day"
        else if (days == 0L) "Race day" else "Race date was ${-days} days ago"
    )
    Text(dateLabel(raceDate))
    SemanticCard(Tile.Race) {
        Text("Your goal", style = MaterialTheme.typography.titleMedium)
        Text(
            goal.targetTime()?.let { PaceCalculator.formatTime(it) } ?: goal.mode().label(),
            style = MaterialTheme.typography.headlineMedium,
        )
        goal.targetTime()?.let {
            Text("Target average ${formatPaceSeconds(it.getSeconds()/(metres/1000))}/km")
        }
    }
    SemanticCard(Tile.Progress) {
        Text("Estimated capability", style = MaterialTheme.typography.titleMedium)
        prediction?.let { p ->
            Text(
                if (p.lowSeconds() == null) "Not enough comparable evidence yet"
                else
                    "${PaceCalculator.formatTime(Durations.ofSeconds(requireNotNull(p.lowSeconds())))}–${PaceCalculator.formatTime(Durations.ofSeconds(requireNotNull(p.highSeconds())))}"
            )
            Text("Confidence: ${p.confidence()}")
            WhyDetails(requireNotNull(p.reasons()).joinToString("\n"), "Why this range?")
        }
        Text("Your chosen target stays unchanged. This range is not a guarantee.")
    }
    Row(
        Modifier.heightIn(min = 48.dp)
            .toggleable(value = hilly, role = Role.Checkbox, onValueChange = { hilly = it }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = hilly, onCheckedChange = null)
        Text("Hilly course / uncertain elevation — use effort guidance")
    }
    val stages =
        remember(goal, hilly) {
            RaceStrategyEngine.create(metres, goal.targetTime()?.getSeconds(), hilly)
        }
    CoachHeading("Your pacing strategy")
    stages.forEach { stage ->
        SemanticCard(Tile.Neutral) {
            Text("${decimal(stage.from()/1000)}–${decimal(stage.to()/1000)} km")
            if (stage.fastPace() != null)
                Text(
                    "${formatPaceSeconds(requireNotNull(stage.fastPace()))}–${formatPaceSeconds(requireNotNull(stage.slowPace()))}/km"
                )
            stage.guidance()?.let { Text(it) }
        }
    }
    val race =
        state.currentPlan?.plan()?.workouts()?.firstOrNull {
            it.category() == org.racedaydreams.domain.WorkoutCategory.RACE
        }
    if (race != null && state.currentPlan?.goalSnapshot()?.raceGoal() == goal)
        CoachButton("Prepare / start race") { start(state.currentPlan!!.id(), race.id(), hilly) }
    else
        Text(
            "Create or explicitly rebuild the programme for this goal before linking Race Day. Free recording remains available from Home; no goal is changed automatically."
        )
    TextButton(
        onClick = {
            routine(
                org.racedaydreams.domain.support.RunningRoutines.warmup(
                    org.racedaydreams.domain.WorkoutCategory.RACE,
                    goal.distance().kilometres(),
                )
            )
        }
    ) {
        Text("Race warm-up")
    }
    if (days in 0L..7L) {
        checklist(goal)
        SemanticCard(Tile.Planned) {
            Text("Remaining taper sessions", style = MaterialTheme.typography.titleMedium)
            state.currentPlan
                ?.plan()
                ?.workouts()
                ?.filter {
                    it.date().toEpochDay() >= today.toEpochDay() &&
                        it.date().toEpochDay() < raceDate.toEpochDay()
                }
                ?.forEach { Text("${dateLabel(it.date())} · ${CalendarData.label(it.category())}") }
        }
    }
    if (days in 0L..7L)
        SemanticCard(Tile.Insight) {
            Text("Race-week preparation", style = MaterialTheme.typography.titleMedium)
            Text(
                if (days > 3)
                    "The taper prioritises arriving recovered. Avoid adding missed mileage."
                else
                    "Prepare familiar kit, eat normally, hydrate appropriately and check your travel plan. Avoid last-minute experiments."
            )
        }
    WhyDetails(
        "Shoes · socks · number · timing chip if used · phone · headphones if allowed · familiar fuel/hydration plan · weather-appropriate kit · travel arrangements. Adapt this checklist to your event.",
        "Race-morning checklist",
    )
    TextButton(onClick = results) { Text("Add or view official result") }
}
