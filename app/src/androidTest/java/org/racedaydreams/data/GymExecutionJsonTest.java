package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionEngine;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutAdapter;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.StrengthExercise;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;

/** Real Android JSON implementation on API28/35/36; no database write precedes validation. */
@SuppressWarnings("deprecation")
public final class GymExecutionJsonTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00.123456789Z");

    public void testWorkoutRoundTripPreservesEveryMeasurementOpenSlotAndSuperset() {
        var slots = new ArrayList<GymWorkoutDefinition.Slot>();
        for (var measurement : StrengthExercise.MeasurementProfile.values()) {
            var exercise = exercise("custom-" + measurement.name(), measurement);
            boolean reps = measurement.usesReps();
            var target =
                    new GymWorkoutDefinition.Target(
                            2,
                            reps ? 5 : null,
                            reps ? 8 : null,
                            measurement.usesDuration() ? 30L : null,
                            measurement.usesDistance() ? 25.5 : null,
                            new GymWorkoutDefinition.EffortRange(
                                    reps
                                            ? GymWorkoutDefinition.EffortScale.RIR
                                            : GymWorkoutDefinition.EffortScale.RPE,
                                    2,
                                    4),
                            90,
                            measurement.usesLoad() ? 12.5 : null);
            slots.add(
                    new GymWorkoutDefinition.Slot(
                            measurement.name(),
                            exercise,
                            target,
                            slots.size() < 2 ? "pair" : null));
        }
        slots.add(
                new GymWorkoutDefinition.Slot(
                        "open",
                        exercise("open", StrengthExercise.MeasurementProfile.REPS_ONLY),
                        null,
                        null));
        var workout = new GymWorkoutDefinition("saved", 2, NOW, "Frozen custom workout", slots);
        assertEquals(workout, GymWorkoutJson.read(GymWorkoutJson.write(workout)));
        assertEquals(workout, GymWorkoutJson.decode(GymWorkoutJson.encode(workout)));
        assertNull(GymWorkoutJson.read(GymWorkoutJson.write(workout)).slots().get(6).target());
        assertEquals(
                "Custom LOAD_REPS",
                GymWorkoutJson.read(GymWorkoutJson.write(workout))
                        .slots()
                        .get(0)
                        .exercise()
                        .name());
    }

    public void testEveryOriginRoundTripsWithoutInventingActualSetsOrAssociation() {
        var definition = workout();
        for (var origin : GymExecution.Origin.values()) {
            GymExecution.SourceReference source = null;
            GymWorkoutDefinition selected = definition;
            if (origin == GymExecution.Origin.FREE) selected = null;
            if (origin == GymExecution.Origin.REPEATED)
                source =
                        new GymExecution.SourceReference(
                                GymExecution.SourceKind.ACTUAL_SESSION, "previous-actual", 0, null);
            if (origin == GymExecution.Origin.FAVOURITE)
                source =
                        new GymExecution.SourceReference(
                                GymExecution.SourceKind.SAVED_WORKOUT,
                                definition.id(),
                                definition.revision(),
                                null);
            if (origin == GymExecution.Origin.PLANNED) {
                var prepared = planned();
                source = prepared.source();
                selected = prepared.definition();
            }
            var before =
                    GymExecutionEngine.start(
                                    "execution-" + origin.name(),
                                    origin,
                                    selected,
                                    source,
                                    ZoneOffset.ofHoursMinutes(5, 30),
                                    tick(0))
                            .snapshot();
            var restored = roundTrip(before);
            assertEquals(before, restored);
            assertTrue(restored.actualSets().isEmpty());
            assertNull(restored.completedSession());
            assertNull(restored.plannedAssociation());
        }
    }

    public void testOriginalTodayOnlyAndActualDefinitionsRemainIndependent() {
        var engine = engine();
        engine.saveSet("slot", new StrengthSession.SetEntry("warm", 5, 20, null, true), tick(1000));
        engine.saveSet(
                "slot",
                new StrengthSession.SetEntry(
                        "original-working", 8, 60, null, false, null, null, 2.0),
                tick(2000));
        var replacement =
                new StrengthExercise(
                        "replacement",
                        "Frozen replacement name",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        true,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        engine.replaceExercise("slot", replacement, tick(3000));
        engine.saveSet(
                "slot",
                new StrengthSession.SetEntry("replacement-working", 6, 15, 8.0, false),
                tick(4000));
        var finished =
                engine.finish(tick(5000), null, "Recorded actuals, not suggested loading", true);
        var restored = roundTrip(finished);
        assertEquals(finished, restored);
        assertEquals("Frozen bench name", restored.prescription().slots().get(0).exercise().name());
        assertEquals("Frozen replacement name", restored.slots().get(0).exercise().name());
        assertEquals("Frozen bench name", restored.actualSets().get(0).exercise().name());
        assertEquals("Frozen replacement name", restored.actualSets().get(2).exercise().name());
        assertNull(restored.slots().get(0).target().referenceLoadKg());
        assertEquals(80.0, restored.prescription().slots().get(0).target().referenceLoadKg(), 0);
        assertEquals(15.0, restored.actualSets().get(2).actual().kilograms(), 0);
        assertTrue(restored.actualSets().get(0).actual().warmUp());
        assertNull(restored.actualSets().get(1).actual().rpe());
        assertEquals(
                Double.valueOf(2), restored.actualSets().get(1).actual().repetitionsInReserve());
        assertTrue(restored.partial());
    }

    public void testPausedAndInterruptedClocksRoundTripWithoutAddingGapTime() {
        var engine = engine();
        engine.saveSet(
                "slot", new StrengthSession.SetEntry("working", 8, 60, 8.0, false), tick(1000));
        engine.pause(tick(2000));
        var paused = roundTrip(engine.snapshot());
        assertEquals(GymExecution.Status.PAUSED, paused.status());
        var recovered =
                GymExecutionEngine.recover(
                        paused,
                        new GymExecution.Tick(
                                "new-boot", Long.MAX_VALUE - 10000, NOW.plusSeconds(200000)));
        assertEquals(recovered.snapshot(), roundTrip(recovered.snapshot()));
        assertEquals(GymExecution.Status.PAUSED, recovered.snapshot().status());
        recovered.resume(
                new GymExecution.Tick("new-boot", Long.MAX_VALUE - 9000, NOW.minusSeconds(1000)));
        recovered.observe(
                new GymExecution.Tick("new-boot", Long.MAX_VALUE - 8000, NOW.minusSeconds(2000)));
        var active = roundTrip(recovered.snapshot());
        assertEquals(4000L, active.recordedElapsedMillis());
        assertEquals(3000L, active.activeMillis());
        var interrupted =
                GymExecutionEngine.recover(active, new GymExecution.Tick("third-boot", 1, NOW));
        assertEquals(
                GymExecution.Status.RECOVERY_REQUIRED, roundTrip(interrupted.snapshot()).status());
        assertEquals(3000L, roundTrip(interrupted.snapshot()).activeMillis());
    }

    public void testCompletedActualMeasurementTypesAndMissingEffortSurvive() {
        var engine =
                GymExecutionEngine.start(
                        "all-measurements",
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        ZoneOffset.UTC,
                        tick(0));
        int sequence = 0;
        for (var measurement : StrengthExercise.MeasurementProfile.values()) {
            String slot = measurement.name();
            var exercise = exercise(slot, measurement);
            engine.addExercises(
                    Immutable.list(new GymWorkoutDefinition.Slot(slot, exercise, null, null)),
                    tick(++sequence * 1000));
            engine.saveSet(
                    slot,
                    new StrengthSession.SetEntry(
                            "actual-" + slot,
                            measurement.usesReps() ? 7 : 0,
                            measurement.usesLoad() ? 12.5 : 0,
                            null,
                            false,
                            measurement.usesDuration() ? 20L : null,
                            measurement.usesDistance() ? 30.25 : null),
                    tick(++sequence * 1000));
        }
        var finished = engine.finish(tick(++sequence * 1000), null, "", false);
        assertEquals(finished, roundTrip(finished));
        assertEquals(6, roundTrip(finished).completedSession().exercises().size());
        assertNull(roundTrip(finished).completedSession().rpe());
        assertNull(roundTrip(finished).plannedAssociation());
        var discarded = engine();
        discarded.discard(tick(1000));
        assertEquals(discarded.snapshot(), roundTrip(discarded.snapshot()));
    }

    public void testUnknownFutureAndMissingFieldsAreRejectedAtEveryLevel() throws Exception {
        var json = GymWorkoutJson.encode(workout());
        json.put("version", 2);
        reject(() -> GymWorkoutJson.decode(json));
        json.put("version", 1);
        json.put("future", true);
        reject(() -> GymWorkoutJson.decode(json));
        json.remove("future");
        var exercise = json.getJSONArray("slots").getJSONObject(0).getJSONObject("exercise");
        exercise.put("future", true);
        reject(() -> GymWorkoutJson.decode(json));
        exercise.remove("future");
        exercise.remove("measurement");
        reject(() -> GymWorkoutJson.decode(json));
        var execution = GymExecutionJson.encode(engine().snapshot());
        execution.remove("completedSession");
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("completedSession", JSONObject.NULL);
        execution.getJSONObject("lastTick").put("extra", 1);
        reject(() -> GymExecutionJson.decode(execution));
    }

    public void testNumbersBooleansAndNullsAreNeverCoerced() throws Exception {
        var execution = GymExecutionJson.encode(engine().snapshot());
        execution.put("recordedElapsedMillis", "0");
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("recordedElapsedMillis", 0.5);
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("recordedElapsedMillis", 0L);
        execution.getJSONObject("lastTick").put("monotonicMillis", 1.0);
        reject(() -> GymExecutionJson.decode(execution));
        execution.getJSONObject("lastTick").put("monotonicMillis", 0L);
        execution.put("partial", "false");
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("partial", false);
        execution.put("origin", "UNKNOWN");
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("origin", GymExecution.Origin.CUSTOM.name());
        execution.put("startedAt", "not-a-timestamp");
        reject(() -> GymExecutionJson.decode(execution));
        var workout = GymWorkoutJson.encode(workout());
        workout.put("revision", 2147483648L);
        reject(() -> GymWorkoutJson.decode(workout));
        workout.put("revision", 1);
        var target = workout.getJSONArray("slots").getJSONObject(0).getJSONObject("target");
        target.put("referenceLoadKg", "80");
        reject(() -> GymWorkoutJson.decode(workout));
        target.put("referenceLoadKg", JSONObject.NULL);
        target.remove("minimumReps");
        reject(() -> GymWorkoutJson.decode(workout));
    }

    public void testCorruptEventHistoryAndForgedCompletionAreRejected() throws Exception {
        var engine = engine();
        engine.saveSet("slot", new StrengthSession.SetEntry("one", 8, 60, 8.0, false), tick(1000));
        var saved = GymExecutionJson.encode(engine.snapshot());
        saved.getJSONArray("events").getJSONObject(1).put("actualSetId", "not-recorded");
        reject(() -> GymExecutionJson.decode(saved));
        var badSequence = GymExecutionJson.encode(engine.snapshot());
        badSequence.getJSONArray("events").getJSONObject(1).put("sequence", 0);
        reject(() -> GymExecutionJson.decode(badSequence));
        var finished = GymExecutionJson.encode(engine.finish(tick(2000), null, "", false));
        finished.getJSONObject("completedSession")
                .getJSONArray("exercises")
                .getJSONObject(0)
                .getJSONArray("sets")
                .getJSONObject(0)
                .put("kilograms", 80);
        reject(() -> GymExecutionJson.decode(finished));
        var partialEngine = engine();
        partialEngine.replaceExercise(
                "slot",
                exercise("different", StrengthExercise.MeasurementProfile.LOAD_REPS),
                tick(1000));
        partialEngine.saveSet(
                "slot", new StrengthSession.SetEntry("actual", 6, 15, null, false), tick(2000));
        var partial = GymExecutionJson.encode(partialEngine.finish(tick(3000), null, "", true));
        partial.put("partial", false);
        reject(() -> GymExecutionJson.decode(partial));
    }

    public void testSourceIdentityChecksDoNotResolveCurrentCatalog() throws Exception {
        var prepared = planned();
        var engine =
                GymExecutionEngine.start(
                        "planned",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick(0));
        assertEquals(engine.snapshot(), roundTrip(engine.snapshot()));
        var badSource = GymExecutionJson.encode(engine.snapshot());
        badSource.getJSONObject("source").put("revision", 2);
        reject(() -> GymExecutionJson.decode(badSource));
        var badSession = GymExecutionJson.encode(engine.snapshot());
        badSession.getJSONObject("source").put("sessionId", "different");
        reject(() -> GymExecutionJson.decode(badSession));
        var frozen = GymWorkoutJson.encode(workout());
        frozen.getJSONArray("slots")
                .getJSONObject(0)
                .getJSONObject("exercise")
                .put("id", "canonical-no-longer-installed");
        assertEquals(
                "canonical-no-longer-installed",
                GymWorkoutJson.decode(frozen).slots().get(0).exercise().id());
    }

    public void testArrayBoundsAndDuplicateSlotsRejectBeforePersistence() throws Exception {
        var definition = GymWorkoutJson.encode(workout());
        JSONObject slot = definition.getJSONArray("slots").getJSONObject(0);
        JSONArray slots = new JSONArray();
        for (int i = 0; i < 101; i++) slots.put(slot);
        definition.put("slots", slots);
        reject(() -> GymWorkoutJson.decode(definition));
        definition.put("slots", new JSONArray().put(slot).put(slot));
        reject(() -> GymWorkoutJson.decode(definition));
        var execution = GymExecutionJson.encode(engine().snapshot());
        JSONArray values = new JSONArray();
        for (int i = 0; i < 10001; i++) values.put(JSONObject.NULL);
        execution.put("actualSets", values);
        reject(() -> GymExecutionJson.decode(execution));
        execution.put("actualSets", new JSONArray());
        values = new JSONArray();
        for (int i = 0; i < 50001; i++) values.put(JSONObject.NULL);
        execution.put("events", values);
        reject(() -> GymExecutionJson.decode(execution));
    }

    public void testMalformedLenientDuplicateAndDeepJsonAreRejected() {
        String[] invalid = {
            "{} trailing",
            "/* comment */ {}",
            "{version:1}",
            "{'version':1}",
            "{\"version\":1,\"version\":1}",
            "{\"version\":1,\"\\u0076ersion\":1}",
            "{\"a\":01}",
            "{\"a\":1,}",
            "{\"a\":[1,]}",
            "{\"a\":NaN}",
            "{\"a\":1e}",
            "{\"a\":\"bad\ntext\"}",
            "{\"a\":\"\\x\"}",
            "{\"a\":false;\"b\":1}"
        };
        for (String payload : invalid) reject(() -> GymExecutionJson.read(payload));
        StringBuilder deep = new StringBuilder("{\"a\":");
        for (int i = 0; i < 30; i++) deep.append('[');
        deep.append('0');
        for (int i = 0; i < 30; i++) deep.append(']');
        deep.append('}');
        reject(() -> GymExecutionJson.read(deep.toString()));
    }

    public void testPayloadAndStringResourceLimitsAreEnforced() throws Exception {
        char[] tooLarge = new char[GymJsonSupport.MAX_BYTES + 1];
        java.util.Arrays.fill(tooLarge, ' ');
        String payload = new String(tooLarge);
        reject(() -> GymExecutionJson.read(payload));
        var json = GymWorkoutJson.encode(workout());
        char[] title = new char[121];
        java.util.Arrays.fill(title, 'a');
        json.put("title", new String(title));
        reject(() -> GymWorkoutJson.decode(json));
        var invalidRow = GymWorkoutJson.encode(workout());
        invalidRow.getJSONArray("slots").put(0, JSONObject.NULL);
        reject(() -> GymWorkoutJson.decode(invalidRow));
    }

    private static GymWorkoutDefinition workout() {
        var exercise =
                new StrengthExercise(
                        "bench-press",
                        "Frozen bench name",
                        StrengthExercise.Region.UPPER,
                        StrengthExercise.Mode.WEIGHTED,
                        true,
                        StrengthExercise.MeasurementProfile.LOAD_REPS);
        var target =
                new GymWorkoutDefinition.Target(
                        1,
                        5,
                        8,
                        null,
                        null,
                        new GymWorkoutDefinition.EffortRange(
                                GymWorkoutDefinition.EffortScale.RPE, 6, 8),
                        120,
                        80.0);
        return new GymWorkoutDefinition(
                "saved",
                1,
                NOW,
                "Frozen workout",
                Immutable.list(new GymWorkoutDefinition.Slot("slot", exercise, target, null)));
    }

    private static StrengthExercise exercise(
            String id, StrengthExercise.MeasurementProfile measurement) {
        return new StrengthExercise(
                id,
                "Custom " + measurement.name(),
                StrengthExercise.Region.FULL,
                measurement.usesLoad()
                        ? StrengthExercise.Mode.WEIGHTED
                        : StrengthExercise.Mode.BODYWEIGHT,
                false,
                measurement);
    }

    private static GymWorkoutAdapter.PreparedWorkout planned() {
        var start = LocalDate.of(2026, 10, 27);
        var settings =
                new GymProgramme.Settings(
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Immutable.set(DayOfWeek.TUESDAY),
                        1,
                        Immutable.set("full gym"),
                        45,
                        GymProgramme.Effort.RPE);
        var programme =
                new GymProgramme(
                        "retained-programme",
                        1,
                        NOW,
                        "Athlete choice",
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
                                                        "",
                                                        80.0)),
                                        "")),
                        Immutable.list());
        return GymWorkoutAdapter.fromProgramme(programme, "retained-session");
    }

    private static GymExecutionEngine engine() {
        return GymExecutionEngine.start(
                "execution", GymExecution.Origin.CUSTOM, workout(), null, ZoneOffset.UTC, tick(0));
    }

    private static GymExecution.Tick tick(long millis) {
        return new GymExecution.Tick("boot", millis, NOW.plusMillis(millis));
    }

    private static GymExecution roundTrip(GymExecution before) {
        return GymExecutionJson.read(GymExecutionJson.write(before));
    }

    private static void reject(Runnable operation) {
        try {
            operation.run();
            fail("Invalid gym payload was accepted.");
        } catch (IllegalArgumentException expected) {
            /* Rejected before any persistence mutation. */
        }
    }
}
