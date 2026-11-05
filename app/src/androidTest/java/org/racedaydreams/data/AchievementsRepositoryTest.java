package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.achievements.TrainingMilestones.*;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.portability.BackupManager;

import java.time.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class AchievementsRepositoryTest extends AndroidTestCase {
    SqliteAchievementsRepository repository;
    final Instant now = Instant.parse("2026-11-05T12:00:00Z");

    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        repository = new SqliteAchievementsRepository(getContext());
    }

    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    Award first() {
        return new Award(
                Kind.FIRST_GYM,
                now,
                Immutable.list(
                        new Proof(Source.GYM, "recorded-workout", now, ZoneOffset.UTC, null)));
    }

    public void testRecognitionIsDurableIdempotentAndNeverRedatesExistingEvidence() {
        assertEquals(1, repository.recognize(Immutable.list(first()), now));
        assertEquals(
                0,
                new SqliteAchievementsRepository(getContext())
                        .recognize(Immutable.list(first()), now.plusSeconds(10)));
        assertEquals(now, repository.awards().get(0).recognizedAt());
        assertFalse(repository.celebrations());
        repository.celebrations(true);
        assertTrue(new SqliteAchievementsRepository(getContext()).celebrations());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
    }

    public void testJsonRootBackupRoundTripAndOldTenDefaults() throws Exception {
        repository.recognize(Immutable.list(first()), now);
        repository.celebrations(true);
        var before = repository.awards();
        var backup = new BackupManager(getContext(), new SqliteBackupRepository(getContext()));
        var output = new java.io.ByteArrayOutputStream();
        backup.exportTo(output);
        var envelope = new JSONObject(output.toString("UTF-8"));
        assertEquals(11, envelope.getInt("version"));
        new SqliteUserPreferencesRepository(getContext()).clear();
        backup.restoreFrom(new java.io.ByteArrayInputStream(output.toByteArray()));
        assertEquals(before, repository.awards());
        assertTrue(repository.celebrations());
        assertEquals(0, repository.recognize(Immutable.list(first()), now));
        envelope.put("version", 10);
        envelope.getJSONObject("data").remove("achievements");
        backup.restoreFrom(
                new java.io.ByteArrayInputStream(
                        envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(repository.awards().isEmpty());
        assertFalse(repository.celebrations());
    }

    public void testMalformedAndFutureAwardDataRejectBeforeReplacement() throws Exception {
        repository.recognize(Immutable.list(first()), now);
        var backup = new SqliteBackupRepository(getContext());
        var data = backup.exportUserData();
        for (int mode = 0; mode < 4; mode++) {
            var bad = copy(data);
            var history = bad.getJSONObject("achievements");
            if (mode == 0) history.put("version", 99);
            if (mode == 1)
                history.getJSONArray("awards").put(history.getJSONArray("awards").getJSONObject(0));
            if (mode == 2) history.getJSONArray("awards").getJSONObject(0).put("kind", "FIVE_GYM");
            if (mode == 3)
                history.getJSONArray("awards")
                        .getJSONObject(0)
                        .getJSONArray("evidence")
                        .getJSONObject(0)
                        .put("source", "RUN");
            try {
                backup.restoreUserData(bad, false);
                fail("Invalid award accepted");
            } catch (IllegalArgumentException expected) {
            }
            assertEquals(1, repository.awards().size());
            assertEquals(Kind.FIRST_GYM, repository.awards().get(0).award().kind());
        }
    }

    public void testRootElevenRequiresAchievementSectionBeforeReplacement() throws Exception {
        repository.recognize(Immutable.list(first()), now);
        repository.celebrations(true);
        var manager = new BackupManager(getContext(), new SqliteBackupRepository(getContext()));
        var bytes = new java.io.ByteArrayOutputStream();
        manager.exportTo(bytes);
        for (int mode = 0; mode < 3; mode++) {
            var envelope = new JSONObject(bytes.toString("UTF-8"));
            var data = envelope.getJSONObject("data");
            if (mode == 0) data.remove("achievements");
            if (mode == 1) data.put("achievements", JSONObject.NULL);
            if (mode == 2) data.getJSONObject("achievements").put("version", 99);
            try {
                manager.restoreFrom(
                        new java.io.ByteArrayInputStream(
                                envelope.toString()
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                fail("Invalid current section accepted");
            } catch (java.io.IOException expected) {
            }
            assertEquals(1, repository.awards().size());
            assertTrue(repository.celebrations());
        }
    }

    public void testLaterRestoreFailureRollsBackOtherDataAndPreviousAwards() throws Exception {
        repository.recognize(Immutable.list(first()), now);
        var backup = new SqliteBackupRepository(getContext());
        var data = backup.exportUserData();
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL("INSERT INTO settings VALUES('keep','original')");
        db.execSQL(
                "CREATE TRIGGER fail_awards BEFORE INSERT ON training_milestone_sources BEGIN"
                        + " SELECT RAISE(ABORT,'injected'); END");
        try {
            backup.restoreUserData(data, false);
            fail("Injected failure did not occur");
        } catch (android.database.sqlite.SQLiteException expected) {
        }
        assertEquals("original", new SqliteSettingsRepository(getContext()).getSetting("keep"));
        assertEquals(1, repository.awards().size());
    }

    public void testSixteenUpgradeRetainsTimerAndHistoryBoundary() {
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO settings VALUES('previous','yes')");
        GymSchemaFixture.removeVersionSeventeenTables(db);
        db.setVersion(16);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(17, db.getVersion());
        assertEquals("yes", new SqliteSettingsRepository(getContext()).getSetting("previous"));
        assertTrue(repository.awards().isEmpty());
        assertFalse(repository.celebrations());
    }

    private static JSONObject copy(JSONObject value) {
        try {
            return new JSONObject(value.toString());
        } catch (JSONException error) {
            throw new AssertionError(error);
        }
    }
}
