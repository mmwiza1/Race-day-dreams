package org.racedaydreams.domain

import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.*
import org.racedaydreams.domain.achievements.TrainingMilestones.Award
import org.racedaydreams.domain.achievements.TrainingMilestones.Kind
import org.racedaydreams.domain.achievements.TrainingMilestones.Proof
import org.racedaydreams.domain.achievements.TrainingMilestones.Source
import org.racedaydreams.domain.plan.PlanRegressionReport

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
class RetainedRecognitionOwnershipTest {
    @Test
    fun rawDiagnosticFieldsRemainUnknownAndReportCollectionsAreSnapshots() {
        val issue = PlanRegressionReport.Issue(null, null, null, null)
        val input = mutableListOf(issue)
        val report = PlanRegressionReport(input, emptyList())
        input.clear()
        assertSame(issue, report.issues().single())
        assertNull(issue.severity())
        assertNull(issue.code())
        assertNull(issue.date())
        assertNull(issue.explanation())
        assertFailsWith<UnsupportedOperationException> {
            (report.issues() as java.util.List<*>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (report.weeks() as java.util.List<*>).clear()
        }
    }

    @Test
    fun recognitionEvidenceCannotBeChangedByAJavaCollectionCaller() {
        val at = Instant.parse("2026-11-15T12:00:00Z")
        val proof = Proof(Source.RUN, "raw", at, ZoneOffset.UTC, null)
        val input = mutableListOf(proof)
        val award = Award(Kind.FIRST_RUN, at, input)
        input.clear()
        assertEquals(listOf(proof), award.evidence())
        assertFailsWith<UnsupportedOperationException> {
            (award.evidence() as java.util.List<*>).clear()
        }
    }
}
