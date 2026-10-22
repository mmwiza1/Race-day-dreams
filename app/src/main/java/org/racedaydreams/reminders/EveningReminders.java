package org.racedaydreams.reminders;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.racedaydreams.R;
import org.racedaydreams.data.FeatureStore;
import org.racedaydreams.domain.PlannedWorkout;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.reminders.EveningReminder;
import org.racedaydreams.domain.reminders.EveningReminderPolicy;
import org.racedaydreams.domain.reminders.ReminderPlanEntry;
import org.racedaydreams.domain.reminders.ReminderPreferences;
import org.racedaydreams.domain.reminders.ReminderScheduleRepository;

/** Local inexact alarms. Settings and every queued delivery are checked against the current plan. */
public final class EveningReminders implements ReminderScheduleRepository {
    public static final String ACTION_DELIVER = "org.racedaydreams.REMINDER_DELIVER";
    static final String EXTRA_REMINDER = "reminder";
    private static final String PREFERENCES = "reminders.preferences.v1";
    private static final String SCHEDULE = "reminders.scheduled.v1";
    private static final String CHANNEL = "evening_run_preparation";
    private static final int MAX_PENDING = 64;
    private final Context context;
    private final FeatureStore store;
    private final Clock clock;

    public EveningReminders(Context context, FeatureStore store) {
        this(context, store, Clock.systemUTC());
    }

    EveningReminders(Context context, FeatureStore store, Clock clock) {
        this.context = context.getApplicationContext();
        this.store = store;
        this.clock = clock;
    }

    /** Wall-clock time follows the device zone, including after a time-zone change broadcast. */
    public ReminderPreferences preferences() {
        requireIo();
        String value = store.getSetting(PREFERENCES);
        if (value == null) return ReminderPreferences.defaults(ZoneId.systemDefault());
        try {
            JSONObject data = new JSONObject(value);
            if (data.getInt("version") != 1) return ReminderPreferences.defaults(ZoneId.systemDefault());
            ReminderPreferences.QuietHours quiet = data.isNull("quietStart") ? null
                    : new ReminderPreferences.QuietHours(LocalTime.parse(data.getString("quietStart")),
                            LocalTime.parse(data.getString("quietEnd")));
            return new ReminderPreferences(ReminderPreferences.Mode.valueOf(data.getString("mode")),
                    LocalTime.parse(data.getString("eveningTime")), quiet, ZoneId.systemDefault());
        } catch (JSONException | RuntimeException invalid) {
            // An invalid restored preference never enables notifications.
            return ReminderPreferences.defaults(ZoneId.systemDefault());
        }
    }

    public void savePreferences(ReminderPreferences preferences) {
        requireIo();
        try {
            JSONObject data = new JSONObject().put("version", 1).put("mode", preferences.mode().name())
                    .put("eveningTime", preferences.eveningTime().toString())
                    .put("quietStart", preferences.quietHours() == null ? JSONObject.NULL : preferences.quietHours().start().toString())
                    .put("quietEnd", preferences.quietHours() == null ? JSONObject.NULL : preferences.quietHours().end().toString());
            store.putSetting(PREFERENCES, data.toString());
            reconcile();
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Reminder preferences could not be saved.", invalid);
        }
    }

    /** Call after any plan/actual-completion/preference change, restore and app launch. */
    public void reconcile() {
        requireIo();
        ReminderPreferences preferences = preferences();
        boolean permitted = hasPermission();
        if (preferences.mode() == ReminderPreferences.Mode.DISABLED || !permitted) {
            replaceSchedule(Collections.emptyList());
            return;
        }
        List<EveningReminder> reminders = new ArrayList<>(EveningReminderPolicy.schedule(
                entries(), preferences, clock, true));
        reminders.sort(Comparator.comparing(EveningReminder::triggerAt).thenComparing(EveningReminder::workoutId));
        // Refilled on app launch, delivery and plan changes; bounds platform alarms for long plans.
        replaceSchedule(reminders.subList(0, Math.min(MAX_PENDING, reminders.size())));
    }

    /** Call before confirmed local reset or replacement restore discards the stored alarm IDs. */
    public void cancelAllScheduled() {
        requireIo();
        replaceSchedule(Collections.emptyList());
    }

    private List<ReminderPlanEntry> entries() {
        Set<String> completed = new HashSet<>();
        for (ActivityRecord activity : store.activities()) {
            if (activity.plannedWorkoutId() != null) completed.add(activity.plannedWorkoutId());
        }
        List<ReminderPlanEntry> entries = new ArrayList<>();
        for (PlannedWorkout workout : store.plannedWorkouts()) {
            long revision = store.plannedWorkoutRevision(workout.id());
            if (revision < 1) continue; // Concurrent deletion is never a reason to send a reminder.
            entries.add(new ReminderPlanEntry(workout, revision, completed.contains(workout.id())
                    ? ReminderPlanEntry.Status.COMPLETED : ReminderPlanEntry.Status.PLANNED));
        }
        return entries;
    }

    @Override public void replaceSchedule(List<EveningReminder> reminders) {
        requireIo();
        if (reminders == null || reminders.size() > MAX_PENDING) {
            throw new IllegalArgumentException("A reminder schedule must contain at most 64 workouts.");
        }
        Set<String> identities = new HashSet<>();
        for (EveningReminder reminder : reminders) {
            if (reminder == null || !identities.add(reminder.workoutId())) {
                throw new IllegalArgumentException("A reminder schedule needs distinct workout identities.");
            }
        }
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) throw new IllegalStateException("Local alarm scheduling is unavailable.");
        for (EveningReminder previous : previousSchedule()) cancel(alarms, previous.workoutId());
        JSONArray scheduled = new JSONArray();
        try {
            for (EveningReminder reminder : reminders) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt().toEpochMilli(), pending(reminder));
                scheduled.put(json(reminder));
            }
            store.putSetting(SCHEDULE, scheduled.toString());
        } catch (RuntimeException failure) {
            for (EveningReminder reminder : reminders) cancel(alarms, reminder.workoutId());
            store.putSetting(SCHEDULE, "[]");
            throw failure;
        }
    }

    private List<EveningReminder> previousSchedule() {
        String value = store.getSetting(SCHEDULE);
        if (value == null) return Collections.emptyList();
        try {
            JSONArray data = new JSONArray(value);
            List<EveningReminder> values = new ArrayList<>();
            if (data.length() > MAX_PENDING) return Collections.emptyList();
            for (int index = 0; index < data.length(); index++) values.add(fromJson(data.getJSONObject(index)));
            return values;
        } catch (JSONException | RuntimeException invalid) {
            return Collections.emptyList();
        }
    }

    private Intent identity(String workoutId) {
        return new Intent(context, ReminderReceiver.class).setAction(ACTION_DELIVER)
                .setData(new Uri.Builder().scheme("rdd-reminder").authority("workout").appendPath(workoutId).build());
    }

    private PendingIntent pending(EveningReminder reminder) {
        return PendingIntent.getBroadcast(context, 0,
                identity(reminder.workoutId()).putExtra(EXTRA_REMINDER, json(reminder).toString()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void cancel(AlarmManager alarms, String workoutId) {
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, identity(workoutId),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) { alarms.cancel(pending); pending.cancel(); }
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (notifications != null) notifications.cancel(workoutId, 0);
    }

    /** Called on a receiver's IO thread. A moved, deleted or completed workout cannot notify. */
    void deliver(EveningReminder scheduled) {
        requireIo();
        ReminderPlanEntry current = null;
        for (ReminderPlanEntry candidate : entries()) {
            if (candidate.workout().id().equals(scheduled.workoutId())) { current = candidate; break; }
        }
        boolean allowed = EveningReminderPolicy.canDeliver(scheduled, current, preferences(), clock, hasPermission());
        // Cancels obsolete platform entries and fills the next scheduling window before posting.
        reconcile();
        if (!allowed) return;
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        if (notifications == null) return;
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Evening run preparation", NotificationManager.IMPORTANCE_DEFAULT));
        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle(scheduled.title())
                .setContentText(scheduled.message()).setStyle(new Notification.BigTextStyle().bigText(scheduled.message()))
                .setAutoCancel(true).setOnlyAlertOnce(true).setTimeoutAfter(2 * 60 * 60 * 1000L);
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch != null) {
            launch.putExtra("plannedWorkoutId", scheduled.workoutId());
            builder.setContentIntent(PendingIntent.getActivity(context, 0, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        // Recheck immediately before posting; permission may be revoked after alarm scheduling.
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        notifications.notify(scheduled.workoutId(), 0, builder.build());
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        return notifications != null && notifications.areNotificationsEnabled();
    }

    static JSONObject json(EveningReminder reminder) {
        try {
            return new JSONObject().put("workoutId", reminder.workoutId()).put("revision", reminder.revision())
                    .put("workoutDate", reminder.workoutDate().toString()).put("triggerAt", reminder.triggerAt().toString())
                    .put("title", reminder.title()).put("message", reminder.message());
        } catch (JSONException impossible) { throw new IllegalArgumentException("Invalid reminder.", impossible); }
    }

    static EveningReminder fromJson(JSONObject data) throws JSONException {
        return new EveningReminder(data.getString("workoutId"), data.getLong("revision"),
                LocalDate.parse(data.getString("workoutDate")), Instant.parse(data.getString("triggerAt")),
                data.getString("title"), data.getString("message"));
    }

    private static void requireIo() {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Reminder scheduling requires an IO thread.");
    }
}
