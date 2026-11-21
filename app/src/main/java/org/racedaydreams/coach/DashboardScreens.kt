@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import org.racedaydreams.domain.strength.*
import org.racedaydreams.domain.UnitSystem

@Composable fun StrengthDashboard(
    state:CoachUiState,
    bouldering:org.racedaydreams.coach.climbing.BoulderingViewModel?,
    open:(String)->Unit,
):Unit {
    val climbing = bouldering?.coordinator?.state?.collectAsState()?.value
    StrengthDashboardContent(state, climbing, open)
}

@Composable private fun StrengthDashboardContent(
    state:CoachUiState,
    climbing:org.racedaydreams.coach.climbing.BoulderingState?,
    open:(String)->Unit,
)=SharedStrengthDashboard(
    state,
    java.time.LocalDate.now(),
    java.time.Instant.now(),
    ::shortDate,
    ::friendlyDate,
    ::decimal,
    StrengthLink.entries.toList(),
    open = { link ->
    open(
        when (link) {
            StrengthLink.GYM_NOW -> "gym-now"
            StrengthLink.LOG -> "strength/log"
            StrengthLink.PROGRAMME -> "gym"
            StrengthLink.ANALYTICS -> "strength/analytics"
            StrengthLink.PBS -> "strength/pbs"
            StrengthLink.MILESTONES -> "achievements"
            StrengthLink.HISTORY -> "history?kind=STRENGTH"
            StrengthLink.EXERCISES -> "exercises"
            StrengthLink.RECOVERY -> "strength/recovery"
            StrengthLink.BOULDERING -> "bouldering"
        }
    )
    },
    openWorkout = { open("strength/source/${android.net.Uri.encode(it)}") },
    climbing = climbing?.sessions.orEmpty(),
    climbingDraft = climbing?.draft != null,
    gradeSystem = climbing?.gradeSystem ?: org.racedaydreams.domain.climbing.GradeSystem.V_SCALE,
)
@Composable fun ProgressDashboard(state:CoachUiState,open:(String)->Unit)=SharedProgressDashboard(
    state,
    androidHomePresentation(state),
    { it.atZone(java.time.ZoneId.systemDefault()).toLocalDate() },
    ProgressLink.entries.toList(),
    { open("activity/${android.net.Uri.encode(it)}") },
    java.time.Instant.now(),
) { link ->
    open(
        when (link) {
            ProgressLink.HISTORY -> "history?kind=RUNNING"
            ProgressLink.TRENDS -> "progress/trends"
            ProgressLink.PBS -> "progress/pbs"
            ProgressLink.WEEKLY_REVIEW -> "weekly"
            ProgressLink.MILESTONES -> "achievements"
            ProgressLink.BODY_WEIGHT -> "progress/weight"
            ProgressLink.BODY_GUIDANCE -> "progress/body"
            ProgressLink.STRENGTH_ANALYTICS -> "strength/analytics"
        }
    )
}
fun exerciseMatches(state:CoachUiState,query:String):List<StrengthExercise>{
    val recent=state.strengthSessions.sortedByDescending{it.startedAt()}.flatMap{it.exercises().map{it.exercise().id()}}.distinct()
    return ExerciseSearch.search(state.exercises,query,state.exerciseChoices.filter{it.favourite()}.map{it.exerciseId()}.toSet(),recent,state.exerciseChoices.filter{it.archived()}.map{it.exerciseId()}.toSet()).filter{candidate->query.isNotBlank()||state.exerciseExclusions.none{it.exerciseId()==candidate.id()}}
}
@Composable
fun ExerciseBrowser(
    state: CoachUiState,
    model: CoachViewModel,
    createClassifiedCustom: (() -> Unit)? = null,
    customMetadata: Map<String, org.racedaydreams.domain.strength.CustomStrengthExercise> = emptyMap(),
    open: (String) -> Unit,
) {
    org.racedaydreams.coach.strength.ExerciseBrowseScreen(
        state, { model.strength.saveExercise(it) }, open,
        { model.exerciseExclusion(it, null) }, createClassifiedCustom, customMetadata,
    )
}
@Composable
fun ExerciseDetail(
    state: CoachUiState,
    model: CoachViewModel,
    id: String?,
    customMetadata: CustomStrengthExercise? = null,
    source: (String) -> Unit = {},
) = SharedExerciseDetail(
    state = state,
    id = id,
    onExerciseChoice = { exercise, favourite, archived ->
        model.strength.exerciseChoice(exercise, favourite, archived)
    },
    onExclusion = { exercise, reason -> model.exerciseExclusion(exercise, reason) },
    onSaveExercise = { model.strength.saveExercise(it) },
    onDeleteHistorical = { model.strength.deleteHistorical(it) },
    source = source,
    formatDate = ::friendlyDate,
    formatNumber = ::decimal,
    displaySet = StrengthMeasurements::display,
    technique = { TechniqueSection(it) },
    customMetadata = customMetadata,
)
