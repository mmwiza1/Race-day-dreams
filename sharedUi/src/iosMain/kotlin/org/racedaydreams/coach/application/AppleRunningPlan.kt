package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.support.RunningRoutines
import org.racedaydreams.domain.time.DayOfWeek
import org.racedaydreams.domain.time.LocalDate

@Composable
internal fun AppleRunningPlan(
    session: AppleCoachSession<*>,
    openWorkout: (String) -> Unit,
    openStrength: (LocalDate) -> Unit,
) {
    val state by session.services.operations.state.collectAsState()
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    var editing by rememberSaveable { mutableStateOf(false) }
    var routineId by rememberSaveable { mutableStateOf<String?>(null) }
    val routine = routineId
    if (editing) {
        AppleGoalForm(session, state) { editing = false }
        return
    }
    if (routine != null) {
        CoachPage {
            CoachButton("Back to plan") { routineId = null }
            val definition = RunningRoutines.get(routine)
            CoachHeading(requireNotNull(definition.title()))
            definition.movements().orEmpty().forEach { move ->
                CoachCard {
                    Text(requireNotNull(move.title()))
                    Text(requireNotNull(move.dose()))
                    Text(requireNotNull(move.instructions()))
                }
            }
        }
        return
    }
    SharedTrainingPlanScreen(
        state = state,
        dates = AppleCalendarDates,
        weekRangeLabel = {
            "${AppleCalendarDates.label(it)} – ${AppleCalendarDates.label(it.plusDays(6))}"
        },
        distanceLabel = { distanceLabel(it, imperial) },
        generate = {
            if (state.profile == null) editing = true
            else session.services.runningPlan.generatePlan()
        },
        decide = { proposal, choice -> session.services.runningPlan.decide(proposal, choice) },
        open = openWorkout,
        editGoal = { editing = true },
        strength = openStrength,
        routine = { id, _ -> routineId = id },
        warmup = { _, _, open -> TextButton(onClick = open) { Text("View warm-up") } },
        mobility = {
            Text("Optional movement support", style = MaterialTheme.typography.titleMedium)
            RunningRoutines.all().forEach { item ->
                TextButton(onClick = { routineId = requireNotNull(item.id()) }) {
                    Text(requireNotNull(item.title()))
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppleGoalForm(session: AppleCoachSession<*>, state: CoachUiState, back: () -> Unit) {
    val profile = state.profile
    val goal = profile?.raceGoal()
    val prior = profile?.preferences()
    var distance by remember { mutableStateOf(goal?.distance() ?: RaceDistance.GENERAL_FITNESS) }
    var name by rememberSaveable { mutableStateOf(goal?.raceName().orEmpty()) }
    var date by rememberSaveable { mutableStateOf(goal?.raceDate()?.toString().orEmpty()) }
    var mode by remember { mutableStateOf(goal?.mode() ?: GoalMode.FINISH) }
    var target by rememberSaveable {
        mutableStateOf(goal?.targetTime()?.let(PaceCalculator::formatTime).orEmpty())
    }
    var strategy by remember {
        mutableStateOf(prior?.strategy() ?: TrainingStrategy.DISTANCE_FIRST)
    }
    var days by remember { mutableStateOf(prior?.runningDays().orEmpty()) }
    var gym by remember { mutableStateOf(prior?.gymDays().orEmpty()) }
    var longDay by remember { mutableStateOf(prior?.longRunDay()) }
    var runs by rememberSaveable { mutableStateOf(prior?.runsPerWeek()?.toString() ?: "3") }
    var schedule by remember { mutableStateOf(prior?.scheduleType() ?: ScheduleType.FLEXIBLE) }
    var units by remember { mutableStateOf(prior?.units() ?: UnitSystem.METRIC) }
    var validation by remember { mutableStateOf<String?>(null) }

    CoachPage {
        CoachHeading("Goal and availability")
        options("Distance", RaceDistance.entries, distance, { it.label() }) { distance = it }
        if (distance != RaceDistance.GENERAL_FITNESS) {
            field("Race name (optional)", name) { name = it }
            field("Race date (optional, YYYY-MM-DD)", date) { date = it }
            options("Goal", GoalMode.entries, mode, { it.label() }) { mode = it }
            if (mode == GoalMode.TARGET_TIME)
                field("Target time (HH:MM:SS)", target) { target = it }
        }
        options(
            "Training strategy",
            TrainingStrategy.entries.filter {
                it != TrainingStrategy.PERFORMANCE ||
                    state.performanceEligible ||
                    prior?.strategy() == it
            },
            strategy,
            { it.label() },
        ) {
            strategy = it
        }
        Text(strategy.description())
        Text("Available running days")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.values().forEach { day ->
                FilterChip(
                    day in days,
                    {
                        days = if (day in days) days - day else days + day
                        if (longDay !in days) longDay = null
                    },
                    label = { Text(weekday(day)) },
                )
            }
        }
        field("Runs per week", runs) { runs = it }
        options(
            "Preferred long-run day",
            listOf(null) + days.toList(),
            longDay,
            { it?.let(::weekday) ?: "Any day" },
        ) {
            longDay = it
        }
        Text("Gym days (optional)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.values().forEach { day ->
                FilterChip(
                    day in gym,
                    { gym = if (day in gym) gym - day else gym + day },
                    label = { Text(weekday(day)) },
                )
            }
        }
        options("Schedule", ScheduleType.entries, schedule, { it.label() }) { schedule = it }
        options("Units", UnitSystem.entries, units, { it.label() }) { units = it }
        (validation ?: state.error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            enabled = !state.busy,
            onClick = {
                try {
                    validation = null
                    val hasRace = distance != RaceDistance.GENERAL_FITNESS
                    val selectedMode = if (hasRace) mode else GoalMode.FINISH
                    val updated =
                        UserProfile(
                            RaceGoal(
                                distance,
                                name.takeIf { hasRace },
                                date
                                    .takeIf { hasRace && it.isNotBlank() }
                                    ?.let(AppleCalendarDates::parse),
                                selectedMode,
                                if (selectedMode == GoalMode.TARGET_TIME)
                                    PaceCalculator.parseTime(target)
                                else null,
                            ),
                            TrainingPreferences(
                                strategy,
                                days,
                                runs.toInt(),
                                longDay,
                                gym,
                                schedule,
                                units,
                            ),
                            state.performanceEligible,
                        )
                    session.services.operations.submit(
                        message = "Your goal and routine are saved on this device.",
                        onSuccess = back,
                    ) {
                        session.services.profile.save(updated, state.performanceEligible)
                    }
                } catch (failure: IllegalArgumentException) {
                    validation = failure.message ?: "Check the goal and availability."
                }
            },
        ) {
            Text("Save goal")
        }
        TextButton(onClick = back, enabled = !state.busy) { Text("Back to plan") }
    }
}

@Composable
private fun field(label: String, value: String, update: (String) -> Unit) {
    OutlinedTextField(
        value,
        update,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> options(
    label: String,
    values: List<T>,
    selected: T,
    name: (T) -> String,
    select: (T) -> Unit,
) {
    Text(label)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { item ->
            FilterChip(item == selected, { select(item) }, label = { Text(name(item)) })
        }
    }
}

private fun weekday(day: DayOfWeek): String =
    day.name.lowercase().replaceFirstChar { it.uppercase() }
