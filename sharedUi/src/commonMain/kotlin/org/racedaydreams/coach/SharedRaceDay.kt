package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        ScreenHeader(Discipline.RUNNING, Glyph.FLAG, "Your race", "Race Day")
        QuietNote(
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
    val days = raceDate.toEpochDay() - today.toEpochDay()
    // The race itself leads: name, countdown, date and the chosen goal in one running-colour hero.
    HeroSurface(Discipline.RUNNING, Glyph.FLAG, "Race day", Modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HeroTitle(goal.raceName().ifBlank { goal.distance().label() })
            Text(
                if (days > 0) "$days days to race day"
                else if (days == 0L) "Race day" else "Race date was ${-days} days ago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                dateLabel(raceDate),
                style = MaterialTheme.typography.bodyLarge,
                color = LocalContentColor.current.copy(alpha = 0.85f),
            )
        }
        StatRow(
            buildList {
                add(
                    Stat(
                        "Your goal",
                        goal.targetTime()?.let { PaceCalculator.formatTime(it) }
                            ?: goal.mode().label(),
                    )
                )
                goal.targetTime()?.let {
                    add(
                        Stat(
                            "Target average",
                            formatPaceSeconds(it.getSeconds() / (metres / 1000)),
                            "/km",
                            unitGap = false,
                        )
                    )
                }
            }
        )
    }
    ExpressiveSection(
        "Estimated capability",
        Glyph.TREND,
        ChartHue.TEAL,
        caption = "From your comparable recorded runs and results",
    ) {
        prediction?.let { p ->
            Text(
                if (p.lowSeconds() == null) "Not enough comparable evidence yet"
                else
                    "${PaceCalculator.formatTime(Durations.ofSeconds(requireNotNull(p.lowSeconds())))}–${PaceCalculator.formatTime(Durations.ofSeconds(requireNotNull(p.highSeconds())))}",
                style =
                    if (p.lowSeconds() == null) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Confidence: ${p.confidence()}", style = MaterialTheme.typography.bodyLarge)
            WhyDetails(requireNotNull(p.reasons()).joinToString("\n"), "Why this range?")
        }
        QuietNote("Your chosen target stays unchanged. This range is not a guarantee.")
    }
    val stages =
        remember(goal, hilly) {
            RaceStrategyEngine.create(metres, goal.targetTime()?.getSeconds(), hilly)
        }
    SectionLabel(
        "Your pacing strategy",
        Glyph.RUN,
        ChartHue.VIOLET,
        caption = "Stage by stage, from start to finish",
    )
    Panel {
        CheckRow("Hilly course / uncertain elevation — use effort guidance", hilly) { hilly = it }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StageTimeline(
            stages.map { stage ->
                TimelineStage(
                    "${decimal(stage.from()/1000)}–${decimal(stage.to()/1000)} km",
                    if (stage.fastPace() != null)
                        "${formatPaceSeconds(requireNotNull(stage.fastPace()))}–${formatPaceSeconds(requireNotNull(stage.slowPace()))}/km"
                    else null,
                    stage.guidance(),
                )
            },
            Discipline.RUNNING,
        )
    }
    val race =
        state.currentPlan?.plan()?.workouts()?.firstOrNull {
            it.category() == org.racedaydreams.domain.WorkoutCategory.RACE
        }
    if (race != null && state.currentPlan?.goalSnapshot()?.raceGoal() == goal)
        HeroPrimaryAction(Discipline.RUNNING, "Prepare / start race") {
            start(state.currentPlan!!.id(), race.id(), hilly)
        }
    else
        QuietNote(
            "Create or explicitly rebuild the programme for this goal before linking Race Day. Free recording remains available from Home; no goal is changed automatically."
        )
    ActionGroup(null) {
        ActionRow("Race warm-up", "A warm-up routine for this distance") {
            routine(
                org.racedaydreams.domain.support.RunningRoutines.warmup(
                    org.racedaydreams.domain.WorkoutCategory.RACE,
                    goal.distance().kilometres(),
                )
            )
        }
        ActionRow("Add or view official result", onClick = results)
    }
    if (days in 0L..7L) {
        checklist(goal)
        ExpressiveSection("Remaining taper sessions", Glyph.CALENDAR, ChartHue.BLUE) {
            val taper =
                state.currentPlan?.plan()?.workouts()?.filter {
                    it.date().toEpochDay() >= today.toEpochDay() &&
                        it.date().toEpochDay() < raceDate.toEpochDay()
                }
            if (taper.isNullOrEmpty()) QuietNote("No planned sessions remain before the race.")
            taper?.forEach {
                Text(
                    "${dateLabel(it.date())} · ${CalendarData.label(it.category())}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        ExpressiveSection("Race-week preparation", Glyph.SPARK, ChartHue.TEAL) {
            Text(
                if (days > 3)
                    "The taper prioritises arriving recovered. Avoid adding missed mileage."
                else
                    "Prepare familiar kit, eat normally, hydrate appropriately and check your travel plan. Avoid last-minute experiments.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    WhyDetails(
        "Shoes · socks · number · timing chip if used · phone · headphones if allowed · familiar fuel/hydration plan · weather-appropriate kit · travel arrangements. Adapt this checklist to your event.",
        "Race-morning checklist",
    )
}
