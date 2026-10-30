package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.*;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

/** Typed authoring rows over the shared helper. All commands are IO-only and transactional. */
public final class SqliteStrengthProgrammingRepository extends SqliteStoreAccess
        implements StrengthProgrammingRepository {
    private final Clock clock;

    public SqliteStrengthProgrammingRepository(Context context) {
        this(context, Clock.systemDefaultZone());
    }

    public SqliteStrengthProgrammingRepository(Context context, Clock clock) {
        super(context);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Reference currentReference() {
        return snapshot(SqliteStrengthProgrammingRepository::current);
    }

    /** Normalised detail reads must share one snapshot with their indexed header. */
    private <T> T snapshot(java.util.function.Function<SQLiteDatabase, T> query) {
        var db = read();
        db.beginTransactionNonExclusive();
        try {
            var result = query.apply(db);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    static Reference current(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT programme_id,revision FROM strength_programme_current WHERE id=1",
                        null)) {
            if (c.moveToFirst()) {
                try (var legacy = db.rawQuery("SELECT 1 FROM gym_current WHERE id=1", null)) {
                    if (legacy.moveToFirst()) throw invalid("two current strength programmes");
                }
                return new Reference(Owner.AUTHORED, c.getString(0), c.getInt(1));
            }
        }
        try (var c =
                db.rawQuery(
                        "SELECT v.plan_id,v.version FROM gym_current c JOIN gym_plan_versions v ON"
                                + " c.version_id=v.version_id WHERE c.id=1",
                        null)) {
            return c.moveToFirst()
                    ? new Reference(Owner.LEGACY, c.getString(0), c.getInt(1))
                    : null;
        }
    }

    @Override
    public StrengthProgramme programme(String id, int revision) {
        return snapshot(db -> load(db, id, revision));
    }

    static StrengthProgramme load(SQLiteDatabase db, String id, int revision) {
        new Reference(Owner.AUTHORED, id, revision);
        try (var c =
                db.rawQuery(
                        "SELECT header,name,start_date,end_date,parent_revision FROM"
                            + " strength_programme_revisions WHERE programme_id=? AND revision=?",
                        args(id, revision))) {
            if (!c.moveToFirst()) return null;
            var json = GymJsonSupport.read(c.getString(0));
            var blocks = new JSONArray();
            try (var b =
                    db.rawQuery(
                            "SELECT position,block_id,header FROM strength_programme_blocks WHERE"
                                    + " programme_id=? AND revision=? ORDER BY position",
                            args(id, revision))) {
                while (b.moveToNext()) {
                    if (b.getInt(0) != blocks.length()) throw invalid("block order");
                    var block = GymJsonSupport.read(b.getString(2));
                    var workouts = new JSONArray();
                    if (!text(block, "id", 256).equals(b.getString(1)))
                        throw invalid("block identity");
                    try (var w =
                            db.rawQuery(
                                    "SELECT position,session_id,session_date,payload FROM"
                                        + " strength_programme_sessions WHERE programme_id=? AND"
                                        + " revision=? AND block_id=? ORDER BY position",
                                    new String[] {id, String.valueOf(revision), b.getString(1)})) {
                        while (w.moveToNext()) {
                            var workout = GymJsonSupport.read(w.getString(3));
                            if (w.getInt(0) != workouts.length()
                                    || !text(workout, "id", 256).equals(w.getString(1))
                                    || !text(workout, "date", 64).equals(w.getString(2)))
                                throw invalid("session index");
                            workouts.put(workout);
                        }
                    }
                    put(block, "workouts", workouts);
                    blocks.put(block);
                }
            }
            put(json, "blocks", blocks);
            var result = StrengthProgrammingJson.decode(json);
            if (!result.id().equals(id)
                    || result.revision() != revision
                    || !result.name().equals(c.getString(1))
                    || !result.start().toString().equals(c.getString(2))
                    || !result.end().toString().equals(c.getString(3))
                    || result.parentRevision() != c.getInt(4))
                throw invalid("programme summary index");
            return result;
        }
    }

    @Override
    public List<Summary> versions(int limit, int offset) {
        page(limit, offset);
        var result = new ArrayList<Summary>();
        try (var c =
                read().rawQuery(
                                "SELECT programme_id,revision,name,start_date,end_date FROM"
                                        + " strength_programme_revisions ORDER BY start_date"
                                        + " DESC,programme_id,revision DESC LIMIT ? OFFSET ?",
                                args(limit, offset))) {
            while (c.moveToNext())
                result.add(
                        new Summary(
                                c.getString(0),
                                c.getInt(1),
                                c.getString(2),
                                LocalDate.parse(c.getString(3)),
                                LocalDate.parse(c.getString(4))));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public StrengthProgrammeDraft draft(String id) {
        return snapshot(db -> draft(db, required(id, "draft")));
    }

    private static StrengthProgrammeDraft draft(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT header,revision,programme_id,base_revision,name,start_date FROM"
                                + " strength_programme_drafts WHERE id=?",
                        new String[] {id})) {
            if (!c.moveToFirst()) return null;
            var json = GymJsonSupport.read(c.getString(0));
            var blocks = new JSONArray();
            try (var b =
                    db.rawQuery(
                            "SELECT position,payload FROM strength_programme_draft_blocks WHERE"
                                    + " draft_id=? ORDER BY position",
                            new String[] {id})) {
                while (b.moveToNext()) {
                    if (b.getInt(0) != blocks.length()) throw invalid("draft block order");
                    blocks.put(GymJsonSupport.read(b.getString(1)));
                }
            }
            put(json, "blocks", blocks);
            var result = StrengthProgrammingJson.decodeDraft(json);
            if (!result.id().equals(id)
                    || result.revision() != c.getInt(1)
                    || !result.programmeId().equals(c.getString(2))
                    || result.baseRevision() != c.getInt(3)
                    || !result.name().equals(c.getString(4))
                    || !result.start().toString().equals(c.getString(5)))
                throw invalid("draft summary index");
            return result;
        }
    }

    @Override
    public List<DraftSummary> drafts(int limit, int offset) {
        page(limit, offset);
        var result = new ArrayList<DraftSummary>();
        try (var c =
                read().rawQuery(
                                "SELECT id,revision,name,start_date FROM strength_programme_drafts"
                                        + " ORDER BY id LIMIT ? OFFSET ?",
                                args(limit, offset))) {
            while (c.moveToNext())
                result.add(
                        new DraftSummary(
                                c.getString(0),
                                c.getInt(1),
                                c.getString(2),
                                LocalDate.parse(c.getString(3))));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void saveDraft(StrengthProgrammeDraft value, int expectedRevision) {
        Objects.requireNonNull(value);
        var db = write();
        db.beginTransaction();
        try {
            var previous = draft(db, value.id());
            if (value.equals(previous)) {
                db.setTransactionSuccessful();
                return;
            }
            if (expectedRevision < 0
                    || (previous == null
                            ? expectedRevision != 0
                            : previous.revision() != expectedRevision)
                    || value.revision() <= expectedRevision) throw conflict();
            if (legacyId(db, value.programmeId())
                    || value.baseRevision() > 0
                            && load(db, value.programmeId(), value.baseRevision()) == null)
                throw invalid("draft base programme");
            for (var b : value.blocks()) {
                for (var d : b.days()) {
                    for (var slot : d.slots()) {
                        if (ExerciseCatalog.definition(slot.exercise().id()) == null
                                && custom(db, slot.exercise().id()) == null) {
                            throw invalid("draft custom metadata");
                        }
                    }
                }
            }
            writeDraft(db, value);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void deleteDraft(String id, int expectedRevision) {
        var db = write();
        db.beginTransaction();
        try {
            var previous = draft(db, required(id, "draft"));
            if (previous != null) {
                if (previous.revision() != expectedRevision) throw conflict();
                db.delete("strength_programme_drafts", "id=?", new String[] {id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public CustomStrengthExercise customExercise(String id) {
        return custom(read(), required(id, "custom exercise"));
    }

    private static CustomStrengthExercise custom(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT payload FROM strength_custom_exercises WHERE id=?",
                        new String[] {id})) {
            if (!c.moveToFirst()) return null;
            var e = StrengthProgrammingJson.readCustom(c.getString(0));
            if (!e.exercise().id().equals(id)) throw invalid("custom exercise identity");
            return e;
        }
    }

    @Override
    public List<CustomStrengthExercise> customExercises() {
        return customs(read());
    }

    private static List<CustomStrengthExercise> customs(SQLiteDatabase db) {
        var result = new ArrayList<CustomStrengthExercise>();
        try (var c =
                db.rawQuery("SELECT id,payload FROM strength_custom_exercises ORDER BY id", null)) {
            while (c.moveToNext()) {
                var value = StrengthProgrammingJson.readCustom(c.getString(1));
                if (!value.exercise().id().equals(c.getString(0)))
                    throw invalid("custom exercise identity");
                result.add(value);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void saveCustom(CustomStrengthExercise value, CustomStrengthExercise expected) {
        Objects.requireNonNull(value);
        var db = write();
        db.beginTransaction();
        try {
            var previous = custom(db, value.exercise().id());
            if (value.equals(previous)) {
                db.setTransactionSuccessful();
                return;
            }
            if (!Objects.equals(previous, expected)) throw conflict();
            if (previous != null
                    && (previous.exercise().measurement() != value.exercise().measurement()
                            || previous.exercise().mode() != value.exercise().mode()))
                throw new IllegalArgumentException(
                        "Create a new exercise for a different measurement task.");
            try (var c =
                    db.rawQuery(
                            "SELECT payload FROM strength_exercises WHERE id=?",
                            new String[] {value.exercise().id()})) {
                if (c.moveToFirst()) {
                    var existing = CoachingJson.exercise(FeatureJson.object(c.getString(0)));
                    if (!existing.equals(previous == null ? value.exercise() : previous.exercise()))
                        throw conflict();
                }
            }
            writeCustom(db, value);
            db.setTransactionSuccessful();
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        } finally {
            db.endTransaction();
        }
    }

    /** Existing logging/detail editors must not leave typed metadata and its mirror divergent. */
    static boolean updateLoggingDefinition(SQLiteDatabase db, StrengthExercise exercise) {
        var previous = custom(db, exercise.id());
        if (previous == null) return false;
        if (previous.exercise().measurement() != exercise.measurement()
                || previous.exercise().mode() != exercise.mode())
            throw new IllegalArgumentException(
                    "Create a new exercise for a different measurement task.");
        writeCustom(
                db,
                new CustomStrengthExercise(
                        exercise,
                        previous.bodyArea(),
                        previous.role(),
                        previous.equipment(),
                        previous.notes()));
        return true;
    }

    @Override
    public Decision activate(
            StrengthProgramme proposed,
            Reference expected,
            String decisionId,
            List<StrengthProgrammeEditor.Change> substitutions) {
        return activateInternal(proposed, expected, decisionId, substitutions, null);
    }

    @Override
    public Decision activateDraft(
            StrengthProgramme proposed,
            Reference expected,
            String decisionId,
            StrengthProgrammeDraft reviewed) {
        Objects.requireNonNull(reviewed, "reviewed draft");
        return activateInternal(proposed, expected, decisionId, Collections.emptyList(), reviewed);
    }

    private Decision activateInternal(
            StrengthProgramme proposed,
            Reference expected,
            String decisionId,
            List<StrengthProgrammeEditor.Change> substitutions,
            StrengthProgrammeDraft reviewed) {
        Objects.requireNonNull(proposed);
        Objects.requireNonNull(substitutions);
        required(decisionId, "decision");
        var db = write();
        db.beginTransaction();
        try {
            var resultRef = new Reference(Owner.AUTHORED, proposed.id(), proposed.revision());
            var retry = decision(db, decisionId);
            if (retry != null) {
                if (!Objects.equals(retry.before(), expected)
                        || !resultRef.equals(retry.after())
                        || !resultRef.equals(current(db))
                        || !proposed.equals(load(db, proposed.id(), proposed.revision()))
                        || !retry.substitutions().equals(substitutions)) throw conflict();
                db.setTransactionSuccessful();
                return retry;
            }
            if (reviewed != null) {
                if (!reviewed.equals(draft(db, reviewed.id()))) throw conflict();
                var custom = new HashMap<String, CustomStrengthExercise>();
                for (var exercise : customs(db)) custom.put(exercise.exercise().id(), exercise);
                var excluded = new HashSet<String>();
                for (var e : SqliteExerciseExclusionRepository.rowsForChoices(db))
                    excluded.add(e.exerciseId());
                try (var c =
                        db.rawQuery(
                                "SELECT exercise_id FROM strength_user_state WHERE archived=1",
                                null)) {
                    while (c.moveToNext()) excluded.add(c.getString(0));
                }
                if (!proposed.equals(reviewed.preview(proposed.createdAt(), excluded, custom)))
                    throw invalid("reviewed draft and programme preview differ");
            }
            if (!Objects.equals(current(db), expected)) throw conflict();
            if (legacyId(db, proposed.id())
                    || proposed.createdAt().isAfter(clock.instant())
                    || proposed.effectiveFrom().isBefore(LocalDate.now(clock))) throw conflict();
            Set<String> protectedIds = Collections.emptySet();
            StrengthProgramme before = null;
            if (expected != null
                    && expected.owner() == Owner.AUTHORED
                    && expected.id().equals(proposed.id())) {
                before = load(db, expected.id(), expected.revision());
                protectedIds = completed(db, expected.id());
                var retainedIds = new HashSet<String>();
                for (var w : before.workouts()) retainedIds.add(w.id());
                protectedIds.retainAll(retainedIds);
                new StrengthProgrammeEditor.Proposal(before, proposed, protectedIds, substitutions);
            } else if (proposed.revision() != 1 || !substitutions.isEmpty()) throw conflict();
            if (load(db, proposed.id(), proposed.revision()) != null) throw conflict();
            eligible(db, proposed, before, protectedIds);
            var protectedList = new ArrayList<>(protectedIds);
            Collections.sort(protectedList);
            var accepted =
                    new Decision(
                            decisionId,
                            expected,
                            resultRef,
                            clock.instant(),
                            proposed.reason(),
                            protectedList,
                            substitutions);
            insert(db, proposed);
            writeDecision(db, accepted);
            db.delete("gym_current", null, null);
            var pointer = new ContentValues();
            pointer.put("id", 1);
            pointer.put("programme_id", proposed.id());
            pointer.put("revision", proposed.revision());
            db.insertWithOnConflict(
                    "strength_programme_current", null, pointer, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
            return accepted;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Decision stop(Reference expected, String decisionId) {
        Objects.requireNonNull(expected);
        required(decisionId, "decision");
        var db = write();
        db.beginTransaction();
        try {
            var retry = decision(db, decisionId);
            if (retry != null) {
                if (!Objects.equals(retry.before(), expected)
                        || retry.after() != null
                        || current(db) != null) throw conflict();
                db.setTransactionSuccessful();
                return retry;
            }
            if (!expected.equals(current(db))) throw conflict();
            var result =
                    new Decision(
                            decisionId,
                            expected,
                            null,
                            clock.instant(),
                            "Athlete stopped programming; all history is retained.",
                            Collections.emptyList(),
                            Collections.emptyList());
            writeDecision(db, result);
            db.delete("strength_programme_current", null, null);
            db.delete("gym_current", null, null);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    private static void eligible(
            SQLiteDatabase db,
            StrengthProgramme p,
            StrengthProgramme previous,
            Set<String> protectedIds) {
        var old = new HashMap<String, StrengthProgramme.Workout>();
        if (previous != null) for (var w : previous.workouts()) old.put(w.id(), w);
        var excluded = new HashSet<String>();
        for (var e : SqliteExerciseExclusionRepository.rowsForChoices(db))
            excluded.add(e.exerciseId());
        for (var w : p.workouts()) {
            if (w.date().isBefore(p.effectiveFrom())
                    || protectedIds.contains(w.id())
                    || w.equals(old.get(w.id()))
                            && previous.settings().equipment().equals(p.settings().equipment()))
                continue;
            if (!p.settings().days().contains(w.date().getDayOfWeek()))
                throw invalid("future available day");
            for (var slot : w.definition().slots()) {
                if (slot.target() == null || excluded.contains(slot.exercise().id()))
                    throw invalid("missing target or excluded exercise");
                var m = ExerciseMetadataCatalog.find(slot.exercise().id());
                if (m != null) {
                    if (!ExerciseCatalog.definition(slot.exercise().id())
                                    .exercise()
                                    .equals(slot.exercise())
                            || !m.equipment().availableWith(p.settings().equipment()))
                        throw invalid("canonical exercise setup");
                } else {
                    var custom = custom(db, slot.exercise().id());
                    if (custom == null
                            || !custom.exercise().equals(slot.exercise())
                            || !custom.equipment().availableWith(p.settings().equipment()))
                        throw invalid("custom exercise setup");
                }
            }
            if (w.expectedMinutes() != StrengthWorkoutBudget.workoutMinutes(w.definition().slots()))
                throw invalid("review the changed workout time estimate");
        }
    }

    private static Set<String> completed(SQLiteDatabase db, String programmeId) {
        var ids = new HashSet<String>();
        try (var c =
                db.rawQuery(
                        "SELECT DISTINCT t.session_id FROM strength_programme_revisions p JOIN"
                            + " strength_association_targets t ON t.programme_key=p.programme_id ||"
                            + " ':v' || p.revision JOIN strength_sessions s ON s.id=t.actual_id"
                            + " WHERE p.programme_id=?",
                        new String[] {programmeId})) {
            while (c.moveToNext()) ids.add(c.getString(0));
        }
        return ids;
    }

    private static boolean legacyId(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT 1 FROM gym_plan_versions WHERE plan_id=? LIMIT 1",
                        new String[] {id})) {
            return c.moveToFirst();
        }
    }

    @Override
    public List<Decision> decisions() {
        return decisions(read());
    }

    private static List<Decision> decisions(SQLiteDatabase db) {
        var result = new ArrayList<Decision>();
        try (var c =
                db.rawQuery(
                        "SELECT payload FROM strength_programme_decisions ORDER BY position",
                        null)) {
            while (c.moveToNext())
                result.add(
                        StrengthProgrammingArchive.decision(GymJsonSupport.read(c.getString(0))));
        }
        return Collections.unmodifiableList(result);
    }

    private static Decision decision(SQLiteDatabase db, String id) {
        try (var c =
                db.rawQuery(
                        "SELECT payload FROM strength_programme_decisions WHERE id=?",
                        new String[] {id})) {
            return c.moveToFirst()
                    ? StrengthProgrammingArchive.decision(GymJsonSupport.read(c.getString(0)))
                    : null;
        }
    }

    private static void writeDecision(SQLiteDatabase db, Decision d) {
        long position;
        try (var c =
                db.rawQuery(
                        "SELECT COALESCE(MAX(position),0)+1 FROM strength_programme_decisions",
                        null)) {
            c.moveToFirst();
            position = c.getLong(0);
        }
        var values = new ContentValues();
        values.put("id", d.id());
        values.put("position", position);
        values.put("payload", bounded(StrengthProgrammingArchive.decision(d), 524288));
        db.insertOrThrow("strength_programme_decisions", null, values);
    }

    static void insert(SQLiteDatabase db, StrengthProgramme value) {
        var json = StrengthProgrammingJson.encode(value);
        var blocks = array(json, "blocks", 12);
        json.remove("blocks");
        var v = new ContentValues();
        v.put("programme_id", value.id());
        v.put("revision", value.revision());
        v.put("parent_revision", value.parentRevision());
        v.put("name", value.name());
        v.put("start_date", value.start().toString());
        v.put("end_date", value.end().toString());
        v.put("header", bounded(json, 262144));
        db.insertOrThrow("strength_programme_revisions", null, v);
        for (int i = 0; i < blocks.length(); i++) {
            var block = row(blocks, i);
            var workouts = array(block, "workouts", 156);
            block.remove("workouts");
            v = new ContentValues();
            v.put("programme_id", value.id());
            v.put("revision", value.revision());
            v.put("position", i);
            v.put("block_id", text(block, "id", 256));
            v.put("header", bounded(block, 262144));
            db.insertOrThrow("strength_programme_blocks", null, v);
            for (int n = 0; n < workouts.length(); n++) {
                var workout = row(workouts, n);
                v = new ContentValues();
                v.put("programme_id", value.id());
                v.put("revision", value.revision());
                v.put("block_id", text(block, "id", 256));
                v.put("position", n);
                v.put("session_id", text(workout, "id", 256));
                v.put("session_date", text(workout, "date", 64));
                v.put("payload", bounded(workout, 262144));
                db.insertOrThrow("strength_programme_sessions", null, v);
            }
        }
    }

    private static void writeDraft(SQLiteDatabase db, StrengthProgrammeDraft value) {
        var json = StrengthProgrammingJson.encodeDraft(value);
        var blocks = array(json, "blocks", 12);
        json.remove("blocks");
        var v = new ContentValues();
        v.put("id", value.id());
        v.put("revision", value.revision());
        v.put("programme_id", value.programmeId());
        v.put("base_revision", value.baseRevision());
        v.put("name", value.name());
        v.put("start_date", value.start().toString());
        v.put("header", bounded(json, 262144));
        if (db.update("strength_programme_drafts", v, "id=?", new String[] {value.id()}) == 0)
            db.insertOrThrow("strength_programme_drafts", null, v);
        db.delete("strength_programme_draft_blocks", "draft_id=?", new String[] {value.id()});
        for (int i = 0; i < blocks.length(); i++) {
            v = new ContentValues();
            v.put("draft_id", value.id());
            v.put("position", i);
            v.put("payload", bounded(row(blocks, i), 262144));
            db.insertOrThrow("strength_programme_draft_blocks", null, v);
        }
    }

    private static void writeCustom(SQLiteDatabase db, CustomStrengthExercise value) {
        var v = new ContentValues();
        v.put("id", value.exercise().id());
        v.put("payload", bounded(StrengthProgrammingJson.encodeCustom(value), 65536));
        db.insertWithOnConflict(
                "strength_custom_exercises", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        v = new ContentValues();
        v.put("id", value.exercise().id());
        v.put("payload", CoachingJson.exercise(value.exercise()).toString());
        db.insertWithOnConflict("strength_exercises", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    static JSONObject exportData(SQLiteDatabase db) {
        var programmes = new JSONArray();
        var drafts = new JSONArray();
        var custom = new JSONArray();
        var decisions = new JSONArray();
        try (var c =
                db.rawQuery(
                        "SELECT programme_id,revision FROM strength_programme_revisions ORDER BY"
                                + " programme_id,revision",
                        null)) {
            while (c.moveToNext())
                programmes.put(
                        StrengthProgrammingJson.encode(load(db, c.getString(0), c.getInt(1))));
        }
        try (var c = db.rawQuery("SELECT id FROM strength_programme_drafts ORDER BY id", null)) {
            while (c.moveToNext())
                drafts.put(StrengthProgrammingJson.encodeDraft(draft(db, c.getString(0))));
        }
        for (var e : customs(db)) custom.put(StrengthProgrammingJson.encodeCustom(e));
        for (var d : decisions(db)) decisions.put(StrengthProgrammingArchive.decision(d));
        var current = current(db);
        return object(
                "version",
                1,
                "programmes",
                programmes,
                "drafts",
                drafts,
                "customExercises",
                custom,
                "decisions",
                decisions,
                "current",
                current == null || current.owner() == Owner.LEGACY
                        ? null
                        : StrengthProgrammingArchive.reference(current));
    }

    static void clear(SQLiteDatabase db) {
        for (String table :
                new String[] {
                    "strength_programme_current",
                    "strength_programme_decisions",
                    "strength_programme_sessions",
                    "strength_programme_blocks",
                    "strength_programme_revisions",
                    "strength_programme_draft_blocks",
                    "strength_programme_drafts",
                    "strength_custom_exercises"
                }) db.delete(table, null, null);
    }

    static void restore(SQLiteDatabase db, StrengthProgrammingArchive.Snapshot graph) {
        for (var p : graph.programmes().values()) {
            insert(db, p);
        }
        for (var custom : graph.custom()) {
            writeCustom(db, custom);
        }
        for (var draft : graph.drafts()) {
            writeDraft(db, draft);
        }
        for (var d : graph.decisions()) {
            writeDecision(db, d);
        }
        if (graph.current() != null) {
            var v = new ContentValues();
            v.put("id", 1);
            v.put("programme_id", graph.current().id());
            v.put("revision", graph.current().revision());
            db.insertOrThrow("strength_programme_current", null, v);
        }
    }

    private static String[] args(Object... values) {
        return Arrays.stream(values).map(Object::toString).toArray(String[]::new);
    }

    private static void put(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw FeatureJson.invalid(e);
        }
    }

    static String bounded(JSONObject json, int maximum) {
        var s = GymJsonSupport.write(json);
        if (s.getBytes(StandardCharsets.UTF_8).length > maximum)
            throw invalid("authoring row size");
        return s;
    }

    private static void page(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0)
            throw new IllegalArgumentException("Choose a bounded programme page.");
    }

    static void validateRows(StrengthProgramme p) {
        var root = StrengthProgrammingJson.encode(p);
        var blocks = array(root, "blocks", 12);
        root.remove("blocks");
        bounded(root, 262144);
        for (int i = 0; i < blocks.length(); i++) {
            var block = row(blocks, i);
            var workouts = array(block, "workouts", 156);
            block.remove("workouts");
            bounded(block, 262144);
            for (int n = 0; n < workouts.length(); n++) bounded(row(workouts, n), 262144);
        }
    }

    static void validateRows(StrengthProgrammeDraft d) {
        var root = StrengthProgrammingJson.encodeDraft(d);
        var blocks = array(root, "blocks", 12);
        root.remove("blocks");
        bounded(root, 262144);
        for (int i = 0; i < blocks.length(); i++) bounded(row(blocks, i), 262144);
    }

    private static IllegalArgumentException conflict() {
        return new IllegalArgumentException(
                "The programme or draft changed. Reload and review before saving.");
    }
}
