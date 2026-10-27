package org.racedaydreams.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.json.JSONObject;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.ActivityRepository;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultRepository;
import org.racedaydreams.domain.activity.RunnerDataRepository;
import org.racedaydreams.domain.activity.Shoe;
import org.racedaydreams.domain.activity.ShoeRepository;
import org.racedaydreams.domain.baseline.AssessmentResult;
import org.racedaydreams.domain.baseline.BaselineRepository;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository;
import org.racedaydreams.domain.settings.SettingsRepository;

/**
 * Compatibility composition root for existing integrations. SQL and feature persistence rules live
 * in scoped repositories; all adapters retain the same application database and transaction pool.
 * New orchestration should accept the smaller contracts exposed below.
 */
public final class FeatureStore extends SqliteStoreAccess implements RunnerDataRepository, AutoCloseable {
    public static final int EXPORT_STORE_VERSION = SqliteBackupRepository.EXPORT_STORE_VERSION;
    private final ActivityRepository activities;
    private final ResultRepository results;
    private final ShoeRepository shoes;
    private final BaselineRepository baseline;
    private final SettingsRepository settings;
    private final PlannedWorkoutRepository planned;
    private final BackupRepository backup;

    public FeatureStore(Context context) {
        super(context);
        activities = new SqliteActivityRepository(context);
        results = new SqliteResultRepository(context);
        shoes = new SqliteShoeRepository(context);
        baseline = new SqliteBaselineRepository(context);
        settings = new SqliteSettingsRepository(context);
        planned = new SqlitePlannedWorkoutRepository(context);
        backup = new SqliteBackupRepository(context);
    }

    public ActivityRepository activityRepository() { return activities; }
    public ResultRepository resultRepository() { return results; }
    public ShoeRepository shoeRepository() { return shoes; }
    public BaselineRepository baselineRepository() { return baseline; }
    public SettingsRepository settingsRepository() { return settings; }
    public PlannedWorkoutRepository plannedWorkoutRepository() { return planned; }
    public BackupRepository backupRepository() { return backup; }

    @Override public List<ActivityRecord> activities() { return activities.activities(); }
    @Override public List<ActivityRecord> activitySummaries() { return activities.activitySummaries(); }
    @Override public ActivityRecord activityDetail(String id) { return activities.activityDetail(id); }
    @Override public List<ActivityRecord> coachingActivities(LocalDate today) { return activities.coachingActivities(today); }
    @Override public List<ActivityRecord> activitySummaryPage(Instant before, String afterId, int limit) { return activities.activitySummaryPage(before, afterId, limit); }
    @Override public void upsertManualActivity(ActivityRecord activity) { activities.upsertManualActivity(activity); }
    @Override public void deleteActivity(String id) { activities.deleteActivity(id); }
    @Override public void applyImport(String provider, List<ActivityRecord> upserts, List<String> deletedRecordIds, String nextToken) { activities.applyImport(provider, upserts, deletedRecordIds, nextToken); }
    @Override public String importToken(String provider) { return activities.importToken(provider); }
    @Override public void saveActivityNotes(String id, String notes, Double rpe) { activities.saveActivityNotes(id, notes, rpe); }
    @Override public void assignShoe(String id, String shoeId) { activities.assignShoe(id, shoeId); }
    @Override public List<RecordedResult> results() { return results.results(); }
    @Override public void saveResult(RecordedResult result) { results.saveResult(result); }
    @Override public void deleteResult(String id) { results.deleteResult(id); }
    @Override public List<Shoe> shoes() { return shoes.shoes(); }
    @Override public void saveShoe(Shoe shoe) { shoes.saveShoe(shoe); }
    @Override public ManualBaselineInput savedManualBaseline() { return baseline.savedManualBaseline(); }
    @Override public Instant manualBaselineRecordedAt() { return baseline.manualBaselineRecordedAt(); }
    @Override public void saveManualBaseline(ManualBaselineInput input) { baseline.saveManualBaseline(input); }
    @Override public List<AssessmentResult> assessments() { return baseline.assessments(); }
    @Override public void saveAssessment(AssessmentResult assessment) { baseline.saveAssessment(assessment); }
    @Override public void saveAssessment(ActivityRecord activity, AssessmentResult assessment) { baseline.saveAssessment(activity, assessment); }
    public void recordAssessment(JSONObject assessment) { baseline.saveAssessment(FeatureJson.assessment(assessment)); }
    @Override public String getSetting(String key) { return settings.getSetting(key); }
    @Override public void putSetting(String key, String value) { settings.putSetting(key, value); }
    @Override public void removeSetting(String key) { settings.removeSetting(key); }
    @Override public List<PlannedWorkout> plannedWorkouts() { return planned.plannedWorkouts(); }
    @Override public long plannedWorkoutRevision(String id) { return planned.plannedWorkoutRevision(id); }
    @Override public void savePlannedWorkout(PlannedWorkout workout) { planned.savePlannedWorkout(workout); }
    @Override public void deletePlannedWorkout(String id) { planned.deletePlannedWorkout(id); }
    public JSONObject exportUserData() { return backup.exportUserData(); }
    public void restoreUserData(JSONObject data, boolean merge) { backup.restoreUserData(data, merge); }
    public static void validateUserData(JSONObject data) { SqliteBackupRepository.validateUserData(data); }

    /** Existing recorder transaction hook; this does not begin a second transaction. */
    static void putActivity(SQLiteDatabase db, ActivityRecord activity, boolean userOwned) { SqliteActivityRepository.putActivity(db, activity, userOwned); }

    @Override public void close() { requireIo(); helper.close(); }
}
