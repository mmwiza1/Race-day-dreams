package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.strength.ExerciseExclusionRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Typed preferences over the shared database, independent of legacy archive/hide choices. */
public final class SqliteExerciseExclusionRepository extends SqliteStoreAccess
        implements ExerciseExclusionRepository {
    public SqliteExerciseExclusionRepository(Context context) {
        super(context);
    }

    @Override
    public List<Exclusion> exclusions() {
        return rowsForChoices(read());
    }

    static List<Exclusion> rowsForChoices(SQLiteDatabase db) {
        var out = new ArrayList<Exclusion>();
        try (var c =
                db.rawQuery(
                        "SELECT exercise_id,reason,excluded_at FROM strength_exclusions ORDER BY"
                                + " exercise_id",
                        null)) {
            while (c.moveToNext())
                out.add(
                        new Exclusion(
                                c.getString(0),
                                Reason.valueOf(c.getString(1)),
                                Instant.parse(c.getString(2))));
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public void exclude(Exclusion exclusion) {
        putForChoices(write(), exclusion);
    }

    static void putForChoices(SQLiteDatabase db, Exclusion exclusion) {
        var v = new ContentValues();
        v.put("exercise_id", exclusion.exerciseId());
        v.put("reason", exclusion.reason().name());
        v.put("excluded_at", exclusion.excludedAt().toString());
        db.insertWithOnConflict("strength_exclusions", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void restoreExercise(String id) {
        new Exclusion(id, Reason.PREFERENCE, Instant.EPOCH);
        write().delete("strength_exclusions", "exercise_id=?", new String[] {id});
    }

    static JSONObject exportData(SQLiteDatabase db) {
        try {
            var entries = new JSONArray();
            for (var e : rowsForChoices(db))
                entries.put(
                        new JSONObject()
                                .put("exerciseId", e.exerciseId())
                                .put("reason", e.reason().name())
                                .put("excludedAt", e.excludedAt().toString()));
            return new JSONObject().put("version", 1).put("entries", entries);
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static List<Exclusion> validate(JSONObject data) {
        try {
            GymJsonSupport.fields(data, "version", "entries");
            if (FeatureJson.requiredInteger(data, "version") != 1)
                throw new IllegalArgumentException("Unsupported exercise exclusions.");
            var entries = data.getJSONArray("entries");
            if (entries.length() > 10000)
                throw new IllegalArgumentException("Too many exercise exclusions.");
            var ids = new HashSet<String>();
            var out = new ArrayList<Exclusion>();
            for (int i = 0; i < entries.length(); i++) {
                var row = entries.getJSONObject(i);
                GymJsonSupport.fields(row, "exerciseId", "reason", "excludedAt");
                var e =
                        new Exclusion(
                                FeatureJson.required(row, "exerciseId"),
                                Reason.valueOf(FeatureJson.required(row, "reason")),
                                Instant.parse(FeatureJson.required(row, "excludedAt")));
                if (!ids.add(e.exerciseId()))
                    throw new IllegalArgumentException("Duplicate exercise exclusion.");
                out.add(e);
            }
            return Collections.unmodifiableList(out);
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static void restore(SQLiteDatabase db, JSONObject data) {
        for (var e : validate(data)) putForChoices(db, e);
    }
}
