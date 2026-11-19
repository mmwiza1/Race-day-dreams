package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.recovery.TrainingRecovery

class RecoveryRecordCompatibilityTest {
    @Test
    fun rawOptionalEvidenceIsNotFabricatedAndReviewListsKeepOriginalSnapshots() {
        val row = TrainingRecovery.Evidence(null, null, null, null)
        val rows = mutableListOf(row)
        val limitations = mutableListOf("Unknown")
        val review = TrainingRecovery.Review(null, null, null, false, false, rows, limitations)
        rows.clear()
        limitations.clear()
        assertNull(review.date())
        assertNull(review.heading())
        assertNull(review.instruction())
        assertNull(row.at())
        assertNull(row.id())
        assertNull(row.explanation())
        assertEquals(listOf(row), review.evidence())
        assertEquals(listOf("Unknown"), review.limitations())
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val javaRows = review.evidence() as java.util.List<*>
        assertFailsWith<UnsupportedOperationException> { javaRows.clear() }
    }
}
