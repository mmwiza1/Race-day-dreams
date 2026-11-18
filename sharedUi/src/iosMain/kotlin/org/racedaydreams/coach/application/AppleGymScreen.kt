package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.racedaydreams.coach.CoachCard
import org.racedaydreams.coach.strength.*
import org.racedaydreams.domain.PaceCalculator
import org.racedaydreams.domain.presentation.StrengthPrescriptionText
import org.racedaydreams.domain.strength.GymRestTimer
import org.racedaydreams.domain.strength.GymWorkoutDefinition
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession
import org.racedaydreams.domain.time.Durations
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.reminders.KotlinxReminderCalendar

private enum class GymPage {
    ENTRY,
    BUILDER,
    WORKOUT,
}

@Composable
internal fun AppleGymScreen(
    session: AppleCoachSession<*>,
    plannedDate: LocalDate? = null,
    back: () -> Unit,
) {
    val model = session.gym
    val state by model.state.collectAsState()
    val scope = rememberCoroutineScope()
    var alertError by remember { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(GymPage.ENTRY) }
    val workout = { page = GymPage.WORKOUT }
    val entry = {
        page = GymPage.ENTRY
        session.services.operations.refresh()
    }
    when (page) {
        GymPage.ENTRY ->
            SharedGymNowScreen(
                model,
                workout,
                { page = GymPage.BUILDER },
                back,
                state,
                AppleCalendarDates.today(),
                AppleCalendarDates::label,
                {
                    AppleCalendarDates.label(
                        KotlinxReminderCalendar.at(it, TimeZone.currentSystemDefault().id).date
                    )
                },
                ::appleGymTarget,
                // The shared entry screen provides explicit Back controls on iOS.
                plannedDate = plannedDate,
                backHandler = { _, _ -> },
            )
        GymPage.BUILDER ->
            SharedGymWorkoutBuilderScreen(
                model,
                state,
                workout,
                entry,
                { it.name() },
                ::appleGymTarget,
                ::number,
            )
        GymPage.WORKOUT -> {
            LaunchedEffect(model) {
                while (isActive) {
                    model.observeWorkoutTime()
                    delay(1_000)
                }
            }
            SharedGymWorkoutScreen(
                model,
                state,
                entry,
                { PaceCalculator.formatTime(Durations.ofMillis(it)) },
                ::actual,
                { it.name() },
                ::appleGymTarget,
                AppleCalendarDates::label,
                restControls = { _, enabled ->
                    CoachCard {
                        Text("Rest timer")
                        val preferences = state.restPreferences
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                scope.launch {
                                    try {
                                        alertError = null
                                        val notify = !preferences.notifications()
                                        if (!notify || session.reminders.requestPermission()) {
                                            model.restPreferences(
                                                GymRestTimer.Preferences(
                                                    preferences.defaultSeconds(),
                                                    preferences.suggestAfterSet(),
                                                    notify,
                                                    preferences.vibration(),
                                                    preferences.audio(),
                                                )
                                            )
                                        } else alertError = "Notification access is off."
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        alertError = "Could not change rest alerts."
                                    }
                                }
                            },
                        ) {
                            Text(
                                if (preferences.notifications()) "Turn off rest alerts"
                                else "Allow rest alerts"
                            )
                        }
                        alertError?.let { Text(it) }

                        val timer = state.restTimer
                        when (timer?.phase()) {
                            GymRestTimer.Phase.RUNNING -> {
                                Text("${timer.secondsRemaining()} seconds remaining")
                                TextButton(
                                    onClick = { model.restAction("pause") },
                                    enabled = enabled,
                                ) {
                                    Text("Pause rest")
                                }
                            }
                            GymRestTimer.Phase.PAUSED ->
                                TextButton(
                                    onClick = { model.restAction("resume") },
                                    enabled = enabled,
                                ) {
                                    Text("Resume rest")
                                }
                            else ->
                                TextButton(
                                    onClick = { model.restAction("start") },
                                    enabled = enabled,
                                ) {
                                    Text("Start rest")
                                }
                        }
                        if (
                            timer?.phase() in
                                listOf(GymRestTimer.Phase.RUNNING, GymRestTimer.Phase.PAUSED)
                        ) {
                            TextButton(onClick = { model.restAction("+30") }, enabled = enabled) {
                                Text("Add 30 seconds")
                            }
                            TextButton(onClick = { model.restAction("skip") }, enabled = enabled) {
                                Text("Skip rest")
                            }
                        }
                    }
                },
                warmupGuidance = { _, enabled ->
                    model.warmupSuggestion()?.let { suggestion ->
                        CoachCard {
                            Text("Optional warm-up")
                            suggestion.instruction()?.let { Text(it) }
                            TextButton(onClick = { model.prepareWarmup(null) }, enabled = enabled) {
                                Text("Log a warm-up set")
                            }
                            suggestion.ramps().forEach { ramp ->
                                TextButton(
                                    onClick = { model.prepareWarmup(ramp) },
                                    enabled = enabled,
                                ) {
                                    Text(
                                        "${number(ramp.kilograms())} kg · ${ramp.repetitions()} repetitions"
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

internal fun appleGymTarget(slot: GymWorkoutDefinition.Slot): String = buildString {
    append(StrengthPrescriptionText.instructions(slot).joinToString(" "))
    slot.target()?.effort()?.let { append(" ").append(StrengthPrescriptionText.effortDetails(it)) }
    slot.target()?.referenceLoadKg()?.let {
        append(" Optional reference: ").append(number(it)).append(" kg.")
    }
}

private fun actual(set: StrengthSession.SetEntry, exercise: StrengthExercise): String {
    val load =
        if (
            exercise.measurement().usesLoad() && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
        )
            "${number(set.kilograms())} kg · "
        else ""
    val task =
        when {
            exercise.measurement().usesReps() -> "${set.reps()} repetitions"
            set.durationSeconds() != null -> "${set.durationSeconds()} seconds"
            else -> "${number(requireNotNull(set.distanceMetres()))} metres"
        }
    val effort =
        set.rpe()?.let { " · RPE ${number(it)}" }
            ?: set.repetitionsInReserve()?.let { " · RIR ${number(it)}" }.orEmpty()
    return load + task + effort + if (set.warmUp()) " · warm-up" else ""
}

private fun number(value: Double): String = value.toString().removeSuffix(".0")
