package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;

import org.json.*;
import org.racedaydreams.domain.*;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

import java.time.Instant;
import java.util.*;

/**
 * Shared database and transactions; immutable versions plus one current pointer. All calls require
 * IO.
 */
public final class CoachingStore implements CoachingRepository {
    private final ProfileDatabase helper;
    private final org.racedaydreams.storage.SqliteCoachingRepository repository;

    public CoachingStore(Context context) {
        helper = ProfileDatabase.get(context);
        repository =
                new org.racedaydreams.storage.SqliteCoachingRepository(
                        new AndroidSqlConnection(this::db),
                        Instant::now,
                        () -> UUID.randomUUID().toString(),
                        org.racedaydreams.storage.PortableLegacyStrengthFingerprint.INSTANCE);
    }

    private SQLiteDatabase db() {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("Coaching storage requires IO.");
        return helper.getWritableDatabase();
    }

    public void transaction(Runnable action) {
        repository.transaction(action);
    }

    public List<PlanVersion> versions() {
        return repository.versions();
    }

    public PlanVersion current() {
        return repository.current();
    }

    public List<StrengthExercise> exercises() {
        return repository.exercises();
    }

    public List<StrengthSession> strength() {
        return repository.strength();
    }

    public List<RunExecution> responses() {
        return repository.responses();
    }

    public List<AdjustmentProposal> proposals() {
        return repository.proposals();
    }

    public boolean proposalKnown(String id) {
        return repository.proposalKnown(id);
    }

    public void saveExercise(StrengthExercise exercise) {
        repository.saveExercise(exercise);
    }

    public void saveStrength(StrengthSession actual) {
        repository.saveStrength(actual);
    }

    public void deleteStrength(String id) {
        repository.deleteStrength(id);
    }

    public void saveResponse(RunExecution response) {
        repository.saveResponse(response);
    }

    public void saveFeedback(String activityId, String versionId, RunFeedback feedback) {
        repository.saveFeedback(activityId, versionId, feedback);
    }

    public void saveProposal(AdjustmentProposal proposal) {
        repository.saveProposal(proposal);
    }

    public void saveCurrent(PlanVersion version) {
        repository.saveCurrent(version);
    }

    public void decide(AdjustmentProposal proposal, String optionId, Set<String> completed) {
        repository.decide(proposal, optionId, completed);
    }

    static final String[] TABLES = {
        "run_feedback",
        "run_responses",
        "coach_proposals",
        "coach_current_plan",
        "coach_plan_versions",
        "strength_sessions",
        "strength_exercises"
    };

    static JSONObject exportData(SQLiteDatabase db) {
        JSONObject out = FeatureJson.json("version", 1);
        try {
            for (String table : TABLES) {
                JSONArray rows = new JSONArray();
                try (Cursor c = db.rawQuery("SELECT * FROM " + table + " ORDER BY rowid", null)) {
                    while (c.moveToNext()) {
                        JSONObject row = new JSONObject();
                        for (int i = 0; i < c.getColumnCount(); i++)
                            row.put(
                                    c.getColumnName(i),
                                    c.isNull(i) ? JSONObject.NULL : c.getString(i));
                        rows.put(row);
                    }
                }
                out.put(table, rows);
            }
            return out;
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    /**
     * Validate identities, typed payloads and graph references before the replacement transaction.
     */
    static void validate(JSONObject data) {
        try {
            if (FeatureJson.requiredInteger(data, "version") != 1)
                throw new IllegalArgumentException("Unsupported coaching backup version.");
            Map<String, PlanVersion> versions = new HashMap<>();
            Set<String> numbers = new HashSet<>();
            for (JSONObject row : array(data, "coach_plan_versions")) {
                var v = CoachingJson.version(FeatureJson.object(row.getString("payload")));
                match(row, "id", v.id());
                match(row, "plan_id", v.planId());
                match(row, "version", Integer.toString(v.version()));
                if (versions.put(v.id(), v) != null || !numbers.add(v.planId() + ":" + v.version()))
                    throw new IllegalArgumentException("Duplicated plan version.");
            }
            var current = array(data, "coach_current_plan");
            if (current.size() > 1)
                throw new IllegalArgumentException("Only one plan can be current.");
            for (var row : current) {
                match(row, "id", "1");
                if (!versions.containsKey(row.getString("version_id")))
                    throw new IllegalArgumentException("Current version is missing.");
            }
            Set<String> ids = new HashSet<>();
            for (var row : array(data, "strength_exercises")) {
                var e = CoachingJson.exercise(FeatureJson.object(row.getString("payload")));
                match(row, "id", e.id());
                if (!ids.add(e.id())) throw new IllegalArgumentException("Duplicate exercise.");
            }
            ids.clear();
            for (var row : array(data, "strength_sessions")) {
                var s = CoachingJson.strength(FeatureJson.object(row.getString("payload")));
                match(row, "id", s.id());
                match(row, "started_at", s.startedAt().toString());
                if (!ids.add(s.id()))
                    throw new IllegalArgumentException("Duplicate strength workout.");
            }
            ids.clear();
            for (var row : array(data, "run_responses")) {
                var r = CoachingJson.response(FeatureJson.object(row.getString("payload")));
                match(row, "activity_id", r.activityId());
                match(row, "planned_workout_id", r.plannedWorkoutId());
                match(row, "plan_version_id", r.planVersionId());
                if (!ids.add(r.activityId()))
                    throw new IllegalArgumentException("Duplicate response.");
                if (r.planVersionId() != null) {
                    var v = versions.get(r.planVersionId());
                    if (v == null
                            || v.plan().workouts().stream()
                                    .noneMatch(w -> w.id().equals(r.plannedWorkoutId())))
                        throw new IllegalArgumentException("Response prescription is missing.");
                }
            }
            ids.clear();
            for (var row : array(data, "coach_proposals")) {
                var p = CoachingJson.proposal(FeatureJson.object(row.getString("payload")));
                match(row, "id", p.id());
                match(row, "base_version_id", p.baseVersionId());
                if (!ids.add(p.id()) || !versions.containsKey(p.baseVersionId()))
                    throw new IllegalArgumentException("Invalid proposal identity/version.");
                String decision = FeatureJson.text(row, "decision"),
                        result = FeatureJson.text(row, "result_version_id");
                if ((decision == null) != (result == null))
                    throw new IllegalArgumentException(
                            "Decision and resulting version must be recorded together.");
                if (decision != null
                        && (p.options().stream().noneMatch(o -> o.id().equals(decision))
                                || result == null
                                || !versions.containsKey(result)))
                    throw new IllegalArgumentException("Invalid proposal decision.");
            }
            ids.clear();
            for (var row : array(data, "run_feedback")) {
                String activity = row.getString("activity_id");
                if (!ids.add(activity) || !versions.containsKey(row.getString("plan_version_id")))
                    throw new IllegalArgumentException("Invalid feedback identity/version.");
                JSONObject f = FeatureJson.object(row.getString("payload"));
                if (FeatureJson.requiredInteger(f, "ruleVersion") < 1
                        || FeatureJson.requiredInteger(f, "ruleVersion")
                                > RunScoreEngine.RULE_VERSION)
                    throw new IllegalArgumentException("Unsupported feedback rule version.");
                Integer score = FeatureJson.integer(f, "score");
                if (score != null && (score < 1 || score > 10))
                    throw new IllegalArgumentException("Invalid Run Score.");
                f.getJSONArray("components");
            }
        } catch (JSONException | java.time.DateTimeException e) {
            throw FeatureJson.invalid(e);
        }
    }

    private static void match(JSONObject row, String key, String value) throws JSONException {
        if (!Objects.equals(FeatureJson.text(row, key), value))
            throw new IllegalArgumentException("Backup column/payload disagree: " + key);
    }

    private static List<JSONObject> array(JSONObject j, String key) throws JSONException {
        JSONArray a = j.getJSONArray(key);
        if (a.length() > 100000) throw new IllegalArgumentException("Too many coaching records.");
        List<JSONObject> rows = new ArrayList<>();
        String[] expected =
                switch (key) {
                    case "coach_plan_versions" ->
                            new String[] {"id", "plan_id", "version", "payload"};
                    case "coach_current_plan" -> new String[] {"id", "version_id"};
                    case "coach_proposals" ->
                            new String[] {
                                "id", "base_version_id", "payload", "decision", "result_version_id"
                            };
                    case "strength_sessions" -> new String[] {"id", "started_at", "payload"};
                    case "run_responses" ->
                            new String[] {
                                "activity_id", "planned_workout_id", "plan_version_id", "payload"
                            };
                    case "run_feedback" ->
                            new String[] {"activity_id", "plan_version_id", "payload"};
                    default -> new String[] {"id", "payload"};
                };
        Set<String> columns = new HashSet<>(Arrays.asList(expected));
        for (int i = 0; i < a.length(); i++) {
            JSONObject row = a.getJSONObject(i);
            Set<String> found = new HashSet<>();
            for (Iterator<String> it = row.keys(); it.hasNext(); ) found.add(it.next());
            if (!columns.equals(found))
                throw new IllegalArgumentException("Unexpected coaching backup columns.");
            rows.add(row);
        }
        return rows;
    }

    static void clear(SQLiteDatabase db) {
        for (String table : TABLES) db.delete(table, null, null);
    }

    static void restore(SQLiteDatabase db, JSONObject data) {
        try {
            for (int t = TABLES.length - 1; t >= 0; t--) {
                String table = TABLES[t];
                for (var row : array(data, table)) {
                    ContentValues v = new ContentValues();
                    for (Iterator<String> it = row.keys(); it.hasNext(); ) {
                        String key = it.next();
                        v.put(key, FeatureJson.text(row, key));
                    }
                    db.insertOrThrow(table, null, v);
                }
            }
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }
}
