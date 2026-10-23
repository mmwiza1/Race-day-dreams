package org.racedaydreams;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Checks the merged application manifest and packaged notices, including transitive libraries. */
@SuppressWarnings("deprecation")
public final class AppPrivacyTest extends AndroidTestCase {
    public void testMergedManifestHasNoNetworkBackgroundLocationOrUnusedHealthAccess() throws Exception {
        PackageInfo info = getContext().getPackageManager().getPackageInfo(
                getContext().getPackageName(), PackageManager.GET_PERMISSIONS);
        List<String> permissions = info.requestedPermissions == null
                ? Collections.emptyList() : Arrays.asList(info.requestedPermissions);
        for (String forbidden : new String[]{"android.permission.INTERNET",
                "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.MANAGE_EXTERNAL_STORAGE",
                "android.permission.SCHEDULE_EXACT_ALARM", "android.permission.health.WRITE_SLEEP",
                "android.permission.health.WRITE_HEART_RATE_VARIABILITY", "android.permission.health.WRITE_STEPS",
                "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"}) {
            assertFalse("Unexpected application permission: " + forbidden, permissions.contains(forbidden));
        }
        assertTrue(permissions.contains("android.permission.health.READ_EXERCISE"));
        assertTrue(permissions.contains("android.permission.health.READ_DISTANCE"));
        for(String required:new String[]{"android.permission.ACCESS_FINE_LOCATION","android.permission.FOREGROUND_SERVICE_LOCATION","android.permission.health.READ_SLEEP","android.permission.health.READ_RESTING_HEART_RATE","android.permission.health.READ_HEART_RATE_VARIABILITY","android.permission.health.READ_STEPS","android.permission.health.WRITE_EXERCISE","android.permission.health.WRITE_DISTANCE"})assertTrue("Implemented optional permission missing: "+required,permissions.contains(required));
        assertEquals(0, info.applicationInfo.flags & ApplicationInfo.FLAG_ALLOW_BACKUP);
    }

    public void testOnlyIntendedEntryPointsArePublic() throws Exception {
        PackageManager manager = getContext().getPackageManager();
        String application = getContext().getPackageName();
        assertTrue(manager.getActivityInfo(new ComponentName(application,
                "org.racedaydreams.coach.CoachActivity"), 0).exported);
        assertFalse(manager.getActivityInfo(new ComponentName(application,
                "org.racedaydreams.ui.MainActivity"), 0).exported);
        assertFalse(manager.getServiceInfo(new ComponentName(application,"org.racedaydreams.recording.RunRecordingService"),0).exported);
        assertFalse(manager.getReceiverInfo(new ComponentName(application,
                "org.racedaydreams.reminders.ReminderReceiver"), 0).exported);
    }

    public void testRequiredNoticesAreAvailableOffline() throws Exception {
        for (String name : new String[]{"third_party.md", "apache-2.0.txt",
                "kotlin-LICENSE.txt", "coroutines-LICENSE.txt", "protobuf-LICENSE.txt",
                "transitive-NOTICES.txt"}) {
            try (InputStream source = getContext().getAssets().open(name)) {
                int count = 0;
                byte[] buffer = new byte[2048];
                int read;
                while ((read = source.read(buffer)) != -1) count += read;
                assertTrue("Missing/truncated notice: " + name, count > 500);
            }
        }
    }
}
