package org.racedaydreams.coach.application

import org.racedaydreams.domain.settings.SettingsRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.TrainingStrategy
import org.racedaydreams.domain.UserProfile

/** Profile persistence/eligibility rules, separate from lifecycle-owned draft debouncing. */
class ProfileOperations(
    private val profiles: SqliteUserPreferencesRepository,
    private val settings: SettingsRepository,
) {
    fun save(profile: UserProfile, eligible: Boolean) {
        val retainingPerformance =
            profiles.load().orElse(null)?.preferences()?.strategy() == TrainingStrategy.PERFORMANCE
        require(profile.preferences().strategy() != TrainingStrategy.PERFORMANCE ||
            eligible || retainingPerformance) {
            "Current evidence does not support a new Performance selection. Review your baseline or choose another strategy; your saved goal is unchanged."
        }
        profiles.save(UserProfile(profile.raceGoal(), profile.preferences(), eligible))
        settings.putSetting("baseline.reviewed", "true")
    }

    fun saveDraft(json: String) { profiles.saveDraft(json) }
    fun clearDraft() { profiles.clearDraft() }
}
