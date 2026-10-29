package org.racedaydreams.data;

import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.*;
import org.racedaydreams.domain.strength.StrengthProgrammingRepository.*;

import java.time.*;
import java.util.*;

/** Real SQLite authoring transactions; prescriptions remain independent from actuals. */
@SuppressWarnings("deprecation")
public final class StrengthProgrammingRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final LocalDate START = LocalDate.of(2026, 10, 28);
    private SqliteStrengthProgrammingRepository repository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        repository =
                new SqliteStrengthProgrammingRepository(
                        getContext(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testDraftCompareAndSaveRetryAndStaleDelete() {
        var draft = draft();
        repository.saveDraft(draft, 0);
        repository.saveDraft(draft, 0);
        assertEquals(draft, repository.draft(draft.id()));
        var edited =
                new StrengthProgrammeDraft(
                        draft.id(),
                        2,
                        draft.programmeId(),
                        0,
                        "Edited",
                        draft.settings(),
                        START,
                        draft.blocks(),
                        draft.origin(),
                        null);
        rejects(() -> repository.saveDraft(edited, 0));
        repository.saveDraft(edited, 1);
        rejects(() -> repository.deleteDraft(draft.id(), 1));
        assertEquals("Edited", repository.drafts(20, 0).get(0).name());
        repository.deleteDraft(draft.id(), 2);
        assertNull(repository.draft(draft.id()));
    }

    public void testActivationIsExplicitAtomicIdempotentAndRetainedAfterStop() {
        var programme = programme();
        assertNull(repository.currentReference());
        var accepted = repository.activate(programme, null, "accept", Collections.emptyList());
        assertEquals(
                accepted, repository.activate(programme, null, "accept", Collections.emptyList()));
        assertEquals(
                new Reference(Owner.AUTHORED, programme.id(), 1), repository.currentReference());
        assertEquals(programme, repository.programme(programme.id(), 1));
        assertEquals(1, repository.versions(20, 0).size());
        rejects(() -> repository.activate(programme, null, "another", Collections.emptyList()));
        var stop = repository.stop(repository.currentReference(), "stop");
        assertEquals(stop, repository.stop(accepted.after(), "stop"));
        assertNull(repository.currentReference());
        assertEquals(programme, repository.programme(programme.id(), 1));
        assertEquals(2, repository.decisions().size());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
    }

    public void testDecisionWriteFailureRollsBackEveryProgrammeRowAndPointer() {
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER reject_programme_decision BEFORE INSERT ON"
                    + " strength_programme_decisions BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            try {
                repository.activate(programme(), null, "accept", Collections.emptyList());
                fail("Expected SQL failure");
            } catch (SQLiteException expected) {
            }
            assertNull(repository.currentReference());
            assertTrue(repository.versions(20, 0).isEmpty());
            assertTrue(repository.decisions().isEmpty());
            try (var c = db.rawQuery("SELECT COUNT(*) FROM strength_programme_sessions", null)) {
                c.moveToFirst();
                assertEquals(0, c.getInt(0));
            }
        } finally {
            db.execSQL("DROP TRIGGER reject_programme_decision");
        }
    }

    public void testSummaryPageDoesNotHydrateSessionPayloads() {
        var programme = programme();
        repository.activate(programme, null, "accept", Collections.emptyList());
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL("UPDATE strength_programme_sessions SET payload='corrupt'");
        assertEquals(programme.name(), repository.versions(1, 0).get(0).name());
        rejects(() -> repository.programme(programme.id(), 1));
        rejects(() -> repository.versions(101, 0));
        rejects(() -> repository.versions(1, -1));
    }

    public void testTypedBackupRoundTripAndCorruptGraphIsNonDestructive() throws Exception {
        var programme = programme();
        repository.saveDraft(draft(), 0);
        repository.activate(programme, null, "accept", Collections.emptyList());
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        var retained = repository.currentReference();
        repository.stop(retained, "stop");
        backups.restoreUserData(data, false);
        assertEquals(retained, repository.currentReference());
        assertEquals(programme, repository.programme(programme.id(), 1));
        assertEquals(draft(), repository.draft("draft"));
        assertEquals(1, repository.decisions().size());
        var corrupt = new JSONObject(data.toString());
        corrupt.getJSONObject("strengthProgramming").getJSONArray("decisions").remove(0);
        rejects(() -> backups.restoreUserData(corrupt, false));
        assertEquals(retained, repository.currentReference());
        assertEquals(programme, repository.programme(programme.id(), 1));
    }

    public void testOlderEightDataWithoutAuthoringSectionRemainsRestorable() throws Exception {
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        data.remove("strengthProgramming");
        repository.activate(programme(), null, "accept", Collections.emptyList());
        backups.restoreUserData(data, false);
        assertNull(repository.currentReference());
        assertTrue(repository.versions(20, 0).isEmpty());
    }

    public void testAuthoredExecutionRecoveryFinishAndFreshCompletionProtectRevision()
            throws Exception {
        var normal = settings();
        var weightedSettings =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        normal.priority(),
                        GymProgramme.Experience.INTERMEDIATE,
                        normal.days(),
                        normal.frequency(),
                        normal.sessionMinutes(),
                        normal.equipment());
        var programme =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        "authoring",
                                        NOW,
                                        "My strength programme",
                                        weightedSettings,
                                        START,
                                        6,
                                        null,
                                        PrimaryFocus.STRENGTH,
                                        Collections.emptySet(),
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        false));
        repository.activate(programme, null, "activate", Collections.emptyList());
        var prescribed = programme.workouts().get(0);
        var prepared = GymWorkoutAdapter.fromAuthoredProgramme(programme, prescribed.id());
        var gym = new SqliteGymExecutionRepository(getContext());
        var engine =
                GymExecutionEngine.start(
                        "authored-actual",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        var started = gym.start(engine.snapshot());
        var slot =
                prepared.definition().slots().stream()
                        .filter(
                                v ->
                                        v.exercise().measurement()
                                                == StrengthExercise.MeasurementProfile.LOAD_REPS)
                        .findFirst()
                        .orElseThrow();
        engine.saveSet(
                slot.id(),
                new StrengthSession.SetEntry("actual-set", 6, 20.0, 7.0, false),
                tick(1000));
        engine.pause(tick(2000));
        var saved = gym.save(engine.snapshot(), started.revision());
        var recovered =
                GymExecutionEngine.recover(gym.load("authored-actual").execution(), tick(3000));
        assertEquals(saved.execution(), gym.load("authored-actual").execution());
        var finished =
                gym.finish(
                        recovered.finish(tick(4000), null, "Recorded evidence", false),
                        saved.revision());
        assertEquals(
                20.0,
                new CoachingStore(getContext())
                        .strength()
                        .get(0)
                        .exercises()
                        .get(0)
                        .sets()
                        .get(0)
                        .kilograms(),
                0.0);
        var link = new DevelopmentStore(getContext()).gymLinks().get("authored-actual");
        assertEquals("authoring:v1", link.versionKey());
        assertEquals(prescribed.id(), link.prescribedSessionId());
        var oldSettings = programme.settings();
        var changedSettings =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.MUSCLE,
                        oldSettings.priority(),
                        oldSettings.experience(),
                        oldSettings.days(),
                        oldSettings.frequency(),
                        oldSettings.sessionMinutes(),
                        oldSettings.equipment());
        var generated =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        programme.id(),
                                        NOW,
                                        "Stronger",
                                        changedSettings,
                                        START,
                                        6,
                                        null,
                                        PrimaryFocus.STRENGTH,
                                        Collections.emptySet(),
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        false));
        var stale =
                StrengthProgrammeEditor.replaceFuture(
                        programme,
                        generated,
                        START,
                        NOW,
                        Collections.emptySet(),
                        "Reviewed change");
        rejects(
                () ->
                        repository.activate(
                                stale.proposed(),
                                repository.currentReference(),
                                "stale",
                                Collections.emptyList()));
        var reviewed =
                StrengthProgrammeEditor.replaceFuture(
                        programme,
                        generated,
                        START,
                        NOW,
                        Collections.singleton(prescribed.id()),
                        "Keep completed prescription");
        repository.activate(
                reviewed.proposed(),
                repository.currentReference(),
                "reviewed",
                Collections.emptyList());
        assertEquals(programme, repository.programme(programme.id(), 1));
        assertEquals(
                Collections.singletonList(prescribed.id()),
                repository.decisions().get(1).protectedWorkoutIds());
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
        assertEquals(finished.execution(), gym.load("authored-actual").execution());
        assertEquals(2, repository.currentReference().revision());
        assertEquals(programme, repository.programme(programme.id(), 1));
    }

    private static GymExecution.Tick tick(long millis) {
        return new GymExecution.Tick("boot", millis, NOW.plusMillis(millis));
    }

    public void testCustomMetadataCompareAndSaveKeepsTaskIdentityAndRoundTrips() throws Exception {
        var exercise =
                new StrengthExercise(
                        "custom-own-press",
                        "Own press",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        var requirement =
                new ExerciseMetadata.EquipmentRequirement(
                        Collections.singletonList(
                                EnumSet.of(ExerciseMetadata.Equipment.DUMBBELLS)));
        var value =
                new CustomStrengthExercise(
                        exercise,
                        ExerciseMetadata.BodyArea.CHEST,
                        ExerciseMetadata.Role.HORIZONTAL_PUSH,
                        requirement,
                        "My setup");
        repository.saveCustom(value, null);
        repository.saveCustom(value, null);
        assertEquals(Collections.singletonList(value), repository.customExercises());
        var renamedExercise =
                new StrengthExercise(
                        exercise.id(),
                        "Renamed own press",
                        exercise.region(),
                        exercise.mode(),
                        false,
                        exercise.measurement());
        var renamed =
                new CustomStrengthExercise(
                        renamedExercise, value.bodyArea(), value.role(), requirement, null);
        rejects(() -> repository.saveCustom(renamed, null));
        repository.saveCustom(renamed, value);
        assertEquals(renamed, repository.customExercise(exercise.id()));
        var otherTask =
                new CustomStrengthExercise(
                        new StrengthExercise(
                                exercise.id(),
                                exercise.name(),
                                exercise.region(),
                                StrengthExercise.Mode.BODYWEIGHT,
                                false,
                                StrengthExercise.MeasurementProfile.REPS_ONLY),
                        value.bodyArea(),
                        value.role(),
                        requirement,
                        null);
        rejects(() -> repository.saveCustom(otherTask, renamed));
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        backups.restoreUserData(data, false);
        assertEquals(Collections.singletonList(renamed), repository.customExercises());
        var bad = new JSONObject(data.toString());
        bad.getJSONObject("strengthProgramming")
                .getJSONArray("customExercises")
                .getJSONObject(0)
                .getJSONObject("exercise")
                .put("name", "Conflicting name");
        rejects(() -> backups.restoreUserData(bad, false));
        assertEquals(renamed, repository.customExercise(exercise.id()));
    }

    public void testVersionThirteenUpgradeRetainsHistoryFavouritesAndExclusions() {
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO settings VALUES('retained','yes')");
        db.execSQL(
                "INSERT INTO strength_sessions VALUES('old-actual','2026-10-28','retained"
                        + " payload')");
        db.execSQL("INSERT INTO strength_user_state VALUES('bench-press',1,1)");
        db.execSQL(
                "INSERT INTO strength_exclusions"
                        + " VALUES('bench-press','PREFERENCE','2026-10-28T12:00:00Z')");
        GymSchemaFixture.removeVersionFourteenTables(db);
        db.setVersion(13);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(14, db.getVersion());
        for (String table :
                new String[] {"strength_sessions", "strength_user_state", "strength_exclusions"}) {
            try (var c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
                c.moveToFirst();
                assertEquals(1, c.getInt(0));
            }
        }
        try (var c = db.rawQuery("SELECT payload FROM strength_sessions", null)) {
            c.moveToFirst();
            assertEquals("retained payload", c.getString(0));
        }
        assertEquals("yes", new SqliteSettingsRepository(getContext()).getSetting("retained"));
        assertTrue(repository.versions(20, 0).isEmpty());
        assertNull(repository.currentReference());
    }

    public void testNineRequiresAuthoringSectionAndPreviousEightStillRestores() throws Exception {
        var backups = new SqliteBackupRepository(getContext());
        var manager =
                new org.racedaydreams.portability.BackupManager(
                        getContext(), backups, Clock.fixed(NOW, ZoneOffset.UTC));
        var output = new java.io.ByteArrayOutputStream();
        manager.exportTo(output);
        var previous =
                new JSONObject(output.toString(java.nio.charset.StandardCharsets.UTF_8.name()));
        assertEquals(9, previous.getInt("version"));
        previous.getJSONObject("data").remove("strengthProgramming");
        repository.activate(programme(), null, "accept", Collections.emptyList());
        try {
            manager.restoreFrom(
                    new java.io.ByteArrayInputStream(
                            previous.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            fail("Version nine must include the authoring section");
        } catch (java.io.IOException expected) {
        }
        assertNotNull(repository.currentReference());
        previous.put("version", 8);
        manager.restoreFrom(
                new java.io.ByteArrayInputStream(
                        previous.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertNull(repository.currentReference());
        assertTrue(repository.versions(20, 0).isEmpty());
    }

    public void testLegacyTransitionIsExplicitRetainsOriginalAndRejectsIdentityCollision()
            throws Exception {
        var legacySettings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.BALANCED,
                        GymProgramme.Experience.BEGINNER,
                        EnumSet.allOf(DayOfWeek.class),
                        2,
                        Collections.singleton("full gym"),
                        60,
                        GymProgramme.Effort.RPE);
        var legacy =
                new GymProgramme(
                        "legacy",
                        1,
                        NOW,
                        "Existing programme",
                        legacySettings,
                        START,
                        START.plusDays(41),
                        Collections.singletonList(
                                new GymProgramme.Session(
                                        "legacy-session",
                                        START,
                                        "Original workout",
                                        Collections.singletonList(
                                                new GymProgramme.Prescription(
                                                        "bench-press",
                                                        3,
                                                        5,
                                                        8,
                                                        null,
                                                        null,
                                                        6,
                                                        8,
                                                        120,
                                                        "Original",
                                                        80.0)),
                                        "Preserved")),
                        Collections.emptyList());
        var oldStore = new GymProgrammeStore(getContext());
        oldStore.save(legacy);
        var oldReference = new Reference(Owner.LEGACY, "legacy", 1);
        assertEquals(oldReference, repository.currentReference());
        var p = programme();
        var collision =
                new StrengthProgramme(
                        "legacy",
                        1,
                        0,
                        NOW,
                        p.name(),
                        p.origin(),
                        p.templateId(),
                        p.settings(),
                        p.blocks(),
                        p.effectiveFrom(),
                        p.reason(),
                        p.tradeoffs());
        rejects(
                () ->
                        repository.activate(
                                collision, oldReference, "collision", Collections.emptyList()));
        assertEquals(oldReference, repository.currentReference());
        repository.activate(p, oldReference, "accept", Collections.emptyList());
        assertNull(oldStore.current());
        assertEquals(legacy, oldStore.version("legacy", 1));
        rejects(() -> oldStore.save(legacy));
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        backups.restoreUserData(data, false);
        assertEquals(legacy, oldStore.version("legacy", 1));
        assertEquals(p, repository.programme(p.id(), 1));
        assertEquals(oldReference, repository.decisions().get(0).before());
    }

    public void testLongProgrammeUsesBoundedSessionRowsAndRoundTrips() {
        var normal = settings();
        var settings =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.MUSCLE,
                        normal.priority(),
                        GymProgramme.Experience.EXPERIENCED,
                        normal.days(),
                        6,
                        90,
                        normal.equipment());
        var p =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        "long-programme",
                                        NOW,
                                        "Long programme",
                                        settings,
                                        START,
                                        26,
                                        null,
                                        PrimaryFocus.STRENGTH,
                                        Collections.emptySet(),
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        false));
        repository.activate(p, null, "accept", Collections.emptyList());
        assertEquals(156, p.workouts().size());
        assertEquals(p, repository.programme(p.id(), 1));
        try (var c =
                ProfileDatabase.get(getContext())
                        .getReadableDatabase()
                        .rawQuery(
                                "SELECT COUNT(*),MAX(length(CAST(payload AS BLOB))) FROM"
                                        + " strength_programme_sessions",
                                null)) {
            c.moveToFirst();
            assertEquals(156, c.getInt(0));
            assertTrue(c.getInt(1) <= 262144);
        }
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
        assertEquals(p, repository.programme(p.id(), 1));
    }

    public void testRestoreSqlFailureAfterReplacementStartsRollsBackTypedRows() {
        var p = programme();
        repository.activate(p, null, "accept", Collections.emptyList());
        repository.saveDraft(draft(), 0);
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER fail_authored_restore BEFORE INSERT ON strength_programme_sessions"
                        + " BEGIN SELECT RAISE(ABORT,'injected restore'); END");
        try {
            try {
                backups.restoreUserData(data, false);
                fail("Expected SQL restore failure");
            } catch (SQLiteException expected) {
            }
            assertEquals(p, repository.programme(p.id(), 1));
            assertEquals(draft(), repository.draft("draft"));
            assertEquals(new Reference(Owner.AUTHORED, p.id(), 1), repository.currentReference());
            assertEquals(1, repository.decisions().size());
        } finally {
            db.execSQL("DROP TRIGGER fail_authored_restore");
        }
    }

    public void testExistingLoggingEditorKeepsTypedMetadataAndBackupConsistent() {
        var exercise =
                new StrengthExercise(
                        "custom-log-editor",
                        "Own movement",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        var value =
                new CustomStrengthExercise(
                        exercise,
                        ExerciseMetadata.BodyArea.CHEST,
                        ExerciseMetadata.Role.HORIZONTAL_PUSH,
                        new ExerciseMetadata.EquipmentRequirement(
                                Collections.singletonList(
                                        EnumSet.of(ExerciseMetadata.Equipment.DUMBBELLS))),
                        null);
        repository.saveCustom(value, null);
        var renamed =
                new StrengthExercise(
                        exercise.id(),
                        "Renamed movement",
                        exercise.region(),
                        exercise.mode(),
                        false,
                        exercise.measurement());
        new CoachingStore(getContext()).saveExercise(renamed);
        assertEquals(renamed, repository.customExercise(exercise.id()).exercise());
        assertEquals(value.equipment(), repository.customExercise(exercise.id()).equipment());
        assertEquals("Own movement", value.exercise().name());
        rejects(
                () ->
                        new CoachingStore(getContext())
                                .saveExercise(
                                        new StrengthExercise(
                                                exercise.id(),
                                                "Another task",
                                                exercise.region(),
                                                StrengthExercise.Mode.BODYWEIGHT,
                                                false,
                                                StrengthExercise.MeasurementProfile.REPS_ONLY)));
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
        assertEquals(renamed, repository.customExercise(exercise.id()).exercise());
    }

    public void testSimilarlyNamedProgrammeCannotClaimAnotherProgrammesCompletedSession() {
        var p = programme();
        var other =
                new StrengthProgramme(
                        p.id() + ":v2",
                        1,
                        0,
                        NOW,
                        "Other programme",
                        p.origin(),
                        p.templateId(),
                        p.settings(),
                        p.blocks(),
                        p.effectiveFrom(),
                        p.reason(),
                        p.tradeoffs());
        repository.activate(other, null, "other", Collections.emptyList());
        var prescribed = other.workouts().get(0);
        var prepared = GymWorkoutAdapter.fromAuthoredProgramme(other, prescribed.id());
        var gym = new SqliteGymExecutionRepository(getContext());
        var engine =
                GymExecutionEngine.start(
                        "other-actual",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        gym.start(engine.snapshot());
        var slot =
                prepared.definition().slots().stream()
                        .filter(v -> v.target().minimumReps() != null)
                        .findFirst()
                        .orElseThrow();
        engine.saveSet(
                slot.id(),
                new StrengthSession.SetEntry(
                        "set",
                        6,
                        slot.exercise().measurement().usesLoad() ? 20.0 : 0.0,
                        7.0,
                        false),
                tick(1000));
        gym.finish(engine.finish(tick(2000), null, "Actual evidence", false), 1);
        repository.activate(p, repository.currentReference(), "main", Collections.emptyList());
        var normal = p.settings();
        var changed =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        normal.priority(),
                        normal.experience(),
                        normal.days(),
                        normal.frequency(),
                        normal.sessionMinutes(),
                        normal.equipment());
        var candidate =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        p.id(),
                                        NOW,
                                        "Changed programme",
                                        changed,
                                        START,
                                        6,
                                        null,
                                        PrimaryFocus.STRENGTH,
                                        Collections.emptySet(),
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        false));
        var proposal =
                StrengthProgrammeEditor.replaceFuture(
                        p,
                        candidate,
                        START,
                        NOW,
                        Collections.emptySet(),
                        "Only this programme changes");
        var accepted =
                repository.activate(
                        proposal.proposed(),
                        repository.currentReference(),
                        "change",
                        Collections.emptyList());
        assertTrue(accepted.protectedWorkoutIds().isEmpty());
        assertEquals(other, repository.programme(other.id(), 1));
        assertEquals(p, repository.programme(p.id(), 1));
        assertEquals(other.id(), gym.load("other-actual").execution().source().id());
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
        assertEquals(proposal.proposed(), repository.programme(p.id(), 2));
        assertEquals(other.id(), gym.load("other-actual").execution().source().id());
    }

    private static StrengthProgramme.Settings settings() {
        return new StrengthProgramme.Settings(
                GymProgramme.Goal.GENERAL,
                GymProgramme.Priority.BALANCED,
                GymProgramme.Experience.BEGINNER,
                EnumSet.allOf(DayOfWeek.class),
                2,
                60,
                EnumSet.allOf(ExerciseMetadata.Equipment.class));
    }

    private static StrengthProgramme programme() {
        return new StrengthProgrammeGenerator()
                .generate(
                        new StrengthProgrammeGenerator.Request(
                                "authoring",
                                NOW,
                                "My programme",
                                settings(),
                                START,
                                6,
                                null,
                                PrimaryFocus.STRENGTH,
                                Collections.emptySet(),
                                null,
                                Collections.emptySet(),
                                Collections.emptySet(),
                                false));
    }

    private static StrengthProgrammeDraft draft() {
        return new StrengthProgrammeDraft(
                "draft",
                1,
                "draft-programme",
                0,
                "Draft",
                settings(),
                START,
                Collections.emptyList(),
                StrengthProgramme.Origin.CUSTOM,
                null);
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            fail("Expected rejected operation");
        } catch (IllegalArgumentException expected) {
        }
    }
}
