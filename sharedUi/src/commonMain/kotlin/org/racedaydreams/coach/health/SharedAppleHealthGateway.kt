package org.racedaydreams.coach.health

import kotlinx.coroutines.withContext
import org.racedaydreams.coach.application.applicationIoDispatcher
import org.racedaydreams.domain.activity.ActivityRepository
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.health.*
import org.racedaydreams.health.body.*
import org.racedaydreams.health.workouts.*
import org.racedaydreams.storage.SqlDatabase
import org.racedaydreams.storage.SqliteHealthKitBodyImport

/** Native SDK evidence only. These methods never claim that Apple disclosed a read grant. */
interface AppleHealthReadSource {
    fun available(): Boolean

    suspend fun workouts(window: BodyReadWindow): List<HealthKitRun>

    suspend fun body(window: BodyReadWindow, metrics: Set<BodyMetric>): BodyReadSnapshot
}

/** Actual provider import orchestration; the native facade owns SDK queries and request sheets. */
class SharedAppleHealthGateway(
    private val database: SqlDatabase,
    private val activities: ActivityRepository,
    private val bundleIdentifier: String,
    private val source: AppleHealthReadSource,
    private val readWindow: () -> BodyReadWindow,
    private val selectedWorkouts: () -> Boolean,
    private val selectedBodyMetrics: () -> Set<BodyMetric>,
    private val now: () -> Instant,
    newId: () -> String,
) : HealthGateway {
    private val bodyImport = SqliteHealthKitBodyImport(database, now, newId)

    init {
        require(bundleIdentifier.isNotBlank()) { "The application bundle identity is required." }
    }

    override fun availability() =
        if (source.available()) HealthAvailability.AVAILABLE else HealthAvailability.UNAVAILABLE

    /** Unknown read permission is not a grant and is not a claim that access was refused. */
    override suspend fun grantedPermissions(): Set<String> = emptySet()

    override suspend fun sync(store: HealthImportRepository): HealthSyncResult {
        require(store === activities) { "Health import must use the application's own repository." }
        if (availability() != HealthAvailability.AVAILABLE)
            return HealthSyncResult(
                HealthSyncStatus.UNAVAILABLE,
                message =
                    "Apple Health is unavailable. Manual details and retained training remain available.",
            )
        val wantWorkouts = selectedWorkouts()
        val metrics = selectedBodyMetrics().toSet()
        if (!wantWorkouts && metrics.isEmpty())
            return HealthSyncResult(
                HealthSyncStatus.PERMISSION_REQUIRED,
                message =
                    "Choose which Apple Health data you want to read. No history has been changed; manual details remain available.",
            )
        val window = readWindow()
        val runs =
            if (wantWorkouts) {
                val result = source.workouts(window)
                require(result.size <= HealthKitRunningImport.MAX_RECORDS) {
                    "Choose a shorter workout read window; nothing was truncated."
                }
                Immutable.copyList(result)
            } else null
        // Stage every selected read before any SQL. SDK failure leaves the whole selection intact.
        val measurements = if (metrics.isEmpty()) null else source.body(window, metrics)
        check(
            measurements == null ||
                measurements.completeness == ReadCompleteness.VISIBLE_RECORDS_ONLY &&
                    measurements.metrics == metrics &&
                    measurements.window.from == window.from &&
                    measurements.window.until == window.until &&
                    measurements.records.all {
                        it.provider == "health-kit" && it.offsetSeconds == null
                    }
        ) {
            "Apple Health returned evidence outside the selected read scope. History is unchanged."
        }
        val importedAt = now()
        val heights =
            measurements
                ?.records
                .orEmpty()
                .filter { it.metric == BodyMetric.HEIGHT_METRES }
                .map { BodyEvidence.height(it, "hk-height:${it.sourceId}", importedAt) }
        val weights =
            measurements
                ?.records
                .orEmpty()
                .filter { it.metric == BodyMetric.WEIGHT_KILOGRAMS }
                .map { BodyEvidence.weight(it, "hk-weight:${it.sourceId}") }
        val count =
            withContext(applicationIoDispatcher()) {
                database.transaction {
                    val batch =
                        runs?.let {
                            HealthKitRunningImport.batch(
                                window,
                                bundleIdentifier,
                                it,
                                activities.activities(),
                                activities.importToken("health-kit"),
                            )
                        }
                    if (batch != null) HealthImportPolicy.commit(activities, batch)
                    bodyImport.merge(heights, weights)
                    batch?.upserts()?.size ?: 0
                }
            }
        val summaries =
            listOfNotNull(
                if (wantWorkouts) "$count available workouts" else null,
                if (metrics.isNotEmpty()) "${heights.size + weights.size} body measurements"
                else null,
            )
        // No authoritative-window fields: a visible-only read cannot prove complete coverage.
        return HealthSyncResult(
            HealthSyncStatus.PARTIAL,
            importedCount = count,
            message =
                "Read ${summaries.joinToString(" and ")} from Apple Health. Existing history was kept. Apple does not confirm read access or complete coverage." +
                    if (weights.isNotEmpty())
                        " Choose a weight explicitly before using protein guidance."
                    else "",
        )
    }
}
