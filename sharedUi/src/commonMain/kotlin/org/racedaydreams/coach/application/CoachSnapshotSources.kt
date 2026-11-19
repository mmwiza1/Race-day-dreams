package org.racedaydreams.coach.application

import org.racedaydreams.domain.UserProfile
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.coaching.WorkoutDecision
import org.racedaydreams.domain.presentation.StrengthTodaySummary
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.strength.ExerciseExclusionRepository
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.strength.HistoricalStrengthRecord
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.storage.LegacyGymProgrammePayloadJson
import org.racedaydreams.storage.SqliteStrengthChoices

/**
 * Typed snapshot inputs and existing review/reminder effects. The application owner serialises
 * reads and mutations; platform adapters own permissions, import coverage and reminder delivery. No
 * default empty implementations: every host must bind the real existing evidence.
 */
interface CoachSnapshotSources {
    fun primaryFocus(): org.racedaydreams.domain.settings.PrimaryFocus?

    fun profile(): UserProfile?

    fun profileDraft(): String?

    fun importWindow(): Pair<Instant, Instant>?

    fun shoeDiscomfort(): Set<String>

    fun interruption(): WeeklyCoaching.Interruption?

    fun sessionDecisions(): List<WorkoutDecision>

    fun reviewKnown(id: String): Boolean

    fun saveReview(review: WeeklyCoaching.Review)

    fun decisionForReview(review: WeeklyCoaching.Review): String?

    fun reconcileReminders()

    fun reminderPreferences(): ReminderSettings

    fun weights(): List<BodyWeight>

    fun historicalStrength(): List<HistoricalStrengthRecord>

    fun strengthLinks(): Map<String, LocalDate>

    fun gymLinks(): Map<String, StrengthTodaySummary.Association>

    fun gymProgramme(): GymProgramme?

    fun gymVersions(): List<GymProgramme>

    fun gymDecisions(): List<LegacyGymProgrammePayloadJson.Decision>

    fun exerciseChoices(): List<SqliteStrengthChoices.Choice>

    fun exerciseExclusions(): List<ExerciseExclusionRepository.Exclusion>

    fun acknowledgedPbs(): Set<String>

    fun healthAvailability(): HealthAvailability

    suspend fun grantedHealthPermissions(): Set<String>
}
