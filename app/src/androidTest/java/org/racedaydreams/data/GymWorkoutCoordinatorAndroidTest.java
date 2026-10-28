package org.racedaydreams.data;

import android.test.AndroidTestCase;

import org.racedaydreams.domain.coaching.Immutable;
import org.racedaydreams.domain.strength.ExerciseCatalog;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymWorkoutDefinition;
import org.racedaydreams.domain.strength.GymWorkoutSessionCoordinator;
import org.racedaydreams.domain.strength.StrengthSession;

import java.time.Instant;
import java.time.ZoneOffset;

/** Real coordinator + shared SQLite finalisation/recovery smoke on every supported Android API. */
@SuppressWarnings("deprecation")
public final class GymWorkoutCoordinatorAndroidTest extends AndroidTestCase {
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

    public void testAcceptedActualsPausedRecoveryAndAtomicFinishUseOneEngine() {
        var repository = new SqliteGymExecutionRepository(getContext());
        var clock = new Clock();
        var coordinator = new GymWorkoutSessionCoordinator(repository, clock);
        coordinator.start(
                "coordinator-runtime", GymExecution.Origin.FREE, null, null, ZoneOffset.UTC);
        clock.millis = 1000;
        coordinator.addExercises(
                Immutable.list(
                        new GymWorkoutDefinition.Slot(
                                "bench",
                                ExerciseCatalog.definition("bench-press").exercise(),
                                null,
                                null)));
        clock.millis = 2000;
        coordinator.saveSet("bench", new StrengthSession.SetEntry("entered", 7, 65, 7.0, false));
        assertEquals(
                coordinator.view().accepted(),
                new SqliteGymExecutionRepository(getContext()).load("coordinator-runtime"));
        assertNull(coordinator.view().pending());
        assertNull(coordinator.view().accepted().execution().completedSession());
        assertTrue(new CoachingStore(getContext()).strength().isEmpty());
        clock.millis = 3000;
        coordinator.pause();
        clock.millis = 8000;
        coordinator.checkpoint();
        assertEquals(3000L, coordinator.view().accepted().execution().activeMillis());
        assertEquals(8000L, coordinator.view().accepted().execution().recordedElapsedMillis());

        var recoveredClock = new Clock();
        recoveredClock.continuity = "new-process";
        recoveredClock.wall = NOW.plusSeconds(3600);
        var recovered =
                new GymWorkoutSessionCoordinator(
                        new SqliteGymExecutionRepository(getContext()), recoveredClock);
        assertNull(recovered.observe().accepted());
        recovered.recoverExplicitly("coordinator-runtime");
        assertEquals(GymExecution.Status.PAUSED, recovered.view().accepted().execution().status());
        assertEquals(3000L, recovered.view().accepted().execution().activeMillis());
        assertEquals(8000L, recovered.view().accepted().execution().recordedElapsedMillis());
        assertEquals(1, recovered.view().accepted().execution().actualSets().size());
        recoveredClock.millis = 1000;
        recovered.resume();
        recoveredClock.millis = 2000;
        recovered.finish(null, "Entered evidence only", false);
        assertEquals(
                GymExecution.Status.FINISHED, recovered.view().accepted().execution().status());
        assertEquals(4000L, recovered.view().accepted().execution().activeMillis());
        assertNull(recovered.view().accepted().execution().plannedAssociation());
        var actual = new CoachingStore(getContext()).strength();
        assertEquals(1, actual.size());
        assertEquals("coordinator-runtime", actual.get(0).id());
        assertEquals(65.0, actual.get(0).exercises().get(0).sets().get(0).kilograms(), 0.0);
        assertEquals(7, actual.get(0).exercises().get(0).sets().get(0).reps());
        assertTrue(repository.unfinished().isEmpty());
        assertEquals(recovered.view().accepted(), repository.load("coordinator-runtime"));
    }

    private static final class Clock implements GymWorkoutSessionCoordinator.TickSource {
        String continuity = "boot";
        Instant wall = NOW;
        long millis;

        @Override
        public GymExecution.Tick now() {
            return new GymExecution.Tick(continuity, millis, wall.plusMillis(millis));
        }
    }
}
