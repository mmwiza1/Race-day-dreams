package org.racedaydreams.coach.application

import kotlinx.coroutines.CoroutineScope
import org.racedaydreams.domain.activity.*
import org.racedaydreams.domain.baseline.BaselineRepository
import org.racedaydreams.domain.coaching.*
import org.racedaydreams.domain.settings.SettingsRepository
import org.racedaydreams.domain.strength.StrengthProgrammingRepository
import org.racedaydreams.domain.time.*
import org.racedaydreams.health.HealthGateway
import org.racedaydreams.storage.*

/** Borrowed feature repositories for the application's ONE database, not a second storage owner. */
class CoachRepositories(
    val activities: ActivityRepository,
    val results: ResultRepository,
    val shoes: ShoeRepository,
    val baseline: BaselineRepository,
    val settings: SettingsRepository,
    val planned: PlannedWorkoutRepository,
    val coaching: CoachingRepository,
    val programming: StrengthProgrammingRepository,
)

/** Real platform facts/effects required by the existing application; there are no fake defaults. */
class CoachPlatformBindings<File>(
    val snapshot: CoachSnapshotPlatform,
    val healthGateway: HealthGateway,
    val bodyPermissions: Set<String>,
    val syncBodyMeasurements: suspend () -> String,
    val settings: SettingsPlatform<File>,
    val gym: SharedGymProgrammeOperations,
    val localNoon: (LocalDate) -> Pair<Instant, ZoneOffset>,
    val runTiming: (LocalDate, String, Duration) -> RunningEvidenceCoordinator.Timing,
    val shortDate: (LocalDate) -> String,
    val recordingActive: () -> Boolean,
)

/**
 * Actual cross-feature composition used by Android and available to the native app host. All
 * coordinators publish through ONE runner/snapshot; the host still owns lifecycle, IO and SDKs.
 * Constructing this root must not open/read storage or start services on the UI thread.
 */
class SharedCoachApplicationServices<File>(
    database: SqlDatabase,
    scope: CoroutineScope,
    savedDraft: () -> String?,
    repositories: CoachRepositories,
    platform: CoachPlatformBindings<File>,
    now: () -> Instant,
    today: () -> LocalDate,
    newId: () -> String,
) {
    private val profiles = SqliteAthleteProfile(database)
    private val context = SqliteCoachingContext(database, now, newId)
    private val support = SqliteRunningSupportArchives(database)
    private val snapshot =
        SharedCoachSnapshotReader(
            repositories.activities,
            repositories.results,
            repositories.shoes,
            repositories.baseline,
            repositories.settings,
            repositories.planned,
            SqliteCoachSnapshotSources(database, now, newId, platform.snapshot),
            repositories.coaching,
            savedDraft,
            repositories.programming,
            now,
            today,
        )
    val operations = CoachOperationRunner(scope, snapshot::read)
    val profile = ProfileOperations(profiles, repositories.settings)
    val baseline =
        SharedBaselineCoordinator(
            operations,
            repositories.baseline,
            repositories.settings,
            now,
            newId,
        )
    val runningPlan =
        RunningPlanCoordinator(
            support,
            operations,
            repositories.activities,
            profiles,
            repositories.coaching,
            context,
            now,
            today,
            newId,
            platform.shortDate,
        )
    val runningEvidence =
        RunningEvidenceCoordinator(
            support,
            operations,
            repositories.activities,
            repositories.results,
            repositories.shoes,
            repositories.coaching,
            now,
            today,
            newId,
            platform.runTiming,
        )
    val strength =
        StrengthCoordinator(
            SqliteLegacyGymProgrammes(database, now, newId),
            SqliteStrengthChoices(database),
            SqliteExerciseExclusions(database),
            operations,
            repositories.coaching,
            context,
            platform.gym,
            now,
            today,
        )
    val progress = ProgressCoordinator(operations, context, today, platform.localNoon, newId)
    val health =
        HealthCoordinator(
            operations,
            platform.healthGateway,
            repositories.activities,
            repositories.settings,
            platform.bodyPermissions,
            platform.syncBodyMeasurements,
        )
    val settings =
        SettingsCoordinator(
            operations,
            repositories.settings,
            SqlitePrimaryFocus(repositories.settings),
            platform.settings,
            platform.recordingActive,
        )
}
