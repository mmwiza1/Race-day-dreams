package org.racedaydreams.health.body

import org.racedaydreams.domain.body.BodyHeight
import org.racedaydreams.domain.body.BodyWeight
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.Instants
import org.racedaydreams.domain.time.Offsets

/** Original provider-to-retained-record conversion. Import authority stays with the caller. */
object BodyEvidence {
    fun height(record: BodyObservation, id: String, importedAt: Instant?): BodyHeight {
        require(record.metric == BodyMetric.HEIGHT_METRES) { "A height observation is required." }
        return BodyHeight(
            id,
            record.value,
            time(record),
            record.offsetSeconds?.let(Offsets::ofTotalSeconds),
            record.provider,
            record.sourceId,
            record.origin,
            importedAt,
        )
    }

    fun weight(record: BodyObservation, id: String): BodyWeight {
        require(record.metric == BodyMetric.WEIGHT_KILOGRAMS) {
            "A weight observation is required."
        }
        // Legacy retained weights require an offset: preserve the existing UTC display fallback.
        // This is not a claim that the provider supplied its original local offset.
        return BodyWeight(
            id,
            record.value,
            time(record),
            Offsets.ofTotalSeconds(record.offsetSeconds ?: 0),
            record.provider,
            record.sourceId,
            record.origin,
        )
    }

    private fun time(record: BodyObservation): Instant =
        Instants.ofEpochSecond(record.at.epochSeconds, record.at.nanosecondsOfSecond.toLong())
}
