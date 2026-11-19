package org.racedaydreams.coach.health

import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.health.HealthGateway
import org.racedaydreams.health.body.*
import org.racedaydreams.health.workouts.HealthKitRunningReader
import org.racedaydreams.storage.SqlDatabase

/**
 * Real Apple SDK facade for the shared root. The host supplies its actual database, bundle, window
 * and athlete-selected categories. Sync does not request access or infer read grants. Selected body
 * reads are included in sync itself; no fictional body-permission tokens are needed by the root.
 */
class AppleHealthGateway(
    database: SqlDatabase,
    activities: ActivityRepository,
    bundleIdentifier: String,
    readWindow: () -> BodyReadWindow,
    selectedWorkouts: () -> Boolean,
    selectedBodyMetrics: () -> Set<BodyMetric>,
    now: () -> Instant,
    newId: () -> String,
) : HealthGateway {
    private val workouts = HealthKitRunningReader(bundleIdentifier)
    private val body = HealthKitBodyReader()
    private val shared =
        SharedAppleHealthGateway(
            database,
            activities,
            bundleIdentifier,
            object : AppleHealthReadSource {
                override fun available() = this@AppleHealthGateway.workouts.available()

                override suspend fun workouts(window: BodyReadWindow) =
                    this@AppleHealthGateway.workouts.read(window).records

                override suspend fun body(window: BodyReadWindow, metrics: Set<BodyMetric>) =
                    this@AppleHealthGateway.body.read(window, metrics)
            },
            readWindow,
            selectedWorkouts,
            selectedBodyMetrics,
            now,
            newId,
        )

    override fun availability() = shared.availability()

    override suspend fun grantedPermissions() = shared.grantedPermissions()

    override suspend fun sync(store: HealthImportRepository) = shared.sync(store)

    /** Uses only selected body categories and the same owned database/import pipeline. */
    @Throws(Exception::class) suspend fun syncBodyMeasurements() = shared.syncBodyMeasurements()

    /** Completion means only that Apple's request sheet finished, never that read access exists. */
    @Throws(Exception::class) suspend fun requestWorkoutReadAccess() = workouts.requestReadAccess()

    @Throws(Exception::class)
    suspend fun requestBodyReadAccess(metrics: Set<BodyMetric>) = body.requestReadAccess(metrics)
}
