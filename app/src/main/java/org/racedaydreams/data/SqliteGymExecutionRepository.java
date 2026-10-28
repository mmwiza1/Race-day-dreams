package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionRepository;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.GymWorkoutSourceValidator;
import org.racedaydreams.domain.strength.StrengthExercise;
import org.racedaydreams.domain.strength.StrengthSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Typed gym staging on the shared SQLite connection. Set/event rows are append-only; a list query
 * never decodes them. Completed training evidence and its original association commit together.
 */
public final class SqliteGymExecutionRepository extends SqliteStoreAccess
        implements GymExecutionRepository {
    public static final int BACKUP_VERSION = 1;
    public static final int MAX_ROW_BYTES = 256 * 1024;
    private static final String UNFINISHED = "state IN ('ACTIVE','PAUSED','RECOVERY_REQUIRED')";

    private record WorkoutKey(String id, int revision) {}

    private record WorkoutRow(
            GymWorkoutDefinition definition,
            GymExecution.SourceReference source,
            JSONObject sourceSnapshot) {}

    private record Parts(String header, List<String> sets, List<String> events) {}

    private record BlockKey(String slotId, StrengthExercise exercise) {}

    public SqliteGymExecutionRepository(Context context) {
        super(context);
    }

    @Override
    public Stored start(GymExecution initial) {
        Objects.requireNonNull(initial, "initial gym execution");
        if (initial.status() != GymExecution.Status.ACTIVE
                || !initial.actualSets().isEmpty()
                || initial.events().size() != 1
                || initial.recordedElapsedMillis() != 0
                || initial.activeMillis() != 0) {
            throw new IllegalArgumentException("Start with an empty, active gym execution.");
        }
        if (initial.source() != null
                && initial.source().kind() == GymExecution.SourceKind.ACTUAL_SESSION
                && initial.id().equals(initial.source().id()))
            throw new IllegalArgumentException("A gym execution cannot repeat itself.");
        Parts prepared = parts(initial);
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            try (Cursor actual =
                    db.rawQuery(
                            "SELECT 1 FROM strength_sessions WHERE id=?",
                            new String[] {initial.id()})) {
                if (actual.moveToFirst())
                    throw new IllegalArgumentException(
                            "This gym identity already belongs to a completed strength workout.");
            }
            Stored existing = load(db, initial.id());
            if (existing != null) {
                if (!existing.execution().equals(initial)) throw conflict();
                db.setTransactionSuccessful();
                return existing;
            }
            if (hasUnfinished(db))
                throw new IllegalArgumentException(
                        "Finish or recover the unfinished gym workout before starting another.");
            ensureWorkout(db, initial.prescription(), initial.source());
            insertExecution(db, initial, 1, prepared);
            db.setTransactionSuccessful();
            return new Stored(initial, 1);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Stored load(String id) {
        SQLiteDatabase db = read();
        db.beginTransactionNonExclusive();
        try {
            Stored stored = load(db, validId(id));
            db.setTransactionSuccessful();
            return stored;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public List<Summary> unfinished() {
        var out = new ArrayList<Summary>();
        try (Cursor c =
                read().rawQuery(
                                "SELECT"
                                    + " id,origin,state,title,started_at,recorded_elapsed,active_millis,set_count,revision"
                                    + " FROM strength_executions WHERE "
                                        + UNFINISHED
                                        + " ORDER BY started_at DESC,id",
                                null)) {
            while (c.moveToNext())
                out.add(
                        new Summary(
                                c.getString(0),
                                GymExecution.Origin.valueOf(c.getString(1)),
                                GymExecution.Status.valueOf(c.getString(2)),
                                c.getString(3),
                                Instant.parse(c.getString(4)),
                                c.getLong(5),
                                c.getLong(6),
                                c.getInt(7),
                                c.getLong(8)));
        }
        return Immutable.copyList(out);
    }

    public static boolean hasUnfinished(SQLiteDatabase db) {
        try (Cursor c =
                db.rawQuery(
                        "SELECT 1 FROM strength_executions WHERE " + UNFINISHED + " LIMIT 1",
                        null)) {
            return c.moveToFirst();
        }
    }

    @Override
    public Stored save(GymExecution snapshot, long expectedRevision) {
        if (snapshot.status() == GymExecution.Status.FINISHED)
            throw new IllegalArgumentException("Final gym evidence must use atomic finalisation.");
        return persist(snapshot, expectedRevision, false);
    }

    @Override
    public Stored finish(GymExecution finished, long expectedRevision) {
        if (finished.status() != GymExecution.Status.FINISHED)
            throw new IllegalArgumentException(
                    "Finish the gym execution before finalising its actual evidence.");
        return persist(finished, expectedRevision, true);
    }

    private Stored persist(GymExecution next, long expectedRevision, boolean finish) {
        Objects.requireNonNull(next, "gym execution");
        if (expectedRevision < 1 || expectedRevision == Long.MAX_VALUE) throw conflict();
        Parts prepared = parts(next);
        String actualPayload =
                finish ? bounded(CoachingJson.strength(next.completedSession())) : null;
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            Stored previous = load(db, next.id());
            if (previous == null)
                throw new IllegalArgumentException("The staged gym workout is missing.");
            if (previous.execution().equals(next)) {
                if (finish) verifyCompleted(db, next);
                db.setTransactionSuccessful();
                return previous;
            }
            if (previous.revision() != expectedRevision) throw conflict();
            GymExecution before = previous.execution();
            if (before.status() == GymExecution.Status.FINISHED
                    || before.status() == GymExecution.Status.DISCARDED
                    || !before.origin().equals(next.origin())
                    || !Objects.equals(before.prescription(), next.prescription())
                    || !Objects.equals(before.source(), next.source())
                    || !before.startedAt().equals(next.startedAt())
                    || !before.offset().equals(next.offset())
                    || next.recordedElapsedMillis() < before.recordedElapsedMillis()
                    || next.activeMillis() < before.activeMillis()
                    || !prefix(before.actualSets(), next.actualSets())
                    || !prefix(before.events(), next.events())) throw conflict();
            ensureWorkout(db, next.prescription(), next.source());
            if (finish) {
                if (next.startedAt().isAfter(Instant.now()))
                    throw new IllegalArgumentException(
                            "A completed strength session cannot be in the future.");
                saveActualAndAssociation(db, next, actualPayload);
            }
            ContentValues summary = summary(next, expectedRevision + 1, prepared.header());
            int changed =
                    db.update(
                            "strength_executions",
                            summary,
                            "id=? AND revision=?",
                            new String[] {next.id(), Long.toString(expectedRevision)});
            if (changed != 1) throw conflict();
            appendRows(db, next, prepared, before.actualSets().size(), before.events().size());
            db.setTransactionSuccessful();
            return new Stored(next, expectedRevision + 1);
        } finally {
            db.endTransaction();
        }
    }

    private static boolean prefix(List<?> before, List<?> after) {
        return after.size() >= before.size() && before.equals(after.subList(0, before.size()));
    }

    @Override
    public void saveWorkout(GymWorkoutDefinition workout, GymExecution.SourceReference source) {
        Objects.requireNonNull(workout, "workout");
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            ensureWorkout(db, workout, source);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public GymWorkoutDefinition workout(String id, int revision) {
        WorkoutRow row = workoutRow(read(), validId(id), revision);
        return row == null ? null : row.definition();
    }

    /** Bounded favourite definitions for selection; execution/set/event histories stay unloaded. */
    public List<GymWorkoutDefinition> favouriteWorkouts(int limit) {
        return favouriteWorkouts(limit, 0);
    }

    public List<GymWorkoutDefinition> favouriteWorkouts(int limit, int offset) {
        if (limit < 1 || limit > 50 || offset < 0 || offset > 10000)
            throw new IllegalArgumentException("Choose a bounded saved-workout page.");
        var out = new ArrayList<GymWorkoutDefinition>();
        try (Cursor c =
                read().rawQuery(
                                "SELECT w.payload FROM strength_workout_preferences p JOIN"
                                    + " strength_workout_versions w ON w.workout_id=p.workout_id"
                                    + " AND w.revision=p.current_revision WHERE p.favourite=1 ORDER"
                                    + " BY p.workout_id LIMIT ? OFFSET ?",
                                new String[] {Integer.toString(limit), Integer.toString(offset)})) {
            while (c.moveToNext())
                out.add(workoutRow(GymJsonSupport.read(c.getString(0))).definition());
        }
        return Immutable.copyList(out);
    }

    /**
     * Explicit repeat selection needs a few actuals, never the athlete's entire workout history.
     */
    public List<StrengthSession> recentActual(int limit) {
        if (limit < 1 || limit > 20)
            throw new IllegalArgumentException("Choose 1–20 recent actual workouts.");
        var out = new ArrayList<StrengthSession>();
        String epoch = "CAST(ROUND((julianday(started_at)-2440587.5)*86400000) AS INTEGER)";
        try (Cursor c =
                read().rawQuery(
                                "SELECT payload FROM strength_sessions WHERE "
                                        + epoch
                                        + "<=? ORDER BY "
                                        + epoch
                                        + " DESC,id LIMIT ?",
                                new String[] {
                                    Long.toString(Instant.now().toEpochMilli()),
                                    Integer.toString(limit)
                                })) {
            while (c.moveToNext())
                out.add(CoachingJson.strength(FeatureJson.object(c.getString(0))));
        } catch (JSONException invalid) {
            throw FeatureJson.invalid(invalid);
        }
        return Immutable.copyList(out);
    }

    @Override
    public List<Preference> preferences() {
        var out = new ArrayList<Preference>();
        try (Cursor c =
                read().rawQuery(
                                "SELECT workout_id,current_revision,favourite FROM "
                                        + "strength_workout_preferences ORDER BY workout_id",
                                null)) {
            while (c.moveToNext())
                out.add(new Preference(c.getString(0), c.getInt(1), c.getInt(2) == 1));
        }
        return Immutable.copyList(out);
    }

    @Override
    public void savePreference(Preference preference) {
        Objects.requireNonNull(preference, "workout preference");
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            if (workoutRow(db, preference.workoutId(), preference.currentRevision()) == null)
                throw new IllegalArgumentException("The saved workout revision is missing.");
            ContentValues row = new ContentValues();
            row.put("workout_id", preference.workoutId());
            row.put("current_revision", preference.currentRevision());
            row.put("favourite", preference.favourite() ? 1 : 0);
            if (db.update(
                            "strength_workout_preferences",
                            row,
                            "workout_id=?",
                            new String[] {preference.workoutId()})
                    == 0) db.insertOrThrow("strength_workout_preferences", null, row);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Explicit deletion removes only this terminal execution; repeat templates remain retained. */
    public void deleteCompleted(String id) {
        SQLiteDatabase db = write();
        db.beginTransaction();
        try {
            deleteCompleted(db, validId(id));
            db.delete("strength_sessions", "id=?", new String[] {id});
            db.delete("coach_evidence", "kind='STRENGTH_LINK' AND id=?", new String[] {id});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    static void deleteCompleted(SQLiteDatabase db, String id) {
        try (Cursor c =
                db.rawQuery(
                        "SELECT state FROM strength_executions WHERE id=?",
                        new String[] {validId(id)})) {
            if (c.moveToFirst()
                    && !c.getString(0).equals("FINISHED")
                    && !c.getString(0).equals("DISCARDED"))
                throw new IllegalArgumentException(
                        "Finish or discard the unfinished gym workout first.");
        }
        acknowledgeSourceChange(db, id, null, Instant.now());
        db.delete("strength_executions", "id=?", new String[] {id});
    }

    /** Called in the same transaction as an explicitly accepted edit to the completed actual. */
    static void recordActualEdit(SQLiteDatabase db, StrengthSession edited, Instant at) {
        Objects.requireNonNull(edited, "edited actual workout");
        Objects.requireNonNull(at, "edit observation time");
        boolean staged = false;
        try (Cursor c =
                db.rawQuery(
                        "SELECT state,accepted_actual FROM strength_executions WHERE id=?",
                        new String[] {edited.id()})) {
            if (c.moveToFirst()) {
                staged = true;
                if (!c.getString(0).equals("FINISHED"))
                    throw new IllegalArgumentException(
                            "An unfinished gym execution cannot be replaced by a completed"
                                    + " workout.");
                if (!GymAcceptedActualJson.read(FeatureJson.object(c.getString(1)))
                        .equals(actual(db, edited.id()))) throw conflict();
            }
        }
        acknowledgeSourceChange(db, edited.id(), edited, at);
        if (!staged) return;
        ContentValues values = new ContentValues();
        values.put("accepted_actual", bounded(CoachingJson.strength(edited)));
        values.put("actual_edited_at", at.toString());
        db.update("strength_executions", values, "id=?", new String[] {edited.id()});
    }

    private static Stored load(SQLiteDatabase db, String id) {
        JSONObject header;
        long revision;
        int sets, events;
        Object[] indexed;
        try (Cursor c =
                db.rawQuery(
                        "SELECT"
                            + " revision,header,set_count,event_count,origin,state,started_at,title,recorded_elapsed,active_millis,workout_id,workout_revision"
                            + " FROM strength_executions WHERE id=?",
                        new String[] {id})) {
            if (!c.moveToFirst()) return null;
            revision = c.getLong(0);
            header = GymJsonSupport.read(c.getString(1));
            sets = c.getInt(2);
            events = c.getInt(3);
            indexed =
                    new Object[] {
                        c.getString(4),
                        c.getString(5),
                        c.getString(6),
                        c.getString(7),
                        c.getLong(8),
                        c.getLong(9),
                        c.isNull(10) ? null : c.getString(10),
                        c.isNull(11) ? null : c.getInt(11)
                    };
        }
        JSONArray actual = rows(db, "strength_execution_sets", id, sets);
        JSONArray timeline = rows(db, "strength_execution_events", id, events);
        try {
            header.put("actualSets", actual).put("events", timeline);
            if (!isNull(header, "completedSession")) {
                child(header, "completedSession").put("exercises", actualBlocks(actual));
            }
        } catch (JSONException invalid) {
            throw FeatureJson.invalid(invalid);
        }
        GymExecution execution = GymExecutionJson.decode(header);
        if (!execution.id().equals(id))
            throw new IllegalArgumentException("Gym storage identity differs.");
        GymWorkoutDefinition prescription = execution.prescription();
        Object[] expected =
                new Object[] {
                    execution.origin().name(),
                    execution.status().name(),
                    execution.startedAt().toString(),
                    prescription == null ? "Free workout" : prescription.title(),
                    execution.recordedElapsedMillis(),
                    execution.activeMillis(),
                    prescription == null ? null : prescription.id(),
                    prescription == null ? null : prescription.revision()
                };
        if (!java.util.Arrays.equals(indexed, expected))
            throw new IllegalArgumentException(
                    "Gym summary columns differ from the retained execution.");
        return new Stored(execution, revision);
    }

    private static JSONArray rows(SQLiteDatabase db, String table, String id, int expected) {
        JSONArray out = new JSONArray();
        try (Cursor c =
                db.rawQuery(
                        "SELECT sequence,payload FROM "
                                + table
                                + " WHERE execution_id=? ORDER BY sequence",
                        new String[] {id})) {
            while (c.moveToNext()) {
                if (c.getInt(0) != out.length() || out.length() >= expected) throw conflict();
                out.put(GymJsonSupport.read(c.getString(1)));
            }
        }
        if (out.length() != expected)
            throw new IllegalArgumentException("Gym staging rows are incomplete.");
        return out;
    }

    private static JSONArray actualBlocks(JSONArray actual) {
        var groups = new LinkedHashMap<BlockKey, JSONObject>();
        for (int i = 0; i < actual.length(); i++) {
            JSONObject row = row(actual, i);
            var key =
                    new BlockKey(
                            text(row, "slotId", 256),
                            GymWorkoutJson.exercise(child(row, "exercise")));
            JSONObject block = groups.get(key);
            if (block == null) {
                block = object("exercise", child(row, "exercise"), "sets", new JSONArray());
                groups.put(key, block);
            }
            array(block, "sets", 100).put(child(row, "actual"));
        }
        JSONArray result = new JSONArray();
        for (JSONObject block : groups.values()) result.put(block);
        return result;
    }

    private static Parts parts(GymExecution execution) {
        JSONObject json = GymExecutionJson.encode(execution);
        JSONArray actual = array(json, "actualSets", 10000);
        JSONArray events = array(json, "events", 50000);
        var setRows = new ArrayList<String>();
        var eventRows = new ArrayList<String>();
        for (int i = 0; i < actual.length(); i++) setRows.add(bounded(row(actual, i)));
        for (int i = 0; i < events.length(); i++) eventRows.add(bounded(row(events, i)));
        json.remove("actualSets");
        json.remove("events");
        if (!isNull(json, "completedSession")) child(json, "completedSession").remove("exercises");
        return new Parts(bounded(json), setRows, eventRows);
    }

    private static String bounded(JSONObject json) {
        String payload = json.toString();
        if (payload == null || payload.getBytes(StandardCharsets.UTF_8).length > MAX_ROW_BYTES)
            throw new IllegalArgumentException(
                    "This gym record exceeds the supported 256 KiB row limit."
                            + " Existing staged sets were retained.");
        return payload;
    }

    private static ContentValues summary(GymExecution snapshot, long revision, String header) {
        ContentValues row = new ContentValues();
        row.put("id", snapshot.id());
        row.put("revision", revision);
        row.put("origin", snapshot.origin().name());
        row.put("state", snapshot.status().name());
        row.put("started_at", snapshot.startedAt().toString());
        row.put(
                "title",
                snapshot.prescription() == null ? "Free workout" : snapshot.prescription().title());
        row.put("recorded_elapsed", snapshot.recordedElapsedMillis());
        row.put("active_millis", snapshot.activeMillis());
        row.put("set_count", snapshot.actualSets().size());
        row.put("event_count", snapshot.events().size());
        row.put(
                "workout_id",
                snapshot.prescription() == null ? null : snapshot.prescription().id());
        if (snapshot.prescription() == null) row.putNull("workout_revision");
        else row.put("workout_revision", snapshot.prescription().revision());
        row.put("header", header);
        row.put(
                "accepted_actual",
                snapshot.completedSession() == null
                        ? null
                        : bounded(CoachingJson.strength(snapshot.completedSession())));
        row.putNull("actual_edited_at");
        return row;
    }

    private static void insertExecution(
            SQLiteDatabase db, GymExecution snapshot, long revision, Parts prepared) {
        db.insertOrThrow(
                "strength_executions", null, summary(snapshot, revision, prepared.header()));
        appendRows(db, snapshot, prepared, 0, 0);
    }

    private static void appendRows(
            SQLiteDatabase db,
            GymExecution snapshot,
            Parts prepared,
            int existingSets,
            int existingEvents) {
        for (int i = existingSets; i < prepared.sets().size(); i++) {
            ContentValues row = new ContentValues();
            row.put("execution_id", snapshot.id());
            row.put("sequence", i);
            row.put("actual_set_id", snapshot.actualSets().get(i).actual().id());
            row.put("payload", prepared.sets().get(i));
            db.insertOrThrow("strength_execution_sets", null, row);
        }
        for (int i = existingEvents; i < prepared.events().size(); i++) {
            ContentValues row = new ContentValues();
            row.put("execution_id", snapshot.id());
            row.put("sequence", i);
            row.put("payload", prepared.events().get(i));
            db.insertOrThrow("strength_execution_events", null, row);
        }
    }

    private static WorkoutRow workoutRow(SQLiteDatabase db, String id, int revision) {
        try (Cursor c =
                db.rawQuery(
                        "SELECT payload,source_kind,source_id FROM strength_workout_versions WHERE "
                                + "workout_id=? AND revision=?",
                        new String[] {id, Integer.toString(revision)})) {
            if (!c.moveToFirst()) return null;
            WorkoutRow row = workoutRow(GymJsonSupport.read(c.getString(0)));
            if (!row.definition().id().equals(id)
                    || row.definition().revision() != revision
                    || !Objects.equals(
                            row.source() == null ? null : row.source().kind().name(),
                            c.isNull(1) ? null : c.getString(1))
                    || !Objects.equals(
                            row.source() == null ? null : row.source().id(),
                            c.isNull(2) ? null : c.getString(2))) throw conflict();
            return row;
        }
    }

    private static WorkoutRow workoutRow(JSONObject json) {
        fields(json, "definition", "source", "sourceSnapshot");
        var definition = GymWorkoutJson.decode(child(json, "definition"));
        var source = isNull(json, "source") ? null : source(child(json, "source"));
        if (source != null && source.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
            throw new IllegalArgumentException(
                    "A saved revision cannot create itself as a source.");
        var proof = isNull(json, "sourceSnapshot") ? null : child(json, "sourceSnapshot");
        if (source == null || source.kind() != GymExecution.SourceKind.ACTUAL_SESSION) {
            if (proof != null)
                throw new IllegalArgumentException("Unexpected repeated-source evidence.");
        } else {
            fields(
                    proof,
                    "kind",
                    "actualSessionId",
                    "definition",
                    "templateKind",
                    "originalSourceSlots",
                    "originalSourceFingerprint",
                    "currentSourceFingerprint",
                    "sourceEditedAt",
                    "sourceDeletedAt");
            if (!text(proof, "kind", 64).equals("REPEATED_TEMPLATE")
                    || !text(proof, "actualSessionId", 256).equals(source.id())
                    || !GymWorkoutJson.decode(child(proof, "definition")).equals(definition))
                throw new IllegalArgumentException(
                        "Repeated-source evidence differs from its frozen template.");
            validateFrozenRepeat(definition, proof);
        }
        return new WorkoutRow(definition, source, proof);
    }

    private static JSONObject workoutJson(WorkoutRow row) {
        return object(
                "definition",
                GymWorkoutJson.encode(row.definition()),
                "source",
                row.source() == null ? null : source(row.source()),
                "sourceSnapshot",
                row.sourceSnapshot());
    }

    private static JSONObject source(GymExecution.SourceReference source) {
        return object(
                "kind",
                source.kind().name(),
                "id",
                source.id(),
                "revision",
                source.revision(),
                "sessionId",
                source.sessionId());
    }

    private static GymExecution.SourceReference source(JSONObject json) {
        fields(json, "kind", "id", "revision", "sessionId");
        return new GymExecution.SourceReference(
                choice(json, "kind", GymExecution.SourceKind.class),
                text(json, "id", 256),
                integer(json, "revision"),
                nullableText(json, "sessionId", 256));
    }

    private static void ensureWorkout(
            SQLiteDatabase db,
            GymWorkoutDefinition definition,
            GymExecution.SourceReference source) {
        if (definition == null) {
            if (source != null)
                throw new IllegalArgumentException("A workout source requires its prescription.");
            return;
        }
        WorkoutRow existing = workoutRow(db, definition.id(), definition.revision());
        if (existing != null) {
            if (!existing.definition().equals(definition)) throw conflict();
            if (source != null && source.kind() == GymExecution.SourceKind.SAVED_WORKOUT) {
                GymWorkoutSourceValidator.savedWorkout(definition, source, existing.definition());
            } else if (!Objects.equals(existing.source(), source)) throw conflict();
            validateProgrammeSource(db, existing);
            if (existing.source() != null
                    && existing.source().kind() == GymExecution.SourceKind.ACTUAL_SESSION)
                validateAvailableSource(
                        existing,
                        actual(db, existing.source().id()),
                        execution(db, existing.source().id()));
            return;
        }
        JSONObject proof = null;
        if (source != null) {
            if (source.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
                throw new IllegalArgumentException("The saved workout revision is missing.");
            if (source.kind() == GymExecution.SourceKind.PROGRAMME) {
                GymWorkoutSourceValidator.programme(definition, source, programme(db, source));
            } else {
                proof = actualSourceProof(db, definition, source);
            }
        }
        WorkoutRow row = new WorkoutRow(definition, source, proof);
        insertWorkout(db, row);
    }

    private static void insertWorkout(SQLiteDatabase db, WorkoutRow row) {
        ContentValues values = new ContentValues();
        values.put("workout_id", row.definition().id());
        values.put("revision", row.definition().revision());
        values.put("source_kind", row.source() == null ? null : row.source().kind().name());
        values.put("source_id", row.source() == null ? null : row.source().id());
        values.put("payload", bounded(workoutJson(row)));
        db.insertOrThrow("strength_workout_versions", null, values);
    }

    private static JSONObject actualSourceProof(
            SQLiteDatabase db,
            GymWorkoutDefinition definition,
            GymExecution.SourceReference source) {
        StrengthSession actual = actual(db, source.id());
        if (actual == null)
            throw new IllegalArgumentException("The actual repeat source is missing.");
        Stored prior = load(db, source.id());
        if (prior != null && prior.execution().status() == GymExecution.Status.FINISHED) {
            try {
                GymWorkoutSourceValidator.repeatedExecution(definition, source, prior.execution());
                return repeatProof(
                        definition, source, "EXECUTION", prior.execution().slots(), actual);
            } catch (IllegalArgumentException notExecutionStructure) {
                // An explicit repeat of its actual working exercise list is also supported.
            }
        }
        GymWorkoutSourceValidator.repeatedActual(definition, source, actual);
        return repeatProof(definition, source, "ACTUAL", definition.slots(), actual);
    }

    private static GymExecution execution(SQLiteDatabase db, String id) {
        Stored row = load(db, id);
        return row == null ? null : row.execution();
    }

    private static JSONObject repeatProof(
            GymWorkoutDefinition definition,
            GymExecution.SourceReference source,
            String kind,
            List<GymWorkoutDefinition.Slot> slots,
            StrengthSession actual) {
        String fingerprint = fingerprint(actual);
        return object(
                "kind",
                "REPEATED_TEMPLATE",
                "actualSessionId",
                source.id(),
                "definition",
                GymWorkoutJson.encode(definition),
                "templateKind",
                kind,
                "originalSourceSlots",
                GymWorkoutJson.slots(slots),
                "originalSourceFingerprint",
                fingerprint,
                "currentSourceFingerprint",
                fingerprint,
                "sourceEditedAt",
                null,
                "sourceDeletedAt",
                null);
    }

    private static void validateFrozenRepeat(GymWorkoutDefinition definition, JSONObject proof) {
        String templateKind = text(proof, "templateKind", 16);
        if (!templateKind.equals("ACTUAL") && !templateKind.equals("EXECUTION")) throw conflict();
        var originals = GymWorkoutJson.slots(array(proof, "originalSourceSlots", 100));
        var expected = new ArrayList<GymWorkoutDefinition.Slot>();
        for (int i = 0; i < originals.size(); i++) {
            var slot = originals.get(i);
            if (templateKind.equals("ACTUAL")
                    && (slot.target() != null
                            || slot.supersetGroup() != null
                            || !slot.id().equals("slot-" + i))) throw conflict();
            expected.add(
                    new GymWorkoutDefinition.Slot(
                            slot.id(),
                            slot.exercise(),
                            slot.target() == null ? null : slot.target().withoutLoadReference(),
                            slot.supersetGroup()));
        }
        if (!definition.slots().equals(expected)) throw conflict();
        requireFingerprint(text(proof, "originalSourceFingerprint", 64));
        if (isNull(proof, "sourceDeletedAt")) {
            requireFingerprint(text(proof, "currentSourceFingerprint", 64));
            if (isNull(proof, "sourceEditedAt")
                    && !text(proof, "originalSourceFingerprint", 64)
                            .equals(text(proof, "currentSourceFingerprint", 64))) throw conflict();
        } else {
            instant(proof, "sourceDeletedAt");
            if (!isNull(proof, "currentSourceFingerprint")) throw conflict();
        }
        if (!isNull(proof, "sourceEditedAt")) instant(proof, "sourceEditedAt");
    }

    private static void validateAvailableSource(
            WorkoutRow row, StrengthSession actual, GymExecution execution) {
        JSONObject proof = row.sourceSnapshot();
        if (!isNull(proof, "sourceDeletedAt")) {
            if (actual != null || execution != null)
                throw new IllegalArgumentException(
                        "A deleted repeat source still appears as retained evidence.");
            return;
        }
        if (actual == null
                || !fingerprint(actual).equals(text(proof, "currentSourceFingerprint", 64)))
            throw new IllegalArgumentException(
                    "The repeat source changed without an explicit retained decision.");
        String kind = text(proof, "templateKind", 16);
        if (kind.equals("EXECUTION")) {
            if (execution == null
                    || !execution
                            .slots()
                            .equals(GymWorkoutJson.slots(array(proof, "originalSourceSlots", 100))))
                throw conflict();
            GymWorkoutSourceValidator.repeatedExecution(row.definition(), row.source(), execution);
        } else if (isNull(proof, "sourceEditedAt")) {
            GymWorkoutSourceValidator.repeatedActual(row.definition(), row.source(), actual);
        }
    }

    private static void acknowledgeSourceChange(
            SQLiteDatabase db, String id, StrengthSession changed, Instant at) {
        var rows = new ArrayList<WorkoutRow>();
        try (Cursor c =
                db.rawQuery(
                        "SELECT payload FROM strength_workout_versions WHERE"
                                + " source_kind='ACTUAL_SESSION' AND source_id=?",
                        new String[] {id})) {
            while (c.moveToNext()) rows.add(workoutRow(GymJsonSupport.read(c.getString(0))));
        }
        if (rows.isEmpty()) return;
        StrengthSession before = actual(db, id);
        GymExecution original = execution(db, id);
        for (WorkoutRow row : rows) {
            validateAvailableSource(row, before, original);
            JSONObject proof = row.sourceSnapshot();
            if (changed == null && !isNull(proof, "sourceDeletedAt")) continue;
            try {
                if (changed == null)
                    proof.put("sourceDeletedAt", at.toString())
                            .put("currentSourceFingerprint", JSONObject.NULL);
                else
                    proof.put("sourceEditedAt", at.toString())
                            .put("currentSourceFingerprint", fingerprint(changed));
            } catch (JSONException invalid) {
                throw FeatureJson.invalid(invalid);
            }
            ContentValues values = new ContentValues();
            values.put("payload", bounded(workoutJson(row)));
            db.update(
                    "strength_workout_versions",
                    values,
                    "workout_id=? AND revision=?",
                    new String[] {
                        row.definition().id(), Integer.toString(row.definition().revision())
                    });
        }
    }

    private static String fingerprint(StrengthSession actual) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(
                                    CoachingJson.strength(actual)
                                            .toString()
                                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest)
                hex.append(Character.forDigit((value >>> 4) & 15, 16))
                        .append(Character.forDigit(value & 15, 16));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required.", impossible);
        }
    }

    private static void requireFingerprint(String fingerprint) {
        if (!fingerprint.matches("[0-9a-f]{64}")) throw conflict();
    }

    private static GymProgramme programme(SQLiteDatabase db, GymExecution.SourceReference source) {
        try (Cursor c =
                db.rawQuery(
                        "SELECT payload FROM gym_plan_versions WHERE version_id=?",
                        new String[] {source.id() + ":v" + source.revision()})) {
            if (!c.moveToFirst())
                throw new IllegalArgumentException("The retained programme revision is missing.");
            return GymProgrammeStore.parse(FeatureJson.object(c.getString(0)));
        }
    }

    private static void validateProgrammeSource(SQLiteDatabase db, WorkoutRow row) {
        if (row.source() != null && row.source().kind() == GymExecution.SourceKind.PROGRAMME)
            GymWorkoutSourceValidator.programme(
                    row.definition(), row.source(), programme(db, row.source()));
    }

    private static StrengthSession actual(SQLiteDatabase db, String id) {
        try (Cursor c =
                db.rawQuery(
                        "SELECT payload FROM strength_sessions WHERE id=?", new String[] {id})) {
            if (!c.moveToFirst()) return null;
            try {
                return CoachingJson.strength(FeatureJson.object(c.getString(0)));
            } catch (JSONException invalid) {
                throw FeatureJson.invalid(invalid);
            }
        }
    }

    private static void saveActualAndAssociation(
            SQLiteDatabase db, GymExecution finished, String payload) {
        StrengthSession existing = actual(db, finished.id());
        if (existing != null && !existing.equals(finished.completedSession())) throw conflict();
        if (existing == null) {
            ContentValues row = new ContentValues();
            row.put("id", finished.id());
            row.put("started_at", finished.startedAt().toString());
            row.put("payload", payload);
            db.insertOrThrow("strength_sessions", null, row);
        }
        var source = finished.plannedAssociation();
        if (source != null) {
            var session =
                    GymWorkoutSourceValidator.programme(
                            finished.prescription(), source, programme(db, source));
            JSONObject expected =
                    object(
                            "id",
                            finished.id(),
                            "plannedDate",
                            session.date().toString(),
                            "gymVersion",
                            source.id() + ":v" + source.revision(),
                            "gymSession",
                            source.sessionId());
            try (Cursor c =
                    db.rawQuery(
                            "SELECT payload FROM coach_evidence WHERE kind='STRENGTH_LINK' AND"
                                    + " id=?",
                            new String[] {finished.id()})) {
                if (c.moveToFirst()) {
                    if (!sameLink(FeatureJson.object(c.getString(0)), expected)) throw conflict();
                    return;
                }
            }
            ContentValues row = new ContentValues();
            row.put("kind", "STRENGTH_LINK");
            row.put("id", finished.id());
            row.put("payload", bounded(expected));
            db.insertOrThrow("coach_evidence", null, row);
        }
    }

    private static void verifyCompleted(SQLiteDatabase db, GymExecution finished) {
        if (!finished.completedSession().equals(actual(db, finished.id()))) throw conflict();
        if (finished.plannedAssociation() != null) {
            try (Cursor c =
                    db.rawQuery(
                            "SELECT payload FROM coach_evidence WHERE kind='STRENGTH_LINK' AND"
                                    + " id=?",
                            new String[] {finished.id()})) {
                if (!c.moveToFirst()) throw conflict();
                checkLink(
                        finished,
                        FeatureJson.object(c.getString(0)),
                        programme(db, finished.source()));
            }
        }
    }

    private static void checkLink(GymExecution execution, JSONObject link, GymProgramme programme) {
        var session =
                GymWorkoutSourceValidator.programme(
                        execution.prescription(), execution.source(), programme);
        JSONObject expected =
                object(
                        "id",
                        execution.id(),
                        "plannedDate",
                        session.date().toString(),
                        "gymVersion",
                        programme.id() + ":v" + programme.version(),
                        "gymSession",
                        session.id());
        if (!sameLink(link, expected))
            throw new IllegalArgumentException(
                    "The gym execution association differs from its original prescription.");
    }

    private static boolean sameLink(JSONObject actual, JSONObject expected) {
        for (String name : new String[] {"id", "plannedDate", "gymVersion", "gymSession"}) {
            if (!Objects.equals(actual.opt(name), expected.opt(name))) return false;
        }
        return true;
    }

    public static JSONObject exportData(SQLiteDatabase db) {
        JSONArray workouts = new JSONArray(),
                preferences = new JSONArray(),
                executions = new JSONArray();
        try (Cursor c =
                db.rawQuery(
                        "SELECT payload FROM strength_workout_versions ORDER BY"
                                + " workout_id,revision",
                        null)) {
            while (c.moveToNext()) workouts.put(GymJsonSupport.read(c.getString(0)));
        }
        try (Cursor c =
                db.rawQuery(
                        "SELECT workout_id,current_revision,favourite FROM"
                                + " strength_workout_preferences ORDER BY workout_id",
                        null)) {
            while (c.moveToNext())
                preferences.put(
                        object(
                                "id",
                                c.getString(0),
                                "currentRevision",
                                c.getInt(1),
                                "favourite",
                                c.getInt(2) == 1));
        }
        try (Cursor c =
                db.rawQuery(
                        "SELECT id,accepted_actual,actual_edited_at FROM strength_executions ORDER"
                                + " BY started_at,id",
                        null)) {
            while (c.moveToNext()) {
                Stored row = load(db, c.getString(0));
                executions.put(
                        object(
                                "revision",
                                row.revision(),
                                "snapshot",
                                GymExecutionJson.encode(row.execution()),
                                "currentActual",
                                c.isNull(1) ? null : FeatureJson.object(c.getString(1)),
                                "actualEditedAt",
                                c.isNull(2) ? null : c.getString(2)));
            }
        }
        return object(
                "version",
                BACKUP_VERSION,
                "workouts",
                workouts,
                "preferences",
                preferences,
                "executions",
                executions);
    }

    public static void validate(
            JSONObject data, JSONObject coaching, JSONObject gym, JSONObject development) {
        fields(data, "version", "workouts", "preferences", "executions");
        version(data, BACKUP_VERSION);
        var workouts = new HashMap<WorkoutKey, WorkoutRow>();
        JSONArray rows = array(data, "workouts", 10000);
        for (int i = 0; i < rows.length(); i++) {
            WorkoutRow workout = workoutRow(row(rows, i));
            bounded(workoutJson(workout));
            WorkoutKey key =
                    new WorkoutKey(workout.definition().id(), workout.definition().revision());
            if (workouts.put(key, workout) != null)
                throw new IllegalArgumentException("Duplicate frozen workout revision.");
        }
        var programmes = new HashMap<String, GymProgramme>();
        if (gym != null) {
            rows = array(gym, "versions", 1000);
            for (int i = 0; i < rows.length(); i++) {
                var p = GymProgrammeStore.parse(row(rows, i));
                programmes.put(p.id() + ":v" + p.version(), p);
            }
        }
        for (WorkoutRow workout : workouts.values()) {
            var source = workout.source();
            if (source != null && source.kind() == GymExecution.SourceKind.PROGRAMME)
                GymWorkoutSourceValidator.programme(
                        workout.definition(),
                        source,
                        programmes.get(source.id() + ":v" + source.revision()));
            if (source != null && source.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
                throw new IllegalArgumentException(
                        "A saved revision cannot create itself as a source.");
        }
        var preferenceIds = new HashSet<String>();
        rows = array(data, "preferences", 10000);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = row(rows, i);
            fields(row, "id", "currentRevision", "favourite");
            var p =
                    new Preference(
                            text(row, "id", 256),
                            integer(row, "currentRevision"),
                            bool(row, "favourite"));
            if (!preferenceIds.add(p.workoutId())
                    || !workouts.containsKey(new WorkoutKey(p.workoutId(), p.currentRevision())))
                throw new IllegalArgumentException(
                        "Saved workout preference is duplicated or missing its revision.");
        }
        var actuals = new HashMap<String, StrengthSession>();
        if (coaching != null) {
            rows = array(coaching, "strength_sessions", 100000);
            for (int i = 0; i < rows.length(); i++) {
                try {
                    var actual =
                            CoachingJson.strength(
                                    FeatureJson.object(
                                            text(row(rows, i), "payload", 16 * 1024 * 1024)));
                    actuals.put(actual.id(), actual);
                } catch (JSONException invalid) {
                    throw FeatureJson.invalid(invalid);
                }
            }
        }
        var links = new HashMap<String, JSONObject>();
        if (development != null) {
            rows = array(development, "records", 100000);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = row(rows, i);
                if (text(row, "kind", 64).equals("STRENGTH_LINK"))
                    links.put(text(row, "id", 256), child(row, "payload"));
            }
        }
        var executionIds = new HashSet<String>();
        var retainedExecutions = new HashMap<String, GymExecution>();
        int unfinished = 0;
        rows = array(data, "executions", 100000);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = row(rows, i);
            fields(row, "revision", "snapshot", "currentActual", "actualEditedAt");
            if (whole(row, "revision") < 1) throw conflict();
            GymExecution execution = GymExecutionJson.decode(child(row, "snapshot"));
            parts(execution);
            if (!executionIds.add(execution.id()))
                throw new IllegalArgumentException("Duplicate gym execution.");
            retainedExecutions.put(execution.id(), execution);
            var source = execution.source();
            if (source != null
                    && source.kind() == GymExecution.SourceKind.ACTUAL_SESSION
                    && source.id().equals(execution.id()))
                throw new IllegalArgumentException("A gym execution cannot repeat itself.");
            if (execution.prescription() != null) {
                WorkoutRow retained =
                        workouts.get(
                                new WorkoutKey(
                                        execution.prescription().id(),
                                        execution.prescription().revision()));
                if (retained == null || !retained.definition().equals(execution.prescription()))
                    throw new IllegalArgumentException(
                            "The execution's exact workout snapshot is missing.");
                if (source != null && source.kind() == GymExecution.SourceKind.SAVED_WORKOUT)
                    GymWorkoutSourceValidator.savedWorkout(
                            execution.prescription(), source, retained.definition());
                else if (!Objects.equals(source, retained.source()))
                    throw new IllegalArgumentException("Execution and workout source disagree.");
            } else if (source != null)
                throw new IllegalArgumentException("A source requires its workout.");
            if (execution.status() == GymExecution.Status.FINISHED) {
                bounded(CoachingJson.strength(execution.completedSession()));
                StrengthSession current = GymAcceptedActualJson.read(child(row, "currentActual"));
                bounded(CoachingJson.strength(current));
                if (!current.id().equals(execution.id())
                        || !current.equals(actuals.get(execution.id())))
                    throw new IllegalArgumentException(
                            "Accepted gym evidence is missing or differs from its actual workout.");
                if (!isNull(row, "actualEditedAt")) instant(row, "actualEditedAt");
                if (!current.equals(execution.completedSession()) && isNull(row, "actualEditedAt"))
                    throw new IllegalArgumentException(
                            "A changed actual workout needs its explicit edit observation.");
                if (execution.plannedAssociation() != null) {
                    JSONObject link = links.get(execution.id());
                    if (link == null)
                        throw new IllegalArgumentException(
                                "The original planned gym association is missing.");
                    checkLink(
                            execution,
                            link,
                            programmes.get(source.id() + ":v" + source.revision()));
                }
            } else {
                if (!isNull(row, "currentActual")
                        || !isNull(row, "actualEditedAt")
                        || actuals.containsKey(execution.id()))
                    throw new IllegalArgumentException(
                            "Unfinished or discarded execution cannot claim completed actual"
                                    + " evidence.");
                if (execution.status() != GymExecution.Status.DISCARDED && ++unfinished > 1)
                    throw new IllegalArgumentException(
                            "A backup contains several unfinished gym workouts.");
            }
        }
        for (WorkoutRow workout : workouts.values()) {
            if (workout.source() != null
                    && workout.source().kind() == GymExecution.SourceKind.ACTUAL_SESSION)
                validateAvailableSource(
                        workout,
                        actuals.get(workout.source().id()),
                        retainedExecutions.get(workout.source().id()));
        }
    }

    public static void clear(SQLiteDatabase db) {
        db.delete("strength_execution_events", null, null);
        db.delete("strength_execution_sets", null, null);
        db.delete("strength_executions", null, null);
        db.delete("strength_workout_preferences", null, null);
        db.delete("strength_workout_versions", null, null);
    }

    /** The complete graph must be validated by the backup boundary before replacement starts. */
    public static void restore(SQLiteDatabase db, JSONObject data) {
        fields(data, "version", "workouts", "preferences", "executions");
        version(data, BACKUP_VERSION);
        JSONArray rows = array(data, "workouts", 10000);
        for (int i = 0; i < rows.length(); i++) insertWorkout(db, workoutRow(row(rows, i)));
        rows = array(data, "preferences", 10000);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = row(rows, i);
            ContentValues values = new ContentValues();
            values.put("workout_id", text(row, "id", 256));
            values.put("current_revision", integer(row, "currentRevision"));
            values.put("favourite", bool(row, "favourite") ? 1 : 0);
            db.insertOrThrow("strength_workout_preferences", null, values);
        }
        rows = array(data, "executions", 100000);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = row(rows, i);
            var execution = GymExecutionJson.decode(child(row, "snapshot"));
            insertExecution(db, execution, whole(row, "revision"), parts(execution));
            if (execution.status() == GymExecution.Status.FINISHED) {
                ContentValues values = new ContentValues();
                values.put("accepted_actual", bounded(child(row, "currentActual")));
                values.put("actual_edited_at", nullableText(row, "actualEditedAt", 64));
                db.update("strength_executions", values, "id=?", new String[] {execution.id()});
            }
        }
    }

    private static String validId(String id) {
        if (id == null || id.trim().isEmpty() || id.length() > 256)
            throw new IllegalArgumentException("A stable gym ID is required.");
        return id;
    }

    private static IllegalArgumentException conflict() {
        return new IllegalArgumentException(
                "The gym workout changed or conflicts with retained evidence. Reload it before"
                        + " saving.");
    }
}
