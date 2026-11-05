package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.portability.BackupManager;

import java.time.*;
import java.util.*;

/** Actual SQLite/JSON/migration/rollback and interrupted monotonic advisory state. */
@SuppressWarnings("deprecation")
public final class GymToolsRepositoryTest extends AndroidTestCase {
    private SqliteGymToolsRepository tools;
    private SqliteGymExecutionRepository executions;
    private final Instant now = Instant.parse("2026-11-05T12:00:00Z");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        tools = new SqliteGymToolsRepository(getContext());
        executions = new SqliteGymExecutionRepository(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private GymExecution start() {
        var e =
                GymExecutionEngine.start(
                                "workout",
                                GymExecution.Origin.FREE,
                                null,
                                null,
                                ZoneOffset.UTC,
                                new GymExecution.Tick("process", 1000, now))
                        .snapshot();
        executions.start(e);
        return e;
    }

    public void testTimerSurvivesRepositoryRecreationWithoutChangingExecution() {
        var original = start();
        var t =
                tools.change(
                        "workout",
                        old ->
                                GymRestTimer.start(
                                        "workout",
                                        "manual",
                                        180,
                                        new GymRestTimer.Clock("boot", 1000)));
        assertEquals(t, new SqliteGymToolsRepository(getContext()).timer("workout"));
        assertEquals(120L, t.observe(new GymRestTimer.Clock("boot", 61000)).secondsRemaining());
        assertEquals(
                GymRestTimer.Phase.INTERRUPTED,
                t.observe(new GymRestTimer.Clock("reboot", 1000)).phase());
        assertEquals(original, executions.load("workout").execution());
        assertEquals(1L, executions.load("workout").revision());
    }

    public void testAlarmCompletionIsIdempotentAndCannotCompleteAnotherStart() {
        start();
        tools.change(
                "workout",
                old ->
                        GymRestTimer.start(
                                "workout", "first", 30, new GymRestTimer.Clock("boot", 1000)));
        assertFalse(tools.completeIfDue("workout", "first", new GymRestTimer.Clock("boot", 2000)));
        assertFalse(tools.completeIfDue("workout", "other", new GymRestTimer.Clock("boot", 50000)));
        assertFalse(
                tools.completeIfDue("workout", "first", new GymRestTimer.Clock("newboot", 50000)));
        assertTrue(tools.completeIfDue("workout", "first", new GymRestTimer.Clock("boot", 50000)));
        assertFalse(tools.completeIfDue("workout", "first", new GymRestTimer.Clock("boot", 50001)));
    }

    public void testPreferencesAndTimerBackupRoundTripAndCorruptionRejectBeforeReplacement()
            throws Exception {
        var original = start();
        var p = new GymRestTimer.Preferences(180, false, true, true, false);
        tools.preferences(p);
        var t =
                tools.change(
                        "workout",
                        old ->
                                GymRestTimer.start(
                                        "workout",
                                        "manual",
                                        180,
                                        new GymRestTimer.Clock("boot", 1000)));
        var store = new SqliteBackupRepository(getContext());
        var data = store.exportUserData();
        store.validateSnapshot(data);
        var bad = new JSONObject(data.toString());
        bad.getJSONObject("gymTools")
                .getJSONArray("timers")
                .getJSONObject(0)
                .put("execution", "missing");
        try {
            store.validateSnapshot(bad);
            fail("Orphan timer graph validated");
        } catch (IllegalArgumentException expected) {
        }
        try {
            store.restoreUserData(bad, false);
            fail("Orphan timer accepted");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(t, tools.timer("workout"));
        assertEquals(p, tools.preferences());
        new SqliteUserPreferencesRepository(getContext()).clear();
        tools = new SqliteGymToolsRepository(getContext());
        executions = new SqliteGymExecutionRepository(getContext());
        store = new SqliteBackupRepository(getContext());
        store.restoreUserData(data, false);
        assertEquals(t, tools.timer("workout"));
        assertEquals(p, tools.preferences());
        assertEquals(original, executions.load("workout").execution());
    }

    public void testPreviousNineRestoresWithDefaultToolsAndTenRequiresTools() throws Exception {
        var store = new SqliteBackupRepository(getContext());
        var manager = new BackupManager(getContext(), store);
        var out = new java.io.ByteArrayOutputStream();
        manager.exportTo(out);
        var envelope = new JSONObject(out.toString("UTF-8"));
        assertEquals(12, envelope.getInt("version"));
        tools.preferences(new GymRestTimer.Preferences(300, false, false, false, false));
        envelope.put("version", 10); // Retain the previous envelope's required gym-tools contract.
        envelope.getJSONObject("data").remove("gymTools");
        try {
            manager.restoreFrom(
                    new java.io.ByteArrayInputStream(
                            envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            fail("Missing tools accepted");
        } catch (java.io.IOException expected) {
        }
        assertEquals(300, tools.preferences().defaultSeconds());
        envelope.put("version", 9);
        manager.restoreFrom(
                new java.io.ByteArrayInputStream(
                        envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(GymRestTimer.Preferences.defaults(), tools.preferences());
    }

    public void testSchemaFifteenUpgradePreservesExistingHistoryAndChoices() {
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO settings VALUES('preserved','yes')");
        db.execSQL(
                "INSERT INTO strength_sessions VALUES('history','2026-11-04','unchanged actual')");
        GymSchemaFixture.removeVersionSixteenTables(db);
        db.setVersion(15);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(18, db.getVersion());
        assertEquals("yes", new SqliteSettingsRepository(getContext()).getSetting("preserved"));
        try (var c =
                db.rawQuery("SELECT payload FROM strength_sessions WHERE id='history'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("unchanged actual", c.getString(0));
        }
        assertEquals(GymRestTimer.Preferences.defaults(), tools.preferences());
    }

    public void testTransactionalPreferenceRestoreFailureRollsBackOtherTables() {
        var store = new SqliteBackupRepository(getContext());
        var before = store.exportUserData();
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL("INSERT INTO settings VALUES('keep','value')");
        db.execSQL(
                "CREATE TRIGGER fail_rest BEFORE INSERT ON gym_rest_preferences BEGIN SELECT"
                        + " RAISE(ABORT,'injected'); END");
        try {
            try {
                store.restoreUserData(before, false);
                fail("Injected failure ignored");
            } catch (android.database.sqlite.SQLiteException expected) {
            }
            assertEquals("value", new SqliteSettingsRepository(getContext()).getSetting("keep"));
        } finally {
            db.execSQL("DROP TRIGGER fail_rest");
        }
    }
}
