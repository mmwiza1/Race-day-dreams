package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.time.*;
import java.util.*;

/** Normalised concrete drafts share the existing database/restore/activation transaction. */
public final class SqliteConcreteStrengthProgrammeDraftRepository extends SqliteStoreAccess
        implements ConcreteStrengthProgrammeDraftRepository {
    private final Context context;
    private final Clock clock;

    public SqliteConcreteStrengthProgrammeDraftRepository(Context context) {
        this(context, Clock.systemDefaultZone());
    }

    public SqliteConcreteStrengthProgrammeDraftRepository(Context context, Clock clock) {
        super(context);
        this.context = context.getApplicationContext();
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ConcreteStrengthProgrammeDraft get(String id) {
        required(id, "concrete draft");
        var db = read();
        db.beginTransactionNonExclusive();
        try {
            var result = get(db, id);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    static ConcreteStrengthProgrammeDraft get(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT"
                            + " header,revision,programme_id,programme_revision,name,start_date,end_date"
                            + " FROM strength_concrete_drafts WHERE id=?",
                        new String[] {id})) {
            if (!c.moveToFirst()) return null;
            var json = GymJsonSupport.read(c.getString(0));
            var programme = child(json, "programme");
            var blocks = new JSONArray();
            try (var b =
                    db.rawQuery(
                            "SELECT position,block_id,header FROM strength_concrete_draft_blocks"
                                    + " WHERE draft_id=? ORDER BY position",
                            new String[] {id})) {
                while (b.moveToNext()) {
                    if (!Integer.toString(blocks.length()).equals(b.getString(0)))
                        throw invalid("concrete block order");
                    var block = GymJsonSupport.read(b.getString(2));
                    var workouts = new JSONArray();
                    if (!text(block, "id", 256).equals(b.getString(1)))
                        throw invalid("concrete block identity");
                    try (var w =
                            db.rawQuery(
                                    "SELECT position,workout_id,workout_date,payload FROM"
                                        + " strength_concrete_draft_workouts WHERE draft_id=? AND"
                                        + " block_id=? ORDER BY position",
                                    new String[] {id, b.getString(1)})) {
                        while (w.moveToNext()) {
                            var workout = GymJsonSupport.read(w.getString(3));
                            if (!Integer.toString(workouts.length()).equals(w.getString(0))
                                    || !text(workout, "id", 256).equals(w.getString(1))
                                    || !text(workout, "date", 64).equals(w.getString(2)))
                                throw invalid("concrete workout index");
                            workouts.put(workout);
                        }
                    }
                    put(block, "workouts", workouts);
                    blocks.put(block);
                }
            }
            put(programme, "blocks", blocks);
            var result = ConcreteStrengthProgrammeJson.decode(json);
            var p = result.programme();
            if (!result.id().equals(id)
                    || !Integer.toString(result.revision()).equals(c.getString(1))
                    || !p.id().equals(c.getString(2))
                    || !Integer.toString(p.revision()).equals(c.getString(3))
                    || !p.name().equals(c.getString(4))
                    || !p.start().toString().equals(c.getString(5))
                    || !p.end().toString().equals(c.getString(6)))
                throw invalid("concrete draft index");
            return result;
        }
    }

    @Override
    public List<Summary> list(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0 || offset > 1_000_000)
            throw new IllegalArgumentException("Invalid draft page.");
        var result = new ArrayList<Summary>();
        try (var c =
                read().rawQuery(
                                "SELECT"
                                    + " d.id,d.revision,d.programme_id,d.programme_revision,d.name,d.start_date,d.end_date,EXISTS(SELECT"
                                    + " 1 FROM strength_programme_revisions p WHERE"
                                    + " p.programme_id=d.programme_id AND"
                                    + " p.revision=d.programme_revision) FROM"
                                    + " strength_concrete_drafts d ORDER BY d.start_date DESC,d.id"
                                    + " LIMIT ? OFFSET ?",
                                new String[] {String.valueOf(limit), String.valueOf(offset)})) {
            while (c.moveToNext())
                result.add(
                        new Summary(
                                c.getString(0),
                                c.getInt(1),
                                c.getString(2),
                                c.getInt(3),
                                c.getString(4),
                                LocalDate.parse(c.getString(5)),
                                LocalDate.parse(c.getString(6)),
                                c.getInt(7) == 1));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void save(ConcreteStrengthProgrammeDraft value, int expectedRevision) {
        Objects.requireNonNull(value);
        validateRows(value);
        var db = write();
        db.beginTransaction();
        try {
            var previous = get(db, value.id());
            if (value.equals(previous)) {
                db.setTransactionSuccessful();
                return;
            }
            if (expectedRevision < 0
                    || expectedRevision != (previous == null ? 0 : previous.revision())
                    || value.revision() != Math.addExact(expectedRevision, 1)) throw changed();
            if (previous != null
                    && (!previous.programme().id().equals(value.programme().id())
                            || previous.programme().revision() != value.programme().revision()
                            || !Objects.equals(previous.before(), value.before()))) throw changed();
            if (SqliteStrengthProgrammingRepository.legacyId(db, value.programme().id())
                    || SqliteStrengthProgrammingRepository.load(
                                    db, value.programme().id(), value.programme().revision())
                            != null)
                throw new IllegalArgumentException(
                        "Preserved programmes cannot be edited as unaccepted drafts.");
            checkReferences(db, value);
            if (previous == null)
                try (var count =
                        db.rawQuery("SELECT COUNT(*) FROM strength_concrete_drafts", null)) {
                    count.moveToFirst();
                    if (count.getLong(0) >= 1000)
                        throw new IllegalArgumentException(
                                "Manage saved drafts before adding another.");
                }
            writeDraft(db, value);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void delete(String id, int expectedRevision) {
        required(id, "concrete draft");
        var db = write();
        db.beginTransaction();
        try {
            var old = get(db, id);
            if (old == null || old.revision() != expectedRevision) throw changed();
            db.delete("strength_concrete_drafts", "id=?", new String[] {id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Decision activate(ConcreteStrengthProgrammeDraft reviewed, String decisionId) {
        Objects.requireNonNull(reviewed);
        required(decisionId, "decision");
        var db = write();
        db.beginTransaction();
        try {
            // A lost acknowledgement remains retryable even after the saved draft was removed.
            if (SqliteStrengthProgrammingRepository.decision(db, decisionId) == null
                    && !reviewed.equals(get(db, reviewed.id()))) throw changed();
            var result =
                    new SqliteStrengthProgrammingRepository(context, clock)
                            .activate(
                                    reviewed.programme(),
                                    reviewed.before(),
                                    decisionId,
                                    reviewed.substitutions());
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private static IllegalArgumentException changed() {
        return new IllegalArgumentException(
                "This draft or programme changed. Reload and review before saving or activating.");
    }

    private static void checkReferences(SQLiteDatabase db, ConcreteStrengthProgrammeDraft draft) {
        var ref = draft.before();
        if (ref != null) {
            boolean found;
            if (ref.owner() == Owner.AUTHORED)
                found =
                        SqliteStrengthProgrammingRepository.load(db, ref.id(), ref.revision())
                                != null;
            else
                try (var c =
                        db.rawQuery(
                                "SELECT 1 FROM gym_plan_versions WHERE plan_id=? AND version=?",
                                new String[] {ref.id(), String.valueOf(ref.revision())})) {
                    found = c.moveToFirst();
                }
            if (!found) throw invalid("concrete draft base reference");
        }
        if (draft.programme().parentRevision() > 0)
            draft.validateAgainst(
                    SqliteStrengthProgrammingRepository.load(db, ref.id(), ref.revision()));
        for (var w : draft.programme().workouts())
            for (var slot : w.definition().slots())
                if (ExerciseCatalog.definition(slot.exercise().id()) == null
                        && SqliteStrengthProgrammingRepository.custom(db, slot.exercise().id())
                                == null) throw invalid("concrete draft custom metadata");
    }

    static void validateRows(ConcreteStrengthProgrammeDraft draft) {
        SqliteStrengthProgrammingRepository.validateRows(draft.programme());
        var json = ConcreteStrengthProgrammeJson.encode(draft);
        child(json, "programme").remove("blocks");
        SqliteStrengthProgrammingRepository.bounded(json, 262144);
    }

    static void writeDraft(SQLiteDatabase db, ConcreteStrengthProgrammeDraft draft) {
        var json = ConcreteStrengthProgrammeJson.encode(draft);
        var p = child(json, "programme");
        var blocks = array(p, "blocks", 12);
        p.remove("blocks");
        var value = draft.programme();
        var v = new ContentValues();
        v.put("id", draft.id());
        v.put("revision", draft.revision());
        v.put("programme_id", value.id());
        v.put("programme_revision", value.revision());
        v.put("name", value.name());
        v.put("start_date", value.start().toString());
        v.put("end_date", value.end().toString());
        v.put("header", SqliteStrengthProgrammingRepository.bounded(json, 262144));
        if (db.update("strength_concrete_drafts", v, "id=?", new String[] {draft.id()}) == 0)
            db.insertOrThrow("strength_concrete_drafts", null, v);
        db.delete("strength_concrete_draft_blocks", "draft_id=?", new String[] {draft.id()});
        for (int i = 0; i < blocks.length(); i++) {
            var block = row(blocks, i);
            var workouts = array(block, "workouts", 156);
            block.remove("workouts");
            String blockId = text(block, "id", 256);
            v = new ContentValues();
            v.put("draft_id", draft.id());
            v.put("block_id", blockId);
            v.put("position", i);
            v.put("header", SqliteStrengthProgrammingRepository.bounded(block, 262144));
            db.insertOrThrow("strength_concrete_draft_blocks", null, v);
            for (int n = 0; n < workouts.length(); n++) {
                var w = row(workouts, n);
                v = new ContentValues();
                v.put("draft_id", draft.id());
                v.put("block_id", blockId);
                v.put("position", n);
                v.put("workout_id", text(w, "id", 256));
                v.put("workout_date", text(w, "date", 64));
                v.put("payload", SqliteStrengthProgrammingRepository.bounded(w, 262144));
                db.insertOrThrow("strength_concrete_draft_workouts", null, v);
            }
        }
    }

    static JSONArray exportData(SQLiteDatabase db) {
        var rows = new JSONArray();
        try (var c = db.rawQuery("SELECT id FROM strength_concrete_drafts ORDER BY id", null)) {
            while (c.moveToNext()) {
                if (rows.length() >= 1000) throw invalid("too many concrete drafts");
                rows.put(ConcreteStrengthProgrammeJson.encode(get(db, c.getString(0))));
            }
        }
        return rows;
    }

    static void clear(SQLiteDatabase db) {
        db.delete("strength_concrete_drafts", null, null);
    }

    private static void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException invalid) {
            throw FeatureJson.invalid(invalid);
        }
    }
}
