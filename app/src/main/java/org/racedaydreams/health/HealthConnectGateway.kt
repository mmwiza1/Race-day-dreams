package org.racedaydreams.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.WeightRecord
import org.racedaydreams.data.DevelopmentStore
import org.racedaydreams.domain.body.BodyWeight
import java.time.ZoneOffset
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.racedaydreams.domain.activity.ActivityLap
import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.health.HealthImportPolicy
import org.racedaydreams.domain.health.HealthImportRepository
import org.racedaydreams.domain.health.HealthReadPolicy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

/**
 * Read-only Health Connect adapter. Uses the official Android Health sample as an API reference;
 * all mapping/synchronization logic here is original. It never writes to Health Connect or sends
 * records off-device. A complete read is staged before its transaction/checkpoint is committed.
 */
class HealthConnectGateway(
    context: Context,
    private val clock: Clock = Clock.systemUTC()
) : HealthGateway {
    private val context = context.applicationContext
    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(this.context) }

    override fun availability(): HealthAvailability = try {
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.UPDATE_REQUIRED
            else -> HealthAvailability.UNAVAILABLE
        }
    } catch (_: RuntimeException) { HealthAvailability.UNAVAILABLE }

    override suspend fun grantedPermissions(): Set<String> {
        if (availability() != HealthAvailability.AVAILABLE) return emptySet()
        return try { client.permissionController.getGrantedPermissions() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { emptySet() }
    }

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    fun historyPermissionAvailable(): Boolean = availability() == HealthAvailability.AVAILABLE &&
        runCatching {
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        }.getOrDefault(false)

    fun providerUpdateIntent(): Intent = Intent(Intent.ACTION_VIEW,
        Uri.parse("market://details?id=com.google.android.apps.healthdata"))

    fun settingsIntent(): Intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

    /** Optional measurement feature; a completed bounded read replaces only its provider window. */
    suspend fun syncWeights(store: DevelopmentStore) = withContext(Dispatchers.IO) {
        if (WEIGHT_PERMISSION !in grantedPermissions()) return@withContext
        val end=clock.instant()
        val start=end.minus(Duration.ofDays(if(HISTORY_PERMISSION in grantedPermissions()) 84 else 29))
        val weights=mutableListOf<BodyWeight>();var page:String?=null;var pages=0
        do {
            check(++pages<=MAX_PAGES) { "Weight history exceeds this import limit." }
            val response=client.readRecords(ReadRecordsRequest(recordType=WeightRecord::class,timeRangeFilter=TimeRangeFilter.between(start,end),pageSize=500,pageToken=page))
            response.records.forEach { r -> weights.add(BodyWeight("hc-weight:${r.metadata.id}",r.weight.inKilograms,r.time,r.zoneOffset?:ZoneOffset.UTC,PROVIDER,r.metadata.id,r.metadata.dataOrigin.packageName)) }
            page=response.pageToken
        } while(page!=null)
        check(WEIGHT_PERMISSION in grantedPermissions()) { "Weight permission changed; no measurements were replaced." }
        store.replaceWeightWindow(PROVIDER,start,end,weights)
    }

    override suspend fun sync(store: HealthImportRepository): HealthSyncResult =
        withContext(Dispatchers.IO) {
            SYNC_LOCK.withLock { syncLocked(store) }
        }

    private suspend fun syncLocked(store: HealthImportRepository): HealthSyncResult {
        when (availability()) {
            HealthAvailability.UNAVAILABLE -> return result(HealthSyncStatus.UNAVAILABLE,
                "Health Connect is unavailable on this device. Manual setup remains available.")
            HealthAvailability.UPDATE_REQUIRED -> return result(HealthSyncStatus.UPDATE_REQUIRED,
                "Health Connect needs installation or an update. You can continue manually.")
            HealthAvailability.AVAILABLE -> Unit
        }
        val granted = grantedPermissions()
        if (EXERCISE_PERMISSION !in granted) return result(HealthSyncStatus.PERMISSION_REQUIRED,
            "Exercise-session access was not granted. Your existing data is unchanged; manual setup remains available.")

        return try {
            val hasDistance = DISTANCE_PERMISSION in granted
            val hasHeartRate = HEART_RATE_PERMISSION in granted
            val hasHistory = HISTORY_PERMISSION in granted
            val now = clock.instant()
            // ReadRecords silently excludes inaccessible history. Use an honest, fully authorized
            // window rather than claiming twelve complete weeks when only recent access exists.
            val start = HealthReadPolicy.windowStart(now, hasHistory)
            val relevantPermissions = granted.intersect(ALL_PERMISSIONS + HISTORY_PERMISSION)
            val existing = store.activities().filter { it.source().provider() == PROVIDER }
            val oldCheckpoint = Checkpoint.decode(store.importToken(PROVIDER))
                ?.takeIf { it.permissions == relevantPermissions }
            var token: String
            var resync = oldCheckpoint == null
            val explicitDeletions = linkedSetOf<String>()
            val changedSessions = linkedMapOf<String, ExerciseSessionRecord>()
            var refreshOlderMetrics = false
            if (oldCheckpoint != null) {
                var next = oldCheckpoint.token
                var expired = false
                var pages = 0
                do {
                    if (++pages > MAX_PAGES) throw IllegalStateException("Too many Health Connect change pages.")
                    val changes = client.getChanges(next)
                    if (changes.changesTokenExpired) { expired = true; break }
                    for (change in changes.changes) when (change) {
                        is DeletionChange -> {
                            explicitDeletions.add(change.recordId)
                            changedSessions.remove(change.recordId)
                            // Deleted changes do not include record type/time. Current-window
                            // sessions are refreshed below; older retained sessions need refresh
                            // too when an unknown (possibly distance/HR) source record disappeared.
                            if (existing.none { it.source().recordId() == change.recordId }) refreshOlderMetrics = true
                        }
                        is UpsertionChange -> when (val record = change.record) {
                            is ExerciseSessionRecord -> if (isRunning(record) && record.endTime <= now) {
                                changedSessions[record.metadata.id] = record
                                explicitDeletions.remove(record.metadata.id)
                            } else explicitDeletions.add(record.metadata.id)
                            is DistanceRecord, is HeartRateRecord -> refreshOlderMetrics = true
                        }
                    }
                    next = changes.nextChangesToken
                } while (changes.hasMore)
                token = if (expired) {
                    // Acquire before the fresh snapshot: concurrent changes are replayed next time.
                    changedSessions.clear()
                    explicitDeletions.clear()
                    refreshOlderMetrics = hasHistory
                    resync = true
                    client.getChangesToken(ChangesTokenRequest(recordTypes(granted)))
                } else next
            } else {
                token = client.getChangesToken(ChangesTokenRequest(recordTypes(granted)))
            }

            // With extended access, an expired/re-scoped checkpoint reconciles every record this
            // app retained, including records now older than the baseline window. Without that
            // access older records are kept and the limitation is explicitly reported.
            val queryStart = if (resync && hasHistory) {
                existing.minOfOrNull { it.start() }?.let { minOf(it, start) } ?: start
            } else start
            val sessions = readSessions(queryStart, now).filter { isRunning(it) && it.endTime <= now }
                .associateBy { it.metadata.id }.toMutableMap()
            // The valid changes feed can surface updated sessions outside the initial window.
            // Never add a future session or silently turn a non-running session into a run.
            for ((id, session) in changedSessions) if (session.endTime <= now) sessions[id] = session
            val previous = existing.associateBy { it.source().recordId() }
            val staged = mutableListOf<ActivityRecord>()
            for (session in sessions.values) {
                staged.add(mapSession(session, hasDistance, hasHeartRate, previous[session.metadata.id]))
            }
            var olderUnavailable = resync && !hasHistory && existing.any { it.start().isBefore(start) }
            if (refreshOlderMetrics) {
                for (old in existing) {
                    if (old.source().recordId() in sessions || old.source().recordId() in explicitDeletions) continue
                    if (!old.start().isBefore(start)) continue
                    if (!hasHistory) { olderUnavailable = true; continue }
                    try { staged.add(refreshMetrics(old, hasDistance, hasHeartRate)) }
                    catch (_: SecurityException) { olderUnavailable = true }
                }
            }
            // Revocation during the read must not write a partial snapshot or advance its token.
            val stillGranted = grantedPermissions()
            if (!stillGranted.containsAll(relevantPermissions)) return result(HealthSyncStatus.PERMISSION_REQUIRED,
                "Health permissions changed during import. Nothing was committed; you can retry or continue manually.")
            val checkpoint = Checkpoint(token, relevantPermissions, start, now).encode()
            val batch = HealthImportPolicy.Batch(PROVIDER, HealthImportPolicy.unique(staged),
                explicitDeletions.toList(), checkpoint, queryStart, now, true)
            HealthImportPolicy.commit(store, batch)
            val partial = !hasDistance || olderUnavailable
            val windowText = if (hasHistory) "up to 12 weeks" else "the last 29 days (extended history is not enabled)"
            val recentCount = sessions.values.count { it.startTime >= start }
            val parts = mutableListOf("Found $recentCount running sessions from $windowText. Review the recorded baseline before using it.")
            if (!hasDistance) parts.add("Distance permission is missing; unavailable distances remain blank. Previously imported distance is retained.")
            if (!hasHeartRate) parts.add("Heart rate is optional and was not refreshed.")
            if (olderUnavailable) parts.add("Some older associated metrics could not be refreshed with the current history access and were retained.")
            parts.add("Session totals do not provide continuous samples for kilometre splits or rolling personal bests.")
            HealthSyncResult(if (partial) HealthSyncStatus.PARTIAL else HealthSyncStatus.SUCCESS,
                recentCount, start, now, parts.joinToString(" "))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            result(HealthSyncStatus.PERMISSION_REQUIRED,
                "Health Connect did not allow this read. No import/checkpoint was committed. Review permissions or continue manually.")
        } catch (_: Exception) {
            // Do not log health content, provider IDs, checkpoints or sensitive exception messages.
            result(HealthSyncStatus.FAILED,
                "Health Connect could not finish the import. Your existing data and checkpoint are unchanged. Try again or continue manually.")
        }
    }

    private suspend fun readSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var page: String? = null
        var pages = 0
        do {
            if (++pages > MAX_PAGES) throw IllegalStateException("Too many Health Connect record pages.")
            val response = client.readRecords(ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 500,
                pageToken = page
            ))
            sessions.addAll(response.records)
            if (sessions.size > MAX_SESSIONS) throw IllegalStateException("The readable history is too large for one import.")
            page = response.pageToken
        } while (page != null)
        return sessions
    }

    private suspend fun mapSession(session: ExerciseSessionRecord, distanceAllowed: Boolean,
                                   heartRateAllowed: Boolean, previous: ActivityRecord?): ActivityRecord {
        val skeleton = ActivityRecord(
            "$PROVIDER:${session.metadata.id}",
            SourceIdentity(PROVIDER, session.metadata.id, session.metadata.dataOrigin.packageName),
            session.startTime, session.endTime, session.startZoneOffset, session.endZoneOffset,
            previous?.distanceMetres(), null, previous?.averageHeartRate(), previous?.maxHeartRate(),
            null, null, null, session.notes, emptyList(),
            session.laps.mapIndexed { index, lap -> ActivityLap("${session.metadata.id}:lap:$index",
                lap.startTime, lap.endTime, lap.length?.inMeters, "Lap ${index + 1}", null) },
            null, null)
        return refreshMetrics(skeleton, distanceAllowed, heartRateAllowed)
    }

    private suspend fun refreshMetrics(activity: ActivityRecord, distanceAllowed: Boolean,
                                       heartRateAllowed: Boolean): ActivityRecord {
        // Restrict related measurements to the session's declared writer. Combining every source
        // can double count mirrored/synchronized distance, and missing same-source data is unknown.
        val origins = activity.source().originPackage()?.takeIf { it.isNotBlank() }
            ?.let { setOf(DataOrigin(it)) } ?: emptySet()
        val request = AggregateRequest(
            metrics = buildSet {
                if (distanceAllowed) add(DistanceRecord.DISTANCE_TOTAL)
                if (heartRateAllowed) { add(HeartRateRecord.BPM_AVG); add(HeartRateRecord.BPM_MAX) }
            },
            timeRangeFilter = TimeRangeFilter.between(activity.start(), activity.end()),
            dataOriginFilter = origins)
        if (!distanceAllowed && !heartRateAllowed) return activity
        val aggregate = client.aggregate(request)
        val distance = if (distanceAllowed) aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters else activity.distanceMetres()
        val average = if (heartRateAllowed) aggregate[HeartRateRecord.BPM_AVG]?.toDouble() else activity.averageHeartRate()
        val maximum = if (heartRateAllowed) aggregate[HeartRateRecord.BPM_MAX]?.toDouble() else activity.maxHeartRate()
        return ActivityRecord(activity.id(), activity.source(), activity.start(), activity.end(),
            activity.startOffset(), activity.endOffset(), distance, null, average, maximum,
            null, null, null, activity.notes(), emptyList(), activity.laps(),
            activity.plannedWorkoutId(), activity.shoeId())
    }

    private fun recordTypes(granted: Set<String>): Set<KClass<out Record>> = buildSet {
        add(ExerciseSessionRecord::class)
        if (DISTANCE_PERMISSION in granted) add(DistanceRecord::class)
        if (HEART_RATE_PERMISSION in granted) add(HeartRateRecord::class)
    }

    private data class Checkpoint(val token: String, val permissions: Set<String>,
                                  val windowStart: Instant?, val windowEnd: Instant?) {
        fun encode(): String = JSONObject().put("version", 1).put("token", token)
            .put("permissions", JSONArray(permissions.sorted()))
            .put("windowStart", windowStart?.toString())
            .put("windowEnd", windowEnd?.toString()).toString()
        companion object {
            fun decode(value: String?): Checkpoint? = try {
                if (value == null) null else JSONObject(value).let { json ->
                    if (json.get("version") != 1) null else {
                        val token = json.getString("token")
                        val from = Instant.parse(json.getString("windowStart"))
                        val to = Instant.parse(json.getString("windowEnd"))
                        if (token.isBlank() || !to.isAfter(from)) null else Checkpoint(token,
                            json.getJSONArray("permissions").let { list ->
                                (0 until list.length()).map { list.getString(it) }.toSet()
                            }, from, to)
                    }
                }
            } catch (_: Exception) { null }
        }
    }

    companion object {
        const val PROVIDER = "health-connect"
        // Multiple Activity/ViewModel instances must not race provider checkpoints.
        private val SYNC_LOCK = Mutex()
        private const val MAX_PAGES = 1000
        private const val MAX_SESSIONS = 10000
        val WEIGHT_PERMISSION: String = HealthPermission.getReadPermission(WeightRecord::class)
        val EXERCISE_PERMISSION: String = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        val DISTANCE_PERMISSION: String = HealthPermission.getReadPermission(DistanceRecord::class)
        val HEART_RATE_PERMISSION: String = HealthPermission.getReadPermission(HeartRateRecord::class)
        const val HISTORY_PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
        val BASELINE_PERMISSIONS: Set<String> = setOf(EXERCISE_PERMISSION, DISTANCE_PERMISSION)
        val OPTIONAL_PERMISSIONS: Set<String> = setOf(HEART_RATE_PERMISSION)
        val ALL_PERMISSIONS: Set<String> = BASELINE_PERMISSIONS + OPTIONAL_PERMISSIONS

        /** Covered baseline window is committed atomically with the opaque provider checkpoint. */
        fun importWindow(checkpoint: String?): Pair<Instant, Instant>? = Checkpoint.decode(checkpoint)?.let {
            if (it.windowStart == null || it.windowEnd == null) null else it.windowStart to it.windowEnd
        }

        private fun isRunning(session: ExerciseSessionRecord): Boolean =
            session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING ||
                session.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL

        private fun result(status: HealthSyncStatus, message: String) = HealthSyncResult(status, message = message)
    }
}
