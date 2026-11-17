package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.time.CalendarDates

/** Original four-tab detail UI; hosts supply reviewed artwork, exact locale labels and actions. */
@Composable
fun SharedExerciseDetail(
    state: CoachUiState,
    id: String?,
    onExerciseChoice: (String, Boolean, Boolean) -> Unit,
    onExclusion:
        (String, org.racedaydreams.domain.strength.ExerciseExclusionRepository.Reason?) -> Unit,
    onSaveExercise: (StrengthExercise) -> Unit,
    onDeleteHistorical: (String) -> Unit,
    source: (String) -> Unit,
    formatDate: (org.racedaydreams.domain.time.LocalDate) -> String,
    formatNumber: (Double) -> String,
    displaySet:
        (StrengthSession.SetEntry, org.racedaydreams.domain.body.MassUnit, UnitSystem) -> String,
    technique: @Composable (String) -> Unit,
    customMetadata: CustomStrengthExercise? = null,
) {
    val e = state.exercises.firstOrNull { it.id() == id } ?: return
    var section by rememberSaveable(id) { mutableStateOf("Overview") }
    var renamed by rememberSaveable(id) { mutableStateOf(e.name()) }
    val choice = state.exerciseChoices.firstOrNull { it.exerciseId() == id }
    val sessions =
        remember(state.strengthSessions, id) {
            state.strengthSessions
                .filter { s -> s.exercises().any { it.exercise().id() == id } }
                .sortedByDescending { it.startedAt() }
        }
    CoachPage {
        Row(Modifier.fillMaxWidth()) {
            Text(
                e.name(),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onExerciseChoice(
                        e.id(),
                        choice?.favourite() != true,
                        choice?.archived() == true,
                    )
                }
            ) {
                Text(
                    if (choice?.favourite() == true) "★" else "☆",
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                (if (choice?.favourite() == true)
                                    "Remove ${e.name()} from favourites"
                                else "Add ${e.name()} to favourites")
                        },
                )
            }
        }
        CoachSectionTabs(listOf("Overview", "How to do it", "History", "PBs"), section) {
            section = it
        }
        if (section == "Overview")
            org.racedaydreams.coach.strength.ExerciseExclusionControl(
                e.name(),
                state.exerciseExclusions.any { it.exerciseId() == e.id() },
                state.busy,
            ) {
                onExclusion(e.id(), it)
            }
        when (section) {
            "Overview" -> {
                CoachCard {
                    Text(ExerciseCatalog.definition(e.id())?.category() ?: "Custom exercise")
                    Text(
                        "Equipment: ${ExerciseCatalog.definition(e.id())?.equipment()?:customMetadata?.let{org.racedaydreams.coach.strength.customEquipmentDescription(it)}?:"User defined"}"
                    )
                    Text(org.racedaydreams.domain.presentation.AthleteLanguage.measurement(e))
                    WhyDetails(
                        org.racedaydreams.domain.presentation.AthleteLanguage.GYM_TERMS,
                        "Sets and repetitions explained",
                    )
                    customMetadata?.let {
                        org.racedaydreams.coach.strength.CustomExerciseMetadataSummary(it)
                    }
                }
                CoachButton(if (choice?.favourite() == true) "Unfavourite" else "Favourite") {
                    onExerciseChoice(
                        e.id(),
                        choice?.favourite() != true,
                        choice?.archived() == true,
                    )
                }
                TextButton(
                    onClick = {
                        onExerciseChoice(
                            e.id(),
                            choice?.favourite() == true,
                            choice?.archived() != true,
                        )
                    }
                ) {
                    Text(
                        if (choice?.archived() == true) "Restore to My Exercises"
                        else if (ExerciseCatalog.definition(e.id()) == null) "Archive exercise"
                        else "Hide from My Exercises"
                    )
                }
                Text("${sessions.size} recorded workouts")
                if (ExerciseCatalog.definition(e.id()) == null) {
                    CoachTextInput("Custom exercise name", renamed, { renamed = it })
                    CoachButton("Save exercise name") {
                        onSaveExercise(
                            StrengthExercise(
                                e.id(),
                                renamed.trim(),
                                e.region(),
                                e.mode(),
                                e.estimateOneRepMax(),
                                e.measurement(),
                            )
                        )
                    }
                }
            }
            "How to do it" -> technique(e.id())
            "History" -> {
                if (sessions.isEmpty())
                    Text("No workouts for this exercise yet. Log a workout to start its history.")
                var limit by rememberSaveable { mutableIntStateOf(20) }
                sessions.take(limit).forEach { s ->
                    CoachCard {
                        Text(formatDate(CalendarDates.atOffset(s.startedAt(), s.offset())))
                        s.exercises()
                            .filter { it.exercise().id() == id }
                            .forEach {
                                it.sets().forEach { set ->
                                    Text(
                                        displaySet(
                                            set,
                                            state.massUnit,
                                            state.profile?.preferences()?.units()
                                                ?: UnitSystem.METRIC,
                                        )
                                    )
                                }
                            }
                    }
                }
                if (sessions.size > limit)
                    TextButton(onClick = { limit += 20 }) { Text("Load more") }
            }
            "PBs" -> {
                val records =
                    remember(state.strengthSessions, state.historicalStrength, id) {
                        StrengthProgression.timeline(
                                state.strengthSessions,
                                state.historicalStrength,
                            )
                            .filter { it.exerciseId() == id }
                    }
                if (records.isEmpty())
                    Text(
                        "No personal best recorded yet. Record your main sets or add a previous result from PBs & progression."
                    )
                records.forEach { r ->
                    CoachCard {
                        Text(
                            org.racedaydreams.domain.presentation.AthleteLanguage.recordLabel(
                                requireNotNull(r.category())
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            if (requireNotNull(r.category()).startsWith("Duration"))
                                StrengthMeasurementPolicy.duration(r.value().toLong())
                            else if (requireNotNull(r.category()).startsWith("Distance"))
                                "${formatNumber(r.value())} m"
                            else if (r.category() == "Estimated 1RM")
                                "Estimated maximum for one repetition: ~${kotlin.math.round(state.massUnit.fromKilograms(r.value())).toInt()} ${state.massUnit.name.lowercase()}"
                            else if (r.category() == "Bodyweight reps") "${r.reps()} reps"
                            else
                                "${formatNumber(state.massUnit.fromKilograms(r.kilograms()))} ${state.massUnit.name.lowercase()} × ${r.reps()}"
                        )
                        Text(requireNotNull(r.provenance()))
                        Text(
                            r.date()?.let {
                                (if (
                                    r.precision() ==
                                        HistoricalStrengthRecord.DatePrecision.APPROXIMATE
                                )
                                    "Around "
                                else "") + formatDate(it)
                            } ?: "Date unknown · outside chronology"
                        )
                        if (r.sessionId() != null)
                            TextButton(onClick = { source(requireNotNull(r.sessionId())) }) {
                                Text("Edit source workout")
                            }
                        if (r.sessionId() == null)
                            TextButton(onClick = { onDeleteHistorical(requireNotNull(r.id())) }) {
                                Text("Delete historical PB")
                            }
                    }
                }
            }
        }
    }
}
