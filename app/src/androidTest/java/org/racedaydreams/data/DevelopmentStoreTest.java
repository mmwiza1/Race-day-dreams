package org.racedaydreams.data;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import org.json.*;
import org.racedaydreams.domain.body.*;
import org.racedaydreams.domain.coaching.*;
import org.racedaydreams.domain.strength.*;

import java.time.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class DevelopmentStoreTest extends AndroidTestCase {
    private DevelopmentStore development;
    private FeatureStore features;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new SqliteUserPreferencesRepository(getContext()).clear();
        development = new DevelopmentStore(getContext());
        features = new FeatureStore(getContext());
    }

    @Override
    protected void tearDown() throws Exception {
        new SqliteUserPreferencesRepository(getContext()).clear();
        super.tearDown();
    }

    private BodyWeight weight(String id, String provider, double kg) {
        return new BodyWeight(
                id,
                kg,
                Instant.parse("2026-10-01T12:00:00Z"),
                ZoneOffset.UTC,
                provider,
                id,
                "source");
    }

    public void testManualWeightEditDeleteAndUnitRoundTrip() {
        development.saveWeight(weight("w", "manual", MassUnit.LB.toKilograms(180)));
        assertEquals(
                180, MassUnit.LB.fromKilograms(development.weights().get(0).kilograms()), .00001);
        development.saveWeight(weight("w", "manual", 81));
        assertEquals(1, development.weights().size());
        assertEquals(81, development.weights().get(0).kilograms(), .00001);
        development.deleteWeight("w");
        assertTrue(development.weights().isEmpty());
    }

    public void testHistoricalUnknownApproximateDatesAndCustomExerciseRoundTrip() {
        var e =
                new StrengthExercise(
                        "custom",
                        "My lift",
                        StrengthExercise.Region.LOWER,
                        StrengthExercise.Mode.WEIGHTED,
                        true);
        development.saveHistorical(
                new HistoricalStrengthRecord(
                        "unknown",
                        e,
                        100,
                        5,
                        null,
                        HistoricalStrengthRecord.DatePrecision.UNKNOWN,
                        "Before installation"));
        development.saveHistorical(
                new HistoricalStrengthRecord(
                        "approx",
                        e,
                        90,
                        10,
                        LocalDate.of(2024, 1, 1),
                        HistoricalStrengthRecord.DatePrecision.APPROXIMATE,
                        "Approximate year"));
        assertEquals(2, development.historical().size());
        assertNull(
                development.historical().stream()
                        .filter(r -> r.id().equals("unknown"))
                        .findFirst()
                        .get()
                        .date());
        JSONObject backup = features.exportUserData();
        features.restoreUserData(backup, false);
        assertEquals(2, development.historical().size());
        assertEquals("custom", development.historical().get(0).exercise().id());
    }

    public void testExportIncludesManualWeightButNotRawProviderMeasurements() throws Exception {
        development.saveWeight(weight("manual", "manual", 80));
        development.saveWeight(weight("provider", "health-connect", 81));
        JSONObject backup = features.exportUserData();
        JSONArray rows = backup.getJSONObject("development").getJSONArray("records");
        assertEquals(1, rows.length());
        features.restoreUserData(backup, false);
        assertEquals(1, development.weights().size());
        assertEquals("manual", development.weights().get(0).provider());
    }

    public void testInvalidEvidenceBackupDoesNotChangeExistingRecords() throws Exception {
        development.saveWeight(weight("w", "manual", 80));
        JSONObject backup = features.exportUserData();
        backup.getJSONObject("development")
                .getJSONArray("records")
                .getJSONObject(0)
                .getJSONObject("payload")
                .put("kg", -10);
        try {
            features.restoreUserData(backup, false);
            fail("Invalid weight accepted");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(80, development.weights().get(0).kilograms(), .0001);
    }

    public void testProviderWeightWindowUpdatesAndDeletesWithoutTouchingManual() {
        development.saveWeight(weight("m", "manual", 80));
        development.saveWeight(weight("p", "health-connect", 81));
        development.replaceWeightWindow(
                "health-connect",
                Instant.parse("2026-09-01T00:00:00Z"),
                Instant.parse("2026-11-01T00:00:00Z"),
                List.of());
        assertEquals(1, development.weights().size());
        assertEquals("m", development.weights().get(0).id());
    }

    public void testAllPriorSchemasUpgradeWithoutLosingSettings() throws Exception {
        for (int old = 1; old <= 6; old++) {
            new SqliteUserPreferencesRepository(getContext()).clear();
            SQLiteDatabase db = getContext().openOrCreateDatabase(ProfileDatabase.NAME, 0, null);
            String[] methods = {
                "createVersionOne",
                "migrateOneToTwo",
                "migrateTwoToThree",
                "migrateThreeToFour",
                "migrateFourToFive",
                "migrateFiveToSix"
            };
            for (int i = 0; i < old; i++) {
                var method =
                        ProfileDatabase.class.getDeclaredMethod(methods[i], SQLiteDatabase.class);
                method.setAccessible(true);
                method.invoke(null, db);
            }
            db.execSQL("INSERT INTO user_profile VALUES (1,0)");
            if (old >= 2) db.execSQL("INSERT INTO settings VALUES ('retained','yes')");
            db.setVersion(old);
            db.close();
            var store = new FeatureStore(getContext());
            store.activities();
            if (old >= 2) assertEquals("yes", store.getSetting("retained"));
            assertTrue(new DevelopmentStore(getContext()).weights().isEmpty());
            try (var read = getContext().openOrCreateDatabase(ProfileDatabase.NAME, 0, null)) {
                assertEquals(ProfileDatabase.VERSION, read.getVersion());
                try (var c = read.rawQuery("SELECT COUNT(*) FROM user_profile", null)) {
                    assertTrue(c.moveToFirst());
                    assertEquals(1, c.getInt(0));
                }
            }
        }
    }

    public void testSelectedCalendarViewAndMassPreferenceSurviveBackup() {
        features.putSetting("calendar.view", "Month");
        features.putSetting("mass.unit", "LB");
        JSONObject b = features.exportUserData();
        features.restoreUserData(b, false);
        assertEquals("Month", features.getSetting("calendar.view"));
        assertEquals("LB", features.getSetting("mass.unit"));
    }

    public void testBreakStateAndPlannedStrengthLinkSurviveBackup() {
        development.saveInterruption(
                new WeeklyCoaching.Interruption(
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), "Travel"));
        development.linkStrength("session", LocalDate.of(2026, 10, 1));
        JSONObject b = features.exportUserData();
        features.restoreUserData(b, false);
        assertEquals("Travel", development.interruption().reason());
    }

    private PlanVersion seedPlan() {
        var today = LocalDate.now();
        var goal =
                new org.racedaydreams.domain.RaceGoal(
                        org.racedaydreams.domain.RaceDistance.GENERAL_FITNESS,
                        "",
                        null,
                        org.racedaydreams.domain.GoalMode.FINISH,
                        null);
        var prefs =
                new org.racedaydreams.domain.TrainingPreferences(
                        org.racedaydreams.domain.TrainingStrategy.ZONE_2,
                        EnumSet.allOf(DayOfWeek.class),
                        3,
                        DayOfWeek.SUNDAY,
                        Set.of(),
                        org.racedaydreams.domain.ScheduleType.FLEXIBLE,
                        org.racedaydreams.domain.UnitSystem.METRIC);
        var profile = new org.racedaydreams.domain.UserProfile(goal, prefs, false);
        var w =
                new org.racedaydreams.domain.PlannedWorkout(
                        "run",
                        today.plusDays(1),
                        org.racedaydreams.domain.WorkoutCategory.EASY_RUN,
                        8.0,
                        null,
                        List.of(),
                        "Original");
        var v =
                new PlanVersion(
                        "version",
                        "plan",
                        1,
                        Instant.now(),
                        "Test fixture",
                        profile,
                        "Fixture",
                        new org.racedaydreams.domain.TrainingPlan(
                                "plan", today, today.plusWeeks(3), List.of(), List.of(w)));
        new CoachingStore(getContext()).saveCurrent(v);
        return v;
    }

    private WeeklyCoaching.Review review(PlanVersion v) {
        var w = v.plan().workouts().get(0);
        return new WeeklyCoaching.Review(
                "review",
                v.id(),
                LocalDate.now()
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .minusWeeks(1),
                3,
                2,
                20,
                0,
                10.0,
                List.of("Synthetic response evidence"),
                List.of(),
                "Hold volume",
                List.of(
                        new WeeklyCoaching.Choice("keep", "Keep original", List.of()),
                        new WeeklyCoaching.Choice(
                                "reduce",
                                "Reduce",
                                List.of(
                                        new org.racedaydreams.domain.PlannedWorkout(
                                                w.id(),
                                                w.date(),
                                                w.category(),
                                                6.0,
                                                null,
                                                List.of(),
                                                "Runner selected")))));
    }

    public void testAcceptedWeeklyReviewVersionDecisionAndBackup() {
        var v = seedPlan();
        var r = review(v);
        var coach = new CoachingStore(getContext());
        development.saveReview(r);
        development.decide(r, "reduce", coach, Set.of());
        assertEquals(2, coach.current().version());
        assertEquals(6, coach.current().plan().workouts().get(0).distanceKm(), .00001);
        assertEquals("reduce", development.reviewDecision(r.id()));
        JSONObject b = features.exportUserData();
        features.restoreUserData(b, false);
        assertEquals(2, coach.versions().size());
        assertEquals("reduce", development.reviewDecision(r.id()));
    }

    public void testRejectedWeeklyReviewPreservesCurrentAndCannotApplyTwice() {
        var v = seedPlan();
        var r = review(v);
        var coach = new CoachingStore(getContext());
        development.saveReview(r);
        development.decide(r, "keep", coach, Set.of());
        assertEquals(v, coach.current());
        try {
            development.decide(r, "reduce", coach, Set.of());
            fail("Resolved review reapplied");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(1, coach.versions().size());
    }

    public void testCompletedWorkoutRejectsWeeklyChangeTransactionally() {
        var v = seedPlan();
        var r = review(v);
        var coach = new CoachingStore(getContext());
        development.saveReview(r);
        try {
            development.decide(r, "reduce", coach, Set.of("run"));
            fail("Completed session modified");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(v, coach.current());
        assertNull(development.reviewDecision(r.id()));
    }
}
