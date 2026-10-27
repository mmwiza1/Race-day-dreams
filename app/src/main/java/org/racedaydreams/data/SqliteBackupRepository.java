package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.UserProfile;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.ActivityRepository;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultRepository;
import org.racedaydreams.domain.activity.ResultSource;
import org.racedaydreams.domain.activity.Shoe;
import org.racedaydreams.domain.activity.ShoeRepository;
import org.racedaydreams.domain.activity.SourceIdentity;
import org.racedaydreams.domain.baseline.ManualBaselineInput;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** SQLite adapter for backup data. Shares the application database and its transaction boundary. */
public final class SqliteBackupRepository extends SqliteStoreAccess implements BackupRepository {
    public SqliteBackupRepository(Context context) {
        super(context);
        activities = new SqliteActivityRepository(context);
        shoes = new SqliteShoeRepository(context);
        results = new SqliteResultRepository(context);
    }

    public static final int EXPORT_STORE_VERSION = 1;
    private final ActivityRepository activities;
    private final ShoeRepository shoes;
    private final ResultRepository results;

    /**
     * Exports app-owned information only. Imported measurements can be re-imported with consent.
     */
    public JSONObject exportUserData() {
        SQLiteDatabase db = read();
        db.beginTransactionNonExclusive();
        try {
            UserProfile profile = SqliteUserPreferencesRepository.readProfile(db).orElse(null);
            JSONObject out =
                    FeatureJson.json(
                            "storeVersion",
                            EXPORT_STORE_VERSION,
                            "profile",
                            profile == null ? null : FeatureJson.profile(profile),
                            "draft",
                            null,
                            "manualBaseline",
                            null,
                            "manualBaselineRecordedAt",
                            null);
            try (Cursor c = db.rawQuery("SELECT payload FROM onboarding_draft WHERE id=1", null)) {
                if (c.moveToFirst()) out.put("draft", c.getString(0));
            }
            try (Cursor c =
                    db.rawQuery(
                            "SELECT manual_json,manual_recorded_at FROM baseline_state WHERE id=1",
                            null)) {
                if (c.moveToFirst()) {
                    out.put("manualBaseline", FeatureJson.object(c.getString(0)));
                    out.put(
                            "manualBaselineRecordedAt",
                            c.isNull(1) ? JSONObject.NULL : c.getString(1));
                }
            }
            JSONArray activities = new JSONArray();
            for (ActivityRecord a : this.activities.activitySummaries())
                if (SqliteActivityRepository.isUserActivity(a))
                    activities.put(
                            FeatureJson.activity(SqliteActivityRepository.backupActivity(a)));
            out.put("activities", activities);
            JSONArray shoes = new JSONArray();
            for (Shoe shoe : this.shoes.shoes()) shoes.put(FeatureJson.shoe(shoe));
            out.put("shoes", shoes);
            JSONArray results = new JSONArray();
            for (RecordedResult result : this.results.results())
                if (result.source() != ResultSource.ROLLING)
                    results.put(FeatureJson.result(result));
            out.put("results", results);
            JSONArray bestEfforts = new JSONArray();
            Set<String> effortIds = new HashSet<>();
            try (Cursor c =
                    db.rawQuery(
                            "SELECT b.id,b.payload,a.provider,a.record_id,a.origin_package FROM"
                                + " best_efforts b JOIN activities a ON b.activity_id=a.id ORDER BY"
                                + " b.id",
                            null)) {
                while (c.moveToNext()) {
                    effortIds.add(c.getString(0));
                    bestEfforts.put(
                            FeatureJson.json(
                                    "result",
                                    FeatureJson.object(c.getString(1)),
                                    "source",
                                    FeatureJson.json(
                                            "provider",
                                            c.getString(2),
                                            "recordId",
                                            c.getString(3),
                                            "originPackage",
                                            c.getString(4))));
                }
            }
            try (Cursor c =
                    db.rawQuery(
                            "SELECT id,payload,provider,record_id,origin_package FROM"
                                    + " preserved_best_efforts ORDER BY id",
                            null)) {
                while (c.moveToNext())
                    if (effortIds.add(c.getString(0)))
                        bestEfforts.put(
                                FeatureJson.json(
                                        "result",
                                        FeatureJson.object(c.getString(1)),
                                        "source",
                                        FeatureJson.json(
                                                "provider",
                                                c.getString(2),
                                                "recordId",
                                                c.getString(3),
                                                "originPackage",
                                                c.getString(4))));
            }
            out.put("bestEfforts", bestEfforts);
            JSONArray annotations = new JSONArray();
            try (Cursor c =
                    db.rawQuery(
                            "SELECT provider,record_id,notes,rpe,shoe_id FROM activity_annotations"
                                    + " ORDER BY provider,record_id",
                            null)) {
                while (c.moveToNext())
                    annotations.put(
                            FeatureJson.json(
                                    "provider",
                                    c.getString(0),
                                    "recordId",
                                    c.getString(1),
                                    "notes",
                                    c.getString(2),
                                    "rpe",
                                    c.isNull(3) ? null : c.getDouble(3),
                                    "shoeId",
                                    c.getString(4)));
            }
            out.put("annotations", annotations);
            JSONObject settings = new JSONObject();
            try (Cursor c =
                    db.rawQuery(
                            "SELECT setting_key,setting_value FROM settings ORDER BY setting_key",
                            null)) {
                while (c.moveToNext()) settings.put(c.getString(0), c.getString(1));
            }
            out.put("settings", settings);
            JSONArray plans = new JSONArray();
            try (Cursor c =
                    db.rawQuery(
                            "SELECT payload,revision FROM planned_workouts ORDER BY id", null)) {
                while (c.moveToNext())
                    plans.put(
                            FeatureJson.json(
                                    "workout",
                                    FeatureJson.object(c.getString(0)),
                                    "revision",
                                    c.getLong(1)));
            }
            out.put("plannedWorkouts", plans);
            out.put("assessments", opaqueRows(db, "assessment_results"));
            out.put("trainingPlans", opaqueRows(db, "training_plans"));
            out.put("planAdjustments", opaqueRows(db, "plan_adjustments"));
            out.put("coaching", CoachingStore.exportData(db));
            out.put("development", DevelopmentStore.exportData(db));
            out.put("strengthState", StrengthUserStore.exportData(db));
            out.put("gymProgramming", GymProgrammeStore.exportData(db));
            out.put("support", SupportStore.exportData(db));
            out.put("release08", Release08Store.exportData(db));
            out.put("strengthEngine", SqliteGymExecutionRepository.exportData(db));
            db.setTransactionSuccessful();
            return out;
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        } finally {
            db.endTransaction();
        }
    }

    private static JSONArray opaqueRows(SQLiteDatabase db, String table) {
        JSONArray out = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT payload FROM " + table + " ORDER BY id", null)) {
            while (c.moveToNext()) out.put(FeatureJson.object(c.getString(0)));
        }
        return out;
    }

    /** Validation is public for backup previews; it never reads or changes the database. */
    @Override
    public void validateSnapshot(JSONObject data) {
        validateUserData(data);
    }

    public static void validateUserData(JSONObject data) {
        validated(data);
        if (data.has("support")) SupportStore.validate(data.optJSONObject("support"));
    }

    private record RestoreData(
            UserProfile profile,
            String draft,
            ManualBaselineInput baseline,
            Instant baselineAt,
            List<ActivityRecord> activities,
            List<Shoe> shoes,
            List<RecordedResult> results,
            JSONArray bestEfforts,
            JSONArray annotations,
            JSONObject settings,
            JSONArray plans,
            JSONArray assessments,
            JSONArray trainingPlans,
            JSONArray adjustments,
            JSONObject coaching,
            JSONObject development,
            JSONObject strengthState,
            JSONObject gymProgramming,
            JSONObject strengthEngine,
            JSONObject support) {}

    private record SourceKey(String provider, String recordId) {}

    private static RestoreData validated(JSONObject data) {
        try {
            if (data.has("support")) SupportStore.validate(data.getJSONObject("support"));
            if (data.has("release08")) Release08Store.validate(data.getJSONObject("release08"));
            if (FeatureJson.requiredInteger(data, "storeVersion") != EXPORT_STORE_VERSION)
                throw new IllegalArgumentException("Unsupported backup store version.");
            UserProfile profile =
                    data.isNull("profile")
                            ? null
                            : FeatureJson.profile(data.getJSONObject("profile"));
            String draft = FeatureJson.text(data, "draft");
            if (draft != null) FeatureJson.object(draft);
            ManualBaselineInput baseline =
                    data.isNull("manualBaseline")
                            ? null
                            : FeatureJson.baseline(data.getJSONObject("manualBaseline"));
            Instant baselineAt =
                    data.isNull("manualBaselineRecordedAt")
                            ? null
                            : Instant.parse(data.getString("manualBaselineRecordedAt"));
            List<Shoe> shoes = new ArrayList<>();
            Set<String> shoeIds = new HashSet<>();
            int defaults = 0;
            JSONArray shoeRows = array(data, "shoes");
            for (int i = 0; i < shoeRows.length(); i++) {
                Shoe s = FeatureJson.shoe(shoeRows.getJSONObject(i));
                unique(shoeIds, s.id());
                if (s.defaultShoe()) defaults++;
                shoes.add(s);
            }
            if (defaults > 1)
                throw new IllegalArgumentException("A backup has more than one default shoe.");
            List<ActivityRecord> activities = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            Set<SourceKey> sources = new HashSet<>();
            Map<String, SourceIdentity> activitySources = new HashMap<>();
            JSONArray rows = array(data, "activities");
            for (int i = 0; i < rows.length(); i++) {
                ActivityRecord a = FeatureJson.activity(rows.getJSONObject(i));
                if (!SqliteActivityRepository.isUserActivity(a))
                    throw new IllegalArgumentException(
                            "Backup contains external health measurements.");
                unique(ids, a.id());
                unique(sources, new SourceKey(a.source().provider(), a.source().recordId()));
                activitySources.put(a.id(), a.source());
                if (a.shoeId() != null && !shoeIds.contains(a.shoeId()))
                    throw new IllegalArgumentException("Activity shoe is missing.");
                activities.add(a);
            }
            Map<String, Double> catalog = SqliteResultRepository.distanceCatalog();
            List<RecordedResult> results = new ArrayList<>();
            Set<String> resultIds = new HashSet<>();
            JSONArray resultRows = array(data, "results");
            for (int i = 0; i < resultRows.length(); i++) {
                RecordedResult r = FeatureJson.result(resultRows.getJSONObject(i));
                if (r.source() == ResultSource.ROLLING)
                    throw new IllegalArgumentException(
                            "Derived efforts must be recalculated from retained samples.");
                unique(resultIds, r.id());
                SqliteResultRepository.registerDistance(catalog, r.distance());
                results.add(r);
            }
            JSONArray bestEfforts = array(data, "bestEfforts");
            Set<String> effortPairs = new HashSet<>();
            for (int i = 0; i < bestEfforts.length(); i++) {
                JSONObject e = bestEfforts.getJSONObject(i);
                RecordedResult r = FeatureJson.result(e.getJSONObject("result"));
                if (r.source() != ResultSource.ROLLING)
                    throw new IllegalArgumentException(
                            "An archived effort requires rolling provenance.");
                unique(resultIds, r.id());
                unique(effortPairs, r.activityId() + "\n" + r.distance().code());
                SqliteResultRepository.registerDistance(catalog, r.distance());
                JSONObject source = e.getJSONObject("source");
                SourceIdentity identity =
                        new SourceIdentity(
                                FeatureJson.required(source, "provider"),
                                FeatureJson.required(source, "recordId"),
                                FeatureJson.text(source, "originPackage"));
                SourceIdentity prior = activitySources.put(r.activityId(), identity);
                if (prior != null && !prior.equals(identity))
                    throw new IllegalArgumentException(
                            "Archived efforts disagree about their activity source.");
            }
            JSONArray annotations = array(data, "annotations");
            Set<SourceKey> annotationIds = new HashSet<>();
            for (int i = 0; i < annotations.length(); i++) {
                JSONObject a = annotations.getJSONObject(i);
                String provider = FeatureJson.required(a, "provider"),
                        record = FeatureJson.required(a, "recordId");
                unique(annotationIds, new SourceKey(provider, record));
                if (!a.has("notes"))
                    throw new IllegalArgumentException("Annotation notes presence is required.");
                FeatureJson.text(a, "notes");
                Double rpe = FeatureJson.number(a, "rpe");
                if (rpe != null && (rpe < 0 || rpe > 10))
                    throw new IllegalArgumentException("Invalid RPE.");
                String shoe = FeatureJson.text(a, "shoeId");
                if (shoe != null && !shoeIds.contains(shoe))
                    throw new IllegalArgumentException("Annotation shoe is missing.");
            }
            JSONObject settings = data.getJSONObject("settings");
            if (data.has("strengthState"))
                StrengthUserStore.validate(data.getJSONObject("strengthState"));
            for (Iterator<String> it = settings.keys(); it.hasNext(); ) {
                String key = it.next();
                required(key, "setting key");
                if (!(settings.get(key) instanceof String))
                    throw new IllegalArgumentException("Setting values must be text.");
            }
            JSONArray plans = array(data, "plannedWorkouts");
            Set<String> planIds = new HashSet<>();
            for (int i = 0; i < plans.length(); i++) {
                JSONObject row = plans.getJSONObject(i);
                PlannedWorkout w = FeatureJson.planned(row.getJSONObject("workout"));
                unique(planIds, w.id());
                if (FeatureJson.requiredLong(row, "revision") < 1)
                    throw new IllegalArgumentException("Invalid workout revision.");
            }
            JSONArray assessments = array(data, "assessments"),
                    trainingPlans = array(data, "trainingPlans"),
                    adjustments = array(data, "planAdjustments");
            validateOpaque(assessments);
            for (int i = 0; i < assessments.length(); i++)
                FeatureJson.assessment(assessments.getJSONObject(i));
            validateOpaque(trainingPlans);
            validateOpaque(adjustments);
            JSONObject coaching = data.has("coaching") ? data.getJSONObject("coaching") : null;
            if (coaching != null) {
                CoachingStore.validate(coaching);
                JSONArray current = coaching.getJSONArray("coach_current_plan");
                if (current.length() == 1) {
                    String versionId = current.getJSONObject(0).getString("version_id");
                    JSONArray versions = coaching.getJSONArray("coach_plan_versions");
                    for (int i = 0; i < versions.length(); i++)
                        if (versions.getJSONObject(i).getString("id").equals(versionId)) {
                            var version =
                                    CoachingJson.version(
                                            FeatureJson.object(
                                                    versions.getJSONObject(i)
                                                            .getString("payload")));
                            Set<PlannedWorkout> mirror = new HashSet<>();
                            for (int k = 0; k < plans.length(); k++)
                                mirror.add(
                                        FeatureJson.planned(
                                                plans.getJSONObject(k).getJSONObject("workout")));
                            if (!mirror.equals(new HashSet<>(version.plan().workouts())))
                                throw new IllegalArgumentException(
                                        "Backup current programme and reminder prescriptions"
                                                + " disagree.");
                        }
                }
            }
            JSONObject development =
                    data.has("development") ? data.getJSONObject("development") : null;
            if (development != null) DevelopmentStore.validate(development);
            if (data.has("gymProgramming"))
                GymProgrammeStore.validate(data.getJSONObject("gymProgramming"));
            GymProgrammeStore.validateLinks(
                    development, coaching, data.optJSONObject("gymProgramming"));
            JSONObject strengthEngine =
                    data.has("strengthEngine") ? data.getJSONObject("strengthEngine") : null;
            if (strengthEngine != null)
                SqliteGymExecutionRepository.validate(
                        strengthEngine, coaching, data.optJSONObject("gymProgramming"), development);
            return new RestoreData(
                    profile,
                    draft,
                    baseline,
                    baselineAt,
                    activities,
                    shoes,
                    results,
                    bestEfforts,
                    annotations,
                    settings,
                    plans,
                    assessments,
                    trainingPlans,
                    adjustments,
                    coaching,
                    development,
                    data.optJSONObject("strengthState"),
                    data.optJSONObject("gymProgramming"),
                    strengthEngine,
                    data.optJSONObject("support"));
        } catch (JSONException | java.time.DateTimeException e) {
            throw FeatureJson.invalid(e);
        }
    }

    private static JSONArray array(JSONObject data, String key) throws JSONException {
        JSONArray rows = data.getJSONArray(key);
        if (rows.length() > 100_000)
            throw new IllegalArgumentException("Too many " + key + " records.");
        return rows;
    }

    private static <T> void unique(Set<T> ids, T id) {
        if (!ids.add(id)) throw new IllegalArgumentException("Duplicate backup identity: " + id);
    }

    private static void validateOpaque(JSONArray rows) throws JSONException {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < rows.length(); i++)
            unique(ids, FeatureJson.required(rows.getJSONObject(i), "id"));
    }

    public void restoreUserData(JSONObject data, boolean merge) {
        if (merge)
            throw new IllegalArgumentException(
                    "Merge restore is not supported; use validated replacement.");
        RestoreData snapshot =
                validated(FeatureJson.object(Objects.requireNonNull(data).toString()));
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            try (Cursor owned =
                    db.rawQuery(
                            "SELECT id FROM activities WHERE provider IN ('rdd','gpx')", null)) {
                Set<String> restoredIds = new HashSet<>();
                for (ActivityRecord a : snapshot.activities()) restoredIds.add(a.id());
                while (owned.moveToNext())
                    if (!restoredIds.contains(owned.getString(0)))
                        throw new IllegalArgumentException(
                                "This backup would remove user-owned recordings. Export raw"
                                    + " recordings first and restore into an empty installation, or"
                                    + " use a current app backup.");
            }
            try (Cursor active =
                    db.rawQuery(
                            "SELECT 1 FROM recording_sessions WHERE state IN"
                                    + " ('ACTIVE','PAUSED','RECOVERABLE')",
                            null)) {
                if (active.moveToFirst())
                    throw new IllegalArgumentException(
                            "Finish or recover the active recording before replacing app data.");
            }
            if (SqliteGymExecutionRepository.hasUnfinished(db))
                throw new IllegalArgumentException(
                        "Finish or discard the unfinished gym workout before replacing app data.");
            SqliteGymExecutionRepository.clear(db);
            GymProgrammeStore.clear(db);
            StrengthUserStore.clear(db);
            DevelopmentStore.clear(db);
            CoachingStore.clear(db);
            for (String table :
                    new String[] {
                        "best_efforts",
                        "preserved_best_efforts",
                        "activity_annotations",
                        "manual_results",
                        "activities",
                        "shoes",
                        "import_checkpoints",
                        "baseline_state",
                        "settings",
                        "assessment_results",
                        "planned_workouts",
                        "training_plans",
                        "plan_adjustments",
                        "onboarding_draft",
                        "user_profile"
                    }) db.delete(table, null, null);
            if (snapshot.profile() != null)
                SqliteUserPreferencesRepository.writeProfile(db, snapshot.profile());
            if (snapshot.draft() != null) {
                ContentValues v = values("payload", snapshot.draft());
                v.put("id", 1);
                db.insertOrThrow("onboarding_draft", null, v);
            }
            for (Shoe s : snapshot.shoes()) SqliteShoeRepository.putShoe(db, s);
            for (ActivityRecord a : snapshot.activities())
                SqliteActivityRepository.putActivity(db, a, true, false);
            for (RecordedResult r : snapshot.results())
                SqliteResultRepository.putResult(db, r, false);
            for (int i = 0; i < snapshot.bestEfforts().length(); i++) {
                JSONObject e = snapshot.bestEfforts().getJSONObject(i),
                        source = e.getJSONObject("source");
                RecordedResult r = FeatureJson.result(e.getJSONObject("result"));
                ContentValues v = values("id", r.id());
                v.put("activity_id", r.activityId());
                v.put("distance_code", r.distance().code());
                v.put("provider", source.getString("provider"));
                v.put("record_id", source.getString("recordId"));
                v.put("origin_package", FeatureJson.text(source, "originPackage"));
                v.put("payload", FeatureJson.result(r).toString());
                replace(db, "preserved_best_efforts", v);
            }
            for (int i = 0; i < snapshot.annotations().length(); i++) {
                JSONObject a = snapshot.annotations().getJSONObject(i);
                SqliteActivityRepository.putAnnotation(
                        db,
                        a.getString("provider"),
                        a.getString("recordId"),
                        FeatureJson.text(a, "notes"),
                        FeatureJson.number(a, "rpe"),
                        FeatureJson.text(a, "shoeId"));
            }
            if (snapshot.baseline() != null)
                SqliteBaselineRepository.putBaseline(
                        db, snapshot.baseline(), snapshot.baselineAt());
            for (Iterator<String> it = snapshot.settings().keys(); it.hasNext(); ) {
                String key = it.next();
                SqliteSettingsRepository.putSetting(db, key, snapshot.settings().getString(key));
            }
            for (int i = 0; i < snapshot.plans().length(); i++) {
                JSONObject p = snapshot.plans().getJSONObject(i);
                SqlitePlannedWorkoutRepository.putPlanned(
                        db,
                        FeatureJson.planned(p.getJSONObject("workout")),
                        FeatureJson.requiredLong(p, "revision"));
            }
            for (int i = 0; i < snapshot.assessments().length(); i++)
                putOpaque(db, "assessment_results", snapshot.assessments().getJSONObject(i));
            for (int i = 0; i < snapshot.trainingPlans().length(); i++)
                putOpaque(db, "training_plans", snapshot.trainingPlans().getJSONObject(i));
            for (int i = 0; i < snapshot.adjustments().length(); i++)
                putOpaque(db, "plan_adjustments", snapshot.adjustments().getJSONObject(i));
            if (snapshot.coaching() != null) CoachingStore.restore(db, snapshot.coaching());
            if (snapshot.development() != null)
                DevelopmentStore.restore(db, snapshot.development());
            if (snapshot.strengthState() != null)
                StrengthUserStore.restore(db, snapshot.strengthState());
            if (snapshot.gymProgramming() != null)
                GymProgrammeStore.restore(db, snapshot.gymProgramming());
            if (snapshot.strengthEngine() != null)
                SqliteGymExecutionRepository.restore(db, snapshot.strengthEngine());
            SupportStore.restore(db, snapshot.support());
            Release08Store.restore(db, data.optJSONObject("release08"));
            RecordingStore.restoreRetainedSamples(db);
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        } finally {
            db.endTransaction();
        }
    }
}
