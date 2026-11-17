package org.racedaydreams.coach.application

import java.time.Instant
import java.time.LocalDate
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.Release08Store
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.activity.ResultRepository
import org.racedaydreams.domain.activity.ShoeRepository
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.coaching.CoachingRepository
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository
import org.racedaydreams.domain.coaching.WeeklyCoaching
import org.racedaydreams.domain.settings.PrimaryFocusRepository
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.reminders.EveningReminders

/** Coherent cross-feature projection. Reads/derived proposals never accept a plan change. */
class CoachSnapshotReader(
    private val activitiesRepository: ActivityRepository,
    private val resultsRepository: ResultRepository,
    private val shoesRepository: ShoeRepository,
    private val baselineRepository: BaselineRepository,
    private val settings: org.racedaydreams.domain.settings.SettingsRepository,
    private val focus: PrimaryFocusRepository,
    private val planned: PlannedWorkoutRepository,
    private val release: Release08Store,
    private val gym: GymProgrammeStore,
    private val choices: StrengthUserStore,
    private val coaching: CoachingRepository,
    private val development: DevelopmentStore,
    private val profiles: SqliteUserPreferencesRepository,
    private val reminders: EveningReminders,
    private val health: HealthConnectGateway,
    private val savedDraft: () -> String?,
    private val programming: org.racedaydreams.domain.strength.StrengthProgrammingRepository? = null,
) {
    private val shared =
        SharedCoachSnapshotReader(
            activitiesRepository,
            resultsRepository,
            shoesRepository,
            baselineRepository,
            settings,
            planned,
            object : CoachSnapshotSources {
                override fun primaryFocus() = focus.load().orElse(null)

                override fun profile() = profiles.load().orElse(null)

                override fun profileDraft() = profiles.loadDraft().orElse(null)

                override fun importWindow() =
                    HealthConnectGateway.importWindow(
                        activitiesRepository.importToken(HealthConnectGateway.PROVIDER)
                    )

                override fun shoeDiscomfort(): Set<String> =
                    release.get("config", "shoe-feedback")?.getJSONArray("activities")?.let { a ->
                        (0 until a.length()).map { a.getString(it) }.toSet()
                    } ?: emptySet()

                override fun interruption() = development.interruption()

                override fun sessionDecisions() = development.sessionDecisions()

                override fun reviewKnown(id: String) = development.reviewKnown(id)

                override fun saveReview(review: WeeklyCoaching.Review) =
                    development.saveReview(review)

                override fun decisionForReview(review: WeeklyCoaching.Review) =
                    development.decisionForReview(review)

                override fun reconcileReminders() = reminders.reconcile()

                override fun reminderPreferences() = reminders.preferences().shared()

                override fun weights() = development.weights()

                override fun historicalStrength() = development.historical()

                override fun strengthLinks() = development.strengthLinks()

                override fun gymLinks() =
                    development.gymLinks().mapValues { (_, link) ->
                        org.racedaydreams.domain.presentation.StrengthTodaySummary.Association(
                            link.actualSessionId(),
                            link.versionKey(),
                            link.prescribedSessionId(),
                        )
                    }

                override fun gymProgramme() = gym.current()

                override fun gymVersions() = gym.versions()

                override fun gymDecisions() =
                    gym.decisions().map { decision ->
                        org.racedaydreams.storage.LegacyGymProgrammePayloadJson.Decision(
                            decision.id(),
                            decision.baseVersion(),
                            decision.resultingVersion(),
                            decision.prescribedSession(),
                            decision.exerciseId(),
                            decision.choice(),
                            decision.evidence(),
                            decision.at(),
                        )
                    }

                override fun exerciseChoices() =
                    choices.choices().map { choice ->
                        org.racedaydreams.storage.SqliteStrengthChoices.Choice(
                            choice.exerciseId(),
                            choice.favourite(),
                            choice.archived(),
                        )
                    }

                override fun exerciseExclusions() = choices.exclusions()

                override fun acknowledgedPbs() = choices.acknowledged()

                override fun healthAvailability() = health.availability()

                override suspend fun grantedHealthPermissions() = health.grantedPermissions()
            },
            coaching,
            savedDraft,
            programming,
            Instant::now,
            LocalDate::now,
        )

    suspend fun read(): CoachUiState = shared.read()
}
