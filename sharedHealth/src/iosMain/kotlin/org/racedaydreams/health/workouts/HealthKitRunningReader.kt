@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.health.workouts

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import org.racedaydreams.health.body.BodyReadWindow
import org.racedaydreams.health.body.HealthReadAuthorizationRequest
import platform.Foundation.NSSortDescriptor
import platform.HealthKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Read-only native workout adapter. Host supplies real bundle identity and stages DB reads on IO.
 */
class HealthKitRunningReader(
    private val ownBundleIdentifier: String,
    private val store: HKHealthStore = HKHealthStore(),
) {
    init {
        require(ownBundleIdentifier.isNotBlank())
    }

    fun available(): Boolean = HKHealthStore.isHealthDataAvailable()

    /** Completion of Apple's sheet does not reveal whether read access was granted. */
    @Throws(Exception::class)
    suspend fun requestReadAccess(): HealthReadAuthorizationRequest =
        withContext(Dispatchers.Main) {
            if (!available()) return@withContext HealthReadAuthorizationRequest.UNAVAILABLE
            suspendCancellableCoroutine { continuation ->
                store.requestAuthorizationToShareTypes(
                    emptySet<HKSampleType>(),
                    setOf(HKObjectType.workoutType()),
                ) { success, error ->
                    if (continuation.isActive) {
                        if (!success || error != null)
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "Workout access request did not complete. Retained training is unchanged."
                                )
                            )
                        else continuation.resume(HealthReadAuthorizationRequest.COMPLETED)
                    }
                }
            }
        }

    /** Returns visible-only SDK evidence. Does not read/write SQL or claim an authorised window. */
    @Throws(Exception::class)
    suspend fun read(window: BodyReadWindow): HealthKitRunningRead =
        withContext(Dispatchers.Main) {
            check(available()) { "Apple Health is unavailable. Retained training is unchanged." }
            HealthKitRunningRead(window, ownBundleIdentifier, query(window))
        }

    private suspend fun query(window: BodyReadWindow): List<HealthKitRun> =
        suspendCancellableCoroutine { continuation ->
            val predicate =
                HKQuery.predicateForSamplesWithStartDate(
                    window.from.toNSDate(),
                    window.until.toNSDate(),
                    HKQueryOptionStrictStartDate,
                )
            val query =
                HKSampleQuery(
                    HKObjectType.workoutType(),
                    predicate,
                    (HealthKitRunningImport.MAX_RECORDS + 1).toULong(),
                    listOf(NSSortDescriptor(HKSampleSortIdentifierStartDate, true)),
                ) { _, samples, error ->
                    if (continuation.isActive) {
                        val result = runCatching {
                            check(error == null) {
                                "Apple Health workout read failed; history is unchanged."
                            }
                            val values = samples ?: emptyList<Any>()
                            check(values.size <= HealthKitRunningImport.MAX_RECORDS) {
                                "Choose a shorter workout read window; retained history is unchanged."
                            }
                            values.mapNotNull { raw ->
                                val workout =
                                    raw as? HKWorkout
                                        ?: error("Unexpected Apple Health workout sample.")
                                val origin = workout.sourceRevision.source.bundleIdentifier
                                val start = workout.startDate.toKotlinInstant()
                                val end = workout.endDate.toKotlinInstant()
                                if (
                                    workout.workoutActivityType != HKWorkoutActivityTypeRunning ||
                                        origin == ownBundleIdentifier ||
                                        !window.contains(start) ||
                                        end > window.until
                                )
                                    null
                                else
                                    HealthKitRun(
                                        workout.UUID.UUIDString,
                                        origin,
                                        start,
                                        end,
                                        workout.totalDistance?.doubleValueForUnit(
                                            HKUnit.meterUnit()
                                        ),
                                    )
                            }
                        }
                        result.fold(
                            onSuccess = { continuation.resume(it) },
                            onFailure = { continuation.resumeWithException(it) },
                        )
                    }
                }
            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) { store.stopQuery(query) }
            }
            if (continuation.isActive) store.executeQuery(query)
        }
}
