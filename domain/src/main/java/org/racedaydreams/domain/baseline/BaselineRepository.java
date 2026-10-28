package org.racedaydreams.domain.baseline;

import org.racedaydreams.domain.activity.ActivityRecord;

import java.time.Instant;
import java.util.List;

/** User-owned baseline evidence, including its original observation timestamp. */
public interface BaselineRepository {
    ManualBaselineInput savedManualBaseline();

    Instant manualBaselineRecordedAt();

    void saveManualBaseline(ManualBaselineInput input);

    List<AssessmentResult> assessments();

    void saveAssessment(AssessmentResult assessment);

    /** The actual manual activity and its assessment must commit together. */
    void saveAssessment(ActivityRecord activity, AssessmentResult assessment);
}
