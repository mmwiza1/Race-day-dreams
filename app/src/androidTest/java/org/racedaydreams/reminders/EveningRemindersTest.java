package org.racedaydreams.reminders;

import android.test.AndroidTestCase;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONObject;
import org.racedaydreams.data.FeatureStore;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.WorkoutCategory;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.SourceIdentity;
import org.racedaydreams.domain.reminders.EveningReminder;
import org.racedaydreams.domain.reminders.EveningReminderPolicy;
import org.racedaydreams.domain.reminders.ReminderPlanEntry;
import org.racedaydreams.domain.reminders.ReminderPreferences;

/** Platform preference/serialization checks; deterministic delivery/cancellation is tested in domain. */
@SuppressWarnings("deprecation")
public final class EveningRemindersTest extends AndroidTestCase {
    private FeatureStore store;
    private EveningReminders reminders;
    @Override protected void setUp() throws Exception {
        super.setUp(); getContext().deleteDatabase("racedaydreams.db");
        store = new FeatureStore(getContext());
        reminders = new EveningReminders(getContext(), store,
                Clock.fixed(Instant.parse("2030-04-01T12:00:00Z"), ZoneOffset.UTC));
    }
    @Override protected void tearDown() throws Exception {
        reminders.savePreferences(ReminderPreferences.defaults(ZoneId.systemDefault()));
        store.close(); getContext().deleteDatabase("racedaydreams.db"); super.tearDown();
    }
    public void testRemindersDefaultToDisabledAndNoPlanSchedulesNothing() {
        assertEquals(ReminderPreferences.Mode.DISABLED, reminders.preferences().mode());
        assertEquals(LocalTime.of(19, 0), reminders.preferences().eveningTime());
        reminders.reconcile();
        assertEquals("[]", store.getSetting("reminders.scheduled.v1"));
    }
    public void testPreferenceRoundTripRetainsOptInTimeAndQuietHours() {
        ReminderPreferences expected = new ReminderPreferences(ReminderPreferences.Mode.LONG_HARD_ONLY,
                LocalTime.of(18, 30), new ReminderPreferences.QuietHours(LocalTime.of(21, 0), LocalTime.of(8, 0)),
                ZoneId.systemDefault());
        reminders.savePreferences(expected);
        assertEquals(expected, reminders.preferences());
        assertEquals("[]", store.getSetting("reminders.scheduled.v1"));
    }
    public void testMalformedRestoredPreferencesFailClosed() {
        store.putSetting("reminders.preferences.v1", "{\"version\":1,\"mode\":\"ALL\",\"eveningTime\":\"broken\"}");
        assertEquals(ReminderPreferences.Mode.DISABLED, reminders.preferences().mode());
        reminders.reconcile(); assertEquals("[]", store.getSetting("reminders.scheduled.v1"));
    }
    public void testDeliverySnapshotRoundTripsExactIdentity() throws Exception {
        EveningReminder value = new EveningReminder("run/with/id", 5, java.time.LocalDate.of(2030, 4, 2),
                Instant.parse("2030-04-01T19:00:00Z"), "Easy run tomorrow", "Your real prescription");
        assertEquals(value, EveningReminders.fromJson(new JSONObject(EveningReminders.json(value).toString())));
    }

    public void testReplacementAndCancellationInvalidateOldPendingIntent() throws Exception {
        EveningReminder original = new EveningReminder("test/moved", 1, LocalDate.of(2030, 4, 2),
                Instant.parse("2030-04-01T19:00:00Z"), "Easy run tomorrow", "Synthetic prescription");
        reminders.replaceSchedule(Collections.singletonList(original));
        PendingIntent old = pending(original.workoutId());
        assertNotNull(old);
        EveningReminder moved = new EveningReminder(original.workoutId(), 2, original.workoutDate().plusDays(1),
                original.triggerAt().plus(Duration.ofDays(1)), original.title(), original.message());
        reminders.replaceSchedule(Collections.singletonList(moved));
        PendingIntent replacement = pending(original.workoutId());
        assertNotNull(replacement);
        assertFalse("A moved reminder must replace its old broadcast token.", old.equals(replacement));
        org.json.JSONArray schedule = new org.json.JSONArray(store.getSetting("reminders.scheduled.v1"));
        assertEquals(moved, EveningReminders.fromJson(schedule.getJSONObject(0)));
        reminders.cancelAllScheduled();
        assertNull(pending(original.workoutId()));
        assertEquals("[]", store.getSetting("reminders.scheduled.v1"));
    }

    public void testInvalidReplacementDoesNotDiscardExistingSchedule() {
        EveningReminder original = new EveningReminder("test-valid", 1, LocalDate.of(2030, 4, 2),
                Instant.parse("2030-04-01T19:00:00Z"), "Easy run tomorrow", "Synthetic prescription");
        reminders.replaceSchedule(Collections.singletonList(original));
        try {
            reminders.replaceSchedule(Arrays.asList(original, original));
            fail("A duplicate workout schedule should be rejected before cancelling valid alarms.");
        } catch (IllegalArgumentException expected) {
            assertNotNull(pending(original.workoutId()));
        }
    }

    public void testMovedAndDeletedWorkoutsCannotNotifyFromOldAlarm() {
        PlannedWorkout original = workout("test-stale", LocalDate.of(2030, 4, 2));
        store.savePlannedWorkout(original);
        ReminderPreferences enabled = enabled();
        reminders.savePreferences(enabled);
        EveningReminder stale = EveningReminderPolicy.candidate(new ReminderPlanEntry(original,
                store.plannedWorkoutRevision(original.id()), ReminderPlanEntry.Status.PLANNED), enabled).get();
        EveningReminders delivery = atDelivery(stale);
        store.savePlannedWorkout(workout(original.id(), original.date().plusDays(1)));
        delivery.deliver(stale);
        assertNoNotification(original.id());
        store.deletePlannedWorkout(original.id());
        delivery.deliver(stale);
        assertNoNotification(original.id());
    }

    public void testCompletedWorkoutCannotNotifyFromOldAlarm() {
        PlannedWorkout planned = workout("test-completed", LocalDate.of(2030, 4, 2));
        store.savePlannedWorkout(planned);
        ReminderPreferences enabled = enabled();
        reminders.savePreferences(enabled);
        EveningReminder stale = EveningReminderPolicy.candidate(new ReminderPlanEntry(planned,
                store.plannedWorkoutRevision(planned.id()), ReminderPlanEntry.Status.PLANNED), enabled).get();
        store.upsertManualActivity(new ActivityRecord("completed-activity", new SourceIdentity("manual", "completed-activity", null),
                Instant.parse("2030-04-01T12:00:00Z"), Instant.parse("2030-04-01T12:30:00Z"),
                ZoneOffset.UTC, ZoneOffset.UTC, 5000.0, null, null, null, null, null, null,
                "Synthetic completion", Collections.emptyList(), Collections.emptyList(), planned.id(), null));
        atDelivery(stale).deliver(stale);
        assertNoNotification(planned.id());
    }

    private PlannedWorkout workout(String id, LocalDate date) {
        return new PlannedWorkout(id, date, WorkoutCategory.EASY_RUN, 5.0, null, Collections.emptyList(), "Synthetic");
    }
    private ReminderPreferences enabled() {
        return new ReminderPreferences(ReminderPreferences.Mode.ALL, LocalTime.of(19, 0), null, ZoneId.systemDefault());
    }
    private EveningReminders atDelivery(EveningReminder reminder) {
        return new EveningReminders(getContext(), store, Clock.fixed(reminder.triggerAt().plusSeconds(30), ZoneOffset.UTC));
    }
    private PendingIntent pending(String id) {
        Intent identity = new Intent(getContext(), ReminderReceiver.class).setAction(EveningReminders.ACTION_DELIVER)
                .setData(new Uri.Builder().scheme("rdd-reminder").authority("workout").appendPath(id).build());
        return PendingIntent.getBroadcast(getContext(), 0, identity, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }
    private void assertNoNotification(String workoutId) {
        NotificationManager manager = getContext().getSystemService(NotificationManager.class);
        assertNotNull(manager);
        for (android.service.notification.StatusBarNotification notification : manager.getActiveNotifications()) {
            assertFalse("An obsolete workout must not display a notification.", workoutId.equals(notification.getTag()));
        }
    }
}
