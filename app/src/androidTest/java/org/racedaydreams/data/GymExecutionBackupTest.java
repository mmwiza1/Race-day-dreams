package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Log;

import org.json.JSONObject;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionEngine;
import org.racedaydreams.domain.strength.GymExecutionRepository;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutAdapter;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;
import org.racedaydreams.domain.strength.StrengthProgression;
import org.racedaydreams.domain.strength.StrengthSession;
import org.racedaydreams.portability.BackupManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;

/** Real shared SQLite and Android JSON/ZIP restore: corruption never clears retained actuals. */
@SuppressWarnings("deprecation")
public final class GymExecutionBackupTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2025-10-27T12:00:00Z");
    private SqliteGymExecutionRepository gym;
    private SqliteSettingsRepository settings;
    private SqliteBackupRepository backups;
    private BackupManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resetDatabase();
        openAdapters();
    }

    @Override
    protected void tearDown() throws Exception {
        resetDatabase();
        super.tearDown();
    }

    public void testCurrentEnvelopeRoundTripRetainsFrozenSourcesActualsEventsAndFavourites()
            throws Exception {
        var finished = finishPlanned();
        gym.saveWorkout(workout(), null);
        gym.savePreference(new GymExecutionRepository.Preference("saved", 1, true));
        settings.putSetting("retained", "before export");
        byte[] bytes = export();
        JSONObject envelope = object(bytes);
        assertEquals(9, envelope.getInt("version"));
        assertEquals(
                2,
                envelope.getJSONObject("data").getJSONObject("strengthEngine").getInt("version"));
        settings.putSetting("retained", "temporary");
        manager.restoreFrom(new ByteArrayInputStream(bytes));
        assertEquals("before export", settings.getSetting("retained"));
        assertEquals(finished.execution(), gym.load("planned-actual").execution());
        assertEquals(finished.revision(), gym.load("planned-actual").revision());
        assertEquals(workout(), gym.workout("saved", 1));
        assertEquals(
                Immutable.list(new GymExecutionRepository.Preference("saved", 1, true)),
                gym.preferences());
        assertEquals(
                65.0,
                new CoachingStore(getContext())
                        .strength()
                        .get(0)
                        .exercises()
                        .get(0)
                        .sets()
                        .get(0)
                        .kilograms(),
                0);
        var link = new DevelopmentStore(getContext()).gymLinks().get("planned-actual");
        assertEquals("retained-programme:v1", link.versionKey());
        assertEquals("retained-session", link.prescribedSessionId());
    }

    public void testAllOlderEnvelopesIncludingPreviousSevenRestoreWithoutGymEngineSection()
            throws Exception {
        var finished = finishPlanned();
        JSONObject old = object(export());
        old.getJSONObject("data").remove("strengthEngine");
        for (int version = 1; version <= 7; version++) {
            gym.saveWorkout(workout(), null);
            settings.putSetting("after legacy export", "discard this replace-only setting");
            old.put("version", version);
            var summary = manager.restoreFrom(input(old));
            assertEquals(version, summary.version());
            assertEquals(
                    finished.execution().completedSession(),
                    new CoachingStore(getContext()).strength().get(0));
            assertNull(gym.load("planned-actual"));
            assertTrue(gym.preferences().isEmpty());
            assertNull(settings.getSetting("after legacy export"));
            assertEquals(
                    "retained-programme:v1",
                    new DevelopmentStore(getContext())
                            .gymLinks()
                            .get("planned-actual")
                            .versionKey());
        }
    }

    public void testMissingFutureOrCorruptSectionCannotReplaceCurrentData() throws Exception {
        var before = finishCustom("retained", 1, "Actual note");
        byte[] bytes = export();
        settings.putSetting("sentinel", "must survive rejection");
        var missing = object(bytes);
        missing.getJSONObject("data").remove("strengthEngine");
        reject(missing);
        for (int version = 7; version <= 8; version++) {
            var missingChoices = object(bytes);
            missingChoices.put("version", version);
            missingChoices.getJSONObject("data").remove("release08");
            reject(missingChoices);
        }
        var future = object(bytes);
        future.getJSONObject("data").getJSONObject("strengthEngine").put("version", 3);
        reject(future);
        var corrupt = object(bytes);
        corrupt.getJSONObject("data")
                .getJSONObject("strengthEngine")
                .getJSONArray("executions")
                .getJSONObject(0)
                .getJSONObject("snapshot")
                .getJSONArray("actualSets")
                .getJSONObject(0)
                .getJSONObject("actual")
                .put("kilograms", "65");
        reject(corrupt);
        var unknown = object(bytes);
        unknown.getJSONObject("data")
                .getJSONObject("strengthEngine")
                .put("futureCollection", new JSONObject());
        reject(unknown);
        assertEquals("must survive rejection", settings.getSetting("sentinel"));
        assertEquals(before.execution(), gym.load("retained").execution());
        assertEquals(
                before.execution().completedSession(),
                new CoachingStore(getContext()).strength().get(0));
    }

    public void testUnfinishedActualsAreExportedButDestinationWorkoutBlocksBothRestores()
            throws Exception {
        var engine = engine("unfinished");
        var initial = gym.start(engine.snapshot());
        engine.saveSet(
                "slot", new StrengthSession.SetEntry("actual", 7, 60, null, false), tick(1000));
        engine.pause(tick(2000));
        var staged = gym.save(engine.snapshot(), initial.revision());
        byte[] json = export();
        var completeOutput = new ByteArrayOutputStream();
        new CompleteBackup(getContext()).exportTo(completeOutput);
        settings.putSetting("sentinel", "active stage retained");
        reject(object(json));
        try {
            new CompleteBackup(getContext())
                    .restoreFrom(new ByteArrayInputStream(completeOutput.toByteArray()));
            fail("Complete restore must not replace an unfinished gym workout.");
        } catch (IllegalArgumentException expected) {
            /* Before raw or app replacement. */
        }
        assertEquals(staged.execution(), gym.load("unfinished").execution());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
        assertEquals("active stage retained", settings.getSetting("sentinel"));

        engine.discard(tick(3000));
        gym.save(engine.snapshot(), staged.revision());
        manager.restoreFrom(new ByteArrayInputStream(json));
        var restored = gym.load("unfinished").execution();
        assertEquals(staged.execution(), restored);
        assertEquals(GymExecution.Status.PAUSED, restored.status());
        var recovery =
                GymExecutionEngine.recover(
                        restored,
                        new GymExecution.Tick("after-reboot", 1, NOW.plusSeconds(100000)));
        assertEquals(GymExecution.Status.PAUSED, recovery.snapshot().status());
        assertEquals(2000L, recovery.snapshot().activeMillis());
        assertEquals(1, recovery.snapshot().actualSets().size());
    }

    public void testCompletePackageRoundTripIncludesGymActualsAndPreferences() throws Exception {
        var finished = finishCustom("complete-actual", 2, "Keep actual evidence");
        gym.savePreference(new GymExecutionRepository.Preference("saved", 1, true));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new CompleteBackup(getContext()).exportTo(output);
        resetDatabase();
        openAdapters();
        new CompleteBackup(getContext())
                .restoreFrom(new ByteArrayInputStream(output.toByteArray()));
        assertEquals(finished.execution(), gym.load("complete-actual").execution());
        assertEquals(workout(), gym.workout("saved", 1));
        assertEquals(
                Immutable.list(new GymExecutionRepository.Preference("saved", 1, true)),
                gym.preferences());
        assertEquals(
                finished.execution().completedSession(),
                new CoachingStore(getContext()).strength().get(0));
        assertTrue(gym.unfinished().isEmpty());
    }

    public void testSqlFailureRollsBackAllGymAndExistingFeatureReplacement() throws Exception {
        var before = finishCustom("rollback", 1, "Retained actuals");
        byte[] bytes = export();
        settings.putSetting("sentinel", "before failed restore");
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER gym_backup_failure BEFORE INSERT ON strength_execution_events "
                        + "BEGIN SELECT RAISE(ABORT, 'injected gym restore failure'); END");
        try {
            manager.restoreFrom(new ByteArrayInputStream(bytes));
            fail("An execution insert failure must roll back the whole replacement.");
        } catch (SQLiteException expected) {
            assertEquals(before.execution(), gym.load("rollback").execution());
            assertEquals(
                    before.execution().completedSession(),
                    new CoachingStore(getContext()).strength().get(0));
            assertEquals("before failed restore", settings.getSetting("sentinel"));
        } finally {
            db.execSQL("DROP TRIGGER gym_backup_failure");
        }
    }

    public void testChangedPrescriptionOrFinishedActualCannotBypassRetainedSourceValidation()
            throws Exception {
        var before = finishPlanned();
        byte[] bytes = export();
        settings.putSetting("sentinel", "source graph retained");
        var forgedTarget = object(bytes);
        forgedTarget
                .getJSONObject("data")
                .getJSONObject("strengthEngine")
                .getJSONArray("workouts")
                .getJSONObject(0)
                .getJSONObject("definition")
                .getJSONArray("slots")
                .getJSONObject(0)
                .getJSONObject("target")
                .put("referenceLoadKg", 85.0);
        reject(forgedTarget);
        var forgedActual = object(bytes);
        JSONObject row =
                forgedActual
                        .getJSONObject("data")
                        .getJSONObject("coaching")
                        .getJSONArray("strength_sessions")
                        .getJSONObject(0);
        JSONObject payload = new JSONObject(row.getString("payload"));
        payload.getJSONArray("exercises")
                .getJSONObject(0)
                .getJSONArray("sets")
                .getJSONObject(0)
                .put("kg", 66.0);
        row.put("payload", payload.toString());
        reject(forgedActual);
        assertEquals(before.execution(), gym.load("planned-actual").execution());
        assertEquals("source graph retained", settings.getSetting("sentinel"));
    }

    public void testAcceptedActualUsesStrictNewFieldsWithoutChangingLegacyReaders()
            throws Exception {
        var original = finishCustom("strict-current", 1, "Retained actuals");
        byte[] bytes = export();
        settings.putSetting("sentinel", "strict rejection retained");
        for (int kind = 0; kind < 4; kind++) {
            JSONObject changed = object(bytes);
            JSONObject actual =
                    changed.getJSONObject("data")
                            .getJSONObject("strengthEngine")
                            .getJSONArray("executions")
                            .getJSONObject(0)
                            .getJSONObject("currentActual");
            JSONObject block = actual.getJSONArray("exercises").getJSONObject(0);
            if (kind == 0) block.getJSONObject("exercise").remove("measurement");
            if (kind == 1) actual.put("futureField", true);
            if (kind == 2) block.getJSONArray("sets").getJSONObject(0).put("reps", "6");
            if (kind == 3) block.getJSONArray("sets").getJSONObject(0).remove("rir");
            reject(changed);
            assertEquals(original.execution(), gym.load("strict-current").execution());
            assertEquals("strict rejection retained", settings.getSetting("sentinel"));
        }
    }

    public void testExplicitActualEditRoundTripsWithoutRewritingOriginalExecution()
            throws Exception {
        var original = finishCustom("edited-actual", 1, "Original entered evidence").execution();
        var before = original.completedSession();
        var corrected =
                new StrengthSession(
                        before.id(),
                        before.startedAt(),
                        before.offset(),
                        before.duration(),
                        9.0,
                        "Athlete corrected the actual log",
                        Immutable.list(
                                new StrengthSession.ExerciseSets(
                                        before.exercises().get(0).exercise(),
                                        Immutable.list(
                                                new StrengthSession.SetEntry(
                                                        "corrected-set", 5, 62.5, 9.0, false)))));
        new CoachingStore(getContext()).saveStrength(corrected);
        assertEquals(original, gym.load("edited-actual").execution());
        JSONObject envelope = object(export());
        JSONObject executionRow =
                envelope.getJSONObject("data")
                        .getJSONObject("strengthEngine")
                        .getJSONArray("executions")
                        .getJSONObject(0);
        assertFalse(executionRow.isNull("actualEditedAt"));
        assertEquals(corrected, CoachingJson.strength(executionRow.getJSONObject("currentActual")));
        manager.restoreFrom(input(envelope));
        assertEquals(original, gym.load("edited-actual").execution());
        var current = new CoachingStore(getContext()).strength();
        assertEquals(Immutable.list(corrected), current);
        var pb =
                StrengthProgression.best(
                        StrengthProgression.timeline(current, Immutable.list()),
                        "bench-press",
                        "Load");
        assertNotNull(pb);
        assertEquals(62.5, pb.kilograms(), 0);
        assertEquals(
                65.0,
                gym.load("edited-actual").execution().actualSets().get(0).actual().kilograms(),
                0);
        JSONObject reexported =
                object(export())
                        .getJSONObject("data")
                        .getJSONObject("strengthEngine")
                        .getJSONArray("executions")
                        .getJSONObject(0);
        assertEquals(
                executionRow.getString("actualEditedAt"), reexported.getString("actualEditedAt"));
    }

    public void testNearRowLimitEscapedActualPayloadRemainsExportableAndRestorable()
            throws Exception {
        // Independent of event count: a valid <=256KiB SQL row can exceed256k serialized characters
        // when its quotes/backslashes are escaped inside the coaching graph's payload string.
        StrengthSession actual = null;
        int rawBytes = 0, serializedLength = 0;
        for (int setsPerExercise = 8; setsPerExercise <= 24; setsPerExercise++) {
            actual = largeActual(setsPerExercise);
            String payload = CoachingJson.strength(actual).toString();
            rawBytes = payload.getBytes(StandardCharsets.UTF_8).length;
            serializedLength = new JSONObject().put("payload", payload).toString().length();
            if (serializedLength > 270000) break;
        }
        assertNotNull(actual);
        assertTrue(
                "The fixture must exercise escaped-string expansion beyond the former ceiling.",
                serializedLength > 270000);
        assertTrue(
                "The original typed row must remain within the actual SQLite limit.",
                rawBytes <= SqliteGymExecutionRepository.MAX_ROW_BYTES);

        var initial =
                GymExecutionEngine.start(
                                actual.id(),
                                GymExecution.Origin.FREE,
                                null,
                                null,
                                ZoneOffset.UTC,
                                tick(0))
                        .snapshot();
        var stored = gym.start(initial);
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        var actualSets = new ArrayList<org.racedaydreams.domain.strength.GymExecutionSet>();
        var events = new ArrayList<GymExecution.Event>(initial.events());
        events.add(
                new GymExecution.Event(
                        1,
                        GymExecution.EventType.EXERCISES_ADDED,
                        "slot-0",
                        null,
                        1,
                        1,
                        NOW.plusMillis(1)));
        for (var block : actual.exercises()) {
            String slot = "slot-" + slots.size();
            slots.add(new GymWorkoutDefinition.Slot(slot, block.exercise(), null, null));
            for (var set : block.sets()) {
                actualSets.add(
                        new org.racedaydreams.domain.strength.GymExecutionSet(
                                slot, block.exercise(), set));
                long at = events.size();
                events.add(
                        new GymExecution.Event(
                                events.size(),
                                GymExecution.EventType.SET_SAVED,
                                slot,
                                set.id(),
                                at,
                                at,
                                NOW.plusMillis(at)));
            }
        }
        events.add(
                new GymExecution.Event(
                        events.size(),
                        GymExecution.EventType.WORKOUT_COMPLETED,
                        "slot-0",
                        null,
                        60000,
                        60000,
                        NOW.plusSeconds(60)));
        var finished =
                new GymExecution(
                        actual.id(),
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        NOW,
                        ZoneOffset.UTC,
                        GymExecution.Status.FINISHED,
                        tick(60000),
                        60000,
                        60000,
                        "slot-0",
                        slots,
                        actualSets,
                        events,
                        actual,
                        false);
        gym.finish(finished, stored.revision());
        byte[] backup = export();
        manager.restoreFrom(new ByteArrayInputStream(backup));
        assertEquals(finished, gym.load(actual.id()).execution());
        assertEquals(actual, new CoachingStore(getContext()).strength().get(0));
        Log.i(
                "GymBackupFixture",
                "nearRowRawBytes="
                        + rawBytes
                        + " escapedPayloadCharacters="
                        + serializedLength
                        + " actualSets="
                        + actualSets.size()
                        + " backupBytes="
                        + backup.length);
    }

    public void testBoundedRetainedStrengthHistoryAboveOldEightMiBLimitRoundTrips()
            throws Exception {
        char[] notes = new char[9500];
        Arrays.fill(notes, 'n');
        String actualNotes = new String(notes);
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        long started = SystemClock.elapsedRealtime();
        db.beginTransaction();
        try {
            for (int i = 0; i < 450; i++) finishCustom("history-" + i, 5, actualNotes);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        long heapBeforeExport = usedHeap();
        byte[] bytes = export();
        assertTrue(
                "The retained-history fixture must exceed the old8MiB ceiling.",
                bytes.length > 8 * 1024 * 1024);
        assertTrue(bytes.length <= BackupManager.MAX_BYTES);
        long heapAfterExport = usedHeap();
        manager.restoreFrom(new ByteArrayInputStream(bytes));
        assertEquals(450, new CoachingStore(getContext()).strength().size());
        assertEquals(5, gym.load("history-0").execution().actualSets().size());
        assertEquals(actualNotes, gym.load("history-449").execution().completedSession().notes());
        Log.i(
                "GymBackupFixture",
                "workouts=450 workingSets=2250 bytes="
                        + bytes.length
                        + " elapsedMillis="
                        + (SystemClock.elapsedRealtime() - started)
                        + " heapBeforeExport="
                        + heapBeforeExport
                        + " heapAfterExport="
                        + heapAfterExport
                        + " heapAfterRestore="
                        + usedHeap()
                        + " heapLimit="
                        + Runtime.getRuntime().maxMemory());
    }

    public void testProvablyOversizedHistoryIsRejectedBeforeJsonHydrationOrDestinationWrites()
            throws Exception {
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        var exercise = workout().slots().get(0).exercise();
        char[] title = new char[120];
        Arrays.fill(title, 'w');
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (int index = 0; index < 100; index++) {
            slots.add(
                    new GymWorkoutDefinition.Slot(
                            "retained-slot-" + index,
                            new StrengthExercise(
                                    "retained-exercise-" + index,
                                    new String(title),
                                    exercise.region(),
                                    exercise.mode(),
                                    exercise.estimateOneRepMax(),
                                    exercise.measurement()),
                            workout().slots().get(0).target(),
                            null));
        }
        int rowCount;
        db.beginTransaction();
        try {
            var first = new GymWorkoutDefinition("overflow-0000", 1, NOW, new String(title), slots);
            gym.saveWorkout(first, null);
            long rowBytes;
            try (var cursor =
                    db.rawQuery(
                            "SELECT length(CAST(payload AS BLOB)) FROM strength_workout_versions"
                                    + " WHERE workout_id='overflow-0000'",
                            null)) {
                assertTrue(cursor.moveToFirst());
                rowBytes = cursor.getLong(0);
            }
            assertTrue(rowBytes > 0);
            assertTrue(rowBytes <= SqliteGymExecutionRepository.MAX_ROW_BYTES);
            rowCount = (int) (BackupManager.MAX_BYTES / rowBytes) + 1;
            assertTrue("The fixture must fit the supported workout-count bound.", rowCount < 10000);
            for (int index = 1; index < rowCount; index++) {
                String id = String.format(java.util.Locale.ROOT, "overflow-%04d", index);
                gym.saveWorkout(
                        new GymWorkoutDefinition(id, 1, NOW, new String(title), slots), null);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        long retainedBytes;
        try (var cursor =
                db.rawQuery(
                        "SELECT SUM(length(CAST(payload AS BLOB))),COUNT(*)"
                                + " FROM strength_workout_versions",
                        null)) {
            assertTrue(cursor.moveToFirst());
            retainedBytes = cursor.getLong(0);
            assertEquals(rowCount, cursor.getInt(1));
        }
        assertTrue(retainedBytes > BackupManager.MAX_BYTES);
        settings.putSetting("oversized-export-sentinel", "retained");
        var destination = new ByteArrayOutputStream();
        try {
            manager.exportTo(destination);
            fail("A provably oversized history must be rejected before building the JSON tree.");
        } catch (IOException expected) {
            assertEquals(0, destination.size());
            assertTrue(expected.getCause() instanceof IllegalArgumentException);
            assertTrue(expected.getCause().getMessage().contains("retained gym data"));
        }
        assertEquals("retained", settings.getSetting("oversized-export-sentinel"));
        assertEquals(100, gym.workout("overflow-0000", 1).slots().size());
        String lastId = String.format(java.util.Locale.ROOT, "overflow-%04d", rowCount - 1);
        assertEquals(100, gym.workout(lastId, 1).slots().size());
        try (var cursor = db.rawQuery("SELECT COUNT(*) FROM strength_workout_versions", null)) {
            assertTrue(cursor.moveToFirst());
            assertEquals(rowCount, cursor.getInt(0));
        }
        Log.i(
                "GymBackupFixture",
                "oversizedRetainedBytes="
                        + retainedBytes
                        + " frozenWorkouts="
                        + rowCount
                        + " destinationBytes="
                        + destination.size());
    }

    private GymExecutionRepository.Stored finishCustom(String id, int workingSets, String notes) {
        var engine = engine(id);
        var stored = gym.start(engine.snapshot());
        for (int i = 0; i < workingSets; i++)
            engine.saveSet(
                    "slot",
                    new StrengthSession.SetEntry("set-" + i, 6, 65, 8.0, false),
                    tick((i + 1) * 1000L));
        return gym.finish(
                engine.finish(tick((workingSets + 1) * 1000L), null, notes, false),
                stored.revision());
    }

    private GymExecutionRepository.Stored finishPlanned() {
        var programme = programme();
        new GymProgrammeStore(getContext()).save(programme);
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "retained-session");
        var engine =
                GymExecutionEngine.start(
                        "planned-actual",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        var stored = gym.start(engine.snapshot());
        engine.saveSet(
                "slot-0",
                new StrengthSession.SetEntry("planned-set", 6, 65, 8.0, false),
                tick(1000));
        return gym.finish(
                engine.finish(tick(2000), null, "Actual performance", false), stored.revision());
    }

    private static GymProgramme programme() {
        LocalDate start = LocalDate.of(2025, 10, 27);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Immutable.set(DayOfWeek.MONDAY),
                        1,
                        Immutable.set("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        return new GymProgramme(
                "retained-programme",
                1,
                NOW,
                "Athlete chose",
                settings,
                start,
                start.plusDays(41),
                Immutable.list(
                        new GymProgramme.Session(
                                "retained-session",
                                start,
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
                                                "Retained intent",
                                                80.0)),
                                "Preserved session")),
                Immutable.list());
    }

    private static GymWorkoutDefinition workout() {
        var exercise =
                new StrengthExercise(
                        "bench-press",
                        "Frozen bench",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        true,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        var target = new GymWorkoutDefinition.Target(1, 5, 8, null, null, null, 120, 80.0);
        return new GymWorkoutDefinition(
                "saved",
                1,
                NOW,
                "Saved workout",
                Immutable.list(new GymWorkoutDefinition.Slot("slot", exercise, target, null)));
    }

    private static StrengthSession largeActual(int setsPerExercise) {
        var blocks = new ArrayList<StrengthSession.ExerciseSets>();
        String base =
                "Controlled exercise variation with a retained descriptive name and equipment"
                        + " context ";
        String name = (base + base).substring(0, 120);
        StringBuilder notes = new StringBuilder();
        while (notes.length() < 9500) notes.append("Keep \"steady\" \\ controlled; ");
        for (int exerciseIndex = 0; exerciseIndex < 100; exerciseIndex++) {
            var exercise =
                    new StrengthExercise(
                            "custom-row-" + exerciseIndex,
                            name,
                            StrengthExercise.Region.UPPER,
                            StrengthExercise.Mode.WEIGHTED,
                            true,
                            StrengthExercise.MeasurementProfile.LOAD_REPS);
            var sets = new ArrayList<StrengthSession.SetEntry>();
            for (int setIndex = 0; setIndex < setsPerExercise; setIndex++)
                sets.add(
                        new StrengthSession.SetEntry(
                                "actual-" + exerciseIndex + "-" + setIndex, 5, 65, 8.0, false));
            blocks.add(new StrengthSession.ExerciseSets(exercise, sets));
        }
        return new StrengthSession(
                "near-row-limit",
                NOW,
                ZoneOffset.UTC,
                Duration.ofMinutes(1),
                7.0,
                notes.substring(0, 9500),
                blocks);
    }

    private static GymExecutionEngine engine(String id) {
        return GymExecutionEngine.start(
                id, GymExecution.Origin.CUSTOM, workout(), null, ZoneOffset.UTC, tick(0));
    }

    private static GymExecution.Tick tick(long millis) {
        return new GymExecution.Tick("boot", millis, NOW.plusMillis(millis));
    }

    private static long usedHeap() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private byte[] export() throws IOException {
        var output = new ByteArrayOutputStream();
        manager.exportTo(output);
        return output.toByteArray();
    }

    private static JSONObject object(byte[] bytes) throws Exception {
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }

    private static ByteArrayInputStream input(JSONObject json) {
        return new ByteArrayInputStream(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void reject(JSONObject envelope) throws Exception {
        try {
            manager.restoreFrom(input(envelope));
            fail("Invalid or unsafe backup must not replace current data.");
        } catch (IOException expected) {
            /* Typed validation or destination protection rejected it. */
        }
    }

    private void openAdapters() {
        gym = new SqliteGymExecutionRepository(getContext());
        settings = new SqliteSettingsRepository(getContext());
        backups = new SqliteBackupRepository(getContext());
        manager = new BackupManager(getContext(), backups);
    }

    private void resetDatabase() {
        ProfileDatabase.get(getContext()).close();
        getContext().deleteDatabase(ProfileDatabase.NAME);
    }
}
