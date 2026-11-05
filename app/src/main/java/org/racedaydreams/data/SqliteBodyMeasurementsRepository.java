package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/** Typed height/preferences storage; existing weight history keeps its original durable owner. */
public final class SqliteBodyMeasurementsRepository extends SqliteStoreAccess
        implements BodyMeasurementsRepository {
    private final DevelopmentStore weights;

    public SqliteBodyMeasurementsRepository(Context context) {
        super(context);
        weights = new DevelopmentStore(context);
    }

    public record Snapshot(List<BodyHeight> heights, Preferences preferences) {
        public Snapshot {
            if (heights.size() > 100000)
                throw new IllegalArgumentException(
                        "Height history exceeds this format limit; no records were truncated.");
            heights = Immutable.copyList(heights);
            Objects.requireNonNull(preferences);
            var ids = new HashSet<String>();
            var sources = new HashSet<List<String>>();
            for (var row : heights)
                if (!ids.add(row.id())
                        || !sources.add(Immutable.list(row.provider(), row.sourceId())))
                    throw new IllegalArgumentException("Duplicate height identity.");
        }
    }

    @Override
    public List<BodyHeight> heights(int limit, int offset) {
        if (limit < 1 || limit > 500 || offset < 0)
            throw new IllegalArgumentException("Invalid height page.");
        return heights(read(), limit, offset);
    }

    private static List<BodyHeight> heights(SQLiteDatabase db, int limit, int offset) {
        var result = new ArrayList<BodyHeight>();
        try (var c =
                db.rawQuery(
                        "SELECT"
                            + " id,metres,observed_at,offset_seconds,provider,source_id,origin,imported_at"
                            + " FROM body_heights ORDER BY observed_seconds DESC,observed_nanos"
                            + " DESC,id LIMIT ? OFFSET ?",
                        new String[] {Integer.toString(limit), Integer.toString(offset)})) {
            while (c.moveToNext())
                result.add(
                        new BodyHeight(
                                c.getString(0),
                                c.getDouble(1),
                                Instant.parse(c.getString(2)),
                                c.isNull(3) ? null : ZoneOffset.ofTotalSeconds(c.getInt(3)),
                                c.getString(4),
                                c.getString(5),
                                c.isNull(6) ? null : c.getString(6),
                                c.isNull(7) ? null : Instant.parse(c.getString(7))));
        }
        return result;
    }

    @Override
    public List<BodyWeight> weights() {
        requireIo();
        return weights.weights();
    }

    @Override
    public void saveWeight(BodyWeight value) {
        requireIo();
        Objects.requireNonNull(value);
        weights.saveWeight(value);
    }

    @Override
    public void saveHeight(BodyHeight value) {
        put(write(), Objects.requireNonNull(value));
    }

    @Override
    public void deleteHeight(String id) {
        write().delete("body_heights", "id=?", new String[] {required(id, "height")});
    }

    private static void put(SQLiteDatabase db, BodyHeight value) {
        var v = values("id", value.id());
        v.put("metres", value.metres());
        v.put("observed_at", value.at().toString());
        v.put("observed_seconds", value.at().getEpochSecond());
        v.put("observed_nanos", value.at().getNano());
        if (value.offset() == null) v.putNull("offset_seconds");
        else v.put("offset_seconds", value.offset().getTotalSeconds());
        v.put("provider", value.provider());
        v.put("source_id", value.sourceId());
        v.put("origin", value.origin());
        v.put("imported_at", value.importedAt() == null ? null : value.importedAt().toString());
        // Do not replace a conflicting source identity by SQLite's destructive REPLACE handling.
        try (var c =
                db.rawQuery(
                        "SELECT id FROM body_heights WHERE provider=? AND source_id=?",
                        new String[] {value.provider(), value.sourceId()})) {
            if (c.moveToFirst() && !c.getString(0).equals(value.id()))
                throw new IllegalArgumentException(
                        "Height source already has a different identity.");
        }
        if (db.update("body_heights", v, "id=?", new String[] {value.id()}) == 0)
            db.insertOrThrow("body_heights", null, v);
    }

    @Override
    public Preferences preferences() {
        return preferences(read());
    }

    private static Preferences preferences(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT protein_enabled,height_unit,basis_id,basis_at,basis_kg FROM"
                                + " body_guidance_preferences WHERE id=1",
                        null)) {
            if (!c.moveToFirst()) return Preferences.defaults();
            return new Preferences(
                    c.getInt(0) == 1,
                    HeightUnit.valueOf(c.getString(1)),
                    c.isNull(2)
                            ? null
                            : new ProteinGuidance.Basis(
                                    c.getString(2), Instant.parse(c.getString(3)), c.getDouble(4)));
        }
    }

    @Override
    public void preferences(Preferences value) {
        preferences(write(), Objects.requireNonNull(value));
    }

    private static void preferences(SQLiteDatabase db, Preferences value) {
        var v = values("id", "1");
        v.put("protein_enabled", value.proteinEnabled() ? 1 : 0);
        v.put("height_unit", value.heightUnit().name());
        if (value.basis() == null) {
            v.putNull("basis_id");
            v.putNull("basis_at");
            v.putNull("basis_kg");
        } else {
            v.put("basis_id", value.basis().id());
            v.put("basis_at", value.basis().at().toString());
            v.put("basis_kg", value.basis().kilograms());
        }
        replace(db, "body_guidance_preferences", v);
    }

    @Override
    public void replaceImportedWindow(
            String provider,
            Instant from,
            Instant until,
            List<BodyHeight> heights,
            List<BodyWeight> values) {
        required(provider, "body source");
        if (!from.isBefore(until)) throw new IllegalArgumentException("Invalid import window.");
        if (heights != null) {
            for (var row : heights) {
                if (!row.provider().equals(provider)
                        || row.at().isBefore(from)
                        || !row.at().isBefore(until)) {
                    throw new IllegalArgumentException("Height is outside its authorized window.");
                }
            }
            new Snapshot(heights, Preferences.defaults());
        }
        if (values != null) {
            var ids = new HashSet<String>();
            for (var row : values) {
                if (!row.provider().equals(provider)
                        || row.at().isBefore(from)
                        || !row.at().isBefore(until)
                        || !ids.add(row.id())) {
                    throw new IllegalArgumentException(
                            "Weight is outside its authorized window or duplicated.");
                }
            }
        }
        var db = write();
        db.beginTransaction();
        try {
            if (heights != null) {
                // Instants can have different fractional precision: compare as Instants, not SQL
                // text.
                var remove = new ArrayList<String>();
                try (var c =
                        db.rawQuery(
                                "SELECT id,observed_at FROM body_heights WHERE provider=?",
                                new String[] {provider})) {
                    while (c.moveToNext()) {
                        var at = Instant.parse(c.getString(1));
                        if (!at.isBefore(from) && at.isBefore(until)) remove.add(c.getString(0));
                    }
                }
                for (var id : remove) db.delete("body_heights", "id=?", new String[] {id});
                for (var row : heights) put(db, row);
            }
            if (values != null) weights.replaceWeightWindow(provider, from, until, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static JSONObject exportData(SQLiteDatabase db) {
        var rows = new ArrayList<BodyHeight>();
        int offset = 0;
        while (true) {
            var page = heights(db, 500, offset);
            rows.addAll(page);
            if (page.size() < 500) break;
            offset = Math.addExact(offset, 500);
        }
        if (rows.size() > 100000)
            throw new IllegalArgumentException(
                    "Height history exceeds this backup format limit; no records were truncated.");
        var array = new JSONArray();
        for (var row : rows)
            array.put(
                    FeatureJson.json(
                            "id",
                            row.id(),
                            "metres",
                            row.metres(),
                            "at",
                            row.at().toString(),
                            "offset",
                            row.offset() == null ? null : row.offset().toString(),
                            "provider",
                            row.provider(),
                            "sourceId",
                            row.sourceId(),
                            "origin",
                            row.origin(),
                            "importedAt",
                            row.importedAt() == null ? null : row.importedAt().toString()));
        var p = preferences(db);
        var basis = p.basis();
        return FeatureJson.json(
                "version",
                1,
                "heights",
                array,
                "preferences",
                FeatureJson.json(
                        "proteinEnabled",
                        p.proteinEnabled(),
                        "heightUnit",
                        p.heightUnit().name(),
                        "basis",
                        basis == null
                                ? null
                                : FeatureJson.json(
                                        "id",
                                        basis.id(),
                                        "at",
                                        basis.at().toString(),
                                        "kg",
                                        basis.kilograms())));
    }

    public static Snapshot validate(JSONObject input) {
        if (input == null) return new Snapshot(Collections.emptyList(), Preferences.defaults());
        fields(input, "version", "heights", "preferences");
        if (integer(input, "version") != 1) throw invalid("body format");
        var rows = array(input, "heights", 100000);
        var values = new ArrayList<BodyHeight>();
        for (int i = 0; i < rows.length(); i++) {
            var row = row(rows, i);
            fields(
                    row,
                    "id",
                    "metres",
                    "at",
                    "offset",
                    "provider",
                    "sourceId",
                    "origin",
                    "importedAt");
            values.add(
                    new BodyHeight(
                            text(row, "id", 256),
                            number(row, "metres"),
                            Instant.parse(text(row, "at", 128)),
                            nullableText(row, "offset", 32) == null
                                    ? null
                                    : ZoneOffset.of(nullableText(row, "offset", 32)),
                            text(row, "provider", 256),
                            text(row, "sourceId", 512),
                            nullableText(row, "origin", 512),
                            nullableText(row, "importedAt", 128) == null
                                    ? null
                                    : Instant.parse(nullableText(row, "importedAt", 128))));
        }
        var p = child(input, "preferences");
        fields(p, "proteinEnabled", "heightUnit", "basis");
        ProteinGuidance.Basis basis = null;
        if (!p.has("basis")) throw invalid("body basis field");
        if (!p.isNull("basis")) {
            var b = child(p, "basis");
            fields(b, "id", "at", "kg");
            basis =
                    new ProteinGuidance.Basis(
                            text(b, "id", 256), Instant.parse(text(b, "at", 128)), number(b, "kg"));
        }
        return new Snapshot(
                values,
                new Preferences(
                        bool(p, "proteinEnabled"),
                        choice(p, "heightUnit", HeightUnit.class),
                        basis));
    }

    public static void restore(SQLiteDatabase db, Snapshot snapshot) {
        db.delete("body_heights", null, null);
        db.delete("body_guidance_preferences", null, null);
        for (var row : snapshot.heights()) put(db, row);
        preferences(db, snapshot.preferences());
    }
}
