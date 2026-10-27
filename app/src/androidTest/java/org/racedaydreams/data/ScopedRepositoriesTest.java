package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;

import org.json.JSONObject;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.ActivityRepository;
import org.racedaydreams.domain.activity.PerformanceDistance;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultRepository;
import org.racedaydreams.domain.activity.ResultSource;
import org.racedaydreams.domain.activity.Shoe;
import org.racedaydreams.domain.activity.ShoeRepository;
import org.racedaydreams.domain.activity.SourceIdentity;
import org.racedaydreams.domain.baseline.BaselineRepository;
import org.racedaydreams.domain.baseline.ManualBaselineInput;
import org.racedaydreams.domain.coaching.PlannedWorkoutRepository;
import org.racedaydreams.domain.settings.SettingsRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** The scoped adapters must remain views over the same real SQLite transaction and old format. */
@SuppressWarnings("deprecation")
public final class ScopedRepositoriesTest extends AndroidTestCase {
    private FeatureStore legacy;
    private ActivityRepository activities;
    private ShoeRepository shoes;
    private ResultRepository results;
    private BaselineRepository baseline;
    private SettingsRepository settings;
    private PlannedWorkoutRepository planned;
    private BackupRepository backups;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ProfileDatabase.get(getContext()).close();
        getContext().deleteDatabase(ProfileDatabase.NAME);
        legacy = new FeatureStore(getContext());
        activities = new SqliteActivityRepository(getContext());
        shoes = new SqliteShoeRepository(getContext());
        results = new SqliteResultRepository(getContext());
        baseline = new SqliteBaselineRepository(getContext());
        settings = new SqliteSettingsRepository(getContext());
        planned = new SqlitePlannedWorkoutRepository(getContext());
        backups = new SqliteBackupRepository(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        legacy.close();
        getContext().deleteDatabase(ProfileDatabase.NAME);
        super.tearDown();
    }

    public void testIndependentRepositoriesAndCompatibilityFacadeSeeTheSameEvidence() {
        shoes.saveShoe(shoe());
        activities.upsertManualActivity(activity());
        activities.saveActivityNotes("actual", "Kept independently", 6d);
        results.saveResult(result());
        baseline.saveManualBaseline(manualBaseline());
        planned.savePlannedWorkout(plan());
        settings.putSetting("athlete.preference", "explicit");

        assertEquals(activities.activitySummaries(), legacy.activitySummaries());
        assertEquals("Kept independently", legacy.activityDetail("actual").notes());
        assertEquals("trainer", legacy.activityDetail("actual").shoeId());
        assertEquals(results.results(), legacy.results());
        assertEquals(shoes.shoes(), legacy.shoes());
        assertEquals(manualBaseline(), legacy.savedManualBaseline());
        assertEquals(1L, legacy.plannedWorkoutRevision("prescription"));
        assertEquals("explicit", legacy.getSetting("athlete.preference"));

        legacy.putSetting("athlete.preference", "changed explicitly");
        assertEquals("changed explicitly", settings.getSetting("athlete.preference"));
        legacy.savePlannedWorkout(plan());
        assertEquals(2L, planned.plannedWorkoutRevision("prescription"));
        assertEquals(5000d, activities.activityDetail("actual").distanceMetres(), 0d);
    }

    public void testAllRepositoriesParticipateInOneOuterTransaction() {
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.beginTransaction();
        try {
            shoes.saveShoe(shoe());
            activities.upsertManualActivity(activity());
            results.saveResult(result());
            baseline.saveManualBaseline(manualBaseline());
            planned.savePlannedWorkout(plan());
            settings.putSetting("transaction", "must roll back");
            assertEquals(1, legacy.activitySummaries().size());
            // Deliberately omit setTransactionSuccessful: nested repository writes must roll back.
        } finally {
            db.endTransaction();
        }
        assertTrue(legacy.activitySummaries().isEmpty());
        assertTrue(legacy.shoes().isEmpty());
        assertTrue(legacy.results().isEmpty());
        assertEquals(ManualBaselineInput.empty(), legacy.savedManualBaseline());
        assertNull(legacy.manualBaselineRecordedAt());
        assertTrue(legacy.plannedWorkouts().isEmpty());
        assertNull(legacy.getSetting("transaction"));
    }

    public void testScopedBackupRoundTripRetainsTheLegacyFormatAndObservationTime()
            throws Exception {
        shoes.saveShoe(shoe());
        activities.upsertManualActivity(activity());
        results.saveResult(result());
        baseline.saveManualBaseline(manualBaseline());
        planned.savePlannedWorkout(plan());
        settings.putSetting("retained", "yes");
        Instant observed = baseline.manualBaselineRecordedAt();
        JSONObject snapshot = backups.exportUserData();
        assertEquals(FeatureStore.EXPORT_STORE_VERSION, snapshot.getInt("storeVersion"));
        FeatureStore.validateUserData(snapshot);

        settings.putSetting("retained", "temporary");
        backups.restoreUserData(snapshot, false);
        assertEquals("yes", settings.getSetting("retained"));
        assertEquals(observed, baseline.manualBaselineRecordedAt());
        assertEquals("trainer", activities.activityDetail("actual").shoeId());
        assertEquals(List.of(result()), results.results());
        assertEquals(List.of(plan()), planned.plannedWorkouts());
    }

    public void testScopedRestoreFailureRollsBackAllFeatureViews() {
        shoes.saveShoe(shoe());
        activities.upsertManualActivity(activity());
        results.saveResult(result());
        baseline.saveManualBaseline(manualBaseline());
        planned.savePlannedWorkout(plan());
        settings.putSetting("retained", "before");
        JSONObject snapshot = backups.exportUserData();
        SQLiteDatabase db = ProfileDatabase.get(getContext()).getWritableDatabase();
        db.execSQL(
                "CREATE TRIGGER scoped_restore_failure BEFORE INSERT ON settings "
                        + "BEGIN SELECT RAISE(ABORT, 'injected scoped restore failure'); END");
        try {
            backups.restoreUserData(snapshot, false);
            fail("A write failure must abort the whole replacement.");
        } catch (SQLiteException expected) {
            assertEquals("before", settings.getSetting("retained"));
            assertEquals("trainer", activities.activityDetail("actual").shoeId());
            assertEquals(List.of(result()), results.results());
            assertEquals(manualBaseline(), baseline.savedManualBaseline());
            assertEquals(List.of(plan()), planned.plannedWorkouts());
        }
    }

    public void testEveryScopeRejectsMainThreadDatabaseAccessBeforeOpeningStorage()
            throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Handler(Looper.getMainLooper())
                .post(
                        () -> {
                            try {
                                List<Runnable> reads =
                                        List.of(
                                                () -> activities.activitySummaries(),
                                                () -> shoes.shoes(),
                                                () -> results.results(),
                                                () -> baseline.savedManualBaseline(),
                                                () -> planned.plannedWorkouts(),
                                                () -> settings.getSetting("read"),
                                                () -> backups.exportUserData());
                                for (Runnable read : reads) {
                                    try {
                                        read.run();
                                        throw new AssertionError(
                                                "Every repository must reject UI-thread IO.");
                                    } catch (IllegalStateException expected) {
                                        assertTrue(expected.getMessage().contains("IO thread"));
                                    }
                                }
                            } catch (Throwable error) {
                                failure.set(error);
                            } finally {
                                completed.countDown();
                            }
                        });
        assertTrue(completed.await(5, TimeUnit.SECONDS));
        if (failure.get() != null)
            throw new AssertionError("Scoped IO guard failed", failure.get());
        assertFalse(getContext().getDatabasePath(ProfileDatabase.NAME).exists());
        assertTrue(activities.activitySummaries().isEmpty());
    }

    private static Shoe shoe() {
        return new Shoe(
                "trainer", "Brand", "Model", LocalDate.of(2026, 1, 1), null, false, null, true);
    }

    private static ActivityRecord activity() {
        Instant start = Instant.parse("2026-10-26T12:00:00Z");
        return new ActivityRecord(
                "actual",
                new SourceIdentity("manual", "actual", null),
                start,
                start.plusSeconds(1800),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                5000d,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null);
    }

    private static RecordedResult result() {
        return new RecordedResult(
                "official",
                PerformanceDistance.FIVE_K,
                Duration.ofMinutes(30),
                LocalDate.of(2026, 10, 26),
                null,
                ResultSource.OFFICIAL_CHIP,
                "actual",
                null);
    }

    private static ManualBaselineInput manualBaseline() {
        return new ManualBaselineInput(20d, 3d, 10d, null, null, null, null, 8);
    }

    private static PlannedWorkout plan() {
        return new PlannedWorkout(
                "prescription",
                LocalDate.of(2026, 10, 27),
                WorkoutCategory.EASY_RUN,
                6d,
                null,
                Collections.emptyList(),
                null);
    }
}
