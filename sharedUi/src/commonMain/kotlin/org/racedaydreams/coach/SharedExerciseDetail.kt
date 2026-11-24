package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        // Compact strength header: the exercise name is the page heading and the favourite star
        // is its single trailing control (spoken label unchanged).
        ScreenHeader(
            discipline = Discipline.STRENGTH,
            glyph = Glyph.DUMBBELL,
            eyebrow = "Strength exercise",
            title = e.name(),
        ) {
            IconButton(
                onClick = {
                    onExerciseChoice(
                        e.id(),
                        choice?.favourite() != true,
                        choice?.archived() == true,
                    )
                },
                modifier = Modifier.size(CoachSpace.Touch),
            ) {
                Text(
                    if (choice?.favourite() == true) "★" else "☆",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Discipline.STRENGTH.onContainer(),
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
                Panel {
                    Text(
                        ExerciseCatalog.definition(e.id())?.category() ?: "Custom exercise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val equipment =
                        ExerciseCatalog.definition(e.id())?.equipment()
                            ?: customMetadata?.let {
                                org.racedaydreams.coach.strength.customEquipmentDescription(it)
                            }
                            ?: "User defined"
                    Text("Equipment: $equipment")
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
                Text(
                    "${sessions.size} recorded workouts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (ExerciseCatalog.definition(e.id()) == null) {
                    CoachTextInput("Custom exercise name", renamed, { renamed = it })
                    // The Favourite button stays the screen's one filled action.
                    OutlinedButton(
                        onClick = {
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
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = CoachSpace.Touch),
                    ) {
                        Text("Save exercise name")
                    }
                }
            }
            "How to do it" -> technique(e.id())
            "History" -> {
                if (sessions.isEmpty())
                    Text(
                        "No workouts for this exercise yet. Log a workout to start its history.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                var limit by rememberSaveable { mutableIntStateOf(20) }
                sessions.take(limit).forEach { s ->
                    Panel(contentPadding = PaddingValues(16.dp)) {
                        Text(
                            formatDate(CalendarDates.atOffset(s.startedAt(), s.offset())),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Discipline.STRENGTH.accent(),
                        )
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
                        "No personal best recorded yet. Record your main sets or add a previous result from PBs & progression.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                records.forEach { r ->
                    // Borderless record panel: the label, then the result as the figure that leads.
                    Panel(contentPadding = PaddingValues(16.dp)) {
                        Text(
                            org.racedaydreams.domain.presentation.AthleteLanguage.recordLabel(
                                // A loaded hold or carry's key names its kg load (B-05).
                                org.racedaydreams.domain.presentation.MassText.category(
                                    requireNotNull(r.category()),
                                    state.massUnit,
                                )
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Discipline.STRENGTH.accent(),
                            text =
                                if (requireNotNull(r.category()).startsWith("Duration"))
                                    StrengthMeasurementPolicy.duration(r.value().toLong())
                                else if (requireNotNull(r.category()).startsWith("Distance"))
                                    "${formatNumber(r.value())} m"
                                else if (r.category() == "Estimated 1RM")
                                    "Estimated maximum for one repetition: ~${kotlin.math.round(state.massUnit.fromKilograms(r.value())).toInt()} ${state.massUnit.name.lowercase()}"
                                else if (r.category() == "Bodyweight reps") "${r.reps()} reps"
                                else
                                    "${formatNumber(state.massUnit.fromKilograms(r.kilograms()))} ${state.massUnit.name.lowercase()} × ${r.reps()}",
                        )
                        Text(
                            requireNotNull(r.provenance()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            text =
                                r.date()?.let {
                                    (if (
                                        r.precision() ==
                                            HistoricalStrengthRecord.DatePrecision.APPROXIMATE
                                    )
                                        "Around "
                                    else "") + formatDate(it)
                                } ?: "Date unknown · outside chronology",
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
