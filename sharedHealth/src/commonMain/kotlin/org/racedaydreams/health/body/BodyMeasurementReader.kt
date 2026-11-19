package org.racedaydreams.health.body

import kotlinx.datetime.Instant
import org.racedaydreams.domain.coaching.Immutable

enum class BodyMetric {
    HEIGHT_METRES,
    WEIGHT_KILOGRAMS,
}

/** Apple deliberately does not disclose whether read permission was denied. */
enum class ReadCompleteness {
    AUTHORIZED_WINDOW,
    VISIBLE_RECORDS_ONLY,
}

/**
 * Provider observations, never prescriptions, readiness scores or an automatically chosen weight.
 */
data class BodyObservation(
    val metric: BodyMetric,
    val value: Double,
    val at: Instant,
    val offsetSeconds: Int?,
    val provider: String,
    val sourceId: String,
    val origin: String?,
) {
    init {
        require(value.isFinite() && value > 0) { "Body measurement must be positive and finite." }
        require(provider.isNotBlank() && sourceId.isNotBlank()) {
            "A provider and source identity are required."
        }
        require(offsetSeconds == null || offsetSeconds in -64800..64800) {
            "Invalid source time offset."
        }
    }
}

class BodyReadWindow(val from: Instant, val until: Instant) {
    init {
        require(from < until) { "A non-empty health read window is required." }
    }

    fun contains(at: Instant): Boolean = at >= from && at < until
}

/**
 * A complete staged result. Visible-only snapshots must NEVER replace an imported window or imply
 * deletion: HealthKit returns no samples for denied read access as well as empty history.
 */
class BodyReadSnapshot(
    val window: BodyReadWindow,
    metrics: Set<BodyMetric>,
    records: Collection<BodyObservation>,
    val completeness: ReadCompleteness,
) {
    val metrics: Set<BodyMetric> = Immutable.copySet(metrics)
    val records: List<BodyObservation>

    init {
        val unique = linkedMapOf<Triple<String, BodyMetric, String>, BodyObservation>()
        records.forEach { record ->
            require(record.metric in metrics && window.contains(record.at)) {
                "Observation outside the requested health window."
            }
            val key = Triple(record.provider, record.metric, record.sourceId)
            val previous = unique[key]
            check(previous == null || previous == record) {
                "A health observation changed during the read; retained data must not be replaced."
            }
            unique[key] = record
        }
        this.records = Immutable.copyList(unique.values)
    }

    fun recordsForReplacement(metric: BodyMetric): List<BodyObservation> {
        check(completeness == ReadCompleteness.AUTHORIZED_WINDOW) {
            "Visible-only health results cannot replace retained history or establish deletion."
        }
        require(metric in metrics) { "This measurement was not authorized in this snapshot." }
        return Immutable.copyList(records.filter { it.metric == metric })
    }
}

enum class HealthReadAuthorizationRequest {
    COMPLETED,
    UNAVAILABLE,
}

interface BodyMeasurementReader {
    @Throws(Exception::class)
    suspend fun read(window: BodyReadWindow, metrics: Set<BodyMetric>): BodyReadSnapshot
}
