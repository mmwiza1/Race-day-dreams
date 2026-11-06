package org.racedaydreams.data;

import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.settings.PrimaryFocus;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

/** Real shared SQLite transactions, strict old/current archives and immutable concrete previews. */
@SuppressWarnings("deprecation")
public final class ConcreteStrengthProgrammeDraftRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-11-04T12:00:00Z");
    private SqliteConcreteStrengthProgrammeDraftRepository drafts;
    private SqliteStrengthProgrammingRepository programmes;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        drafts =
                new SqliteConcreteStrengthProgrammeDraftRepository(
                        getContext(), Clock.fixed(NOW, ZoneOffset.UTC));
        programmes =
                new SqliteStrengthProgrammingRepository(
                        getContext(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private static ConcreteStrengthProgrammeDraft draft(String id) {
        var settings =
                new StrengthProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.BALANCED,
                        GymProgramme.Experience.INTERMEDIATE,
                        EnumSet.allOf(DayOfWeek.class),
                        4,
                        60,
                        EnumSet.allOf(ExerciseMetadata.Equipment.class));
        var p =
                new StrengthProgrammeGenerator()
                        .generate(
                                new StrengthProgrammeGenerator.Request(
                                        id + "-programme",
                                        NOW,
                                        "Concrete preview",
                                        settings,
                                        LocalDate.of(2026, 11, 4),
                                        16,
                                        "upper-lower-4",
                                        PrimaryFocus.STRENGTH,
                                        Collections.emptySet(),
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        true));
        return new ConcreteStrengthProgrammeDraft(id, 1, null, p, Collections.emptyList());
    }

    public void testRecoveryPreviewRoundTripsWithoutActivatingAndKeepsOriginalRevision()
            throws Exception {
        var original = draft("recovery");
        drafts.save(original, 0);
        drafts.activate(original, "original-acceptance");
        var before = programmes.currentReference();
        var review =
                new org.racedaydreams.domain.recovery.TrainingRecovery.Review(
                        LocalDate.of(2026, 11, 4),
                        "Optional review",
                        "Keep or preview",
                        true,
                        true,
                        Collections.emptyList(),
                        Collections.emptyList());
        var proposal =
                org.racedaydreams.domain.recovery.RecoveryProgrammeProposal.lighterWeek(
                        original.programme(), review, NOW, ZoneOffset.UTC, Collections.emptySet());
        var next =
                new ConcreteStrengthProgrammeDraft(
                        "recovery-preview",
                        1,
                        before,
                        proposal.proposed(),
                        Collections.emptyList());
        drafts.save(next, 0);
        assertEquals(before, programmes.currentReference());
        assertEquals(original.programme(), programmes.programme(before.id(), before.revision()));
        var backup = new SqliteBackupRepository(getContext());
        var archive = backup.exportUserData();
        backup.restoreUserData(archive, false);
        assertEquals(next, drafts.get(next.id()));
        assertEquals(before, programmes.currentReference());
        var accepted = drafts.activate(next, "athlete-reviewed-lighter-week");
        assertEquals(
                next.programme(),
                programmes.programme(accepted.after().id(), accepted.after().revision()));
        assertEquals(original.programme(), programmes.programme(before.id(), before.revision()));
        assertEquals(2, programmes.decisions().size());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
    }

    public void testExactDatedPreviewReloadAndBoundedSummaryDoesNotActivateOrHydrate() {
        var value = draft("concrete");
        drafts.save(value, 0);
        drafts.save(value, 0);
        assertEquals(value, drafts.get(value.id()));
        assertEquals(64, drafts.get(value.id()).programme().workouts().size());
        assertNull(programmes.currentReference());
        assertTrue(programmes.versions(20, 0).isEmpty());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
        assertEquals(value.programme().name(), drafts.list(1, 0).get(0).name());
        assertTrue(drafts.list(1, 1).isEmpty());
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL("UPDATE strength_concrete_draft_workouts SET payload='corrupt'");
        assertEquals(value.id(), drafts.list(1, 0).get(0).id());
        rejects(() -> drafts.get(value.id()));
        rejects(() -> drafts.list(101, 0));
        rejects(() -> drafts.list(1, -1));
    }

    public void testOptimisticSaveStaleReviewAndActivationRetryAfterDraftDeletion() {
        var value = draft("concrete");
        drafts.save(value, 0);
        var edit = value.withProgramme(value.programme(), Collections.emptyList());
        rejects(() -> drafts.save(edit, 0));
        drafts.save(edit, 1);
        rejects(() -> drafts.activate(value, "accept"));
        rejects(() -> drafts.delete(value.id(), 1));
        var accepted = drafts.activate(edit, "accept");
        assertEquals(edit.programme(), programmes.programme(edit.programme().id(), 1));
        assertTrue(drafts.list(1, 0).get(0).revisionExists());
        drafts.delete(edit.id(), 2);
        assertEquals(accepted, drafts.activate(edit, "accept"));
        assertNull(drafts.get(edit.id()));
        assertEquals(1, programmes.decisions().size());
        rejects(() -> drafts.save(edit, 0));
    }

    public void testFreshCurrentAndExclusionsAreRecheckedAtAcceptance() {
        var value = draft("concrete");
        drafts.save(value, 0);
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        var exercise =
                value.programme().workouts().get(0).definition().slots().get(0).exercise().id();
        db.execSQL(
                "INSERT INTO strength_exclusions VALUES(?,'PREFERENCE',?)",
                new Object[] {exercise, NOW.toString()});
        rejects(() -> drafts.activate(value, "excluded"));
        assertNull(programmes.currentReference());
        db.delete("strength_exclusions", null, null);
        var other = draft("other");
        programmes.activate(other.programme(), null, "other", Collections.emptyList());
        rejects(() -> drafts.activate(value, "stale-owner"));
        assertEquals(other.programme(), programmes.programme(other.programme().id(), 1));
        assertEquals(value, drafts.get(value.id()));
    }

    public void testLateDecisionFailureRollsBackActivationAndKeepsReviewedDraft() {
        var value = draft("concrete");
        drafts.save(value, 0);
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER reject_concrete_activation BEFORE INSERT ON"
                    + " strength_programme_decisions BEGIN SELECT RAISE(ABORT,'injected'); END");
        try {
            try {
                drafts.activate(value, "accept");
                fail("Expected SQL failure");
            } catch (SQLiteException expected) {
            }
            assertNull(programmes.currentReference());
            assertTrue(programmes.versions(20, 0).isEmpty());
            assertTrue(programmes.decisions().isEmpty());
            assertEquals(value, drafts.get(value.id()));
        } finally {
            db.execSQL("DROP TRIGGER reject_concrete_activation");
        }
    }

    public void
            testCurrentRootNineRoundTripOldAuthoringOneAndFutureCorruptionRejectBeforeReplacement()
                    throws Exception {
        var value = draft("concrete");
        drafts.save(value, 0);
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        assertEquals(2, data.getJSONObject("strengthProgramming").getInt("version"));
        drafts.delete(value.id(), 1);
        backups.restoreUserData(data, false);
        assertEquals(value, drafts.get(value.id()));
        var old = new JSONObject(data.toString());
        old.getJSONObject("strengthProgramming").put("version", 1).remove("concreteDrafts");
        backups.restoreUserData(old, false);
        assertTrue(drafts.list(10, 0).isEmpty());
        drafts.save(value, 0);
        var bad = new JSONObject(data.toString());
        bad.getJSONObject("strengthProgramming").put("version", 3);
        rejects(() -> backups.restoreUserData(bad, false));
        assertEquals(value, drafts.get(value.id()));
        var corrupt = new JSONObject(data.toString());
        corrupt.getJSONObject("strengthProgramming")
                .getJSONArray("concreteDrafts")
                .getJSONObject(0)
                .getJSONObject("programme")
                .put("revision", 2);
        rejects(() -> backups.restoreUserData(corrupt, false));
        assertEquals(value, drafts.get(value.id()));
        var missing = new JSONObject(data.toString());
        missing.getJSONObject("strengthProgramming").remove("concreteDrafts");
        rejects(() -> backups.restoreUserData(missing, false));
        assertEquals(value, drafts.get(value.id()));
    }

    public void testRestoreSqlFailureRollsBackEveryConcreteRow() {
        var value = draft("concrete");
        drafts.save(value, 0);
        var backups = new SqliteBackupRepository(getContext());
        var data = backups.exportUserData();
        var db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER reject_concrete_restore BEFORE INSERT ON"
                        + " strength_concrete_draft_workouts BEGIN SELECT RAISE(ABORT,'injected');"
                        + " END");
        try {
            try {
                backups.restoreUserData(data, false);
                fail("Expected SQL failure");
            } catch (SQLiteException expected) {
            }
            assertEquals(value, drafts.get(value.id()));
            assertNull(programmes.currentReference());
        } finally {
            db.execSQL("DROP TRIGGER reject_concrete_restore");
        }
    }

    public void testFourteenUpgradePreservesDraftActualAndOwner() {
        var value = draft("concrete");
        programmes.activate(value.programme(), null, "accept", Collections.emptyList());
        var helper = ProfileDatabase.get(getContext());
        var db = helper.getWritableDatabase();
        db.execSQL("INSERT INTO strength_sessions VALUES('actual','2026-11-04','original actual')");
        GymSchemaFixture.removeVersionFifteenTables(db);
        db.setVersion(14);
        helper.close();
        db = helper.getWritableDatabase();
        assertEquals(19, db.getVersion());
        assertEquals(value.programme(), programmes.programme(value.programme().id(), 1));
        assertEquals(value.programme().id(), programmes.currentReference().id());
        try (var c = db.rawQuery("SELECT payload FROM strength_sessions WHERE id='actual'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("original actual", c.getString(0));
        }
        assertTrue(drafts.list(10, 0).isEmpty());
    }

    public void testRootNineAndCompleteBackupPreserveUnacceptedPreview() throws Exception {
        var value = draft("complete");
        drafts.save(value, 0);
        var manager =
                new org.racedaydreams.portability.BackupManager(
                        getContext(),
                        new SqliteBackupRepository(getContext()),
                        Clock.fixed(NOW, ZoneOffset.UTC));
        var bytes = new java.io.ByteArrayOutputStream();
        manager.exportTo(bytes);
        assertEquals(13, new JSONObject(bytes.toString("UTF-8")).getInt("version"));
        drafts.delete(value.id(), 1);
        manager.restoreFrom(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(value, drafts.get(value.id()));
        var complete = new CompleteBackup(getContext());
        bytes.reset();
        complete.exportTo(bytes);
        drafts.delete(value.id(), 1);
        complete.restoreFrom(new java.io.ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(value, drafts.get(value.id()));
        assertNull(programmes.currentReference());
    }

    public void testConcreteCodecRejectsFutureUnknownFieldsAndMissingBase() throws Exception {
        var value = draft("codec");
        assertEquals(
                value,
                ConcreteStrengthProgrammeJson.decode(ConcreteStrengthProgrammeJson.encode(value)));
        var future = ConcreteStrengthProgrammeJson.encode(value).put("version", 2);
        rejects(() -> ConcreteStrengthProgrammeJson.decode(future));
        var extra = ConcreteStrengthProgrammeJson.encode(value).put("unknown", true);
        rejects(() -> ConcreteStrengthProgrammeJson.decode(extra));
        drafts.save(value, 0);
        var backups = new SqliteBackupRepository(getContext());
        var corrupt = backups.exportUserData();
        var base = GymJsonSupport.object("owner", "AUTHORED", "id", "missing", "revision", 1);
        corrupt.getJSONObject("strengthProgramming")
                .getJSONArray("concreteDrafts")
                .getJSONObject(0)
                .put("before", base);
        rejects(() -> backups.restoreUserData(corrupt, false));
        assertEquals(value, drafts.get(value.id()));
    }

    public void testCompetingRevisionDraftsRemainIndependentAfterAcceptanceAndBackup() {
        var original = draft("base").programme();
        programmes.activate(original, null, "initial", Collections.emptyList());
        var base = programmes.currentReference();
        var next =
                new StrengthProgramme(
                        original.id(),
                        2,
                        1,
                        NOW,
                        original.name(),
                        original.origin(),
                        original.templateId(),
                        original.settings(),
                        original.blocks(),
                        original.effectiveFrom(),
                        "Athlete reviewing future work",
                        original.tradeoffs());
        var a =
                new ConcreteStrengthProgrammeDraft(
                        "candidate-a", 1, base, next, Collections.emptyList());
        var first = next.workouts().get(0);
        var changed =
                new StrengthProgramme.Workout(
                        first.id(),
                        first.date(),
                        first.expectedMinutes(),
                        first.definition(),
                        "Reviewed alternative notes");
        var candidate = ConcreteStrengthProgrammeEditor.replaceWorkout(a, changed).programme();
        var b =
                new ConcreteStrengthProgrammeDraft(
                        "candidate-b", 1, base, candidate, Collections.emptyList());
        drafts.save(a, 0);
        drafts.save(b, 0);
        drafts.activate(b, "accept-b");
        var backups = new SqliteBackupRepository(getContext());
        backups.restoreUserData(backups.exportUserData(), false);
        assertEquals(a, drafts.get(a.id()));
        assertEquals(b, drafts.get(b.id()));
        assertEquals(original, programmes.programme(original.id(), 1));
        assertEquals(candidate, programmes.programme(original.id(), 2));
        assertTrue(
                drafts.list(10, 0).stream()
                        .allMatch(
                                ConcreteStrengthProgrammeDraftRepository.Summary::revisionExists));
        rejects(() -> drafts.activate(a, "stale-a"));
        rejects(() -> drafts.save(a.withProgramme(a.programme(), Collections.emptyList()), 1));
        assertEquals(candidate, programmes.programme(original.id(), 2));
    }

    public void testForwardActivationWaitsForTheSameProgrammesNativeWorkout() {
        var original = draft("live-base").programme();
        programmes.activate(original, null, "initial-live", Collections.emptyList());
        var base = programmes.currentReference();
        var next =
                new StrengthProgramme(
                        original.id(),
                        2,
                        1,
                        NOW,
                        original.name(),
                        original.origin(),
                        original.templateId(),
                        original.settings(),
                        original.blocks(),
                        original.effectiveFrom(),
                        "Reviewed future work",
                        original.tradeoffs());
        var value =
                new ConcreteStrengthProgrammeDraft(
                        "live-proposal", 1, base, next, Collections.emptyList());
        drafts.save(value, 0);
        var workout = original.workouts().get(0);
        var prepared = GymWorkoutAdapter.fromAuthoredProgramme(original, workout.id());
        var engine =
                GymExecutionEngine.start(
                        "live-preserved",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        new GymExecution.Tick("boot", 0, NOW));
        var gym = new SqliteGymExecutionRepository(getContext());
        var started = gym.start(engine.snapshot());
        rejects(() -> drafts.activate(value, "blocked-active"));
        engine.saveSet(
                workout.definition().slots().get(0).id(),
                new StrengthSession.SetEntry("live-set", 6, 20.0, 7.0, false),
                new GymExecution.Tick("boot", 1000, NOW.plusMillis(1000)));
        engine.pause(new GymExecution.Tick("boot", 2000, NOW.plusMillis(2000)));
        var paused = gym.save(engine.snapshot(), started.revision());
        rejects(() -> drafts.activate(value, "blocked-paused"));
        assertEquals(base, programmes.currentReference());
        assertEquals(1, programmes.decisions().size());
        assertEquals(value, drafts.get(value.id()));
        assertNull(programmes.programme(original.id(), 2));
        var finished =
                gym.finish(
                        engine.finish(
                                new GymExecution.Tick("boot", 3000, NOW.plusMillis(3000)),
                                null,
                                null,
                                false),
                        paused.revision());
        drafts.activate(value, "accepted-after-finish");
        assertEquals(next, programmes.programme(original.id(), 2));
        assertEquals(original, programmes.programme(original.id(), 1));
        assertEquals(finished.execution(), gym.load("live-preserved").execution());
        assertTrue(programmes.decisions().get(1).protectedWorkoutIds().contains(workout.id()));
    }

    public void testArchivedAfterPreviewCannotBeNewlyActivated() {
        var value = draft("fresh-archive");
        drafts.save(value, 0);
        var exercise =
                value.programme().workouts().get(0).definition().slots().get(0).exercise().id();
        ProfileDatabase.get(getContext())
                .getWritableDatabase()
                .execSQL("INSERT INTO strength_user_state VALUES(?,0,1)", new Object[] {exercise});
        rejects(() -> drafts.activate(value, "archived-after-review"));
        assertNull(programmes.currentReference());
        assertTrue(programmes.decisions().isEmpty());
        assertEquals(value, drafts.get(value.id()));
    }

    public void testUnrelatedReplacementCannotBypassFreshIntentValidation() {
        var original = draft("intent-base").programme();
        programmes.activate(original, null, "initial-intent", Collections.emptyList());
        var base = programmes.currentReference();
        var workout = original.workouts().get(0);
        var originalSlot = workout.definition().slots().get(0);
        var replacement = ExerciseCatalog.definition("seated-leg-curl").exercise();
        var slots = new ArrayList<>(workout.definition().slots());
        slots.set(
                0,
                new GymWorkoutDefinition.Slot(
                        originalSlot.id(),
                        replacement,
                        originalSlot.target().withoutLoadReference(),
                        null));
        var d = workout.definition();
        var changed =
                new StrengthProgramme.Workout(
                        workout.id(),
                        workout.date(),
                        StrengthWorkoutBudget.workoutMinutes(slots),
                        new GymWorkoutDefinition(d.id(), d.revision() + 1, NOW, d.title(), slots),
                        workout.notes());
        var next =
                new StrengthProgramme(
                        original.id(),
                        2,
                        1,
                        NOW,
                        original.name(),
                        original.origin(),
                        original.templateId(),
                        original.settings(),
                        original.blocks(),
                        original.effectiveFrom(),
                        "Reviewed replacement",
                        original.tradeoffs());
        var pending =
                new ConcreteStrengthProgrammeDraft(
                        "intent-review", 1, base, next, Collections.emptyList());
        var altered = ConcreteStrengthProgrammeEditor.replaceWorkout(pending, changed).programme();
        var mapping =
                new StrengthProgrammeEditor.Change(
                        workout.id(),
                        originalSlot.id(),
                        originalSlot.exercise().id(),
                        replacement.id(),
                        "Unrelated replacement must be rejected");
        var value =
                new ConcreteStrengthProgrammeDraft(
                        "intent-bypass", 1, base, altered, Collections.singletonList(mapping));
        drafts.save(value, 0);
        rejects(() -> drafts.activate(value, "reject-unrelated-intent"));
        assertEquals(base, programmes.currentReference());
        assertNull(programmes.programme(original.id(), 2));
        assertEquals(value, drafts.get(value.id()));
        assertEquals(1, programmes.decisions().size());
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {
        }
    }
}
