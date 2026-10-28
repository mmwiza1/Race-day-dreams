package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymExecutionEngine;
import org.racedaydreams.domain.strength.GymNowWorkouts;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.Instant;
import java.time.ZoneOffset;

/** Real one-off selection and actual persistence on API28/35/36, without a running profile. */
@SuppressWarnings("deprecation")
public final class GymNowWorkoutsAndroidTest extends AndroidTestCase {
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    public void testQuickAndGeneratedActualsRemainIndependentOfProgrammes() {
        var request =
                new GymNowWorkouts.Request(
                        45,
                        Immutable.set("bodyweight"),
                        GymNowWorkouts.Focus.FULL_BODY,
                        GymProgramme.Goal.GENERAL,
                        GymProgramme.Experience.BEGINNER,
                        GymProgramme.Priority.STRENGTH_FIRST,
                        GymProgramme.Effort.RIR);
        var suggestion = GymNowWorkouts.generate("one-off-workout", NOW, request);
        var repository = new SqliteGymExecutionRepository(getContext());
        for (var origin :
                new GymExecution.Origin[] {
                    GymExecution.Origin.QUICK, GymExecution.Origin.GENERATED
                }) {
            var engine =
                    GymExecutionEngine.start(
                            "actual-" + origin.name(),
                            origin,
                            suggestion.definition(),
                            null,
                            ZoneOffset.UTC,
                            new GymExecution.Tick("boot", 0, NOW));
            var started = repository.start(engine.snapshot());
            assertTrue(started.execution().actualSets().isEmpty());
            assertNull(started.execution().source());
            var first = suggestion.definition().slots().get(0);
            assertNull(first.target().referenceLoadKg());
            engine.saveSet(
                    first.id(),
                    new StrengthSession.SetEntry("entered", 7, 0, null, false, null, null, 3.0),
                    new GymExecution.Tick("boot", 1000, NOW.plusSeconds(1)));
            var finished =
                    repository.finish(
                            engine.finish(
                                    new GymExecution.Tick("boot", 2000, NOW.plusSeconds(2)),
                                    null,
                                    "Entered actual, not a prescription copy",
                                    true),
                            started.revision());
            assertEquals(origin, finished.execution().origin());
            assertNull(finished.execution().plannedAssociation());
            assertEquals(1, finished.execution().actualSets().size());
            assertEquals(7, finished.execution().actualSets().get(0).actual().reps());
            assertEquals(
                    Double.valueOf(3),
                    finished.execution().actualSets().get(0).actual().repetitionsInReserve());
        }
        assertFalse(new SqliteUserPreferencesRepository(getContext()).load().isPresent());
        assertNull(new GymProgrammeStore(getContext()).current());
        assertTrue(new GymProgrammeStore(getContext()).versions().isEmpty());
        assertTrue(new DevelopmentStore(getContext()).gymLinks().isEmpty());
        assertEquals(2, new CoachingStore(getContext()).strength().size());
        assertTrue(repository.unfinished().isEmpty());
    }
}
