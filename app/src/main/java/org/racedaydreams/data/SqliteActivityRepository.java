package org.racedaydreams.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.ActivityRepository;
import org.racedaydreams.domain.activity.BestEffortCalculator;
import org.racedaydreams.domain.activity.RecordedResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * SQLite adapter for activity data. Shares the application database and its transaction boundary.
 */
public final class SqliteActivityRepository extends SqliteStoreAccess
        implements ActivityRepository {
    public SqliteActivityRepository(Context context) {
        super(context);
    }

    @Override
    public List<ActivityRecord> activities() {
        return activitySnapshot(true);
    }

    public List<ActivityRecord> activitySummaries() {
        return activitySnapshot(false);
    }

    public ActivityRecord activityDetail(String id) {
        for (ActivityRecord a : activitySnapshot(false, id))
            if (a.id().equals(id)) return RecordingStore.withRetainedSamples(read(), a);
        return null;
    }

    /** Bounded sample hydration for recent coaching, never every route in calendar history. */
    public List<ActivityRecord> coachingActivities(java.time.LocalDate today) {
        List<ActivityRecord> summary = activitySnapshot(false);
        Set<String> detailed = new HashSet<>();
        summary.stream()
                .filter(
                        a ->
                                !a.start()
                                        .isBefore(
                                                today.minusDays(28)
                                                        .atStartOfDay(java.time.ZoneOffset.UTC)
                                                        .toInstant()))
                .sorted(java.util.Comparator.comparing(ActivityRecord::start).reversed())
                .limit(12)
                .forEach(a -> detailed.add(a.id()));
        List<ActivityRecord> out = new ArrayList<>();
        int remaining = 60000;
        for (ActivityRecord a : summary) {
            int count = 0;
            if (detailed.contains(a.id()))
                try (Cursor c =
                        read().rawQuery(
                                        "SELECT COUNT(*) FROM recording_points p JOIN"
                                                + " recording_sessions s ON s.id=p.session_id WHERE"
                                                + " s.activity_id=?",
                                        new String[] {a.id()})) {
                    if (c.moveToFirst()) count = c.getInt(0);
                }
            if (count <= remaining && detailed.contains(a.id())) {
                out.add(RecordingStore.withRetainedSamples(read(), a));
                remaining -= count;
            } else out.add(a);
        }
        return out;
    }

    private List<ActivityRecord> activitySnapshot(boolean includeSamples) {
        return activitySnapshot(includeSamples, null);
    }

    public List<ActivityRecord> activitySummaryPage(Instant before, String afterId, int limit) {
        if (limit < 1 || limit > 100 || (before == null) != (afterId == null))
            throw new IllegalArgumentException("Invalid history cursor/page size");
        return activitySnapshot(false, null, before, afterId, limit);
    }

    private List<ActivityRecord> activitySnapshot(boolean includeSamples, String onlyId) {
        return activitySnapshot(includeSamples, onlyId, null, null, 0);
    }

    private List<ActivityRecord> activitySnapshot(
            boolean includeSamples, String onlyId, Instant before, String afterId, int limit) {
        SQLiteDatabase db = read();
        List<ActivityRecord> out = new ArrayList<>();
        String where =
                onlyId != null
                        ? " WHERE a.id=?"
                        : before != null
                                ? " WHERE (a.start_millis<CAST(? AS INTEGER) OR"
                                        + " (a.start_millis=CAST(? AS INTEGER) AND a.id>?))"
                                : "";
        String[] arguments =
                onlyId != null
                        ? new String[] {onlyId}
                        : before != null
                                ? new String[] {
                                    Long.toString(before.toEpochMilli()),
                                    Long.toString(before.toEpochMilli()),
                                    afterId
                                }
                                : null;
        try (Cursor cursor =
                db.rawQuery(
                        "SELECT a.payload, n.provider, n.notes, n.rpe, n.shoe_id,"
                                + " r.planned_workout_id FROM activities a LEFT JOIN"
                                + " activity_annotations n ON a.provider=n.provider AND"
                                + " a.record_id=n.record_id LEFT JOIN run_responses r ON"
                                + " r.activity_id=a.id"
                                + where
                                + " ORDER BY a.start_millis DESC, a.id"
                                + (limit == 0 ? "" : " LIMIT " + limit),
                        arguments)) {
            while (cursor.moveToNext()) {
                ActivityRecord a = FeatureJson.activity(FeatureJson.object(cursor.getString(0)));
                if (!cursor.isNull(1))
                    a =
                            annotated(
                                    a,
                                    cursor.isNull(2) ? a.notes() : cursor.getString(2),
                                    cursor.isNull(3) ? null : cursor.getDouble(3),
                                    cursor.getString(4));
                if (!cursor.isNull(5))
                    a =
                            new ActivityRecord(
                                    a.id(),
                                    a.source(),
                                    a.start(),
                                    a.end(),
                                    a.startOffset(),
                                    a.endOffset(),
                                    a.distanceMetres(),
                                    a.movingTime(),
                                    a.averageHeartRate(),
                                    a.maxHeartRate(),
                                    a.elevationGainMetres(),
                                    a.cadence(),
                                    a.rpe(),
                                    a.notes(),
                                    a.samples(),
                                    a.laps(),
                                    cursor.getString(5),
                                    a.shoeId());
                if (includeSamples) a = RecordingStore.withRetainedSamples(db, a);
                out.add(a);
            }
        }
        return out;
    }

    private static ActivityRecord annotated(
            ActivityRecord a, String notes, Double rpe, String shoe) {
        return new ActivityRecord(
                a.id(),
                a.source(),
                a.start(),
                a.end(),
                a.startOffset(),
                a.endOffset(),
                a.distanceMetres(),
                a.movingTime(),
                a.averageHeartRate(),
                a.maxHeartRate(),
                a.elevationGainMetres(),
                a.cadence(),
                rpe,
                notes,
                a.samples(),
                a.laps(),
                a.plannedWorkoutId(),
                shoe);
    }

    public void upsertManualActivity(ActivityRecord activity) {
        if (!isUserActivity(activity))
            throw new IllegalArgumentException("Use the import boundary for provider activities.");
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            putActivity(db, activity, true);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void applyImport(
            String provider,
            List<ActivityRecord> upserts,
            List<String> deletedRecordIds,
            String nextToken) {
        required(provider, "provider");
        Objects.requireNonNull(upserts);
        Objects.requireNonNull(deletedRecordIds);
        if (provider.equals("manual") || provider.equals("assessment"))
            throw new IllegalArgumentException("Manual activities do not use provider imports.");
        for (ActivityRecord a : upserts)
            if (!a.source().provider().equals(provider))
                throw new IllegalArgumentException("Import source does not match provider.");
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            for (String recordId : deletedRecordIds) {
                String[] source = {provider, required(recordId, "source record ID")};
                db.delete("preserved_best_efforts", "provider=? AND record_id=?", source);
                db.delete("activities", "provider=? AND record_id=?", source);
            }
            for (ActivityRecord a : upserts) putActivity(db, a, false);
            ContentValues checkpoint = values("provider", provider);
            checkpoint.put("token", nextToken);
            checkpoint.put("updated_at", Instant.now().toString());
            replace(db, "import_checkpoints", checkpoint);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    static void putActivity(SQLiteDatabase db, ActivityRecord a, boolean userOwned) {
        putActivity(db, a, userOwned, true);
    }

    static void putActivity(
            SQLiteDatabase db, ActivityRecord a, boolean userOwned, boolean assignDefault) {
        required(a.id(), "activity ID");
        required(a.source().provider(), "activity provider");
        required(a.source().recordId(), "source record ID");
        boolean existed = false;
        try (Cursor existing =
                db.rawQuery(
                        "SELECT id,provider,record_id FROM activities WHERE id=? OR (provider=? AND"
                                + " record_id=?)",
                        new String[] {a.id(), a.source().provider(), a.source().recordId()})) {
            while (existing.moveToNext()) {
                existed = true;
                if (!existing.getString(0).equals(a.id())
                        || !existing.getString(1).equals(a.source().provider())
                        || !existing.getString(2).equals(a.source().recordId()))
                    throw new IllegalArgumentException(
                            "Activity identity conflicts with existing data.");
            }
        }
        ContentValues v = values("id", a.id());
        v.put("provider", a.source().provider());
        v.put("record_id", a.source().recordId());
        v.put("origin_package", a.source().originPackage());
        v.put("start_millis", a.start().toEpochMilli());
        v.put("end_millis", a.end().toEpochMilli());
        v.put("payload", FeatureJson.activity(backupActivity(a)).toString());
        if (db.update("activities", v, "id=?", new String[] {a.id()}) == 0)
            db.insertOrThrow("activities", null, v);
        if (userOwned) {
            String shoe = a.shoeId();
            if (!existed && assignDefault && shoe == null) shoe = defaultShoe(db);
            putAnnotation(
                    db, a.source().provider(), a.source().recordId(), a.notes(), a.rpe(), shoe);
        } else {
            try (Cursor annotation =
                    db.rawQuery(
                            "SELECT 1 FROM activity_annotations WHERE provider=? AND record_id=?",
                            new String[] {a.source().provider(), a.source().recordId()})) {
                if (!annotation.moveToFirst()) {
                    String shoe = assignDefault ? defaultShoe(db) : null;
                    putAnnotation(
                            db, a.source().provider(), a.source().recordId(), null, null, shoe);
                }
            }
        }
        db.delete("best_efforts", "activity_id=?", new String[] {a.id()});
        db.delete(
                "preserved_best_efforts",
                "activity_id=? OR (provider=? AND record_id=?)",
                new String[] {a.id(), a.source().provider(), a.source().recordId()});
        for (RecordedResult result : BestEffortCalculator.qualifiedResults(a))
            SqliteResultRepository.putResult(db, result, true);
    }

    private static String defaultShoe(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT id FROM shoes WHERE is_default=1", null)) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    public void deleteActivity(String id) {
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            try (Cursor source =
                    db.rawQuery(
                            "SELECT provider,record_id FROM activities WHERE id=?",
                            new String[] {required(id, "activity ID")})) {
                if (source.moveToFirst())
                    db.delete(
                            "activity_annotations",
                            "provider=? AND record_id=?",
                            new String[] {source.getString(0), source.getString(1)});
            }
            db.delete("preserved_best_efforts", "activity_id=?", new String[] {id});
            db.delete("recording_sessions", "activity_id=?", new String[] {id});
            db.delete("activities", "id=?", new String[] {id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public String importToken(String provider) {
        try (Cursor c =
                read().rawQuery(
                                "SELECT token FROM import_checkpoints WHERE provider=?",
                                new String[] {required(provider, "provider")})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    // Null in storage means no app override; an explicit save of null/empty clears the note.
    public void saveActivityNotes(String id, String notes, Double rpe) {
        updateAnnotation(id, notes == null ? "" : notes, rpe, null, false);
    }

    public void assignShoe(String id, String shoeId) {
        updateAnnotation(id, null, null, shoeId, true);
    }

    private void updateAnnotation(
            String id, String notes, Double rpe, String shoe, boolean changeShoe) {
        SQLiteDatabase db = write();
        db.beginTransaction();
        try (Cursor c =
                db.rawQuery(
                        "SELECT provider,record_id FROM activities WHERE id=?",
                        new String[] {required(id, "activity ID")})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("Activity no longer exists.");
            String provider = c.getString(0),
                    record = c.getString(1),
                    oldNotes = null,
                    oldShoe = null;
            Double oldRpe = null;
            try (Cursor old =
                    db.rawQuery(
                            "SELECT notes,rpe,shoe_id FROM activity_annotations WHERE provider=?"
                                    + " AND record_id=?",
                            new String[] {provider, record})) {
                if (old.moveToFirst()) {
                    oldNotes = old.getString(0);
                    oldRpe = old.isNull(1) ? null : old.getDouble(1);
                    oldShoe = old.getString(2);
                }
            }
            putAnnotation(
                    db,
                    provider,
                    record,
                    changeShoe ? oldNotes : notes,
                    changeShoe ? oldRpe : rpe,
                    changeShoe ? shoe : oldShoe);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    static void putAnnotation(
            SQLiteDatabase db,
            String provider,
            String record,
            String notes,
            Double rpe,
            String shoe) {
        if (rpe != null && (!Double.isFinite(rpe) || rpe < 0 || rpe > 10))
            throw new IllegalArgumentException("RPE must be between 0 and 10.");
        ContentValues v = values("provider", required(provider, "annotation provider"));
        v.put("record_id", required(record, "annotation record ID"));
        v.put("notes", notes);
        if (rpe == null) v.putNull("rpe");
        else v.put("rpe", rpe);
        v.put("shoe_id", shoe);
        replace(db, "activity_annotations", v);
    }

    static ActivityRecord backupActivity(ActivityRecord a) {
        if (!a.source().provider().equals("rdd") && !a.source().provider().equals("gpx")) return a;
        return new ActivityRecord(
                a.id(),
                a.source(),
                a.start(),
                a.end(),
                a.startOffset(),
                a.endOffset(),
                a.distanceMetres(),
                a.movingTime(),
                a.averageHeartRate(),
                a.maxHeartRate(),
                a.elevationGainMetres(),
                a.cadence(),
                a.rpe(),
                a.notes(),
                java.util.Collections.emptyList(),
                a.laps(),
                a.plannedWorkoutId(),
                a.shoeId());
    }

    static boolean isUserActivity(ActivityRecord a) {
        return a.source().provider().equals("manual")
                || a.source().provider().equals("assessment")
                || a.source().provider().equals("rdd")
                || a.source().provider().equals("gpx");
    }
}
