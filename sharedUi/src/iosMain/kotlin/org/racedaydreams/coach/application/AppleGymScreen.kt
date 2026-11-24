package org.racedaydreams.coach.application

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
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
                { slot -> appleGymTarget(slot, state.massUnit) },
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
                { slot -> appleGymTarget(slot, state.massUnit) },
                ::number,
            )
        GymPage.WORKOUT -> {
            // As on Android: the shared coordinator ticks the clock while the workout is visible,
            // and checkpoints the workout and stops ticking when the app goes to the background.
            DisposableEffect(model) {
                model.setWorkoutVisible(true)
                val visibility = AppleAppVisibility { visible -> model.setWorkoutVisible(visible) }
                onDispose {
                    visibility.close()
                    model.setWorkoutVisible(false)
                }
            }
            SharedGymWorkoutScreen(
                model,
                state,
                entry,
                { PaceCalculator.formatTime(Durations.ofMillis(it)) },
                { set, exercise -> actual(set, exercise, state.massUnit) },
                { it.name() },
                { slot -> appleGymTarget(slot, state.massUnit) },
                AppleCalendarDates::label,
                restControls = { slot, enabled ->
                    GymRestControls(
                        prefs = state.restPreferences,
                        timer = state.restTimer,
                        prescribedRestSeconds = slot.target()?.restSeconds(),
                        enabled = enabled,
                        notificationExplanation =
                            "Rest never blocks your next set. Completion alerts follow your iPhone notification settings.",
                        onAction = model::restAction,
                        onPreferencesChange = model::restPreferences,
                        onNotificationsChange = { notify ->
                            scope.launch {
                                try {
                                    alertError = null
                                    if (!notify || session.reminders.requestPermission()) {
                                        val prefs = model.state.value.restPreferences
                                        model.restPreferences(
                                            GymRestTimer.Preferences(
                                                prefs.defaultSeconds(),
                                                prefs.suggestAfterSet(),
                                                notify,
                                                prefs.vibration(),
                                                prefs.audio(),
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
                        showVibrationPreference = false,
                    )
                    alertError?.let { Text(it) }
                },
                warmupGuidance = { slot, enabled ->
                    val suggestion =
                        remember(slot, state.recent, state.view?.observed()?.actualSets()) {
                            model.warmupSuggestion()
                        }
                    GymWarmupGuidanceCard(
                        slotId = slot.id(),
                        exerciseId = slot.exercise().id(),
                        suggestion = suggestion,
                        enabled = enabled,
                        formatKilograms = ::number,
                        onPrepare = model::prepareWarmup,
                        massUnit = state.massUnit,
                    )
                },
            )
        }
    }
}

internal fun appleGymTarget(
    slot: GymWorkoutDefinition.Slot,
    unit: org.racedaydreams.domain.body.MassUnit = org.racedaydreams.domain.body.MassUnit.KG,
): String = buildString {
    append(StrengthPrescriptionText.instructions(slot).joinToString(" "))
    slot.target()?.effort()?.let { append(" ").append(StrengthPrescriptionText.effortDetails(it)) }
    slot.target()?.referenceLoadKg()?.let {
        append(" Optional reference: ")
            .append(org.racedaydreams.domain.presentation.MassText.label(it, unit))
            .append(".")
    }
}

private fun actual(
    set: StrengthSession.SetEntry,
    exercise: StrengthExercise,
    unit: org.racedaydreams.domain.body.MassUnit,
): String {
    val load =
        if (
            exercise.measurement().usesLoad() && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
        )
            "${org.racedaydreams.domain.presentation.MassText.label(set.kilograms(), unit)} · "
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
