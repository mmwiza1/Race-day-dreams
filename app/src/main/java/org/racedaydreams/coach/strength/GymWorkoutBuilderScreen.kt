package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale
import org.racedaydreams.domain.strength.*

/** Android lifecycle and original numeric labels for the shared production builder/editor. */
@Composable
fun GymWorkoutBuilderScreen(model: GymNowViewModel, onWorkout: () -> Unit, onBack: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    SharedGymWorkoutBuilderScreen(
        model.shared,
        state,
        onWorkout,
        onBack,
        ::gymExerciseContext,
        ::gymTargetDescription,
        ::gymNumber,
    )
}

@Composable
internal fun GymSlotEditor(
    slot: GymWorkoutDefinition.Slot?,
    exercises: List<StrengthExercise>,
    favourites: Set<String>,
    recent: List<String>,
    archived: Set<String>,
    excluded: Set<String>,
    error: String?,
    allowOpen: Boolean = true,
    allowExerciseChange: Boolean = true,
    allowReferenceLoad: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (StrengthExercise, GymSlotInput) -> Unit,
) =
    SharedGymSlotEditor(
        slot,
        exercises,
        favourites,
        recent,
        archived,
        excluded,
        error,
        ::gymExerciseContext,
        ::gymNumber,
        allowOpen,
        allowExerciseChange,
        allowReferenceLoad,
        onDismiss,
        onSave,
    )

internal fun gymExerciseContext(exercise: StrengthExercise): String =
    ExerciseCatalog.definition(exercise.id())?.let { "${it.category()} · ${it.equipment()}" }
        ?: "Custom exercise · ${exercise.measurement().name.lowercase(Locale.UK).replace('_', ' ')}"

internal fun gymTargetDescription(slot: GymWorkoutDefinition.Slot): String {
    val target = slot.target() ?: return "Open · record the sets you choose"
    val measurement =
        target.minimumReps()?.let { minimum ->
            val maximum = target.maximumReps()
            if (minimum == maximum) "$minimum reps" else "$minimum–$maximum reps"
        }
            ?: target.seconds()?.let { "${it}s per set" }
            ?: target.metres()?.let { "${gymNumber(it)} m per set" }
            ?: ""
    val effort =
        target.effort()?.let {
            " · ${it.scale().name} ${gymNumber(it.minimum())}–${gymNumber(it.maximum())}"
        } ?: ""
    val rest = target.restSeconds()?.let { " · ${it}s rest" } ?: ""
    val reference = target.referenceLoadKg()?.let { " · ${gymNumber(it)} kg reference" } ?: ""
    return "${target.sets()} × $measurement$effort$rest$reference"
}

private fun gymNumber(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
