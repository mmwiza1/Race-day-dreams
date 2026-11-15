package org.racedaydreams.domain.baseline

import org.racedaydreams.domain.activity.ActivityRecord
import org.racedaydreams.domain.time.Instant

/** User-owned baseline evidence and original observation time. Platforms own storage and IO. */
interface BaselineRepository {
    fun savedManualBaseline(): ManualBaselineInput

    fun manualBaselineRecordedAt(): Instant?

    fun saveManualBaseline(input: ManualBaselineInput)

    fun assessments(): List<AssessmentResult>

    fun saveAssessment(assessment: AssessmentResult)

    /** Actual manual activity and its assessment must commit together in the platform adapter. */
    fun saveAssessment(activity: ActivityRecord, assessment: AssessmentResult)
}
