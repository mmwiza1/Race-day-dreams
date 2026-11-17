package org.racedaydreams.coach.application

import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.reminders.ReminderSettings
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.health.HealthAvailability
import org.racedaydreams.storage.*

/** Actual platform effects: availability is not proof that health-read permission was granted. */
interface CoachSnapshotPlatform {
    fun importWindow(): Pair<Instant, Instant>?

    fun reconcileReminders()

    fun reminderPreferences(): ReminderSettings

    fun healthAvailability(): HealthAvailability

    suspend fun grantedHealthPermissions(): Set<String>
}

/**
 * Bind the coherent snapshot to the existing typed repositories over ONE borrowed database. Hosts
 * must provide real permission/reminder effects and real clock/identity sources. Constructing this
 * binding never opens a connection; the application still owns IO and operation serialisation.
 */
class SqliteCoachSnapshotSources(
    database: SqlDatabase,
    now: () -> Instant,
    nextId: () -> String,
    private val platform: CoachSnapshotPlatform,
) : CoachSnapshotSources {
    private val profiles = SqliteAthleteProfile(database)
    private val focus = SqlitePrimaryFocus(SqliteSettings(database))
    private val context = SqliteCoachingContext(database, now, nextId)
    private val gym = SqliteLegacyGymProgrammes(database, now, nextId)
    private val choices = SqliteStrengthChoices(database)
    private val exclusions = SqliteExerciseExclusions(database)
    private val support = SqliteRunningSupportArchives(database)

    override fun primaryFocus() = focus.load()

    override fun profile() = profiles.load()

    override fun profileDraft() = profiles.loadDraft()

    override fun importWindow() = platform.importWindow()

    override fun shoeDiscomfort() = support.shoeDiscomfort()

    override fun interruption() = context.interruption()

    override fun sessionDecisions() = context.sessionDecisions()

    override fun reviewKnown(id: String) = context.reviewKnown(id)

    override fun saveReview(review: WeeklyCoaching.Review) = context.saveReview(review)

    override fun decisionForReview(review: WeeklyCoaching.Review) =
        context.decisionForReview(review)

    override fun reconcileReminders() = platform.reconcileReminders()

    override fun reminderPreferences() = platform.reminderPreferences()

    override fun weights() = context.weights()

    override fun historicalStrength() = context.historical()

    override fun strengthLinks() = context.strengthLinks()

    override fun gymLinks() = context.gymLinks()

    override fun gymProgramme() = gym.current()

    override fun gymVersions() = gym.versions()

    override fun gymDecisions() = gym.decisions()

    override fun exerciseChoices() = choices.choices()

    override fun exerciseExclusions() = exclusions.exclusions()

    override fun acknowledgedPbs() = choices.acknowledged()

    override fun healthAvailability() = platform.healthAvailability()

    override suspend fun grantedHealthPermissions() = platform.grantedHealthPermissions()
}
