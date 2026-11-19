package org.racedaydreams.coach.strength

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.domain.strength.GymExecution
import org.racedaydreams.domain.strength.StrengthExercise
import org.racedaydreams.domain.strength.StrengthSession

/** The screen displays acknowledged evidence; the shared coordinator owns all workout timing. */
@Composable
fun GymWorkoutScreen(model: GymNowViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val state by model.state.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) model.setWorkoutVisible(true)
            if (event == Lifecycle.Event.ON_STOP) model.setWorkoutVisible(false)
        }
        owner.lifecycle.addObserver(observer)
        model.setWorkoutVisible(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            owner.lifecycle.removeObserver(observer)
            model.setWorkoutVisible(false)
        }
    }
    SharedGymWorkoutScreen(
        model.shared,
        state,
        onBack,
        ::gymElapsed,
        ::gymActualDescription,
        ::gymExerciseContext,
        ::gymTargetDescription,
        { it.format(java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK)) },
        { slot, enabled -> GymRestCard(state, model, slot, enabled) },
        { slot, enabled -> GymWarmupCard(state, model, slot, enabled) },
    )
}

internal fun gymActualDescription(
    actual: StrengthSession.SetEntry,
    exercise: StrengthExercise,
): String {
    val load =
        if (
            exercise.measurement().usesLoad() && exercise.mode() != StrengthExercise.Mode.BODYWEIGHT
        )
            "${gymDecimal(actual.kilograms())} kg × "
        else ""
    val measurement =
        if (exercise.measurement().usesReps()) "${actual.reps()} reps"
        else
            actual.durationSeconds()?.let { "${it}s" }
                ?: "${gymDecimal(actual.distanceMetres()!!)} m"
    val effort =
        actual.rpe()?.let { " · RPE ${gymDecimal(it)}" }
            ?: actual.repetitionsInReserve()?.let { " · RIR ${gymDecimal(it)}" }
            ?: ""
    return "$load$measurement$effort" + if (actual.warmUp()) " · warm-up" else ""
}

internal fun gymElapsed(millis: Long): String {
    val total = millis / 1000
    return String.format(Locale.UK, "%d:%02d:%02d", total / 3600, total / 60 % 60, total % 60)
}

private fun gymDecimal(value: Double) =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

internal fun gymOriginLabel(origin: GymExecution.Origin): String =
    when (origin) {
        GymExecution.Origin.PLANNED -> "Planned workout"
        GymExecution.Origin.QUICK -> "Quick workout"
        GymExecution.Origin.CUSTOM -> "Custom workout"
        GymExecution.Origin.REPEATED -> "Repeated workout"
        GymExecution.Origin.FAVOURITE -> "Favourite workout"
        GymExecution.Origin.GENERATED -> "Generated one-off workout"
        GymExecution.Origin.FREE -> "Spontaneous workout"
    }
