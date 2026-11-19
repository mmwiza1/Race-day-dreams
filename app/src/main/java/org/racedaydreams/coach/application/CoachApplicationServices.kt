package org.racedaydreams.coach.application

import android.app.Application
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import org.racedaydreams.coach.shortDate
import org.racedaydreams.data.AndroidSqlConnection
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.health.HealthConnectGateway
import org.racedaydreams.recording.RunRecordingService
import org.racedaydreams.reminders.EveningReminders

/** Android SDK/lifecycle bindings; the actual feature composition and command owner are common. */
class CoachApplicationServices(app: Application, scope: CoroutineScope, savedDraft: () -> String?) {
    private val store = FeatureStore(app)
    private val coaching = CoachingStore(app)
    private val profiles = SqliteUserPreferencesRepository(app)
    private val reminders = EveningReminders(app, store)
    val healthGateway = HealthConnectGateway(app)
    private val shared =
        SharedCoachApplicationServices(
            AndroidSqlConnection.forApplication(app),
            scope,
            savedDraft,
            CoachRepositories(
                store.activityRepository(),
                store.resultRepository(),
                store.shoeRepository(),
                store.baselineRepository(),
                store.settingsRepository(),
                store.plannedWorkoutRepository(),
                coaching,
                org.racedaydreams.data.SqliteStrengthProgrammingRepository(app),
            ),
            CoachPlatformBindings(
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
                healthGateway,
                HealthConnectGateway.BODY_PERMISSIONS,
                {
                    healthGateway.syncBodyMeasurements(
                        org.racedaydreams.data.SqliteBodyMeasurementsRepository(app)
                    )
                },
                AndroidSettingsPlatform(app, store.backupRepository(), profiles, reminders),
                GymProgrammeOperations().shared,
                { date ->
                    val at = date.atTime(12, 0).atZone(ZoneId.systemDefault())
                    at.toInstant() to at.offset
                },
                { date, time, duration ->
                    val start = date.atTime(LocalTime.parse(time)).atZone(ZoneId.systemDefault())
                    val end = start.plus(duration)
                    RunningEvidenceCoordinator.Timing(
                        start.toInstant(),
                        end.toInstant(),
                        start.offset,
                        end.offset,
                    )
                },
                ::shortDate,
                { RunRecordingService.running.value },
            ),
            Instant::now,
            LocalDate::now,
            { UUID.randomUUID().toString() },
        )
    val operations = shared.operations
    val profile = shared.profile
    val baseline = BaselineCoordinator(shared.baseline)
    val runningPlan = shared.runningPlan
    val runningEvidence = shared.runningEvidence
    val strength = shared.strength
    val progress = shared.progress
    val health = shared.health
    val settings = shared.settings
}
