package org.racedaydreams.health.body

import kotlin.test.*
import kotlinx.datetime.Instant
import org.racedaydreams.domain.time.Instants

class BodyEvidenceTest {
    private val at = Instant.parse("2026-11-13T15:30:01.123456789Z")

    private fun row(metric: BodyMetric, offset: Int? = null) =
        BodyObservation(
            metric,
            if (metric == BodyMetric.HEIGHT_METRES) 1.82 else 87.0,
            at,
            offset,
            "provider",
            "source",
            "origin",
        )

    @Test
    fun providerHeightRetainsUnknownOffsetAndImportTimestampSeparately() {
        val importedAt = Instants.ofEpochSecond(at.epochSeconds + 10)
        val actual = BodyEvidence.height(row(BodyMetric.HEIGHT_METRES), "stable-id", importedAt)
        assertEquals("stable-id", actual.id())
        assertEquals(1.82, actual.metres())
        assertEquals(at.epochSeconds, actual.at().getEpochSecond())
        assertEquals(at.nanosecondsOfSecond, actual.at().getNano())
        assertNull(actual.offset())
        assertEquals(importedAt, actual.importedAt())
        assertEquals("provider", actual.provider())
        assertEquals("source", actual.sourceId())
        assertEquals("origin", actual.origin())
    }

    @Test
    fun weightPreservesExistingUtcFallbackAndExplicitSourceOffsets() {
        val utc = BodyEvidence.weight(row(BodyMetric.WEIGHT_KILOGRAMS), "weight-id")
        val explicit = BodyEvidence.weight(row(BodyMetric.WEIGHT_KILOGRAMS, 19801), "weight-id")
        assertEquals(0, utc.offset().getTotalSeconds())
        assertEquals(19801, explicit.offset().getTotalSeconds())
        assertEquals(87.0, explicit.kilograms())
        assertEquals("provider", explicit.provider())
    }

    @Test
    fun incompatibleOrInvalidMeasurementsCannotBeRetainedAsAnotherModality() {
        assertFails { BodyEvidence.height(row(BodyMetric.WEIGHT_KILOGRAMS), "h", null) }
        assertFails { BodyEvidence.weight(row(BodyMetric.HEIGHT_METRES), "w") }
        assertFails {
            BodyEvidence.height(row(BodyMetric.HEIGHT_METRES).copy(value = 4.0), "h", null)
        }
    }
}
