package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.portability.BackupManager;

import java.time.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class BodyMeasurementsRepositoryTest extends AndroidTestCase {
    private SqliteBodyMeasurementsRepository body;
    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        body = new SqliteBodyMeasurementsRepository(getContext());
    }

    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private static BodyHeight height(String id, String provider, Instant at) {
        return new BodyHeight(
                id,
                1.78,
                at,
                null,
                provider,
                id,
                provider.equals("manual") ? null : "provider.app",
                provider.equals("manual") ? null : NOW);
    }

    private static BodyWeight weight(String id, String provider, Instant at) {
        return new BodyWeight(id, 87, at, ZoneOffset.UTC, provider, id, null);
    }

    public void testManualSourcesUnitsAndExplicitBasisAreDurable() {
        var h = height("manual-h", "manual", NOW);
        var w = weight("manual-w", "manual", NOW);
        body.saveHeight(h);
        body.saveWeight(w);
        assertFalse(body.preferences().proteinEnabled());
        assertNull(body.preferences().basis());
        var selected =
                new BodyMeasurementsRepository.Preferences(
                        true, HeightUnit.INCHES, ProteinGuidance.Basis.from(w));
        body.preferences(selected);
        var reopened = new SqliteBodyMeasurementsRepository(getContext());
        assertEquals(h, reopened.heights(1, 0).get(0));
        assertEquals(w, reopened.weights().get(0));
        assertEquals(selected, reopened.preferences());
        assertEquals(w, new DevelopmentStore(getContext()).weights().get(0));
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
        assertTrue(new FeatureStore(getContext()).activities().isEmpty());
    }

    public void testPartialPermissionWindowsKeepManualAndDeniedMetrics() {
        var manual = height("manual", "manual", NOW.minusSeconds(20));
        var old = height("old", "health-connect", NOW.minusSeconds(30));
        body.saveHeight(manual);
        body.saveHeight(old);
        body.saveWeight(weight("old-weight", "health-connect", NOW.minusSeconds(30)));
        body.replaceImportedWindow(
                "health-connect",
                NOW.minusSeconds(60),
                NOW,
                null,
                Immutable.list(weight("new-weight", "health-connect", NOW.minusSeconds(10))));
        assertEquals(2, body.heights(20, 0).size());
        assertEquals("new-weight", body.weights().get(0).id());
        body.replaceImportedWindow(
                "health-connect", NOW.minusSeconds(60), NOW, Immutable.list(), null);
        assertEquals(Immutable.list(manual), body.heights(20, 0));
        assertEquals(1, body.weights().size());
        body.replaceImportedWindow(
                "health-connect", NOW.minusSeconds(60), NOW, Immutable.list(), null);
        assertEquals(Immutable.list(manual), body.heights(20, 0));
    }

    public void testExactTimePagingAndDuplicateSourceNeverDeleteAnObservation() {
        body.saveHeight(height("whole", "manual", NOW));
        body.saveHeight(height("fraction", "manual", NOW.plusNanos(1)));
        assertEquals("fraction", body.heights(1, 0).get(0).id());
        assertEquals("whole", body.heights(1, 1).get(0).id());
        rejects(
                () ->
                        body.saveHeight(
                                new BodyHeight(
                                        "alias", 1.8, NOW, null, "manual", "whole", null, null)));
        assertEquals(2, body.heights(20, 0).size());
        rejects(
                () ->
                        body.replaceImportedWindow(
                                "health-connect",
                                NOW.minusSeconds(10),
                                NOW,
                                Immutable.list(
                                        height("future", "health-connect", NOW.plusSeconds(1))),
                                null));
        assertEquals(2, body.heights(20, 0).size());
    }

    public void testBodyAndLegacyWeightImportRollbackTogether() {
        var old = height("old", "health-connect", NOW.minusSeconds(30));
        body.saveHeight(old);
        var w = weight("old-weight", "health-connect", NOW.minusSeconds(30));
        body.saveWeight(w);
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER reject_body_weight BEFORE INSERT ON coach_evidence WHEN"
                        + " NEW.kind='WEIGHT' BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            try {
                body.replaceImportedWindow(
                        "health-connect",
                        NOW.minusSeconds(60),
                        NOW,
                        Immutable.list(height("new", "health-connect", NOW.minusSeconds(10))),
                        Immutable.list(
                                weight("new-weight", "health-connect", NOW.minusSeconds(10))));
                fail("Expected injected failure");
            } catch (android.database.sqlite.SQLiteException expected) {
            }
            assertEquals(Immutable.list(old), body.heights(20, 0));
            assertEquals(Immutable.list(w), body.weights());
        } finally {
            db.execSQL("DROP TRIGGER reject_body_weight");
        }
    }

    public void testCurrentBackupRoundTripPreviousElevenAndFutureReject() throws Exception {
        body.saveHeight(height("manual", "manual", NOW));
        var w = weight("manual-weight", "manual", NOW);
        body.saveWeight(w);
        body.preferences(
                new BodyMeasurementsRepository.Preferences(
                        true, HeightUnit.INCHES, ProteinGuidance.Basis.from(w)));
        var before = body.heights(20, 0);
        var preferences = body.preferences();
        var manager = new BackupManager(getContext(), new SqliteBackupRepository(getContext()));
        var output = new java.io.ByteArrayOutputStream();
        manager.exportTo(output);
        var envelope = new JSONObject(output.toString("UTF-8"));
        assertEquals(13, envelope.getInt("version"));
        manager.restoreFrom(new java.io.ByteArrayInputStream(output.toByteArray()));
        assertEquals(before, body.heights(20, 0));
        assertEquals(preferences, body.preferences());
        var missing = new JSONObject(envelope.toString());
        missing.getJSONObject("data").remove("bodyMeasurements");
        try {
            manager.restoreFrom(
                    new java.io.ByteArrayInputStream(
                            missing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            fail("Missing current body section accepted");
        } catch (java.io.IOException expected) {
        }
        assertEquals(before, body.heights(20, 0));
        missing.put("version", 11);
        manager.restoreFrom(
                new java.io.ByteArrayInputStream(
                        missing.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(body.heights(20, 0).isEmpty());
        assertEquals(BodyMeasurementsRepository.Preferences.defaults(), body.preferences());
        assertEquals(Immutable.list(w), body.weights());
        var future = new JSONObject(envelope.toString()).put("version", 14);
        try {
            manager.restoreFrom(
                    new java.io.ByteArrayInputStream(
                            future.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            fail("Future accepted");
        } catch (java.io.IOException expected) {
        }
        assertTrue(body.heights(20, 0).isEmpty());
        assertEquals(Immutable.list(w), body.weights());
    }

    public void testCorruptBodyAndLateRestoreFailurePreserveEverything() throws Exception {
        var h = height("manual", "manual", NOW);
        body.saveHeight(h);
        var w = weight("manual-weight", "manual", NOW);
        body.saveWeight(w);
        var backups = new SqliteBackupRepository(getContext());
        var good = backups.exportUserData();
        var malformed = new JSONObject(good.toString());
        malformed
                .getJSONObject("bodyMeasurements")
                .getJSONArray("heights")
                .getJSONObject(0)
                .put("metres", "1.78");
        rejects(() -> backups.restoreUserData(malformed, false));
        assertEquals(Immutable.list(h), body.heights(20, 0));
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER fail_height_restore BEFORE INSERT ON body_heights BEGIN SELECT"
                        + " RAISE(ABORT,'injected'); END");
        try {
            try {
                backups.restoreUserData(good, false);
                fail("Expected rollback");
            } catch (android.database.sqlite.SQLiteException expected) {
            }
            assertEquals(Immutable.list(h), body.heights(20, 0));
            assertEquals(Immutable.list(w), body.weights());
        } finally {
            db.execSQL("DROP TRIGGER fail_height_restore");
        }
    }

    public void testSeventeenUpgradeRetainsExistingWeightAndMilestoneData() {
        var w = weight("legacy-weight", "manual", NOW);
        new DevelopmentStore(getContext()).saveWeight(w);
        new SqliteAchievementsRepository(getContext()).celebrations(true);
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        GymSchemaFixture.removeVersionEighteenTables(db);
        db.setVersion(17);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(19, db.getVersion());
        assertEquals(Immutable.list(w), body.weights());
        assertTrue(new SqliteAchievementsRepository(getContext()).celebrations());
        assertFalse(body.preferences().proteinEnabled());
        assertTrue(body.heights(20, 0).isEmpty());
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            fail("Expected rejected body data");
        } catch (IllegalArgumentException expected) {
        }
    }
}
