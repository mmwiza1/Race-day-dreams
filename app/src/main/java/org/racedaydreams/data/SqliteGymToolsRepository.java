package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.strength.GymRestTimer;

import java.util.*;
import java.util.function.UnaryOperator;

/** Typed advisory state on the shared transaction pool. Never writes actual sets/prescriptions. */
public final class SqliteGymToolsRepository extends SqliteStoreAccess {
    public SqliteGymToolsRepository(Context context) {
        super(context);
    }

    public GymRestTimer.Preferences preferences() {
        return preferences(read());
    }

    private static GymRestTimer.Preferences preferences(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT default_seconds,suggest_after_set,notifications,vibration,audio"
                                + " FROM gym_rest_preferences WHERE id=1",
                        null)) {
            return c.moveToFirst()
                    ? new GymRestTimer.Preferences(
                            c.getInt(0),
                            c.getInt(1) == 1,
                            c.getInt(2) == 1,
                            c.getInt(3) == 1,
                            c.getInt(4) == 1)
                    : GymRestTimer.Preferences.defaults();
        }
    }

    public void preferences(GymRestTimer.Preferences value) {
        putPreferences(write(), value);
    }

    private static void putPreferences(SQLiteDatabase db, GymRestTimer.Preferences p) {
        var v = new ContentValues();
        v.put("id", 1);
        v.put("default_seconds", p.defaultSeconds());
        v.put("suggest_after_set", p.suggestAfterSet() ? 1 : 0);
        v.put("notifications", p.notifications() ? 1 : 0);
        v.put("vibration", p.vibration() ? 1 : 0);
        v.put("audio", p.audio() ? 1 : 0);
        replace(db, "gym_rest_preferences", v);
    }

    public GymRestTimer timer(String execution) {
        return timer(read(), execution);
    }

    private static GymRestTimer timer(SQLiteDatabase db, String execution) {
        try (var c =
                db.rawQuery(
                        "SELECT"
                            + " trigger_id,phase,epoch,observed_millis,remaining_millis,duration_millis"
                            + " FROM gym_rest_timers WHERE execution_id=?",
                        new String[] {execution})) {
            return c.moveToFirst()
                    ? new GymRestTimer(
                            execution,
                            c.getString(0),
                            GymRestTimer.Phase.valueOf(c.getString(1)),
                            c.getString(2),
                            c.getLong(3),
                            c.getLong(4),
                            c.getLong(5))
                    : null;
        }
    }

    public GymRestTimer change(String execution, UnaryOperator<GymRestTimer> command) {
        var db = write();
        db.beginTransaction();
        try {
            try (var c =
                    db.rawQuery(
                            "SELECT state FROM strength_executions WHERE id=?",
                            new String[] {execution})) {
                if (!c.moveToFirst()
                        || !org.racedaydreams.domain.coaching.Immutable.set(
                                        "ACTIVE", "PAUSED", "RECOVERY_REQUIRED")
                                .contains(c.getString(0)))
                    throw new IllegalArgumentException("Rest belongs to an unfinished workout.");
            }
            var value = Objects.requireNonNull(command.apply(timer(db, execution)));
            if (!execution.equals(value.executionId()))
                throw new IllegalArgumentException("Rest workout identity changed.");
            putTimer(db, value);
            db.setTransactionSuccessful();
            return value;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * A stable notification trigger can be acknowledged only once; stale alarms cannot finish newer
     * rest.
     */
    public boolean completeIfDue(String execution, String trigger, GymRestTimer.Clock now) {
        var db = write();
        db.beginTransaction();
        try {
            var saved = timer(db, execution);
            if (saved == null
                    || !saved.triggerId().equals(trigger)
                    || saved.phase() != GymRestTimer.Phase.RUNNING) return false;
            try (var c =
                    db.rawQuery(
                            "SELECT state FROM strength_executions WHERE id=?",
                            new String[] {execution})) {
                if (!c.moveToFirst()
                        || !org.racedaydreams.domain.coaching.Immutable.set(
                                        "ACTIVE", "PAUSED", "RECOVERY_REQUIRED")
                                .contains(c.getString(0))) return false;
            }
            var observed = saved.observe(now);
            if (observed.phase() != GymRestTimer.Phase.COMPLETE) return false;
            putTimer(db, observed);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    private static void putTimer(SQLiteDatabase db, GymRestTimer t) {
        var v = values("execution_id", t.executionId());
        v.put("trigger_id", t.triggerId());
        v.put("phase", t.phase().name());
        v.put("epoch", t.epoch());
        v.put("observed_millis", t.observedMillis());
        v.put("remaining_millis", t.remainingMillis());
        v.put("duration_millis", t.durationMillis());
        replace(db, "gym_rest_timers", v);
    }

    public static JSONObject exportData(SQLiteDatabase db) {
        var p = preferences(db);
        var rows = new JSONArray();
        try (var c =
                db.rawQuery(
                        "SELECT"
                            + " execution_id,trigger_id,phase,epoch,observed_millis,remaining_millis,duration_millis"
                            + " FROM gym_rest_timers ORDER BY execution_id",
                        null)) {
            while (c.moveToNext()) {
                var t =
                        new GymRestTimer(
                                c.getString(0),
                                c.getString(1),
                                GymRestTimer.Phase.valueOf(c.getString(2)),
                                c.getString(3),
                                c.getLong(4),
                                c.getLong(5),
                                c.getLong(6));
                rows.put(
                        object(
                                "execution",
                                t.executionId(),
                                "trigger",
                                t.triggerId(),
                                "phase",
                                t.phase().name(),
                                "epoch",
                                t.epoch(),
                                "observed",
                                t.observedMillis(),
                                "remaining",
                                t.remainingMillis(),
                                "duration",
                                t.durationMillis()));
            }
        }
        return object(
                "version",
                1,
                "preferences",
                object(
                        "defaultSeconds",
                        p.defaultSeconds(),
                        "suggestAfterSet",
                        p.suggestAfterSet(),
                        "notifications",
                        p.notifications(),
                        "vibration",
                        p.vibration(),
                        "audio",
                        p.audio()),
                "timers",
                rows);
    }

    public record Snapshot(GymRestTimer.Preferences preferences, List<GymRestTimer> timers) {}

    public static Snapshot validate(JSONObject data, JSONObject engine) {
        if (data == null)
            return new Snapshot(
                    GymRestTimer.Preferences.defaults(),
                    org.racedaydreams.domain.coaching.Immutable.list());
        fields(data, "version", "preferences", "timers");
        if (integer(data, "version") != 1) throw invalid("unsupported gym tools version");
        var p = child(data, "preferences");
        fields(p, "defaultSeconds", "suggestAfterSet", "notifications", "vibration", "audio");
        var preferences =
                new GymRestTimer.Preferences(
                        integer(p, "defaultSeconds"),
                        bool(p, "suggestAfterSet"),
                        bool(p, "notifications"),
                        bool(p, "vibration"),
                        bool(p, "audio"));
        var known = new HashSet<String>();
        if (engine != null) {
            var executions = array(engine, "executions", 100000);
            for (int i = 0; i < executions.length(); i++)
                known.add(text(child(row(executions, i), "snapshot"), "id", 128));
        }
        var rows = array(data, "timers", 100000);
        var result = new ArrayList<GymRestTimer>();
        var seen = new HashSet<String>();
        for (int i = 0; i < rows.length(); i++) {
            var r = row(rows, i);
            fields(
                    r,
                    "execution",
                    "trigger",
                    "phase",
                    "epoch",
                    "observed",
                    "remaining",
                    "duration");
            var t =
                    new GymRestTimer(
                            text(r, "execution", 128),
                            text(r, "trigger", 128),
                            choice(r, "phase", GymRestTimer.Phase.class),
                            text(r, "epoch", 128),
                            whole(r, "observed"),
                            whole(r, "remaining"),
                            whole(r, "duration"));
            if (!known.contains(t.executionId()) || !seen.add(t.executionId()))
                throw invalid("orphan or duplicate rest timer");
            result.add(t);
        }
        return new Snapshot(
                preferences, org.racedaydreams.domain.coaching.Immutable.copyList(result));
    }

    public static void restore(SQLiteDatabase db, Snapshot snapshot) {
        db.delete("gym_rest_timers", null, null);
        db.delete("gym_rest_preferences", null, null);
        putPreferences(db, snapshot.preferences());
        for (var t : snapshot.timers()) putTimer(db, t);
    }
}
