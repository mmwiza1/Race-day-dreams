package org.racedaydreams.coach.application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlinx.coroutines.withContext
import org.racedaydreams.coach.*
import org.racedaydreams.coach.strength.SharedCustomStrengthExerciseScreen
import org.racedaydreams.coach.strength.SharedExerciseBrowseScreen
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.body.MassUnit
import org.racedaydreams.domain.strength.*
import platform.Foundation.NSUUID

@Composable
internal fun AppleExerciseLibrary(session: AppleCoachSession<*>, back: () -> Unit) {
    val state by session.services.operations.state.collectAsState()
    val author by session.authoring.state.collectAsState()
    val metadata by
        produceState<Map<String, CustomStrengthExercise>?>(null, state.exercises) {
            try {
                value =
                    withContext(applicationIoDispatcher()) {
                        session.programming.customExercises().associateBy { it.exercise().id() }
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                value = null
            }
        }
    var exerciseId by remember { mutableStateOf<String?>(null) }
    var sourceId by remember { mutableStateOf<String?>(null) }
    var custom by remember { mutableStateOf(false) }
    val id = exerciseId
    val source = sourceId
    val strength = session.services.strength
    when {
        source != null ->
            CoachPage {
                CoachButton("Back to exercise") { sourceId = null }
                CoachHeading("Recorded strength workout")
                val workout = state.strengthSessions.firstOrNull { it.id() == source }
                if (workout == null) Text("This source workout is unavailable.")
                else
                    workout.exercises().forEach { group ->
                        CoachCard {
                            Text(group.exercise().name())
                            group.sets().forEach { set ->
                                Text(
                                    displaySet(
                                        set,
                                        state.massUnit,
                                        state.profile?.preferences()?.units() ?: UnitSystem.METRIC,
                                    )
                                )
                            }
                        }
                    }
            }
        custom -> {
            LaunchedEffect(session.authoring) { session.authoring.reload() }
            SharedCustomStrengthExerciseScreen(
                session.authoring,
                author,
                onBack = { custom = false },
                onSaved = { newId ->
                    session.services.operations.refresh()
                    exerciseId = newId
                    custom = false
                },
            )
        }
        id != null -> {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.TextButton(onClick = { exerciseId = null }) {
                    Text("Back to exercise library")
                }
                Box(Modifier.weight(1f)) {
                    if (state.exercises.none { it.id() == id }) Text("Exercise unavailable")
                    else
                        SharedExerciseDetail(
                            state,
                            id,
                            strength::exerciseChoice,
                            strength::exerciseExclusion,
                            strength::saveExercise,
                            strength::deleteHistorical,
                            { sourceId = it },
                            AppleCalendarDates::label,
                            ::number,
                            ::displaySet,
                            technique = { exercise ->
                                SharedTechniqueContent(
                                    TechniqueGuide.forExercise(exercise),
                                    null,
                                    false,
                                ) {}
                            },
                            customMetadata = metadata?.get(id),
                        )
                }
            }
        }
        else ->
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.TextButton(onClick = back) { Text("Back to Gym") }
                Box(Modifier.weight(1f)) {
                    SharedExerciseBrowseScreen(
                        state,
                        onCreateCustom = strength::saveExercise,
                        open = { exerciseId = it },
                        thumbnail = { exercise ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        state.exercises
                                            .firstOrNull { it.id() == exercise }
                                            ?.name()
                                            ?.take(1)
                                            .orEmpty()
                                    )
                                }
                            }
                        },
                        nextExerciseId = { NSUUID().UUIDString },
                        onRestore = { strength.exerciseExclusion(it, null) },
                        onCreateClassifiedCustom = { custom = true },
                        customMetadata = metadata.orEmpty(),
                    )
                }
            }
    }
}

private fun displaySet(set: StrengthSession.SetEntry, mass: MassUnit, units: UnitSystem): String {
    val prefix =
        if (set.kilograms() > 0)
            "${number(mass.fromKilograms(set.kilograms()))} ${mass.name.lowercase()} × "
        else ""
    val duration = set.durationSeconds()
    if (duration != null) return prefix + StrengthMeasurementPolicy.duration(duration)
    val distance = set.distanceMetres()
    if (distance != null) {
        val imperial = units == UnitSystem.IMPERIAL
        return prefix +
            number(if (imperial) distance / 0.9144 else distance) +
            if (imperial) " yd" else " m"
    }
    return "$prefix${set.reps()} reps"
}

private fun number(value: Double): String {
    val tenths = (value * 10).roundToLong()
    return (tenths / 10).toString() + "." + kotlin.math.abs(tenths % 10)
}
