package org.racedaydreams.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.storage.DetailRetention;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Read-only detail inventory. Never decodes a route, recomputes PBs or deletes evidence. */
public final class SqliteRecordingInventoryRepository extends SqliteStoreAccess {
    private final File databaseFile;

    public SqliteRecordingInventoryRepository(Context context) {
        super(context);
        databaseFile = context.getDatabasePath(ProfileDatabase.NAME);
    }

    /** Payload bytes exclude indexes/SQLite overhead; file bytes include database, WAL and SHM. */
    public record Usage(
            long pointRows,
            long eventRows,
            long detailPayloadBytes,
            long activityRows,
            long activityPayloadBytes,
            long databaseFileBytes) {}

    public record Inventory(
            List<DetailRetention.Recording> recordings, boolean unfinishedRecording, Usage usage) {
        public Inventory {
            recordings = Immutable.copyList(recordings);
        }
    }

    /** One transaction and four queries, independent of the number of recordings. */
    public Inventory inspect() {
        var db = read();
        db.beginTransaction();
        try {
            var result = new ArrayList<DetailRetention.Recording>();
            boolean unfinished = false;
            // Aggregate before joining: no point/event cross-product or per-session SQL query.
            // Only identifiers, counts and lengths cross the cursor boundary, never raw payloads.
            try (var c =
                    db.rawQuery(
                            "SELECT s.id,s.state,s.activity_id,a.provider,a.record_id,a.end_millis,"
                                + " COALESCE(p.n,0),COALESCE(e.n,0) FROM recording_sessions s LEFT"
                                + " JOIN activities a ON a.id=s.activity_id LEFT JOIN (SELECT"
                                + " session_id,COUNT(*) n FROM recording_points GROUP BY"
                                + " session_id) p ON p.session_id=s.id LEFT JOIN (SELECT"
                                + " session_id,COUNT(*) n FROM recording_events GROUP BY"
                                + " session_id) e ON e.session_id=s.id ORDER BY s.id",
                            null)) {
                while (c.moveToNext()) {
                    String state = c.getString(1);
                    if (state.equals("ACTIVE")
                            || state.equals("PAUSED")
                            || state.equals("RECOVERABLE")) unfinished = true;
                    String provider = c.isNull(3) ? "" : c.getString(3);
                    // end_millis is an indexed summary, rounded to milliseconds. Use an upper
                    // bound so a sub-millisecond timestamp cannot expire prematurely. Original
                    // activity timestamps, payloads and recorded totals remain untouched.
                    Instant end =
                            c.isNull(5) ? null : Instant.ofEpochMilli(c.getLong(5)).plusMillis(1);
                    result.add(
                            new DetailRetention.Recording(
                                    c.getString(0),
                                    c.isNull(2) ? null : c.getString(2),
                                    provider,
                                    c.isNull(4) ? "" : c.getString(4),
                                    end,
                                    state.equals("FINISHED"),
                                    c.getLong(6),
                                    c.getLong(7),
                                    // Native/GPX canonical payloads deliberately omit dense
                                    // samples;
                                    // RecordingStore owns them in recording_points. Other sources
                                    // unknown.
                                    provider.equals("rdd") || provider.equals("gpx") ? 0L : null));
                }
            }
            long[] points = counts(db, "recording_points"), events = counts(db, "recording_events");
            long[] activities = counts(db, "activities");
            var usage =
                    new Usage(
                            points[0],
                            events[0],
                            Math.addExact(points[1], events[1]),
                            activities[0],
                            activities[1],
                            fileBytes());
            db.setTransactionSuccessful();
            return new Inventory(result, unfinished, usage);
        } finally {
            db.endTransaction();
        }
    }

    private static long[] counts(SQLiteDatabase db, String table) {
        // Table names are private constants at call sites, never user input.
        try (var c =
                db.rawQuery(
                        "SELECT COUNT(*),COALESCE(SUM(length(CAST(payload AS BLOB))),0) FROM "
                                + table,
                        null)) {
            if (!c.moveToFirst()) throw new IllegalStateException("Storage inventory unavailable.");
            return new long[] {c.getLong(0), c.getLong(1)};
        }
    }

    private long fileBytes() {
        // Approximate observation: WAL allocation can change even during a read snapshot.
        long size = databaseFile.length();
        for (String suffix : new String[] {"-wal", "-shm"})
            size = Math.addExact(size, new File(databaseFile.getPath() + suffix).length());
        return size;
    }
}
