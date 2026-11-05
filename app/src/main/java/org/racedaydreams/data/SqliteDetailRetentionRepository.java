package org.racedaydreams.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.storage.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Explicit, bounded raw-only cleanup. Every mutation shares the app database transaction. */
public final class SqliteDetailRetentionRepository extends SqliteStoreAccess {
    private final SqliteRecordingInventoryRepository inventory;

    public SqliteDetailRetentionRepository(Context context) {
        super(context);
        inventory = new SqliteRecordingInventoryRepository(context);
    }

    public DetailRetention.Preferences preferences() {
        return preferences(read());
    }

    static DetailRetention.Preferences preferences(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT policy,choice,chosen_at,choice_id FROM detail_retention_preferences"
                                + " WHERE id=1",
                        null)) {
            if (!c.moveToFirst())
                throw new IllegalStateException(
                        "Retention preferences missing; original detail retained.");
            return new DetailRetention.Preferences(
                    DetailRetention.Policy.valueOf(c.getString(0)),
                    DetailRetention.Choice.valueOf(c.getString(1)),
                    c.isNull(2) ? null : Instant.parse(c.getString(2)),
                    c.isNull(3) ? null : c.getString(3));
        }
    }

    /**
     * Called only after the athlete has read the consequences, including existing-user protection.
     */
    public void acknowledge(DetailRetention.Policy policy, Instant now) {
        var db = write();
        db.beginTransaction();
        try {
            setPreferences(
                    db,
                    DetailRetention.Preferences.acknowledged(
                            policy, now, UUID.randomUUID().toString()));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void setPreferences(SQLiteDatabase db, DetailRetention.Preferences value) {
        var v = values("id", "1");
        v.put("policy", value.policy().name());
        v.put("choice", value.choice().name());
        v.put("chosen_at", value.chosenAt() == null ? null : value.chosenAt().toString());
        v.put("choice_id", value.choiceId());
        replace(db, "detail_retention_preferences", v);
    }

    public DetailRetention.Preview preview(DetailRetention.Policy policy, Instant now) {
        var db = read();
        db.beginTransaction();
        try {
            var result =
                    DetailRetention.preview(
                            policy, inventory.inspect().recordings(), now, epoch(db));
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public DetailRetention.Approval approve(DetailRetention.Preview preview, Instant now) {
        var db = read();
        db.beginTransaction();
        try {
            var result =
                    DetailRetention.approve(
                            preview, preferences(db), now, epoch(db), unfinished(db), false);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    /** One complete session per transaction. A process interruption rolls this whole batch back. */
    public boolean cleanOne(DetailRetention.Approval approval, String sessionId, Instant now) {
        var db = write();
        db.beginTransaction();
        try {
            // Recheck before even an idempotent retry; restored data cannot reuse old consent.
            DetailRetention.checkBatch(
                    approval,
                    preferences(db),
                    now,
                    epoch(db),
                    Immutable.list(),
                    unfinished(db),
                    false);
            var approved =
                    approval.preview().recordings().stream()
                            .filter(r -> r.sessionId().equals(sessionId))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Recording was not included in the approved"
                                                            + " preview."));
            var row = SqliteRecordingInventoryRepository.recording(db, sessionId);
            if (row != null && row.points() == 0 && row.events() == 0) {
                var receipt = removal(db, sessionId);
                if (receipt != null
                        && receipt.activityId().equals(approved.activityId())
                        && receipt.points() == approved.points()
                        && receipt.events() == approved.events()) {
                    db.setTransactionSuccessful();
                    return false;
                }
            }
            if (row == null || !row.equals(approved))
                throw new IllegalStateException(
                        "Recording changed after preview; original detail retained.");
            DetailRetention.checkBatch(
                    approval,
                    preferences(db),
                    now,
                    epoch(db),
                    Immutable.list(row),
                    unfinished(db),
                    false);
            RemovedRecordingDetail proof;
            try (var c =
                    db.rawQuery(
                            "SELECT started_at,started_mono,last_mono FROM recording_sessions WHERE"
                                    + " id=? AND state='FINISHED' AND activity_id=?",
                            new String[] {row.sessionId(), row.activityId()})) {
                if (!c.moveToFirst())
                    throw new IllegalStateException("Finished recording changed.");
                proof =
                        new RemovedRecordingDetail(
                                row.sessionId(),
                                row.activityId(),
                                row.provider(),
                                row.sourceRecordId(),
                                row.endedAt(),
                                now,
                                approval.preview().policy(),
                                Instant.parse(c.getString(0)),
                                c.getLong(1),
                                c.getLong(2),
                                row.points(),
                                row.events(),
                                evidenceHash(db, "recording_points", row.sessionId()),
                                evidenceHash(db, "recording_events", row.sessionId()));
            }
            putRemoval(db, proof);
            if (db.delete("recording_points", "session_id=?", new String[] {row.sessionId()})
                            != row.points()
                    || db.delete("recording_events", "session_id=?", new String[] {row.sessionId()})
                            != row.events())
                throw new IllegalStateException("Recording detail changed; cleanup rolled back.");
            // Never putActivity here: qualified/preserved efforts and canonical totals stay exact.
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    static boolean unfinished(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT 1 FROM recording_sessions WHERE state IN"
                                + " ('ACTIVE','PAUSED','RECOVERABLE') LIMIT 1",
                        null)) {
            return c.moveToFirst();
        }
    }

    static String epoch(SQLiteDatabase db) {
        try (var c = db.rawQuery("SELECT epoch FROM detail_retention_state WHERE id=1", null)) {
            if (!c.moveToFirst()) throw new IllegalStateException("Storage identity missing.");
            return c.getString(0);
        }
    }

    static void invalidateApprovals(SQLiteDatabase db) {
        var v = values("epoch", UUID.randomUUID().toString());
        if (db.update("detail_retention_state", v, "id=1", null) != 1)
            throw new IllegalStateException("Storage identity missing.");
    }

    static String evidenceHash(SQLiteDatabase db, String table, String id) {
        if (!table.equals("recording_points") && !table.equals("recording_events"))
            throw new IllegalArgumentException("Unsupported evidence.");
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var c =
                    db.rawQuery(
                            "SELECT sequence,payload FROM "
                                    + table
                                    + " WHERE session_id=? ORDER BY sequence",
                            new String[] {id})) {
                while (c.moveToNext())
                    digest.update(
                            (c.getLong(0) + ":" + c.getString(1) + "\n")
                                    .getBytes(StandardCharsets.UTF_8));
            }
            var out = new StringBuilder();
            for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public RemovedRecordingDetail removal(String sessionId) {
        return removal(read(), required(sessionId, "recording"));
    }

    private static final String REMOVAL_COLUMNS =
            "activity_id,provider,record_id,ended_at,removed_at,policy,started_at,started_mono,last_mono,point_count,event_count,points_hash,events_hash";

    static RemovedRecordingDetail removal(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT "
                                + REMOVAL_COLUMNS
                                + " FROM recording_detail_removals WHERE session_id=?",
                        new String[] {id})) {
            return c.moveToFirst() ? readRemovalCursor(id, c) : null;
        }
    }

    private static RemovedRecordingDetail readRemovalCursor(String id, android.database.Cursor c) {
        return new RemovedRecordingDetail(
                id,
                c.getString(0),
                c.getString(1),
                c.getString(2),
                Instant.parse(c.getString(3)),
                Instant.parse(c.getString(4)),
                DetailRetention.Policy.valueOf(c.getString(5)),
                Instant.parse(c.getString(6)),
                c.getLong(7),
                c.getLong(8),
                c.getLong(9),
                c.getLong(10),
                c.getString(11),
                c.getString(12));
    }

    static void putRemoval(SQLiteDatabase db, RemovedRecordingDetail row) {
        var v = values("session_id", row.sessionId());
        v.put("activity_id", row.activityId());
        v.put("provider", row.provider());
        v.put("record_id", row.recordId());
        v.put("ended_at", row.endedAt().toString());
        v.put("removed_at", row.removedAt().toString());
        v.put("policy", row.policy().name());
        v.put("started_at", row.startedAt().toString());
        v.put("started_mono", row.startedMono());
        v.put("last_mono", row.lastMono());
        v.put("point_count", row.points());
        v.put("event_count", row.events());
        v.put("points_hash", row.pointsHash());
        v.put("events_hash", row.eventsHash());
        if (db.update(
                        "recording_detail_removals",
                        v,
                        "session_id=?",
                        new String[] {row.sessionId()})
                == 0) db.insertOrThrow("recording_detail_removals", null, v);
    }

    static JSONObject removalJson(RemovedRecordingDetail row) {
        return FeatureJson.json(
                "session",
                row.sessionId(),
                "activity",
                row.activityId(),
                "provider",
                row.provider(),
                "record",
                row.recordId(),
                "end",
                row.endedAt().toString(),
                "removed",
                row.removedAt().toString(),
                "policy",
                row.policy().name(),
                "start",
                row.startedAt().toString(),
                "mono",
                row.startedMono(),
                "last",
                row.lastMono(),
                "points",
                row.points(),
                "events",
                row.events(),
                "pointsHash",
                row.pointsHash(),
                "eventsHash",
                row.eventsHash());
    }

    static RemovedRecordingDetail readRemoval(JSONObject j) throws JSONException {
        GymJsonSupport.fields(
                j,
                "session",
                "activity",
                "provider",
                "record",
                "end",
                "removed",
                "policy",
                "start",
                "mono",
                "last",
                "points",
                "events",
                "pointsHash",
                "eventsHash");
        return new RemovedRecordingDetail(
                FeatureJson.required(j, "session"),
                FeatureJson.required(j, "activity"),
                FeatureJson.required(j, "provider"),
                FeatureJson.required(j, "record"),
                Instant.parse(j.getString("end")),
                Instant.parse(j.getString("removed")),
                DetailRetention.Policy.valueOf(j.getString("policy")),
                Instant.parse(j.getString("start")),
                FeatureJson.requiredLong(j, "mono"),
                FeatureJson.requiredLong(j, "last"),
                FeatureJson.requiredLong(j, "points"),
                FeatureJson.requiredLong(j, "events"),
                j.getString("pointsHash"),
                j.getString("eventsHash"));
    }

    public record Snapshot(
            DetailRetention.Preferences preferences, List<RemovedRecordingDetail> removals) {
        public Snapshot {
            Objects.requireNonNull(preferences);
            removals = Immutable.copyList(removals);
            if (removals.size() > 100000)
                throw new IllegalArgumentException(
                        "Too many detail receipts; nothing was truncated.");
            var ids = new HashSet<String>();
            var activities = new HashSet<String>();
            for (var r : removals)
                if (!ids.add(r.sessionId()) || !activities.add(r.activityId()))
                    throw new IllegalArgumentException("Duplicate removed-detail identity.");
        }
    }

    static JSONObject exportData(SQLiteDatabase db) throws JSONException {
        var p = preferences(db);
        var rows = new JSONArray();
        try (var c =
                db.rawQuery(
                        "SELECT "
                                + REMOVAL_COLUMNS
                                + ",session_id FROM recording_detail_removals ORDER BY session_id",
                        null)) {
            while (c.moveToNext()) {
                if (rows.length() >= 100000)
                    throw new IllegalArgumentException(
                            "Detail receipt export limit exceeded; nothing was truncated.");
                rows.put(removalJson(readRemovalCursor(c.getString(13), c)));
            }
        }
        return FeatureJson.json(
                "version",
                1,
                "policy",
                p.policy().name(),
                "choice",
                p.choice().name(),
                "chosenAt",
                p.chosenAt() == null ? null : p.chosenAt().toString(),
                "choiceId",
                p.choiceId(),
                "removals",
                rows);
    }

    static Snapshot validate(JSONObject j) throws JSONException {
        if (j == null)
            return new Snapshot(DetailRetention.Preferences.existing(), Immutable.list());
        GymJsonSupport.fields(j, "version", "policy", "choice", "chosenAt", "choiceId", "removals");
        if (FeatureJson.requiredInteger(j, "version") != 1)
            throw new IllegalArgumentException("Unsupported detail-retention format.");
        var prefs =
                new DetailRetention.Preferences(
                        DetailRetention.Policy.valueOf(j.getString("policy")),
                        DetailRetention.Choice.valueOf(j.getString("choice")),
                        j.isNull("chosenAt") ? null : Instant.parse(j.getString("chosenAt")),
                        j.isNull("choiceId") ? null : j.getString("choiceId"));
        var a = j.getJSONArray("removals");
        if (a.length() > 100000) throw new IllegalArgumentException("Too many detail receipts.");
        var rows = new ArrayList<RemovedRecordingDetail>();
        for (int i = 0; i < a.length(); i++) rows.add(readRemoval(a.getJSONObject(i)));
        return new Snapshot(prefs, rows);
    }

    static void restore(SQLiteDatabase db, Snapshot snapshot) {
        setPreferences(db, snapshot.preferences());
        db.delete("recording_detail_removals", null, null);
        for (var row : snapshot.removals()) putRemoval(db, row);
        reconcileAvailable(db);
        invalidateApprovals(db);
    }

    /** Metadata backup must never hide, overwrite or remove available conflicting raw evidence. */
    static void reconcileAvailable(SQLiteDatabase db) {
        var restored = new ArrayList<String>();
        try (var c =
                db.rawQuery(
                        "SELECT r.session_id FROM recording_detail_removals r WHERE EXISTS(SELECT 1"
                                + " FROM recording_points p WHERE p.session_id=r.session_id) OR"
                                + " EXISTS(SELECT 1 FROM recording_events e WHERE"
                                + " e.session_id=r.session_id)",
                        null)) {
            while (c.moveToNext()) {
                var id = c.getString(0);
                var row = removal(db, id);
                requireMatches(db, row);
                restored.add(id);
            }
        }
        for (var id : restored)
            db.delete("recording_detail_removals", "session_id=?", new String[] {id});
    }

    static void requireMatches(SQLiteDatabase db, RemovedRecordingDetail row) {
        for (var pair :
                Immutable.list(
                        new String[] {
                            "recording_points", Long.toString(row.points()), row.pointsHash()
                        },
                        new String[] {
                            "recording_events", Long.toString(row.events()), row.eventsHash()
                        })) {
            try (var c =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM " + pair[0] + " WHERE session_id=?",
                            new String[] {row.sessionId()})) {
                if (!c.moveToFirst()
                        || c.getLong(0) != Long.parseLong(pair[1])
                        || !evidenceHash(db, pair[0], row.sessionId()).equals(pair[2]))
                    throw new IllegalArgumentException(
                            "Archived detail conflicts with the original removed evidence; existing"
                                    + " data retained.");
            }
        }
    }
}
