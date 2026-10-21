package org.racedaydreams.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;
import org.racedaydreams.data.FeatureStore;

/** Revalidates actual local plan data before every delivery; no health data leaves the device. */
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        Context application = context.getApplicationContext();
        new Thread(() -> {
            try {
                // The application owns the shared SQLite pool. A short-lived receiver must not
                // close it while a foreground ViewModel operation may still be using the pool.
                FeatureStore store = new FeatureStore(application);
                EveningReminders reminders = new EveningReminders(application, store);
                if (EveningReminders.ACTION_DELIVER.equals(intent.getAction())) {
                    String payload = intent.getStringExtra(EveningReminders.EXTRA_REMINDER);
                    if (payload != null) reminders.deliver(EveningReminders.fromJson(new JSONObject(payload)));
                } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                        || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                    reminders.reconcile();
                }
            } catch (Exception unavailable) {
                // A missing/corrupt plan, revoked permission or malformed alarm never creates a
                // notification. The app's normal storage/permission UI supplies recovery controls.
            } finally {
                pending.finish();
            }
        }, "rdd-evening-reminder").start();
    }
}
