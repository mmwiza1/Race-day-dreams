package org.racedaydreams.coach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.ActivityRecord

/**
 * Android profile/date/unit adapter. Production comparison UI and its explicit choice are shared.
 */
@Composable
fun RunComparisonSection(state: CoachUiState, current: ActivityRecord) {
    val prescriptions =
        remember(state.planVersions) {
            state.planVersions.flatMap { it.plan().workouts() }.distinctBy { it.id() }
        }
    SharedRunComparisonSection(
        current,
        state.activities,
        prescriptions,
        state.profile?.preferences()?.units() == UnitSystem.IMPERIAL,
        ::displayActivityDate,
        { formatDistance(it, state) },
        ::formatPaceSeconds,
        ::decimal,
    )
}
