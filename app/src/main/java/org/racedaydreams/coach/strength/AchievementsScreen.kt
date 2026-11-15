package org.racedaydreams.coach.strength

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.racedaydreams.coach.friendlyDate
import org.racedaydreams.domain.achievements.TrainingMilestones

/** Android owns lifecycle, repository operations and local date labels; content is shared. */
@Composable
fun AchievementsScreen(
    model: AchievementsViewModel,
    onSource: (TrainingMilestones.Proof) -> Unit,
    onBack: () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    val recognized = run {
        state.awards.map { stored ->
            SharedRecognizedMilestone(
                stored.award(),
                friendlyDate(
                    stored.recognizedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                ),
            )
        }
    }
    SharedAchievementsScreen(
        loading = state.loading,
        awards = recognized,
        celebrations = state.celebrations,
        availableSources = state.availableSources,
        notice = state.notice,
        error = state.error,
        sourceDateLabel = { evidence ->
            evidence.offset()?.let { friendlyDate(evidence.at().atOffset(it).toLocalDate()) }
                ?: "${evidence.at()} · local time zone unavailable"
        },
        onSource = onSource,
        onBack = onBack,
        onDismiss = model::dismiss,
        onRefresh = model::refresh,
    )
}
