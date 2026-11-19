package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord

@Composable
internal fun RecordedRunSummary(activity: ActivityRecord, state: CoachUiState) {
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    SharedRecordedRunSummary(
        activity,
        { formatDistance(it, state) },
        recordedElapsedPace(activity, imperial),
    )
}

@Composable
internal fun RecordedHeartRateSummary(activity: ActivityRecord) =
    SharedRecordedHeartRateSummary(activity, ::decimal)

internal fun recordedElapsedPace(activity: ActivityRecord, imperial: Boolean): String? =
    sharedRecordedElapsedPace(activity, imperial, ::formatPaceSeconds)
