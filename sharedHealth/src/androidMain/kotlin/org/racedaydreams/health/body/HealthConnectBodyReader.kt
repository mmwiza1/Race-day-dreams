package org.racedaydreams.health.body

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant

/** Only SDK mapping lives here; shared policy stages pages and rechecks actual read grants. */
class HealthConnectBodyReader(private val client: HealthConnectClient) : BodyMeasurementReader {
    private val permissionByMetric =
        mapOf(
            BodyMetric.HEIGHT_METRES to HealthPermission.getReadPermission(HeightRecord::class),
            BodyMetric.WEIGHT_KILOGRAMS to HealthPermission.getReadPermission(WeightRecord::class),
        )
    private val reader =
        AuthorizedBodyReader(
            object : AuthorizedBodyPageSource {
                override suspend fun grantedMetrics(): Set<BodyMetric> {
                    val granted = client.permissionController.getGrantedPermissions()
                    return permissionByMetric.filterValues { it in granted }.keys
                }

                override suspend fun page(
                    window: BodyReadWindow,
                    metric: BodyMetric,
                    token: String?,
                ): BodyReadPage {
                    val range =
                        TimeRangeFilter.between(
                            window.from.toJavaInstant(),
                            window.until.toJavaInstant(),
                        )
                    return if (metric == BodyMetric.HEIGHT_METRES) {
                        val response =
                            client.readRecords(
                                ReadRecordsRequest(
                                    HeightRecord::class,
                                    range,
                                    pageSize = 500,
                                    pageToken = token,
                                )
                            )
                        BodyReadPage(
                            response.records.map { record ->
                                BodyObservation(
                                    metric,
                                    record.height.inMeters,
                                    record.time.toKotlinInstant(),
                                    record.zoneOffset?.totalSeconds,
                                    "health-connect",
                                    record.metadata.id,
                                    record.metadata.dataOrigin.packageName,
                                )
                            },
                            response.pageToken,
                        )
                    } else {
                        val response =
                            client.readRecords(
                                ReadRecordsRequest(
                                    WeightRecord::class,
                                    range,
                                    pageSize = 500,
                                    pageToken = token,
                                )
                            )
                        BodyReadPage(
                            response.records.map { record ->
                                // Keep the existing weight-import UTC fallback; heights retain
                                // unknown offset.
                                BodyObservation(
                                    metric,
                                    record.weight.inKilograms,
                                    record.time.toKotlinInstant(),
                                    record.zoneOffset?.totalSeconds ?: 0,
                                    "health-connect",
                                    record.metadata.id,
                                    record.metadata.dataOrigin.packageName,
                                )
                            },
                            response.pageToken,
                        )
                    }
                }
            }
        )

    override suspend fun read(window: BodyReadWindow, metrics: Set<BodyMetric>): BodyReadSnapshot =
        reader.read(window, metrics)
}
