package org.racedaydreams.health.body

import kotlin.test.*
import kotlinx.datetime.Instant

class BodyReadSnapshotTest {
    private val start = Instant.parse("2026-01-01T00:00:00Z")
    private val end = Instant.parse("2026-02-01T00:00:00Z")
    private val window = BodyReadWindow(start, end)

    private fun weight(value: Double = 87.0, at: Instant = start) =
        BodyObservation(
            BodyMetric.WEIGHT_KILOGRAMS,
            value,
            at,
            null,
            "provider",
            "sample",
            "origin",
        )

    @Test
    fun authorizedEmptyAndUnrequestedMetricsRemainDistinct() {
        val snapshot =
            BodyReadSnapshot(
                window,
                setOf(BodyMetric.WEIGHT_KILOGRAMS),
                emptyList(),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        assertTrue(snapshot.recordsForReplacement(BodyMetric.WEIGHT_KILOGRAMS).isEmpty())
        assertFailsWith<IllegalArgumentException> {
            snapshot.recordsForReplacement(BodyMetric.HEIGHT_METRES)
        }
    }

    @Test
    fun healthKitEmptyReadCannotErasePreviouslyRetainedRecords() {
        val snapshot =
            BodyReadSnapshot(
                window,
                setOf(BodyMetric.WEIGHT_KILOGRAMS),
                emptyList(),
                ReadCompleteness.VISIBLE_RECORDS_ONLY,
            )
        assertFailsWith<IllegalStateException> {
            snapshot.recordsForReplacement(BodyMetric.WEIGHT_KILOGRAMS)
        }
    }

    @Test
    fun changedDuplicateRejectsTheWholeReadWhileIdenticalDuplicatesDeduplicate() {
        assertFailsWith<IllegalStateException> {
            BodyReadSnapshot(
                window,
                setOf(BodyMetric.WEIGHT_KILOGRAMS),
                listOf(weight(), weight(88.0)),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        }
        val snapshot =
            BodyReadSnapshot(
                window,
                setOf(BodyMetric.WEIGHT_KILOGRAMS),
                listOf(weight(), weight()),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        assertEquals(1, snapshot.records.size)
    }

    @Test
    fun providerIdentityIsQualifiedByMeasurementType() {
        val height = weight().copy(metric = BodyMetric.HEIGHT_METRES, value = 1.8)
        val snapshot =
            BodyReadSnapshot(
                window,
                BodyMetric.entries.toSet(),
                listOf(weight(), height),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        assertEquals(2, snapshot.records.size)
    }

    @Test
    fun sourceCollectionsCannotRetroactivelyChangeStagedEvidence() {
        val records = mutableListOf(weight())
        val metrics = mutableSetOf(BodyMetric.WEIGHT_KILOGRAMS)
        val snapshot =
            BodyReadSnapshot(window, metrics, records, ReadCompleteness.AUTHORIZED_WINDOW)
        records.clear()
        metrics.clear()
        assertEquals(listOf(weight()), snapshot.records)
        assertEquals(setOf(BodyMetric.WEIGHT_KILOGRAMS), snapshot.metrics)
    }

    @Test
    fun exclusiveEndAndMetricBoundariesAreEnforced() {
        assertFailsWith<IllegalArgumentException> {
            BodyReadSnapshot(
                window,
                setOf(BodyMetric.WEIGHT_KILOGRAMS),
                listOf(weight(at = end)),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BodyReadSnapshot(
                window,
                emptySet(),
                listOf(weight()),
                ReadCompleteness.AUTHORIZED_WINDOW,
            )
        }
    }
}
