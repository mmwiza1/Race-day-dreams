package org.racedaydreams.data;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;

import org.json.*;
import org.racedaydreams.domain.strength.GymProgramme;

import java.time.*;
import java.util.*;

/**
 * Append-only prescribed blocks; the current pointer may be disabled without deleting any history.
 */
public final class GymProgrammeStore {
    private final ProfileDatabase helper;

    public GymProgrammeStore(Context c) {
        helper = ProfileDatabase.get(c);
    }

    private SQLiteDatabase db() {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("Gym storage requires IO.");
        return helper.getWritableDatabase();
    }

    public List<GymProgramme> versions() {
        return versions(db());
    }

    /** Load only the exact preserved version needed by an active workout. */
    public GymProgramme version(String id, int revision) {
        if (id == null || id.isBlank() || revision < 1)
            throw new IllegalArgumentException("Choose a valid gym programme revision.");
        try (Cursor c =
                db().rawQuery(
                                "SELECT payload FROM gym_plan_versions WHERE version_id=?",
                                new String[] {id + ":v" + revision})) {
            if (!c.moveToFirst()) return null;
            var plan = parse(FeatureJson.object(c.getString(0)));
            if (!id.equals(plan.id()) || revision != plan.version())
                throw new IllegalArgumentException(
                        "The retained programme identity is inconsistent.");
            return plan;
        }
    }

    private static List<GymProgramme> versions(SQLiteDatabase db) {
        var out = new ArrayList<GymProgramme>();
        try (Cursor c =
                db.rawQuery("SELECT payload FROM gym_plan_versions ORDER BY rowid DESC", null)) {
            while (c.moveToNext()) out.add(parse(FeatureJson.object(c.getString(0))));
        }
        return Collections.unmodifiableList(out);
    }

    public GymProgramme current() {
        try (Cursor c =
                db().rawQuery(
                                "SELECT v.payload FROM gym_current c JOIN gym_plan_versions v ON"
                                        + " c.version_id=v.version_id WHERE c.id=1",
                                null)) {
            return c.moveToFirst() ? parse(FeatureJson.object(c.getString(0))) : null;
        }
    }

    public void save(GymProgramme plan) {
        var db = db();
        db.beginTransaction();
        try {
            var current = current();
            if (current != null
                    && (!current.id().equals(plan.id()) || plan.version() != current.version() + 1))
                throw new IllegalArgumentException(
                        "Review the current programme before saving this version.");
            insert(db, plan);
            ContentValues v = new ContentValues();
            v.put("id", 1);
            v.put("version_id", key(plan));
            db.insertWithOnConflict("gym_current", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void disable() {
        db().delete("gym_current", null, null);
    }

    public record Decision(
            String id,
            String baseVersion,
            String resultingVersion,
            String prescribedSession,
            String exerciseId,
            String choice,
            String evidence,
            Instant at) {}

    public List<Decision> decisions() {
        var out = new ArrayList<Decision>();
        try (Cursor c =
                db().rawQuery(
                                "SELECT payload FROM gym_progression_decisions ORDER BY rowid",
                                null)) {
            while (c.moveToNext()) out.add(decision(FeatureJson.object(c.getString(0))));
        }
        return Collections.unmodifiableList(out);
    }

    private static JSONObject json(Decision d) {
        return FeatureJson.json(
                "id",
                d.id(),
                "base",
                d.baseVersion(),
                "result",
                d.resultingVersion(),
                "session",
                d.prescribedSession(),
                "exercise",
                d.exerciseId(),
                "choice",
                d.choice(),
                "evidence",
                d.evidence(),
                "at",
                d.at().toString());
    }

    private static Decision decision(JSONObject j) {
        try {
            String choice = FeatureJson.required(j, "choice");
            if (!choice.equals("KEEP") && !choice.equals("INCREASE"))
                throw new IllegalArgumentException("Invalid progression choice.");
            return new Decision(
                    FeatureJson.required(j, "id"),
                    FeatureJson.required(j, "base"),
                    FeatureJson.required(j, "result"),
                    FeatureJson.required(j, "session"),
                    FeatureJson.required(j, "exercise"),
                    choice,
                    FeatureJson.required(j, "evidence"),
                    Instant.parse(FeatureJson.required(j, "at")));
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    public void decide(
            GymProgramme expected,
            GymProgramme proposed,
            String session,
            String exercise,
            String evidence) {
        var db = db();
        db.beginTransaction();
        try {
            var current = current();
            if (current == null || !key(current).equals(key(expected)))
                throw new IllegalArgumentException(
                        "The programme changed. Review the current suggestion.");
            if (proposed != null) save(proposed);
            var d =
                    new Decision(
                            UUID.randomUUID().toString(),
                            key(expected),
                            proposed == null ? key(expected) : key(proposed),
                            session,
                            exercise,
                            proposed == null ? "KEEP" : "INCREASE",
                            evidence,
                            Instant.now());
            ContentValues v = new ContentValues();
            v.put("id", d.id());
            v.put("payload", json(d).toString());
            db.insertOrThrow("gym_progression_decisions", null, v);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static String key(GymProgramme plan) {
        return plan.id() + ":v" + plan.version();
    }

    private static void insert(SQLiteDatabase db, GymProgramme p) {
        ContentValues v = new ContentValues();
        v.put("version_id", key(p));
        v.put("plan_id", p.id());
        v.put("version", p.version());
        v.put("payload", json(p).toString());
        db.insertOrThrow("gym_plan_versions", null, v);
    }

    static JSONObject json(GymProgramme p) {
        var s = p.settings();
        JSONArray days = new JSONArray();
        for (var d : DayOfWeek.values()) if (s.days().contains(d)) days.put(d.name());
        JSONArray sessions = new JSONArray();
        for (var session : p.sessions()) {
            JSONArray exercises = new JSONArray();
            for (var e : session.exercises())
                exercises.put(
                        FeatureJson.json(
                                "exerciseId",
                                e.exerciseId(),
                                "sets",
                                e.sets(),
                                "minReps",
                                e.minimumReps(),
                                "maxReps",
                                e.maximumReps(),
                                "seconds",
                                e.seconds(),
                                "metres",
                                e.metres(),
                                "minRpe",
                                e.minimumRpe(),
                                "maxRpe",
                                e.maximumRpe(),
                                "rest",
                                e.restSeconds(),
                                "intent",
                                e.intent(),
                                "referenceKg",
                                e.referenceLoadKg()));
            sessions.put(
                    FeatureJson.json(
                            "id",
                            session.id(),
                            "date",
                            session.date().toString(),
                            "title",
                            session.title(),
                            "exercises",
                            exercises,
                            "explanation",
                            session.explanation()));
        }
        return FeatureJson.json(
                "id",
                p.id(),
                "version",
                p.version(),
                "createdAt",
                p.createdAt().toString(),
                "reason",
                p.reason(),
                "goal",
                s.goal().name(),
                "priority",
                s.priority().name(),
                "experience",
                s.experience().name(),
                "days",
                days,
                "frequency",
                s.frequency(),
                "equipment",
                new JSONArray(new TreeSet<>(s.equipment())),
                "minutes",
                s.minutes(),
                "effort",
                s.effort().name(),
                "start",
                p.start().toString(),
                "end",
                p.end().toString(),
                "sessions",
                sessions,
                "tradeoffs",
                new JSONArray(p.tradeoffs()));
    }

    static GymProgramme parse(JSONObject j) {
        try {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            JSONArray a = j.getJSONArray("days");
            if (a.length() > 7) throw new IllegalArgumentException("Too many gym days.");
            for (int i = 0; i < a.length(); i++)
                if (!days.add(DayOfWeek.valueOf(a.getString(i))))
                    throw new IllegalArgumentException("Duplicate gym day.");
            Set<String> equipment = new HashSet<>();
            a = j.getJSONArray("equipment");
            if (a.length() > 20) throw new IllegalArgumentException("Too much gym equipment.");
            for (int i = 0; i < a.length(); i++) equipment.add(a.getString(i));
            var settings =
                    new GymProgramme.Settings(
                            GymProgramme.Goal.valueOf(FeatureJson.required(j, "goal")),
                            GymProgramme.Priority.valueOf(FeatureJson.required(j, "priority")),
                            GymProgramme.Experience.valueOf(FeatureJson.required(j, "experience")),
                            days,
                            FeatureJson.requiredInteger(j, "frequency"),
                            equipment,
                            FeatureJson.requiredInteger(j, "minutes"),
                            GymProgramme.Effort.valueOf(FeatureJson.required(j, "effort")));
            var sessions = new ArrayList<GymProgramme.Session>();
            a = j.getJSONArray("sessions");
            if (a.length() > 56) throw new IllegalArgumentException("Too many gym sessions.");
            for (int i = 0; i < a.length(); i++) {
                var row = a.getJSONObject(i);
                var exercises = new ArrayList<GymProgramme.Prescription>();
                var b = row.getJSONArray("exercises");
                if (b.length() > 10)
                    throw new IllegalArgumentException("Too many prescribed exercises.");
                for (int k = 0; k < b.length(); k++) {
                    var e = b.getJSONObject(k);
                    exercises.add(
                            new GymProgramme.Prescription(
                                    FeatureJson.required(e, "exerciseId"),
                                    FeatureJson.requiredInteger(e, "sets"),
                                    FeatureJson.requiredInteger(e, "minReps"),
                                    FeatureJson.requiredInteger(e, "maxReps"),
                                    FeatureJson.integer(e, "seconds"),
                                    FeatureJson.number(e, "metres"),
                                    FeatureJson.requiredInteger(e, "minRpe"),
                                    FeatureJson.requiredInteger(e, "maxRpe"),
                                    FeatureJson.requiredInteger(e, "rest"),
                                    FeatureJson.required(e, "intent"),
                                    FeatureJson.number(e, "referenceKg")));
                }
                sessions.add(
                        new GymProgramme.Session(
                                FeatureJson.required(row, "id"),
                                LocalDate.parse(FeatureJson.required(row, "date")),
                                FeatureJson.required(row, "title"),
                                exercises,
                                FeatureJson.required(row, "explanation")));
            }
            var tradeoffs = new ArrayList<String>();
            a = j.getJSONArray("tradeoffs");
            if (a.length() > 300) throw new IllegalArgumentException("Too many gym notes.");
            for (int i = 0; i < a.length(); i++) tradeoffs.add(a.getString(i));
            return new GymProgramme(
                    FeatureJson.required(j, "id"),
                    FeatureJson.requiredInteger(j, "version"),
                    Instant.parse(FeatureJson.required(j, "createdAt")),
                    FeatureJson.required(j, "reason"),
                    settings,
                    LocalDate.parse(FeatureJson.required(j, "start")),
                    LocalDate.parse(FeatureJson.required(j, "end")),
                    sessions,
                    tradeoffs);
        } catch (JSONException | java.time.DateTimeException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static JSONObject exportData(SQLiteDatabase db) {
        var versions = new JSONArray();
        for (var p : versions(db)) versions.put(json(p));
        String current = null;
        try (Cursor c = db.rawQuery("SELECT version_id FROM gym_current WHERE id=1", null)) {
            if (c.moveToFirst()) current = c.getString(0);
        }
        JSONArray decisions = new JSONArray();
        try (Cursor c =
                db.rawQuery("SELECT payload FROM gym_progression_decisions ORDER BY rowid", null)) {
            while (c.moveToNext()) decisions.put(FeatureJson.object(c.getString(0)));
        }
        return FeatureJson.json(
                "version", 1, "versions", versions, "current", current, "decisions", decisions);
    }

    static void validate(JSONObject data) {
        try {
            if (FeatureJson.requiredInteger(data, "version") != 1)
                throw new IllegalArgumentException("Unsupported gym backup.");
            JSONArray rows = data.getJSONArray("versions");
            if (rows.length() > 1000)
                throw new IllegalArgumentException("Too many programme versions.");
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < rows.length(); i++)
                if (!ids.add(key(parse(rows.getJSONObject(i)))))
                    throw new IllegalArgumentException("Duplicate gym plan version.");
            String current = FeatureJson.text(data, "current");
            if (current != null && !ids.contains(current))
                throw new IllegalArgumentException("Current gym version is missing.");
            JSONArray decisions = data.has("decisions") ? data.getJSONArray("decisions") : null;
            if (decisions != null) {
                if (decisions.length() > 100000)
                    throw new IllegalArgumentException("Too many progression decisions.");
                Set<String> decisionIds = new HashSet<>();
                for (int i = 0; i < decisions.length(); i++) {
                    var d = decision(decisions.getJSONObject(i));
                    if (!decisionIds.add(d.id())
                            || !ids.contains(d.baseVersion())
                            || !ids.contains(d.resultingVersion()))
                        throw new IllegalArgumentException(
                                "Progression version reference is missing.");
                }
            }
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static void validateLinks(JSONObject development, JSONObject coaching, JSONObject gym) {
        if (development == null) return;
        try {
            var versions = new HashMap<String, GymProgramme>();
            if (gym != null) {
                var rows = gym.getJSONArray("versions");
                for (int i = 0; i < rows.length(); i++) {
                    var p = parse(rows.getJSONObject(i));
                    versions.put(key(p), p);
                }
            }
            Set<String> actual = new HashSet<>();
            if (coaching != null) {
                var rows = coaching.getJSONArray("strength_sessions");
                for (int i = 0; i < rows.length(); i++)
                    actual.add(rows.getJSONObject(i).getString("id"));
            }
            var rows = development.getJSONArray("records");
            for (int i = 0; i < rows.length(); i++) {
                var row = rows.getJSONObject(i);
                if (!row.getString("kind").equals("STRENGTH_LINK")) continue;
                var payload = row.getJSONObject("payload");
                String version = FeatureJson.text(payload, "gymVersion");
                if (version == null) continue;
                var p = versions.get(version);
                String session = FeatureJson.required(payload, "gymSession");
                LocalDate planned = LocalDate.parse(FeatureJson.required(payload, "plannedDate"));
                if (p == null
                        || !actual.contains(row.getString("id"))
                        || p.sessions().stream()
                                .noneMatch(s -> s.id().equals(session) && s.date().equals(planned)))
                    throw new IllegalArgumentException(
                            "Gym actual/prescription reference is missing.");
            }
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static void clear(SQLiteDatabase db) {
        db.delete("gym_progression_decisions", null, null);
        db.delete("gym_current", null, null);
        db.delete("gym_plan_versions", null, null);
    }

    static void restore(SQLiteDatabase db, JSONObject data) {
        validate(data);
        try {
            var rows = data.getJSONArray("versions");
            for (int i = 0; i < rows.length(); i++) insert(db, parse(rows.getJSONObject(i)));
            JSONArray decisions = data.has("decisions") ? data.getJSONArray("decisions") : null;
            if (decisions != null)
                for (int i = 0; i < decisions.length(); i++) {
                    var d = decision(decisions.getJSONObject(i));
                    ContentValues row = new ContentValues();
                    row.put("id", d.id());
                    row.put("payload", json(d).toString());
                    db.insertOrThrow("gym_progression_decisions", null, row);
                }
            String current = FeatureJson.text(data, "current");
            if (current != null) {
                ContentValues v = new ContentValues();
                v.put("id", 1);
                v.put("version_id", current);
                db.insertOrThrow("gym_current", null, v);
            }
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }
}
