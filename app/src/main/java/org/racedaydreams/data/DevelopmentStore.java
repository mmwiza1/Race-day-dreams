package org.racedaydreams.data;

import static org.racedaydreams.data.FeatureJson.*;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/** Schema-five app-owned evidence. Typed validation is also used before transactional restore. */
public final class DevelopmentStore {
    private final ProfileDatabase helper;
    private final org.racedaydreams.storage.SqliteCoachingContext coachingContext;

    public DevelopmentStore(Context context) {
        helper = ProfileDatabase.get(context);
        coachingContext =
                new org.racedaydreams.storage.SqliteCoachingContext(
                        new AndroidSqlConnection(this::db),
                        Instant::now,
                        () -> UUID.randomUUID().toString());
    }

    private SQLiteDatabase db() {
        return helper.getWritableDatabase();
    }

    private void put(String kind, String id, JSONObject payload) {
        validatePayload(kind, id, payload);
        var db = db();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("kind", kind);
            v.put("id", id);
            v.put("payload", payload.toString());
            if (db.update("coach_evidence", v, "kind=? AND id=?", new String[] {kind, id}) == 0)
                db.insertOrThrow("coach_evidence", null, v);
            if (kind.equals("STRENGTH_LINK"))
                SqliteGymAssociationRepository.project(db, id, payload);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteWeight(String id) {
        coachingContext.deleteWeight(id);
    }

    public List<BodyWeight> weights() {
        return coachingContext.weights();
    }

    public void saveWeight(BodyWeight weight) {
        coachingContext.saveWeight(weight);
    }

    /** Replace only a successfully fetched provider window; manual data is untouched. */
    public void replaceWeightWindow(
            String provider, Instant from, Instant to, List<BodyWeight> weights) {
        coachingContext.replaceWeightWindow(provider, from, to, weights);
    }

    private static BodyWeight weight(JSONObject j) {
        try {
            return new BodyWeight(
                    required(j, "id"),
                    requiredNumber(j, "kg"),
                    Instant.parse(required(j, "at")),
                    ZoneOffset.of(required(j, "offset")),
                    required(j, "provider"),
                    required(j, "sourceId"),
                    text(j, "origin"));
        } catch (JSONException e) {
            throw invalid(e);
        }
    }

    public void saveHistorical(HistoricalStrengthRecord record) {
        coachingContext.saveHistorical(record);
    }

    public List<HistoricalStrengthRecord> historical() {
        return coachingContext.historical();
    }

    public void deleteHistorical(String id) {
        coachingContext.deleteHistorical(id);
    }

    private static HistoricalStrengthRecord historical(JSONObject j) {
        try {
            return new HistoricalStrengthRecord(
                    required(j, "id"),
                    CoachingJson.exercise(j.getJSONObject("exercise")),
                    requiredNumber(j, "kg"),
                    requiredInteger(j, "reps"),
                    text(j, "date") == null ? null : LocalDate.parse(required(j, "date")),
                    HistoricalStrengthRecord.DatePrecision.valueOf(required(j, "precision")),
                    text(j, "notes"),
                    j.isNull("seconds") ? null : requiredLong(j, "seconds"),
                    number(j, "metres"));
        } catch (JSONException e) {
            throw invalid(e);
        }
    }

    public void saveSessionDecision(WorkoutDecision d) {
        coachingContext.saveSessionDecision(d);
    }

    public List<WorkoutDecision> sessionDecisions() {
        return coachingContext.sessionDecisions();
    }

    private static WorkoutDecision sessionDecision(JSONObject j) {
        try {
            return new WorkoutDecision(
                    required(j, "id"),
                    required(j, "workoutId"),
                    required(j, "versionId"),
                    LocalDate.parse(required(j, "originalDate")),
                    text(j, "movedTo") == null ? null : LocalDate.parse(required(j, "movedTo")),
                    WorkoutDecision.Kind.valueOf(required(j, "kind")),
                    Instant.parse(required(j, "at")));
        } catch (JSONException e) {
            throw invalid(e);
        }
    }

    public Map<String, LocalDate> strengthLinks() {
        return coachingContext.strengthLinks();
    }

    public record GymLink(String actualSessionId, String versionKey, String prescribedSessionId) {}

    public Map<String, GymLink> gymLinks() {
        Map<String, GymLink> out = new HashMap<>();
        for (var entry : coachingContext.gymLinks().entrySet()) {
            var link = entry.getValue();
            out.put(
                    entry.getKey(),
                    new GymLink(
                            link.actualSessionId(), link.versionKey(), link.prescribedSessionId()));
        }
        return out;
    }

    public void linkGym(String actual, LocalDate planned, String version, String session) {
        coachingContext.linkGym(actual, planned, version, session);
    }

    public void deleteStrengthLink(String id) {
        coachingContext.deleteStrengthLink(id);
    }

    public void linkStrength(String sessionId, LocalDate planned) {
        coachingContext.linkStrength(sessionId, planned);
    }

    public WeeklyCoaching.Interruption interruption() {
        return coachingContext.interruption();
    }

    public void saveInterruption(WeeklyCoaching.Interruption i) {
        coachingContext.saveInterruption(i);
    }

    public boolean reviewKnown(String id) {
        return coachingContext.reviewKnown(id);
    }

    public String reviewDecision(String id) {
        return coachingContext.reviewDecision(id);
    }

    public String decisionForReview(WeeklyCoaching.Review review) {
        return coachingContext.decisionForReview(review);
    }

    public void saveReview(WeeklyCoaching.Review r) {
        coachingContext.saveReview(r);
    }

    public void decide(
            WeeklyCoaching.Review r,
            String choice,
            CoachingRepository coaching,
            Set<String> completed) {
        coachingContext.decide(r, choice, coaching, completed);
    }

    private static void validatePayload(String kind, String id, JSONObject j) {
        try {
            if (!id.equals(required(j, "id")))
                throw new IllegalArgumentException("Evidence identity differs.");
            switch (kind) {
                case "WEIGHT" -> weight(j);
                case "HISTORICAL_PB" -> historical(j);
                case "INTERRUPTION" ->
                        new WeeklyCoaching.Interruption(
                                LocalDate.parse(required(j, "from")),
                                LocalDate.parse(required(j, "returned")),
                                required(j, "reason"));
                case "SESSION_DECISION" -> sessionDecision(j);
                case "STRENGTH_LINK" -> {
                    LocalDate.parse(required(j, "plannedDate"));
                    if ((text(j, "gymVersion") == null) != (text(j, "gymSession") == null))
                        throw new IllegalArgumentException(
                                "Gym prescription identity is incomplete.");
                }
                case "REVIEW_DECISION" -> {
                    required(j, "base");
                    required(j, "choice");
                    required(j, "result");
                }
                case "WEEKLY_REVIEW" -> {
                    required(j, "base");
                    LocalDate.parse(required(j, "week"));
                    if (requiredInteger(j, "planned") < 0
                            || requiredInteger(j, "completed") < 0
                            || requiredNumber(j, "km") < 0)
                        throw new IllegalArgumentException("Invalid weekly summary.");
                    j.getJSONArray("observations");
                    j.getJSONArray("channels");
                    JSONArray choices = j.getJSONArray("choices");
                    for (int i = 0; i < choices.length(); i++) {
                        JSONArray changes = choices.getJSONObject(i).getJSONArray("replacements");
                        for (int k = 0; k < changes.length(); k++)
                            planned(changes.getJSONObject(k));
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported evidence kind.");
            }
        } catch (JSONException e) {
            throw invalid(e);
        }
    }

    static JSONObject exportData(SQLiteDatabase db) {
        JSONArray out = new JSONArray();
        try (Cursor c =
                db.rawQuery("SELECT kind,id,payload FROM coach_evidence ORDER BY kind,id", null)) {
            while (c.moveToNext()) {
                JSONObject p = object(c.getString(2));
                if (c.getString(0).equals("WEIGHT") && !"manual".equals(p.optString("provider")))
                    continue;
                out.put(json("kind", c.getString(0), "id", c.getString(1), "payload", p));
            }
        }
        return json("version", 1, "records", out);
    }

    static void validate(JSONObject data) {
        try {
            if (requiredInteger(data, "version") != 1)
                throw new IllegalArgumentException("Unsupported evidence format.");
            JSONArray a = data.getJSONArray("records");
            if (a.length() > 100000)
                throw new IllegalArgumentException("Too many evidence records.");
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject r = a.getJSONObject(i);
                String kind = required(r, "kind"), id = required(r, "id");
                if (!ids.add(kind + ":" + id))
                    throw new IllegalArgumentException("Duplicated evidence.");
                validatePayload(kind, id, r.getJSONObject("payload"));
            }
        } catch (JSONException e) {
            throw invalid(e);
        }
    }

    static void clear(SQLiteDatabase db) {
        db.delete("coach_evidence", null, null);
    }

    static void restore(SQLiteDatabase db, JSONObject data) {
        try {
            JSONArray a = data.getJSONArray("records");
            for (int i = 0; i < a.length(); i++) {
                JSONObject r = a.getJSONObject(i);
                ContentValues v = new ContentValues();
                v.put("kind", r.getString("kind"));
                v.put("id", r.getString("id"));
                v.put("payload", r.getJSONObject("payload").toString());
                db.insertOrThrow("coach_evidence", null, v);
            }
        } catch (JSONException e) {
            throw invalid(e);
        }
    }
}
