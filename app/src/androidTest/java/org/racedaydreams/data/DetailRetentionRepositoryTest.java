package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.recording.RunMetricsEngine;
import org.racedaydreams.domain.storage.*;
import org.racedaydreams.portability.BackupManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.zip.*;

@SuppressWarnings("deprecation")
public final class DetailRetentionRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");
    private static final Instant OLD = Instant.parse("2020-01-01T00:00:00Z");
    private SqliteDetailRetentionRepository repo;
    private SQLiteDatabase db;

    protected void setUp() throws Exception {
        super.setUp();
        reset();
    }

    private void reset() {
        new SqliteUserPreferencesRepository(getContext()).clear();
        repo = new SqliteDetailRetentionRepository(getContext());
        db = ProfileDatabase.get(getContext()).getWritableDatabase();
    }

    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private String record(String id) {
        var e = new RunMetricsEngine(0, OLD);
        for (int i = 0; i <= 2000; i++)
            e.accept(
                    new RunMetricsEngine.Fix(
                            OLD.plusSeconds(i), i * 1000L, 0, i * .00003, 3.0, null, null, 3.0));
        var store = new RecordingStore(getContext());
        store.start(id, OLD, 0, null, null);
        store.appendBatch(id, e.points(), e.events(), 0, 0, 2000000, "ACTIVE", null);
        return store.finish(store.unfinished(), null, "rdd", false);
    }

    private DetailRetention.Approval approval() {
        repo.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, NOW);
        return repo.approve(repo.preview(DetailRetention.Policy.LAST_24_MONTHS, NOW), NOW);
    }

    public void testFreshDefaultAndUpgradeRequireDifferentConsent() {
        assertEquals(DetailRetention.Choice.NEW_INSTALL_DEFAULT, repo.preferences().choice());
        assertEquals(DetailRetention.Policy.LAST_24_MONTHS, repo.preferences().policy());
        record("old");
        GymSchemaFixture.removeVersionNineteenTables(db);
        db.setVersion(18);
        ProfileDatabase.get(getContext()).close();
        db = ProfileDatabase.get(getContext()).getWritableDatabase();
        assertEquals(19, db.getVersion());
        assertEquals(DetailRetention.Preferences.existing(), repo.preferences());
        var preview = repo.preview(DetailRetention.Policy.LAST_24_MONTHS, NOW);
        rejects(() -> repo.approve(preview, NOW));
        assertTrue(new RecordingStore(getContext()).points("old").size() > 0);
        for (var policy : DetailRetention.Policy.values()) {
            repo.acknowledge(policy, NOW);
            assertEquals(
                    policy,
                    new SqliteDetailRetentionRepository(getContext()).preferences().policy());
            assertEquals(DetailRetention.Choice.ACKNOWLEDGED, repo.preferences().choice());
        }
    }

    public void testOnlyRawRowsRemovedAndCommittedRetryIsIdempotent() {
        var id = record("old");
        String summary = scalar("SELECT payload FROM activities WHERE id='" + id + "'");
        String pb = scalar("SELECT payload FROM best_efforts ORDER BY id LIMIT 1");
        long efforts = count("best_efforts");
        assertTrue(efforts > 0);
        var approval = approval();
        assertTrue(
                repo.cleanOne(approval, approval.preview().recordings().get(0).sessionId(), NOW));
        assertFalse(
                repo.cleanOne(approval, approval.preview().recordings().get(0).sessionId(), NOW));
        assertFalse(
                repo.cleanOne(approval, approval.preview().recordings().get(0).sessionId(), NOW));
        assertEquals(0L, count("recording_points"));
        assertEquals(0L, count("recording_events"));
        assertEquals(1L, count("recording_sessions"));
        assertEquals(1L, count("activities"));
        assertEquals(summary, scalar("SELECT payload FROM activities WHERE id='" + id + "'"));
        assertEquals(pb, scalar("SELECT payload FROM best_efforts ORDER BY id LIMIT 1"));
        assertEquals(efforts, count("best_efforts"));
        assertEquals(2001L, repo.removal("old").points());
        assertEquals(id, repo.removal("old").activityId());
        assertTrue(new FeatureStore(getContext()).activityDetail(id).samples().isEmpty());
        var gpx = new ByteArrayOutputStream();
        rejects(() -> new org.racedaydreams.recording.GpxFiles(getContext()).exportTo("old", gpx));
        assertEquals(0, gpx.size());
    }

    public void testDeleteFailureRollsBackReceiptPointsEventsAndSummaries() {
        record("rollback");
        var approval = approval();
        long points = count("recording_points"), events = count("recording_events");
        assertTrue(events > 0);
        db.execSQL(
                "CREATE TRIGGER reject_raw_cleanup BEFORE DELETE ON recording_events BEGIN SELECT"
                        + " RAISE(ABORT,'injected'); END");
        try {
            rejects(
                    () ->
                            repo.cleanOne(
                                    approval,
                                    approval.preview().recordings().get(0).sessionId(),
                                    NOW));
        } finally {
            db.execSQL("DROP TRIGGER reject_raw_cleanup");
        }
        assertEquals(points, count("recording_points"));
        assertEquals(events, count("recording_events"));
        assertNull(repo.removal("rollback"));
        assertTrue(
                repo.cleanOne(approval, approval.preview().recordings().get(0).sessionId(), NOW));
    }

    public void testEveryUnfinishedStateBlocksAndNoAutomaticCleanupOccurs() {
        record("finished");
        var approval = approval();
        long points = count("recording_points");
        for (String state : new String[] {"ACTIVE", "PAUSED", "RECOVERABLE"}) {
            new RecordingStore(getContext()).start("unfinished", NOW, 0, null, null);
            db.execSQL(
                    "UPDATE recording_sessions SET state=? WHERE id='unfinished'",
                    new Object[] {state});
            rejects(
                    () ->
                            repo.cleanOne(
                                    approval,
                                    approval.preview().recordings().get(0).sessionId(),
                                    NOW));
            assertEquals(points, count("recording_points"));
            assertNull(repo.removal("finished"));
            db.delete("recording_sessions", "id='unfinished'", null);
        }
        repo.acknowledge(DetailRetention.Policy.KEEP_FOREVER, NOW);
        rejects(
                () ->
                        repo.cleanOne(
                                approval, approval.preview().recordings().get(0).sessionId(), NOW));
        assertEquals(points, count("recording_points"));
    }

    public void testPreviewExpiryChoiceAndReplacementInvalidateApproval() throws Exception {
        record("identity");
        var approval = approval();
        rejects(
                () ->
                        repo.cleanOne(
                                approval,
                                approval.preview().recordings().get(0).sessionId(),
                                NOW.plus(Duration.ofHours(24)).plusNanos(1)));
        repo.acknowledge(DetailRetention.Policy.LAST_24_MONTHS, NOW);
        rejects(
                () ->
                        repo.cleanOne(
                                approval, approval.preview().recordings().get(0).sessionId(), NOW));
        var fresh = repo.approve(repo.preview(DetailRetention.Policy.LAST_24_MONTHS, NOW), NOW);
        var backup = appBackup();
        manager().restoreFrom(new ByteArrayInputStream(backup));
        rejects(() -> repo.cleanOne(fresh, fresh.preview().recordings().get(0).sessionId(), NOW));
        assertNull(repo.removal("identity"));
        assertEquals(2001L, count("recording_points"));
    }

    public void testRawArchiveOneTwoAndThreeRestoreExactRemovedEvidenceWithoutDuplicateActuals()
            throws Exception {
        var id = record("restore");
        byte[] full = rawBackup();
        String summary = scalar("SELECT payload FROM activities WHERE id='" + id + "'");
        assertTrue(
                repo.cleanOne(
                        approval(),
                        new RecordingStore(getContext()).finished().get(0).getFirst(),
                        NOW));
        byte[] pruned = rawBackup();
        assertNotNull(repo.removal("restore"));
        for (int version : new int[] {1, 2}) {
            new RecordingArchive(getContext())
                    .restoreFrom(new ByteArrayInputStream(asVersion(full, version, false)));
            assertNull(repo.removal("restore"));
            assertEquals(2001L, count("recording_points"));
            if (version == 1) assertTrue(repo.cleanOne(approval(), "restore", NOW));
        }
        assertEquals(summary, scalar("SELECT payload FROM activities WHERE id='" + id + "'"));
        new RecordingArchive(getContext()).restoreFrom(new ByteArrayInputStream(full));
        assertEquals(2001L, count("recording_points"));
        assertEquals(1L, count("activities"));
        // An older pruned archive must not remove currently available exact detail.
        new RecordingArchive(getContext()).restoreFrom(new ByteArrayInputStream(pruned));
        assertEquals(2001L, count("recording_points"));
        assertNull(repo.removal("restore"));
    }

    public void testConflictingArchivedCoordinatesRollBackAndRetainRemovalProof() throws Exception {
        record("conflict");
        byte[] original = rawBackup();
        assertTrue(
                repo.cleanOne(
                        approval(),
                        new RecordingStore(getContext()).finished().get(0).getFirst(),
                        NOW));
        var receipt = repo.removal("conflict");
        rejects(
                () ->
                        new RecordingArchive(getContext())
                                .restoreFrom(
                                        new ByteArrayInputStream(asVersion(original, 2, true))));
        assertEquals(0L, count("recording_points"));
        assertEquals(receipt, repo.removal("conflict"));
        assertEquals(1L, count("activities"));
    }

    public void testCurrentCompleteBackupAndOlderJsonPreserveHistoryAndSafePolicy()
            throws Exception {
        record("complete");
        repo.cleanOne(
                approval(), new RecordingStore(getContext()).finished().get(0).getFirst(), NOW);
        var receipt = repo.removal("complete");
        var out = new ByteArrayOutputStream();
        new CompleteBackup(getContext()).exportTo(out);
        reset();
        new CompleteBackup(getContext()).restoreFrom(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(receipt, repo.removal("complete"));
        assertEquals(0L, count("recording_points"));
        assertEquals(1L, count("activities"));
        assertTrue(count("preserved_best_efforts") > 0);
        assertEquals(DetailRetention.Policy.LAST_24_MONTHS, repo.preferences().policy());
        var json = new JSONObject(new String(appBackup(), StandardCharsets.UTF_8));
        assertEquals(13, json.getInt("version"));
        json.put("version", 12);
        json.getJSONObject("data").remove("detailRetention");
        manager()
                .restoreFrom(
                        new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(DetailRetention.Preferences.existing(), repo.preferences());
        assertEquals(1L, count("activities"));
        assertTrue(count("preserved_best_efforts") > 0);
    }

    public void testCorruptFutureAndFailedRetentionRestoreNeverReplaceCurrentState()
            throws Exception {
        record("safe");
        repo.cleanOne(
                approval(), new RecordingStore(getContext()).finished().get(0).getFirst(), NOW);
        byte[] good = appBackup();
        var before = repo.preferences();
        var receipt = repo.removal("safe");
        for (int kind = 0; kind < 3; kind++) {
            var invalid = new JSONObject(new String(good, StandardCharsets.UTF_8));
            if (kind == 0) invalid.getJSONObject("data").remove("detailRetention");
            if (kind == 1)
                invalid.getJSONObject("data").getJSONObject("detailRetention").put("version", 2);
            if (kind == 2)
                invalid.getJSONObject("data")
                        .getJSONObject("detailRetention")
                        .getJSONArray("removals")
                        .getJSONObject(0)
                        .put("pointsHash", "not-a-hash");
            rejects(
                    () ->
                            manager()
                                    .restoreFrom(
                                            new ByteArrayInputStream(
                                                    invalid.toString()
                                                            .getBytes(StandardCharsets.UTF_8))));
            assertEquals(before, repo.preferences());
            assertEquals(receipt, repo.removal("safe"));
        }
        db.execSQL(
                "CREATE TRIGGER reject_retention_restore BEFORE INSERT ON recording_detail_removals"
                        + " BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            rejects(() -> manager().restoreFrom(new ByteArrayInputStream(good)));
        } finally {
            db.execSQL("DROP TRIGGER reject_retention_restore");
        }
        assertEquals(before, repo.preferences());
        assertEquals(receipt, repo.removal("safe"));
        assertEquals(1L, count("activities"));
    }

    public void testApprovedBatchesResumeIndependentlyWithoutWideningSelection() {
        record("first");
        record("second");
        var approval = approval();
        assertEquals(2, approval.preview().recordings().size());
        assertTrue(repo.cleanOne(approval, "first", NOW));
        assertEquals(2001, new RecordingStore(getContext()).points("second").size());
        rejects(() -> repo.cleanOne(approval, "unapproved", NOW));
        assertFalse(
                new SqliteDetailRetentionRepository(getContext()).cleanOne(approval, "first", NOW));
        assertTrue(
                new SqliteDetailRetentionRepository(getContext())
                        .cleanOne(approval, "second", NOW));
        assertEquals(2L, count("activities"));
        assertEquals(0L, count("recording_points"));
        assertTrue(count("best_efforts") >= 10);
    }

    public void testCompleteArchiveKeepsMoreThanThousandSummariesWithoutTruncation()
            throws Exception {
        db.beginTransaction();
        try {
            for (int i = 0; i < 1100; i++) {
                var id = "long-" + i;
                var start = OLD.plusSeconds(i * 1200L);
                var activity =
                        new org.racedaydreams.domain.activity.ActivityRecord(
                                "rdd:" + id,
                                new org.racedaydreams.domain.activity.SourceIdentity(
                                        "rdd", id, "org.racedaydreams"),
                                start,
                                start.plusSeconds(1000),
                                ZoneOffset.UTC,
                                ZoneOffset.UTC,
                                2000.0,
                                Duration.ofSeconds(1000),
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                java.util.Collections.emptyList(),
                                java.util.Collections.emptyList(),
                                null,
                                null);
                SqliteActivityRepository.putActivity(db, activity, true, false);
                db.execSQL(
                        "INSERT INTO"
                            + " recording_sessions(id,state,started_at,started_mono,last_mono,activity_id)"
                            + " VALUES(?,'FINISHED',?,0,1000000,?)",
                        new Object[] {id, start.toString(), activity.id()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        var out = new ByteArrayOutputStream();
        new CompleteBackup(getContext()).exportTo(out);
        reset();
        new CompleteBackup(getContext()).restoreFrom(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(1100L, count("activities"));
        assertEquals(1100L, count("recording_sessions"));
        assertEquals(0L, count("recording_points"));
    }

    private BackupManager manager() {
        return new BackupManager(getContext(), new FeatureStore(getContext()));
    }

    private byte[] appBackup() throws Exception {
        var out = new ByteArrayOutputStream();
        manager().exportTo(out);
        return out.toByteArray();
    }

    private byte[] rawBackup() throws Exception {
        var out = new ByteArrayOutputStream();
        new RecordingArchive(getContext()).exportTo(out);
        return out.toByteArray();
    }

    private static byte[] asVersion(byte[] archive, int version, boolean alter) throws Exception {
        JSONObject manifest = null;
        byte[] data = null;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                var bytes = new ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = zip.read(b)) != -1) bytes.write(b, 0, n);
                if (entry.getName().equals("manifest.json"))
                    manifest = new JSONObject(bytes.toString("UTF-8"));
                else data = bytes.toByteArray();
            }
        }
        if (alter) {
            var text = new String(data, StandardCharsets.UTF_8);
            var rows = text.split("\n");
            for (int i = 0; i < rows.length; i++) {
                var row = new JSONObject(rows[i]);
                if (row.getString("kind").equals("point")) {
                    row.getJSONObject("payload")
                            .put("lon", row.getJSONObject("payload").getDouble("lon") + .000001);
                    rows[i] = row.toString();
                    break;
                }
            }
            data = (String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8);
        }
        var hash = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data))
            hash.append(String.format(Locale.ROOT, "%02x", b & 255));
        manifest.put("version", version).put("bytes", data.length).put("sha256", hash.toString());
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("manifest.json"));
            zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("recordings.jsonl"));
            zip.write(data);
            zip.closeEntry();
        }
        return out.toByteArray();
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

    private interface Work {
        void run() throws Exception;
    }

    private static void rejects(Work work) {
        try {
            work.run();
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("Expected safe rejection");
    }
}
