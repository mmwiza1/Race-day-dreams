package org.racedaydreams.domain

import java.time.Instant
import kotlin.test.*
import org.racedaydreams.domain.baseline.*

class BaselineRawRecordCompatibilityTest {
    @Suppress("UNCHECKED_CAST")
    @Test
    fun originalSummaryAndDecisionSnapshotsRetainRawNullableEntries() {
        val source = mutableListOf<String?>("known", null)
        val raw = source as List<String>
        val summary = AssessmentSummary(300.0, null, null, null, null, null, null, 1.0, raw)
        val recommendation = AssessmentRecommendation(1, false, raw)
        val decision = EligibilityDecision(false, raw, Instant.EPOCH)
        source.clear()
        for (rows in listOf(summary.findings(), recommendation.reasons(), decision.reasons())) {
            assertEquals(2, rows.size)
            assertNull(rows[1])
            assertFails { (rows as MutableList).clear() }
        }
        assertNull(summary.confidence())
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun baselineMapPreservesRawNullsOrderAndSnapshotOwnership() {
        val map =
            linkedMapOf<String?, BaselineProvenance?>(
                "known" to BaselineProvenance.MANUAL,
                null to null,
            )
        val b =
            RunnerBaseline(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1),
                null,
                BaselineConfidence.LOW,
                map as Map<String, BaselineProvenance>,
                emptyList(),
                emptyList(),
                emptyList(),
            )
        map.clear()
        assertEquals(listOf("known", null), b.provenance().keys.toList())
        assertNull((b.provenance() as Map<String?, BaselineProvenance?>)[null])
        val iterator = b.provenance().entries.iterator()
        iterator.next()
        assertFails {
            (iterator as MutableIterator<Map.Entry<String, BaselineProvenance>>).remove()
        }
        assertEquals(2, b.provenance().size)
        assertFails { (b.provenance() as MutableMap).clear() }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun baselineStrictListsStillRejectNullMembersInsteadOfDroppingThem() {
        val raw = listOf<String?>(null) as List<String>
        assertFailsWith<NullPointerException> {
            RunnerBaseline(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(1),
                null,
                BaselineConfidence.LOW,
                emptyMap(),
                raw,
                emptyList(),
                emptyList(),
            )
        }
    }
}
