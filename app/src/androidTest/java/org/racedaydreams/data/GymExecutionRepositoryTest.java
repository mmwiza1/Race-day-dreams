package org.racedaydreams.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.ExerciseCatalog;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionEngine;
import org.racedaydreams.domain.strength.GymExecutionRepository;
import org.racedaydreams.domain.strength.GymExecutionSet;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutAdapter;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Real SQLite durability, fault injection and retained-source regressions on every supported API.
 */
@SuppressWarnings("deprecation")
public final class GymExecutionRepositoryTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");
    private SqliteGymExecutionRepository gym;
    private CoachingStore coaching;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        gym = new SqliteGymExecutionRepository(getContext());
        coaching = new CoachingStore(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testEveryAcceptedSetSurvivesRecreationWithoutCompletedEvidence() {
        var engine = custom("durable");
        var first = gym.start(engine.snapshot());
        engine.saveSet("bench", set("actual", 7, 65), tick(1000));
        var saved = gym.save(engine.snapshot(), first.revision());
        assertEquals(2L, saved.revision());
        assertEquals(saved, new SqliteGymExecutionRepository(getContext()).load("durable"));
        assertTrue(coaching.strength().isEmpty());
        assertEquals(1, gym.unfinished().get(0).actualSetCount());
        assertEquals(
                65.0,
                gym.load("durable").execution().actualSets().get(0).actual().kilograms(),
                0.0);
        assertEquals(
                80.0,
                gym.load("durable")
                        .execution()
                        .prescription()
                        .slots()
                        .get(0)
                        .target()
                        .referenceLoadKg(),
                0.0);
        try (Cursor c =
                db().rawQuery("SELECT header FROM strength_executions WHERE id='durable'", null)) {
            assertTrue(c.moveToFirst());
            var header = FeatureJson.object(c.getString(0));
            assertFalse(header.has("actualSets"));
            assertFalse(header.has("events"));
        }
    }

    public void testLostAcknowledgementAndStaleRevisionNeverRewriteActuals() {
        var engine = custom("retry");
        var initial = engine.snapshot();
        assertEquals(gym.start(initial), gym.start(initial));
        engine.saveSet("bench", set("actual", 7, 65), tick(1000));
        var saved = gym.save(engine.snapshot(), 1);
        assertEquals(saved, gym.save(engine.snapshot(), 1));
        var other = custom("retry");
        other.saveSet("bench", set("other", 8, 70), tick(1000));
        rejects(() -> gym.save(other.snapshot(), 1));
        rejects(() -> gym.save(other.snapshot(), saved.revision()));
        assertEquals(saved, gym.load("retry"));
        assertEquals(1, count("strength_execution_sets"));
        assertEquals(2, count("strength_execution_events"));
    }

    public void testUnfinishedWorkoutAndCompletedManualIdentityRejectNewStaging() {
        gym.start(custom("first").snapshot());
        rejects(() -> gym.start(custom("second").snapshot()));
        assertEquals(1, count("strength_executions"));
        assertEquals(1, count("strength_workout_versions"));
        gym.save(
                GymExecutionEngine.recover(gym.load("first").execution(), tick(1000))
                        .discard(tick(1000)),
                1);
        coaching.saveStrength(manual("manual-existing", "bench-press", 65));
        rejects(() -> gym.start(custom("manual-existing").snapshot()));
        assertNull(gym.load("manual-existing"));
        assertEquals(1, coaching.strength().size());
    }

    public void testPausedProcessRecoveryKeepsKnownTimeAndSets() {
        var engine = custom("paused");
        gym.start(engine.snapshot());
        engine.saveSet("bench", set("actual", 6, 65), tick(1000));
        engine.pause(tick(2000));
        var paused = gym.save(engine.snapshot(), 1);
        var recovered =
                GymExecutionEngine.recover(
                        gym.load("paused").execution(),
                        new GymExecution.Tick("after-reboot", 50, NOW.plusSeconds(10000)));
        assertEquals(GymExecution.Status.PAUSED, recovered.snapshot().status());
        assertEquals(2000L, recovered.snapshot().activeMillis());
        var durable = gym.save(recovered.snapshot(), paused.revision());
        recovered.resume(new GymExecution.Tick("after-reboot", 100, NOW.plusSeconds(10001)));
        var result =
                recovered.finish(
                        new GymExecution.Tick("after-reboot", 1100, NOW.plusSeconds(10002)),
                        null,
                        "Recovered with a gap",
                        true);
        var finished = gym.finish(result, durable.revision());
        assertEquals(3000L, finished.execution().activeMillis());
        assertEquals(3050L, finished.execution().recordedElapsedMillis());
        assertEquals(1, finished.execution().actualSets().size());
        assertEquals(Duration.ofSeconds(3), coaching.strength().get(0).duration());
        assertTrue(gym.unfinished().isEmpty());
        assertEquals(finished, gym.load("paused"));
    }

    public void testPlannedFinishRetainsOriginalVersionDespiteCurrentProgrammeChange() {
        var original = programme(1, 80);
        var programmes = new GymProgrammeStore(getContext());
        programmes.save(original);
        var prepared = GymWorkoutAdapter.fromProgramme(original, "planned-session");
        var engine =
                GymExecutionEngine.start(
                        "planned",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        gym.start(engine.snapshot());
        programmes.save(programme(2, 82.5));
        engine.saveSet("slot-0", set("actual", 6, 65), tick(1000));
        var finished = gym.finish(engine.finish(tick(2000), null, "Actuals", false), 1);
        var link = new DevelopmentStore(getContext()).gymLinks().get("planned");
        assertEquals("programme:v1", link.versionKey());
        assertEquals("planned-session", link.prescribedSessionId());
        assertEquals(2, programmes.current().version());
        assertEquals(
                80.0,
                finished.execution().prescription().slots().get(0).target().referenceLoadKg(),
                0.0);
        assertEquals(
                65.0, coaching.strength().get(0).exercises().get(0).sets().get(0).kilograms(), 0.0);
        assertEquals(finished, gym.finish(finished.execution(), 1));
        assertEquals(1, coaching.strength().size());
        assertEquals(1, count("strength_execution_sets"));
    }

    public void testAssociationFailureRollsBackActualAndExecutionTogether() {
        var original = programme(1, 80);
        new GymProgrammeStore(getContext()).save(original);
        var prepared = GymWorkoutAdapter.fromProgramme(original, "planned-session");
        var engine =
                GymExecutionEngine.start(
                        "failure",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        gym.start(engine.snapshot());
        engine.saveSet("slot-0", set("actual", 6, 65), tick(1000));
        var staged = gym.save(engine.snapshot(), 1);
        var finished = engine.finish(tick(2000), null, "Actuals", false);
        db().execSQL(
                        "CREATE TRIGGER reject_gym_association BEFORE INSERT ON coach_evidence WHEN"
                                + " NEW.kind='STRENGTH_LINK' BEGIN SELECT RAISE(ABORT,'injected"
                                + " association failure'); END");
        try {
            gym.finish(finished, staged.revision());
            fail("The injected finalisation failure must abort.");
        } catch (SQLiteException expected) {
            assertTrue(coaching.strength().isEmpty());
            assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
            assertEquals(staged, gym.load("failure"));
        } finally {
            db().execSQL("DROP TRIGGER reject_gym_association");
        }
        assertEquals(finished, gym.finish(finished, staged.revision()).execution());
        assertEquals(1, coaching.strength().size());
    }

    public void testExplicitActualEditPreservesOriginalExecutionAndDeletionDoesNotResurrectIt() {
        var engine = custom("edited");
        gym.start(engine.snapshot());
        engine.saveSet("bench", set("actual", 6, 65), tick(1000));
        var original = gym.finish(engine.finish(tick(2000), null, "Original", true), 1);
        var changed = manual("edited", "bench-press", 62.5);
        coaching.saveStrength(changed);
        assertEquals(original, gym.load("edited"));
        assertEquals(changed, coaching.strength().get(0));
        rejects(() -> gym.finish(original.execution(), 1));
        var backup = new FeatureStore(getContext()).exportUserData();
        new FeatureStore(getContext()).restoreUserData(backup, false);
        assertEquals(original, gym.load("edited"));
        assertEquals(changed, coaching.strength().get(0));
        coaching.deleteStrength("edited");
        assertNull(gym.load("edited"));
        assertTrue(coaching.strength().isEmpty());
        assertEquals(1, count("strength_workout_versions"));
        new FeatureStore(getContext())
                .backupRepository()
                .validateSnapshot(new FeatureStore(getContext()).exportUserData());
    }

    public void testManualRepeatSourceCanBeEditedAndDeletedWithoutChangingFrozenTemplate() {
        var source = manual("source", "bench-press", 65);
        coaching.saveStrength(source);
        var repeat = GymWorkoutAdapter.repeatActual("repeat", 1, NOW, "Repeat", source);
        gym.saveWorkout(repeat.definition(), repeat.source());
        gym.savePreference(new GymExecutionRepository.Preference("repeat", 1, true));
        coaching.saveStrength(manual("source", "dumbbell-row", 24));
        assertEquals(repeat.definition(), gym.workout("repeat", 1));
        var features = new FeatureStore(getContext());
        features.backupRepository().validateSnapshot(features.exportUserData());
        coaching.deleteStrength("source");
        var backup = features.exportUserData();
        features.backupRepository().validateSnapshot(backup);
        features.restoreUserData(backup, false);
        assertEquals(repeat.definition(), gym.workout("repeat", 1));
        assertTrue(gym.preferences().get(0).favourite());
        var engine =
                GymExecutionEngine.start(
                        "repeated-after-delete",
                        GymExecution.Origin.REPEATED,
                        repeat.definition(),
                        repeat.source(),
                        ZoneOffset.UTC,
                        tick(0));
        gym.start(engine.snapshot());
        engine.saveSet("slot-0", set("new-actual", 5, 60), tick(1000));
        gym.finish(engine.finish(tick(2000), null, "Repeated intentions", false), 1);
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        assertEquals("bench-press", coaching.strength().get(0).exercises().get(0).exercise().id());
    }

    public void testForgedRepeatTemplateCannotOverrideAvailableSource() throws Exception {
        var source = manual("source", "bench-press", 65);
        coaching.saveStrength(source);
        var repeat = GymWorkoutAdapter.repeatActual("repeat", 1, NOW, "Repeat", source);
        gym.saveWorkout(repeat.definition(), repeat.source());
        var features = new FeatureStore(getContext());
        JSONObject backup = features.exportUserData();
        var workout =
                backup.getJSONObject("strengthEngine").getJSONArray("workouts").getJSONObject(0);
        var fake =
                new GymWorkoutDefinition(
                        "repeat",
                        1,
                        NOW,
                        "Repeat",
                        Immutable.list(
                                new GymWorkoutDefinition.Slot(
                                        "slot-0", exercise("back-squat"), null, null)));
        workout.put("definition", GymWorkoutJson.encode(fake));
        var proof = workout.getJSONObject("sourceSnapshot");
        proof.put("definition", GymWorkoutJson.encode(fake));
        proof.put("originalSourceSlots", GymWorkoutJson.slots(fake.slots()));
        rejects(() -> features.backupRepository().validateSnapshot(backup));
        assertEquals(repeat.definition(), gym.workout("repeat", 1));
        assertEquals(source, coaching.strength().get(0));
    }

    public void testSummaryAvoidsActualHydrationAndDetailDetectsIndexedCorruption() {
        var engine = custom("summary");
        gym.start(engine.snapshot());
        engine.saveSet("bench", set("actual", 6, 65), tick(1000));
        gym.save(engine.snapshot(), 1);
        db().execSQL(
                        "UPDATE strength_execution_sets SET payload='not JSON' WHERE"
                                + " execution_id='summary'");
        assertEquals(1, gym.unfinished().get(0).actualSetCount());
        rejects(() -> gym.load("summary"));
        db().execSQL(
                        "UPDATE strength_execution_sets SET payload=? WHERE execution_id='summary'",
                        new Object[] {
                            GymExecutionJson.encode(engine.snapshot())
                                    .optJSONArray("actualSets")
                                    .opt(0)
                                    .toString()
                        });
        db().execSQL("UPDATE strength_executions SET active_millis=999 WHERE id='summary'");
        rejects(() -> gym.load("summary"));
    }

    public void testConcurrentReadsSeeCoherentHeadersAndAppendRows() throws Exception {
        var engine = custom("concurrent");
        gym.start(engine.snapshot());
        var workers = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var writer =
                    workers.submit(
                            () -> {
                                start.await();
                                long revision = 1;
                                for (int i = 0; i < 40; i++) {
                                    engine.saveSet("bench", set("set-" + i, 6, 65), tick(i + 1));
                                    revision = gym.save(engine.snapshot(), revision).revision();
                                }
                                return revision;
                            });
            var reader =
                    workers.submit(
                            () -> {
                                start.await();
                                for (int i = 0; i < 80; i++) {
                                    var snapshot =
                                            new SqliteGymExecutionRepository(getContext())
                                                    .load("concurrent");
                                    assertNotNull(snapshot);
                                    assertEquals(
                                            snapshot.execution().actualSets().size() + 1,
                                            snapshot.execution().events().size());
                                }
                                return true;
                            });
            start.countDown();
            assertEquals(41L, writer.get(30, TimeUnit.SECONDS).longValue());
            assertTrue(reader.get(30, TimeUnit.SECONDS));
            assertEquals(40, gym.load("concurrent").execution().actualSets().size());
        } finally {
            workers.shutdownNow();
        }
    }

    public void testOversizedFinishedActualLeavesAcceptedSetsRecoverable() {
        var engine =
                GymExecutionEngine.start(
                        "large", GymExecution.Origin.FREE, null, null, ZoneOffset.UTC, tick(0));
        var initial = engine.snapshot();
        gym.start(initial);
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        var sets = new ArrayList<GymExecutionSet>();
        var events = new ArrayList<>(initial.events());
        events.add(
                new GymExecution.Event(
                        1, GymExecution.EventType.EXERCISES_ADDED, "slot-0", null, 0, 0, NOW));
        for (int block = 0; block < 10; block++) {
            var slot =
                    new GymWorkoutDefinition.Slot(
                            "slot-" + block, exercise("bench-press"), null, null);
            slots.add(slot);
            for (int i = 0; i < 100; i++) {
                String setId = "set-" + block + "-" + i + "-" + repeat('x', 230);
                var actual = set(setId, 6, 65);
                sets.add(new GymExecutionSet(slot.id(), slot.exercise(), actual));
                events.add(
                        new GymExecution.Event(
                                events.size(),
                                GymExecution.EventType.SET_SAVED,
                                slot.id(),
                                setId,
                                sets.size(),
                                sets.size(),
                                NOW.plusMillis(sets.size())));
            }
        }
        var staged =
                new GymExecution(
                        "large",
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        NOW,
                        ZoneOffset.UTC,
                        GymExecution.Status.ACTIVE,
                        tick(1000),
                        1000,
                        1000,
                        "slot-0",
                        slots,
                        sets,
                        events,
                        null,
                        false);
        var saved = gym.save(staged, 1);
        var recovered = GymExecutionEngine.recover(staged, tick(2000));
        recovered.resume(tick(2001));
        var finished = recovered.finish(tick(2002), null, "Accepted staged evidence", false);
        rejects(() -> gym.finish(finished, saved.revision()));
        assertEquals(saved, gym.load("large"));
        assertEquals(1000, count("strength_execution_sets"));
        assertTrue(coaching.strength().isEmpty());
        // Legacy/manual logging retains its pre-existing valid capacity independently of staging.
        rejects(() -> coaching.saveStrength(finished.completedSession()));
        var legacy =
                new StrengthSession(
                        "independent-large-legacy",
                        NOW,
                        ZoneOffset.UTC,
                        finished.completedSession().duration(),
                        null,
                        "Legacy actual capacity",
                        finished.completedSession().exercises());
        coaching.saveStrength(legacy);
        assertEquals(legacy, coaching.strength().get(0));
    }

    private GymExecutionEngine custom(String id) {
        var target =
                new GymWorkoutDefinition.Target(
                        2,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RPE, 6, 8),
                        90,
                        80.0);
        var definition =
                new GymWorkoutDefinition(
                        "workout-" + id,
                        1,
                        NOW,
                        "Upper",
                        Immutable.list(
                                new GymWorkoutDefinition.Slot(
                                        "bench", exercise("bench-press"), target, null)));
        return GymExecutionEngine.start(
                id, GymExecution.Origin.CUSTOM, definition, null, ZoneOffset.UTC, tick(0));
    }

    private static GymProgramme programme(int version, double reference) {
        var day = LocalDate.of(2026, 10, 25);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Immutable.set(DayOfWeek.SUNDAY),
                        1,
                        Immutable.set("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        return new GymProgramme(
                "programme",
                version,
                NOW.plusMillis(version),
                "Athlete chose",
                settings,
                day,
                day.plusDays(41),
                Immutable.list(
                        new GymProgramme.Session(
                                "planned-session",
                                day,
                                "Upper",
                                Immutable.list(
                                        new GymProgramme.Prescription(
                                                "bench-press",
                                                1,
                                                5,
                                                8,
                                                null,
                                                null,
                                                6,
                                                8,
                                                120,
                                                "Preserved",
                                                reference)),
                                "Original")),
                Immutable.list());
    }

    private static StrengthSession manual(String id, String exercise, double load) {
        return new StrengthSession(
                id,
                NOW,
                ZoneOffset.UTC,
                Duration.ofMinutes(30),
                null,
                "User-entered",
                Immutable.list(
                        new StrengthSession.ExerciseSets(
                                exercise(exercise), Immutable.list(set("actual", 6, load)))));
    }

    private static StrengthExercise exercise(String id) {
        return ExerciseCatalog.definition(id).exercise();
    }

    private static StrengthSession.SetEntry set(String id, int reps, double load) {
        return new StrengthSession.SetEntry(id, reps, load, 7.0, false);
    }

    private static GymExecution.Tick tick(long millis) {
        return new GymExecution.Tick("boot", millis, NOW.plusMillis(millis));
    }

    private SQLiteDatabase db() {
        return ProfileDatabase.get(getContext()).getWritableDatabase();
    }

    private int count(String table) {
        try (Cursor c = db().rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, value);
        return new String(chars);
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
            fail("Conflicting or corrupt gym evidence must be rejected.");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
