package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.plan.PlanRegressionPolicy

class TrainingEvidenceModelsTest {
    @Test
    fun missingWeeklyObservationsRemainDistinctFromRecordedZero() {
        val missing = TrainingMetrics(null, null, null, null, null, null, null, null)
        val zero = TrainingMetrics(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0)
        assertNull(missing.currentWeekKm())
        assertNull(missing.runningDays())
        assertEquals(0.0, zero.currentWeekKm())
        assertNotEquals(missing, zero)
        assertFailsWith<IllegalArgumentException> {
            TrainingMetrics(Double.NaN, null, null, null, null, null, null, null)
        }
        assertFailsWith<IllegalArgumentException> {
            TrainingMetrics(null, null, null, null, 8, null, null, null)
        }
    }

    @Test
    fun fixtureAcceptanceBudgetsRetainFiniteAndOrderedLimits() {
        val policy = PlanRegressionPolicy(5.0, 1.2, 3.0, 0.5, 2, 2, 1, 0.8)
        assertEquals(1.2, policy.maximumWeeklyMultiplier())
        assertEquals(2, policy.minimumDaysBetweenHardSessions())
        assertFailsWith<IllegalArgumentException> {
            PlanRegressionPolicy(5.0, 0.9, 3.0, 0.5, 2, 2, 1, 0.8)
        }
        assertFailsWith<IllegalArgumentException> {
            PlanRegressionPolicy(Double.POSITIVE_INFINITY, 1.2, 3.0, 0.5, 2, 2, 1, 0.8)
        }
    }
}
