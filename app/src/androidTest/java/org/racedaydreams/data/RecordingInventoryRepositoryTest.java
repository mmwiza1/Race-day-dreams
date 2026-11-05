package org.racedaydreams.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import org.racedaydreams.domain.recording.RunMetricsEngine;
import org.racedaydreams.domain.storage.DetailRetention;

import java.time.Instant;

@SuppressWarnings("deprecation")
public final class RecordingInventoryRepositoryTest extends AndroidTestCase {
    private static final Instant OLD = Instant.parse("2020-01-01T00:00:00Z");
    private SQLiteDatabase db;
    private SqliteRecordingInventoryRepository repository;

    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        db = ProfileDatabase.get(getContext()).getWritableDatabase();
        repository = new SqliteRecordingInventoryRepository(getContext());
    }

    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testRealFinishedEvidenceAndQualifiedResultsRemainByteIdentical() {
        var engine = new RunMetricsEngine(0, OLD);
        for (int i = 0; i <= 2000; i++)
            engine.accept(
                    new RunMetricsEngine.Fix(
                            OLD.plusSeconds(i), i * 1000L, 0, i * .00003, 3.0, null, null, 3.0));
        var store = new RecordingStore(getContext());
        store.start("inventory-real", OLD, 0, null, null);
        store.appendBatch(
                "inventory-real", engine.points(), engine.events(), 0, 0, 2000000, "ACTIVE", null);
        String activityId = store.finish(store.unfinished(), null, "rdd", false);
        String payload = scalar("SELECT payload FROM activities WHERE id='" + activityId + "'");
        String point = scalar("SELECT payload FROM recording_points WHERE sequence=2000");
        long bestEfforts = count("best_efforts");
        assertTrue("The fixture must contain qualified PB evidence", bestEfforts > 0);
        String effort = scalar("SELECT payload FROM best_efforts ORDER BY id LIMIT 1");
        var inventory = repository.inspect();
        assertFalse(inventory.unfinishedRecording());
        assertEquals(1, inventory.recordings().size());
        var row = inventory.recordings().get(0);
        assertEquals(activityId, row.activityId());
        assertEquals("inventory-real", row.sourceRecordId());
        assertEquals(2001L, row.points());
        assertTrue(row.finished());
        assertEquals(0L, row.denseSamples().longValue());
        assertEquals(
                1,
                DetailRetention.preview(
                                DetailRetention.Policy.LAST_24_MONTHS,
                                inventory.recordings(),
                                Instant.parse("2026-11-05T00:00:00Z"),
                                "test-epoch")
                        .recordings()
                        .size());
        assertEquals(
                payload, scalar("SELECT payload FROM activities WHERE id='" + activityId + "'"));
        assertEquals(point, scalar("SELECT payload FROM recording_points WHERE sequence=2000"));
        assertEquals(bestEfforts, count("best_efforts"));
        assertEquals(effort, scalar("SELECT payload FROM best_efforts ORDER BY id LIMIT 1"));
        assertEquals(engine.points(), store.points("inventory-real"));
        assertEquals(inventory.recordings(), repository.inspect().recordings());
    }

    public void testAggregatesThousandsOfRowsWithoutDecodingOrHydratingPayloads() {
        // Deliberately opaque oversized payload: materialising this cell would exceed normal
        // CursorWindow limits. The inventory must only ask SQLite for its byte length.
        String oversized = new String(new char[3 * 1024 * 1024]).replace('\0', 'x');
        db.beginTransaction();
        try {
            for (int i = 0; i < 1000; i++) {
                session("s" + i, "FINISHED", "a" + i);
                activity("a" + i, "s" + i, "rdd", "summary");
                for (int j = 0; j < 5; j++) detail("recording_points", "s" + i, j, "opaque");
            }
            detail("recording_events", "s0", 0, oversized);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        var result = repository.inspect();
        assertEquals(1000, result.recordings().size());
        assertEquals(5000L, result.usage().pointRows());
        assertEquals(1L, result.usage().eventRows());
        assertEquals(5000L * 6 + oversized.length(), result.usage().detailPayloadBytes());
        assertEquals(1000L, result.usage().activityRows());
        assertEquals(7000L, result.usage().activityPayloadBytes());
        assertTrue(result.usage().databaseFileBytes() > 0);
        assertEquals(5000L, count("recording_points"));
        assertEquals(1L, count("recording_events"));
    }

    public void testUnknownOrUnownedDetailIsNotEligibleAndPausedSessionBlocksCleanup() {
        session("foreign", "FINISHED", "foreign-activity");
        activity("foreign-activity", "foreign", "health-connect", "summary");
        session("orphan", "FINISHED", "missing-activity");
        session("wrong-source", "FINISHED", "wrong-activity");
        activity("wrong-activity", "another-source", "rdd", "summary");
        session("paused", "PAUSED", null);
        for (String id : new String[] {"foreign", "orphan", "wrong-source", "paused"})
            detail("recording_points", id, 0, "opaque");
        var result = repository.inspect();
        assertTrue(result.unfinishedRecording());
        assertEquals(4, result.recordings().size());
        assertTrue(
                result.recordings().stream()
                        .filter(r -> r.sessionId().equals("orphan"))
                        .allMatch(r -> r.endedAt() == null && r.denseSamples() == null));
        assertTrue(
                DetailRetention.preview(
                                DetailRetention.Policy.LAST_24_MONTHS,
                                result.recordings(),
                                Instant.parse("2026-11-05T00:00:00Z"),
                                "test-epoch")
                        .recordings()
                        .isEmpty());
        assertEquals(4L, result.usage().pointRows());
    }

    public void testCutoffUsesConservativeSummaryUpperBoundAndEmptyUsageIsZero() {
        var empty = repository.inspect();
        assertTrue(empty.recordings().isEmpty());
        assertEquals(0L, empty.usage().detailPayloadBytes());
        assertEquals(0L, empty.usage().activityRows());
        session("boundary", "FINISHED", "at-boundary");
        activity("at-boundary", "boundary", "gpx", "summary");
        detail("recording_points", "boundary", 0, "é");
        var result = repository.inspect();
        assertEquals(2L, result.usage().detailPayloadBytes());
        assertEquals(OLD.plusMillis(1), result.recordings().get(0).endedAt());
        assertTrue(
                DetailRetention.preview(
                                DetailRetention.Policy.LAST_24_MONTHS,
                                result.recordings(),
                                OLD.plusMillis(1)
                                        .atOffset(java.time.ZoneOffset.UTC)
                                        .plusYears(2)
                                        .toInstant(),
                                "test-epoch")
                        .recordings()
                        .isEmpty());
    }

    private void session(String id, String state, String activity) {
        var v = values("id", id);
        v.put("state", state);
        v.put("activity_id", activity);
        v.put("started_at", OLD.minusSeconds(60).toString());
        v.put("started_mono", 0);
        v.put("last_mono", 60000);
        db.insertOrThrow("recording_sessions", null, v);
    }

    private void activity(String id, String source, String provider, String payload) {
        var v = values("id", id);
        v.put("provider", provider);
        v.put("record_id", source);
        v.put("start_millis", OLD.minusSeconds(60).toEpochMilli());
        v.put("end_millis", OLD.toEpochMilli());
        v.put("payload", payload);
        db.insertOrThrow("activities", null, v);
    }

    private void detail(String table, String session, int sequence, String payload) {
        var v = values("session_id", session);
        v.put("sequence", sequence);
        v.put("payload", payload);
        db.insertOrThrow(table, null, v);
    }

    private static ContentValues values(String key, String value) {
        var v = new ContentValues();
        v.put(key, value);
        return v;
    }

    private String scalar(String sql) {
        try (var c = db.rawQuery(sql, null)) {
            assertTrue(c.moveToFirst());
            return c.getString(0);
        }
    }

    private long count(String table) {
        try (var c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }
}
