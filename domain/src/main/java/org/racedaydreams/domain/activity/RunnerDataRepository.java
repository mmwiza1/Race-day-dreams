package org.racedaydreams.domain.activity;

import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.baseline.AssessmentResult;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.health.HealthImportRepository;
import java.time.Instant;
import java.util.List;

/** Provider- and Android-independent local data boundary. Implementations document IO behavior. */
public interface RunnerDataRepository extends HealthImportRepository {
    void upsertManualActivity(ActivityRecord activity);
    void deleteActivity(String id);
    void saveActivityNotes(String id, String notes, Double rpe);
    void assignShoe(String activityId, String shoeId);
    List<RecordedResult> results();
    void saveResult(RecordedResult result);
    void deleteResult(String id);
    List<Shoe> shoes();
    void saveShoe(Shoe shoe);
    ManualBaselineInput savedManualBaseline();
    Instant manualBaselineRecordedAt();
    void saveManualBaseline(ManualBaselineInput input);
    List<AssessmentResult> assessments();
    void saveAssessment(AssessmentResult assessment);
    String getSetting(String key);
    void putSetting(String key, String value);
    List<PlannedWorkout> plannedWorkouts();
    long plannedWorkoutRevision(String id);
    void savePlannedWorkout(PlannedWorkout workout);
    void deletePlannedWorkout(String id);
}
