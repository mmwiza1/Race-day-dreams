package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;
import org.racedaydreams.domain.achievements.TrainingMilestones.*;
import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/**
 * Fixed, bounded milestone definitions with normalized source proof; shared restore transaction.
 */
public final class SqliteAchievementsRepository extends SqliteStoreAccess {
    public SqliteAchievementsRepository(Context context) {
        super(context);
    }

    public record Stored(Award award, Instant recognizedAt) {
        public Stored {
            Objects.requireNonNull(award);
            Objects.requireNonNull(recognizedAt);
            if (recognizedAt.isBefore(award.earnedAt()))
                throw new IllegalArgumentException("Recognition precedes evidence.");
        }
    }

    public record Snapshot(boolean celebrations, List<Stored> awards) {
        public Snapshot {
            awards = Immutable.copyList(awards);
            var ids = new HashSet<Kind>();
            for (var row : awards)
                if (!ids.add(row.award().kind()))
                    throw new IllegalArgumentException("Duplicate milestone.");
        }
    }

    public List<Stored> awards() {
        return awards(read());
    }

    public boolean celebrations() {
        return celebrations(read());
    }

    private static boolean celebrations(SQLiteDatabase db) {
        try (var c =
                db.rawQuery(
                        "SELECT celebrations FROM training_milestone_preferences WHERE id=1",
                        null)) {
            return c.moveToFirst() && c.getInt(0) == 1;
        }
    }

    public void celebrations(boolean enabled) {
        putPreferences(write(), enabled);
    }

    private static void putPreferences(SQLiteDatabase db, boolean enabled) {
        var v = values("id", "1");
        v.put("celebrations", enabled ? 1 : 0);
        replace(db, "training_milestone_preferences", v);
    }

    /** Existing recognition is never duplicated or silently redated by older imported history. */
    public int recognize(List<Award> candidates, Instant now) {
        var db = write();
        db.beginTransaction();
        try {
            var known = new HashSet<Kind>();
            for (var award : awards(db)) known.add(award.award().kind());
            int added = 0;
            for (var candidate : candidates)
                if (known.add(candidate.kind())) {
                    put(db, new Stored(candidate, now));
                    added++;
                }
            db.setTransactionSuccessful();
            return added;
        } finally {
            db.endTransaction();
        }
    }

    private static List<Stored> awards(SQLiteDatabase db) {
        var result = new ArrayList<Stored>();
        try (var c =
                db.rawQuery(
                        "SELECT kind,earned_at,recognized_at FROM training_milestones ORDER BY"
                                + " earned_at DESC,kind",
                        null)) {
            while (c.moveToNext()) {
                var proof = new ArrayList<Proof>();
                // At most seven fixed definitions, each with at most fifty references; never raw
                // samples.
                try (var p =
                        db.rawQuery(
                                "SELECT"
                                    + " source_type,record_id,occurred_at,offset_seconds,distance_metres"
                                    + " FROM training_milestone_sources WHERE award_kind=? ORDER BY"
                                    + " position",
                                new String[] {c.getString(0)})) {
                    while (p.moveToNext())
                        proof.add(
                                new Proof(
                                        Source.valueOf(p.getString(0)),
                                        p.getString(1),
                                        Instant.parse(p.getString(2)),
                                        p.isNull(3) ? null : ZoneOffset.ofTotalSeconds(p.getInt(3)),
                                        p.isNull(4) ? null : p.getDouble(4)));
                }
                result.add(
                        new Stored(
                                new Award(
                                        Kind.valueOf(c.getString(0)),
                                        Instant.parse(c.getString(1)),
                                        proof),
                                Instant.parse(c.getString(2))));
            }
        }
        return Immutable.copyList(result);
    }

    private static void put(SQLiteDatabase db, Stored row) {
        var award = row.award();
        var v = values("kind", award.kind().name());
        v.put("earned_at", award.earnedAt().toString());
        v.put("recognized_at", row.recognizedAt().toString());
        if (db.insertOrThrow("training_milestones", null, v) < 0)
            throw new IllegalArgumentException("Milestone could not be retained.");
        int position = 0;
        for (var source : award.evidence()) {
            var proof = values("award_kind", award.kind().name());
            proof.put("position", position++);
            proof.put("source_type", source.source().name());
            proof.put("record_id", source.recordId());
            proof.put("occurred_at", source.at().toString());
            proof.put(
                    "offset_seconds",
                    source.offset() == null ? null : source.offset().getTotalSeconds());
            proof.put("distance_metres", source.distanceMetres());
            db.insertOrThrow("training_milestone_sources", null, proof);
        }
    }

    public static JSONObject exportData(SQLiteDatabase db) {
        var rows = new JSONArray();
        for (var stored : awards(db)) {
            var evidence = new JSONArray();
            for (var proof : stored.award().evidence())
                evidence.put(
                        object(
                                "source",
                                proof.source().name(),
                                "id",
                                proof.recordId(),
                                "at",
                                proof.at().toString(),
                                "offset",
                                proof.offset() == null ? null : proof.offset().getTotalSeconds(),
                                "distance",
                                proof.distanceMetres()));
            rows.put(
                    object(
                            "kind",
                            stored.award().kind().name(),
                            "earnedAt",
                            stored.award().earnedAt().toString(),
                            "recognizedAt",
                            stored.recognizedAt().toString(),
                            "evidence",
                            evidence));
        }
        return object("version", 1, "celebrations", celebrations(db), "awards", rows);
    }

    public static Snapshot validate(JSONObject json) {
        if (json == null) return new Snapshot(false, Immutable.list());
        fields(json, "version", "celebrations", "awards");
        if (integer(json, "version") != 1) throw invalid("unsupported achievements version");
        var rows = array(json, "awards", Kind.values().length);
        var result = new ArrayList<Stored>();
        for (int i = 0; i < rows.length(); i++) {
            var r = row(rows, i);
            fields(r, "kind", "earnedAt", "recognizedAt", "evidence");
            var proof = array(r, "evidence", 50);
            var sources = new ArrayList<Proof>();
            for (int j = 0; j < proof.length(); j++) {
                var p = row(proof, j);
                fields(p, "source", "id", "at", "offset", "distance");
                var offset = nullableInteger(p, "offset");
                sources.add(
                        new Proof(
                                choice(p, "source", Source.class),
                                text(p, "id", 256),
                                instant(p, "at"),
                                offset == null ? null : ZoneOffset.ofTotalSeconds(offset),
                                nullableNumber(p, "distance")));
            }
            result.add(
                    new Stored(
                            new Award(
                                    choice(r, "kind", Kind.class), instant(r, "earnedAt"), sources),
                            instant(r, "recognizedAt")));
        }
        return new Snapshot(bool(json, "celebrations"), result);
    }

    public static void restore(SQLiteDatabase db, Snapshot snapshot) {
        db.delete("training_milestone_sources", null, null);
        db.delete("training_milestones", null, null);
        db.delete("training_milestone_preferences", null, null);
        putPreferences(db, snapshot.celebrations());
        for (var row : snapshot.awards()) put(db, row);
    }
}
