package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionEngine;
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

/** Invoke the real domain runtime on every supported Android API, including API28. No DB or UI. */
@SuppressWarnings("deprecation")
public final class GymExecutionAndroidTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    public void testLegacyPrescriptionActualsAndRecoveryRemainIndependent() {
        var day = LocalDate.of(2026, 10, 27);
        var preferences =
                new GymProgramme.Settings(
                        GymProgramme.Goal.STRENGTH,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Experience.INTERMEDIATE,
                        Immutable.set(DayOfWeek.TUESDAY),
                        1,
                        Immutable.set("full gym"),
                        45,
                        GymProgramme.Effort.RIR);
        var prescription =
                new GymProgramme.Prescription(
                        "bench-press", 2, 5, 8, null, null, 6, 8, 120, "Preserved target", 80.0);
        var programme =
                new GymProgramme(
                        "android-runtime-programme",
                        1,
                        NOW,
                        "Athlete chose",
                        preferences,
                        day,
                        day.plusDays(41),
                        Immutable.list(
                                new GymProgramme.Session(
                                        "android-runtime-session",
                                        day,
                                        "Upper",
                                        Immutable.list(prescription),
                                        "Preserved")),
                        Immutable.list());
        var prepared = GymWorkoutAdapter.fromProgramme(programme, "android-runtime-session");
        assertEquals(
                GymWorkoutDefinition.EffortScale.RIR,
                prepared.definition().slots().get(0).target().effort().scale());
        assertEquals(2.0, prepared.definition().slots().get(0).target().effort().minimum(), 0.0);
        var engine =
                GymExecutionEngine.start(
                        "android-runtime-execution",
                        GymExecution.Origin.PLANNED,
                        prepared.definition(),
                        prepared.source(),
                        ZoneOffset.UTC,
                        tick("boot", 0));
        assertTrue(engine.snapshot().actualSets().isEmpty());
        engine.saveSet(
                "slot-0",
                new StrengthSession.SetEntry("warm", 5, 20, null, true),
                tick("boot", 1000));
        engine.saveSet(
                "slot-0",
                new StrengthSession.SetEntry("working-one", 7, 65, 8.0, false),
                tick("boot", 2000));
        assertNull(engine.snapshot().completedSession());
        assertEquals(1, engine.workingSets("slot-0"));
        engine.pause(tick("boot", 3000));
        engine.resume(tick("boot", 63000));
        engine.observe(tick("boot", 64000));
        var staged = engine.snapshot();
        var recovered =
                GymExecutionEngine.recover(
                        staged,
                        new GymExecution.Tick("after-reboot", 50, NOW.plus(Duration.ofHours(4))));
        assertEquals(GymExecution.Status.RECOVERY_REQUIRED, recovered.snapshot().status());
        assertEquals(4000L, recovered.snapshot().activeMillis());
        assertEquals(staged.actualSets(), recovered.snapshot().actualSets());
        recovered.resume(tick("after-reboot", 100));
        recovered.saveSet(
                "slot-0",
                new StrengthSession.SetEntry("working-two", 6, 62.5, null, false, null, null, 2.0),
                tick("after-reboot", 1100));
        var finished = recovered.finish(tick("after-reboot", 2100), null, "Actual workout", false);
        assertEquals(66000L, finished.recordedElapsedMillis());
        assertEquals(6000L, finished.activeMillis());
        assertEquals(Duration.ofSeconds(6), finished.completedSession().duration());
        assertEquals(3, finished.completedSession().exercises().get(0).sets().size());
        assertEquals(
                65.0,
                finished.completedSession().exercises().get(0).sets().get(1).kilograms(),
                0.0);
        assertEquals(
                62.5,
                finished.completedSession().exercises().get(0).sets().get(2).kilograms(),
                0.0);
        assertEquals(80.0, finished.prescription().slots().get(0).target().referenceLoadKg(), 0.0);
        assertEquals(prepared.source(), finished.plannedAssociation());
        assertFalse(finished.partial());
        assertEquals(1, programme.version());
    }

    public void testOpenSpontaneousTasksDoNotBecomeRepTargetsOrPlannedCompletion() {
        var engine =
                GymExecutionEngine.start(
                        "open-runtime-execution",
                        GymExecution.Origin.FREE,
                        null,
                        null,
                        ZoneOffset.UTC,
                        tick("boot", 0));
        var timed =
                new StrengthExercise(
                        "timed-task",
                        "Timed task",
                        StrengthExercise.Region.FULL,
                        StrengthExercise.Mode.BODYWEIGHT,
                        false,
                        StrengthExercise.MeasurementProfile.DURATION);
        var carry =
                new StrengthExercise(
                        "carry-task",
                        "Carry task",
                        StrengthExercise.Region.FULL,
                        StrengthExercise.Mode.WEIGHTED,
                        false,
                        StrengthExercise.MeasurementProfile.LOAD_DISTANCE);
        engine.addExercises(
                Immutable.list(
                        new GymWorkoutDefinition.Slot("timed", timed, null, null),
                        new GymWorkoutDefinition.Slot("carry", carry, null, null)),
                tick("boot", 1000));
        engine.saveSet(
                "timed",
                new StrengthSession.SetEntry("hold", 0, 0, null, false, 20L, null),
                tick("boot", 21000));
        engine.saveSet(
                "carry",
                new StrengthSession.SetEntry("carry-distance", 0, 12, null, false, null, 25.0),
                tick("boot", 29000));
        var finished = engine.finish(tick("boot", 30000), null, "Spontaneous", false);
        assertNull(finished.plannedAssociation());
        assertEquals(2, finished.completedSession().exercises().size());
        assertEquals(
                Long.valueOf(20),
                finished.completedSession().exercises().get(0).sets().get(0).durationSeconds());
        assertEquals(
                25.0,
                finished.completedSession().exercises().get(1).sets().get(0).distanceMetres(),
                0.0);
        assertEquals(0, finished.completedSession().exercises().get(1).sets().get(0).reps());
        assertNull(finished.completedSession().rpe());
        var repeated =
                GymWorkoutAdapter.repeatExecution(
                        "repeated-runtime", 1, NOW, "Repeat exercise structure", finished);
        var next =
                GymExecutionEngine.start(
                        "next-runtime-execution",
                        GymExecution.Origin.REPEATED,
                        repeated.definition(),
                        repeated.source(),
                        ZoneOffset.UTC,
                        tick("boot", 0));
        assertTrue(next.snapshot().actualSets().isEmpty());
        assertNull(next.snapshot().plannedAssociation());
        assertEquals("open-runtime-execution", next.snapshot().source().id());
    }

    private static GymExecution.Tick tick(String clock, long millis) {
        return new GymExecution.Tick(clock, millis, NOW.plusMillis(millis));
    }
}
