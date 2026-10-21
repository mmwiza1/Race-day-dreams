package org.racedaydreams.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.RaceDistance;
import org.racedaydreams.domain.RaceGoal;
import org.racedaydreams.domain.ScheduleType;
import org.racedaydreams.domain.TrainingPreferences;
import org.racedaydreams.domain.TrainingStrategy;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.UserProfile;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Device-level persistence tests; domain behaviour is covered separately without Android. */
@SuppressWarnings("deprecation")
public final class SqliteUserPreferencesRepositoryTest extends AndroidTestCase {
    private SqliteUserPreferencesRepository repository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getContext().deleteDatabase(ProfileDatabase.NAME);
        repository = new SqliteUserPreferencesRepository(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        repository.close();
        getContext().deleteDatabase(ProfileDatabase.NAME);
        super.tearDown();
    }

    public void testNewInstallHasNoProfileOrDraft() {
        assertFalse(repository.load().isPresent());
        assertFalse(repository.loadDraft().isPresent());
    }

    public void testMainThreadStorageAccessIsRejectedBeforeOpeningDatabase() throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                repository.load();
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                completed.countDown();
            }
        });
        assertTrue("The main-thread check did not complete.", completed.await(5, TimeUnit.SECONDS));
        assertTrue("Main-thread access must be rejected.", failure.get() instanceof IllegalStateException);
        assertTrue(failure.get().getMessage().contains("IO executor"));
        assertFalse(getContext().getDatabasePath(ProfileDatabase.NAME).exists());
        // An IO caller can still use the same repository after an incorrect UI-thread call.
        assertFalse(repository.load().isPresent());
    }

    public void testProfileSurvivesRepositoryRecreation() {
        UserProfile expected = halfMarathon();
        repository.save(expected);
        repository.close();
        repository = new SqliteUserPreferencesRepository(getContext());
        assertEquals(expected, repository.load().get());
    }

    public void testRunningAndGymCanShareDays() {
        repository.save(halfMarathon());
        UserProfile profile = repository.load().get();
        assertTrue(profile.preferences().runningDays().contains(DayOfWeek.MONDAY));
        assertTrue(profile.preferences().gymDays().contains(DayOfWeek.MONDAY));
        assertEquals(3, profile.preferences().runsPerWeek());
    }

    public void testNoRaceDateOrTargetTimeIsPersistedWithoutInventingValues() {
        UserProfile expected = generalFitness();
        repository.save(expected);
        UserProfile actual = repository.load().get();
        assertEquals(expected, actual);
        assertNull(actual.raceGoal().raceDate());
        assertNull(actual.raceGoal().targetTime());
    }

    public void testReplacementRemovesPriorAvailabilityAndRaceTarget() {
        repository.save(halfMarathon());
        UserProfile replacement = generalFitness();
        repository.save(replacement);
        assertEquals(replacement, repository.load().get());
        assertEquals(days(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                repository.load().get().preferences().runningDays());
        assertTrue(repository.load().get().preferences().gymDays().isEmpty());
    }

    public void testDraftSurvivesRecreationAndSuccessfulSaveClearsIt() {
        String draft = "{\"version\":1,\"step\":2,\"targetTime\":\"1:59:59\"}";
        repository.saveDraft(draft);
        repository.close();
        repository = new SqliteUserPreferencesRepository(getContext());
        assertEquals(draft, repository.loadDraft().get());
        repository.save(halfMarathon());
        assertFalse(repository.loadDraft().isPresent());
    }

    public void testDraftCanBeReplacedOrClearedWithoutDeletingProfile() {
        UserProfile expected = halfMarathon();
        repository.save(expected);
        repository.saveDraft("{\"step\":1}");
        repository.saveDraft("{\"step\":2}");
        assertEquals("{\"step\":2}", repository.loadDraft().get());
        repository.clearDraft();
        assertFalse(repository.loadDraft().isPresent());
        assertEquals(expected, repository.load().get());
    }

    public void testInvalidDraftDoesNotDestroyExistingDraft() {
        repository.saveDraft("{\"step\":1}");
        try {
            repository.saveDraft("not JSON");
            fail("Invalid JSON should be rejected.");
        } catch (IllegalArgumentException expected) {
            assertEquals("{\"step\":1}", repository.loadDraft().get());
        }
    }

    public void testClearRemovesProfileAndDraft() {
        repository.save(halfMarathon());
        repository.saveDraft("{\"step\":1}");
        repository.clear();
        assertFalse(repository.load().isPresent());
        assertFalse(repository.loadDraft().isPresent());
        repository.close();
        repository = new SqliteUserPreferencesRepository(getContext());
        assertFalse(repository.load().isPresent());
    }

    public void testFailedSaveRollsBackProfileAndKeepsDraft() {
        UserProfile original = halfMarathon();
        repository.save(original);
        repository.saveDraft("{\"step\":3}");
        try (SQLiteDatabase db = getContext().openOrCreateDatabase(
                ProfileDatabase.NAME, 0, null)) {
            db.execSQL("CREATE TRIGGER reject_new_preferences BEFORE INSERT ON training_preferences "
                    + "BEGIN SELECT RAISE(ABORT, 'test write failure'); END");
        }
        try {
            repository.save(generalFitness());
            fail("The induced database failure should propagate.");
        } catch (SQLiteException expected) {
            assertEquals(original, repository.load().get());
            assertTrue(repository.loadDraft().isPresent());
        }
    }

    public void testMissingGoalSurfacesErrorWithoutDeletingRemainingData() {
        repository.save(halfMarathon());
        try (SQLiteDatabase db = getContext().openOrCreateDatabase(
                ProfileDatabase.NAME, 0, null)) {
            db.delete("race_goal", null, null);
        }
        try {
            repository.load();
            fail("A partial saved profile must not appear to be a new install.");
        } catch (IllegalStateException expected) {
            try (SQLiteDatabase db = getContext().openOrCreateDatabase(
                    ProfileDatabase.NAME, 0, null);
                 android.database.Cursor cursor = db.rawQuery(
                         "SELECT COUNT(*) FROM user_profile", null)) {
                cursor.moveToFirst();
                assertEquals(1, cursor.getInt(0));
            }
        }
    }

    public void testUnknownEnumSurfacesErrorWithoutResettingProfile() {
        repository.save(halfMarathon());
        try (SQLiteDatabase db = getContext().openOrCreateDatabase(
                ProfileDatabase.NAME, 0, null)) {
            ContentValues values = new ContentValues();
            values.put("distance", "UNRECOGNISED_DISTANCE");
            db.update("race_goal", values, "user_id = 1", null);
        }
        try {
            repository.load();
            fail("An unsupported stored enum must be visible as a storage error.");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("has not been changed"));
        }
    }

    private static Set<DayOfWeek> days(DayOfWeek... values) {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        Collections.addAll(result, values);
        return result;
    }

    private static UserProfile halfMarathon() {
        return new UserProfile(
                new RaceGoal(RaceDistance.HALF_MARATHON, "Spring Half",
                        LocalDate.of(2030, 4, 14), GoalMode.TARGET_TIME,
                        Duration.ofSeconds(7_199)),
                new TrainingPreferences(TrainingStrategy.DISTANCE_FIRST,
                        days(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
                        3, DayOfWeek.SUNDAY,
                        days(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                        ScheduleType.FLEXIBLE, UnitSystem.METRIC), false);
    }

    private static UserProfile generalFitness() {
        return new UserProfile(
                new RaceGoal(RaceDistance.GENERAL_FITNESS, "", null,
                        GoalMode.FINISH, null),
                new TrainingPreferences(TrainingStrategy.ZONE_2,
                        days(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        2, null, Collections.emptySet(), ScheduleType.FIXED, UnitSystem.IMPERIAL), false);
    }
}
