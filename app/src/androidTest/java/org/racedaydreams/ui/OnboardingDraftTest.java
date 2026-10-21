package org.racedaydreams.ui;

import android.test.AndroidTestCase;

import org.json.JSONObject;
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
import java.util.EnumSet;

/** Platform instrumentation tests: Android's actual org.json implementation, no test libraries. */
@SuppressWarnings("deprecation")
public final class OnboardingDraftTest extends AndroidTestCase {
    public void testRoundTripPreservesIncompleteTextAndAvailability() {
        OnboardingDraft draft = new OnboardingDraft(null, null);
        draft.step = 2;
        draft.distance = RaceDistance.HALF_MARATHON;
        draft.event = true;
        draft.raceName = "My next race";
        draft.raceDate = null;
        draft.goalMode = GoalMode.TARGET_TIME;
        draft.targetTime = "1:";
        draft.runningDays.clear();
        draft.runsPerWeek = 6;
        draft.longRunDay = null;
        draft.gymDays = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);

        OnboardingDraft restored = new OnboardingDraft(null, draft.toJson());
        assertEquals(2, restored.step);
        assertEquals("1:", restored.targetTime);
        assertEquals("My next race", restored.raceName);
        assertNull(restored.raceDate);
        assertTrue(restored.event);
        assertTrue(restored.runningDays.isEmpty());
        assertEquals(6, restored.runsPerWeek);
        assertNull(restored.longRunDay);
        assertEquals(draft.gymDays, restored.gymDays);
        assertFalse(restored.invalidDraft);
        try {
            restored.profile();
            fail("An incomplete form must not become a validated profile");
        } catch (IllegalArgumentException expected) {
            // Incomplete forms are valid draft state, but not valid domain profiles.
        }
    }

    public void testCompletedDraftRoundTripsWithoutChangingProfile() {
        UserProfile initial = profile();
        OnboardingDraft draft = new OnboardingDraft(initial, null);
        draft.step = 3;
        OnboardingDraft restored = new OnboardingDraft(initial, draft.toJson());
        assertEquals(3, restored.step);
        assertEquals(initial, restored.profile());
        assertFalse(restored.invalidDraft);
    }

    public void testRestoredReviewCannotSilentlyDropAnUndatedEvent() {
        OnboardingDraft draft = new OnboardingDraft(null, null);
        draft.step = 3;
        draft.event = true;
        draft.raceName = "";
        draft.raceDate = null;
        draft.goalMode = GoalMode.FINISH;
        OnboardingDraft restored = new OnboardingDraft(null, draft.toJson());
        try {
            restored.profile();
            fail("An event requires a date even if the draft resumes at Review");
        } catch (IllegalArgumentException expected) {
            assertTrue(restored.event);
        }
    }

    public void testMalformedDateRestoresSavedProfileAtomically() throws Exception {
        UserProfile initial = profile();
        JSONObject json = new JSONObject(new OnboardingDraft(initial, null).toJson());
        json.put("distance", RaceDistance.MARATHON.name());
        json.put("raceName", "Unfinished replacement");
        json.put("raceDate", "not-a-date");

        OnboardingDraft restored = new OnboardingDraft(initial, json.toString());
        assertTrue(restored.invalidDraft);
        assertEquals(0, restored.step);
        assertEquals(initial, restored.profile());
    }

    public void testUnsupportedDraftVersionKeepsSavedProfile() throws Exception {
        UserProfile initial = profile();
        JSONObject json = new JSONObject(new OnboardingDraft(initial, null).toJson());
        json.put("version", 999);
        OnboardingDraft restored = new OnboardingDraft(initial, json.toString());
        assertTrue(restored.invalidDraft);
        assertEquals(initial, restored.profile());
    }

    public void testDraftCannotGrantPerformanceEligibility() throws Exception {
        JSONObject json = new JSONObject(new OnboardingDraft(null, null).toJson());
        json.put("strategy", TrainingStrategy.PERFORMANCE.name());
        json.put("performanceEligible", true);

        OnboardingDraft restored = new OnboardingDraft(null, json.toString());
        assertFalse(restored.performanceEligible);
        assertEquals(TrainingStrategy.BALANCED, restored.strategy);
        assertFalse(restored.profile().performanceEligible());
    }

    public void testGeneralFitnessDoesNotRestoreAnEventOrTimeTarget() throws Exception {
        JSONObject json = new JSONObject(new OnboardingDraft(null, null).toJson());
        json.put("distance", RaceDistance.GENERAL_FITNESS.name());
        json.put("event", true);
        json.put("raceDate", LocalDate.now().plusMonths(6).toString());
        json.put("raceName", "Old race");
        json.put("goalMode", GoalMode.TARGET_TIME.name());
        json.put("targetTime", "55:00");

        OnboardingDraft restored = new OnboardingDraft(null, json.toString());
        assertFalse(restored.event);
        assertEquals(GoalMode.FINISH, restored.goalMode);
        assertNull(restored.profile().raceGoal().raceDate());
        assertNull(restored.profile().raceGoal().targetTime());
        assertEquals("", restored.profile().raceGoal().raceName());
    }

    public void testRunningAndGymDaysCanOverlapAfterRestoration() {
        OnboardingDraft draft = new OnboardingDraft(profile(), null);
        OnboardingDraft restored = new OnboardingDraft(profile(), draft.toJson());
        assertTrue(restored.profile().preferences().runningDays().contains(DayOfWeek.TUESDAY));
        assertTrue(restored.profile().preferences().gymDays().contains(DayOfWeek.TUESDAY));
        assertEquals(UnitSystem.IMPERIAL, restored.profile().preferences().units());
        assertEquals(ScheduleType.FIXED, restored.profile().preferences().scheduleType());
    }

    private static UserProfile profile() {
        return new UserProfile(
                new RaceGoal(RaceDistance.HALF_MARATHON, "City Half",
                        LocalDate.now().plusMonths(6), GoalMode.TARGET_TIME,
                        Duration.ofSeconds(7199)),
                new TrainingPreferences(TrainingStrategy.DISTANCE_FIRST,
                        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY),
                        3, DayOfWeek.SUNDAY,
                        EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY),
                        ScheduleType.FIXED, UnitSystem.IMPERIAL), false);
    }
}
