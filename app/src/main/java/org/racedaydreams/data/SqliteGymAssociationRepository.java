package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.strength.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One shared SQLite transaction owns choice history, indexed targets and the coaching projection.
 */
public final class SqliteGymAssociationRepository extends SqliteStoreAccess
        implements GymAssociationRepository {
    static final int ROW_BYTES = 524288;
    private final SqliteGymExecutionRepository executions;
    private final GymProgrammeStore programmes;

    public SqliteGymAssociationRepository(Context context) {
        super(context);
        executions = new SqliteGymExecutionRepository(context);
        programmes = new GymProgrammeStore(context);
    }

    @Override
    public List<GymWorkoutAssociation.Proposal> proposals(String actualId, Instant at) {
        var db = read();
        db.beginTransactionNonExclusive();
        try {
            var stored = executions.load(required(actualId, "actual workout"));
            if (stored == null
                    || stored.execution().status() != GymExecution.Status.FINISHED
                    || linked(db, actualId)) return java.util.Collections.emptyList();
            var plan = programmes.current();
            return GymWorkoutAssociation.proposals(
                    actual(db, actualId),
                    stored.execution().origin(),
                    plan,
                    plan == null
                            ? java.util.Collections.emptySet()
                            : occupied(db, GymProgrammeStore.key(plan), actualId),
                    at);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public GymWorkoutAssociation.Decision current(String actualId) {
        try (var c =
                read().rawQuery(
                                "SELECT d.payload FROM strength_association_targets t JOIN"
                                        + " strength_association_decisions d ON t.decision_id=d.id"
                                        + " WHERE t.actual_id=?",
                                new String[] {required(actualId, "actual workout")})) {
            return c.moveToFirst() ? GymAssociationJson.read(c.getString(0)) : null;
        }
    }

    @Override
    public GymWorkoutAssociation.Proposal reviewCurrent(String actualId, Instant at) {
        var db = read();
        db.beginTransactionNonExclusive();
        try {
            var d = current(actualId);
            if (d == null) return null;
            var stored = executions.load(actualId);
            if (stored == null || stored.execution().status() != GymExecution.Status.FINISHED)
                throw conflict();
            var p = d.proposal();
            var review =
                    new GymWorkoutAssociation.Proposal(
                            actual(db, actualId),
                            stored.execution().origin(),
                            p.programmeId(),
                            p.programmeRevision(),
                            p.prescription(),
                            at);
            GymWorkoutAssociation.validateRetainedEvidence(
                    review,
                    review.actual(),
                    stored.execution().origin(),
                    programmes.version(p.programmeId(), p.programmeRevision()));
            return review;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public GymWorkoutAssociation.Decision decide(GymWorkoutAssociation.Decision decision) {
        Objects.requireNonNull(decision);
        String payload = bounded(decision);
        var db = write();
        db.beginTransaction();
        try {
            try (var c =
                    db.rawQuery(
                            "SELECT payload FROM strength_association_decisions WHERE id=?",
                            new String[] {decision.id()})) {
                if (c.moveToFirst()) {
                    var previous = GymAssociationJson.read(c.getString(0));
                    if (!previous.equals(decision)
                            || !decision.equals(current(decision.proposal().actual().id())))
                        throw conflict();
                    db.setTransactionSuccessful();
                    return previous;
                }
            }
            var p = decision.proposal();
            String id = p.actual().id();
            var stored = executions.load(id);
            if (stored == null || stored.execution().status() != GymExecution.Status.FINISHED)
                throw conflict();
            var plan =
                    decision.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
                            ? programmes.current()
                            : programmes.version(p.programmeId(), p.programmeRevision());
            if (decision.decidedAt().isAfter(Instant.now())) throw conflict();
            if (decision.choice() == GymWorkoutAssociation.Choice.KEEP_EXTRA && linked(db, id)) {
                try (var c =
                        db.rawQuery(
                                "SELECT programme_key,session_id FROM strength_association_targets"
                                        + " WHERE actual_id=?",
                                new String[] {id})) {
                    if (!c.moveToFirst()
                            || !key(p).equals(c.getString(0))
                            || !p.prescription().id().equals(c.getString(1))) throw conflict();
                }
            }
            if (decision.choice() == GymWorkoutAssociation.Choice.KEEP_EXTRA)
                GymWorkoutAssociation.validateRetainedEvidence(
                        p, actual(db, id), stored.execution().origin(), plan);
            else
                GymWorkoutAssociation.validateForAcceptance(
                        p,
                        actual(db, id),
                        stored.execution().origin(),
                        plan,
                        occupied(db, key(p), id),
                        linked(db, id));
            var row = new ContentValues();
            row.put("id", decision.id());
            row.put("actual_id", id);
            row.put("decided_at", decision.decidedAt().toString());
            row.put("payload", payload);
            db.insertOrThrow("strength_association_decisions", null, row);
            if (decision.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION) {
                var link =
                        object(
                                "id",
                                id,
                                "plannedDate",
                                p.prescription().date().toString(),
                                "gymVersion",
                                key(p),
                                "gymSession",
                                p.prescription().id());
                row = new ContentValues();
                row.put("kind", "STRENGTH_LINK");
                row.put("id", id);
                row.put("payload", link.toString());
                db.insertWithOnConflict(
                        "coach_evidence", null, row, SQLiteDatabase.CONFLICT_REPLACE);
            } else db.delete("coach_evidence", "kind='STRENGTH_LINK' AND id=?", new String[] {id});
            setCurrent(db, decision);
            db.setTransactionSuccessful();
            return decision;
        } finally {
            db.endTransaction();
        }
    }

    private static String key(GymWorkoutAssociation.Proposal p) {
        return p.programmeId() + ":v" + p.programmeRevision();
    }

    private static IllegalArgumentException conflict() {
        return new IllegalArgumentException(
                "The workout or association changed. Review the saved evidence again.");
    }

    private static String bounded(GymWorkoutAssociation.Decision d) {
        String value = GymAssociationJson.write(d);
        if (value.getBytes(StandardCharsets.UTF_8).length > ROW_BYTES)
            throw new IllegalArgumentException(
                    "This association snapshot exceeds the supported size. Actual training was"
                            + " preserved.");
        return value;
    }

    private static StrengthSession actual(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT payload FROM strength_sessions WHERE id=?", new String[] {id})) {
            if (!c.moveToFirst()) throw conflict();
            try {
                return CoachingJson.strength(FeatureJson.object(c.getString(0)));
            } catch (JSONException invalid) {
                throw FeatureJson.invalid(invalid);
            }
        }
    }

    private static boolean linked(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT 1 FROM strength_association_targets WHERE actual_id=? AND"
                                + " planned_date IS NOT NULL",
                        new String[] {id})) {
            return c.moveToFirst();
        }
    }

    private static Set<String> occupied(SQLiteDatabase db, String key, String except) {
        var out = new HashSet<String>();
        try (var c =
                db.rawQuery(
                        "SELECT session_id FROM strength_association_targets WHERE programme_key=?"
                                + " AND actual_id<>?",
                        new String[] {key, except})) {
            while (c.moveToNext()) out.add(c.getString(0));
        }
        return out;
    }

    private static void setCurrent(SQLiteDatabase db, GymWorkoutAssociation.Decision d) {
        var p = d.proposal();
        var row = new ContentValues();
        row.put("actual_id", p.actual().id());
        row.put("decision_id", d.id());
        if (d.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION) {
            row.put("programme_key", key(p));
            row.put("session_id", p.prescription().id());
            row.put("planned_date", p.prescription().date().toString());
        } else {
            row.putNull("programme_key");
            row.putNull("session_id");
            row.putNull("planned_date");
        }
        db.insertWithOnConflict(
                "strength_association_targets", null, row, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Legacy links remain legacy; a migration never invents an athlete decision. */
    static void project(SQLiteDatabase db, String id, JSONObject link) {
        String version, session, date;
        try {
            version = FeatureJson.text(link, "gymVersion");
            session = FeatureJson.text(link, "gymSession");
            date = FeatureJson.text(link, "plannedDate");
        } catch (JSONException invalid) {
            throw FeatureJson.invalid(invalid);
        }
        try (var c =
                db.rawQuery(
                        "SELECT programme_key,session_id,planned_date,decision_id FROM"
                                + " strength_association_targets WHERE actual_id=?",
                        new String[] {id})) {
            if (c.moveToFirst() && !c.isNull(3)) {
                if (!Objects.equals(version, c.getString(0))
                        || !Objects.equals(session, c.getString(1))
                        || !Objects.equals(date, c.getString(2))) throw conflict();
                return;
            }
        }
        var row = new ContentValues();
        row.put("actual_id", id);
        row.put("programme_key", version);
        row.put("session_id", session);
        row.put("planned_date", date);
        row.putNull("decision_id");
        db.insertWithOnConflict(
                "strength_association_targets", null, row, SQLiteDatabase.CONFLICT_REPLACE);
    }

    static void rebuildProjection(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT id,payload FROM coach_evidence WHERE kind='STRENGTH_LINK'", null)) {
            while (c.moveToNext()) project(db, c.getString(0), FeatureJson.object(c.getString(1)));
        }
    }

    static void deleteActual(SQLiteDatabase db, String id) {
        db.delete("strength_association_targets", "actual_id=?", new String[] {id});
        db.delete("strength_association_decisions", "actual_id=?", new String[] {id});
    }

    static void removeLegacyLink(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT decision_id FROM strength_association_targets WHERE actual_id=?",
                        new String[] {id})) {
            if (c.moveToFirst() && !c.isNull(0))
                throw new IllegalArgumentException(
                        "Review the explicit association choice before removing it.");
        }
        db.delete("strength_association_targets", "actual_id=?", new String[] {id});
    }

    static void clear(SQLiteDatabase db) {
        db.delete("strength_association_targets", null, null);
        db.delete("strength_association_decisions", null, null);
    }

    static JSONObject exportData(SQLiteDatabase db) {
        var decisions = new JSONArray();
        var current = new JSONArray();
        try (var c =
                db.rawQuery(
                        "SELECT payload FROM strength_association_decisions ORDER BY id", null)) {
            while (c.moveToNext()) decisions.put(GymJsonSupport.read(c.getString(0)));
        }
        try (var c =
                db.rawQuery(
                        "SELECT actual_id,decision_id FROM strength_association_targets WHERE"
                                + " decision_id IS NOT NULL ORDER BY actual_id",
                        null)) {
            while (c.moveToNext())
                current.put(object("actualId", c.getString(0), "decisionId", c.getString(1)));
        }
        return object("version", 1, "decisions", decisions, "current", current);
    }

    /**
     * Graph validation occurs before replacement; historical choices do not rewrite current
     * actuals.
     */
    private record TargetKey(String programme, String session) {}

    static void validate(
            JSONObject data,
            java.util.Map<String, GymExecution> executions,
            java.util.Map<String, StrengthSession> actuals,
            java.util.Map<String, GymProgramme> programmes,
            java.util.Map<String, JSONObject> links) {
        fields(data, "version", "decisions", "current");
        version(data, 1);
        var decisions = new java.util.HashMap<String, GymWorkoutAssociation.Decision>();
        var actualIds = new HashSet<String>();
        var activeIds = new HashSet<String>();
        var rows = array(data, "decisions", 100000);
        for (int i = 0; i < rows.length(); i++) {
            var d = GymAssociationJson.read(row(rows, i).toString());
            bounded(d);
            var p = d.proposal();
            var execution = executions.get(p.actual().id());
            var plan = programmes.get(key(p));
            if (decisions.put(d.id(), d) != null
                    || !actuals.containsKey(p.actual().id())
                    || execution == null
                    || execution.status() != GymExecution.Status.FINISHED
                    || execution.origin() != p.origin()
                    || plan == null)
                throw new IllegalArgumentException(
                        "Association history has a missing or inconsistent source.");
            GymWorkoutAssociation.validateRetainedEvidence(p, p.actual(), execution.origin(), plan);
            if (d.choice() == GymWorkoutAssociation.Choice.COUNT_TOWARD_SESSION
                    && GymWorkoutAssociation.proposals(
                                    p.actual(),
                                    p.origin(),
                                    plan,
                                    java.util.Collections.emptySet(),
                                    p.reviewedAt())
                            .stream()
                            .noneMatch(p::equals)) throw conflict();
            actualIds.add(p.actual().id());
        }
        var occupied = new HashSet<TargetKey>();
        rows = array(data, "current", 100000);
        for (int i = 0; i < rows.length(); i++) {
            var row = row(rows, i);
            fields(row, "actualId", "decisionId");
            String id = text(row, "actualId", 256);
            var d = decisions.get(text(row, "decisionId", 256));
            if (d == null || !id.equals(d.proposal().actual().id()) || !activeIds.add(id))
                throw conflict();
            var link = links.get(id);
            if (d.choice() == GymWorkoutAssociation.Choice.KEEP_EXTRA) {
                if (link != null)
                    throw new IllegalArgumentException(
                            "An extra-workout choice cannot claim a programme link.");
            } else {
                var p = d.proposal();
                if (!occupied.add(new TargetKey(key(p), p.prescription().id())) || link == null)
                    throw conflict();
                try {
                    if (!id.equals(FeatureJson.required(link, "id"))
                            || !key(p).equals(FeatureJson.text(link, "gymVersion"))
                            || !p.prescription().id().equals(FeatureJson.text(link, "gymSession"))
                            || !p.prescription()
                                    .date()
                                    .toString()
                                    .equals(FeatureJson.text(link, "plannedDate")))
                        throw conflict();
                } catch (JSONException invalid) {
                    throw FeatureJson.invalid(invalid);
                }
            }
        }
        if (!activeIds.equals(actualIds))
            throw new IllegalArgumentException(
                    "Association history needs its current explicit choice.");
    }

    static void restore(SQLiteDatabase db, JSONObject data) {
        var decisions = new java.util.HashMap<String, GymWorkoutAssociation.Decision>();
        var rows = array(data, "decisions", 100000);
        for (int i = 0; i < rows.length(); i++) {
            var d = GymAssociationJson.read(row(rows, i).toString());
            decisions.put(d.id(), d);
            var v = new ContentValues();
            v.put("id", d.id());
            v.put("actual_id", d.proposal().actual().id());
            v.put("decided_at", d.decidedAt().toString());
            v.put("payload", bounded(d));
            db.insertOrThrow("strength_association_decisions", null, v);
        }
        rows = array(data, "current", 100000);
        for (int i = 0; i < rows.length(); i++)
            setCurrent(db, decisions.get(text(row(rows, i), "decisionId", 256)));
    }
}
