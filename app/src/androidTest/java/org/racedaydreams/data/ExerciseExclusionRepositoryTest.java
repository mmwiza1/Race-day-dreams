package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.strength.ExerciseExclusionRepository.*;

import java.time.Instant;

/** The actual shared database and backup section, including old reads and invalid-input safety. */
@SuppressWarnings("deprecation")
public final class ExerciseExclusionRepositoryTest extends AndroidTestCase {
    private SqliteExerciseExclusionRepository repository;
    private StrengthUserStore choices;

    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        repository = new SqliteExerciseExclusionRepository(getContext());
        choices = new StrengthUserStore(getContext());
    }

    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private static Exclusion bench() {
        return new Exclusion(
                "bench-press", Reason.PREFERENCE, Instant.parse("2026-10-28T12:00:00Z"));
    }

    public void testExplicitExclusionIsReversibleWithoutChangingExistingChoicesOrEvidence() {
        choices.save(new StrengthUserStore.Choice("bench-press", true, true));
        assertTrue(repository.exclusions().isEmpty());
        repository.exclude(bench());
        repository.exclude(bench());
        assertEquals(1, repository.exclusions().size());
        assertEquals(bench(), repository.exclusions().get(0));
        assertTrue(choices.choices().get(0).favourite());
        assertTrue(choices.choices().get(0).archived());
        repository.restoreExercise("bench-press");
        repository.restoreExercise("bench-press");
        assertTrue(repository.exclusions().isEmpty());
        assertTrue(choices.choices().get(0).favourite());
    }

    public void testCurrentChoicesSectionRoundTripAndOldVersionOneRead() throws Exception {
        choices.save(new StrengthUserStore.Choice("bench-press", true, false));
        repository.exclude(bench());
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        var data = StrengthUserStore.exportData(db);
        assertEquals(2, data.getInt("version"));
        StrengthUserStore.validate(data);
        StrengthUserStore.clear(db);
        StrengthUserStore.restore(db, data);
        assertEquals(bench(), repository.exclusions().get(0));
        data.put("version", 1);
        data.remove("exclusions");
        StrengthUserStore.validate(data);
        StrengthUserStore.clear(db);
        StrengthUserStore.restore(db, data);
        assertTrue(repository.exclusions().isEmpty());
        assertTrue(choices.choices().get(0).favourite());
    }

    public void testCorruptOrFutureSectionRejectedBeforeCurrentDataIsCleared() throws Exception {
        repository.exclude(bench());
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        for (int variant = 0; variant < 5; variant++) {
            var data = StrengthUserStore.exportData(db);
            var section = data.getJSONObject("exclusions");
            if (variant == 0) data.put("version", 3);
            if (variant == 1) section.put("version", 2);
            if (variant == 2)
                section.getJSONArray("entries")
                        .put(section.getJSONArray("entries").getJSONObject(0));
            if (variant == 3)
                section.getJSONArray("entries").getJSONObject(0).put("reason", "MEDICAL_DIAGNOSIS");
            if (variant == 4)
                section.getJSONArray("entries").getJSONObject(0).put("extra", "unrecognised");
            try {
                StrengthUserStore.validate(data);
                fail("Invalid future/corrupt data accepted");
            } catch (IllegalArgumentException expected) {
            }
            assertEquals(bench(), repository.exclusions().get(0));
        }
    }

    public void testCompleteJsonBackupPreservesExclusionsAndRejectsCorruptionNonDestructively()
            throws Exception {
        repository.exclude(bench());
        choices.save(new StrengthUserStore.Choice("bench-press", true, false));
        var manager =
                new org.racedaydreams.portability.BackupManager(
                        getContext(),
                        new SqliteBackupRepository(getContext()),
                        java.time.Clock.systemUTC());
        var output = new java.io.ByteArrayOutputStream();
        manager.exportTo(output);
        byte[] bytes = output.toByteArray();
        repository.restoreExercise("bench-press");
        manager.restoreFrom(new java.io.ByteArrayInputStream(bytes));
        assertEquals(bench(), repository.exclusions().get(0));
        assertTrue(choices.choices().get(0).favourite());
        var envelope = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        envelope.getJSONObject("data")
                .getJSONObject("strengthState")
                .getJSONObject("exclusions")
                .put("version", 2);
        try {
            manager.restoreFrom(
                    new java.io.ByteArrayInputStream(
                            envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            fail("Unsupported exclusion section replaced local data");
        } catch (java.io.IOException expected) {
        }
        assertEquals(bench(), repository.exclusions().get(0));
        assertTrue(choices.choices().get(0).favourite());
    }

    public void testRealVersionTwelveMigrationDoesNotReinterpretArchivedExercises() {
        choices.save(new StrengthUserStore.Choice("bench-press", true, true));
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("DROP TABLE strength_exclusions");
        db.setVersion(12);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(13, db.getVersion());
        assertTrue(repository.exclusions().isEmpty());
        assertTrue(choices.choices().get(0).archived());
        assertTrue(choices.choices().get(0).favourite());
    }
}
