package org.racedaydreams.coach.application

import android.app.Application
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.GymProgrammeStore
import org.racedaydreams.data.Release08Store
import org.racedaydreams.data.SqlitePrimaryFocusRepository
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.data.StrengthUserStore
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.recording.RunRecordingService
import org.racedaydreams.reminders.EveningReminders

/** Composition root only: features share a helper/command boundary, not business ownership. */
class CoachApplicationServices(app: Application, scope: CoroutineScope, savedDraft: () -> String?) {
    private val database = AndroidSqlConnection.forApplication(app)
    private val store = FeatureStore(app)
    private val coaching = CoachingStore(app)
    private val development = DevelopmentStore(app)
    private val profiles = SqliteUserPreferencesRepository(app)
    private val reminders = EveningReminders(app, store)
    private val release = Release08Store(app)
    private val gym = GymProgrammeStore(app)
    private val choices = StrengthUserStore(app)
    private val focus = SqlitePrimaryFocusRepository(store.settingsRepository())
    val healthGateway = HealthConnectGateway(app)
    private val snapshot =
        SharedCoachSnapshotReader(
            store.activityRepository(),
            store.resultRepository(),
            store.shoeRepository(),
            store.baselineRepository(),
            store.settingsRepository(),
            store.plannedWorkoutRepository(),
            SqliteCoachSnapshotSources(
                database,
                Instant::now,
                { UUID.randomUUID().toString() },
                object : CoachSnapshotPlatform {
                    override fun importWindow() =
                        HealthConnectGateway.importWindow(
                            store.activityRepository().importToken(HealthConnectGateway.PROVIDER)
                        )

                    override fun reconcileReminders() = reminders.reconcile()

                    override fun reminderPreferences() = reminders.preferences().shared()

                    override fun healthAvailability() = healthGateway.availability()

                    override suspend fun grantedHealthPermissions() =
                        healthGateway.grantedPermissions()
                },
            ),
            coaching,
            savedDraft,
            org.racedaydreams.data.SqliteStrengthProgrammingRepository(app),
            Instant::now,
            LocalDate::now,
        )
    val operations = CoachOperationRunner(scope, snapshot::read)
    val profile =
        ProfileOperations(
            org.racedaydreams.storage.SqliteAthleteProfile(database),
            store.settingsRepository(),
        )
    val baseline =
        BaselineCoordinator(operations, store.baselineRepository(), store.settingsRepository())
    val runningPlan =
        RunningPlanCoordinator(
            release,
            operations,
            store.activityRepository(),
            profiles,
            coaching,
            development,
        )
    val runningEvidence =
        RunningEvidenceCoordinator(
            release,
            operations,
            store.activityRepository(),
            store.resultRepository(),
            store.shoeRepository(),
            coaching,
        )
    val strength = StrengthCoordinator(gym, choices, operations, coaching, development)
    val progress = ProgressCoordinator(operations, development)
    val health =
        HealthCoordinator(
            operations,
            healthGateway,
            store.activityRepository(),
            store.settingsRepository(),
            development,
            org.racedaydreams.data.SqliteBodyMeasurementsRepository(app),
        )
    val settings =
        SettingsCoordinator(
            app,
            operations,
            store.backupRepository(),
            store.settingsRepository(),
            focus,
            profiles,
            reminders,
            { RunRecordingService.running.value },
        )
}
