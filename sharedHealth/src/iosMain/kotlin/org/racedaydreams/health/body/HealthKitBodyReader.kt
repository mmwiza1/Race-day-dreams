@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.health.body

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSSortDescriptor
import platform.HealthKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Read-only Apple integration. Empty reads are deliberately never authoritative deletions. */
@OptIn(ExperimentalForeignApi::class)
class HealthKitBodyReader(private val store: HKHealthStore = HKHealthStore()) :
    BodyMeasurementReader {
    fun available(): Boolean = HKHealthStore.isHealthDataAvailable()

    /** COMPLETED means only that Apple's sheet completed, never that read access was granted. */
    @Throws(Exception::class)
    suspend fun requestReadAccess(metrics: Set<BodyMetric>): HealthReadAuthorizationRequest =
        withContext(Dispatchers.Main) {
            if (!available()) return@withContext HealthReadAuthorizationRequest.UNAVAILABLE
            if (metrics.isEmpty()) return@withContext HealthReadAuthorizationRequest.COMPLETED
            suspendCancellableCoroutine { continuation ->
                store.requestAuthorizationToShareTypes(
                    emptySet<HKSampleType>(),
                    metrics.map(::quantityType).toSet(),
                ) { success, error ->
                    if (continuation.isActive) {
                        if (!success || error != null)
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "Health access request did not complete. Manual measurements remain available."
                                )
                            )
                        else continuation.resume(HealthReadAuthorizationRequest.COMPLETED)
                    }
                }
            }
        }

    override suspend fun read(window: BodyReadWindow, metrics: Set<BodyMetric>): BodyReadSnapshot =
        withContext(Dispatchers.Main) {
            check(available()) {
                "Apple Health is unavailable. Manual measurements remain available."
            }
            val staged = mutableListOf<BodyObservation>()
            for (metric in metrics) staged += query(window, metric)
            BodyReadSnapshot(window, metrics, staged, ReadCompleteness.VISIBLE_RECORDS_ONLY)
        }

    private fun quantityType(metric: BodyMetric): HKQuantityType =
        checkNotNull(
            HKObjectType.quantityTypeForIdentifier(
                if (metric == BodyMetric.HEIGHT_METRES) HKQuantityTypeIdentifierHeight
                else HKQuantityTypeIdentifierBodyMass
            )
        ) {
            "Requested body measurement is unavailable on this device."
        }

    private suspend fun query(window: BodyReadWindow, metric: BodyMetric): List<BodyObservation> =
        suspendCancellableCoroutine { continuation ->
            val predicate =
                HKQuery.predicateForSamplesWithStartDate(
                    window.from.toNSDate(),
                    window.until.toNSDate(),
                    HKQueryOptionStrictStartDate,
                )
            val query =
                HKSampleQuery(
                    quantityType(metric),
                    predicate,
                    10001uL,
                    listOf(NSSortDescriptor(HKSampleSortIdentifierStartDate, true)),
                ) { _, samples, error ->
                    if (continuation.isActive) {
                        val result = runCatching {
                            check(error == null) {
                                "Apple Health read failed; retained measurements are unchanged."
                            }
                            val values = samples ?: emptyList<Any>()
                            check(values.size <= 10000) {
                                "Body history exceeds this read limit. Choose a shorter window; retained measurements are unchanged."
                            }
                            values
                                .map { raw ->
                                    val sample =
                                        raw as? HKQuantitySample
                                            ?: throw IllegalStateException(
                                                "Unexpected Apple Health sample type."
                                            )
                                    val unit =
                                        if (metric == BodyMetric.HEIGHT_METRES) HKUnit.meterUnit()
                                        else HKUnit.gramUnitWithMetricPrefix(HKMetricPrefixKilo)
                                    BodyObservation(
                                        metric,
                                        sample.quantity.doubleValueForUnit(unit),
                                        sample.startDate.toKotlinInstant(),
                                        null,
                                        "health-kit",
                                        sample.UUID.UUIDString,
                                        sample.sourceRevision.source.bundleIdentifier,
                                    )
                                }
                                .filter { window.contains(it.at) }
                        }
                        result.fold(
                            onSuccess = { continuation.resume(it) },
                            onFailure = { continuation.resumeWithException(it) },
                        )
                    }
                }
            continuation.invokeOnCancellation {
                // Query setup runs on Main; queued cancellation cannot stop before execute starts.
                dispatch_async(dispatch_get_main_queue()) { store.stopQuery(query) }
            }
            if (continuation.isActive) store.executeQuery(query)
        }
}
